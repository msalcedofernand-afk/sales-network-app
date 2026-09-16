package com.salesnetwork.avon.app.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.salesnetwork.avon.app.inspector.inspectable
import com.salesnetwork.avon.app.ui.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginRegisterScreen(
    onLoginSuccess: () -> Unit,
    onRegisterLeader: (name: String, email: String, password: String) -> Result<Any>,
    onRegisterMember: (name: String, email: String, password: String, leaderCode: String) -> Result<Any>,
    onLoginClick: (email: String, password: String) -> Result<Any>,
    onResetPassword: (email: String, newPassword: String) -> Result<Boolean> = { _, _ -> Result.success(true) },
    isLoading: Boolean = false
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
            .background(C.Petroleo900)
            .safeDrawingPadding()
            .imePadding()
            .inspectable("login:screen", "screen", "Inicio de sesión"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .padding(S.M)
                .inspectable("login:card", "card", "Formulario de acceso"),
            shape = SH.Dialog,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = B.cardBorder(),
            elevation = CardDefaults.cardElevation(S.ElevationNone)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(S.L),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(S.SM)
            ) {
                // Logo
                Surface(
                    modifier = Modifier.size(56.dp).inspectable("login:logo", "logo", "VV"),
                    shape = SH.Avatar,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "VV",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = S.TextSubtitle
                        )
                    }
                }

                // Title
                Text(
                    text = if (isRegisterMode) "Crea tu cuenta" else "Tu negocio,\na otro nivel.",
                    fontSize = S.TextHeadline,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.inspectable("login:title", "text")
                )

                // Subtitle
                Text(
                    text = if (isRegisterMode) "Unete a una red de ventas o crea la tuya propia" else "Organiza clientes, catalogo y pedidos desde un solo lugar",
                    fontSize = S.TextBody,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Error message
                AnimatedVisibility(visible = errorMessage != null, enter = fadeIn(), exit = fadeOut()) {
                    Surface(
                        color = C.ErrorLight,
                        shape = SH.Badge,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = C.Error,
                            modifier = Modifier.padding(S.SM),
                            fontSize = S.TextSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Name field (register only)
                AnimatedVisibility(visible = isRegisterMode, enter = fadeIn(), exit = fadeOut()) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre Completo") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(S.IconM)) },
                        modifier = Modifier.fillMaxWidth().inspectable("login:name", "input", "Nombre Completo"),
                        singleLine = true,
                        shape = SH.Input
                    )
                }

                // Email field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo Electronico") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(S.IconM)) },
                    modifier = Modifier.fillMaxWidth().inspectable("login:email", "input", "Correo Electronico"),
                    singleLine = true,
                    shape = SH.Input,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                )

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contrasena") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(S.IconM)) },
                    visualTransformation = if (showPassword) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { showPassword = !showPassword }) {
                            Text(
                                if (showPassword) "Ocultar" else "Ver",
                                fontSize = S.TextSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().inspectable("login:password", "input", "Contrasena"),
                    singleLine = true,
                    shape = SH.Input,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
                )

                // Register mode: role selection
                AnimatedVisibility(visible = isRegisterMode, enter = fadeIn(), exit = fadeOut()) {
                    Column(verticalArrangement = Arrangement.spacedBy(S.S)) {
                        Text(
                            text = "Tipo de Cuenta:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = S.TextBody
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(S.S)
                        ) {
                            FilterChip(
                                selected = selectedRole == UserRole.LIDER,
                                onClick = { selectedRole = UserRole.LIDER },
                                label = { Text("Lider de Red", fontSize = S.TextSmall) },
                                leadingIcon = { Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(S.IconS)) },
                                modifier = Modifier.weight(1f).inspectable("login:role:leader", "selector", "Lider de Red"),
                                shape = SH.Chip
                            )
                            FilterChip(
                                selected = selectedRole == UserRole.MIEMBRO,
                                onClick = { selectedRole = UserRole.MIEMBRO },
                                label = { Text("Vendedor", fontSize = S.TextSmall) },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(S.IconS)) },
                                modifier = Modifier.weight(1f).inspectable("login:role:member", "selector", "Vendedor"),
                                shape = SH.Chip
                            )
                        }

                        // Leader code for members
                        AnimatedVisibility(visible = selectedRole == UserRole.MIEMBRO, enter = fadeIn(), exit = fadeOut()) {
                            Column(verticalArrangement = Arrangement.spacedBy(S.S)) {
                                Surface(
                                    color = C.WarningLight,
                                    shape = SH.Badge,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "CODIGO DE RED OBLIGATORIO: Ingresa el codigo de referido de tu Lider (ej. VV-2026).",
                                        fontSize = S.TextCaption,
                                        fontWeight = FontWeight.SemiBold,
                                        color = C.Warning,
                                        modifier = Modifier.padding(S.SM)
                                    )
                                }

                                OutlinedTextField(
                                    value = leaderCode,
                                    onValueChange = { leaderCode = it.uppercase() },
                                    label = { Text("Codigo de invitacion") },
                                    placeholder = { Text("Ej. VV-2026") },
                                    leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(S.IconM)) },
                                    modifier = Modifier.fillMaxWidth().inspectable("login:leader-code", "input", "Codigo de invitacion"),
                                    singleLine = true,
                                    shape = SH.Input,
                                    isError = leaderCode.isBlank()
                                )
                            }
                        }
                    }
                }

                // Submit button
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
                            onLoginClick(email.trim(), password)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .inspectable("login:submit", "button", if (isRegisterMode) "Crear Cuenta" else "Iniciar Sesion"),
                    shape = SH.Button,
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(S.IconM),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(S.S))
                    }
                    Text(
                        text = if (isLoading) {
                            if (isRegisterMode) "Creando cuenta..." else "Iniciando sesion..."
                        } else {
                            if (isRegisterMode) "Crear Cuenta" else "Iniciar Sesion"
                        },
                        fontSize = S.TextSubtitle,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Forgot password
                AnimatedVisibility(visible = !isRegisterMode, enter = fadeIn(), exit = fadeOut()) {
                    TextButton(
                        onClick = {
                            resetEmail = email.trim()
                            resetNewPassword = ""
                            resetMessage = null
                            showResetDialog = true
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                            .inspectable("login:forgot-password", "button", "Olvidaste tu contrasena?"),
                    ) {
                        Text(
                            text = "Olvidaste tu contrasena?",
                            fontSize = S.TextBody,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Toggle mode
                TextButton(
                    onClick = {
                        isRegisterMode = !isRegisterMode
                        errorMessage = null
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                        .inspectable("login:toggle-mode", "button"),
                ) {
                    Text(
                        text = if (isRegisterMode) "Ya tienes cuenta? Inicia Sesion" else "No tienes cuenta? Registrate aqui",
                        fontSize = S.TextBody
                    )
                }
            }
        }
    }

    // Reset password dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Recuperar Contrasena", fontWeight = FontWeight.Bold, fontSize = S.TextTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(S.SM)) {
                    Text("Ingresa tu correo registrado y tu nueva contrasena:", fontSize = S.TextBody)
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Correo Electronico") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = SH.Input
                    )
                    OutlinedTextField(
                        value = resetNewPassword,
                        onValueChange = { resetNewPassword = it },
                        label = { Text("Nueva Contrasena (minimo 6 caracteres)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = SH.Input
                    )
                    if (resetMessage != null) {
                        Text(
                            text = resetMessage!!,
                            color = if (resetMessage!!.startsWith("OK")) C.Success else MaterialTheme.colorScheme.error,
                            fontSize = S.TextSmall,
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
                            resetMessage = "OK: Contrasena actualizada. Ya puedes iniciar sesion."
                        } else {
                            resetMessage = res.exceptionOrNull()?.message ?: "Error al actualizar."
                        }
                    },
                    shape = SH.Button
                ) {
                    Text("Actualizar", fontWeight = FontWeight.Bold)
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
