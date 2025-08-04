package com.example.siprais

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreenFirebase(
    onBackClicked: () -> Unit
) {
    val historial = remember { mutableStateListOf<RegistroHistorial>() }
    val context = LocalContext.current

    // Cargar historial desde Firebase
    LaunchedEffect(Unit) {
        val database = Firebase.database("https://projectsiprai-default-rtdb.firebaseio.com/")
        val ref = database.getReference("historial_alertas")

        ref.get().addOnSuccessListener { snapshot ->
            historial.clear()
            snapshot.children.forEach { child ->
                val mensaje = child.child("mensaje").getValue(String::class.java) ?: ""
                val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L
                historial.add(
                    RegistroHistorial(
                        fecha = Date(timestamp),
                        reporte = mensaje
                    )
                )
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Historial de Eventos",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFE53935)
                ),
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Regresar",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        content = { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                items(historial.reversed()) { registro ->
                    RegistroHistorialItem(registro = registro)
                    Divider(color = Color.LightGray, thickness = 0.5.dp)
                }
            }
        }
    )
}
