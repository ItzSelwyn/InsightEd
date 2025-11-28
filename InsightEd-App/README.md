# InsightEd Android App (BLE Advertiser)

This Android application broadcasts a **unique Bluetooth Low Energy (BLE) Service UUID** to identify a student’s presence inside a classroom.  
It is one of the core components of the BLE Attendance System.

The ESP32 scanner detects this UUID and logs attendance automatically in Firebase.

## 🚀 Features

- BLE advertising in foreground service  
- Unique UUID assigned per student  
- Low-energy BLE mode  
- Auto-start advertising on app launch  
- Supports Android 8.0+  
- Handles permissions cleanly (Location + Bluetooth)  
- Lightweight and battery-friendly


## 🔧 Technologies

- **Kotlin**
- **Android BLE API**
- **Foreground Service**
- **Material Components**
- **Firebase (optional analytics/auth if added later)**

## 📡 How It Works

1. App generates or loads a **unique BLE service UUID**  
2. Starts a **Foreground BLE Advertiser Service**  
3. Transmits UUID every few milliseconds  
4. ESP32 scanner detects the UUID  
5. Attendance is logged in Firebase by the ESP32

The app does **not** write to Firebase directly.

## 🔒 Important Notes

### `google-services.json` is **not included** in the repository.

If you use Firebase inside the app:

1. Download `google-services.json` from Firebase Console  
2. Place it in:

insighted-app/app/google-services.json

## 🏗 Setup Instructions

1. Open **Android Studio**  
2. Click **Open Project** and select `insighted-app/`  
3. Allow Gradle to sync  
4. Enable Bluetooth + Location on the device  
5. Run on a real device (BLE doesn’t work on most emulators)

---

## 📱 Permissions Required

The app requests:

- `BLUETOOTH`
- `BLUETOOTH_ADMIN`
- `BLUETOOTH_ADVERTISE`
- `ACCESS_FINE_LOCATION` (required by Android BLE)
- Background location (optional if advertising persists)