package com.d.locker.lock.services

import android.content.Intent
import android.util.Log
import com.d.locker.lock.activities.LockActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "╔════════════════════════════════════════════════════════")
        Log.d(TAG, "║ FIREBASE MESSAGE RECEIVED")
        Log.d(TAG, "╠════════════════════════════════════════════════════════")
        Log.d(TAG, "║ Message: ${remoteMessage}")
        Log.d(TAG, "║ From: ${remoteMessage.from}")
        Log.d(TAG, "║ Message ID: ${remoteMessage.messageId}")
        Log.d(TAG, "║ Sent Time: ${remoteMessage.sentTime}")
        
        // Log notification payload (if present)
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "╠════════════════════════════════════════════════════════")
            Log.d(TAG, "║ NOTIFICATION PAYLOAD:")
            Log.d(TAG, "║   Title: ${notification.title}")
            Log.d(TAG, "║   Body: ${notification.body}")
            Log.d(TAG, "║   Click Action: ${notification.clickAction}")
        }

        // Log data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "╠════════════════════════════════════════════════════════")
            Log.d(TAG, "║ DATA PAYLOAD:")
            remoteMessage.data.forEach { (key, value) ->
                Log.d(TAG, "║   $key = $value")
            }
            Log.d(TAG, "╚════════════════════════════════════════════════════════")

            // Process commands
            val command = remoteMessage.data["command"]
            
            if (command == "LOCK") {
                Log.d(TAG, "🔒 Command: LOCK -> Activating Overlay")
                startLockActivity()
            } else if (command == "UNLOCK") {
                Log.d(TAG, "🔓 Command: UNLOCK -> Removing Overlay")
                unlockDevice()
            } else {
                Log.d(TAG, "⚠️ Unknown or missing command: $command")
            }
        } else {
            Log.d(TAG, "║ No data payload")
            Log.d(TAG, "╚════════════════════════════════════════════════════════")
        }
    }

    private fun startLockActivity() {
        try {
            Log.d(TAG, "→ startLockActivity() called")
            val intent = Intent(this, LockActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            Log.d(TAG, "→ Starting LockActivity with intent: $intent")
            startActivity(intent)
            Log.d(TAG, "✅ LockActivity started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ ERROR starting LockActivity", e)
            Log.e(TAG, "  Error message: ${e.message}")
            Log.e(TAG, "  Error type: ${e.javaClass.simpleName}")
        }
    }

    private fun unlockDevice() {
        Log.d(TAG, "→ unlockDevice() called")
        try {
            val intent = Intent(LockActivity.ACTION_UNLOCK)
            intent.setPackage(packageName) // Explicitly target this app
            Log.d(TAG, "→ Sending broadcast: ${intent.action} to package: $packageName")
            sendBroadcast(intent)
            Log.d(TAG, "✅ Unlock broadcast sent")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending unlock broadcast", e)
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(token)
    }
    
    private fun sendRegistrationToServer(token: String?) {
        // Implement this method to send token to your app server.
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
}
