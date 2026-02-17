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
        // Disable back button
    }

    override fun onPause() {
        super.onPause()
        // Try to bring back to front if paused
        try {
            val activityManager = applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.moveTaskToFront(taskId, 0)
        } catch (e: Exception) {
            Log.e(TAG, "Error moving task to front: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "LockActivity"
        const val ACTION_UNLOCK = "com.trustonic.overlaynewdevice.UNLOCK_DEVICE"
    }
}
