package com.example.siprais

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.siprais.ui.theme.SIPRAISTheme
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearUsuarioScreen(
    onBackClicked: () -> Unit,
    onRegisterSuccess: () -> Unit = {}
) {
    // Estados para los campos de texto
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var confirmarContrasena by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Firebase
    val auth = Firebase.auth
    val database = Firebase.database("https://projectsiprai-default-rtdb.firebaseio.com/")

    // Colores
    val darkBackground = Color(0xFF141414)
    val accentColor = Color(0xFFE53935)
    val lightGrayInputBackground = Color(0xFFF0F0F0)
    val primaryTextDark = Color(0xFF212121)
    val secondaryTextGray = Color(0xFF616161)
    val buttonGradientStart = Color(0xFFE53935)
    val buttonGradientEnd = Color(0xFFD32F2F)
    val whiteContent = Color(0xFFFFFFFF)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Crear Cuenta",
                        color = whiteContent,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = whiteContent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = darkBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFFDFDFD))
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sección superior con título
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Regístrate",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = primaryTextDark
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Crea tu cuenta para acceder a SIPRAI",
                    fontSize = 16.sp,
                    color = secondaryTextGray
                )
            }

            // Mostrar mensaje de error si existe
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Formulario de creación de usuario
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 36.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Campo Nombre Completo
                FormInputField(
                    label = "NOMBRE COMPLETO",
                    value = nombre,
                    onValueChange = { nombre = it },
                    placeholder = "Ej. Juan Pérez"
                )

                // Campo Correo Electrónico
                FormInputField(
                    label = "CORREO ELECTRÓNICO",
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "Ej. correo@ejemplo.com"
                )

                // Campo Contraseña
                FormInputField(
                    label = "CONTRASEÑA",
                    value = contrasena,
                    onValueChange = { contrasena = it },
                    placeholder = "Mínimo 8 caracteres",
                    isPassword = true
                )

                // Campo Confirmar Contraseña
                FormInputField(
                    label = "CONFIRMAR CONTRASEÑA",
                    value = confirmarContrasena,
                    onValueChange = { confirmarContrasena = it },
                    placeholder = "Repite tu contraseña",
                    isPassword = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Botón Crear Cuenta
                Button(
                    onClick = {
                        if (nombre.isBlank() || email.isBlank() || contrasena.isBlank()) {
                            errorMessage = "Todos los campos son obligatorios"
                        } else if (contrasena != confirmarContrasena) {
                            errorMessage = "Las contraseñas no coinciden"
                        } else if (contrasena.length < 8) {
                            errorMessage = "La contraseña debe tener al menos 8 caracteres"
                        } else {
                            isLoading = true
                            errorMessage = ""

                            auth.createUserWithEmailAndPassword(email, contrasena)
                                .addOnCompleteListener { task ->
                                    isLoading = false
                                    if (task.isSuccessful) {
                                        // Guardar datos del usuario en la base de datos
                                        val userId = auth.currentUser?.uid
                                        if (userId != null) {
                                            val userData = hashMapOf(
                                                "nombre" to nombre,
                                                "email" to email,
                                                "fechaRegistro" to System.currentTimeMillis()
                                            )

                                            database.reference.child("users").child(userId)
                                                .setValue(userData)
                                                .addOnSuccessListener {
                                                    Toast.makeText(
                                                        context,
                                                        "Registro exitoso",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    onRegisterSuccess()
                                                }
                                                .addOnFailureListener { e ->
                                                    errorMessage = "Error al guardar datos: ${e.message}"
                                                }
                                        }
                                    } else {
                                        errorMessage = "Error al registrar: ${task.exception?.message}"
                                    }
                                }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(buttonGradientStart, buttonGradientEnd)
                            )
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    contentPadding = PaddingValues(0.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White)
                    } else {
                        Text(
                            text = "Crear Cuenta",
                            color = whiteContent,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun FormInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    isPassword: Boolean = false
) {
    val lightGrayInputBackground = Color(0xFFF0F0F0)
    val accentColor = Color(0xFFE53935)
    val primaryTextDark = Color(0xFF212121)
    val secondaryTextGray = Color(0xFF616161)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = secondaryTextGray,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = {
                Text(
                    text = placeholder,
                    color = secondaryTextGray.copy(alpha = 0.6f)
                )
            },
            visualTransformation = if (isPassword) PasswordVisualTransformation()
            else VisualTransformation.None,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = lightGrayInputBackground,
                unfocusedContainerColor = lightGrayInputBackground,
                disabledContainerColor = lightGrayInputBackground,
                focusedBorderColor = accentColor,
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
                focusedTextColor = primaryTextDark,
                unfocusedTextColor = primaryTextDark.copy(alpha = 0.7f),
                cursorColor = accentColor
            ),
            textStyle = LocalTextStyle.current.copy(fontSize = 17.sp)
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun CrearUsuarioScreenPreview() {
    SIPRAISTheme {
        CrearUsuarioScreen(
            onBackClicked = {},
            onRegisterSuccess = {}
        )
    }
}