package com.d.locker.lock.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log

object SimUtils {
    private const val TAG = "SimUtils"

    @SuppressLint("MissingPermission", "HardwareIds") // Be careful with hardware IDs
    fun getFormattedSimDetails(context: Context): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        val simDetailsList = mutableListOf<Map<String, String>>()
        var isSimInserted = false

        try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val activeSubscriptionInfoList = subscriptionManager.activeSubscriptionInfoList

            if (activeSubscriptionInfoList != null && activeSubscriptionInfoList.isNotEmpty()) {
                isSimInserted = true
                for (subscriptionInfo in activeSubscriptionInfoList) {
                    val details = mutableMapOf<String, String>()
                    
                    details["slotIndex"] = subscriptionInfo.simSlotIndex.toString()
                    details["displayName"] = subscriptionInfo.displayName.toString()
                    details["carrierName"] = subscriptionInfo.carrierName.toString()
                    details["countryIso"] = subscriptionInfo.countryIso ?: "Unknown"
                    
                    // Phone Number (might happen to be empty if carrier doesn't provide it)
                    val number = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                         try {
                              subscriptionManager.getPhoneNumber(subscriptionInfo.subscriptionId)
                         } catch (e: Exception) {
                              subscriptionInfo.number
                         }
                    } else {
                         subscriptionInfo.number
                    }
                    details["number"] = if (number.isNullOrEmpty()) "Unknown" else number
                    
                    details["iccId"] = subscriptionInfo.iccId ?: "Unknown" // ICCID is unique SIM ID

                    simDetailsList.add(details)
                    Log.d(TAG, "📱 SIM Found (Slot ${details["slotIndex"]}): ${details["carrierName"]} - Num: ${details["number"]} - Country: ${details["countryIso"]}")
                }
            } else {
                 Log.w(TAG, "⚠️ No active SIM subscriptions found via SubscriptionManager")
                 // Fallback to TelephonyManager - still might mean no SIM if internal logic returns specific state
                 val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                 if (telephonyManager.simState == TelephonyManager.SIM_STATE_READY) {
                     isSimInserted = true
                     val details = mutableMapOf<String, String>()
                     details["carrierName"] = telephonyManager.simOperatorName ?: "Unknown"
                     details["countryIso"] = telephonyManager.simCountryIso ?: "Unknown"
                     details["number"] = telephonyManager.line1Number ?: "Unknown"
                     simDetailsList.add(details)
                     Log.d(TAG, "📱 SIM Details (Fallback): ${details["carrierName"]} - ${details["number"]}")
                 }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get SIM details", e)
        }

        result["isSimInserted"] = isSimInserted
        result["simSlots"] = simDetailsList
        return result
    }
}
