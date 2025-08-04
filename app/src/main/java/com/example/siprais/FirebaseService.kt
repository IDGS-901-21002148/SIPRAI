package com.example.siprais

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
object FirebaseService {
    private val database = FirebaseDatabase.getInstance("https://projectsiprai-default-rtdb.firebaseio.com/")
    private val auth = FirebaseAuth.getInstance()

    fun escribirDatoDePrueba() {
        val user = auth.currentUser
        if (user != null) {
            val ref = database.getReference("usuarios/${user.uid}/prueba")
            val dato = mapOf(
                "mensaje" to "Esto es un dato de prueba",
                "timestamp" to System.currentTimeMillis()
            )
            ref.setValue(dato)
                .addOnSuccessListener {
                    Log.d("FirebaseService", "Dato de prueba guardado con éxito.")
                }
                .addOnFailureListener { error ->
                    Log.e("FirebaseService", "Error al guardar dato de prueba", error)
                }
        } else {
            Log.e("FirebaseService", "Usuario no autenticado.")
        }
    }

    fun guardarDato(sensor: String, valor: String) {
        val user = auth.currentUser
        if (user != null) {
            val ref = database.getReference("usuarios/${user.uid}/historial")
            val registro = mapOf(
                "sensor" to sensor,
                "valor" to valor,
                "timestamp" to System.currentTimeMillis()
            )
            ref.push().setValue(registro)
                .addOnSuccessListener {
                    Log.d("FirebaseService", "Dato $sensor guardado correctamente: $valor")
                }
                .addOnFailureListener { e ->
                    Log.e("FirebaseService", "Error al guardar en Firebase", e)
                }
        } else {
            Log.e("FirebaseService", "Usuario no autenticado.")
        }
    }


}
