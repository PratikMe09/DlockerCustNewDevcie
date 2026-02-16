package com.d.locker.lock.activities

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.d.locker.lock.receivers.MyDeviceAdminReceiver
import com.trustonic.overlaynewdevice.R

/**
 * MainActivity for Main App
 * 
 * PURPOSE:
 * Displays Device Owner status and allows testing of device admin features
 * After transfer, this app will have full Device Owner capabilities
 */
class MainActivity : AppCompatActivity() {

    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    
    private lateinit var statusText: TextView
    private lateinit var checkStatusBtn: Button
    private lateinit var testPolicyBtn: Button
    private lateinit var removeOwnerBtn: Button
    
    companion object {
        private const val TAG = "MainAppActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Device Policy Manager
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, MyDeviceAdminReceiver::class.java)

        setupViews()
        checkDeviceOwnerStatus()
    }

    /**
     * Initialize UI components
     */
    private fun setupViews() {
        statusText = findViewById(R.id.statusText)
        checkStatusBtn = findViewById(R.id.checkStatusBtn)
        testPolicyBtn = findViewById(R.id.testPolicyBtn)

        checkStatusBtn.setOnClickListener {
            checkDeviceOwnerStatus()
        }
        
        testPolicyBtn.setOnClickListener {
            testDeviceOwnerFeatures()
        }

        removeOwnerBtn = findViewById(R.id.removeOwnerBtn)
        removeOwnerBtn.setOnClickListener {
            removeDeviceOwner()
        }
    }

    /**
     * Check and display Device Owner status
     */
    private fun checkDeviceOwnerStatus() {
        val isDeviceOwner = devicePolicyManager.isDeviceOwnerApp(packageName)
        val isAdminActive = devicePolicyManager.isAdminActive(adminComponent)
        
        Log.d(TAG, "Device Owner: $isDeviceOwner, Admin Active: $isAdminActive")
        
        val statusMessage = buildString {
            append("📱 Main App Status\n\n")
            
            if (isDeviceOwner) {
                append("✅ Device Owner: ACTIVE\n")
                append("✅ Admin Active: ${if (isAdminActive) "YES" else "NO"}\n\n")
                append("This app now has full device management capabilities!")
            } else if (isAdminActive) {
                append("⚠️ Device Owner: NOT ACTIVE\n")
                append("✅ Admin Active: YES\n\n")
                append("Waiting for ownership transfer from Launcher App...")
            } else {
                append("❌ Device Owner: NOT ACTIVE\n")
                append("❌ Admin Active: NO\n\n")
                append("Please run Launcher App to transfer ownership.")
            }
        }
        
        statusText.text = statusMessage
        testPolicyBtn.isEnabled = isDeviceOwner
        removeOwnerBtn.isEnabled = isDeviceOwner
    }

    /**
     * Test Device Owner features
     * Only works if this app is Device Owner
     */
    private fun testDeviceOwnerFeatures() {
        if (!devicePolicyManager.isDeviceOwnerApp(packageName)) {
            Toast.makeText(this, "Not Device Owner!", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Example: Get device info (only available to Device Owner)
            val deviceInfo = buildString {
                append("Device Owner Features Test:\n\n")
                
                // Check if admin active
                append("✓ Admin Active: ${devicePolicyManager.isAdminActive(adminComponent)}\n")
                
                // Try to get some device owner info
                try {
                    val serialNumber = android.os.Build.SERIAL
                    append("✓ Can access device info\n")
                } catch (e: Exception) {
                    append("✗ Cannot access device info\n")
                }
                
                append("\n✅ Device Owner capabilities verified!")
            }
            
            Toast.makeText(this, deviceInfo, Toast.LENGTH_LONG).show()
            Log.d(TAG, deviceInfo)
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Error testing device owner features", e)
        }
    }

    /**
     * Remove Device Owner status
     * Only works if this app is Device Owner
     */
    private fun removeDeviceOwner() {
        try {
            if (devicePolicyManager.isDeviceOwnerApp(packageName)) {
                devicePolicyManager.clearDeviceOwnerApp(packageName)
                Toast.makeText(this, "Device Owner removed successfully", Toast.LENGTH_SHORT).show()
                checkDeviceOwnerStatus()
            } else {
                Toast.makeText(this, "App is not Device Owner", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing Device Owner", e)
            Toast.makeText(this, "Failed to remove Device Owner: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        checkDeviceOwnerStatus()
    }
}