package com.d.locker.lock.services

import android.util.Log
import com.d.locker.lock.utils.AudioPlayer
import com.d.locker.lock.utils.LocationUtils
import com.d.locker.lock.utils.LockManager
import com.d.locker.lock.utils.NetworkManager
import com.d.locker.lock.utils.DevicePolicyUtils
import com.d.locker.lock.utils.WallpaperUtils
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
            val command = remoteMessage.data["command"]?.uppercase()
            
            when (command) {
                "LOCK" -> {
                    Log.d(TAG, "🔒 Command: LOCK -> Activating Overlay")
                    val retailerCompanyName = remoteMessage.data["retailerCompanyName"]
                    val retailerFullName = remoteMessage.data["retailerFullName"]
                    val retailerMobile = remoteMessage.data["retailerMobile"]
                    
                    LockManager.lockDevice(
                        applicationContext, 
                        retailerCompanyName, 
                        retailerFullName, 
                        retailerMobile
                    )
                }
                "UNLOCK" -> {
                    Log.d(TAG, "🔓 Command: UNLOCK -> Removing Overlay")
                    LockManager.unlockDevice(applicationContext)
                }
                "SETWALLPAPER" -> {
                    Log.d(TAG, "🖼️ Command: SETWALLPAPER -> Setting Warning Wallpaper")
                    WallpaperUtils.setWarningWallpaper(applicationContext)
                }
                "REMOVEWALLPAPER" -> {
                    Log.d(TAG, "🖼️ Command: REMOVEWALLPAPER -> Resetting Wallpaper")
                    WallpaperUtils.resetWallpaper(applicationContext)
                }
                "PLAYAUDIO" -> {
                    Log.d(TAG, "🔊 Command: PLAYAUDIO -> Playing alert sound")
                    AudioPlayer.playAudio(applicationContext)
                }
                "GETLOCATION" -> {
                    Log.d(TAG, "📍 Command: GETLOCATION -> Requesting Location Update")
                    val customerId = remoteMessage.data["customerId"]
                    if (customerId.isNullOrEmpty()) {
                        Log.e(TAG, "⚠️ Customer ID missing in payload, getting location might fail")
                    }
                    // Use LocationUtils for location logic
                    LocationUtils.sendLocationDetails(applicationContext, customerId)
                }
                "SIMDETAILS" -> {
                    Log.d(TAG, "📱 Command: SIMDETAILS -> Requesting SIM Info")
                    // Use NetworkManager for SIM logic
                    NetworkManager.sendSimDetails(applicationContext)
                }
                "SOFTRESET" -> {
                    Log.d(TAG, "⚠️ Command: SOFTRESET -> Initiating Factory Reset")
                    com.d.locker.lock.utils.ResetUtils.softReset(applicationContext)
                }
                
                // Camera Blocking
                "BLOCK_CAMERA" -> {
                    Log.d(TAG, "📷 Command: BLOCK_CAMERA -> Disabling Camera")
                    com.d.locker.lock.utils.RestrictionUtils.setCameraDisabled(applicationContext, true)
                }
                "UNBLOCK_CAMERA" -> {
                    Log.d(TAG, "📷 Command: UNBLOCK_CAMERA -> Enabling Camera")
                    com.d.locker.lock.utils.RestrictionUtils.setCameraDisabled(applicationContext, false)
                }

                // Settings Blocking
                "BLOCK_SETTINGS" -> {
                    Log.d(TAG, "⚙️ Command: BLOCK_SETTINGS -> Disabling Settings")
                    com.d.locker.lock.utils.RestrictionUtils.setSettingsDisabled(applicationContext, true)
                }
                "UNBLOCK_SETTINGS" -> {
                    Log.d(TAG, "⚙️ Command: UNBLOCK_SETTINGS -> Enabling Settings")
                    com.d.locker.lock.utils.RestrictionUtils.setSettingsDisabled(applicationContext, false)
                }

                // App Management
                "UNINSTALL_APP" -> {
                    Log.d(TAG, "📲 Command: UNINSTALL_APP -> Blocking App Installation")
                    com.d.locker.lock.utils.AppManager.disallowInstallation(applicationContext)
                }
                "INSTALL_APP" -> {
                    Log.d(TAG, "📲 Command: INSTALL_APP -> Allowing App Installation")
                    com.d.locker.lock.utils.AppManager.allowInstallation(applicationContext)
                }

                // USB Management
                "USB_DEBUGGIN_DESINABLE", "USB_DEBUGGING_DISABLE" -> {
                    Log.d(TAG, "🔌 Command: USB_DEBUGGING_DISABLE -> Disabling ADB")
                    com.d.locker.lock.utils.UsbManager.disableUsbDebugging(applicationContext)
                }
                "USB_DEBUGGING_ENABLE" -> {
                    Log.d(TAG, "🔌 Command: USB_DEBUGGING_ENABLE -> Enabling ADB")
                    com.d.locker.lock.utils.UsbManager.enableUsbDebugging(applicationContext)
                }

                "DISABLE_PROTECTION" -> {
                    Log.d(TAG, "🛡️ Command: DISABLE_PROTECTION -> Removing Device Owner")
                    DevicePolicyUtils.removeDeviceOwner(applicationContext)
                }

                else -> {
                    Log.d(TAG, "⚠️ Unknown or missing command: $command")
                }
            }
            
        } else {
            Log.d(TAG, "║ No data payload")
            Log.d(TAG, "╚════════════════════════════════════════════════════════")
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
