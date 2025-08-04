package com.example.siprais
import androidx.compose.ui.tooling.preview.Preview

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.siprais.ui.theme.SIPRAISTheme
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToForgot: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val auth = Firebase.auth
    val database = Firebase.database("https://projectsiprai-default-rtdb.firebaseio.com/")

    val darkBackground = Color(0xFF000000)
    val accentColor = Color(0xFFFF0000)
    val textOnButton = Color(0xFFFFFFFF)
    val primaryText = Color(0xFF000000)
    val secondaryText = Color(0xFF444444)

    LaunchedEffect(Unit) {
        auth.signOut() // Opcional: fuerza logout al entrar
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sección superior
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(darkBackground),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Logo",
                    modifier = Modifier.size(180.dp)
                )
            }

            // Sección inferior: Formulario
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.5f)
                    .clip(RoundedCornerShape(topStart = 60.dp, topEnd = 60.dp))
                    .background(Color.White)
                    .padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "Bienvenidos",
                    color = primaryText,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )

                if (showError) {
                    Text(
                        text = "Usuario o contraseña incorrectos",
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                // Email
                Column {
                    Text("CORREO ELECTRÓNICO", color = secondaryText, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; showError = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = secondaryText,
                            cursorColor = accentColor
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Contraseña
                Column {
                    Text("CONTRASEÑA", color = secondaryText, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; showError = false },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = secondaryText,
                            cursorColor = accentColor
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Botón Ingresar
                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            showError = true
                        } else {
                            isLoading = true
                            auth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    isLoading = false
                                    if (task.isSuccessful) {
                                        val user = auth.currentUser
                                        if (user != null) {
                                            user.getIdToken(true)
                                                .addOnSuccessListener {
                                                    Firebase.database.goOnline()

                                                    val ref = database
                                                        .getReference("users/${user.uid}/lastLogin")

                                                    ref.setValue(System.currentTimeMillis())
                                                        .addOnSuccessListener {
                                                            Toast.makeText(
                                                                context,
                                                                "Inicio de sesión exitoso",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                            onLoginSuccess()
                                                        }
                                                        .addOnFailureListener { dbError ->
                                                            showError = true
                                                            Toast.makeText(
                                                                context,
                                                                "Error al guardar en BD: ${dbError.message}",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                }
                                                .addOnFailureListener { tokenError ->
                                                    showError = true
                                                    Toast.makeText(
                                                        context,
                                                        "Token inválido: ${tokenError.message}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                        }
                                    } else {
                                        showError = true
                                        Toast.makeText(
                                            context,
                                            "Error: ${task.exception?.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = textOnButton
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White)
                    } else {
                        Text(
                            text = "INGRESAR",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Enlaces inferiores
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = onNavigateToForgot,
                        colors = ButtonDefaults.textButtonColors(contentColor = accentColor)
                    ) {
                        Text("¿Olvidaste tu contraseña?")
                    }

                    TextButton(
                        onClick = onNavigateToRegister,
                        colors = ButtonDefaults.textButtonColors(contentColor = accentColor)
                    ) {
                        Text("CREAR USUARIO", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun LoginPreview() {
    SIPRAISTheme {
        LoginScreen(
            onNavigateToRegister = {},
            onNavigateToForgot = {},
            onLoginSuccess = {}
        )
    }
}
