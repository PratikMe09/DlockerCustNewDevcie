package com.d.locker.lock.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import com.d.locker.lock.activities.LockActivity

object LockManager {
    private const val TAG = "LockManager"

    private const val PREFS_NAME = "LockStatePrefs"
    private const val KEY_IS_LOCKED = "is_locked"
    private const val KEY_COMPANY_NAME = "locked_company_name"
    private const val KEY_FULL_NAME = "locked_full_name"
    private const val KEY_MOBILE = "locked_mobile"

    fun lockDevice(context: Context, retailerCompanyName: String? = null, retailerFullName: String? = null, retailerMobile: String? = null) {
        try {
            Log.d(TAG, "→ lockDevice() called")
            
            // Save state
            saveLockState(context, true, retailerCompanyName, retailerFullName, retailerMobile)
            
            val intent = Intent(context, LockActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or 
                          Intent.FLAG_ACTIVITY_CLEAR_TOP or 
                          Intent.FLAG_ACTIVITY_SINGLE_TOP or
                          Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            
            // Pass retailer details
            retailerCompanyName?.let { intent.putExtra("retailer_company_name", it) }
            retailerFullName?.let { intent.putExtra("retailer_full_name", it) }
            retailerMobile?.let { intent.putExtra("retailer_mobile", it) }
            
            Log.d(TAG, "→ Starting LockActivity")
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "❌ ERROR starting LockActivity", e)
        }
    }

    fun unlockDevice(context: Context) {
        Log.d(TAG, "→ unlockDevice() called")
        saveLockState(context, false)
        try {
            val intent = Intent(LockActivity.ACTION_UNLOCK)
            intent.setPackage(context.packageName)
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending unlock broadcast", e)
        }
    }

    private fun saveLockState(context: Context, locked: Boolean, company: String? = null, name: String? = null, mobile: String? = null) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean(KEY_IS_LOCKED, locked)
            putString(KEY_COMPANY_NAME, company)
            putString(KEY_FULL_NAME, name)
            putString(KEY_MOBILE, mobile)
            apply()
        }
    }

    fun isDeviceLocked(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_IS_LOCKED, false)
    }

    fun getLockedRetailerInfo(context: Context): Map<String, String?> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return mapOf(
            "company" to prefs.getString(KEY_COMPANY_NAME, null),
            "name" to prefs.getString(KEY_FULL_NAME, null),
            "mobile" to prefs.getString(KEY_MOBILE, null)
        )
    }
}
