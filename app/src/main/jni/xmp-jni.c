/* Simple and ugly interface adaptor for jni
 * If you need a JNI interface for libxmp, check the Libxmp Java API
 * at https://github.com/cmatsuoka/libxmp-java
 */

#include "audio.h"
#include "common.h"
#include "xmp.h"
#include <jni.h>
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/stat.h>

#define PERIOD_BASE 13696
#define MAX_BUFFER_SIZE 256

#define lock()   pthread_mutex_lock(&mutex)
#define unlock() pthread_mutex_unlock(&mutex)

static xmp_context ctx = NULL;
static struct xmp_module_info mi;
static struct xmp_frame_info *fi;

#pragma clang diagnostic push
#pragma ide diagnostic ignored "bugprone-reserved-identifier"
static int _buffer_num;
static int _cur_vol[XMP_MAX_CHANNELS];
static int _decay = 4;
static int _finalvol[XMP_MAX_CHANNELS];
static int _hold_vol[XMP_MAX_CHANNELS];
static int _ins[XMP_MAX_CHANNELS];
static int _key[XMP_MAX_CHANNELS];
static int _last_key[XMP_MAX_CHANNELS];
static int _loop_count;
static int _mod_is_loaded;
static int _now, _before;
static int _pan[XMP_MAX_CHANNELS];
static int _period[XMP_MAX_CHANNELS];
static int _playing = 0;
static int _pos[XMP_MAX_CHANNELS];
static int _sequence;
static jbyte _buffer[MAX_BUFFER_SIZE];
static pthread_mutex_t mutex;
#pragma clang diagnostic pop



/* For ModList */
JNIEXPORT jboolean JNICALL
Java_org_helllabs_android_xmp_Xmp_init(JNIEnv *env, jobject obj, jint rate, jint ms) {
    (void) env;
    (void) obj;

    /*if (ctx != NULL)
        return;*/

    ctx = xmp_create_context();
    pthread_mutex_init(&mutex, NULL);

    if ((_buffer_num = open_audio(rate, ms)) < 0) {
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_deinit(JNIEnv *env, jobject obj) {
    (void) env;
    (void) obj;

    xmp_free_context(ctx);
    pthread_mutex_destroy(&mutex);
    close_audio();

    return 0;
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_loadModuleFd(JNIEnv *env, jobject obj, jint fd) {
    (void) env;
    (void) obj;

    FILE *file = fdopen(fd, "r");
    if (file == NULL) {
        return -1;
    }

    struct stat statbuf;
    if (fstat(fd, &statbuf) != 0) {
        fclose(file);
        return -1;
    }
    off_t size = (off_t) statbuf.st_size;

    int res = xmp_load_module_from_file(ctx, file, size);

    xmp_get_module_info(ctx, &mi);

    memset(_pos, 0, XMP_MAX_CHANNELS * sizeof(int));
    _sequence = 0;
    _mod_is_loaded = 1;

    fclose(file);

    return res;
}

//JNIEXPORT jint JNICALL
//Java_org_helllabs_android_xmp_Xmp_loadModule(JNIEnv *env, jobject obj, jstring name) {
//    (void) obj;
//
//    const char *filename;
//    int res;
//
//    filename = (*env)->GetStringUTFChars(env, name, NULL);
//    res = xmp_load_module(ctx, (char *) filename);
//    (*env)->ReleaseStringUTFChars(env, name, filename);
//
//    xmp_get_module_info(ctx, &mi);
//
//    memset(_pos, 0, XMP_MAX_CHANNELS * sizeof(int));
//    _sequence = 0;
//    _mod_is_loaded = 1;
//
//    return res;
//}

JNIEXPORT jboolean JNICALL
Java_org_helllabs_android_xmp_Xmp_testModuleFd(JNIEnv *env, jobject obj, jint fd, jobject info) {
    (void) obj;

    FILE *file = fdopen(fd, "rb");
    if (file == NULL) {
        return JNI_FALSE;
    }

    struct xmp_test_info ti;
    int res = xmp_test_module_from_file(file, &ti);
    fclose(file);

    // Should re-handle getting filename if ti->name is empty.

    if (res == 0 && info != NULL) {
        jclass modInfoClass = (*env)->FindClass(env, "org/helllabs/android/xmp/model/ModInfo");

        if (modInfoClass == NULL)
            return JNI_FALSE;

        jfieldID fieldName = (*env)->GetFieldID(env, modInfoClass, "name", "Ljava/lang/String;");
        jfieldID fieldType = (*env)->GetFieldID(env, modInfoClass, "type", "Ljava/lang/String;");

        if (fieldName == NULL || fieldType == NULL)
            return JNI_FALSE;

        (*env)->SetObjectField(env, info, fieldName, (*env)->NewStringUTF(env, ti.name));
        (*env)->SetObjectField(env, info, fieldType, (*env)->NewStringUTF(env, ti.type));
    }

    return res == 0 ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_org_helllabs_android_xmp_Xmp_testModule(JNIEnv *env, jobject obj, jstring name, jobject info) {
    (void) obj;

    const char *filename;
    int i, res;
    struct xmp_test_info ti;

    filename = (*env)->GetStringUTFChars(env, name, NULL);
    res = xmp_test_module((char *) filename, &ti);

    /* If the module title is empty, use the file basename */
    for (i = (int) strlen(ti.name) - 1; i >= 0; i--) {
        if (ti.name[i] == ' ') {
            ti.name[i] = 0;
        } else {
            break;
        }
    }

    if (strlen(ti.name) == 0) {
        const char *x = strrchr(filename, '/');
        if (x == NULL) {
            x = filename;
        }
        strncpy(ti.name, x + 1, XMP_NAME_SIZE);
    }

    (*env)->ReleaseStringUTFChars(env, name, filename);

    if (res == 0) {
        if (info != NULL) {
            jclass modInfoClass = (*env)->FindClass(env, "org/helllabs/android/xmp/model/ModInfo");
            jfieldID field;

            if (modInfoClass == NULL)
                return JNI_FALSE;

            field = (*env)->GetFieldID(env, modInfoClass, "name", "Ljava/lang/String;");

            if (field == NULL)
                return JNI_FALSE;

            (*env)->SetObjectField(env, info, field, (*env)->NewStringUTF(env, ti.name));

            field = (*env)->GetFieldID(env, modInfoClass, "type", "Ljava/lang/String;");

            if (field == NULL)
                return JNI_FALSE;

            (*env)->SetObjectField(env, info, field, (*env)->NewStringUTF(env, ti.type));
        }

        return JNI_TRUE;
    }

    return JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_releaseModule(JNIEnv *env, jobject obj) {
    (void) env;
    (void) obj;

    lock();

    if (_mod_is_loaded) {
        _mod_is_loaded = 0;
        xmp_release_module(ctx);
    }

    unlock();

    return 0;
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_startPlayer(JNIEnv *env, jobject obj, jint rate) {
    (void) env;
    (void) obj;

    int i, ret;

    lock();

    fi = calloc(1, _buffer_num * sizeof(struct xmp_frame_info));
    if (fi == NULL) {
        unlock();
        return -101;
    }

    for (i = 0; i < XMP_MAX_CHANNELS; i++) {
        _key[i] = -1;
        _last_key[i] = -1;
    }

    _now = _before = 0;
    _loop_count = 0;
    _playing = 1;
    ret = xmp_start_player(ctx, rate, 0);

    unlock();

    return ret;
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_endPlayer(JNIEnv *env, jobject obj) {
    (void) env;
    (void) obj;

    lock();

    if (_playing) {
        _playing = 0;
        xmp_end_player(ctx);
        free(fi);
        fi = NULL;
    }

    unlock();

    return 0;
}

int play_buffer(void *buffer, int size, int looped) {
    int ret = -XMP_END;
    int num_loop;

    lock();

    if (_playing) {
        num_loop = looped ? 0 : _loop_count + 1;
        ret = xmp_play_buffer(ctx, buffer, size, num_loop);
        xmp_get_frame_info(ctx, &fi[_now]);
        INC(_before, _buffer_num);
        _now = (_before + _buffer_num - 1) % _buffer_num;
        _loop_count = fi[_now].loop_count;
    }

    unlock();

    return ret;
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_playAudio(JNIEnv *env, jobject obj) {
    (void) env;
    (void) obj;

    return play_audio();
}

JNIEXPORT void JNICALL
Java_org_helllabs_android_xmp_Xmp_dropAudio(JNIEnv *env, jobject obj) {
    (void) env;
    (void) obj;

    drop_audio();
}

JNIEXPORT jboolean JNICALL
Java_org_helllabs_android_xmp_Xmp_stopAudio(JNIEnv *env, jobject obj) {
    (void) env;
    (void) obj;

    return stop_audio() == 0 ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_org_helllabs_android_xmp_Xmp_restartAudio(JNIEnv *env, jobject obj) {
    (void) env;
    (void) obj;

    return restart_audio() == 0 ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_org_helllabs_android_xmp_Xmp_hasFreeBuffer(JNIEnv *env, jobject obj) {
    (void) env;
    (void) obj;

    return has_free_buffer() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_fillBuffer(JNIEnv *env, jobject obj, jboolean looped) {
    (void) env;
    (void) obj;

    return fill_buffer(looped);
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_nextPosition(JNIEnv *env, jobject obj) {
    (void) env;
    (void) obj;

    return xmp_next_position(ctx);
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_prevPosition(JNIEnv *env, jobject obj) {
    (void) env;
    (void) obj;

    return xmp_prev_position(ctx);
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_setPosition(JNIEnv *env, jobject obj, jint n) {
    (void) env;
    (void) obj;

    return xmp_set_position(ctx, n);
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_stopModule(JNIEnv *env, jobject obj) {
    (void) env;
    (void) obj;

    xmp_stop_module(ctx);

    return 0;
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_restartModule(JNIEnv *env, jobject obj) {
    (void) env;
    (void) obj;

    xmp_restart_module(ctx);

    return 0;
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_seek(JNIEnv *env, jobject obj, jint time) {
    (void) env;
    (void) obj;

    int ret;
    int i;

    lock();

    ret = xmp_seek_time(ctx, time);

    if (_playing) {
        for (i = 0; i < _buffer_num; i++) {
            fi[i].time = time;
        }
    }

    unlock ();

    return ret;
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_time(JNIEnv *env, jobject obj) {
    (void) env;
    (void) obj;

    return _playing ? fi[_before].time : -1;
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_mute(JNIEnv *env, jobject obj, jint chn, jint status) {
    (void) env;
    (void) obj;

    return xmp_channel_mute(ctx, chn, status);
}

JNIEXPORT void JNICALL
Java_org_helllabs_android_xmp_Xmp_getInfo(JNIEnv *env, jobject obj, jintArray values) {
    (void) obj;

    int v[7];

    lock();

    if (_playing) {
        v[0] = fi[_before].pos;
        v[1] = fi[_before].pattern;
        v[2] = fi[_before].row;
        v[3] = fi[_before].num_rows;
        v[4] = fi[_before].frame;
        v[5] = fi[_before].speed;
        v[6] = fi[_before].bpm;

        (*env)->SetIntArrayRegion(env, values, 0, 7, v);
    }

    unlock();
}

JNIEXPORT void JNICALL
Java_org_helllabs_android_xmp_Xmp_setPlayer(JNIEnv *env, jobject obj, jint parm, jint val) {
    (void) env;
    (void) obj;

    xmp_set_player(ctx, parm, val);
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_getPlayer(JNIEnv *env, jobject obj, jint parm) {
    (void) env;
    (void) obj;

    return xmp_get_player(ctx, parm);
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_getLoopCount(JNIEnv *env, jobject obj) {
    (void) env;
    (void) obj;

    return fi[_before].loop_count;
}

JNIEXPORT void JNICALL
Java_org_helllabs_android_xmp_Xmp_getModVars(JNIEnv *env, jobject obj, jintArray vars) {
    (void) obj;

    int v[8];

    lock();

    if (!_mod_is_loaded) {
        unlock();
        return;
    }

    v[0] = mi.seq_data[_sequence].duration;
    v[1] = mi.mod->len;
    v[2] = mi.mod->pat;
    v[3] = mi.mod->chn;
    v[4] = mi.mod->ins;
    v[5] = mi.mod->smp;
    v[6] = mi.num_sequences;
    v[7] = _sequence;

    (*env)->SetIntArrayRegion(env, vars, 0, 8, v);

    unlock();
}

JNIEXPORT jstring JNICALL
Java_org_helllabs_android_xmp_Xmp_getVersion(JNIEnv *env, jobject obj) {
    (void) obj;

    return (*env)->NewStringUTF(env, xmp_version);
}

JNIEXPORT jobjectArray JNICALL
Java_org_helllabs_android_xmp_Xmp_getFormats(JNIEnv *env, jobject obj) {
    (void) obj;

    jstring s;
    jclass stringClass;
    jobjectArray stringArray;
    int i, num;
    const char *const *list;
    // char buf[80];

    list = xmp_get_format_list();

    for (num = 0; list[num] != NULL; num++);

    stringClass = (*env)->FindClass(env, "java/lang/String");
    if (stringClass == NULL)
        return NULL;

    stringArray = (*env)->NewObjectArray(env, num, stringClass, NULL);
    if (stringArray == NULL)
        return NULL;

    for (i = 0; i < num; i++) {
        s = (*env)->NewStringUTF(env, list[i]);
        (*env)->SetObjectArrayElement(env, stringArray, i, s);
        (*env)->DeleteLocalRef(env, s);
    }

    return stringArray;
}

JNIEXPORT jstring JNICALL
Java_org_helllabs_android_xmp_Xmp_getModName(JNIEnv *env, jobject obj) {
    (void) obj;

    char *s = _mod_is_loaded ? mi.mod->name : "";

    return (*env)->NewStringUTF(env, s);
}

JNIEXPORT jstring JNICALL
Java_org_helllabs_android_xmp_Xmp_getModType(JNIEnv *env, jobject obj) {
    (void) obj;

    char *s = _mod_is_loaded ? mi.mod->type : "";

    return (*env)->NewStringUTF(env, s);
}

// lazy hack to sanitize comments to valid utf-8
void sanitizeUTF8(char *str) {
    unsigned char *bytes = (unsigned char *) str;
    while (*bytes) {
        if (*bytes < 0x80) {
            // ASCII byte, move to the next one
            bytes++;
        } else if ((*bytes & 0xE0) == 0xC0) {
            // Possible 2-byte sequence
            if ((bytes[1] & 0xC0) == 0x80) {
                bytes += 2; // Valid 2-byte sequence
            } else {
                *bytes++ = '?'; // Invalid continuation byte
            }
        } else if ((*bytes & 0xF0) == 0xE0) {
            // Possible 3-byte sequence
            if ((bytes[1] & 0xC0) == 0x80 && (bytes[2] & 0xC0) == 0x80) {
                bytes += 3; // Valid 3-byte sequence
            } else {
                *bytes++ = '?'; // Invalid sequence, replace and move
            }
        } else if ((*bytes & 0xF8) == 0xF0) {
            // Possible 4-byte sequence
            if ((bytes[1] & 0xC0) == 0x80 && (bytes[2] & 0xC0) == 0x80 &&
                (bytes[3] & 0xC0) == 0x80) {
                bytes += 4; // Valid 4-byte sequence
            } else {
                *bytes++ = '?'; // Invalid sequence, replace and move
            }
        } else {
            // For bytes that don't start a valid sequence, replace them
            *bytes++ = '?';
        }
    }
}

JNIEXPORT jstring JNICALL
Java_org_helllabs_android_xmp_Xmp_getComment(JNIEnv *env, jobject obj) {
    (void) obj;

    if (mi.comment) {
        char *comment = strdup(mi.comment);
        if (!comment) {
            return NULL;
        }

        sanitizeUTF8(comment);

        jstring result = (*env)->NewStringUTF(env, comment);

        free(comment);

        return result;
    } else {
        return NULL;
    }

//    if (mi.comment)
//        return (*env)->NewStringUTF(env, mi.comment);
//    else
//        return NULL;
}

JNIEXPORT jobjectArray JNICALL
Java_org_helllabs_android_xmp_Xmp_getInstruments(JNIEnv *env, jobject obj) {
    (void) obj;

    jstring s;
    jclass stringClass;
    jobjectArray stringArray;
    int i;
    char buf[80];
    // int ins;

    if (!_mod_is_loaded)
        return NULL;

    stringClass = (*env)->FindClass(env, "java/lang/String");
    if (stringClass == NULL)
        return NULL;

    stringArray = (*env)->NewObjectArray(env, mi.mod->ins, stringClass, NULL);
    if (stringArray == NULL)
        return NULL;

    for (i = 0; i < mi.mod->ins; i++) {
        snprintf(buf, 80, "%02X %s", i + 1, mi.mod->xxi[i].name);
        s = (*env)->NewStringUTF(env, buf);
        (*env)->SetObjectArrayElement(env, stringArray, i, s);
        (*env)->DeleteLocalRef(env, s);
    }

    return stringArray;
}

static struct xmp_subinstrument *get_subinstrument(int ins, int key) {
    if (ins >= 0 && ins < mi.mod->ins && key < XMP_MAX_KEYS) {
        if (mi.mod->xxi[ins].map[key].ins != 0xff) {
            int mapped = mi.mod->xxi[ins].map[key].ins;

            return &mi.mod->xxi[ins].sub[mapped];
        }
    }

    return NULL;
}

JNIEXPORT void JNICALL
Java_org_helllabs_android_xmp_Xmp_getChannelData(JNIEnv *env, jobject obj, jintArray vol,
                                                 jintArray finalvol, jintArray pan, jintArray ins,
                                                 jintArray key, jintArray period) {
    (void) obj;

    struct xmp_subinstrument *sub;
    int chn = mi.mod->chn;
    int i;

    lock();

    if (!_mod_is_loaded || !_playing) {
        unlock();
        return;
    }

    for (i = 0; i < chn; i++) {
        struct xmp_channel_info *ci = &fi[_before].channel_info[i];

        if (ci->event.vol > 0) {
            _hold_vol[i] = ci->event.vol * 0x40 / mi.vol_base;
        }

        _cur_vol[i] -= _decay;
        if (_cur_vol[i] < 0) {
            _cur_vol[i] = 0;
        }

        if (ci->event.note > 0 && ci->event.note <= 0x80) {
            _key[i] = ci->event.note - 1;
            _last_key[i] = _key[i];
            sub = get_subinstrument(ci->instrument, _key[i]);
            if (sub != NULL) {
                _cur_vol[i] = sub->vol * 0x40 / mi.vol_base;
            }
        } else {
            _key[i] = -1;
        }

        if (ci->event.vol > 0) {
            _key[i] = _last_key[i];
            _cur_vol[i] = ci->event.vol * 0x40 / mi.vol_base;
        }

        _ins[i] = (int) (unsigned char) ci->instrument;
        _finalvol[i] = ci->volume;
        _pan[i] = ci->pan;
        _period[i] = (int) ci->period >> 8;
    }

    (*env)->SetIntArrayRegion(env, vol, 0, chn, _cur_vol);
    (*env)->SetIntArrayRegion(env, finalvol, 0, chn, _finalvol);
    (*env)->SetIntArrayRegion(env, pan, 0, chn, _pan);
    (*env)->SetIntArrayRegion(env, ins, 0, chn, _ins);
    (*env)->SetIntArrayRegion(env, key, 0, chn, _key);
    (*env)->SetIntArrayRegion(env, period, 0, chn, _period);

    unlock();
}

JNIEXPORT void JNICALL
Java_org_helllabs_android_xmp_Xmp_getPatternRow(JNIEnv *env, jobject obj, jint pat, jint row,
                                                jbyteArray rowNotes, jbyteArray rowInstruments,
                                                jbyteArray rowFxType, jbyteArray rowFxParm) {
    (void) obj;

    struct xmp_pattern *xxp;
    jbyte row_note[XMP_MAX_CHANNELS];
    jbyte row_ins[XMP_MAX_CHANNELS];
    jbyte row_fxt[XMP_MAX_CHANNELS];
    jbyte row_fxp[XMP_MAX_CHANNELS];
    int chn;
    int i;

    if (!_mod_is_loaded)
        return;

    if (pat > mi.mod->pat || row > mi.mod->xxp[pat]->rows)
        return;

    xxp = mi.mod->xxp[pat];
    chn = mi.mod->chn;

    for (i = 0; i < chn; i++) {
        struct xmp_track *xxt = mi.mod->xxt[xxp->index[i]];
        struct xmp_event *e = &xxt->event[row];

        row_note[i] = (jbyte) e->note;
        row_ins[i] = (jbyte) e->ins;

        // Get the Effect or Secondary Effect type
        if (e->fxt > 0) {
            row_fxt[i] = (char) e->fxt;
            row_fxp[i] = (char) e->fxp;
        } else if (e->f2t > 0) {
            row_fxt[i] = (char) e->f2t;
            row_fxp[i] = (char) e->f2p;
        } else {
            if (e->fxt == 0 && e->fxp > 0) {
                // Most likely Arpeggio, good enough.
                row_fxt[i] = (char) e->fxt;
                row_fxp[i] = (char) e->fxp;
            } else {
                row_fxt[i] = -1;
                row_fxp[i] = -1;
            }
        }
    }

    (*env)->SetByteArrayRegion(env, rowNotes, 0, chn, row_note);
    (*env)->SetByteArrayRegion(env, rowInstruments, 0, chn, row_ins);
    (*env)->SetByteArrayRegion(env, rowFxType, 0, chn, row_fxt);
    (*env)->SetByteArrayRegion(env, rowFxParm, 0, chn, row_fxp);
}

JNIEXPORT void JNICALL
Java_org_helllabs_android_xmp_Xmp_getSampleData(JNIEnv *env, jobject obj, jboolean trigger,
                                                jint ins, jint key, jint period, jint chn,
                                                jint width, jbyteArray buffer) {
    (void) obj;

    struct xmp_subinstrument *sub;
    struct xmp_sample *xxs;
    int i, pos, transient_size;
    int limit;
    int step, len, lps, lpe;

    lock();

    if (!_mod_is_loaded)
        goto err;

    if (width > MAX_BUFFER_SIZE) {
        width = MAX_BUFFER_SIZE;
    }

    if (period == 0) {
        goto err;
    }

    if (ins < 0 || ins > mi.mod->ins || key > 0x80) {
        goto err;
    }

    sub = get_subinstrument(ins, key);
    if (sub == NULL || sub->sid < 0 || sub->sid >= mi.mod->smp) {
        goto err;
    }

    xxs = &mi.mod->xxs[sub->sid];
    if (xxs == NULL || xxs->flg & XMP_SAMPLE_SYNTH || xxs->len == 0) {
        goto err;
    }

    step = (PERIOD_BASE << 4) / period;
    len = xxs->len << 5;
    lps = xxs->lps << 5;
    lpe = xxs->lpe << 5;

    pos = _pos[chn];

    /* In case of new keypress, reset sample */
    if (trigger == JNI_TRUE || (pos >> 5) >= xxs->len) {
        pos = 0;
    }

    /* Limit is the buffer size or the remaining transient size */
    if (step == 0) {
        transient_size = 0;
    } else if (xxs->flg & XMP_SAMPLE_LOOP) {
        transient_size = (lps - pos) / step;
    } else {
        transient_size = (len - pos) / step;
    }

    if (transient_size < 0) {
        transient_size = 0;
    }

    limit = width;
    if (limit > transient_size) {
        limit = transient_size;
    }

    if (xxs->flg & XMP_SAMPLE_16BIT) {
        /* transient */
        for (i = 0; i < limit; i++) {
            _buffer[i] = (jbyte) (((short *) xxs->data)[pos >> 5] >> 8);
            pos += step;
        }

        /* loop */
        if (xxs->flg & XMP_SAMPLE_LOOP) {
            for (i = limit; i < width; i++) {

                _buffer[i] = (jbyte) (((short *) xxs->data)[pos >> 5] >> 8);
                pos += step;
                if (pos >= lpe)
                    pos = lps + pos - lpe;
                if (pos >= lpe)        /* avoid division */
                    pos = lps;
            }
        } else {
            for (i = limit; i < width; i++) {
                _buffer[i] = 0;
            }
        }
    } else {
        /* transient */
        for (i = 0; i < limit; i++) {
            _buffer[i] = (jbyte) xxs->data[pos >> 5];
            pos += step;
        }

        /* loop */
        if (xxs->flg & XMP_SAMPLE_LOOP) {
            for (i = limit; i < width; i++) {
                _buffer[i] = (jbyte) xxs->data[pos >> 5];
                pos += step;
                if (pos >= lpe)
                    pos = lps + pos - lpe;
                if (pos >= lpe)        /* avoid division */
                    pos = lps;
            }
        } else {
            for (i = limit; i < width; i++) {
                _buffer[i] = 0;
            }
        }
    }

    _pos[chn] = pos;

    (*env)->SetByteArrayRegion(env, buffer, 0, width, _buffer);

    unlock();

    return;

    err:
    memset(_buffer, 0, width);
    (*env)->SetByteArrayRegion(env, buffer, 0, width, _buffer);

    unlock();
}

JNIEXPORT jboolean JNICALL
Java_org_helllabs_android_xmp_Xmp_setSequence(JNIEnv *env, jobject obj, jint seq) {
    (void) env;
    (void) obj;

    if (seq >= mi.num_sequences)
        return JNI_FALSE;

    if (mi.seq_data[_sequence].duration <= 0)
        return JNI_FALSE;

    if (_sequence == seq)
        return JNI_FALSE;

    _sequence = seq;
    _loop_count = 0;

    xmp_set_position(ctx, mi.seq_data[_sequence].entry_point);
    xmp_play_buffer(ctx, NULL, 0, 0);

    return JNI_TRUE;
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_getMaxSequences(JNIEnv *env, jobject obj) {
    (void) env;
    (void) obj;

    return MAX_SEQUENCES;
}

JNIEXPORT void JNICALL
Java_org_helllabs_android_xmp_Xmp_getSeqVars(JNIEnv *env, jobject obj, jintArray vars) {
    (void) obj;

    int i, num, v[16];

    if (!_mod_is_loaded)
        return;

    num = mi.num_sequences;
    if (num > 16) {
        num = 16;
    }

    for (i = 0; i < num; i++) {
        v[i] = mi.seq_data[i].duration;
    }

    (*env)->SetIntArrayRegion(env, vars, 0, num, v);
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_getVolume(JNIEnv *env, jobject obj) {
    (void) env;
    (void) obj;

    return get_volume();
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_setVolume(JNIEnv *env, jobject obj, jint vol) {
    (void) env;
    (void) obj;

    return set_volume(vol);
}
