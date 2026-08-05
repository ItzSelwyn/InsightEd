# InsightEd: BLE Attendance System

InsightEd is a Bluetooth Low Energy (BLE)-based attendance system where each student device advertises a unique UUID, an ESP32 scanner detects it inside the classroom, and attendance is written to Firebase in real time.

## What This Project Contains

- `InsightEd-App/`: Android app (Kotlin + Compose) that advertises a BLE service UUID.
- `esp32/`: ESP32 firmware (Arduino/C++) that scans BLE advertisements and logs attendance.
- `Firebase/`: Realtime Database rules and sample data structure.

## Key Features

- Automatic BLE-based attendance capture
- UUID-based student identification
- Real-time Firebase Realtime Database updates
- `first_seen` and `last_seen` timestamp tracking
- Attendance status derivation (`pending` or `present`)

## System Flow

1. Student opens the Android app on a real device.
2. App starts foreground BLE advertising with a unique service UUID.
3. ESP32 scans advertisements and filters UUIDs (prefix `ADC1`/`adc1`).
4. ESP32 timestamps detections for the active class period.
5. ESP32 writes attendance data to Firebase.
6. Dashboard or mobile clients read attendance in real time.

## Repository Structure

```text
InsightEd/
	README.md
	InsightEd-App/               # Android app (BLE advertiser)
	esp32/
		BLE-code/BLE-code-sample.ino      # ESP32 scanner firmware
	Firebase/
		rules.json                 # Firebase Realtime DB security rules
		example-structure.json     # Sample database shape
```

## Prerequisites

### Android App

- Android Studio (latest stable)
- Android SDK (configured via `local.properties`)
- Physical Android device (BLE advertising is unreliable in emulators)

### ESP32 Scanner

- ESP32 development board
- Arduino IDE with ESP32 board package
- Libraries:
	- `Firebase ESP Client` (Mobizt)
	- `ESP32 BLE Arduino` (Espressif)

### Backend

- Firebase project with Realtime Database enabled

## Missing Files and Secrets You Must Provide

Some files and values are intentionally local/private. If they are absent in your clone, create/provide them before running.

| Path | Needed For | Required Action |
|---|---|---|
| `InsightEd-App/local.properties` | Android Gradle build | Set local SDK path, for example `sdk.dir=C:\\Users\\<you>\\AppData\\Local\\Android\\Sdk` |
| `InsightEd-App/app/google-services.json` | Firebase config in Android app | Download from Firebase Console and place in this path |
| `esp32/BLE-code/BLE-code-sample.ino` constants | ESP32 runtime connectivity | Replace placeholders: `WIFI_SSID`, `WIFI_PASSWORD`, `API_KEY`, `DATABASE_URL` |

### ESP32 Placeholder Block to Replace

```cpp
#define WIFI_SSID "YOUR_WIFI"
#define WIFI_PASSWORD "YOUR_WIFI_PASSWORD"
#define API_KEY "YOUR_API_KEY"
#define DATABASE_URL "YOUR_DATABASE_URL"
```

## Firebase Setup

1. Create/select a Firebase project.
2. Enable Realtime Database.
3. Apply rules from `Firebase/rules.json`.
4. Optionally import `Firebase/example-structure.json` to seed `students`, `periods`, and sample `attendance`.
5. Ensure anonymous auth is enabled if ESP32 uses anonymous sign-up.

## Run Guide

### 1. Run Android BLE Advertiser

1. Open `InsightEd-App/` in Android Studio.
2. Ensure `local.properties` and `app/google-services.json` are present.
3. Sync Gradle.
4. Install and run on a physical phone.
5. Grant Bluetooth/location permissions when prompted.

### 2. Flash and Run ESP32 Scanner

1. Open `esp32/BLE-code/BLE-code-sample.ino` in Arduino IDE.
2. Install required board package/libraries.
3. Replace Wi-Fi and Firebase placeholders.
4. Upload to ESP32.
5. Open Serial Monitor to confirm Wi-Fi, NTP sync, BLE detections, and Firebase writes.

## Database Shape (Summary)

Main attendance path:

```text
attendance/{dd_mm_yyyy}/{period}/{uuid}/
	first_seen: <unix_timestamp>
	last_seen: <unix_timestamp>
	status: "pending" | "present"
```

## Security Note

- Current rules allow public read and authenticated write under `attendance`.
- For production use, tighten read access and add role-based write restrictions.

## Troubleshooting

- App build fails with Google Services error:
	- Confirm `InsightEd-App/app/google-services.json` exists and matches your Firebase project.
- ESP32 cannot write to Firebase:
	- Verify `API_KEY`, `DATABASE_URL`, and that Firebase auth/rules allow the write path.
- No BLE detections:
	- Check phone Bluetooth permissions and ensure advertised UUID prefix matches scanner filter (`ADC1`/`adc1`).

## Notes

- `InsightEd-App/build/` contains generated artifacts and should not be used as source.
- Keep secrets out of version control and rotate keys if exposed.