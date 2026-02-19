package com.d.locker.lock.utils

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.UserManager
import android.util.Log
import com.d.locker.lock.receivers.MyDeviceAdminReceiver

/**
 * Utility class for managing device restrictions (blocking/unblocking features)
 */
object RestrictionUtils {
    private const val TAG = "RestrictionUtils"

    /**
     * Block or unblock Camera
     */
    fun setCameraDisabled(context: Context, disabled: Boolean) {
        try {
            Log.d(TAG, "→ setCameraDisabled($disabled) called")
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponent = ComponentName(context, MyDeviceAdminReceiver::class.java)

            if (dpm.isDeviceOwnerApp(context.packageName)) {
                dpm.setCameraDisabled(adminComponent, disabled)
                Log.d(TAG, "✅ Camera ${if (disabled) "DISABLED" else "ENABLED"}")
            } else {
                Log.e(TAG, "❌ Cannot change camera policy: App is not Device Owner")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error setting camera policy", e)
        }
    }

    /**
     * Block or unblock Settings (User Restriction)
     * Actually blocks usage of the Settings app by adding a DISALLOW_CONFIG_CREDENTIALS or similar restrictions,
     * but standard approach is setApplicationHidden for the settings package if known, 
     * OR using addUserRestriction for various settings.
     * 
     * Here we will use DISALLOW_ADJUST_VOLUME, DISALLOW_MODIFY_ACCOUNTS, etc. or simply hide the settings app if possible?
     * A more robust way "Block Settings" often means `DISALLOW_OUTGOING_CALLS` or `DISALLOW_CONFIG_WIFI` etc.
     * But usually "Block Settings" implies `DISALLOW_SAFE_BOOT` and others.
     * 
     * To truly "Block Settings app", we can try `setApplicationHidden` for `com.android.settings`.
     */
    fun setSettingsDisabled(context: Context, disabled: Boolean) {
        try {
            Log.d(TAG, "→ setSettingsDisabled($disabled) called")
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponent = ComponentName(context, MyDeviceAdminReceiver::class.java)

            if (dpm.isDeviceOwnerApp(context.packageName)) {
                
                val packagesToHide = listOf(
                    "com.android.settings",
                    "com.android.settings.intelligence",
                    "com.samsung.android.settings",
                    "com.samsung.android.settings.intelligence",
                    "com.sec.android.app.launcher" // Sometimes settings are accessed via launcher shortcuts
                )
                
                for (pkg in packagesToHide) {
                    try {
                        val result = dpm.setApplicationHidden(adminComponent, pkg, disabled)
                        if (result) {
                             Log.d(TAG, "✅ Package $pkg ${if (disabled) "HIDDEN" else "SHOWN"}")
                        } else {
                             Log.e(TAG, "❌ Failed to hide/show package $pkg (setApplicationHidden returned false)")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Exception hiding/showing package $pkg", e)
                    }
                }

                Log.d(TAG, "✅ Package visibility updated")

            } else {
                Log.e(TAG, "❌ Cannot changes settings policy: App is not Device Owner")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error setting settings policy", e)
        }
    }
}
