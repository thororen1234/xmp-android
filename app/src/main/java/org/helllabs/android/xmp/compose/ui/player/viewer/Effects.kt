package org.helllabs.android.xmp.compose.ui.player.viewer

import timber.log.Timber

// Trash attempt for an Effects list
// Far from perfect... Some effects can't be displayed due to libxmp architecture.
// See: https://github.com/libxmp/libxmp/issues/321
// Reference:
// https://wiki.openmpt.org/Manual:_Effect_Reference
// libxmp/docs/formats

// MMD Common?

object Effects {
    private val effects669 = mutableMapOf(
        96 to "A", // FX_669_PORTA_UP
        97 to "B", // FX_669_PORTA_DN
        98 to "C", // FX_669_TPORTA
        99 to "D", // FX_669_FINETUNE
        100 to "E", // FX_669_VIBRATO
        126 to "F" // FX_SPEED_CP
    ).also {
        Timber.d("669 Effects")
    }

    // TODO: Unknown Effect: Farandole Composer 1.0 | 8
    // TODO: Unknown Effect: Farandole Composer 1.0 | 13
    // TODO: Unknown Effect: Farandole Composer 1.0 | 104
    // TODO: Unknown Effect: Farandole Composer 1.0 | 105
    // https://github.com/libxmp/libxmp/blob/master/docs/formats/far100.doc
    private val effectsFarandole = mutableMapOf(
        249 to "1", // FX_FAR_PORTA_UP
        248 to "2", // FX_FAR_PORTA_DN
        122 to "3", // FX_PER_TPORTA
        251 to "4", // FX_FAR_RETRIG
        254 to "5", // FX_FAR_SETVIBRATO
        4 to "6", // FX_VIBRATO
        256 to "7", // FX_FAR_VSLIDE_UP
        252 to "8", // FX_FAR_VSLIDE_DN
        123 to "9", // FX_PER_VIBRATO
        250 to "C", // FX_FAR_DELAY
        15 to "F" // FX_SPEED
        // 13 to "-", // FX_BREAK - Is there a symbol
    ).also {
        Timber.d("Farandole Effects")
    }

    private val effectsImagoOrpheus = mutableMapOf(
        1 to "1", // FX_S3M_SPEED
        2 to "2", // FX_S3M_BPM
        3 to "3", // FX_TONEPORTA
        4 to "4", // FX_TONE_VSLIDE
        5 to "5", // FX_VIBRATO
        6 to "6", // FX_VIBRA_VSLIDE
        7 to "7", // FX_FINE_VIBRATO
        8 to "8", // FX_TREMOLO
        9 to "9", // FX_S3M_ARPEGGIO
        10 to "A", // FX_SETPAN
        11 to "B", // FX_PANSLIDE
        12 to "C", // FX_VOLSET
        13 to "D", // FX_VOLSLIDE
        14 to "E", // FX_F_VSLIDE
        15 to "F", // FX_FINETUNE
        16 to "G", // FX_NSLIDE_UP
        17 to "H", // FX_NSLIDE_DN
        18 to "I", // FX_PORTA_UP
        19 to "J", // FX_PORTA_DN
        20 to "K", // FX_IMF_FPORTA_UP
        21 to "L", // FX_IMF_FPORTA_DN
        22 to "M", // FX_FLT_CUTOFF
        23 to "N", // FX_FLT_RESN
        24 to "O", // FX_OFFSET
        25 to "P", // NONE /* fine offset */
        26 to "Q", // FX_KEYOFF
        27 to "R", // FX_MULTI_RETRIG
        28 to "S", // FX_TREMOR
        29 to "T", // FX_JUMP
        30 to "U", // FX_BREAK
        31 to "V", // FX_GLOBALVOL
        32 to "W", // FX_GVOL_SLIDE
        33 to "X", // FX_EXTENDED
        34 to "Y", // FX_CHORUS
        35 to "Z" // FX_REVERB
    ).also {
        Timber.d("Imago Orpheus Effects")
    }

    private val effectsScream3 = mutableMapOf(
        163 to "A", // FX_S3M_SPEED
        11 to "B", // FX_JUMP
        13 to "C", // FX_BREAK
        10 to "D", // FX_VOLSLIDE
        2 to "E", // FX_PORTA_DN
        1 to "F", // FX_PORTA_UP
        3 to "G", // FX_TONEPORTA
        4 to "H", // FX_VIBRATO
        29 to "I", // FX_TREMOR
        180 to "J", // FX_S3M_ARPEGGIO
        6 to "K", // FX_VIBRA_VSLIDE
        5 to "L", // FX_TONE_VSLIDE
        9 to "O", // FX_OFFSET
        27 to "Q", // FX_MULTI_RETRIG
        7 to "R", // FX_TREMOLO
        254 to "S", // FX_S3M_EXTENDED
        171 to "T", // FX_S3M_BPM
        172 to "U", // FX_FINE_VIBRATO
        16 to "V", // FX_GLOBALVOL
        8 to "X", // FX_SETPAN
        141 to "X", // FX_SURROUND ("XA4")
        14 to "S" // FX_SURROUND
        // S8x ??
    ).also {
        Timber.d("Sream 3 Effects")
    }

    private val effectsImpulse = mutableMapOf(
        163 to "A", // FX_S3M_SPEED
        11 to "B", // FX_JUMP
        142 to "C", // FX_IT_BREAK
        10 to "D", // FX_VOLSLIDE
        2 to "E", // FX_PORTA_DN
        1 to "F", // FX_PORTA_UP
        3 to "G", // FX_TONEPORTA
        4 to "H", // FX_VIBRATO
        29 to "I", // FX_TREMOR
        180 to "J", // FX_S3M_ARPEGGIO
        6 to "K", // FX_VIBRA_VSLIDE
        5 to "L", // FX_TONE_VSLIDE
        128 to "M", // FX_TRK_VOL
        129 to "N", // FX_TRK_VSLIDE
        9 to "O", // FX_OFFSET
        137 to "P", // FX_IT_PANSLIDE
        27 to "Q", // FX_MULTI_RETRIG
        7 to "R", // FX_TREMOLO
        254 to "S", // FX_XTND
        135 to "T", // FX_IT_BPM
        172 to "U", // FX_FINE_VIBRATO
        16 to "V", // FX_GLOBALVOL
        17 to "W", // FX_GVOL_SLIDE
        8 to "X", // FX_SETPAN
        138 to "Y", // FX_PANBRELLO
        141 to "S", // FX_SURROUND ("S9x")
        136 to "S", // FX_IT_ROWDELAY ("SEx")
        14 to "S", // Pattern Delay ("SDx")
        192 to "c", // FX_VSLIDE_UP_2
        193 to "d", // FX_VSLIDE_DN_2
        194 to "a", // FX_F_VSLIDE_UP_2
        195 to "b", // FX_F_VSLIDE_DN_2
        132 to "S", // FX_FLT_CUTOFF
        139 to "S", // FX_PANBRELLO_WF ("S5x")
        140 to "S" // FX_HIOFFSET
        // FX_IT_INSTFUNC ?
        // FX_FLT_RESN ?
    ).also {
        Timber.d("Impulse Effects")
    }

    private val effectsLiquid = mutableMapOf(
        0 to "A", // FX_ARPEGGIO
        171 to "B", // FX_S3M_BPM
        13 to "C", // FX_BREAK
        2 to "D", // FX_PORTA_DN
        172 to "F", // FX_FINE_VIBRATO
        11 to "J", // FX_JUMP
        10 to "L", // FX_VOLSLIDE
        14 to "M", // FX_EXTENDED
        3 to "N", // FX_TONEPORTA
        9 to "O", // FX_OFFSET
        163 to "S", // FX_S3M_SPEED
        7 to "T", // FX_TREMOLO
        1 to "U", // FX_PORTA_UP
        4 to "V", // FX_VIBRATO
        5 to "X", // FX_TONE_VSLIDE
        6 to "Y" // FX_VIBRA_VSLIDE
        // Extended effects?
    ).also {
        // Would need testing
        Timber.d("Liquid Effects")
    }

    private val effectsOktalyzer = mutableMapOf(
        1 to "1", // FX_PORTA_UP
        2 to "2", // FX_PORTA_DN
        112 to "0", // FX_OKT_ARP3
        113 to "0", // FX_OKT_ARP4
        114 to "0", // FX_OKT_ARP5
        115 to "6", // FX_NSLIDE2_DN ?
        116 to "5", // FX_NSLIDE2_UP ?
        156 to "6", // FX_NSLIDE_DN ?
        11 to "B", // FX_JUMP
        15 to "F", // FX_SPEED
        157 to "5", // FX_NSLIDE_UP ?
        12 to "C", // FX_VOLSET
        10 to "A", // FX_VOLSLIDE
        174 to "E", // FX_F_VSLIDE_DN
        17 to "E", // FX_F_VSLIDE_UP
        0 to "0" // FX_ARPEGGIO
    ).also {
        // Note: Based on docs (and pictures, effects are decimal, will sub with MOD style.
        // Would need testing
        Timber.d("Oktalyzer Effects")
    }

    private val effectsScream2 = mutableMapOf(
        15 to "A", // FX_SPEED
        11 to "B", // FX_JUMP
        13 to "C", // FX_BREAK
        10 to "D", // FX_VOLSLIDE
        2 to "E", // FX_PORTA_DN
        1 to "F", // FX_PORTA_UP
        3 to "G", // FX_TONEPORTA
        4 to "H", // FX_VIBRATO
        29 to "I", // FX_TREMOR
        0 to "J" // FX_ARPEGGIO
    ).also {
        Timber.d("Scream 2 Effects")
    }

    private val effectsFunk = mutableMapOf(
        121 to "A", // A  :Frequency Port Up
        120 to "B", // B  :Frequency Port Dn
        122 to "C", // C  :Frequency Porta
        123 to "D", // D  :Frequency Vibrato
        //  to "E", // E  :Freq Vibrato Fanin
        //  to "F", // F  :Freq Vibrato Fanout
        124 to "G", // G  :Volume Sld Up
        125 to "H", // H  :Volume Slide Down
        // /  to "I", // I  :Volume Porta
        // /  to "J", // J  :Volume Reverb
        // /  to "K", // K  :Tremolo
        0 to "L", // L  :Arpeggio
        //  to "M", // M  :Sample Offset
        12 to "N", // N  :Volume
        127 to "O", // FX_PER_CANCEL
        14 to "O", // FX_EXTENDED
        15 to "O" // FX_SPEED
    ).also {
        // Would need testing
        Timber.d("FunkTracker Effects")
    }

    private val effectsProTracker = mutableMapOf(
        0 to "0", // FX_ARPEGGIO
        1 to "1", // FX_PORTA_UP
        2 to "2", // FX_PORTA_DN
        3 to "3", // FX_TONEPORTA
        4 to "4", // FX_VIBRATO
        5 to "5", // FX_TONE_VSLIDE
        6 to "6", // FX_VIBRA_VSLIDE
        7 to "7", // FX_TREMOLO
        8 to "8", // FX_SETPAN
        9 to "9", // FX_OFFSET
        10 to "A", // FX_VOLSLIDE
        11 to "B", // FX_JUMP
        12 to "C", // FX_VOLSET
        13 to "D", // FX_BREAK
        14 to "E", // FX_EXTENDED
        15 to "F", // FX_SPEED
        16 to "G", // FX_GLOBALVOL
        27 to "Q", // FX_MULTI_RETRIG
        181 to "P", // FX_PANSL_NOMEM
        17 to "H", // FX_GVOL_SLIDE
        21 to "L", // FX_ENVPOS
        164 to "c", // FX_VOLSLIDE_2 (up down use same define?)
        33 to "X", // FX_XF_PORTA
        20 to "K", // FX_KEYOFF
        25 to "P", // FX_PANSLIDE
        29 to "T", // FX_TREMOR
        146 to "4", // FX_VIBRATO2
        160 to "x", // FX_VOLSLIDE_UP (Digital Symphony f2t)
        161 to "x", // FX_VOLSLIDE_DN (Digital Symphony f2t)
        171 to "F" // FX_S3M_BPM (DigiBooster Pro f2t)
        // FX_MED_HOLD ??
    ).also {
        Timber.d("ProTracker Effects")
    }

    fun getEffectList(type: String): MutableMap<Int, String> {
        with(type) {
            return when {
                contains("669") -> effects669
                contains("Farandole", true) -> effectsFarandole
                contains("Imago Orpheus", true) -> effectsImagoOrpheus
                contains("S3M", true) -> effectsScream3
                contains("IT", true) -> effectsImpulse
                contains("LIQ", true) -> effectsLiquid
                contains("Oktalyzer", true) -> effectsOktalyzer
                contains("STX", true) -> effectsScream2
                contains("Funk", true) -> effectsFunk
                else -> effectsProTracker // Most likely PTK based.
            }
        }
    }
}
