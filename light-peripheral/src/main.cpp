#include <Arduino.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <esp_timer.h>
#include <sys/time.h>

#include <chrono>
#include <cstring>
#include <ctime>
#include <utility>
#include <variant>
#include <vector>

#define SERVICE_UUID "b53e36d0-a21b-47b2-abac-343f523ff4d5"
#define ADD_LIGHT_PROGRAM_CHARACTERISTIC_UUID "a14af994-2a22-4762-b9e5-cb17a716645c" // W = App writes to it
#define LIGHT_PROGRAMS_CHARACTERISTIC_UUID "265b9c95-a99d-4477-99dd-fef48fa26004"
#define LIGHT_STATE_CHARACTERISTIC_UUID "3c95cda9-7bde-471d-9c2b-ac0364befa78"
#define TIMESTAMP_CHARACTERISTIC_UUID "ab110e08-d3bb-4c8c-87a7-51d7076218cf"

#define TIMESTAMP_SIZE 8

#define CW_PIN 16
#define WW_PIN 17

#define PWM_CHANNEL_CW 0
#define PWM_CHANNEL_WW 1
#define PWM_FREQUENCY 100  // 1 kHz
#define PWM_RESOLUTION 8   // 8-bit resolution

BLECharacteristic* pAddLightProgramCharacteristic;
BLECharacteristic* pLightProgramsCharacteristic;
BLECharacteristic* pLightStateCharacteristic;
BLECharacteristic* pTimestampCharacteristic;

BLEAdvertising* pAdvertising;

BLEUUID addLightProgramsUuid = BLEUUID(ADD_LIGHT_PROGRAM_CHARACTERISTIC_UUID);
BLEUUID lightProgramsUuid = BLEUUID(LIGHT_PROGRAMS_CHARACTERISTIC_UUID);
BLEUUID lightStateUuid = BLEUUID(LIGHT_STATE_CHARACTERISTIC_UUID);
BLEUUID timestampUuid = BLEUUID(TIMESTAMP_CHARACTERISTIC_UUID);

#define DEBUG_LEVEL 4

void logError(const String& message) {
  if constexpr (DEBUG_LEVEL >= 1) {
    Serial.print("ERROR: ");
    Serial.println(message);
  }
}

void logWarning(const String& message) {
  if constexpr (DEBUG_LEVEL >= 2) {
    Serial.print("WARNING: ");
    Serial.println(message);
  }
}

void logInfo(const String& message) {
  if constexpr (DEBUG_LEVEL >= 3) {
    Serial.print("INFO: ");
    Serial.println(message);
  }
}

void logDebug(const String& message) {
  if constexpr (DEBUG_LEVEL >= 4) {
    Serial.print("DEBUG: ");
    Serial.println(message);
  }
}

// utils:
void hexPrint(std::vector<uint8_t>& bytes) {
  for (const auto& byte : bytes) {
    if (byte < 0x10) Serial.print("0");
    Serial.print(byte, HEX);
    Serial.print(" ");
  }
  Serial.println();
}

template <typename T>
T bytesToVal(std::vector<uint8_t>& bytes) {
  T value;
  if (bytes.size() < sizeof(value)) {
    logError("Got too small vector for conversion.");
    return -1;
  }
  /*
  for (size_t i = 0; i < 8; ++i) { // TODO what if bytes.size() < 8?
    value |= static_cast<uint64_t>(bytes[i]) << (8 * i);
  }
  */
  std::memcpy(&value, bytes.data(), sizeof(value));
  return value;
}

template <typename T>
T popValFront(std::vector<uint8_t>& bytes) {
  if (bytes.size() < sizeof(T)) {
    logError("Tried to read value from too small vector");
    throw std::out_of_range("Tried to read value from too small vector");
  }
  T value;
  std::memcpy(&value, bytes.data(), sizeof(T));
  bytes.erase(bytes.begin(), bytes.begin() + sizeof(T));
  return value;
}

String formatTime(const struct tm* timeDetails) {
  char buffer[100];
  strftime(buffer, sizeof(buffer), "%A, %B %d %Y %H:%M:%S", timeDetails);
  return {buffer};
}

String getLocalTime(time_t timestamp) {
  struct tm timeDetails {};
  localtime_r(&timestamp, &timeDetails);
  return formatTime(&timeDetails);
}

void setCurrentTime(time_t timestamp) {
  struct timeval tv {};
  tv.tv_sec = timestamp;
  tv.tv_usec = 0;
  settimeofday(&tv, nullptr);
}

std::pair<uint8_t, uint8_t> getLight() {
  if (pLightStateCharacteristic == nullptr) {
    logError("pLightStateCharacteristic is nullptr");
    return std::pair(0, 0);
  }

  const auto& value = pLightStateCharacteristic->getValue();
  if (value.size() < 2) {
    logError("Got too small vector for conversion.");
    return std::pair(0, 0);
  }

  return std::pair(value[0], value[1]);
}

int last_cw = 0;
int last_ww = 0;
int lastupdate = 0;
void updateLight() {
  auto [cw, ww] = getLight();

  if (last_cw == cw && last_ww == ww) {
    return;
  }
  logInfo("Updating light to: " + String(cw) + " " + String(ww));

  if (last_cw != cw) {
    // reason for the following is the same as this: https://github.com/espressif/arduino-esp32/issues/689#issuecomment-565153280 ...
    if (cw) {
      ledcWrite(PWM_CHANNEL_CW, cw);
    } else {
      ledcAttachPin(CW_PIN, PWM_CHANNEL_CW);
    }
    last_cw = cw;
  }
  if (last_ww != ww) {
    if (ww) {
      ledcWrite(PWM_CHANNEL_WW, ww);
    } else {
      ledcAttachPin(WW_PIN, PWM_CHANNEL_WW);
    }
    last_ww = ww;
  }
  // delay to avoid flickering
}

void setLight(uint8_t cw, uint8_t ww) {
  std::array<uint8_t, 2> colorValues = {cw, ww};
  pLightStateCharacteristic->setValue(&colorValues[0], 2);
  pLightStateCharacteristic->notify();
  updateLight();
}

/* Alarm
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

enum class LightProgramType : uint16_t {
  FIXED = 0,
  RAMP = 1,
  BLINK = 2
};

LightProgramType toLightProgramType(unsigned short value) {
  if (value < 0 || value > 2) {
    logError("Invalid value for LightProgramType: " + value);
    throw std::out_of_range("Invalid value for LightProgramType");
  }

  return static_cast<LightProgramType>(value);
};

struct LightActionFixed {
  uint64_t durationMs;
  uint8_t CW;
  uint8_t WW;

  LightActionFixed(uint64_t durationMs, uint8_t CW, uint8_t WW)
      : durationMs(durationMs), CW(CW), WW(WW) {
  }

  static LightActionFixed popFromBytes(std::vector<uint8_t>& bytes) {
    auto duration = popValFront<uint64_t>(bytes);
    auto CW = popValFront<uint8_t>(bytes);
    auto WW = popValFront<uint8_t>(bytes);
    return {duration, CW, WW};
  }
};

struct LightActionRamp {
  uint64_t durationMs = 30000;
  uint8_t targetCW = 255;
  uint8_t targetWW = 255;

  LightActionRamp(uint64_t durationMs, uint8_t targetCW, uint8_t targetWW)
      : durationMs(durationMs), targetCW(targetCW), targetWW(targetWW) {
  }

  LightActionRamp() = default;

  static LightActionRamp popFromBytes(std::vector<uint8_t>& bytes) {
    auto duration = popValFront<uint64_t>(bytes);
    auto targetCW = popValFront<uint8_t>(bytes);
    auto targetWW = popValFront<uint8_t>(bytes);
    return {duration, targetCW, targetWW};
  }
};

struct LightActionBlink {
  uint64_t blinkDurationMs;
  uint16_t lowDurationMs;
  uint16_t highDurationMs;
  uint8_t lowCW;
  uint8_t lowWW;
  uint8_t highCW;
  uint8_t highWW;

  LightActionBlink(
      uint64_t blinkDurationMs,
      uint16_t lowDurationMs,
      uint16_t highDurationMs,
      uint8_t lowCW,
      uint8_t lowWW,
      uint8_t highCW,
      uint8_t highWW)
      : blinkDurationMs(blinkDurationMs),
        lowDurationMs(lowDurationMs),
        highDurationMs(highDurationMs),
        lowCW(lowCW),
        lowWW(lowWW),
        highCW(highCW),
        highWW(highWW) {
  }

  static LightActionBlink popFromBytes(std::vector<uint8_t>& bytes) {
    auto blinkDuration = popValFront<uint64_t>(bytes);
    auto lowDuration = popValFront<uint16_t>(bytes);
    auto highDuration = popValFront<uint16_t>(bytes);
    auto lowCW = popValFront<uint8_t>(bytes);
    auto lowWW = popValFront<uint8_t>(bytes);
    auto highCW = popValFront<uint8_t>(bytes);
    auto highWW = popValFront<uint8_t>(bytes);
    return {blinkDuration, lowDuration, highDuration, lowCW, lowWW, highCW, highWW};
  };
};

using LightProgramAction = std::variant<LightActionFixed, LightActionRamp, LightActionBlink>;

struct SpecificMoment {
  time_t time = {};

  explicit SpecificMoment(time_t t)
      : time(t) {
  }
  SpecificMoment() = default;
};

enum class DayOfWeek {
  Monday = 0,
  Tuesday,
  Wednesday,
  Thursday,
  Friday,
  Saturday,
  Sunday
};

struct WeekdaysWithLocalTime {
  std::vector<DayOfWeek> days = {};
  std::tm time = {};
};

using Schedule = std::variant<SpecificMoment, WeekdaysWithLocalTime>;

struct LightProgram {
  Schedule schedule = SpecificMoment{};
  std::vector<LightProgramAction> actions = {};

  LightProgram() = default;
  explicit LightProgram(Schedule s)
      : schedule(std::move(s)) {};
};

void printAlarm(const LightProgram& lightProgram) {
  logInfo("LightProgram Action: ");

  logInfo("Schedule Type: ");
  std::visit([]([[maybe_unused]] const auto& schedule) {
    using T = std::decay_t<decltype(schedule)>;
    if constexpr (std::is_same_v<T, SpecificMoment>) {
      logInfo("Specific Timestamp");
    } else if constexpr (std::is_same_v<T, WeekdaysWithLocalTime>) {
      logInfo("Weekdays With Local Time");
    }
  },lightProgram.schedule);
}

bool operator==(const SpecificMoment& lhs, const SpecificMoment& rhs) {
  return lhs.time == rhs.time;
}

bool operator==(const WeekdaysWithLocalTime& lhs, const WeekdaysWithLocalTime& rhs) {
  return lhs.days == rhs.days && std::memcmp(&lhs.time, &rhs.time, sizeof(std::tm)) == 0;
}

bool operator==(const Schedule& lhs, const Schedule& rhs) {
  return lhs.index() == rhs.index() && std::visit([](const auto& l, const auto& r) { return l == r; }, lhs, rhs);
}

bool operator==(const LightActionFixed& lhs, const LightActionFixed& rhs) {
  return lhs.durationMs == rhs.durationMs && lhs.CW == rhs.CW && lhs.WW == rhs.WW;
}

bool operator==(const LightActionRamp& lhs, const LightActionRamp& rhs) {
  return lhs.durationMs == rhs.durationMs && lhs.targetCW == rhs.targetCW && lhs.targetWW == rhs.targetWW;
}

bool operator==(const LightActionBlink& lhs, const LightActionBlink& rhs) {
  return lhs.blinkDurationMs == rhs.blinkDurationMs &&
         lhs.lowDurationMs == rhs.lowDurationMs &&
         lhs.highDurationMs == rhs.highDurationMs &&
         lhs.lowCW == rhs.lowCW &&
         lhs.lowWW == rhs.lowWW &&
         lhs.highCW == rhs.highCW &&
         lhs.highWW == rhs.highWW;
}

bool operator==(const LightProgramAction& lhs, const LightProgramAction& rhs) {
  return lhs.index() == rhs.index() && std::visit([](const auto& l, const auto& r) { return l == r; }, lhs, rhs);
}

bool operator==(const LightProgram& lhs, const LightProgram& rhs) {
  return lhs.schedule == rhs.schedule && lhs.actions == rhs.actions;
}

std::vector<LightProgram> lightPrograms{};
esp_timer_handle_t alarm_timer;

class AddLightProgramCharacteristicHandler final : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic* pCharacteristic) override {
    auto bodySize = pCharacteristic->getLength();
    logDebug("LightPrograms written with length " + String(bodySize));

    auto pLightPrograms = pCharacteristic->getData();

    std::vector<uint8_t> lightProgramBytes(bodySize);
    std::copy_n(pLightPrograms, bodySize, lightProgramBytes.begin());
    std::vector<uint8_t> originalLightProgramBytes(lightProgramBytes);

    hexPrint(lightProgramBytes);
    auto timestamp = static_cast<time_t>(popValFront<uint64_t>(lightProgramBytes));

    logInfo("Adding lightProgram at " + getLocalTime(timestamp));
    LightProgram lightProgram{SpecificMoment(timestamp)};

    while (!lightProgramBytes.empty()) {
      auto type = popValFront<uint8_t>(lightProgramBytes);

      Serial.printf("Found new Element with type %d\n", type);
      switch (toLightProgramType(type)) {
        case LightProgramType::FIXED:
          lightProgram.actions.emplace_back(LightActionFixed::popFromBytes(lightProgramBytes));
          break;
        case LightProgramType::RAMP: {
          auto ramp = LightActionRamp{};
          ramp = LightActionRamp::popFromBytes(lightProgramBytes);
          if (ramp.durationMs == 0) {
            logWarning("Received invalid duration for ramp, aborting.");
            return;
          }
          lightProgram.actions.emplace_back(ramp);
          break;
        }
        case LightProgramType::BLINK:
          lightProgram.actions.emplace_back(LightActionBlink::popFromBytes(lightProgramBytes));
          break;
        default:
          break;
      }
    }

    // TODO: remove when using timer?
    if (const auto it = std::find(lightPrograms.begin(), lightPrograms.end(), lightProgram); it == lightPrograms.end()) {  // ensure we're not adding the same lightProgram twice
      lightPrograms.push_back(lightProgram);
      // TODO set value to all alarms
      pLightProgramsCharacteristic->setValue(originalLightProgramBytes.data(), originalLightProgramBytes.size());
      pLightProgramsCharacteristic->indicate();
      logDebug("Updated list of LightPrograms.");
    }

    pCharacteristic->setValue("");
    // TODO: error handling
  }

  void onRead(BLECharacteristic* pCharacteristic) override {
    logDebug("Alarms read");
  }
};

class LightProgramsCharacteristicHandler final : public BLECharacteristicCallbacks {
  void onRead(BLECharacteristic *pCharacteristic) override {

  }
};

class LightStateCharacteristicHandler : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic* pCharacteristic) override {
    // Light flickers when updating too often. Use active waiting in main loop if too flickery
    updateLight();
    // Serial.println("LightState written.");
  }

  void onRead(BLECharacteristic* pCharacteristic) override {
    logDebug("LightState read");
  }
};

class TimestampCharacteristicHandler : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic* pCharacteristic) override {
    logDebug("Timestamp written with length " + String(pCharacteristic->getLength()));

    auto pTimestampValue = pTimestampCharacteristic->getData();
    std::vector<uint8_t> timestampBytes(8);
    std::copy_n(pTimestampValue, 8, timestampBytes.begin());

    auto timestamp = bytesToVal<time_t>(timestampBytes);
    setCurrentTime(timestamp);
    logInfo("Current time: " + getLocalTime(timestamp));
  }

  void onRead(BLECharacteristic* pCharacteristic) override {
    logDebug("Timestamp read");
  }
};

class BLEServerHandler : public BLEServerCallbacks {
  void onDisconnect(BLEServer* pServer) override {
    logInfo("Server disconnected.");
    // pServer->startAdvertising();
  }

  void onConnect(BLEServer* pServer) override {
    logInfo("Server connected.");
    pServer->startAdvertising();
  }
};

void startAdvertising() {
  BLEDevice::startAdvertising();
  logInfo("Started advertising!");
}


uint64_t getCurrentUsecUTC() {
    struct timeval current {};
    gettimeofday(&current, nullptr);
    return 1'000'000 * static_cast<uint64_t>(current.tv_sec) + current.tv_usec;
}

void printAction(const LightProgramAction& action) {
  std::visit([](const auto& action) {
    using T = std::decay_t<decltype(action)>;
    if constexpr (std::is_same_v<T, LightActionFixed>) {
      Serial.printf("Fixed action - %d %d with duration %lums\n", action.CW, action.WW, action.durationMs);
    } else if constexpr (std::is_same_v<T, LightActionRamp>) {
      Serial.printf("Ramp action - %d %d with duration %lums\n", action.targetCW, action.targetWW, action.durationMs);
    } else if constexpr (std::is_same_v<T, LightActionBlink>) {
      Serial.printf("Blink action - %dms on, %dms off with duration %lums\n", action.highDurationMs, action.lowDurationMs, action.blinkDurationMs);
    }
  },action);
}

void executeLightProgram(LightProgram lightProgram) {
  // TODO: Think about async? Maybe start lightProgram as new thread (maybe interrupt if new one comes in)
  for (auto& action : lightProgram.actions) {
    std::visit([](const auto& action) {
      Serial.print("Starting ");
      printAction(action);
      using T = std::decay_t<decltype(action)>;
      if constexpr (std::is_same_v<T, LightActionFixed>) {
        setLight(action.CW, action.WW);
        delay(action.durationMs);
      } else if constexpr (std::is_same_v<T, LightActionRamp>) {
        uint64_t start_usec = getCurrentUsecUTC();
        uint64_t now_usec, progress_usec;
        double progress_percent;
        auto [startCW, startWW] = getLight();
        auto diffCW = action.targetCW - startCW;
        auto diffWW = action.targetWW - startWW;
        uint8_t newCW, newWW;
        uint64_t rampDurationUsec = action.durationMs *1000;

        do {
          now_usec = getCurrentUsecUTC();
          progress_usec = now_usec - start_usec;
          progress_percent = static_cast<double>(progress_usec) / (action.durationMs * 1000);
          newCW = startCW + progress_percent * diffCW;
          newWW = startWW + progress_percent * diffWW;
          setLight(newCW, newWW);
        } while (progress_usec < rampDurationUsec && progress_percent >= 0);

      } else if constexpr (std::is_same_v<T, LightActionBlink>) {
        auto [startCW, startWW] = getLight();
        auto start_usec = getCurrentUsecUTC();
        auto end_usec = start_usec + action.blinkDurationMs * 1000;
        while (getCurrentUsecUTC() < end_usec) {
          setLight(action.highCW, action.highWW);
          delay(std::min(static_cast<uint64_t>(action.highDurationMs), (end_usec - getCurrentUsecUTC())/1000));
          setLight(action.lowCW, action.lowWW);
          delay(std::min(static_cast<uint64_t>(action.lowDurationMs), (end_usec - getCurrentUsecUTC())/1000));
        }
        setLight(startCW, startWW);
      }
    },action);
  }
}

void init_characteristics(BLEService *pLightService) {
  pAddLightProgramCharacteristic = pLightService->createCharacteristic(addLightProgramsUuid, BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_WRITE | BLECharacteristic::PROPERTY_WRITE_NR);
  pAddLightProgramCharacteristic->setValue({});
  pAddLightProgramCharacteristic->setCallbacks(new AddLightProgramCharacteristicHandler());

  pLightProgramsCharacteristic = pLightService->createCharacteristic(lightProgramsUuid, BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_INDICATE);
  pLightProgramsCharacteristic->setValue({});
  pLightProgramsCharacteristic->setCallbacks(new LightProgramsCharacteristicHandler()); // TODO needed?

  pLightStateCharacteristic = pLightService->createCharacteristic(lightStateUuid, BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_WRITE | BLECharacteristic::PROPERTY_WRITE_NR | BLECharacteristic::PROPERTY_NOTIFY);
  std::uint8_t byteArray[] = {0x00, 0x00};
  pLightStateCharacteristic->setValue(byteArray, sizeof(byteArray));
  pLightStateCharacteristic->setCallbacks(new LightStateCharacteristicHandler());

  pTimestampCharacteristic = pLightService->createCharacteristic(timestampUuid, BLECharacteristic::PROPERTY_WRITE | BLECharacteristic::PROPERTY_WRITE_NR);
  uint64_t customTimestamp = 0;
  pTimestampCharacteristic->setValue(reinterpret_cast<std::uint8_t*>(&customTimestamp), 8);
  pTimestampCharacteristic->setCallbacks(new TimestampCharacteristicHandler());
}

void init_advertising() {
  BLEAdvertising* pAdvertising = BLEDevice::getAdvertising();

  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  pAdvertising->setMinPreferred(0x06);
  pAdvertising->setMinPreferred(0x12);

  startAdvertising();
}


void init_server(BLEServer *&pLightServer) {
  pLightServer = BLEDevice::createServer();
  pLightServer->setCallbacks(new BLEServerHandler());
}

void setup() {
  // using ledc for easier control of frequency so we don't have coil whining
  ledcSetup(PWM_CHANNEL_CW, PWM_FREQUENCY, PWM_RESOLUTION);
  ledcAttachPin(CW_PIN, PWM_CHANNEL_CW);
  ledcAttachPin(WW_PIN, PWM_CHANNEL_WW);

  // set env variable to Europe/Berlin (for printing, see https://remotemonitoringsystems.ca/time-zone-abbreviations.php)
  setenv("TZ", "CET-1CEST-2,M3.5.0/02:00:00,M10.5.0/03:00:00", 1);
  tzset();

  Serial.begin(115200);
  logInfo("Starting BLE work!");

  BLEDevice::setMTU(512);  // needs to be set on android side too for some reason

  BLEDevice::init("Marius' Licht");

  BLEServer *pLightServer;
  init_server(pLightServer);

  BLEService* pLightService = pLightServer->createService(SERVICE_UUID);
  init_characteristics(pLightService);
  pLightService->start();

  init_advertising();
}

void loop() {
  // TODO convert to use with timer library.
  for (auto& lightProgram : lightPrograms) {
    // differenciate between types
    std::visit([&lightProgram](const auto& schedule) {
      using T = std::decay_t<decltype(schedule)>;
      if constexpr (std::is_same_v<T, SpecificMoment>) {
        auto timestamp = static_cast<uint64_t>(schedule.time);
        Serial.println("---------------------");
        Serial.println("LightProgram at: " + getLocalTime(static_cast<time_t>(timestamp)));
        for (auto& action : lightProgram.actions) {
          printAction(action);
        }

        time_t now;
        time(&now);

        if (timestamp <= now) {
          logInfo("lies in the past. Executing.");
          executeLightProgram(lightProgram);

          auto it = std::find(lightPrograms.begin(), lightPrograms.end(), lightProgram);
          if (it != lightPrograms.end()) {
            lightPrograms.erase(it);
          }
        } else {
          logInfo("lies in the future");
        }
      } else if constexpr (std::is_same_v<T, WeekdaysWithLocalTime>) {
        auto weekdays = static_cast<WeekdaysWithLocalTime>(schedule);
        // Print the weekdays...
      }
    },
               lightProgram.schedule);
  }
  delay(1000);
}

