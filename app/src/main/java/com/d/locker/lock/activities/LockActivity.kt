package com.d.locker.lock.activities

import android.app.ActivityManager
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.trustonic.overlaynewdevice.R
import com.d.locker.lock.utils.LockManager

class LockActivity : AppCompatActivity() {

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_UNLOCK) {
                Log.d(TAG, "🔓 Unlock command received")
                try {
                    stopLockTask()
                    Log.d(TAG, "✓ Lock task stopped")
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping lock task: ${e.message}")
                }
                
                Log.d(TAG, "Closing LockActivity...")
                finishAndRemoveTask()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            Log.d(TAG, "Starting LockActivity setup")

            // 1. Configure Window Flags for Lock Screen
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
                val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                keyguardManager.requestDismissKeyguard(this, null)
            } else {
                @Suppress("DEPRECATION")
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                )
            }

            // 2. Fullscreen flags
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            // 3. Set Content View
            setContentView(R.layout.activity_lock)
            Log.d(TAG, "Content view set successfully")

            // 4. Register Receiver
            val filter = IntentFilter(ACTION_UNLOCK)
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(unlockReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(unlockReceiver, filter)
            }
            
            // 5. Start Lock Task (Kiosk Mode)
            try {
                startLockTask()
                Log.d(TAG, "Lock task started")
            } catch (e: Exception) {
                Log.w(TAG, "Could not start lock task: ${e.message}")
            }
            
            // 6. Setup Unlock UI
            setupUnlockUI()

        } catch (e: Exception) {
            Log.e(TAG, "FATAL ERROR in LockActivity onCreate", e)
            // Try to keep running despite error
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(unlockReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered if onCreate crashed
        }
        
        try {
            stopLockTask()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping lock task", e)
        }
    }

    override fun onBackPressed() {
        // Broadly block back button
        Log.d(TAG, "Back button pressed - blocked")
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Try to stay on top if the user tries to leave (Home/Recents)
        if (LockManager.isDeviceLocked(this)) {
            Log.d(TAG, "User leaving hint - bringing back to front")
            val intent = Intent(this, LockActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // Try to bring back to front if paused
        try {
            val activityManager = applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.moveTaskToFront(taskId, 0)
        } catch (e: Exception) {
            Log.e(TAG, "Error moving task to front: ${e.message}")
        }

        // Re-check Lock Task
        if (LockManager.isDeviceLocked(this)) {
            try {
                startLockTask()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start lock task in onResume")
            }
        }
    }

    private fun setupUnlockUI() {
        val retailerInfoContainer = findViewById<android.view.View>(R.id.retailer_info_container)
        val retailerCompanyNameText = findViewById<android.widget.TextView>(R.id.retailer_company_name)
        val retailerFullNameText = findViewById<android.widget.TextView>(R.id.retailer_full_name)
        val retailerMobileText = findViewById<android.widget.TextView>(R.id.retailer_mobile)
        
        val unlockInput = findViewById<android.widget.EditText>(R.id.unlock_pin)
        val unlockButton = findViewById<android.widget.Button>(R.id.unlock_btn)
        val errorText = findViewById<android.widget.TextView>(R.id.error_text)

        // 1. Display Retailer Info
        val companyName = intent.getStringExtra("retailer_company_name")
        val fullName = intent.getStringExtra("retailer_full_name")
        val mobile = intent.getStringExtra("retailer_mobile")

        if (!companyName.isNullOrEmpty() || !fullName.isNullOrEmpty() || !mobile.isNullOrEmpty()) {
            retailerInfoContainer.visibility = android.view.View.VISIBLE
            
            if (!companyName.isNullOrEmpty()) {
                retailerCompanyNameText.text = "Company: $companyName"
                retailerCompanyNameText.visibility = android.view.View.VISIBLE
            } else {
                retailerCompanyNameText.visibility = android.view.View.GONE
            }

            if (!fullName.isNullOrEmpty()) {
                retailerFullNameText.text = "Name: $fullName"
                retailerFullNameText.visibility = android.view.View.VISIBLE
            } else {
                retailerFullNameText.visibility = android.view.View.GONE
            }

            if (!mobile.isNullOrEmpty()) {
                retailerMobileText.text = "Mobile: $mobile"
                retailerMobileText.visibility = android.view.View.VISIBLE
            } else {
                retailerMobileText.visibility = android.view.View.GONE
            }
        } else {
            retailerInfoContainer.visibility = android.view.View.GONE
        }

        // 2. Setup Unlock Logic
        unlockButton.setOnClickListener {
            val enteredCode = unlockInput.text.toString().trim()
            val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            val storedUnlockCode = prefs.getString("unlock_code", "")

            Log.d(TAG, "Unlock attempt. Entered: '$enteredCode', Stored: '$storedUnlockCode'")

            if (enteredCode.isNotEmpty() && enteredCode == storedUnlockCode) {
                // Correct code
                Log.d(TAG, "Correct unlock code entered")
                errorText.visibility = android.view.View.GONE
                
                // Unlock device and remove all restrictions/admin status
                com.d.locker.lock.utils.DevicePolicyUtils.cleanupAllRestrictions(this)
                finishAndRemoveTask()
                
            } else {
                // Incorrect code
                Log.d(TAG, "Incorrect unlock code")
                errorText.visibility = android.view.View.VISIBLE
                errorText.text = "❌ Incorrect Code"
                unlockInput.text.clear()
            }
        }
    }

    companion object {
        private const val TAG = "LockActivity"
        const val ACTION_UNLOCK = "com.trustonic.overlaynewdevice.UNLOCK_DEVICE"
    }
}
