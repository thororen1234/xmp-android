package org.helllabs.android.xmp.compose.ui.player.viewer

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface
import android.os.RemoteException
import android.view.Surface
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.compose.theme.channelViewChannelFontSize
import org.helllabs.android.xmp.compose.theme.channelViewFontSize
import org.helllabs.android.xmp.compose.theme.toPx
import org.helllabs.android.xmp.compose.ui.player.Util
import timber.log.Timber
import java.util.Collections

@Suppress("ViewConstructor")
class ChannelViewer(context: Context, background: Int) : Viewer(context, background) {

    private val font2Size = channelViewChannelFontSize.toPx(context).toInt()

    private val fontSize: Int = channelViewFontSize.toPx(context).toInt()

    private val useNewWaveform = PrefManager.useBetterWaveform

    private lateinit var channelNumber: Array<String?>
    private lateinit var holdKey: IntArray
    private lateinit var insNameTrim: Array<String?>
    private val buffer: Array<ByteArray> // keep several buffers to hold data in pause

    private val bufferXY: FloatArray
    private val font2Height: Int
    private val font2Width: Int
    private val fontHeight: Int
    private val fontWidth: Int
    private val keyRow = IntArray(Xmp.MAX_CHANNELS)
    private val scopeHeight: Int
    private val scopeLeft: Int
    private val scopeWidth: Int
    private val volLeft: Int
    private var cols = 1
    private var insName = mutableListOf<String>()
    private var panLeft = 0
    private var panWidth = 0
    private var volWidth = 0

    private val rect = Rect()
    private val waveformPath = Path()

    private val insPaint: Paint = Paint().apply {
        setARGB(255, 200, 200, 200)
        typeface = Typeface.MONOSPACE
        textSize = fontSize.toFloat()
        isAntiAlias = true
        isLinearText = true
    }
    private val meterPaint: Paint = Paint().apply {
        setARGB(255, 40, 80, 160)
    }
    private val numPaint: Paint = Paint().apply {
        setARGB(255, 200, 200, 200)
        typeface = Typeface.MONOSPACE
        textSize = font2Size.toFloat()
        isAntiAlias = true
        isLinearText = true
    }
    private val scopeLinePaint: Paint = Paint().apply {
        setARGB(255, 80, 160, 80)
        strokeWidth = 2.0f // Thicken the scope line up.
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    private val scopeMutePaint: Paint = Paint().apply {
        setARGB(255, 60, 0, 0)
    }
    private val scopeMuteTextPaint: Paint = Paint().apply {
        setARGB(255, 220, 220, 220)
        typeface = Typeface.MONOSPACE
        textSize = fontSize.toFloat()
        isAntiAlias = true
        isLinearText = true
    }
    private val scopePaint: Paint = Paint().apply {
        setARGB(255, 40, 40, 40)
    }

    // Draw Loop Variables
    private var drawX = 0
    private var drawY = 0
    private var finalVol = 0
    private var h = 0
    private var icol = 0
    private var ins = 0
    private var key = 0
    private var num = 0
    private var numChannels = 0
    private var numInstruments = 0
    private var pan = 0
    private var panX = 0
    private var period = 0
    private var row = 0
    private var vol = 0
    private var volX = 0
    private var volY1 = 0
    private var volY2 = 0

    init {
        fontWidth = insPaint.measureText("X").toInt()
        fontHeight = fontSize * 12 / 10
        font2Width = numPaint.measureText("X").toInt()
        font2Height = font2Size * 12 / 10

        scopeWidth = 8 * fontWidth
        scopeHeight = 3 * fontHeight
        scopeLeft = 2 * font2Width + 2 * fontWidth

        volLeft = scopeLeft + scopeWidth + fontWidth * 2

        buffer = Array(Xmp.MAX_CHANNELS) { ByteArray(scopeWidth) }
        bufferXY = FloatArray(scopeWidth * 2)
    }

    override fun setup(modVars: IntArray) {
        super.setup(modVars)

        val chn = modVars[3]
        val ins = modVars[4]

        try {
            insName = Xmp.getInstruments()?.toMutableList() ?: Collections.nCopies(ins, "")
        } catch (e: RemoteException) {
            Timber.w("Can't get instrument name")
        }

        if (insName.size < ins) {
            insName.addAll(Collections.nCopies(ins - insName.size, ""))
        }

        holdKey = IntArray(chn)
        channelNumber = arrayOfNulls(chn)

        // This is much faster than String.format
        val c = CharArray(2)
        for (i in 0 until chn) {
            Util.to2d(c, i + 1)
            channelNumber[i] = String(c)
        }
    }

    override fun update(info: Info?, isPlaying: Boolean) {
        super.update(info, isPlaying)

        requestCanvasLock { canvas ->
            doDraw(canvas, info, isPlaying)
        }
    }

    private fun findScope(x: Int, y: Int): Int {
        val chn = modVars[3]
        val scopeWidth = 8 * fontWidth
        var scopeLeft = 2 * font2Width + 2 * fontWidth

        if (x >= scopeLeft && x <= scopeLeft + scopeWidth) {
            var scopeNum = (y + posY.toInt() - fontHeight) / (4 * fontHeight)
            if (cols > 1) {
                if (scopeNum >= (chn + 1) / cols) {
                    scopeNum = -1
                }
            } else {
                if (scopeNum >= chn) {
                    scopeNum = -1
                }
            }
            return scopeNum
        } else if (cols <= 1) {
            return -1
        }

        // Two column layout
        scopeLeft += canvasWidth / cols
        return if (x >= scopeLeft && x <= scopeLeft + scopeWidth) {
            var scopeNum = (y + posY.toInt() - fontHeight) / (4 * fontHeight) + (chn + 1) / cols
            if (scopeNum >= chn) {
                scopeNum = -1
            }
            scopeNum
        } else {
            -1
        }
    }

    public override fun onClick(x: Int, y: Int) {
        // Check if clicked on scopes
        val n = findScope(x, y)
        if (n >= 0) {
            try {
                val mute = if (isMuted[n]) 0 else 1
                Xmp.mute(n, mute)
                isMuted[n] = !isMuted[n]
            } catch (e: RemoteException) {
                Timber.w("Can't mute channel $n \n ${e.message}")
            }
        } else {
            super.onClick(x, y)
        }
    }

    public override fun onLongClick(x: Int, y: Int) {
        val chn = modVars[3]

        // Check if clicked on scopes
        val scope = findScope(x, y)

        // If the channel is solo, a long press unmute all channels,
        // otherwise solo this channel
        if (scope >= 0) {
            val count = (0 until chn).count { !isMuted[it] }

            if (count == 1 && !isMuted[scope]) {
                try {
                    for (i in 0 until chn) {
                        Xmp.mute(i, 0)
                        isMuted[i] = false
                    }
                } catch (e: RemoteException) {
                    Timber.w("Can't mute channels")
                }
            } else {
                try {
                    for (i in 0 until chn) {
                        Xmp.mute(i, if (i != scope) 1 else 0)
                        isMuted[i] = i != scope
                    }
                } catch (e: RemoteException) {
                    Timber.w("Can't unmute channel $scope")
                }
            }
        } else {
            super.onLongClick(x, y)
        }
    }

    override fun setRotation(value: Int) {
        super.setRotation(value)

        // Should use canvasWidth but it's not updated yet
        val width = context.resources.displayMetrics.widthPixels
        cols = when (screenSize) {
            Configuration.SCREENLAYOUT_SIZE_NORMAL ->
                if (width < 800) {
                    1
                } else if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
                    1
                } else {
                    2
                }
            Configuration.SCREENLAYOUT_SIZE_LARGE ->
                if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
                    1
                } else {
                    2
                }
            Configuration.SCREENLAYOUT_SIZE_XLARGE -> 2
            else -> 1
        }

        val chn = modVars[3]
        if (cols == 1) {
            setMaxY((chn * 4 + 1) * fontHeight)
        } else {
            setMaxY(((chn + 1) / cols * 4 + 1) * fontHeight)
        }

        volWidth = (width / cols - 5 * fontWidth - volLeft) / 2
        panLeft = volLeft + volWidth + 3 * fontWidth
        panWidth = volWidth
        val textWidth = 2 * volWidth / fontWidth + 3
        val num = insName.size
        insNameTrim = arrayOfNulls(num)

        for (i in 0 until num) {
            if (insName[i].length > textWidth) {
                insNameTrim[i] = insName[i].substring(0, textWidth)
            } else {
                insNameTrim[i] = insName[i]
            }
        }
    }

    private fun doDraw(canvas: Canvas, info: Info?, isPlaying: Boolean) {
        numChannels = modVars[3]
        numInstruments = modVars[4]
        row = info!!.values[2]

        // Clear screen
        canvas.drawColor(bgColor)

        for (chn in 0 until numChannels) {
            num = (numChannels + 1) / cols
            icol = chn % num
            drawX = chn / num * canvasWidth / 2
            drawY = (icol * 4 + 1) * fontHeight - posY.toInt()
            ins = if (isMuted[chn]) -1 else info.instruments[chn]
            vol = if (isMuted[chn]) 0 else info.volumes[chn]
            finalVol = info.finalVols[chn]
            pan = info.pans[chn]
            key = info.keys[chn]
            period = info.periods[chn]

            if (key >= 0) {
                holdKey[chn] = key
                if (keyRow[chn] == row) {
                    key = -1
                } else {
                    keyRow[chn] = row
                }
            }

            // Don't draw if not visible
            if (drawY < -scopeHeight || drawY > canvasHeight) {
                continue
            }

            // Draw channel number
            canvas.drawText(
                channelNumber[chn]!!,
                drawX.toFloat(),
                drawY + (scopeHeight + font2Height) / 2.toFloat(),
                numPaint
            )

            // Draw scopes
            rect.set(
                drawX + scopeLeft,
                drawY + 1,
                drawX + scopeLeft + scopeWidth,
                drawY + scopeHeight
            )
            if (isMuted[chn]) {
                canvas.drawRect(rect, scopeMutePaint)
                canvas.drawText(
                    "MUTE",
                    (drawX + scopeLeft + 2 * fontWidth).toFloat(),
                    (drawY + fontHeight + fontSize).toFloat(),
                    scopeMuteTextPaint
                )
            } else {
                canvas.drawRect(rect, scopePaint)
                if (isPlaying) {
                    try {
                        // Be very careful here!
                        // Our variables are latency-compensated but sample data is current
                        // so caution is needed to avoid retrieving data using old variables
                        // from a module with sample data from a newly loaded one.
                        Xmp.getSampleData(
                            key >= 0,
                            ins,
                            holdKey[chn],
                            period,
                            chn,
                            scopeWidth,
                            buffer[chn]
                        )
                    } catch (e: RemoteException) {
                        // fail silently
                    }
                }

                h = scopeHeight / 2

                if (useNewWaveform) {
                    // New scope lines.
                    var isFirst = true
                    for (j in 0 until scopeWidth) {
                        if (isFirst) {
                            isFirst = false
                            waveformPath.moveTo(
                                (drawX + scopeLeft + j).toFloat(),
                                (drawY + h + buffer[chn][j] * h * finalVol / (48 * 180)).toFloat()
                            )
                        } else {
                            waveformPath.lineTo(
                                (drawX + scopeLeft + j).toFloat(),
                                (drawY + h + buffer[chn][j] * h * finalVol / (48 * 180)).toFloat()
                            )
                        }
                    }
                    canvas.drawPath(waveformPath, scopeLinePaint)
                    waveformPath.reset()
                } else {
                    // Claudio's OG scope lines.
                    for (j in 0 until scopeWidth) {
                        bufferXY[j * 2] = (drawX + scopeLeft + j).toFloat()
                        bufferXY[j * 2 + 1] =
                            (drawY + h + buffer[chn][j] * h * finalVol / (48 * 180)).toFloat()
                    }

                    // Using drawPoints() instead of drawing each point saves a lot of CPU
                    canvas.drawPoints(bufferXY, 0, scopeWidth shl 1, scopeLinePaint)
                }
            }

            // Draw instrument name
            if (ins in 0 until numInstruments) {
                canvas.drawText(
                    insNameTrim[ins]!!,
                    drawX + volLeft.toFloat(),
                    drawY + fontHeight.toFloat(),
                    insPaint
                )
            }

            // Draw volumes
            volX = volLeft + vol * volWidth / 0x40
            volY1 = drawY + 2 * fontHeight
            volY2 = drawY + 2 * fontHeight + fontHeight / 3
            rect.set(drawX + volLeft, volY1, drawX + volX, volY2)
            canvas.drawRect(rect, meterPaint)
            rect.set(drawX + volX + 1, volY1, drawX + volLeft + volWidth, volY2)
            canvas.drawRect(rect, scopePaint)

            // Draw pan
            if (ins < 0) {
                pan = 0x80
            }

            panX = panLeft + pan * panWidth / 0x100
            rect.set(drawX + panLeft, volY1, drawX + panLeft + panWidth, volY2)
            canvas.drawRect(rect, scopePaint)
            rect.set(drawX + panX, volY1, drawX + panX + fontWidth / 2, volY2)
            canvas.drawRect(rect, meterPaint)
        }
    }
}
