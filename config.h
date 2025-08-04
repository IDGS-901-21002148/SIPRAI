#ifndef CONFIG_H
#define CONFIG_H

// --- WiFi ---
const char* WIFI_SSID = "INFINITUM6B2C";
const char* WIFI_PASSWORD = "HJ7bu4aGJT";

// --- MQTT ---
const char* MQTT_SERVER = "1e0ea9fccdc64b7998f051a223663591.s1.eu.hivemq.cloud";
const int MQTT_PORT = 8883;
const char* MQTT_USER = "SipraiIDGS901";
const char* MQTT_PASS = "SipraiIDGS901";

const char* MQTT_TOPIC_CONTROL   = "esp32/control";
const char* MQTT_TOPIC_TEMP      = "esp32/sensors/temperature";
const char* MQTT_TOPIC_HUM       = "esp32/sensors/humidity";
const char* MQTT_TOPIC_GAS       = "esp32/sensors/gas";
const char* MQTT_TOPIC_FLAME     = "esp32/sensors/flame";
const char* MQTT_TOPIC_ALERTAS   = "esp32/alertas";

// --- Pines ---
#define BAUD_RATE           115200
#define PIN_RELE_VENTILADOR 25
#define PIN_RELE_BOMBA      26
#define PIN_MQ2_GAS         34
#define PIN_SENSOR_FLAMA    35
#define PIN_TOUCH_BUTTON    27
#define PIN_BUZZER          33

// --- Umbrales ---
#define GAS_HISTERESIS 5

struct Limites {
  int gas_alto_porcentaje;
};

Limites limites_actuales = {
  .gas_alto_porcentaje = 40
};

#endif
