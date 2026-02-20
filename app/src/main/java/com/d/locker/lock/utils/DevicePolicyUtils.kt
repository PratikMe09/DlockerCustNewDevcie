package com.d.locker.lock.utils

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.d.locker.lock.receivers.MyDeviceAdminReceiver
import com.d.locker.lock.utils.UsbManager
import com.d.locker.lock.utils.LockManager

object DevicePolicyUtils {
    private const val TAG = "DevicePolicyUtils"

    /**
     * Removes the device owner status and deactivates the device administrator.
     * This effectively disables the protection and returns control to the user.
     */
    fun removeDeviceOwner(context: Context) {
        try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponent = ComponentName(context, MyDeviceAdminReceiver::class.java)

            Log.d(TAG, "Attempting to remove device owner...")

            // Check if we are the device owner
            if (dpm.isDeviceOwnerApp(context.packageName)) {
                try {
                    // Clear device owner status
                    dpm.clearDeviceOwnerApp(context.packageName)
                    Log.d(TAG, "✅ Device owner status cleared successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to clear device owner status: ${e.message}")
                }
            } else {
                Log.d(TAG, "⚠️ App is not a device owner, skipping clearDeviceOwnerApp")
            }

            // Remove active admin component
            if (dpm.isAdminActive(adminComponent)) {
                try {
                    dpm.removeActiveAdmin(adminComponent)
                    Log.d(TAG, "✅ Active admin component removed successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to remove active admin: ${e.message}")
                }
            } else {
                Log.d(TAG, "⚠️ App is not an active admin, skipping removeActiveAdmin")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Critical error during device owner removal: ${e.message}", e)
        }
    }

    /**
     * Completely cleans up all restrictions, enables USB debugging, 
     * and clears device owner status. This makes the app uninstallable.
     */
    fun cleanupAllRestrictions(context: Context) {
        try {
            Log.d(TAG, "🧹 Starting full cleanup of all restrictions")
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponent = ComponentName(context, MyDeviceAdminReceiver::class.java)

            if (dpm.isDeviceOwnerApp(context.packageName)) {
                // 1. Clear User Restrictions
                dpm.clearUserRestriction(adminComponent, android.os.UserManager.DISALLOW_FACTORY_RESET)
                dpm.clearUserRestriction(adminComponent, android.os.UserManager.DISALLOW_SAFE_BOOT)
                dpm.clearUserRestriction(adminComponent, android.os.UserManager.DISALLOW_DEBUGGING_FEATURES)
                dpm.clearUserRestriction(adminComponent, android.os.UserManager.DISALLOW_USB_FILE_TRANSFER)
                Log.d(TAG, "✅ User restrictions cleared")

                // 2. Enable USB Debugging (optional but requested)
                UsbManager.enableUsbDebugging(context)
            }

            // 3. Reset internal lock state
            LockManager.unlockDevice(context)

            // 4. Clear Device Owner and Admin status
            removeDeviceOwner(context)
            
            Log.d(TAG, "✨ Full cleanup complete. App is now a normal app.")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error during full cleanup", e)
        }
    }
}
