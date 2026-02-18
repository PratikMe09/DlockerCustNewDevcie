package com.d.locker.lock.utils

import android.util.Log
import com.d.locker.lock.utils.LocationUtils
import com.d.locker.lock.utils.SimUtils
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

object NetworkManager {
    private const val TAG = "NetworkManager"
    
    // TODO: Replace with your actual backend URL. For example: "https://your-server.com/api/device/status"
    private const val API_URL = "https://your-api-endpoint.com/api/devices/data" 
    
    private val client = OkHttpClient()
    private val gson = Gson()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    /**
     * Generic method to send any data payload to the backend.
     */
    fun sendRawData(data: Map<String, Any>) {
        try {
            val jsonData = gson.toJson(data)
            Log.d(TAG, "📤 Sending raw data to backend: $jsonData")

            val body = jsonData.toRequestBody(JSON)
            val request = Request.Builder()
                .url(API_URL)
                .post(body)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "❌ API Call Failed", e)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!response.isSuccessful) {
                            Log.e(TAG, "❌ API Error: ${response.code} - ${response.message}")
                        } else {
                            Log.d(TAG, "✅ API Success: ${response.body?.string()}")
                        }
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error preparing API call", e)
        }
    }

    /**
     * Fetches SIM details and sends them to the backend.
     */
    fun sendSimDetails(context: android.content.Context) {
        Log.d(TAG, "📱 Preparing to send SIM details...")
        val simDetails = SimUtils.getFormattedSimDetails(context)
        
        val payload = mapOf(
            "type" to "SIM_DETAILS_UPDATE",
            "timestamp" to System.currentTimeMillis(),
            "simData" to simDetails
        )
        
        sendRawData(payload)
    }
}
