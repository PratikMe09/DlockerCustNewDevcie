package com.d.locker.lock.utils

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.d.locker.lock.receivers.MyDeviceAdminReceiver

/**
 * Utility class for handling device reset (factory reset)
 */
object ResetUtils {
    private const val TAG = "ResetUtils"

    /**
     * Perform a factory reset (wipe data)
     * This requires the app to be a Device Owner or Device Admin with wipe data permission.
     */
    fun softReset(context: Context) {
        try {
            Log.d(TAG, "→ softReset() called")
            
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponent = ComponentName(context, MyDeviceAdminReceiver::class.java)

            // Check if we are device owner or admin
            // Check if we are device owner
            if (devicePolicyManager.isDeviceOwnerApp(context.packageName)) {
                Log.w(TAG, "⚠️ INITIATING DEVICE REBOOT...")
                
                try {
                    devicePolicyManager.reboot(adminComponent)
                    Log.d(TAG, "✅ Reboot command sent")
                } catch (e: SecurityException) {
                    Log.e(TAG, "❌ SecurityException during reboot: ${e.message}")
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "❌ Could not reboot (maybe ongoing call): ${e.message}")
                }
            } else {
                Log.e(TAG, "❌ Cannot reboot device: App is not Device Owner")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error performing factory reset", e)
        }
    }
}
