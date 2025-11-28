# ESP32 BLE Scanner

This folder contains the ESP32 firmware used in the BLE Attendance System.  
The ESP32 continuously scans for nearby BLE devices broadcasting a specific UUID, validates them, and sends attendance data to Firebase.

The file included in this repository is a **public-safe example**.  
You must replace the placeholder values with your own project details before uploading the code to your ESP32.

## 🔧 Required Changes Before Uploading

Open the `.ino` file and update the following fields:

// Wi-Fi and Firebase credentials
#define WIFI_SSID "YOUR_WIFI"
#define WIFI_PASSWORD "YOUR_WIFI_PASSWORD"
#define API_KEY "YOUR_API_KEY"
#define DATABASE_URL "YOUR_DATABSE_URL"

These values are not committed in this repository to protect private keys.

🚀 How the ESP32 Scanner Works

1. Connects to your Wi-Fi network
2. Starts BLE scanning
3. Detects BLE advertisements from the Android app
4. Extracts UUID
5. Validates and filters duplicate scans
6. Sends verified attendance data to Firebase in real-time

📦 Requirements

• ESP32 Dev Board
• Arduino IDE

**Required libraries:**

Library: Firebase ESP Client
Author: Mobizt
Install in library manager

Library: ESP32 BLE Arduino
Author: Espressif
Install in Board manager

5. ESP32 Board Package

Make sure your Arduino IDE has the ESP32 boards installed:
File → Preferences
Add this to Additional Boards Manager URLs:

https://espressif.github.io/arduino-esp32/package_esp32_index.json