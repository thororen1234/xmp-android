package org.helllabs.android.xmp.compose.ui.player.viewer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.compose.theme.accent
import org.helllabs.android.xmp.compose.theme.instrumentViewFontSize
import org.helllabs.android.xmp.compose.theme.toPx
import timber.log.Timber

@Suppress("ViewConstructor")
class InstrumentViewer(context: Context, val background: Int) : Viewer(context, background) {

    private val startBlue: Int
        get() = accent.toArgb()

    private lateinit var insName: Array<String>
    private val barPaint = arrayListOf<Paint>()
    private val fontHeight: Int
    private val fontSize: Int = instrumentViewFontSize.toPx(context).toInt()
    private val fontWidth: Int
    private val insPaint = arrayListOf<Paint>()
    private val rect = Rect()
    private val paint = Paint().apply {
        alpha = 255
        isAntiAlias = true
    }

    // Draw Loop Variables
    private var chn: Int = 0
    private var drawWidth: Int = 0
    private var drawX: Int = 0
    private var drawY: Int = 0
    private var ins: Int = 0
    private var maxVol: Int = 0
    private var vol: Int = 0

    init {
        // White text volume shades
        for (i in 0..SHADE_STEPS) {
            val value = (i / SHADE_STEPS.toFloat())
            paint.color = ColorUtils.blendARGB(Color.GRAY, Color.WHITE, value)
            paint.typeface = Typeface.MONOSPACE
            paint.textSize = fontSize.toFloat()

            insPaint.add(Paint(paint))
        }

        // Blue bar volume shades
        for (i in SHADE_STEPS downTo 0) {
            val value = (i / SHADE_STEPS.toFloat())
            paint.color = ColorUtils.blendARGB(startBlue, background, value)

            barPaint.add(Paint(paint))
        }

        fontWidth = insPaint[0].measureText("X").toInt()
        fontHeight = fontSize * 14 / 10
    }

    override fun setup(modVars: IntArray) {
        super.setup(modVars)
        Timber.d("Viewer Setup")

        insName = Xmp.getInstruments() ?: arrayOf()
        setMaxY(modVars[4] * fontHeight + fontHeight / 2)
    }

    override fun update(info: Info?, isPlaying: Boolean) {
        super.update(info, isPlaying)

        requestCanvasLock { canvas ->
            doDraw(canvas, info)
        }
    }

    private fun doDraw(canvas: Canvas, info: Info?) {
        chn = modVars[3]
        ins = modVars[4]

        // Clear screen
        canvas.drawColor(bgColor)

        for (i in 0 until ins) {
            drawY = (i + 1) * fontHeight - posY.toInt()
            drawWidth = (canvasWidth - 3 * fontWidth) / chn
            maxVol = 0

            // Don't draw if not visible
            if (drawY < 0 || drawY > canvasHeight + fontHeight) {
                continue
            }

            for (j in 0 until chn) {
                if (isMuted[j]) {
                    continue
                }

                if (info!!.instruments[j] == i) {
                    drawX = 3 * fontWidth + drawWidth * j
                    vol = info.volumes[j] / 2

                    // Clamp
                    if (vol > SHADE_STEPS) {
                        vol = SHADE_STEPS
                    }

                    rect.set(drawX, drawY - fontSize + 4, drawX + drawWidth * 8 / 10, drawY + 8)
                    canvas.drawRect(rect, barPaint[vol])

                    if (vol > maxVol) {
                        maxVol = vol
                    }
                }
            }

            canvas.drawText(insName[i], 0f, drawY.toFloat(), insPaint[maxVol])
        }
    }

    companion object {
        private const val SHADE_STEPS = 32
    }
}
