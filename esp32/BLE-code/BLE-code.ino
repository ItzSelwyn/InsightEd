#include <Arduino.h>
#include <WiFi.h>
#include <Firebase_ESP_Client.h>
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEScan.h>
#include <BLEAdvertisedDevice.h>
#include <time.h>
#include <vector>

// Wi-Fi and Firebase credentials
#define WIFI_SSID "YOUR_WIFI"
#define WIFI_PASSWORD "YOUR_WIFI_PASSWORD"
#define API_KEY "YOUR_API_KEY"
#define DATABASE_URL "YOUR_DATABASE_URL"

// Firebase objects
FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

// BLE
int scanTime = 5;
BLEScan* pBLEScan;

// Store detected UUIDs in one scan
std::vector<String> detectedUUIDs;

// Define periods
struct Period {
    int startHour;
    int startMinute;
    int endHour;
    int endMinute;
    String name;
};

Period periods[] = {
    {9, 15, 10, 15, "period1"},
    {10, 15, 11, 15, "period2"},
    {11, 45, 12, 45, "period3"},
    {13, 45, 14, 40, "period4"},
    {14, 40, 15, 35, "period5"},
    {15, 35, 16, 30, "period6"},
};
const int NUM_PERIODS = sizeof(periods) / sizeof(periods[0]);

// BLE callback class
class MyAdvertisedDeviceCallbacks: public BLEAdvertisedDeviceCallbacks {
    void onResult(BLEAdvertisedDevice advertisedDevice) {
        String serviceUUID = advertisedDevice.getServiceUUID().toString().c_str();
        if (serviceUUID.startsWith("ADC1") || serviceUUID.startsWith("adc1")) {
            Serial.print("Found matching UUID: ");
            Serial.println(serviceUUID);

            // Add UUID if not already in list
            if (std::find(detectedUUIDs.begin(), detectedUUIDs.end(), serviceUUID) == detectedUUIDs.end()) {
                detectedUUIDs.push_back(serviceUUID);
            }
        }
    }
};

// Initialize NTP time
void initTime() {
    configTime(19800, 0, "pool.ntp.org", "time.nist.gov"); // GMT+5:30
    Serial.print("Waiting for NTP time sync...");
    time_t now = time(nullptr);
    while (now < 8 * 3600 * 2) {
        delay(500);
        Serial.print(".");
        now = time(nullptr);
    }
    Serial.println("Time synced!");
}

// Get today's folder
String getTodayFolder() {
    struct tm timeinfo;
    if (!getLocalTime(&timeinfo)) {
        Serial.println("Failed to obtain time");
        return "unknown_date";
    }
    char dateStr[20];
    strftime(dateStr, sizeof(dateStr), "%d_%m_%Y", &timeinfo);
    return String(dateStr);
}

// Determine current period
String getCurrentPeriod() {
    struct tm timeinfo;
    if (!getLocalTime(&timeinfo)) return "unknown_period";

    int nowMinutes = timeinfo.tm_hour * 60 + timeinfo.tm_min;

    for (int i = 0; i < NUM_PERIODS; i++) {
        int start = periods[i].startHour * 60 + periods[i].startMinute;
        int end   = periods[i].endHour * 60 + periods[i].endMinute;
        if (nowMinutes >= start && nowMinutes < end) return periods[i].name;
    }
    return "no_period";
}

void setup() {
    Serial.begin(115200);

    // WiFi
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
    Serial.print("Connecting to Wi-Fi");
    while (WiFi.status() != WL_CONNECTED) {
        Serial.print(".");
        delay(300);
    }
    Serial.println();
    Serial.print("Connected with IP: ");
    Serial.println(WiFi.localIP());

    initTime();

    // Firebase setup
    config.api_key = API_KEY;
    config.database_url = DATABASE_URL;

    if (Firebase.signUp(&config, &auth, "", "")) {
        Serial.println("SignUp OK");
    } else {
        Serial.printf("%s\n", config.signer.signupError.message.c_str());
    }

    Firebase.begin(&config, &auth);
    Firebase.reconnectWiFi(true);

    // BLE setup
    BLEDevice::init("");
    pBLEScan = BLEDevice::getScan();
    pBLEScan->setAdvertisedDeviceCallbacks(new MyAdvertisedDeviceCallbacks());
    pBLEScan->setActiveScan(true);
    pBLEScan->setInterval(100);
    pBLEScan->setWindow(99);
}

void loop() {
    // Start BLE scan
    pBLEScan->start(scanTime, false);
    pBLEScan->clearResults();

    String today = getTodayFolder();
    String period = getCurrentPeriod();

    if (Firebase.ready() && !detectedUUIDs.empty() && period != "no_period" && period != "unknown_period") {
        time_t now = time(nullptr);

        for (String uuid : detectedUUIDs) {
            String basePath = "attendance/" + today + "/" + period + "/" + uuid;

            // Check if first_seen exists
            int firstSeen = 0;

            if (Firebase.RTDB.getInt(&fbdo, basePath + "/first_seen")) {
                if (fbdo.dataType() == "int") {
                    firstSeen = fbdo.intData();
                }
            } else {
                Serial.printf("Logging FIRST SEEN for %s in %s\n", uuid.c_str(), period.c_str());
                if (Firebase.RTDB.setInt(&fbdo, basePath + "/first_seen", now)) {
                    firstSeen = now;
                }
            }

            // Update last_seen
            Serial.printf("Updating LAST SEEN for %s in %s at %ld\n", uuid.c_str(), period.c_str(), now);
            Firebase.RTDB.setInt(&fbdo, basePath + "/last_seen", now);

            // Check attendance duration
            if (firstSeen > 0) {
                int diff = now - firstSeen;
                String status = (diff >= 2700) ? "present" : "pending";
                Firebase.RTDB.setString(&fbdo, basePath + "/status", status);
}

        }

        detectedUUIDs.clear(); // clear after processing
        delay(1000);           // avoid watchdog
    }

    delay(100);
}

