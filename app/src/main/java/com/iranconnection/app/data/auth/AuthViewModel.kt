package com.iranconnection.app.data.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoggedIn: Boolean = false,
    val email: String = "",
    val fullName: String = "",
    // login
    val loginLoading: Boolean = false,
    val loginError: String? = null,
    // register
    val registerLoading: Boolean = false,
    val registerError: String? = null,
    val registerSuccess: Boolean = false,
    val registerMessage: String? = null,
    // profile
    val profile: UserProfile? = null,
    val profileLoading: Boolean = false,
    val profileError: String? = null,
)

class AuthViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(
        AuthUiState(
            isLoggedIn = TokenStore.loggedIn.value,
            email = TokenStore.email,
            fullName = TokenStore.fullName,
        )
    )
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun login(email: String, password: String) {
        if (_state.value.loginLoading) return
        _state.value = _state.value.copy(loginLoading = true, loginError = null)
        viewModelScope.launch {
            val result = AuthRepository.login(email.trim(), password)
            result.fold(
                onSuccess = { r ->
                    _state.value = _state.value.copy(
                        loginLoading = false,
                        isLoggedIn = true,
                        email = r.email ?: email.trim(),
                        fullName = r.fullName ?: "",
                    )
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        loginLoading = false,
                        loginError = e.message ?: "خطای ناشناخته",
                    )
                },
            )
        }
    }

    fun register(email: String, password: String, fullName: String) {
        if (_state.value.registerLoading) return
        _state.value = _state.value.copy(registerLoading = true, registerError = null, registerSuccess = false)
        viewModelScope.launch {
            val result = AuthRepository.register(email.trim(), password, fullName)
            result.fold(
                onSuccess = { r ->
                    _state.value = _state.value.copy(
                        registerLoading = false,
                        registerSuccess = true,
                        registerMessage = r.message ?: "ثبت‌نام با موفقیت انجام شد",
                    )
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        registerLoading = false,
                        registerError = e.message ?: "خطای ناشناخته",
                    )
                },
            )
        }
    }

    fun loadProfile() {
        _state.value = _state.value.copy(profileLoading = true, profileError = null)
        viewModelScope.launch {
            AuthRepository.profile().fold(
                onSuccess = { p ->
                    _state.value = _state.value.copy(
                        profileLoading = false,
                        profile = p,
                        email = p.email ?: _state.value.email,
                        fullName = p.fullName ?: _state.value.fullName,
                    )
                },
                onFailure = { e ->
                    // A 401 here means refresh also failed and tokens were cleared.
                    _state.value = _state.value.copy(
                        profileLoading = false,
                        profileError = e.message,
                        isLoggedIn = TokenStore.loggedIn.value,
                    )
                },
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            AuthRepository.logout()
            _state.value = AuthUiState(isLoggedIn = false)
        }
    }

    fun clearLoginError() {
        if (_state.value.loginError != null) _state.value = _state.value.copy(loginError = null)
    }

    fun clearRegisterState() {
        _state.value = _state.value.copy(registerError = null, registerSuccess = false, registerMessage = null)
    }
}
