package com.d.locker.lock.utils

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserManager
import android.provider.Settings
import android.util.Log
import com.d.locker.lock.receivers.MyDeviceAdminReceiver

/**
 * Utility class for managing device restrictions (blocking/unblocking features)
 */
object RestrictionUtils {
    private const val TAG = "RestrictionUtils"

    /**
     * Block or unblock Camera
     */
    fun setCameraDisabled(context: Context, disabled: Boolean) {
        try {
            Log.d(TAG, "→ setCameraDisabled($disabled) called")
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponent = ComponentName(context, MyDeviceAdminReceiver::class.java)

            if (!dpm.isDeviceOwnerApp(context.packageName)) {
                Log.e(TAG, "❌ Cannot change camera policy: App is not Device Owner")
                return
            }

            // ✅ Works on ALL devices — blocks camera API system-wide
            dpm.setCameraDisabled(adminComponent, disabled)
            Log.d(TAG, "✅ Camera ${if (disabled) "DISABLED" else "ENABLED"}")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error setting camera policy", e)
        }
    }

    /**
     * Block or unblock Settings app
     * Auto-detects correct package for any device (Samsung, Realme, Oppo, Vivo, Xiaomi etc.)
     */
    fun setSettingsDisabled(context: Context, disabled: Boolean) {
        try {
            Log.d(TAG, "→ setSettingsDisabled($disabled) called")
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponent = ComponentName(context, MyDeviceAdminReceiver::class.java)

            if (!dpm.isDeviceOwnerApp(context.packageName)) {
                Log.e(TAG, "❌ Cannot change settings policy: App is not Device Owner")
                return
            }

            // ✅ Step 1: Auto detect which settings packages are on THIS device
            val packagesToHide = detectSettingsPackages(context)
            Log.d(TAG, "🔍 Detected settings packages: $packagesToHide")

            if (packagesToHide.isEmpty()) {
                Log.e(TAG, "❌ No settings packages detected!")
                return
            }

            // ✅ Step 2: Hide/Show only the ones actually installed
            var successCount = 0
            var failCount = 0

            for (pkg in packagesToHide) {
                try {
                    val result = dpm.setApplicationHidden(adminComponent, pkg, disabled)
                    if (result) {
                        successCount++
                        Log.d(TAG, "✅ Package $pkg ${if (disabled) "HIDDEN" else "SHOWN"}")
                    } else {
                        failCount++
                        Log.e(TAG, "❌ Failed to hide/show package $pkg (setApplicationHidden returned false)")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Exception hiding/showing package $pkg", e)
                }
            }

            Log.d(TAG, "✅ Package visibility updated — Success: $successCount | Failed: $failCount")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error setting settings policy", e)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // AUTO DETECT SETTINGS PACKAGES
    // ─────────────────────────────────────────────────────────────

    private fun detectSettingsPackages(context: Context): List<String> {
        val pm = context.packageManager
        val manufacturer = Build.MANUFACTURER.lowercase().trim()
        val brand = Build.BRAND.lowercase().trim()

        Log.d(TAG, "📱 Device → Manufacturer: $manufacturer | Brand: $brand")

        val installed = mutableListOf<String>()

        // ✅ Method 1: Check known packages for this manufacturer
        val candidates = getSettingsPackagesForManufacturer(manufacturer, brand)
        for (pkg in candidates) {
            if (isPackageInstalled(pm, pkg)) {
                installed.add(pkg)
                Log.d(TAG, "✅ Found: $pkg")
            } else {
                Log.d(TAG, "⚠️ Not found: $pkg")
            }
        }

        // ✅ Method 2: Intent scan — finds settings on any unknown device/ROM
        try {
            val intent = Intent(Settings.ACTION_SETTINGS)
            val resolveList = pm.queryIntentActivities(intent, 0)
            for (info in resolveList) {
                val pkg = info.activityInfo.packageName
                if (!installed.contains(pkg)) {
                    installed.add(pkg)
                    Log.d(TAG, "📦 Found via Intent scan: $pkg")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Intent scan failed", e)
        }

        // ✅ Method 3: Keyword scan — last fallback for custom ROMs
        try {
            val keywords = listOf("setting", "safecenter", "systemmanager", "securitycenter")
            
            // Fix: Use flags to find hidden/uninstalled apps
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                PackageManager.MATCH_UNINSTALLED_PACKAGES
            } else {
                PackageManager.GET_UNINSTALLED_PACKAGES
            }

            pm.getInstalledPackages(flags).forEach { pkgInfo ->
                val pkgName = pkgInfo.packageName
                if (keywords.any { pkgName.contains(it, ignoreCase = true) }
                    && !installed.contains(pkgName)
                ) {
                    installed.add(pkgName)
                    Log.d(TAG, "📦 Found via keyword scan: $pkgName")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Keyword scan failed", e)
        }

        return installed.distinct()
    }

    // ─────────────────────────────────────────────────────────────
    // SETTINGS PACKAGE MAP PER MANUFACTURER
    // ─────────────────────────────────────────────────────────────

    private fun getSettingsPackagesForManufacturer(
        manufacturer: String,
        brand: String
    ): List<String> {

        // Always check these on every device
        val common = listOf(
            "com.android.settings",
            "com.android.settings.intelligence"
        )

        val specific = when {

            // Samsung
            manufacturer.contains("samsung") || brand.contains("samsung") -> listOf(
                "com.samsung.android.settings",
                "com.samsung.android.settings.intelligence",
                "com.sec.android.app.launcher",
                "com.samsung.android.lool",
                "com.samsung.android.sm"
            )

            // Realme
            manufacturer.contains("realme") || brand.contains("realme") -> listOf(
                "com.realme.settings",
                "com.coloros.settings",
                "com.oppo.settings",
                "com.oplus.settings",
                "com.coloros.safecenter"
            )

            // OPPO
            manufacturer.contains("oppo") || brand.contains("oppo") -> listOf(
                "com.oppo.settings",
                "com.coloros.settings",
                "com.oplus.settings",
                "com.coloros.safecenter"
            )

            // Vivo
            manufacturer.contains("vivo") || brand.contains("vivo") -> listOf(
                "com.vivo.settings",
                "com.iqoo.secure",
                "com.vivo.secure",
                "com.vivo.permissionmanager"
            )

            // Xiaomi / Redmi / POCO
            manufacturer.contains("xiaomi") || brand.contains("xiaomi")
            || brand.contains("redmi") || brand.contains("poco") -> listOf(
                "com.miui.securitycenter",
                "com.miui.settings",
                "com.xiaomi.misettings"
            )

            // OnePlus
            manufacturer.contains("oneplus") || brand.contains("oneplus") -> listOf(
                "net.oneplus.settings",
                "com.oneplus.settings",
                "com.coloros.settings",
                "com.oplus.settings"
            )

            // Huawei
            manufacturer.contains("huawei") || brand.contains("huawei") -> listOf(
                "com.huawei.systemmanager",
                "com.huawei.settings"
            )

            // Honor
            manufacturer.contains("honor") || brand.contains("honor") -> listOf(
                "com.honor.settings",
                "com.huawei.settings"
            )

            // Motorola
            manufacturer.contains("motorola") || brand.contains("motorola")
            || brand.contains("moto") -> listOf(
                "com.motorola.settings",
                "com.motorola.security"
            )

            // Nokia
            manufacturer.contains("nokia") || brand.contains("nokia") -> listOf(
                "com.nokia.settings"
            )

            // Fallback for any other brand
            else -> listOf(
                "com.google.android.settings",
                "com.lenovo.settings",
                "com.zte.settings"
            )
        }

        return (common + specific).distinct()
    }

    // ─────────────────────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────────────────────

    /**
     * Blocks factory reset, safe boot, and other critical reset paths.
     * Also calls USB disable logic as requested.
     */
    fun applyPostRegistrationRestrictions(context: Context) {
        try {
            Log.d(TAG, "→ applyPostRegistrationRestrictions() called")
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponent = ComponentName(context, MyDeviceAdminReceiver::class.java)

            if (!dpm.isDeviceOwnerApp(context.packageName)) {
                Log.e(TAG, "❌ Cannot apply restrictions: App is not Device Owner")
                return
            }

            // 1. Block Factory Reset
            // This prevents factory data reset from Settings
            dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_FACTORY_RESET)
            
            // 2. Block Safe Boot
            // Prevents holding buttons during boot to enter Safe Mode
            dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_SAFE_BOOT)
            
            // 3. Block Resetting Password/Credentials
            // dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_CONFIG_CREDENTIALS) // Might be too much

            Log.d(TAG, "✅ Factory Reset & Safe Boot DISABLED")

            // 4. Also disable USB Debugging as part of security setup
            UsbManager.disableUsbDebugging(context)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error applying restrictions", e)
        }
    }

    private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean {
        return try {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                PackageManager.MATCH_UNINSTALLED_PACKAGES
            } else {
                PackageManager.GET_UNINSTALLED_PACKAGES
            }
            pm.getApplicationInfo(packageName, flags or PackageManager.GET_META_DATA)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}