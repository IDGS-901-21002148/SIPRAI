package com.example.siprais

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.google.firebase.auth.FirebaseAuth

class SessionManager(private val auth: FirebaseAuth) : LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            Log.e("SessionManager", "Error al cerrar sesión", e)
        }
    }
}