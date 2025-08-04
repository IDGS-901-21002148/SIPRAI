package com.example.siprais

import com.example.siprais.FirebaseService
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.siprais.mqtt.MqttManager
import com.example.siprais.ui.theme.SIPRAISTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergenciaScreen(
    onBackClicked: () -> Unit = {}
) {
    val context = LocalContext.current
    var mqttManager by remember { mutableStateOf<MqttManager?>(null) }
    var isConnected by remember { mutableStateOf(false) }
    var ventiladorState by remember { mutableStateOf(false) }
    var bombaState by remember { mutableStateOf(false) }

    // Botón de prueba
    Button(onClick = {
        FirebaseService.escribirDatoDePrueba()
    }) {
        Text("Enviar dato de prueba")
    }

    // Inicializar MQTT
    LaunchedEffect(Unit) {
        mqttManager = MqttManager().apply {
            setOnMessageReceived { topic, message ->
                when (topic) {
                    "esp32/sensors/temperature" -> FirebaseService.guardarDato("temperatura", message)
                    "esp32/sensors/humidity" -> FirebaseService.guardarDato("humedad", message)
                    "esp32/sensors/gas" -> FirebaseService.guardarDato("gas", message)
                    "esp32/sensors/flame" -> FirebaseService.guardarDato("flama", message)
                }
            }

            connect(
                onSuccess = { isConnected = true },
                onFailure = { isConnected = false }
            )
        }
    }

    // Limpiar MQTT al salir
    DisposableEffect(Unit) {
        onDispose {
            mqttManager?.cleanup()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "SIPRAI - Emergencia",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Regresar",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFE53935)
                )
            )
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFFF5F5F5))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                EmergencyStatusCard(isConnected = isConnected)

                Spacer(modifier = Modifier.height(8.dp))

                EmergencyToggleButton(
                    icon = Icons.Default.Air,
                    text = if (ventiladorState) "Apagar Ventilador" else "Encender Ventilador",
                    isActive = ventiladorState,
                    isEnabled = isConnected,
                    onClick = {
                        if (ventiladorState) mqttManager?.apagarVentilador()
                        else mqttManager?.encenderVentilador()
                        ventiladorState = !ventiladorState
                    },
                    activeColor = Color(0xFF4CAF50),
                    inactiveColor = Color(0xFF757575)
                )

                EmergencyToggleButton(
                    icon = Icons.Default.WaterDrop,
                    text = if (bombaState) "Apagar Riego" else "Encender Riego",
                    isActive = bombaState,
                    isEnabled = isConnected,
                    onClick = {
                        if (bombaState) mqttManager?.apagarBomba()
                        else mqttManager?.encenderBomba()
                        bombaState = !bombaState
                    },
                    activeColor = Color(0xFF2196F3),
                    inactiveColor = Color(0xFF757575)
                )

                Text(
                    text = if (isConnected) {
                        "Conectado al sistema IoT\nActive solo los sistemas necesarios"
                    } else {
                        "Conectando al sistema IoT...\nEspere un momento"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isConnected) Color.Gray else Color(0xFFFF5722),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    )
}

@Composable
fun EmergencyStatusCard(isConnected: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Alerta",
                tint = Color(0xFFFF5252),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "CONTROLES DE EMERGENCIA",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE53935)
            )
            Text(
                text = if (isConnected) "Sistema conectado" else "Conectando...",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isConnected) Color(0xFF4CAF50) else Color(0xFFFF5722)
            )
        }
    }
}

@Composable
fun EmergencyToggleButton(
    icon: ImageVector,
    text: String,
    isActive: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
    activeColor: Color,
    inactiveColor: Color
) {
    Button(
        onClick = onClick,
        enabled = isEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isActive) activeColor else inactiveColor,
            contentColor = Color.White,
            disabledContainerColor = Color(0xFFBDBDBD),
            disabledContentColor = Color(0xFF757575)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun EmergenciaScreenPreview() {
    SIPRAISTheme {
        EmergenciaScreen()
    }
}
