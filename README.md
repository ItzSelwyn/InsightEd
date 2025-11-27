📘 BLE Attendance System (InsightEd)

A Bluetooth Low Energy (BLE)–powered automatic attendance system built using:

• Android App (Kotlin) – broadcasts secure BLE UUIDs
• ESP32 Scanner (Arduino/C++) – detects nearby students
• Firebase – stores attendance in real time

This repository contains the complete source code, architecture, documentation, and instructions for running the system.

🚀 Features

• Automatic attendance detection using BLE
• Unique UUID for every student
• ESP32-based classroom scanner
• Real-time Firebase sync
• Proxy-prevention through verified BLE advertising
• Teacher dashboard support (prototype)
• Low-energy operation

📁 Repository Structure

ble-attendance-system/
│
├── InsightEd-app/             # BLE advertising Android application (Kotlin)
│   ├── app/
│   ├── gradle/
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── README.md
│
├── esp32/                   # ESP32 BLE scanner firmware
│   ├── esp32_ble_scanner.ino
│   ├── src/ (optional)
│   └── README.md
│
├── firebase/                # Firebase setup & configuration
│   ├── rules.json
│   ├── schema.json
│   └── README.md
│
├── docs/                    # Architecture diagrams, project flow, screenshots
│   ├── architecture.png
│   ├── flowchart.png
│   ├── screenshots/
│   └── report.pdf (optional)
│
└── README.md                # (You are here)

📱 Android App (BLE Advertiser)

The Android app broadcasts a secure BLE UUID representing a student’s identity.
Key components include:

• BLE permissions & scanning checks
• Foreground service for continuous advertising
• UUID generator
• Power-optimized BLE mode
• Simple UI

To open the project:

1. Open Android Studio
2. File → Open
3. Select the InsightEd-app/ folder

🔌 ESP32 BLE Scanner

The ESP32 device scans the classroom for BLE packets and forwards attendance data to Firebase.

Core functionality:

• Continuous BLE scanning
• RSSI filtering
• UUID verification
• Timestamp generation
• Firebase API integration (optional)

Source code:

/esp32/

☁️ Firebase Setup

Firebase is used for:

• Real-time attendance logs
• Dashboard data
• Device–cloud sync

Includes:

• rules.json – Security rules
• schema.json – Example database structure

You can set up your own Firebase project and update:

• API keys
• Firebase Realtime DB URL
• Authentication settings

🧠 How It Works — System Flow

1. Student’s phone broadcasts a unique BLE UUID.
2. ESP32 in classroom scans for nearby BLE packets.
3. ESP32 verifies the UUID and logs timestamp.
4. Data is pushed to Firebase immediately.
5. Faculty dashboard updates in real time.

🤝 Contributors

• Nigesh Satheesh
• Divyadharshini Balakrishnan
• Kavinaya Sekar
• Pallavi M
• Kavin Nizvan