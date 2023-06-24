package org.helllabs.android.xmp.player.viewer

import android.annotation.SuppressLint
import android.content.Context
import android.os.RemoteException
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.View.OnTouchListener
import org.helllabs.android.xmp.player.ScreenSizeHelper
import org.helllabs.android.xmp.service.ModInterface
import org.helllabs.android.xmp.util.Log.e
import org.helllabs.android.xmp.util.Log.i
import kotlin.math.abs

open class Viewer(
    protected val ctx: Context
) : SurfaceView(ctx), SurfaceHolder.Callback, View.OnClickListener {

    private val gestureDetector: GestureDetector
    protected lateinit var isMuted: BooleanArray
    protected lateinit var modVars: IntArray
    protected val screenSize: Int
    protected var canvasHeight = 0
    protected var canvasWidth = 0
    protected var modPlayer: ModInterface? = null
    protected var surfaceHolder: SurfaceHolder
    protected var viewerRotation = 0

    // Touch tracking
    private var maxX = 0
    private var maxY = 0
    private var isDown: Boolean
    protected var posX: Float
    protected var posY: Float
    protected var velX = 0f
    protected var velY = 0f

    class Info {
        var time = 0
        val values = IntArray(7) // order pattern row num_rows frame speed bpm
        val volumes = IntArray(64)
        val finalvols = IntArray(64)
        val pans = IntArray(64)
        val instruments = IntArray(64)
        val keys = IntArray(64)
        val periods = IntArray(64)
    }

    private fun limitPosition() {
        if (posX > maxX - canvasWidth) {
            posX = (maxX - canvasWidth).toFloat()
        }
        if (posX < 0) {
            posX = 0f
        }
        if (posY > maxY - canvasHeight) {
            posY = (maxY - canvasHeight).toFloat()
        }
        if (posY < 0) {
            posY = 0f
        }
    }

    private inner class MyGestureDetector : SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            synchronized(this) {
                posX += distanceX
                posY += distanceY
                limitPosition()
                velY = 0f
                velX = velY
            }
            return true
        }

        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            velX = velocityX / 25
            velY = velocityY / 25
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            onClick(e.x.toInt(), e.y.toInt())
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            onLongClick(e.x.toInt(), e.y.toInt())
        }

        override fun onDown(e: MotionEvent): Boolean {
            velY = 0f
            velX = velY // stop fling
            return true
        }
    }

    // Hmpf, reinventing the wheel instead of using Scroller
    private fun updateScroll() {
        posX -= velX
        posY -= velY
        limitPosition()
        velX *= 0.9.toFloat()
        if (abs(velX) < 0.5) {
            velX = 0f
        }
        velY *= 0.9.toFloat()
        if (abs(velY) < 0.5) {
            velY = 0f
        }
    }

    init {

        // register our interest in hearing about changes to our surface
        val holder = holder
        holder.addCallback(this)
        surfaceHolder = holder
        posY = 0f
        posX = posY
        isDown = false

        // Gesture detection
        gestureDetector = GestureDetector(ctx, MyGestureDetector())

        @SuppressLint("ClickableViewAccessibility")
        val gestureListener = OnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
        }
        setOnClickListener(this@Viewer)
        setOnTouchListener(gestureListener)

        val screenSizeHelper = ScreenSizeHelper()
        screenSize = screenSizeHelper.getScreenSize(ctx)
    }

    override fun onClick(view: View) {
        // do nothing
    }

    protected open fun onClick(x: Int, y: Int) {
        (parent as View).performClick()
    }

    protected open fun onLongClick(x: Int, y: Int) {
        // do nothing
    }

    open fun setRotation(value: Int) {
        viewerRotation = value
    }

    open fun update(info: Info?, paused: Boolean) {
        updateScroll()
    }

    open fun setup(modPlayer: ModInterface, modVars: IntArray) {
        i(TAG, "Viewer setup")
        val chn = modVars[3]
        this.modVars = modVars
        this.modPlayer = modPlayer
        isMuted = BooleanArray(chn)
        for (i in 0 until chn) {
            try {
                isMuted[i] = modPlayer.mute(i, -1) == 1
            } catch (e: RemoteException) {
                e(TAG, "Can't read channel mute status")
            }
        }
        posY = 0f
        posX = posY
    }

    fun setMaxX(x: Int) {
        synchronized(this) { maxX = x }
    }

    fun setMaxY(y: Int) {
        synchronized(this) { maxY = y }
    }

    /* Callback invoked when the surface dimensions change. */
    private fun setSurfaceSize(width: Int, height: Int) {
        // synchronized to make sure these all change atomically
        synchronized(surfaceHolder) {
            canvasWidth = width
            canvasHeight = height
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        setSurfaceSize(width, height)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        surfaceHolder = holder
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // do nothing
    }

    companion object {
        private const val TAG = "Viewer"
    }
}
