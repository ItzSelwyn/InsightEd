**📘 BLE Attendance System (InsightEd)**

Bluetooth Low Energy (BLE) Attendance System, where every student’s phone becomes a secure digital identity. An ESP32 device inside the classroom scans BLE packets, validates UUIDs, and pushes the data to Firebase for instant attendance monitoring.

Bluetooth Low Energy (BLE)–powered automatic attendance system built using:

• Android App (Kotlin) – broadcasts a unique BLE UUID
• ESP32 Scanner (C++/Arduino) – detects student UUIDs inside the classroom
• Firebase Realtime Database – stores attendance logs in real time

This project removes manual attendance, prevents proxy marking, and provides a fast, automatic, low-energy solution for classroom attendance.

**🚀 Features**

• Automatic BLE-based attendance
• UUID identification per student
• ESP32 low-energy BLE scanning
• Real-time Firebase updates
• First_seen / last_seen tracking
• Attendance status (pending/present)

**📱 Android App – BLE UUID Advertiser**

The Android app broadcasts a unique BLE service UUID.
When a student enters the classroom with Bluetooth on, their device quietly transmits this UUID.

Highlights:

• Kotlin-based BLE advertiser
• Foreground service for stable advertising
• Secure UUID generation
• Permissions handled cleanly
• Lightweight UI

Full project inside:
/InsightEd-App

**🔌 ESP32 BLE Scanner**

The ESP32 listens for BLE packets matching your UUID prefix (ADC1xxxx).
When detected:

• Checks if the UUID was already seen
• Logs first_seen timestamp
• Updates last_seen
• Computes attendance status
• Writes data to Firebase in real time

The example firmware uses placeholder credentials.
Replace them locally before uploading.

Full code inside:
/esp32

**☁️ Firebase Realtime Database**

Used for:

• Attendance storage
• Period mapping
• Student info
• Optional users list for dashboards

✔ Public Read
Anyone can read database data

✔ Protected Write

Only authenticated devices (ESP32 via anonymous auth) can write.


See:

/firebase/rules.json

🧠 How the System Works

1. Student opens the Android BLE app
2. Phone advertises a unique service UUID
3. ESP32 BLE scanner detects UUIDs
4. ESP32 timestamps and verifies UUIDs
5. Writes attendance → Firebase
6. Dashboard/app can read attendance instantly

This architecture is lightweight, scalable, and requires no manual input.