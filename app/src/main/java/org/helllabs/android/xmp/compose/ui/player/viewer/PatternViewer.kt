package org.helllabs.android.xmp.compose.ui.player.viewer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.compose.theme.patternViewFontSize
import org.helllabs.android.xmp.compose.theme.toPx
import org.helllabs.android.xmp.compose.ui.player.Util
import org.helllabs.android.xmp.service.PlayerService
import timber.log.Timber

@Suppress("ViewConstructor")
class PatternViewer(context: Context, background: Int) : Viewer(context, background) {

    // Draw Loop Variables
    private var adj: Float = 0f
    private var barLine: Int = 0
    private var barY: Int = 0
    private var chn: Int = 0
    private var headerX: Float = 0f
    private var lineInPattern: Int = 0
    private var lines: Int = 0
    private var numRows: Int = 0
    private var ord: Float = 0f
    private var pat: Int = 0
    private var patternX: Float = 0f
    private var patternY: Float = 0f
    private var row: Float = 0f
    private var updateRow: Float = 0f

    private var currentType: String = ""
    private lateinit var effectsTable: Map<Int, String>

    private val allNotes = (0 until MAX_NOTES).map { NOTES[it % 12] + it / 12 }.toMutableList()
    private var hexByte = mutableListOf<String>()
    private val c = CharArray(3)
    private val instHexByte = (0..255).map { i ->
        Util.to02X(c, i)
        String(c)
    }

    private var paint1: Paint = Paint()
    private var paint2: Paint = Paint()
    private var paint3: Paint = Paint()

    private val barPaint: Paint by lazy { createPaint(40, 40, 40, false) }
    private val effectsPaint: Paint by lazy { createPaint(34, 158, 60) }
    private val headerPaint: Paint by lazy { createPaint(52, 96, 186, false) }
    private val headerTextPaint: Paint by lazy { createPaint(220, 220, 220, bold = true) }
    private val insPaint: Paint by lazy { createPaint(160, 80, 80) }
    private val muteEffectsPaint: Paint by lazy { createPaint(16, 75, 28) }
    private val muteInsPaint: Paint by lazy { createPaint(80, 40, 40) }
    private val muteNotePaint: Paint by lazy { createPaint(60, 60, 60) }
    private val notePaint: Paint by lazy { createPaint(140, 140, 160) }
    private val numRowsTextPaint: Paint by lazy { createPaint(200, 200, 200, bold = true) }

    private val fontSize: Float = patternViewFontSize.toPx(context)

    private val fontHeight: Int = (fontSize * 12 / 10).toInt()
    private val fontWidth = notePaint.measureText("X").toInt()

    private val rect = Rect()
    private val rowFxParm = IntArray(64)
    private val rowFxType = IntArray(64)
    private val rowInsts = ByteArray(64)
    private val rowNotes = ByteArray(64)

    private var oldOrd = 0f
    private var oldPosX = 0f
    private var oldRow = 0f

    // Helper function to clean up redundant initialization
    private fun createPaint(
        red: Int,
        green: Int,
        blue: Int,
        bold: Boolean = false
    ) = Paint().apply {
        setARGB(255, red, green, blue)
        isAntiAlias = true
        textSize = fontSize
        typeface = if (bold) {
            Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        } else {
            Typeface.MONOSPACE
        }
    }

    override fun setup(modVars: IntArray) {
        super.setup(modVars)
        Timber.d("Viewer Setup")

        oldRow = -1f
        oldOrd = -1f
        oldPosX = -1f
        setMaxX((modVars[3] * 10 + 4 /*6 + 2*/) * fontWidth)
    }

    override fun update(info: Info?, isPlaying: Boolean) {
        super.update(info, isPlaying)

        updateRow = info!!.values[2].toFloat()
        ord = info.values[0].toFloat()

        if (oldRow == updateRow && oldOrd == ord && oldPosX == posX) {
            return
        }

        //  NumRows
        if (info.values[3] != 0) {
            // Skip first invalid infos
            oldRow = row
            oldOrd = ord
            oldPosX = posX
        }

        // Get a table of valid effects
        if (currentType != info.type) {
            Timber.d("Refreshing effects list")
            currentType = info.type
            effectsTable = Effects.getEffectList(info.type)
        }

        requestCanvasLock { canvas ->
            doDraw(canvas, info)
        }
    }

    private fun doDraw(canvas: Canvas, info: Info?) {
        lines = canvasHeight / fontHeight
        barLine = lines / 2 + 1
        barY = barLine * fontHeight
        row = info!!.values[2].toFloat()
        pat = info.values[1]
        chn = modVars[3]
        numRows = info.values[3]

        // Get the number of rows dynamically
        // Side effect of https://github.com/cmatsuoka/xmp-android/pull/15
        if (numRows > hexByte.size) {
            resizeRows()
        }

        // Clear screen
        canvas.drawColor(bgColor)

        // Header
        rect.set(0, 0, canvasWidth, fontHeight)
        canvas.drawRect(rect, headerPaint)
        for (i in 0 until chn) {
            adj = if (i + 1 < 10) 1f else .5f
            headerX = (3 + i * 10 + 3.5f + adj) * fontWidth - posX
            if (headerX > -2 * fontWidth && headerX < canvasWidth) {
                canvas.drawText((i + 1).toString(), headerX, fontSize, headerTextPaint)
            }
        }

        // Current line bar
        rect.set(0, barY - fontHeight + 10, canvasWidth, barY + 10)
        canvas.drawRect(rect, barPaint)

        // Pattern data
        for (i in 1 until lines) {
            lineInPattern = (i + row - barLine + 1).toInt()
            patternY = ((i + 1) * fontHeight).toFloat()

            if (lineInPattern < 0 || lineInPattern >= numRows) {
                continue
            }

            // Row Number
            if (posX > -2 * fontWidth) {
                canvas.drawText(lineInPattern.toString(), -posX, patternY, numRowsTextPaint)
            }

            for (j in 0 until chn) {
                // Be very careful here!
                // Our variables are latency-compensated but pattern data is current
                // so caution is needed to avoid retrieving data using old variables
                // from a module with pattern data from a newly loaded one.
                if (PlayerService.isAlive) {
                    Xmp.getPatternRow(pat, lineInPattern, rowNotes, rowInsts, rowFxType, rowFxParm)
                }

                // is muted paint
                if (isMuted[j]) {
                    paint1 = muteNotePaint
                    paint2 = muteInsPaint
                    paint3 = muteEffectsPaint
                } else {
                    paint1 = notePaint
                    paint2 = insPaint
                    paint3 = effectsPaint
                }

                patternX = (3 + j * 10 + 1 /*6*/) * fontWidth - posX
                if (patternX < -6 * fontWidth || patternX > canvasWidth) {
                    continue
                }

                // Notes
                val note = rowNotes[j]
                val notes = when {
                    note > MAX_NOTES -> ">>>"
                    note > 0 -> allNotes[note - 1]
                    note < 0 -> "==="
                    else -> "---"
                }
                canvas.drawText(notes, patternX, patternY, paint1)

                // Instruments
                patternX = (3 + j * 10 + 4/*6 + 3*/) * fontWidth - posX
                val inst = if (rowInsts[j] > 0) instHexByte[rowInsts[j].toInt()] else "--"
                canvas.drawText(inst, patternX, patternY, paint2)

                // Effects
                patternX = (3 + j * 10 + 6) * fontWidth - posX
                val effectType = effectsTable[rowFxType[j]]
                val effect: String = when {
                    rowFxType[j] > -1 ->
                        if (effectType != null) {
                            effectType
                        } else {
                            Timber.w("Unknown Effect: $currentType | ${rowFxType[j]}")
                            "?"
                        }
                    else -> "-"
                }
                canvas.drawText(effect, patternX, patternY, paint3)

                // Effects Params
                patternX = (3 + j * 10 + 7) * fontWidth - posX
                val effectParam: String = when {
                    rowFxParm[j] > -1 -> instHexByte[rowFxParm[j]]
                    else -> "--"
                }
                canvas.drawText(effectParam, patternX, patternY, paint3)
            }
        }
    }

    private fun resizeRows() {
        Timber.d("Resizing numRows: $numRows")
        hexByte.clear()
        for (i in 0 until numRows) {
            if (i <= 255) {
                Util.to02X(c, i)
            } else {
                Util.to03X(c, i)
            }
            hexByte.add(String(c))
        }
    }

    companion object {
        private const val MAX_NOTES = 120
        val NOTES = arrayOf(
            "C ", "C#", "D ", "D#", "E ", "F ", "F#", "G ", "G#", "A ", "A#", "B "
        )
    }
}
