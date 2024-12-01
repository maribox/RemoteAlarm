#include <Arduino.h>
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <sys/time.h>
#include <cstring>

#define SERVICE_UUID        "b53e36d0-a21b-47b2-abac-343f523ff4d5"
#define ALARM_ARRAY_CHARACTERISTIC_UUID "a14af994-2a22-4762-b9e5-cb17a716645c"
#define LIGHT_STATE_CHARACTERISTIC_UUID "3c95cda9-7bde-471d-9c2b-ac0364befa78"
#define TIMESTAMP_CHARACTERISTIC_UUID "ab110e08-d3bb-4c8c-87a7-51d7076218cf"

#define CW_PIN 16
#define WW_PIN 17

#define PWM_CHANNEL_CW 0
#define PWM_CHANNEL_WW 1
#define PWM_FREQUENCY 100 // 1 kHz
#define PWM_RESOLUTION 8 // 8-bit resolution


BLECharacteristic* pAlarmArrayCharacteristic;
BLECharacteristic* pLightStateCharacteristic;
BLECharacteristic* pTimestampCharacteristic;

BLEAdvertising *pAdvertising;

BLEUUID alarmArrayUuid = BLEUUID(ALARM_ARRAY_CHARACTERISTIC_UUID);
BLEUUID lightStateUuid = BLEUUID(LIGHT_STATE_CHARACTERISTIC_UUID);
BLEUUID timestampUuid = BLEUUID(TIMESTAMP_CHARACTERISTIC_UUID);


// utils:
void hexPrint(std::vector<uint8_t>& bytes) {
    for (const auto& byte : bytes) {
        if (byte < 0x10) Serial.print("0");
        Serial.print(byte, HEX);
        Serial.print(" ");
    }
    Serial.println();
}

uint64_t bytesToLong(std::vector<uint8_t> bytes) {
  // uses the first 8 bytes in bytes-vector to calculate long
  uint64_t value = 0;
  for (size_t i = 0; i < 8; ++i) { // TODO what if bytes.size() < 8?
    value |= static_cast<uint64_t>(bytes[i]) << (8 * i);
  }
  return value;
}

String formatTime(const struct tm* timeDetails) {
  char buffer[100]; 
  strftime(buffer, sizeof(buffer), "%A, %B %d %Y %H:%M:%S", timeDetails);
  return String(buffer);
}

String getLocalTime(uint64_t timestamp) {
    struct timeval tv {};
    tv.tv_sec = timestamp;
    tv.tv_usec = 0;
    settimeofday(&tv, NULL);

    time_t now;
    struct tm timeDetails {};

    time(&now);
    localtime_r(&now, &timeDetails);
    return formatTime(&timeDetails);
}



int last_cw = 0;
int last_ww = 0;
int lastupdate = 0;
void updateLight() {
    auto cw = static_cast<int>(pLightStateCharacteristic->getValue()[0]);
    auto ww = static_cast<int>(pLightStateCharacteristic->getValue()[1]);
    if (last_cw == cw && last_ww == ww) {
      return;
    }
    Serial.println("Updating light to: " + String(cw) + " " + String(ww));
    if (last_cw != cw) {
      ledcWrite(PWM_CHANNEL_CW, cw);
      last_cw = cw;
    }
    if (last_ww != ww) {
      ledcWrite(PWM_CHANNEL_WW, ww);
      last_ww = ww;
    }
    // delay to avoid flickering
}

/* Alarm
 Body Size (2B) - excluding this size
 timestamp (8B)
 LightProgram - Array of Program elements with types (variable Size, see below):
Every light program is defined by a byte array.
The Header is always one byte with the TYPE of the light program. Keep in mind MTU of BLE.
    0x00: Fixed -> body size 10 bytes
        duration: 8 Bytes (long value in milliseconds)
        CW: 1 Byte (uint value)
        WW: 1 Byte (uint value)
    0x01: Ramp -> body size 10 bytes, will reach target lightState in Duration milliseconds
                  in a linear way from the current lightState
        duration: 8 Bytes (long value in milliseconds)
        CW_target: 1 Byte (uint value)
        WW_target: 1 Byte (uint value)
    0x02: Blink -> body size 16 bytes, will blink for duration milliseconds with
                   high_duration milliseconds on high and low_duration on low each interval
        blink_duration: 8 Bytes (long value in milliseconds)
        high_duration: 2 Bytes (ushort value in milliseconds)
        low_duration: 2 Bytes (ushort value in milliseconds)
        CW_high: 1 Byte (uint value)
        WW_high: 1 Byte (uint value)
        CW_low: 1 Byte (uint value)
        WW_low: 1 Byte (uint value)
 */


class AlarmArrayCharacteristicHandler: public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic* pCharacteristic) {
    Serial.println("Alarms written");
    auto pAlarmArray = pCharacteristic->getData();

    std::array<uint8_t, 2> bytes; 
    std::copy_n(pAlarmArray, 2, bytes.begin());
    u_short body_size = 0;
    std::memcpy(&body_size, bytes.data(), sizeof(body_size));
    Serial.println(body_size);
    
    std::vector<uint8_t> alarmBytes(body_size); 
    std::copy_n(pAlarmArray, body_size, alarmBytes.begin());
    hexPrint(alarmBytes);

    std::vector<uint8_t> timestampBytes(alarmBytes.begin() + 2, alarmBytes.begin() + 10);
    auto timestamp = bytesToLong(timestampBytes);
    Serial.println(getLocalTime(timestamp));
    // TODO: maybe switch size and timestamp so size only says how long the array of lightProgramElements is
    // TODO: error handling
  }

  void onRead(BLECharacteristic* pCharacteristic) {
    Serial.println("Alarms read");
  }
}; 


class LightStateCharacteristicHandler: public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic* pCharacteristic) {
    // Light flickers when updating too often. Use active waiting in main loop if too flickery
    updateLight();
    //Serial.println("LightState written.");
  }

  void onRead(BLECharacteristic* pCharacteristic) {
    Serial.println("LightState read");
  }
};

class TimestampCharacteristicHandler: public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic* pCharacteristic) {
    Serial.println("Timestamp written");

    auto pTimestampValue = pTimestampCharacteristic->getData();
    std::vector<uint8_t> timestampBytes(8);
    std::copy_n(pTimestampValue, 8, timestampBytes.begin());

    hexPrint(timestampBytes);

    auto timestamp = bytesToLong(timestampBytes);
    Serial.println(timestamp);

    getLocalTime(timestamp);
  }

  void onRead(BLECharacteristic* pCharacteristic) {
    Serial.println("Timestamp read");
  }
};

class BLEServerHandler: public BLEServerCallbacks {
  void onDisconnect(BLEServer* pServer) {
    Serial.println("Server disconnected.");
    //pServer->startAdvertising();
  }

  void onConnect(BLEServer* pServer) {
    Serial.println("Server connected.");
    pServer->startAdvertising();
  }
};

void setup() {

  // using ledc for easier control of frequency so we don't have coil whining
  ledcSetup(PWM_CHANNEL_CW, PWM_FREQUENCY, PWM_RESOLUTION);
  ledcAttachPin(CW_PIN, PWM_CHANNEL_CW);
  ledcAttachPin(WW_PIN, PWM_CHANNEL_WW);

  // set env variable to Europe/Berlin (for printing, see https://remotemonitoringsystems.ca/time-zone-abbreviations.php)
  setenv("TZ", "CET-1CEST-2,M3.5.0/02:00:00,M10.5.0/03:00:00", 1); 
  tzset();

  Serial.begin(115200);
  Serial.println("Starting BLE work!");


  BLEDevice::init("Marius' Licht");

  BLEServer *pLightServer = BLEDevice::createServer();
  pLightServer->setCallbacks(new BLEServerHandler());
  
  BLEService *pLightService = pLightServer->createService(SERVICE_UUID);
  pAlarmArrayCharacteristic = pLightService->createCharacteristic(alarmArrayUuid, BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_WRITE | BLECharacteristic::PROPERTY_WRITE_NR);
  pAlarmArrayCharacteristic->setValue({});
  pAlarmArrayCharacteristic->setCallbacks(new AlarmArrayCharacteristicHandler());
  
  pLightStateCharacteristic = pLightService->createCharacteristic(lightStateUuid, BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_WRITE | BLECharacteristic::PROPERTY_WRITE_NR);
  std::uint8_t byteArray[] = {0x00, 0x00};
  pLightStateCharacteristic->setValue(byteArray, sizeof(byteArray));
  pLightStateCharacteristic->setCallbacks(new LightStateCharacteristicHandler());
  
  pTimestampCharacteristic = pLightService->createCharacteristic(timestampUuid, BLECharacteristic::PROPERTY_WRITE | BLECharacteristic::PROPERTY_WRITE_NR);
  int64_t customTimestamp = 0;
  pTimestampCharacteristic->setValue(reinterpret_cast<std::uint8_t*>(&customTimestamp), 8);
  pTimestampCharacteristic->setCallbacks(new TimestampCharacteristicHandler());
  
  pLightService->start();

  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  pAdvertising->setMinPreferred(0x06); 
  pAdvertising->setMinPreferred(0x12);

  startAdvertising();
}

void startAdvertising() {
    BLEDevice::startAdvertising();
    Serial.println("Started advertising!");
}

void loop() {

}