package com.d.locker.lock.utils

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.UserManager
import android.util.Log
import com.d.locker.lock.receivers.MyDeviceAdminReceiver

object AppManager {
    private const val TAG = "AppManager"

    /**
     * Controls whether apps can be installed or uninstalled.
     * @param disabled If true, blocks installation and uninstallation.
     */
    /**
     * Blocks app installation and uninstallation.
     */
    fun disallowInstallation(context: Context) {
        try {
            Log.d(TAG, "→ disallowInstallation() called")
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponent = ComponentName(context, MyDeviceAdminReceiver::class.java)

            if (dpm.isDeviceOwnerApp(context.packageName)) {
                // Block installation of new apps
                dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_INSTALL_APPS)
                // Also block uninstallation of existing apps
                dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_UNINSTALL_APPS)
                Log.d(TAG, "🔒 App Installation/Uninstallation DISABLED")
            } else {
                Log.e(TAG, "❌ App is not Device Owner, cannot manage app restrictions")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in disallowInstallation", e)
        }
    }

    /**
     * Allows app installation and uninstallation.
     */
    fun allowInstallation(context: Context) {
        try {
            Log.d(TAG, "→ allowInstallation() called")
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponent = ComponentName(context, MyDeviceAdminReceiver::class.java)

            if (dpm.isDeviceOwnerApp(context.packageName)) {
                dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_INSTALL_APPS)
                dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_UNINSTALL_APPS)
                Log.d(TAG, "🔓 App Installation/Uninstallation ENABLED")
            } else {
                Log.e(TAG, "❌ App is not Device Owner, cannot manage app restrictions")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in allowInstallation", e)
        }
    }
    /**
     * Blocks specific applications by package name.
     * @param packageNames Comma-separated list of package names.
     */
    fun blockApps(context: Context, packageNames: String?) {
        if (packageNames.isNullOrEmpty()) return
        
        try {
            Log.d(TAG, "→ blockApps($packageNames) called")
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponent = ComponentName(context, MyDeviceAdminReceiver::class.java)

            if (dpm.isDeviceOwnerApp(context.packageName)) {
                val packages = packageNames.split(",").map { it.trim() }.toTypedArray()
                
                // Block apps using the standard DevicePolicyManager method
                val result = dpm.setPackagesSuspended(adminComponent, packages, true)
                Log.d(TAG, "✅ Apps BLOCKED: ${result.joinToString(", ")}")
                
                // Show a toast immediately to confirm blocking
                android.widget.Toast.makeText(context, "Apps have been disabled by retailer", android.widget.Toast.LENGTH_LONG).show()
                
            } else {
                Log.e(TAG, "❌ App is not Device Owner, cannot block apps")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in blockApps", e)
        }
    }

    /**
     * Unblocks specific applications by package name.
     * @param packageNames Comma-separated list of package names.
     */
    fun unblockApps(context: Context, packageNames: String?) {
        if (packageNames.isNullOrEmpty()) return

        try {
            Log.d(TAG, "→ unblockApps($packageNames) called")
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponent = ComponentName(context, MyDeviceAdminReceiver::class.java)

            if (dpm.isDeviceOwnerApp(context.packageName)) {
                val packages = packageNames.split(",").map { it.trim() }.toTypedArray()
                val result = dpm.setPackagesSuspended(adminComponent, packages, false)
                Log.d(TAG, "🔓 Apps UNBLOCKED: ${result.joinToString(", ")}")
                
                android.widget.Toast.makeText(context, "Apps have been enabled", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                Log.e(TAG, "❌ App is not Device Owner, cannot unblock apps")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in unblockApps", e)
        }
    }
}
