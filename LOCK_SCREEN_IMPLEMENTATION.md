# Lock Screen Implementation with Firebase Messaging

## ✅ Features Implemented

### 1. **Lock Screen Activity (`LockActivity`)**
- Full-screen blocking overlay
- Blocks "Back" button
- Blocks interaction
- Displays "DEVICE LOCKED" message in English and Hindi
- Supports PIN unlock (hidden by default, for admin use)
- **Kiosk Mode**: Uses `startLockTask()` to pin the screen (requires Device Owner status)

### 2. **Firebase Messaging Service (`MyFirebaseMessagingService`)**
- Listens for background data messages
- Handles `command` payload:
  - **`LOCK`**: Launches the Lock Screen
  - **`UNLOCK`**: Dismisses the Lock Screen

### 3. **SIM Toolkit Dummy Screen (`SimToolkitActivity`)**
- Non-interactive screen shown after successful registration
- "Always On" display
- Blocks touch events
- Looks like a system menu

### 4. **Registration Flow Updates**
- Parses API response message
- Auto-redirects to SIM Toolkit if already registered
- Saves registration status in SharedPreferences

## 🔧 Setup & Configuration

### Dependencies Added
- `firebase-messaging`
- `firebase-analytics`
- `google-services` plugin

### Manifest Updates
- Registered `LockActivity` (singleTop, no history)
- Registered `MyFirebaseMessagingService`
- Added `SimToolkitActivity`

### ⚠️ IMPORTANT: Google Services Config
The `google-services.json` file you have contains package names:
- `com.dlocker.app`
- `com.overlaylockapp`

**It does NOT contain your app's package:** `com.trustonic.overlaynewdevice`

**Action Required:**
I have temporarily patched the JSON file to allow the build to pass. However, for Firebase to actually work (receive messages), you **MUST**:
1. Go to Firebase Console
2. Add an Android app with package name: `com.trustonic.overlaynewdevice`
3. Download the new `google-services.json`
4. Replace the file in `app/google-services.json`

## 🧪 How to Test

### 1. Build and Install
```bash
.\gradlew.bat assembleRelease
adb install -r app\build\outputs\apk\release\app-release.apk
```

### 2. Test Registration & SIM Toolkit
1. Open App
2. Register successfully
3. App closes
4. Re-open App -> Should go straight to SIM Toolkit

### 3. Test Lock Command
Send a Firebase Data Message (via Postman or Admin SDK):

**To LOCK:**
```json
{
  "to": "<DEVICE_fcm_token>",
  "data": {
    "command": "LOCK"
  }
}
```
**Result:** Device screen turns white with lock message. Back button disabled.

**To UNLOCK:**
```json
{
  "to": "<DEVICE_fcm_token>",
  "data": {
    "command": "UNLOCK"
  }
}
```
**Result:** Lock screen disappears.

## 📁 Key Files Created/Modified
- `app/src/main/java/com/d/locker/lock/services/MyFirebaseMessagingService.kt`
- `app/src/main/java/com/d/locker/lock/activities/LockActivity.kt`
- `app/src/main/java/com/d/locker/lock/activities/SimToolkitActivity.kt`
- `app/src/main/res/layout/activity_lock.xml`
- `app/src/main/AndroidManifest.xml`
