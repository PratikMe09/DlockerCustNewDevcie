package com.d.locker.lock.activities

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.d.locker.lock.receivers.MyDeviceAdminReceiver
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.trustonic.overlaynewdevice.BuildConfig
import com.trustonic.overlaynewdevice.R
import com.google.firebase.messaging.FirebaseMessaging
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import android.media.ExifInterface
import android.graphics.Matrix
import com.google.android.material.imageview.ShapeableImageView
import com.d.locker.lock.utils.RestrictionUtils

/**
 * MainActivity for Customer Registration
 * 
 * Features:
 * - Device Owner status display
 * - Auto-detect IMEI numbers
 * - Camera capture for profile picture
 * - Customer registration with API integration
 */
class MainActivity : AppCompatActivity() {

    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    
    // Device owner status views
    private lateinit var deviceOwnerStatusText: TextView
    private lateinit var statusIcon: ImageView
    
    // Form fields
    private lateinit var nameEditText: TextInputEditText
    private lateinit var mobileEditText: TextInputEditText
    private lateinit var shopIdEditText: TextInputEditText
    private lateinit var imei1EditText: TextInputEditText
    private lateinit var imei2EditText: TextInputEditText
    
    // Input layouts for error display
    private lateinit var nameInputLayout: TextInputLayout
    private lateinit var mobileInputLayout: TextInputLayout
    private lateinit var shopIdInputLayout: TextInputLayout
    
    // Image components
    private lateinit var profileImageView: ShapeableImageView
    private lateinit var selectImageBtn: FloatingActionButton
    private lateinit var profilePictureError: TextView
    
    // Submit button
    private lateinit var submitButton: Button
    
    // Captured photo file and URI
    private var capturedPhotoFile: File? = null
    private var capturedPhotoBitmap: Bitmap? = null
    
    // FCM Token
    private var fcmToken: String? = null
    
    companion object {
        private const val TAG = "MainAppActivity"
        private const val PERMISSION_REQUEST_CODE = 100
        private const val API_BASE_URL = "https://dlockerbackend.idea2reality.tech"
    }

    // Camera launcher
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            capturedPhotoFile?.let { file ->
                // Load and fix the captured image rotation
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, Uri.fromFile(file))
                val fixedBitmap = rotateImageIfRequired(bitmap, file.absolutePath)
                capturedPhotoBitmap = fixedBitmap
                
                profileImageView.setImageBitmap(fixedBitmap)
                profileImageView.setPadding(0, 0, 0, 0) // Remove padding when image is set
                profilePictureError.visibility = android.view.View.GONE
                Log.d(TAG, "Photo captured and fixed successfully: ${file.absolutePath}")
            }
        } else {
            Toast.makeText(this, "Failed to capture photo", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if registration is already complete
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val isRegistered = prefs.getBoolean("is_registered", false)
        
        if (isRegistered) {
            // Registration already done, redirect to SIM Toolkit screen
            val intent = Intent(this, SimToolkitActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        
        setContentView(R.layout.activity_main)

        // Initialize Device Policy Manager
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, MyDeviceAdminReceiver::class.java)

        setupViews()
        updateDeviceOwnerStatus()
        checkAndRequestPermissions()
        getFCMToken()
    }

    /**
     * Initialize UI components
     */
    private fun setupViews() {
        // Device owner status
        deviceOwnerStatusText = findViewById(R.id.deviceOwnerStatusText)
        statusIcon = findViewById(R.id.statusIcon)
        
        // Form fields
        nameEditText = findViewById(R.id.nameEditText)
        mobileEditText = findViewById(R.id.mobileEditText)
        shopIdEditText = findViewById(R.id.shopIdEditText)
        imei1EditText = findViewById(R.id.imei1EditText)
        imei2EditText = findViewById(R.id.imei2EditText)
        
        // Input layouts
        nameInputLayout = findViewById(R.id.nameInputLayout)
        mobileInputLayout = findViewById(R.id.mobileInputLayout)
        shopIdInputLayout = findViewById(R.id.shopIdInputLayout)
        
        // Image components
        profileImageView = findViewById(R.id.profileImageView)
        selectImageBtn = findViewById(R.id.selectImageBtn)
        profilePictureError = findViewById(R.id.profilePictureError)
        
        // Submit button
        submitButton = findViewById(R.id.submitButton)

        // Set up click listeners
        selectImageBtn.setOnClickListener {
            openCamera()
        }
        
        submitButton.setOnClickListener {
            handleSubmit()
        }


    }

    /**
     * Update device owner status display
     */
    private fun updateDeviceOwnerStatus() {
        val isDeviceOwner = devicePolicyManager.isDeviceOwnerApp(packageName)
        
        if (isDeviceOwner) {
            deviceOwnerStatusText.text = "✓ Device Owner Active"
            deviceOwnerStatusText.setTextColor(Color.parseColor("#10B981")) // Green
            statusIcon.setColorFilter(Color.parseColor("#10B981"))
            
            // Enable Kiosk Mode (Lock Task Mode) for this app
            try {
                if (devicePolicyManager.isDeviceOwnerApp(packageName)) {
                    val packages = arrayOf(packageName)
                    devicePolicyManager.setLockTaskPackages(adminComponent, packages)
                    Log.d(TAG, "Kiosk mode enabled for package: $packageName")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set lock task packages", e)
            }
        } else {
            deviceOwnerStatusText.text = "✗ Not Device Owner"
            deviceOwnerStatusText.setTextColor(Color.parseColor("#EF4444")) // Red
            statusIcon.setColorFilter(Color.parseColor("#EF4444"))
        }
        
        Log.d(TAG, "Device Owner Status: $isDeviceOwner")
    }

    /**
     * Check and request necessary permissions
     */
    private fun checkAndRequestPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        // Check READ_PHONE_STATE permission for IMEI
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsNeeded.add(Manifest.permission.READ_PHONE_STATE)
        }

        // Check CAMERA permission
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsNeeded.add(Manifest.permission.CAMERA)
        }

        // Request permissions if needed
        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsNeeded.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        } else {
            // Permissions already granted, auto-fill IMEI
            autoFillIMEI()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            
            if (grantResults.isNotEmpty() && allGranted) {
                autoFillIMEI()
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this,
                    "Some permissions denied. Some features may not work.",
                    Toast.LENGTH_LONG
                ).show()
                // Try to auto-fill IMEI anyway if that permission was granted
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_PHONE_STATE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    autoFillIMEI()
                }
            }
        }
    }

    /**
     * Auto-fill IMEI numbers using TelephonyManager
     */
    private fun autoFillIMEI() {
        try {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_PHONE_STATE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                
                // Get IMEI 1
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try {
                        val imei1 = telephonyManager.getImei(0)
                        if (!imei1.isNullOrEmpty()) {
                            imei1EditText.setText(imei1)
                            Log.d(TAG, "IMEI 1 auto-filled: $imei1")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error getting IMEI 1", e)
                    }
                    
                    // Get IMEI 2 (for dual SIM devices)
                    try {
                        val imei2 = telephonyManager.getImei(1)
                        if (!imei2.isNullOrEmpty()) {
                            imei2EditText.setText(imei2)
                            Log.d(TAG, "IMEI 2 auto-filled: $imei2")
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "IMEI 2 not available (possibly single SIM device)")
                    }
                } else {
                    // For older Android versions
                    @Suppress("DEPRECATION")
                    val deviceId = telephonyManager.deviceId
                    if (!deviceId.isNullOrEmpty()) {
                        imei1EditText.setText(deviceId)
                        Log.d(TAG, "Device ID auto-filled: $deviceId")
                    }
                }
                
                Toast.makeText(this, "IMEI auto-detected", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error auto-filling IMEI", e)
            Toast.makeText(this, "Could not auto-detect IMEI", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Open camera to take a picture
     */
    private fun openCamera() {
        // Check camera permission
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                PERMISSION_REQUEST_CODE
            )
            return
        }

        try {
            // Create a temporary file to store the photo
            val photoFile = File(
                getExternalFilesDir(null),
                "profile_${System.currentTimeMillis()}.jpg"
            )
            capturedPhotoFile = photoFile

            val photoUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )

            cameraLauncher.launch(photoUri)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening camera", e)
            Toast.makeText(this, "Failed to open camera: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Get FCM Token for push notifications
     */
    private fun getFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                fcmToken = task.result
                Log.d(TAG, "FCM Token retrieved: $fcmToken")
            } else {
                Log.e(TAG, "Failed to get FCM token", task.exception)
            }
        }
    }


    /**
     * Validate form inputs
     */
    private fun validateForm(): Boolean {
        var isValid = true

        // Clear previous errors
        nameInputLayout.error = null
        mobileInputLayout.error = null
        shopIdInputLayout.error = null
        profilePictureError.visibility = android.view.View.GONE

        // Validate name
        val name = nameEditText.text.toString().trim()
        if (name.isEmpty()) {
            nameInputLayout.error = "Name is required"
            isValid = false
        }

        // Validate mobile number
        val mobile = mobileEditText.text.toString().trim()
        if (mobile.isEmpty()) {
            mobileInputLayout.error = "Mobile number is required"
            isValid = false
        } else if (mobile.length != 10) {
            mobileInputLayout.error = "Mobile number must be 10 digits"
            isValid = false
        } else if (!mobile.matches(Regex("^[0-9]{10}$"))) {
            mobileInputLayout.error = "Invalid mobile number"
            isValid = false
        }

        // Validate shop ID
        val shopId = shopIdEditText.text.toString().trim()
        if (shopId.isEmpty()) {
            shopIdInputLayout.error = "Shop ID is required"
            isValid = false
        }

        // Validate profile picture
        if (capturedPhotoFile == null || capturedPhotoBitmap == null) {
            profilePictureError.text = "Profile picture is required"
            profilePictureError.visibility = android.view.View.VISIBLE
            isValid = false
        }

        return isValid
    }

    /**
     * Handle form submission
     */
    private fun handleSubmit() {
        if (!validateForm()) {
            Toast.makeText(this, "Please fix the errors", Toast.LENGTH_SHORT).show()
            return
        }

        // Disable submit button to prevent multiple submissions
        submitButton.isEnabled = false
        submitButton.text = "Registering..."

        // Collect and send data
        registerCustomer()
    }

    /**
     * Get device information
     */
    private fun getDeviceInfo(): DeviceInfo {
        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        val isDeviceOwner = devicePolicyManager.isDeviceOwnerApp(packageName)
        
        return DeviceInfo(
            androidId = androidId ?: "",
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            osVersion = Build.VERSION.RELEASE,
            appVersion = BuildConfig.VERSION_NAME,
            isDeviceOwner = isDeviceOwner,
            enrollmentSpecificId = generateEnrollmentId()
        )
    }

    /**
     * Generate enrollment specific ID (UUID)
     */
    private fun generateEnrollmentId(): String {
        return UUID.randomUUID().toString()
    }

    /**
     * Generate a unique unlock code with letters and numbers (5 digits)
     */
    private fun generateUnlockCode(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        return (1..5)
            .map { chars.random() }
            .joinToString("")
    }

    /**
     * Register customer via API
     */
    private fun registerCustomer() {
        Thread {
            try {
                val deviceInfo = getDeviceInfo()
                val unlockCode = generateUnlockCode()
                
                // Save unlock code for local verification
                getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString("unlock_code", unlockCode)
                    .apply()
                
                // Prepare form data
                val formDataBuilder = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                
                // Customer data
                formDataBuilder.addFormDataPart(
                    "customerName",
                    nameEditText.text.toString().trim()
                )
                formDataBuilder.addFormDataPart(
                    "customerPhone",
                    mobileEditText.text.toString().trim()
                )
                formDataBuilder.addFormDataPart(
                    "retailerId",
                    shopIdEditText.text.toString().trim()
                )
                
                // Device data
                formDataBuilder.addFormDataPart(
                    "device.imei1",
                    imei1EditText.text.toString().trim()
                )
                formDataBuilder.addFormDataPart(
                    "device.imei2",
                    imei2EditText.text.toString().trim()
                )
                formDataBuilder.addFormDataPart(
                    "device.androidId",
                    deviceInfo.androidId
                )
                formDataBuilder.addFormDataPart(
                    "device.manufacturer",
                    deviceInfo.manufacturer
                )
                formDataBuilder.addFormDataPart(
                    "device.model",
                    deviceInfo.model
                )
                formDataBuilder.addFormDataPart(
                    "device.osVersion",
                    deviceInfo.osVersion
                )
                formDataBuilder.addFormDataPart(
                    "device.appVersion",
                    deviceInfo.appVersion
                )
                formDataBuilder.addFormDataPart(
                    "device.isDeviceOwner",
                    deviceInfo.isDeviceOwner.toString()
                )
                
                // Device Type (New Device by default)
                formDataBuilder.addFormDataPart(
                    "device.deviceType",
                    "New Device"
                )
                
                // Enrollment data
                formDataBuilder.addFormDataPart(
                    "enrollmentSpecificId",
                    deviceInfo.enrollmentSpecificId
                )
                
                // FCM Token for push notifications
                fcmToken?.let { token ->
                    formDataBuilder.addFormDataPart(
                        "fcm.token",
                        token
                    )
                }
                
                // Profile picture
                capturedPhotoFile?.let { file ->
                    if (file.exists()) {
                        val requestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                        formDataBuilder.addFormDataPart(
                            "profileImageUrl",
                            file.name,
                            requestBody
                        )
                    }
                }
                
                // val requestBody = formDataBuilder.build() // Moved to below
                
                // Log form data
                Log.d(TAG, "=== Registration Request ===")
                Log.d(TAG, "Customer Name: ${nameEditText.text}")
                Log.d(TAG, "Customer Phone: ${mobileEditText.text}")
                Log.d(TAG, "Retailer ID: ${shopIdEditText.text}")
                Log.d(TAG, "IMEI 1: ${imei1EditText.text}")
                Log.d(TAG, "IMEI 2: ${imei2EditText.text}")
                Log.d(TAG, "Android ID: ${deviceInfo.androidId}")
                Log.d(TAG, "Manufacturer: ${deviceInfo.manufacturer}")
                Log.d(TAG, "Model: ${deviceInfo.model}")
                Log.d(TAG, "OS Version: ${deviceInfo.osVersion}")
                Log.d(TAG, "App Version: ${deviceInfo.appVersion}")
                Log.d(TAG, "Device Owner: ${deviceInfo.isDeviceOwner}")
                Log.d(TAG, "Device Type: New Device")
                Log.d(TAG, "Enrollment ID: ${deviceInfo.enrollmentSpecificId}")
                Log.d(TAG, "FCM Token: ${fcmToken ?: "Not available"}")
                Log.d(TAG, "Profile Picture: ${capturedPhotoFile?.name}")
                Log.d(TAG, "Unlock Code: $unlockCode")
                
                // Unlock Code
                formDataBuilder.addFormDataPart(
                    "unlockCode",
                    unlockCode
                )
                
                val requestBody = formDataBuilder.build()
                
                // Create request
                val request = Request.Builder()
                    .url("$API_BASE_URL/v1/private/customers")
                    .post(requestBody)
                    .build()
                
                // Make API call
                val client = OkHttpClient()
                val response = client.newCall(request).execute()
                
                val isSuccessful = response.isSuccessful
                val responseCode = response.code
                val responseMessage = response.message
                val responseBody = response.body?.string()
                
                runOnUiThread {
                    submitButton.isEnabled = true
                    submitButton.text = "Register Device"
                    
                    if (isSuccessful) {
                        val body = responseBody ?: "{}"
                        Log.d(TAG, "Registration success: $body")
                        
                        // Show success alert with response message
                        showSuccessDialog(body)
                        
                    } else {
                        Log.e(TAG, "Registration failed: $responseCode - $responseBody")
                        
                        val errorMessage = if (!responseBody.isNullOrEmpty()) {
                            try {
                                val json = JSONObject(responseBody)
                                json.optString("message", responseBody)
                            } catch (e: Exception) {
                                responseBody
                            }
                        } else {
                            responseMessage
                        }
                        
                        Toast.makeText(
                            this@MainActivity,
                            "Registration failed: $errorMessage",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                
            } catch (e: IOException) {
                Log.e(TAG, "Network error during registration", e)
                runOnUiThread {
                    submitButton.isEnabled = true
                    submitButton.text = "Register Device"
                    Toast.makeText(
                        this,
                        "Network error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during registration", e)
                runOnUiThread {
                    submitButton.isEnabled = true
                    submitButton.text = "Register Device"
                    Toast.makeText(
                        this,
                        "Registration failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    /**
     * Clear form fields
     */
    private fun clearForm() {
        nameEditText.text?.clear()
        mobileEditText.text?.clear()
        shopIdEditText.text?.clear()
        imei1EditText.text?.clear()
        imei2EditText.text?.clear()
        capturedPhotoFile = null
        capturedPhotoBitmap = null
        profileImageView.setImageResource(android.R.drawable.ic_menu_camera)
    }

    /**
     * Show success dialog with API response and launch SIM Toolkit screen
     */
    private fun showSuccessDialog(responseBody: String) {
        // Parse response to extract message
        val message = try {
            val json = JSONObject(responseBody)
            json.optString("message", "Device registered successfully!")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing response JSON", e)
            "Device registered successfully!"
        }
        
        // Save registration status
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_registered", true).apply()
        
        // Block Factory Reset, Safe Boot and Disable USB Debugging immediately
        RestrictionUtils.applyPostRegistrationRestrictions(this)
        
        Log.d(TAG, "Registration status saved and restrictions applied. App will show SIM Toolkit on next launch.")
        
        AlertDialog.Builder(this)
            .setTitle("✅ Registration Successful")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                
                // Launch SIM Toolkit screen and clear task stack
                val intent = Intent(this, SimToolkitActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        updateDeviceOwnerStatus()
    }

    /**
     * Fix image rotation based on EXIF data
     */
    private fun rotateImageIfRequired(img: Bitmap, path: String): Bitmap {
        val ei = ExifInterface(path)
        val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270f)
            else -> img
        }
    }

    private fun rotateImage(img: Bitmap, degree: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree)
        val rotatedImg = Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
        img.recycle()
        return rotatedImg
    }

    /**
     * Data class to hold device information
     */
    data class DeviceInfo(
        val androidId: String,
        val manufacturer: String,
        val model: String,
        val osVersion: String,
        val appVersion: String,
        val isDeviceOwner: Boolean,
        val enrollmentSpecificId: String
    )
}