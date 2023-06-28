package org.helllabs.android.xmp.util

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.widget.Toast
import org.helllabs.android.xmp.R

@Deprecated("Replace with Composables")
object Message {

    @JvmStatic
    fun error(context: Context, message: String?) {
        (context as Activity).runOnUiThread {
            val alertDialog = AlertDialog.Builder(context).create()
            alertDialog.setTitle(R.string.error)
            alertDialog.setMessage(message)
            alertDialog.setButton(
                AlertDialog.BUTTON_NEUTRAL,
                context.getString(R.string.dismiss)
            ) { _, _ ->
                //
            }
            alertDialog.show()
        }
    }

    @JvmStatic
    fun error(context: Context, resId: Int) {
        error(context, context.getString(resId))
    }

    @JvmStatic
    fun toast(context: Context?, message: String?) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    @JvmStatic
    fun toast(context: Context, resId: Int) {
        toast(context, context.getString(resId))
    }

    @JvmStatic
    fun yesNoDialog(activity: Activity, title: String?, message: String?, runnable: Runnable) {
        activity.runOnUiThread {
            val listener = DialogInterface.OnClickListener { _, which ->
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    runnable.run()
                }
            }
            val builder = AlertDialog.Builder(activity)
            builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.yes, listener)
                .setNegativeButton(R.string.no, listener)
                .show()
        }
    }
}
