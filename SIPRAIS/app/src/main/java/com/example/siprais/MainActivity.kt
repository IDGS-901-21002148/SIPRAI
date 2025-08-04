package com.example.siprais

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.siprais.ui.theme.SIPRAISTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FirebaseApp.initializeApp(this)

        // Solicitar permiso para notificaciones en Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        auth = Firebase.auth
        Firebase.database("https://projectsiprai-default-rtdb.firebaseio.com/").also {
            Log.d("Firebase", "Database initialized")
        }

        sessionManager = SessionManager(auth)
        lifecycle.addObserver(sessionManager)

        // Obtener token de Firebase Cloud Messaging
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Log.d("FCM_TOKEN", "Token: $token")
                } else {
                    Log.e("FCM_TOKEN", "Error al obtener token", task.exception)
                }
            }

        setContent {
            SIPRAISTheme {
                val navController = rememberNavController()
                var isUserLoggedIn by remember { mutableStateOf(auth.currentUser != null) }

                LaunchedEffect(key1 = auth) {
                    auth.addAuthStateListener { firebaseAuth ->
                        isUserLoggedIn = firebaseAuth.currentUser != null
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = if (isUserLoggedIn) "menu" else "login"
                ) {
                    composable("login") {
                        LoginScreen(
                            onNavigateToRegister = { navController.navigate("register") },
                            onNavigateToForgot = { navController.navigate("forgot") },
                            onLoginSuccess = {
                                navController.navigate("menu") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("register") {
                        CrearUsuarioScreen(
                            onBackClicked = { navController.popBackStack() },
                            onRegisterSuccess = {
                                navController.navigate("menu") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("forgot") {
                        OlvidarScreen(
                            onBackClicked = { navController.popBackStack() }
                        )
                    }

                    composable("menu") {
                        MenuScreen(
                            onActivitySelected = {
                                navController.navigate("actividad") {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onEmergencySelected = {
                                navController.navigate("emergencia") {
                                    launchSingleTop = true
                                }
                            },
                            onHistorySelected = {
                                navController.navigate("historial") {
                                    launchSingleTop = true
                                }
                            },
                            onLogout = {
                                lifecycleScope.launch {
                                    try {
                                        auth.signOut()
                                        navController.navigate("login") {
                                            popUpTo(0)
                                        }
                                    } catch (e: Exception) {
                                        Log.e("Auth", "Logout error", e)
                                    }
                                }
                            }
                        )
                    }

                    composable("emergencia") {
                        EmergenciaScreen(
                            onBackClicked = { navController.popBackStack() }
                        )
                    }

                    composable("actividad") {
                        ActividadScreenWithMQTT(
                            onBackClicked = { navController.popBackStack() }
                        )
                    }

                    composable("historial") {
                        HistorialScreenFirebase( // ← CORREGIDO
                            onBackClicked = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
