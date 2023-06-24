package org.helllabs.android.xmp.util

import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.view.LayoutInflater
import androidx.core.content.pm.PackageInfoCompat
import androidx.preference.PreferenceManager
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.preferences.Preferences

class ChangeLog(private val context: Context) {

    fun show(): Int = try {
        val packageInfo = with(context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val flags = PackageManager.PackageInfoFlags.of(0)
                packageManager.getPackageInfo(packageName, flags)
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
        }
        val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val lastViewed = prefs.getInt(Preferences.CHANGELOG_VERSION, 0)
        if (lastViewed < versionCode) {
            val editor = prefs.edit()
            editor.putInt(Preferences.CHANGELOG_VERSION, versionCode.toInt())
            editor.apply()
            showLog()
            0
        } else {
            -1
        }
    } catch (e: PackageManager.NameNotFoundException) {
        Log.w(TAG, "Unable to get version code")
        -1
    }

    private fun showLog() {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.changelog, null)
        AlertDialog.Builder(context)
            .setTitle("Changelog")
            .setIcon(android.R.drawable.ic_menu_info_details)
            .setView(view)
            .setNegativeButton("Dismiss") { _, _ ->
                // Do nothing
            }.show()
    }

    companion object {
        private const val TAG = "ChangeLog"
    }
}
