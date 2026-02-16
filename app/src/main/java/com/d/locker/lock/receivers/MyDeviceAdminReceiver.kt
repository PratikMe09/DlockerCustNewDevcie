package com.d.locker.lock.receivers

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

/**
 * Device Admin Receiver for Main App
 * 
 * PURPOSE:
 * Receives callbacks when this app becomes/loses Device Owner status
 * This is where you can initialize your device management policies
 * 
 * FULLY QUALIFIED NAME:
 * com.d.locker.lock.receivers.MyDeviceAdminReceiver
 * This MUST match TARGET_ADMIN_RECEIVER in Launcher App
 */
class MyDeviceAdminReceiver : DeviceAdminReceiver() {
    
    companion object {
        const val TAG = "MainAppDeviceAdmin"
    }

    /**
     * Called when this app is enabled as device admin
     */
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d(TAG, "✅ Device Admin Enabled for Main App")
        Toast.makeText(context, "Main App is now Device Admin", Toast.LENGTH_LONG).show()
    }

    /**
     * Called when device owner is transferred TO this app FROM another app
     * This is called AFTER the Launcher App transfers ownership
     * 
     * This is the perfect place to:
     * - Set up device policies
     * - Configure lock task mode
     * - Set user restrictions
     * - Initialize your device management features
     */
    override fun onTransferOwnershipComplete(context: Context, bundle: android.os.PersistableBundle?) {
        super.onTransferOwnershipComplete(context, bundle)
        Log.d(TAG, "✅ ✅ ✅ Device Owner Transfer Complete! Main App is now Device Owner!")
        Toast.makeText(context, "Main App is now Device Owner!", Toast.LENGTH_LONG).show()
        
        // Initialize your device owner policies here
        try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponent = ComponentName(context, MyDeviceAdminReceiver::class.java)
            
            // Verify we are indeed Device Owner
            if (dpm.isDeviceOwnerApp(context.packageName)) {
                Log.d(TAG, "✅ Verified: Main App is Device Owner")
                
                // Example: Set lock task packages (kiosk mode)
                // dpm.setLockTaskPackages(adminComponent, arrayOf(context.packageName))
                
                // Example: Set user restrictions
                // dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_FACTORY_RESET)
                
                // Add your custom initialization here
                initializeDeviceOwnerPolicies(context, dpm, adminComponent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Device Owner policies", e)
        }
    }

    /**
     * Initialize your custom Device Owner policies
     * Called automatically after transfer completes
     */
    private fun initializeDeviceOwnerPolicies(
        context: Context,
        dpm: DevicePolicyManager,
        adminComponent: ComponentName
    ) {
        Log.d(TAG, "Initializing Device Owner policies...")
        
        // TODO: Add your device management logic here
        // Examples:
        // - Lock the device to kiosk mode
        // - Disable certain system features
        // - Set password policies
        // - Configure network restrictions
        // - Set up app whitelist/blacklist
        
        Log.d(TAG, "✅ Device Owner policies initialized")
    }

    /**
     * Called when device admin is disabled
     */
    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d(TAG, "❌ Device Admin Disabled for Main App")
        Toast.makeText(context, "Main App is no longer Device Admin", Toast.LENGTH_LONG).show()
    }
}