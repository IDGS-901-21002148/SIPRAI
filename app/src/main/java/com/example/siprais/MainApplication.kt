package com.example.siprais

import android.app.Application
import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.siprais.mqtt.MqttManager
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Inicializar Firebase
        FirebaseApp.initializeApp(this)

        // Establecer idioma en español para Firebase Auth
        Firebase.auth.setLanguageCode("es")

        // Iniciar escucha MQTT global


        // Observar ciclo de vida global para cierre de sesión automático
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            SessionManager(Firebase.auth)
        )
    }
}
