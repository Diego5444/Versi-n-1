package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.UserProfile
import com.example.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    data object Loading : AuthUiState
    data class Authenticated(val user: FirebaseUser, val profile: UserProfile?) : AuthUiState
    data object Unauthenticated : AuthUiState
    data class Error(val message: String) : AuthUiState
    data class SuccessMessage(val message: String) : AuthUiState
}

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.observeAuthState().collect { firebaseUser ->
                if (firebaseUser != null) {
                    launch {
                        authRepository.observeUserProfile(firebaseUser.uid).collect { profile ->
                            _userProfile.value = profile
                            _uiState.value = AuthUiState.Authenticated(firebaseUser, profile)
                        }
                    }
                } else {
                    _userProfile.value = null
                    _uiState.value = AuthUiState.Unauthenticated
                }
            }
        }
    }

    fun signInWithEmail(email: String, pass: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.signInWithEmail(email, pass)
            result.onFailure { e ->
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Error al iniciar sesión")
            }
        }
    }

    fun signUpWithEmail(email: String, pass: String, name: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.signUpWithEmail(email, pass, name)
            result.onFailure { e ->
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Error al registrarse")
            }
        }
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.sendPasswordReset(email)
            result.onSuccess {
                _uiState.value = AuthUiState.SuccessMessage("Se ha enviado un correo para restablecer la contraseña.")
            }.onFailure { e ->
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Error al enviar correo de recuperación")
            }
        }
    }

    fun signInAnonymously() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.signInAnonymously()
            result.onFailure { e ->
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Error en acceso invitado")
            }
        }
    }

    fun updateProfile(displayName: String, photoUrl: String = "") {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            authRepository.updateProfile(uid, displayName, photoUrl)
        }
    }

    fun signOut() {
        authRepository.signOut()
    }

    fun deleteAccountAndData(onResult: (Result<Unit>) -> Unit) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val res = authRepository.deleteAccountAndData(uid)
            if (res.isSuccess) {
                _userProfile.value = null
                _uiState.value = AuthUiState.Unauthenticated
            } else {
                _uiState.value = AuthUiState.Error(res.exceptionOrNull()?.localizedMessage ?: "Error al eliminar la cuenta")
            }
            onResult(res)
        }
    }

    fun clearState() {
        _uiState.value = if (authRepository.currentUser != null) {
            AuthUiState.Authenticated(authRepository.currentUser!!, _userProfile.value)
        } else {
            AuthUiState.Unauthenticated
        }
    }
}
