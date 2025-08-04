// Archivo: MqttManager.kt
package com.example.siprais.mqtt
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.siprais.R

import android.util.Log
import kotlinx.coroutines.*
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

class   MqttManager {

    // Configuración MQTT (misma que tu ESP32)
    private val serverUri = "ssl://1e0ea9fccdc64b7998f051a223663591.s1.eu.hivemq.cloud:8883"
    private val clientId = "AndroidClient_${System.currentTimeMillis()}"
    private val username = "SipraiIDGS901"
    private val password = "SipraiIDGS901"

    // Topics
    private val topicControl = "esp32/control"
    private val topicTemp = "esp32/sensors/temperature"
    private val topicHum = "esp32/sensors/humidity"
    private val topicGas = "esp32/sensors/gas"
    private val topicFlame = "esp32/sensors/flame"

    private var mqttClient: MqttClient? = null
    private var onMessageReceived: ((String, String) -> Unit)? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Callback para recibir mensajes
    fun setOnMessageReceived(callback: (topic: String, message: String) -> Unit) {
        onMessageReceived = callback
    }

    suspend fun connect(onSuccess: () -> Unit = {}, onFailure: (Throwable) -> Unit = {}) {
        withContext(Dispatchers.IO) {
            try {
                val persistence = MemoryPersistence()
                mqttClient = MqttClient(serverUri, clientId, persistence)

                val options = MqttConnectOptions().apply {
                    userName = username
                    password = this@MqttManager.password.toCharArray()
                    isCleanSession = true
                    connectionTimeout = 30
                    keepAliveInterval = 60

                    // Configurar SSL
                    socketFactory = createInsecureSSLSocketFactory()
                }

                mqttClient?.setCallback(object : MqttCallback {
                    override fun connectionLost(cause: Throwable?) {
                        Log.e("MQTT", "Conexión perdida", cause)
                    }

                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        val msg = message?.toString() ?: ""
                        Log.d("MQTT", "Mensaje recibido en $topic: $msg")
                        scope.launch {
                            withContext(Dispatchers.Main) {
                                onMessageReceived?.invoke(topic ?: "", msg)
                            }
                        }
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {
                        Log.d("MQTT", "Mensaje enviado correctamente")
                    }
                })

                mqttClient?.connect(options)
                Log.d("MQTT", "Conectado exitosamente")

                // Suscribirse a los topics de sensores
                subscribeToSensors()

                withContext(Dispatchers.Main) {
                    onSuccess()
                }

            } catch (e: Exception) {
                Log.e("MQTT", "Error al conectar", e)
                withContext(Dispatchers.Main) {
                    onFailure(e)
                }
            }
        }
    }
    private fun showAlertNotification(context: Context, title: String, message: String) {
        val channelId = "alert_channel"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alertas del sistema",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.fuego) // Usa un ícono de alerta (está en tu carpeta drawable)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun subscribeToSensors() {
        try {
            val topics = arrayOf(
                topicTemp, topicHum, topicGas, topicFlame,
                "esp32/alertas"
            )
            val qos = IntArray(topics.size) { 1 } // ← Ahora los tamaños coinciden

            mqttClient?.subscribe(topics, qos)
            Log.d("MQTT", "Suscrito a todos los sensores")
        } catch (e: Exception) {
            Log.e("MQTT", "Error al suscribirse a sensores", e)
        }
    }

    // Funciones para controlar dispositivos
    fun encenderVentilador() {
        publishMessage(topicControl, "ventilador_on")
    }

    fun apagarVentilador() {
        publishMessage(topicControl, "ventilador_off")
    }

    fun encenderBomba() {
        publishMessage(topicControl, "bomba_on")
    }

    fun apagarBomba() {
        publishMessage(topicControl, "bomba_off")
    }

    private fun publishMessage(topic: String, message: String) {
        scope.launch {
            try {
                if (mqttClient?.isConnected == true) {
                    val mqttMessage = MqttMessage(message.toByteArray())
                    mqttMessage.qos = 1
                    mqttClient?.publish(topic, mqttMessage)
                    Log.d("MQTT", "Mensaje enviado a $topic: $message")
                } else {
                    Log.w("MQTT", "Cliente no conectado, no se puede enviar mensaje")
                }
            } catch (e: Exception) {
                Log.e("MQTT", "Error al enviar mensaje", e)
            }
        }
    }

    fun disconnect() {
        scope.launch {
            try {
                mqttClient?.disconnect()
                mqttClient?.close()
                Log.d("MQTT", "Desconectado de MQTT")
            } catch (e: Exception) {
                Log.e("MQTT", "Error al desconectar", e)
            }
        }
    }

    fun isConnected(): Boolean {
        return mqttClient?.isConnected ?: false
    }

    // Función para crear SSL Socket Factory inseguro (solo para desarrollo)
    private fun createInsecureSSLSocketFactory(): SSLSocketFactory {
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
        )

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        return sslContext.socketFactory
    }

    fun cleanup() {
        scope.cancel()
        disconnect()
    }
}