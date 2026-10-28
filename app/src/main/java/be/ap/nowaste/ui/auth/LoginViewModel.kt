package be.ap.nowaste.ui.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.ap.nowaste.R
import be.ap.nowaste.data.auth.AuthException
import be.ap.nowaste.data.auth.AuthRepository
import be.ap.nowaste.data.auth.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    @StringRes val emailError: Int? = null,
    @StringRes val passwordError: Int? = null,
    @StringRes val errorMessage: Int? = null,
    @StringRes val infoMessage: Int? = null,
    val isLoading: Boolean = false,
    // Set after a successful login; the screen then navigates to the home of this role
    val loggedInRole: UserRole? = null,
)

class LoginViewModel(
    private val repository: AuthRepository = AuthRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) =
        _uiState.update { it.copy(email = email, emailError = null, errorMessage = null, infoMessage = null) }

    fun onPasswordChange(password: String) =
        _uiState.update { it.copy(password = password, passwordError = null, errorMessage = null) }

    fun login() {
        val state = _uiState.value
        val emailError = if (isValidEmail(state.email)) null else R.string.auth_error_invalid_email
        val passwordError = if (state.password.isEmpty()) R.string.auth_error_password_required else null
        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
        viewModelScope.launch {
            try {
                val role = repository.login(state.email.trim(), state.password)
                _uiState.update { it.copy(isLoading = false, loggedInRole = role) }
            } catch (e: AuthException) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.error.messageRes()) }
            }
        }
    }

    fun sendPasswordReset() {
        val email = _uiState.value.email
        if (!isValidEmail(email)) {
            _uiState.update { it.copy(emailError = R.string.auth_error_reset_needs_email) }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
        viewModelScope.launch {
            try {
                repository.sendPasswordReset(email.trim())
                _uiState.update { it.copy(isLoading = false, infoMessage = R.string.auth_reset_sent) }
            } catch (e: AuthException) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.error.messageRes()) }
            }
        }
    }
}
