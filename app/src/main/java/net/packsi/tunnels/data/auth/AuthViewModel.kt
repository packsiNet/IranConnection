package net.packsi.tunnels.data.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import net.packsi.tunnels.data.subscription.HttpFailure
import net.packsi.tunnels.data.subscription.SubscriptionRepository
import net.packsi.tunnels.data.subscription.SubscriptionResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoggedIn: Boolean = false,
    val email: String = "",
    val fullName: String = "",
    val adsEnabled: Boolean = true,
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
    // subscription (GET /api/subscription)
    val subscription: SubscriptionResponse? = null,
    val subscriptionLoading: Boolean = false,
    val subscriptionError: String? = null,
    val subscriptionNotFound: Boolean = false,
    // verify email (code)
    val verifyLoading: Boolean = false,
    val verifyError: String? = null,
    val verifySuccess: Boolean = false,
    val verifyMessage: String? = null,
    // resend verification code
    val resendLoading: Boolean = false,
    val resendError: String? = null,
    val resendMessage: String? = null,
    // forgot password (request code)
    val forgotLoading: Boolean = false,
    val forgotError: String? = null,
    val forgotSuccess: Boolean = false,
    val forgotMessage: String? = null,
    // reset password (code + new password)
    val resetLoading: Boolean = false,
    val resetError: String? = null,
    val resetSuccess: Boolean = false,
    val resetMessage: String? = null,
    // update profile / change password
    val updateLoading: Boolean = false,
    val updateError: String? = null,
    val updateSuccess: Boolean = false,
)

class AuthViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(
        AuthUiState(
            isLoggedIn = TokenStore.loggedIn.value,
            email = TokenStore.email,
            fullName = TokenStore.fullName,
            adsEnabled = TokenStore.adsEnabled,
        )
    )
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun login(email: String, password: String) {
        if (_state.value.loginLoading) return
        // Dev bypass: default password "123456" logs in without hitting the server.
        if (password == "123456") {
            _state.value = _state.value.copy(
                loginLoading = false,
                loginError = null,
                isLoggedIn = true,
                email = email.trim().ifBlank { "dev@local" },
                fullName = "Dev User",
            )
            return
        }
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
                        // Ads switch is owned by /api/app/config (persisted in TokenStore), not login.
                        adsEnabled = TokenStore.adsEnabled,
                    )
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        loginLoading = false,
                        loginError = e.message ?: "Unknown error",
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
                        registerMessage = r.message ?: "Registration successful",
                    )
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        registerLoading = false,
                        registerError = e.message ?: "Unknown error",
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

    /** Loads per-user subscription status. 404 → user has no subscription (not an error banner). */
    fun loadSubscription() {
        _state.value = _state.value.copy(
            subscriptionLoading = true, subscriptionError = null, subscriptionNotFound = false,
        )
        viewModelScope.launch {
            SubscriptionRepository.getSubscription().fold(
                onSuccess = { s ->
                    _state.value = _state.value.copy(subscriptionLoading = false, subscription = s)
                },
                onFailure = { e ->
                    val notFound = (e as? HttpFailure)?.code == 404
                    _state.value = _state.value.copy(
                        subscriptionLoading = false,
                        subscriptionNotFound = notFound,
                        subscriptionError = if (notFound) null else e.message,
                    )
                },
            )
        }
    }

    // ---- D) verify email with code ----
    fun verifyEmail(email: String, code: String) {
        if (_state.value.verifyLoading) return
        _state.value = _state.value.copy(verifyLoading = true, verifyError = null, verifySuccess = false)
        viewModelScope.launch {
            AuthRepository.verifyEmail(email, code).fold(
                onSuccess = { msg ->
                    _state.value = _state.value.copy(
                        verifyLoading = false, verifySuccess = true,
                        verifyMessage = msg.ifBlank { "Email verified successfully" },
                    )
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(verifyLoading = false, verifyError = e.message ?: "Unknown error")
                },
            )
        }
    }

    // ---- E) resend verification code (requires auth) ----
    fun resendVerification() {
        if (_state.value.resendLoading) return
        _state.value = _state.value.copy(resendLoading = true, resendError = null, resendMessage = null)
        viewModelScope.launch {
            AuthRepository.resendVerification().fold(
                onSuccess = { msg ->
                    _state.value = _state.value.copy(resendLoading = false, resendMessage = msg.ifBlank { "Verification code resent" })
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(resendLoading = false, resendError = e.message ?: "Unknown error")
                },
            )
        }
    }

    // ---- F) forgot password (request code) ----
    fun forgotPassword(email: String) {
        if (_state.value.forgotLoading) return
        _state.value = _state.value.copy(forgotLoading = true, forgotError = null, forgotSuccess = false)
        viewModelScope.launch {
            AuthRepository.forgotPassword(email).fold(
                onSuccess = { msg ->
                    _state.value = _state.value.copy(
                        forgotLoading = false, forgotSuccess = true,
                        forgotMessage = msg.ifBlank { "If the email exists, a code has been sent" },
                    )
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(forgotLoading = false, forgotError = e.message ?: "Unknown error")
                },
            )
        }
    }

    // ---- G) reset password (code + new password) ----
    fun resetPassword(email: String, code: String, newPassword: String) {
        if (_state.value.resetLoading) return
        _state.value = _state.value.copy(resetLoading = true, resetError = null, resetSuccess = false)
        viewModelScope.launch {
            AuthRepository.resetPassword(email, code, newPassword).fold(
                onSuccess = { msg ->
                    _state.value = _state.value.copy(
                        resetLoading = false, resetSuccess = true,
                        resetMessage = msg.ifBlank { "Password changed successfully" },
                    )
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(resetLoading = false, resetError = e.message ?: "Unknown error")
                },
            )
        }
    }

    // ---- H) update profile / change password ----
    fun updateProfile(fullName: String?, currentPassword: String?, newPassword: String?) {
        if (_state.value.updateLoading) return
        _state.value = _state.value.copy(updateLoading = true, updateError = null, updateSuccess = false)
        viewModelScope.launch {
            AuthRepository.updateProfile(fullName, currentPassword, newPassword).fold(
                onSuccess = { p ->
                    _state.value = _state.value.copy(
                        updateLoading = false, updateSuccess = true,
                        profile = p,
                        email = p.email ?: _state.value.email,
                        fullName = p.fullName ?: _state.value.fullName,
                    )
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(updateLoading = false, updateError = e.message ?: "Unknown error")
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

    fun clearVerifyState() {
        _state.value = _state.value.copy(verifyError = null, verifySuccess = false, verifyMessage = null,
            resendError = null, resendMessage = null)
    }

    fun clearForgotState() {
        _state.value = _state.value.copy(forgotError = null, forgotSuccess = false, forgotMessage = null)
    }

    fun clearResetState() {
        _state.value = _state.value.copy(resetError = null, resetSuccess = false, resetMessage = null)
    }

    fun clearUpdateState() {
        _state.value = _state.value.copy(updateError = null, updateSuccess = false)
    }
}
