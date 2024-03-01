#include <jni.h>
#include "XmpEngine.h"

XmpEngine engine;

extern "C"
JNIEXPORT jboolean JNICALL
Java_org_helllabs_android_xmp_Xmp_initPlayer(JNIEnv *env, jobject obj, jint sampleRate) {
    (void) env;
    (void) obj;
    return engine.initPlayer(sampleRate);
}
extern "C"
JNIEXPORT void JNICALL
Java_org_helllabs_android_xmp_Xmp_deInitPlayer(JNIEnv *env, jobject obj) {
    (void) env;
    (void) obj;
    engine.deInitPlayer();
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_org_helllabs_android_xmp_Xmp_loadModule(JNIEnv *env, jobject obj, jint fd) {
    (void) env;
    (void) obj;
    return engine.loadModule(fd);
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_org_helllabs_android_xmp_Xmp_startModule(JNIEnv *env, jobject obj) {
    (void) env;
    (void) obj;
    return engine.startModule(48000, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_org_helllabs_android_xmp_Xmp_stopModule(JNIEnv *env, jobject obj) {
    (void) env;
    (void) obj;
    engine.stopModule();
}

extern "C"
JNIEXPORT jstring JNICALL
Java_org_helllabs_android_xmp_Xmp_getModuleName(JNIEnv *env, jobject obj) {
    (void) obj;
    return env->NewStringUTF(engine.getModuleName());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_org_helllabs_android_xmp_Xmp_getModuleType(JNIEnv *env, jobject obj) {
    (void) obj;

    const char *type = engine.getModuleType();
    if (!type) {
        return NULL;
    }

    jstring modType = env->NewStringUTF(type);

    return modType;
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_org_helllabs_android_xmp_Xmp_getSupportedFormats(JNIEnv *env, jobject obj) {
    (void) obj;

    jstring s;
    jclass stringClass;
    jobjectArray stringArray;

    int i, num;
    const char *const *list = engine.getSupportedFormats();
    for (num = 0; list[num] != NULL; num++);

    stringClass = env->FindClass("java/lang/String");
    if (stringClass == NULL)
        return NULL;

    stringArray = env->NewObjectArray(num, stringClass, NULL);
    if (stringArray == NULL)
        return NULL;

    for (i = 0; i < num; i++) {
        s = env->NewStringUTF(list[i]);
        env->SetObjectArrayElement(stringArray, i, s);
        env->DeleteLocalRef(s);
    }

    return stringArray;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_org_helllabs_android_xmp_Xmp_getVersion(JNIEnv *env, jobject obj) {
    (void) obj;
    return env->NewStringUTF(engine.getVersion());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_org_helllabs_android_xmp_Xmp_getComment(JNIEnv *env, jobject obj) {
    (void) obj;

    const char *modComment = engine.getComment();
    if (!modComment) {
        return NULL;
    }

    char *comment = strdup(modComment);
    jstring string = env->NewStringUTF(comment);
    free(comment);

    return string;
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_org_helllabs_android_xmp_Xmp_getInstruments(JNIEnv *env, jobject obj) {
    (void) obj;

    int i;
    char buf[80];

    jstring s;
    jclass stringClass;
    jobjectArray stringArray;

    int numIns = engine.getNumberOfInstruments();
    xmp_instrument *ins = engine.getInstruments();

    if (!numIns || !ins) {
        goto err;
    }

    stringClass = env->FindClass("java/lang/String");
    if (stringClass == NULL)
        goto err;

    stringArray = env->NewObjectArray(numIns, stringClass, NULL);
    if (stringArray == NULL)
        goto err;

    for (i = 0; i < numIns; i++) {
        snprintf(buf, 80, "%02X %s", i + 1, ins[i].name);
        s = env->NewStringUTF(buf);
        env->SetObjectArrayElement(stringArray, i, s);
        env->DeleteLocalRef(s);
    }

    free(ins);

    return stringArray;

    err:
    free(ins);
    return NULL;
}

extern "C"
JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_getTime(JNIEnv *env, jobject obj) {
    (void) obj;
    (void) env;
    return engine.getTime();
}

extern "C"
JNIEXPORT void JNICALL
Java_org_helllabs_android_xmp_Xmp_getInfo(JNIEnv *env, jobject obj, jintArray values) {
    (void) obj;

    xmp_frame_info *frameInfo = engine.getFrameInfo();

    int v[7];

    v[0] = frameInfo->pos;
    v[1] = frameInfo->pattern;
    v[2] = frameInfo->row;
    v[3] = frameInfo->num_rows;
    v[4] = frameInfo->frame;
    v[5] = frameInfo->speed;
    v[6] = frameInfo->bpm;

    env->SetIntArrayRegion(values, 0, 7, v);
}

extern "C"
JNIEXPORT void JNICALL
Java_org_helllabs_android_xmp_Xmp_restartModule(JNIEnv *env, jobject obj) {
    (void) obj;
    (void) env;
    engine.restartModule();
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_org_helllabs_android_xmp_Xmp_pause(JNIEnv *env, jobject obj, jboolean is_paused) {
    (void) obj;
    (void) env;
    return engine.pause(is_paused);
}

extern "C"
JNIEXPORT void JNICALL
Java_org_helllabs_android_xmp_Xmp_getModVars(JNIEnv *env, jobject obj, jintArray vars) {
    (void) obj;

    int v[8];

    xmp_module_info mi = engine.getModuleInfo();
    int seq = engine.getSequence();

    v[0] = mi.seq_data->duration;
    v[1] = mi.mod->len;
    v[2] = mi.mod->pat;
    v[3] = mi.mod->chn;
    v[4] = mi.mod->ins;
    v[5] = mi.mod->smp;
    v[6] = mi.num_sequences;
    v[7] = seq;

    env->SetIntArrayRegion(vars, 0, 8, v);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_org_helllabs_android_xmp_Xmp_setSequence(JNIEnv *env, jobject obj, jint seq) {
    (void) obj;
    (void) env;
    return engine.setSequence(seq);
}

extern "C"
JNIEXPORT void JNICALL
Java_org_helllabs_android_xmp_Xmp_releaseModule(JNIEnv *env, jobject obj) {
    (void) obj;
    (void) env;
    engine.releaseModule();
}

extern "C"
JNIEXPORT void JNICALL
Java_org_helllabs_android_xmp_Xmp_endPlayer(JNIEnv *env, jobject obj) {
    (void) obj;
    (void) env;
    engine.endPlayer();
}

extern "C"
JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_tick(JNIEnv *env, jobject obj, jboolean should_loop) {
    (void) obj;
    (void) env;
    return static_cast<jint>(engine.tick(should_loop));
}

extern "C"
JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_getMaxSequences(JNIEnv *env, jobject obj) {
    (void) obj;
    (void) env;
    return engine.getMaxSequences();
}

// TODO hoist to engine
extern "C"
JNIEXPORT jboolean JNICALL
Java_org_helllabs_android_xmp_Xmp_testModule(JNIEnv *env, jobject obj, jint fd, jobject info) {
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
        jclass modInfoClass = env->FindClass("org/helllabs/android/xmp/model/ModInfo");

        if (modInfoClass == NULL)
            return JNI_FALSE;

        jfieldID fieldName = env->GetFieldID(modInfoClass, "name", "Ljava/lang/String;");
        jfieldID fieldType = env->GetFieldID(modInfoClass, "type", "Ljava/lang/String;");

        if (fieldName == NULL || fieldType == NULL)
            return JNI_FALSE;

        env->SetObjectField(info, fieldName, env->NewStringUTF(ti.name));
        env->SetObjectField(info, fieldType, env->NewStringUTF(ti.type));
    }

    return res == 0 ? JNI_TRUE : JNI_FALSE;
}

extern "C"
JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_mute(JNIEnv *env, jobject obj, jint channel, jint status) {
    (void) obj;
    (void) env;
    return engine.muteChannel(channel, status);
}

extern "C"
JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_seek(JNIEnv *env, jobject thiz, jint value) {
    return engine.seek(value);
}
