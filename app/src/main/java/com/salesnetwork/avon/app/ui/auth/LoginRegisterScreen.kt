package com.salesnetwork.avon.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salesnetwork.avon.app.domain.model.UserRole

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginRegisterScreen(
    onLoginSuccess: () -> Unit,
    onRegisterLeader: (name: String, email: String, password: String) -> Result<Any>,
    onRegisterMember: (name: String, email: String, password: String, leaderCode: String) -> Result<Any>,
    onLoginClick: (email: String, password: String) -> Result<Any>
) {
    var isRegisterMode by rememberSaveable { mutableStateOf(false) }
    var selectedRole by rememberSaveable { mutableStateOf(UserRole.LIDER) }

    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var leaderCode by rememberSaveable { mutableStateOf("") }

    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isRegisterMode) "Registro de Red" else "Ingreso al Sistema",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Venta Directa & Gestión de Líderes (Chiclayo)",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (errorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 13.sp
                        )
                    }
                }

                if (isRegisterMode) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre Completo") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo Electrónico") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (isRegisterMode) {
                    Text(
                        text = "Seleccione Tipo de Cuenta:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(top = 8.dp, bottom = 4.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        FilterChip(
                            selected = selectedRole == UserRole.LIDER,
                            onClick = { selectedRole = UserRole.LIDER },
                            label = { Text("Soy Líder") },
                            leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) }
                        )
                        FilterChip(
                            selected = selectedRole == UserRole.MIEMBRO,
                            onClick = { selectedRole = UserRole.MIEMBRO },
                            label = { Text("Soy Vendedor") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                        )
                    }

                    if (selectedRole == UserRole.MIEMBRO) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = leaderCode,
                            onValueChange = { leaderCode = it.uppercase() },
                            label = { Text("Código de Líder (Obligatorio)") },
                            placeholder = { Text("Ej. AVON-2026") },
                            leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = leaderCode.isBlank()
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Button(
                    onClick = {
                        errorMessage = null
                        if (isRegisterMode) {
                            if (name.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() || password.length < 8) {
                                errorMessage = "Ingresa un correo válido y una contraseña de al menos 8 caracteres."
                                return@Button
                            }
                            val result = if (selectedRole == UserRole.LIDER) {
                                onRegisterLeader(name.trim(), email.trim(), password)
                            } else {
                                if (leaderCode.isBlank()) {
                                    errorMessage = "Debes ingresar el código de tu líder para registrarte."
                                    return@Button
                                }
                                onRegisterMember(name.trim(), email.trim(), password, leaderCode)
                            }
                            if (result.isSuccess) {
                                onLoginSuccess()
                            } else {
                                errorMessage = result.exceptionOrNull()?.message ?: "Error al registrar usuario."
                            }
                        } else {
                            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() || password.isBlank()) {
                                errorMessage = "Ingresa un correo válido y tu contraseña."
                                return@Button
                            }
                            val result = onLoginClick(email.trim(), password)
                            if (result.isSuccess) {
                                onLoginSuccess()
                            } else {
                                errorMessage = result.exceptionOrNull()?.message ?: "Error al iniciar sesión."
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isRegisterMode) "Crear Cuenta" else "Iniciar Sesión",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = {
                    isRegisterMode = !isRegisterMode
                    errorMessage = null
                }) {
                    Text(
                        text = if (isRegisterMode) "¿Ya tienes cuenta? Inicia Sesión" else "¿No tienes cuenta? Regístrate aquí"
                    )
                }
            }
        }
    }
}
