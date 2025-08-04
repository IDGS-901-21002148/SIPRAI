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
fun HistorialScreen(
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

@Composable
fun RegistroHistorialItem(registro: RegistroHistorial) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = registro.fechaFormateada(),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.Gray
                )

                if (
                    registro.reporte.contains("activado", true) ||
                    registro.reporte.contains("fuego", true) ||
                    registro.reporte.contains("gas", true)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (registro.reporte.contains("fuego", true)) Color(0xFFFF5252)
                                else Color(0xFFFFA726),
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (registro.reporte.contains("fuego", true)) "ALERTA" else "EVENTO",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = registro.reporte,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black
            )
        }
    }
}

data class RegistroHistorial(
    val fecha: Date,
    val reporte: String
) {
    fun fechaFormateada(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(fecha)
    }
}
