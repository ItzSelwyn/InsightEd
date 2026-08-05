#include <Arduino.h>
#include <WiFi.h>
#include <Firebase_ESP_Client.h>
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEScan.h>
#include <BLEAdvertisedDevice.h>
#include <time.h>
#include <algorithm>
#include <map>
#include <vector>

#define WIFI_SSID "<YOUR_WIFI_SSID>"
#define WIFI_PASSWORD "<YOUR_WIFI_PASSWORD>"
#define API_KEY "<YOUR_API_KEY>"
#define DATABASE_URL "<YOUR_DATABASE_URL>"

static const unsigned long BLE_SCAN_SECONDS = 5;
static const unsigned long LOST_CHECK_INTERVAL_MS = 30000UL;
static const unsigned long LOST_THRESHOLD_MS = 60000UL;
static const unsigned long NTP_SYNC_TIMEOUT_MS = 60000UL;
static const unsigned long NTP_RETRY_DELAY_MS = 5000UL;

FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

BLEScan* pBLEScan;

std::vector<String> detectedUUIDs;

struct Period {
    int startHour;
    int startMinute;
    int endHour;
    int endMinute;
    String name;
};

struct TrackedUUIDState {
    unsigned long lastSeenMillis = 0;
    bool isLost = false;
};

Period periods[] = {
    {8, 15, 9, 15, "period1"},
    {9, 15, 10, 15, "period2"},
    {10, 45, 11, 45, "period3"},
    {11, 45, 12, 45, "period4"},
    {13, 45, 14, 45, "period5"},
};

const int NUM_PERIODS = sizeof(periods) / sizeof(periods[0]);

std::map<String, TrackedUUIDState> trackedUUIDs;
String activeDate = "";
String activePeriod = "";
unsigned long lastLostCheckMillis = 0;

class MyAdvertisedDeviceCallbacks : public BLEAdvertisedDeviceCallbacks {
    void onResult(BLEAdvertisedDevice advertisedDevice) override {
        String serviceUUID = advertisedDevice.getServiceUUID().toString().c_str();
        if (serviceUUID.startsWith("ADC1") || serviceUUID.startsWith("adc1")) {
            if (std::find(detectedUUIDs.begin(), detectedUUIDs.end(), serviceUUID) == detectedUUIDs.end()) {
                detectedUUIDs.push_back(serviceUUID);
            }
        }
    }
};

void initTime() {
    configTime(19800, 0, "pool.ntp.org", "time.nist.gov");

    while (true) {
        Serial.println("Waiting for NTP sync...");

        unsigned long startMillis = millis();
        time_t now = time(nullptr);
        struct tm timeinfo;

        while (now < 1700000000 && (millis() - startMillis) < NTP_SYNC_TIMEOUT_MS) {
            delay(500);
            Serial.print(".");
            now = time(nullptr);
        }

        Serial.println();

        if (now >= 1700000000 && getLocalTime(&timeinfo)) {
            char timeStr[32];
            strftime(timeStr, sizeof(timeStr), "%Y-%m-%d %H:%M:%S", &timeinfo);
            Serial.print("NTP synced time: ");
            Serial.println(timeStr);
            return;
        }

        Serial.println("NTP sync timed out, retrying...");
        delay(NTP_RETRY_DELAY_MS);
        configTime(19800, 0, "pool.ntp.org", "time.nist.gov");
    }
}

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

String getCurrentPeriod() {
    struct tm timeinfo;
    if (!getLocalTime(&timeinfo)) {
        return "unknown_period";
    }

    int nowMinutes = timeinfo.tm_hour * 60 + timeinfo.tm_min;

    for (int i = 0; i < NUM_PERIODS; i++) {
        int start = periods[i].startHour * 60 + periods[i].startMinute;
        int end = periods[i].endHour * 60 + periods[i].endMinute;
        if (nowMinutes >= start && nowMinutes < end) {
            return periods[i].name;
        }
    }

    return "no_period";
}

String buildBasePath(const String& today, const String& period, const String& uuid) {
    return "attendance/" + today + "/" + period + "/" + uuid;
}

String buildEventPath(const String& basePath, time_t timestampSeconds) {
    return basePath + "/events/" + String(static_cast<long>(timestampSeconds));
}

time_t currentUnixTimestamp() {
    return time(nullptr);
}

void printSyncedTime() {
    struct tm timeinfo;
    if (getLocalTime(&timeinfo)) {
        char timeStr[32];
        strftime(timeStr, sizeof(timeStr), "%Y-%m-%d %H:%M:%S", &timeinfo);
        Serial.print("Confirmed clock: ");
        Serial.println(timeStr);
    } else {
        Serial.println("Confirmed clock: unable to read local time");
    }
}

void writeEvent(const String& basePath, const String& type, float confidence = -1.0f) {
    time_t eventTs = currentUnixTimestamp();

    if (eventTs < 1700000000) {
        Serial.println("Skipping event write because Unix time is not confirmed");
        return;
    }

    FirebaseJson event;
    event.set("type", type);
    event.set("timestamp", static_cast<int>(eventTs));
    if (confidence >= 0.0f) {
        event.set("confidence", confidence);
    }

    String eventPath = buildEventPath(basePath, eventTs);
    if (!Firebase.RTDB.setJSON(&fbdo, eventPath.c_str(), &event)) {
        Serial.printf("Failed to write event %s: %s\n", eventPath.c_str(), fbdo.errorReason().c_str());
    }
}

void ensureFirstSeen(const String& basePath, time_t firstSeenSeconds) {
    String firstSeenPath = basePath + "/summary/first_seen";
    if (!Firebase.RTDB.getInt(&fbdo, firstSeenPath.c_str())) {
        if (!Firebase.RTDB.setInt(&fbdo, firstSeenPath.c_str(), static_cast<int>(firstSeenSeconds))) {
            Serial.printf("Failed to set first_seen %s: %s\n", firstSeenPath.c_str(), fbdo.errorReason().c_str());
        }
    }
}

void updateLastSeen(const String& basePath, time_t lastSeenSeconds) {
    String lastSeenPath = basePath + "/summary/last_seen";
    if (!Firebase.RTDB.setInt(&fbdo, lastSeenPath.c_str(), static_cast<int>(lastSeenSeconds))) {
        Serial.printf("Failed to set last_seen %s: %s\n", lastSeenPath.c_str(), fbdo.errorReason().c_str());
    }
}

void markUuidSeen(const String& today, const String& period, const String& uuid, unsigned long nowMillis, time_t nowSeconds) {
    TrackedUUIDState& state = trackedUUIDs[uuid];
    String basePath = buildBasePath(today, period, uuid);

    bool firstDetectionThisRuntime = (state.lastSeenMillis == 0);
    bool returningFromLost = state.isLost;

    if (firstDetectionThisRuntime || returningFromLost) {
        writeEvent(basePath, "ble_seen");
    }

    ensureFirstSeen(basePath, nowSeconds);
    updateLastSeen(basePath, nowSeconds);

    state.lastSeenMillis = nowMillis;
    state.isLost = false;

    Serial.printf("BLE seen: %s | first=%s | returning=%s | last_seen=%lu\n",
                  uuid.c_str(),
                  firstDetectionThisRuntime ? "yes" : "no",
                  returningFromLost ? "yes" : "no",
                  static_cast<long>(nowSeconds));
}

void checkLostUUIDs(const String& today, const String& period, unsigned long nowMillis, time_t nowSeconds) {
    for (auto& entry : trackedUUIDs) {
        const String& uuid = entry.first;
        TrackedUUIDState& state = entry.second;

        if (state.lastSeenMillis == 0 || state.isLost) {
            continue;
        }

        if ((nowMillis - state.lastSeenMillis) > LOST_THRESHOLD_MS) {
            String basePath = buildBasePath(today, period, uuid);
            writeEvent(basePath, "ble_lost");
            state.isLost = true;
            updateLastSeen(basePath, nowSeconds);

            Serial.printf("BLE lost: %s | last_seen_ms=%lu\n", uuid.c_str(), state.lastSeenMillis);
        }
    }
}

void syncPeriodState(const String& today, const String& period) {
    if (today != activeDate || period != activePeriod) {
        trackedUUIDs.clear();
        detectedUUIDs.clear();
        activeDate = today;
        activePeriod = period;
        lastLostCheckMillis = 0;
        Serial.printf("Tracking reset for %s / %s\n", today.c_str(), period.c_str());
    }
}

void setup() {
    Serial.begin(115200);

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
    printSyncedTime();

    config.api_key = API_KEY;
    config.database_url = DATABASE_URL;

    if (Firebase.signUp(&config, &auth, "", "")) {
        Serial.println("SignUp OK");
    } else {
        Serial.printf("%s\n", config.signer.signupError.message.c_str());
    }

    Firebase.begin(&config, &auth);
    Firebase.reconnectWiFi(true);

    BLEDevice::init("");
    pBLEScan = BLEDevice::getScan();
    pBLEScan->setAdvertisedDeviceCallbacks(new MyAdvertisedDeviceCallbacks());
    pBLEScan->setActiveScan(true);
    pBLEScan->setInterval(100);
    pBLEScan->setWindow(99);
}

void loop() {
    pBLEScan->start(BLE_SCAN_SECONDS, false);
    pBLEScan->clearResults();

    String today = getTodayFolder();
    String period = getCurrentPeriod();

    if (period == "no_period" || period == "unknown_period" || today == "unknown_date") {
        detectedUUIDs.clear();
        delay(1000);
        return;
    }

    syncPeriodState(today, period);

    if (Firebase.ready()) {
        unsigned long nowMillis = millis();
        time_t nowSeconds = currentUnixTimestamp();

        if (nowSeconds < 1700000000) {
            Serial.println("Skipping BLE processing because Unix time is not yet confirmed");
            detectedUUIDs.clear();
            delay(1000);
            return;
        }

        for (const String& uuid : detectedUUIDs) {
            markUuidSeen(today, period, uuid, nowMillis, nowSeconds);
        }

        if (lastLostCheckMillis == 0 || (nowMillis - lastLostCheckMillis) >= LOST_CHECK_INTERVAL_MS) {
            checkLostUUIDs(today, period, nowMillis, nowSeconds);
            lastLostCheckMillis = nowMillis;
        }
    }

    detectedUUIDs.clear();
    delay(1000);
}