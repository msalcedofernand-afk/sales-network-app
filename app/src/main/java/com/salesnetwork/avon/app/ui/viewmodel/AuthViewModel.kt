package com.salesnetwork.avon.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.salesnetwork.avon.app.data.LeaderNetworkRepository
import com.salesnetwork.avon.app.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val currentUser: User? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoginMode: Boolean = true,
    val selectedRole: String = "LIDER"
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LeaderNetworkRepository.getInstance(application)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.currentUser.collect { user ->
                _uiState.value = _uiState.value.copy(currentUser = user)
            }
        }
    }

    fun toggleAuthMode() {
        _uiState.value = _uiState.value.copy(
            isLoginMode = !_uiState.value.isLoginMode,
            errorMessage = null
        )
    }

    fun setRole(role: String) {
        _uiState.value = _uiState.value.copy(selectedRole = role)
    }

    fun login(email: String, password: String): Result<Any> {
        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Ingrese un correo electrónico.")
            return Result.failure(IllegalArgumentException("Ingrese un correo electrónico."))
        }
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        val result = repository.login(email, password)
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = result.exceptionOrNull()?.message
        )
        return result.map { it as Any }
    }

    fun registerLeader(name: String, email: String, password: String): Result<Any> {
        if (name.isBlank() || email.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Complete todos los campos.")
            return Result.failure(IllegalArgumentException("Complete todos los campos."))
        }
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        val result = repository.registerLeader(name, email, password)
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = result.exceptionOrNull()?.message
        )
        return result.map { it as Any }
    }

    fun registerMember(name: String, email: String, password: String, leaderCode: String): Result<Any> {
        if (name.isBlank() || email.isBlank() || leaderCode.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "El código de líder es obligatorio.")
            return Result.failure(IllegalArgumentException("El código de líder es obligatorio."))
        }
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        val result = repository.registerMember(name, email, password, leaderCode)
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = result.exceptionOrNull()?.message
        )
        return result.map { it as Any }
    }

    fun logout() {
        repository.logout()
    }
}
