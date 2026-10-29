package com.d.locker.lock.utils

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.UserManager
import android.util.Log
import com.d.locker.lock.receivers.MyDeviceAdminReceiver

object UsbManager {
    private const val TAG = "UsbManager"

    /**
     * Disables USB debugging and file transfer.
     */
    fun disableUsbDebugging(context: Context) {
        try {
            Log.d(TAG, "→ disableUsbDebugging() called")
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponent = ComponentName(context, MyDeviceAdminReceiver::class.java)

            if (dpm.isDeviceOwnerApp(context.packageName)) {
                // Block ADB (Debugging)
                dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_DEBUGGING_FEATURES)
                // Block USB File Transfer (often required to fully block ADB/connection)
                dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_USB_FILE_TRANSFER)
                Log.d(TAG, "🔒 USB Debugging & File Transfer DISABLED")
            } else {
                Log.e(TAG, "❌ App is not Device Owner, cannot manage USB restrictions")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in disableUsbDebugging", e)
        }
    }

    /**
     * Enables USB debugging and file transfer.
     */
    fun enableUsbDebugging(context: Context) {
        try {
            Log.d(TAG, "→ enableUsbDebugging() called")
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponent = ComponentName(context, MyDeviceAdminReceiver::class.java)

            if (dpm.isDeviceOwnerApp(context.packageName)) {
                dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_DEBUGGING_FEATURES)
                dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_USB_FILE_TRANSFER)
                Log.d(TAG, "🔓 USB Debugging & File Transfer ENABLED")
            } else {
                Log.e(TAG, "❌ App is not Device Owner, cannot manage USB restrictions")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in enableUsbDebugging", e)
        }
    }
}
