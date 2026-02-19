package com.d.locker.lock.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import com.d.locker.lock.activities.LockActivity

object LockManager {
    private const val TAG = "LockManager"

    fun lockDevice(context: Context, retailerCompanyName: String? = null, retailerFullName: String? = null, retailerMobile: String? = null) {
        try {
            Log.d(TAG, "→ lockDevice() called")
            val intent = Intent(context, LockActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            
            // Pass retailer details if available
            retailerCompanyName?.let { intent.putExtra("retailer_company_name", it) }
            retailerFullName?.let { intent.putExtra("retailer_full_name", it) }
            retailerMobile?.let { intent.putExtra("retailer_mobile", it) }
            
            Log.d(TAG, "→ Starting LockActivity with intent: $intent")
            context.startActivity(intent)
            Log.d(TAG, "✅ LockActivity started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ ERROR starting LockActivity", e)
            Log.e(TAG, "  Error message: ${e.message}")
            Log.e(TAG, "  Error type: ${e.javaClass.simpleName}")
        }
    }

    fun unlockDevice(context: Context) {
        Log.d(TAG, "→ unlockDevice() called")
        try {
            val intent = Intent(LockActivity.ACTION_UNLOCK)
            intent.setPackage(context.packageName) // Explicitly target this app
            Log.d(TAG, "→ Sending broadcast: ${intent.action} to package: ${context.packageName}")
            context.sendBroadcast(intent)
            Log.d(TAG, "✅ Unlock broadcast sent")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending unlock broadcast", e)
        }
    }
}
