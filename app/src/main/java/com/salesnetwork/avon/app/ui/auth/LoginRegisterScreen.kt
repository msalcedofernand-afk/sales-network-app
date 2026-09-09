package com.salesnetwork.avon.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salesnetwork.avon.app.domain.model.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginRegisterScreen(
    onLoginSuccess: () -> Unit,
    onRegisterLeader: (name: String, email: String, password: String) -> Result<Any>,
    onRegisterMember: (name: String, email: String, password: String, leaderCode: String) -> Result<Any>,
    onLoginClick: (email: String, password: String) -> Result<Any>,
    onResetPassword: (email: String, newPassword: String) -> Result<Boolean> = { _, _ -> Result.success(true) }
) {
    var isRegisterMode by rememberSaveable { mutableStateOf(false) }
    var selectedRole by rememberSaveable { mutableStateOf(UserRole.LIDER) }

    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var leaderCode by rememberSaveable { mutableStateOf("") }

    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resetNewPassword by remember { mutableStateOf("") }
    var resetMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF123D49))
            .safeDrawingPadding()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Surface(
                    modifier = Modifier.size(60.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("VV", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isRegisterMode) "Crea tu cuenta" else "Tu negocio,\na otro nivel.",
                    fontSize = 28.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = if (isRegisterMode) "Unete a una red de ventas o crea la tuya propia" else "Organiza clientes, catalogo y pedidos desde un solo lugar",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    label = { Text("Correo Electronico") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contrasena") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = if (showPassword) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { TextButton(onClick = { showPassword = !showPassword }) { Text(if (showPassword) "Ocultar" else "Ver") } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (isRegisterMode) {
                    Text(
                        text = "Seleccione Tipo de Cuenta:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
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
                            label = { Text("Soy Lider de Red") },
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

                        // Aviso de Codigo Obligatorio
                        Surface(
                            color = Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "CODIGO DE RED OBLIGATORIO: Para registrarte como vendedor debes ingresar el codigo de referido de tu Lider (ej. VV-2026).",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFE65100),
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = leaderCode,
                            onValueChange = { leaderCode = it.uppercase() },
                            label = { Text("Codigo de invitacion") },
                            placeholder = { Text("Ej. VV-2026") },
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
                            if (name.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() || password.length < 6) {
                                errorMessage = "Ingresa un correo valido y una contrasena de al menos 6 caracteres."
                                return@Button
                            }
                            val result = if (selectedRole == UserRole.LIDER) {
                                onRegisterLeader(name.trim(), email.trim(), password)
                            } else {
                                if (leaderCode.trim().isBlank()) {
                                    errorMessage = "El codigo de la red es OBLIGATORIO para registrarte como vendedor."
                                    return@Button
                                }
                                onRegisterMember(name.trim(), email.trim(), password, leaderCode.trim())
                            }
                            if (result.isSuccess) {
                                onLoginSuccess()
                            } else {
                                errorMessage = result.exceptionOrNull()?.message ?: "Error al registrar usuario."
                            }
                        } else {
                            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() || password.isBlank()) {
                                errorMessage = "Ingresa un correo valido y tu contrasena."
                                return@Button
                            }
                            val result = onLoginClick(email.trim(), password)
                            if (result.isSuccess) {
                                onLoginSuccess()
                            } else {
                                errorMessage = result.exceptionOrNull()?.message ?: "Error al iniciar sesion."
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = if (isRegisterMode) "Crear Cuenta" else "Iniciar Sesion",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!isRegisterMode) {
                    TextButton(
                        onClick = {
                            resetEmail = email.trim()
                            resetNewPassword = ""
                            resetMessage = null
                            showResetDialog = true
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = "Olvidaste tu contrasena?",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Acceso rapido demo:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        SuggestionChip(
                            onClick = {
                                email = "root@vv.com"
                                password = "RootAdmin2026!"
                            },
                            label = { Text("Root Admin", fontSize = 11.sp) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        SuggestionChip(
                            onClick = {
                                email = "lider.chiclayo@vv.com"
                                password = "LiderVV2026!"
                            },
                            label = { Text("Lider VV", fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                TextButton(onClick = {
                    isRegisterMode = !isRegisterMode
                    errorMessage = null
                }) {
                    Text(
                        text = if (isRegisterMode) "Ya tienes cuenta? Inicia Sesion" else "No tienes cuenta? Registrate aqui"
                    )
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Recuperar Contrasena", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ingresa tu correo registrado y tu nueva contrasena:", fontSize = 13.sp)
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Correo Electronico") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = resetNewPassword,
                        onValueChange = { resetNewPassword = it },
                        label = { Text("Nueva Contrasena (minimo 6 caracteres)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (resetMessage != null) {
                        Text(
                            text = resetMessage!!,
                            color = if (resetMessage!!.startsWith("OK")) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val res = onResetPassword(resetEmail.trim(), resetNewPassword)
                        if (res.isSuccess) {
                            resetMessage = "OK: Contrasena actualizada correctamente. Ya puedes iniciar sesion."
                        } else {
                            resetMessage = res.exceptionOrNull()?.message ?: "Error al actualizar."
                        }
                    }
                ) {
                    Text("Actualizar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}
