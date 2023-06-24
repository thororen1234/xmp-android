package org.helllabs.android.xmp.pluscubed.recyclerfastscroll

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt

object RecyclerFastScrollerUtils {

    fun setViewBackground(view: View?, background: Drawable?) {
        view!!.background = background
    }

    fun isRTL(context: Context): Boolean {
        val config = context.resources.configuration
        return config.layoutDirection == View.LAYOUT_DIRECTION_RTL
    }

    @ColorInt
    fun resolveColor(context: Context, @AttrRes color: Int): Int {
        val a = context.obtainStyledAttributes(intArrayOf(color))
        val resId = a.getColor(0, 0)
        a.recycle()
        return resId
    }

    fun convertDpToPx(context: Context, dp: Float): Int {
        return (dp * context.resources.displayMetrics.density + 0.5f).toInt()
    }
}
