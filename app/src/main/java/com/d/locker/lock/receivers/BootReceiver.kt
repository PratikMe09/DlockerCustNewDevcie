package com.d.locker.lock.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.d.locker.lock.utils.LockManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("BootReceiver", "Received action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED || 
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON") {
            
            if (LockManager.isDeviceLocked(context)) {
                Log.d("BootReceiver", "Device was locked before reboot. Re-locking...")
                val rawInfo = LockManager.getLockedRetailerInfo(context)
                LockManager.lockDevice(
                    context,
                    rawInfo["company"],
                    rawInfo["name"],
                    rawInfo["mobile"]
                )
            } else {
                Log.d("BootReceiver", "Device was not locked. Doing nothing.")
            }
        }
    }
}
