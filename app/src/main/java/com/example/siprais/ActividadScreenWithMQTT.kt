package com.example.siprais
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import com.example.siprais.mqtt.MqttManager
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.*

@Composable
fun ActividadScreenWithMQTT(onBackClicked: () -> Unit = {}) {
    val context = LocalContext.current

    var temperatura by remember { mutableStateOf(0) }
    var humedad by remember { mutableStateOf(0) }
    var gasDetectado by remember { mutableStateOf(false) }
    var fuegoDetectado by remember { mutableStateOf(false) }
    var ventiladorActivaciones by remember { mutableStateOf(0) }
    var bombaAguaActivaciones by remember { mutableStateOf(0) }

    val mqttManager = remember { MqttManager() }

    // Escucha de activaciones desde Firebase
    LaunchedEffect(Unit) {
        val database = Firebase.database("https://projectsiprai-default-rtdb.firebaseio.com/").reference

        database.child("activaciones/ventilador").addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                ventiladorActivaciones = snapshot.getValue(Int::class.java) ?: 0
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Log.e("Firebase", "Error al leer activaciones ventilador", error.toException())
            }
        })

        database.child("activaciones/bomba").addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                bombaAguaActivaciones = snapshot.getValue(Int::class.java) ?: 0
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Log.e("Firebase", "Error al leer activaciones bomba", error.toException())
            }
        })
    }

    // Escucha de mensajes MQTT
    LaunchedEffect(Unit) {
        mqttManager.setOnMessageReceived { topic, message ->
            when (topic) {
                "esp32/sensors/temperature" -> {
                    temperatura = message.toFloatOrNull()?.toInt() ?: temperatura
                }
                "esp32/sensors/humidity" -> {
                    humedad = message.toFloatOrNull()?.toInt() ?: humedad
                }
                "esp32/sensors/gas" -> {
                    val gas = message.toIntOrNull() ?: 0
                    gasDetectado = gas >= 70
                    if (gasDetectado) {
                        mostrarNotificacionLocal(context, "⚠️ Nivel de gas alto", "Se detectó gas por encima del límite seguro.")
                    }
                }
                "esp32/sensors/flame" -> {
                    fuegoDetectado = message == "1"
                    if (fuegoDetectado) {
                        mostrarNotificacionLocal(context, "🔥 FUEGO DETECTADO", "Se ha detectado fuego en el sistema.")
                    }
                }
                "esp32/alertas" -> {
                    val mensaje = message.trim()
                    Log.d("MQTT_ALERTAS", "Mensaje alerta recibido: $mensaje")

                    when {
                        mensaje.contains("Bomba de agua ACTIVADA", true) -> {
                            guardarActivacionFirebase("bomba")
                            guardarEnHistorial(mensaje)
                            mostrarNotificacionLocal(context, "💧 Bomba de agua ACTIVADA", "La bomba se ha encendido automáticamente.")
                        }
                        mensaje.contains("Ventilador ENCENDIDO", true) -> {
                            guardarActivacionFirebase("ventilador")
                            guardarEnHistorial(mensaje)
                        }
                        mensaje.contains("FUEGO", true) ||
                                mensaje.contains("Fuego controlado", true) ||
                                mensaje.contains("Ventilador APAGADO", true) ||
                                mensaje.contains("Bomba de agua DESACTIVADA", true) -> {
                            guardarEnHistorial(mensaje)
                        }
                    }
                }
            }
        }

        mqttManager.connect(
            onSuccess = { Log.d("MQTT", "Conectado exitosamente") },
            onFailure = { Log.e("MQTT", "Error al conectar", it) }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            mqttManager.cleanup()
        }
    }

    ActividadScreen(
        temperatura = temperatura,
        humedad = humedad,
        gasDetectado = gasDetectado,
        fuegoDetectado = fuegoDetectado,
        ventiladorActivaciones = ventiladorActivaciones,
        bombaAguaActivaciones = bombaAguaActivaciones,
        onBackClicked = onBackClicked
    )
}

// Guardar mensaje en historial
fun guardarEnHistorial(mensaje: String) {
    val database = Firebase.database("https://projectsiprai-default-rtdb.firebaseio.com/")
    val ref = database.getReference("historial_alertas")
    val id = ref.push().key ?: UUID.randomUUID().toString()
    val data = mapOf(
        "mensaje" to mensaje,
        "timestamp" to System.currentTimeMillis()
    )
    ref.child(id).setValue(data)
}

// Incrementar contador en Firebase
fun guardarActivacionFirebase(dispositivo: String) {
    val ref = Firebase.database("https://projectsiprai-default-rtdb.firebaseio.com/")
        .getReference("activaciones/$dispositivo")

    ref.get().addOnSuccessListener { snapshot ->
        val valor = snapshot.getValue(Int::class.java) ?: 0
        ref.setValue(valor + 1)
    }.addOnFailureListener {
        Log.e("Firebase", "No se pudo leer $dispositivo", it)
    }
}

// Mostrar notificación local automática
fun mostrarNotificacionLocal(context: Context, titulo: String, mensaje: String) {
    val channelId = "alertas_locales_siprais"
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val canal = NotificationChannel(
            channelId,
            "Alertas Locales SIPRAIS",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(canal)
    }

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.fuego)
        .setContentTitle(titulo)
        .setContentText(mensaje)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()

    notificationManager.notify(System.currentTimeMillis().toInt(), notification)
}
