package com.d.locker.lock.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

object LocationUtils {

    private const val TAG = "LocationUtils"
    private const val BASE_LOCATION_API_URL =
        "https://dlockerbackend.idea2reality.tech/v1/private/customers"
    private const val LOCATION_TIMEOUT_MS = 30_000L  // 30 seconds

    private val client = OkHttpClient()
    private val gson = Gson()
    private val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()

    // ─────────────────────────────────────────────────────────────
    // MAIN ENTRY
    // ─────────────────────────────────────────────────────────────

    fun sendLocationDetails(context: Context, customerId: String?) {
        Log.d(TAG, "📍 GETLOCATION received — Customer ID: $customerId")

        // Step 1: First check is location enabled if not then enable location
        if (!isLocationEnabled(context)) {
            Log.w(TAG, "⚠️ Location is OFF — enabling via Device Owner...")

            enableLocationViaDeviceOwner(context)

            // Wait for GPS warm up
            Log.d(TAG, "⏳ GPS warm up ka wait (5s)...")
            Thread.sleep(5000)

            if (!isLocationEnabled(context)) {
                Log.e(TAG, "❌ Location enable nahi hui")
                return
            }

            Log.d(TAG, "✅ Location ON ho gayi — ab fetch karte hain")
        }

        // Step 2: Grant permissions via Device Owner
        ensureLocationPermission(context)

        // Step 3: Verify permission
        if (!hasLocationPermission(context)) {
            Log.e(TAG, "❌ Location permission denied")
            return
        }

        // Step 4: Fetch and send location (call api with pass lat long)
        fetchLocation(context) { location ->
            if (location != null) {
                Log.d(TAG, "✅ Got location: Lat=${location.latitude}, Lon=${location.longitude}")
                sendLocationToApi(customerId, location)
            } else {
                Log.e(TAG, "❌ Location fetch timed out")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // DEVICE OWNER — ComponentName
    // ─────────────────────────────────────────────────────────────

    private fun getAdminComponent(context: Context): ComponentName {
        return ComponentName(
            "com.trustonic.overlaynewdevice",
            "com.d.locker.lock.receivers.MyDeviceAdminReceiver"
        )
    }

    // ─────────────────────────────────────────────────────────────
    // DEVICE OWNER — Enable Location
    // ─────────────────────────────────────────────────────────────

    private fun enableLocationViaDeviceOwner(context: Context) {
        try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponent = getAdminComponent(context)

            if (!dpm.isDeviceOwnerApp("com.trustonic.overlaynewdevice")) {
                Log.e(TAG, "❌ Not device owner")
                return
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                // Android 12+
                dpm.setLocationEnabled(adminComponent, true)
                Log.d(TAG, "✅ setLocationEnabled(true) called")
            } else {
                // Android 11 and below
                dpm.setSecureSetting(
                    adminComponent,
                    android.provider.Settings.Secure.LOCATION_MODE,
                    android.provider.Settings.Secure.LOCATION_MODE_HIGH_ACCURACY.toString()
                )
                Log.d(TAG, "✅ setSecureSetting LOCATION_MODE called")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ enableLocationViaDeviceOwner failed: ${e.message}", e)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // DEVICE OWNER — Grant Location Permissions
    // ─────────────────────────────────────────────────────────────

    private fun ensureLocationPermission(context: Context) {
        try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponent = getAdminComponent(context)
            val targetPackage = "com.trustonic.overlaynewdevice"

            if (!dpm.isDeviceOwnerApp(targetPackage)) {
                Log.e(TAG, "❌ Not Device Owner")
                return
            }

            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ).forEach { permission ->
                val result = dpm.setPermissionGrantState(
                    adminComponent,
                    targetPackage,
                    permission,
                    DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED
                )
                Log.d(TAG, "📋 Grant [$permission] → $result")
            }

            Log.d(TAG, "✅ All location permissions granted")

        } catch (e: Exception) {
            Log.e(TAG, "❌ ensureLocationPermission failed", e)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Permission & Provider Checks
    // ─────────────────────────────────────────────────────────────

    private fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isLocationEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    // ─────────────────────────────────────────────────────────────
    // Fetch Location — Fused (GPS + Network + WiFi combined)
    // ─────────────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    private fun fetchLocation(context: Context, callback: (Location?) -> Unit) {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val received = AtomicBoolean(false)
        val handler = Handler(Looper.getMainLooper())

        // Timeout
        handler.postDelayed({
            if (received.compareAndSet(false, true)) {
                Log.e(TAG, "❌ Fused timeout after ${LOCATION_TIMEOUT_MS / 1000}s")
                callback(null)
            }
        }, LOCATION_TIMEOUT_MS)

        // Step 1: Last known location try karo (instant)
        fusedClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null && !received.get()) {
                    if (received.compareAndSet(false, true)) {
                        handler.removeCallbacksAndMessages(null)
                        Log.d(TAG, "📍 Fused last location: ${location.latitude}, ${location.longitude}")
                        callback(location)
                    }
                } else {
                    // Step 2: Last location null — live request karo
                    Log.d(TAG, "📍 Last location null — requesting live update...")
                    requestLiveFusedLocation(fusedClient, received, handler, callback)
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "❌ Fused lastLocation failed — trying live", it)
                requestLiveFusedLocation(fusedClient, received, handler, callback)
            }
    }

    @SuppressLint("MissingPermission")
    private fun requestLiveFusedLocation(
        fusedClient: com.google.android.gms.location.FusedLocationProviderClient,
        received: AtomicBoolean,
        handler: Handler,
        callback: (Location?) -> Unit
    ) {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 1000L
        )
            .setMaxUpdates(1)
            .setWaitForAccurateLocation(false) // false = pehla available location lo
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                if (received.compareAndSet(false, true)) {
                    val loc = result.lastLocation
                    Log.d(TAG, "📍 Fused live: ${loc?.latitude}, ${loc?.longitude}")
                    fusedClient.removeLocationUpdates(this)
                    handler.removeCallbacksAndMessages(null)
                    callback(loc)
                }
            }
        }

        try {
            fusedClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            Log.d(TAG, "📡 Fused live location request registered")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fused requestLocationUpdates failed", e)
            if (received.compareAndSet(false, true)) {
                handler.removeCallbacksAndMessages(null)
                callback(null)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // API Calls
    // ─────────────────────────────────────────────────────────────

    private fun sendLocationToApi(customerId: String?, location: Location) {
        if (customerId.isNullOrEmpty()) {
            Log.e(TAG, "❌ Customer ID missing")
            return
        }
        sendApiRequest(
            "$BASE_LOCATION_API_URL/$customerId/location",
            mapOf("latitude" to location.latitude, "longitude" to location.longitude)
        )
    }



    private fun sendApiRequest(url: String, data: Map<String, Any>) {
        try {
            val json = gson.toJson(data)
            Log.d(TAG, "📤 POST $url → $json")

            val request = Request.Builder()
                .url(url)
                .post(json.toRequestBody(JSON))
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "❌ API call failed", e)
                }
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (response.isSuccessful) {
                            Log.d(TAG, "✅ API success: ${response.body?.string()}")
                        } else {
                            Log.e(TAG, "❌ API error: ${response.code} ${response.message}")
                        }
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending API request", e)
        }
    }
}