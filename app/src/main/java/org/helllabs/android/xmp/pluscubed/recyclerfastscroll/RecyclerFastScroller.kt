package org.helllabs.android.xmp.pluscubed.recyclerfastscroll

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import org.helllabs.android.xmp.R

@Suppress("KDocUnresolvedReference")
class RecyclerFastScroller @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val mHiddenTranslationX: Int
    private val mHide: Runnable
    private val mMinScrollHandleHeight: Int
    private var mAnimator: AnimatorSet? = null
    val mBar: View
    val mHandle: View?
    var mAnimatingIn = false
    var mAppBarLayoutOffset = 0
    var mOnTouchListener: OnTouchListener? = null
    var mRecyclerView: RecyclerView? = null

    @get:ColorInt
    var handlePressedColor: Int
        get() = mHandlePressedColor
        set(colorPressed) {
            mHandlePressedColor = colorPressed
            updateHandleColorsAndInset()
        }

    @get:ColorInt
    var handleNormalColor: Int
        get() = mHandleNormalColor
        set(colorNormal) {
            mHandleNormalColor = colorNormal
            updateHandleColorsAndInset()
        }

    /**
     * @param scrollBarColor Scroll bar color. Alpha will be set to ~22% to match stock scrollbar.
     */
    @get:ColorInt
    var barColor: Int
        get() = mBarColor
        set(scrollBarColor) {
            mBarColor = scrollBarColor
            updateBarColorAndInset()
        }

    /**
     * @param touchTargetWidth In pixels, less than or equal to 48dp
     */
    private var touchTargetWidth: Int
        get() = mTouchTargetWidth
        set(touchTargetWidth) {
            mTouchTargetWidth = touchTargetWidth
            val eightDp = RecyclerFastScrollerUtils.convertDpToPx(context, 8f)
            mBarInset = mTouchTargetWidth - eightDp
            val fortyEightDp = RecyclerFastScrollerUtils.convertDpToPx(context, 48f)
            if (mTouchTargetWidth > fortyEightDp) {
                throw RuntimeException("Touch target width cannot be larger than 48dp!")
            }
            mBar.layoutParams = LayoutParams(
                touchTargetWidth,
                ViewGroup.LayoutParams.MATCH_PARENT,
                GravityCompat.END
            )
            mHandle!!.layoutParams = LayoutParams(
                touchTargetWidth,
                ViewGroup.LayoutParams.MATCH_PARENT,
                GravityCompat.END
            )
            updateHandleColorsAndInset()
            updateBarColorAndInset()
        }

    /**
     * @param hidingEnabled whether hiding is enabled
     */
    var isHidingEnabled: Boolean
        get() = mHidingEnabled
        set(hidingEnabled) {
            mHidingEnabled = hidingEnabled
            if (hidingEnabled) {
                postAutoHide()
            }
        }

    /**
     * @param hideDelay the delay in millis to hide the scrollbar
     */
    var hideDelay: Int

    private var mHidingEnabled: Boolean
    private var mHandleNormalColor: Int
    private var mHandlePressedColor: Int
    private var mBarColor: Int
    private var mTouchTargetWidth: Int
    private var mBarInset = 0
    private var mHideOverride = false
    private var mAdapter: RecyclerView.Adapter<*>? = null
    private val mAdapterObserver: AdapterDataObserver = object : AdapterDataObserver() {
        override fun onChanged() {
            super.onChanged()
            requestLayout()
        }
    }

    init {
        val a = context.obtainStyledAttributes(
            attrs,
            R.styleable.RecyclerFastScroller,
            defStyleAttr,
            defStyleRes
        )
        mBarColor = a.getColor(
            R.styleable.RecyclerFastScroller_rfs_barColor,
            RecyclerFastScrollerUtils.resolveColor(context, android.R.attr.colorControlNormal)
        )
        mHandleNormalColor = a.getColor(
            R.styleable.RecyclerFastScroller_rfs_handleNormalColor,
            RecyclerFastScrollerUtils.resolveColor(context, android.R.attr.colorControlNormal)
        )
        mHandlePressedColor = a.getColor(
            R.styleable.RecyclerFastScroller_rfs_handlePressedColor,
            RecyclerFastScrollerUtils.resolveColor(context, android.R.attr.colorAccent)
        )
        mTouchTargetWidth = a.getDimensionPixelSize(
            R.styleable.RecyclerFastScroller_rfs_touchTargetWidth,
            RecyclerFastScrollerUtils.convertDpToPx(context, 24f)
        )
        hideDelay = a.getInt(
            R.styleable.RecyclerFastScroller_rfs_hideDelay,
            DEFAULT_AUTO_HIDE_DELAY
        )
        mHidingEnabled = a.getBoolean(R.styleable.RecyclerFastScroller_rfs_hidingEnabled, true)
        a.recycle()
        val fortyEightDp = RecyclerFastScrollerUtils.convertDpToPx(context, 48f)
        layoutParams = ViewGroup.LayoutParams(fortyEightDp, ViewGroup.LayoutParams.MATCH_PARENT)
        mBar = View(context)
        mHandle = View(context)
        addView(mBar)
        addView(mHandle)
        touchTargetWidth = mTouchTargetWidth
        mMinScrollHandleHeight = fortyEightDp
        val eightDp = RecyclerFastScrollerUtils.convertDpToPx(getContext(), 8f)
        mHiddenTranslationX =
            (if (RecyclerFastScrollerUtils.isRTL(getContext())) -1 else 1) * eightDp
        mHide = Runnable {
            if (!mHandle.isPressed) {
                if (mAnimator != null && mAnimator!!.isStarted) {
                    mAnimator!!.cancel()
                }
                mAnimator = AnimatorSet()
                val animator2 = ObjectAnimator.ofFloat(
                    this@RecyclerFastScroller,
                    TRANSLATION_X,
                    mHiddenTranslationX.toFloat()
                )
                animator2.interpolator = FastOutLinearInInterpolator()
                animator2.duration = 150
                mHandle.isEnabled = false
                mAnimator!!.play(animator2)
                mAnimator!!.start()
            }
        }

        mHandle.setOnTouchListener(object : OnTouchListener {
            private var mInitialBarHeight = 0f
            private var mLastPressedYAdjustedToInitial = 0f
            private var mLastAppBarLayoutOffset = 0

            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                if (mOnTouchListener != null) {
                    mOnTouchListener!!.onTouch(v, event)
                }
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        mHandle.isPressed = true
                        mRecyclerView!!.stopScroll()
                        var nestedScrollAxis = ViewCompat.SCROLL_AXIS_NONE
                        nestedScrollAxis = nestedScrollAxis or ViewCompat.SCROLL_AXIS_VERTICAL
                        mRecyclerView!!.startNestedScroll(nestedScrollAxis)
                        mInitialBarHeight = mBar.height.toFloat()
                        mLastPressedYAdjustedToInitial = event.y + mHandle.y + mBar.y
                        mLastAppBarLayoutOffset = mAppBarLayoutOffset
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val newHandlePressedY = event.y + mHandle.y + mBar.y
                        val barHeight = mBar.height
                        val newHandlePressedYAdjustedToInitial =
                            newHandlePressedY + (mInitialBarHeight - barHeight)
                        val deltaPressedYFromLastAdjustedToInitial =
                            newHandlePressedYAdjustedToInitial - mLastPressedYAdjustedToInitial
                        val dY = (
                            deltaPressedYFromLastAdjustedToInitial / mInitialBarHeight *
                                mRecyclerView!!.computeVerticalScrollRange()
                            ).toInt()
                        updateRvScroll(dY + mLastAppBarLayoutOffset - mAppBarLayoutOffset)
                        mLastPressedYAdjustedToInitial = newHandlePressedYAdjustedToInitial
                        mLastAppBarLayoutOffset = mAppBarLayoutOffset
                    }
                    MotionEvent.ACTION_UP -> {
                        mLastPressedYAdjustedToInitial = -1f
                        mRecyclerView!!.stopNestedScroll()
                        mHandle.isPressed = false
                        postAutoHide()
                    }
                }
                return true
            }
        })
        translationX = mHiddenTranslationX.toFloat()
    }

    private fun updateHandleColorsAndInset() {
        val drawable = StateListDrawable()
        if (!RecyclerFastScrollerUtils.isRTL(context)) {
            drawable.addState(
                PRESSED_ENABLED_STATE_SET,
                InsetDrawable(ColorDrawable(mHandlePressedColor), mBarInset, 0, 0, 0)
            )
            drawable.addState(
                EMPTY_STATE_SET,
                InsetDrawable(ColorDrawable(mHandleNormalColor), mBarInset, 0, 0, 0)
            )
        } else {
            drawable.addState(
                PRESSED_ENABLED_STATE_SET,
                InsetDrawable(ColorDrawable(mHandlePressedColor), 0, 0, mBarInset, 0)
            )
            drawable.addState(
                EMPTY_STATE_SET,
                InsetDrawable(ColorDrawable(mHandleNormalColor), 0, 0, mBarInset, 0)
            )
        }
        RecyclerFastScrollerUtils.setViewBackground(mHandle, drawable)
    }

    private fun updateBarColorAndInset() {
        val drawable: Drawable = if (!RecyclerFastScrollerUtils.isRTL(context)) {
            InsetDrawable(ColorDrawable(mBarColor), mBarInset, 0, 0, 0)
        } else {
            InsetDrawable(ColorDrawable(mBarColor), 0, 0, mBarInset, 0)
        }
        drawable.alpha = 57
        RecyclerFastScrollerUtils.setViewBackground(mBar, drawable)
    }

    fun attachRecyclerView(recyclerView: RecyclerView) {
        mRecyclerView = recyclerView
        mRecyclerView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                show(true)
            }
        })
        if (recyclerView.adapter != null) attachAdapter(recyclerView.adapter)
    }

    fun attachAdapter(adapter: RecyclerView.Adapter<*>?) {
        if (mAdapter === adapter) return
        if (mAdapter != null) {
            mAdapter!!.unregisterAdapterDataObserver(mAdapterObserver)
        }
        adapter?.registerAdapterDataObserver(mAdapterObserver)
        mAdapter = adapter
    }

    fun setOnHandleTouchListener(listener: OnTouchListener?) {
        mOnTouchListener = listener
    }

    /**
     * Show the fast scroller and hide after delay
     *
     * @param animate whether to animate showing the scroller
     */
    fun show(animate: Boolean) {
        requestLayout()
        post(
            Runnable {
                if (mHideOverride) {
                    return@Runnable
                }
                mHandle!!.isEnabled = true
                if (animate) {
                    if (!mAnimatingIn && translationX != 0f) {
                        if (mAnimator != null && mAnimator!!.isStarted) {
                            mAnimator!!.cancel()
                        }
                        mAnimator = AnimatorSet()
                        val animator =
                            ObjectAnimator.ofFloat(this@RecyclerFastScroller, TRANSLATION_X, 0f)
                        animator.interpolator = LinearOutSlowInInterpolator()
                        animator.duration = 100
                        animator.addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                super.onAnimationEnd(animation)
                                mAnimatingIn = false
                            }
                        })
                        mAnimatingIn = true
                        mAnimator!!.play(animator)
                        mAnimator!!.start()
                    }
                } else {
                    translationX = 0f
                }
                postAutoHide()
            }
        )
    }

    fun postAutoHide() {
        if (mRecyclerView != null && mHidingEnabled) {
            mRecyclerView!!.removeCallbacks(mHide)
            mRecyclerView!!.postDelayed(mHide, hideDelay.toLong())
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (mRecyclerView == null) return
        val scrollOffset = mRecyclerView!!.computeVerticalScrollOffset() + mAppBarLayoutOffset
        val verticalScrollRange = (
            mRecyclerView!!.computeVerticalScrollRange() +
                mRecyclerView!!.paddingBottom
            )
        val barHeight = mBar.height
        val ratio = scrollOffset.toFloat() / (verticalScrollRange - barHeight)
        var calculatedHandleHeight = (barHeight.toFloat() / verticalScrollRange * barHeight).toInt()
        if (calculatedHandleHeight < mMinScrollHandleHeight) {
            calculatedHandleHeight = mMinScrollHandleHeight
        }
        if (calculatedHandleHeight >= barHeight) {
            translationX = mHiddenTranslationX.toFloat()
            mHideOverride = true
            return
        }
        mHideOverride = false
        val y = ratio * (barHeight - calculatedHandleHeight)
        mHandle!!.layout(mHandle.left, y.toInt(), mHandle.right, y.toInt() + calculatedHandleHeight)
    }

    fun updateRvScroll(dY: Int) {
        if (mRecyclerView != null && mHandle != null) {
            try {
                mRecyclerView!!.scrollBy(0, dY)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

    companion object {
        private const val DEFAULT_AUTO_HIDE_DELAY = 1500
    }
}
