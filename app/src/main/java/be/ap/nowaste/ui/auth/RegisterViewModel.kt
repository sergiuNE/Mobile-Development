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

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val role: UserRole? = null,
    @StringRes val nameError: Int? = null,
    @StringRes val emailError: Int? = null,
    @StringRes val passwordError: Int? = null,
    @StringRes val roleError: Int? = null,
    @StringRes val errorMessage: Int? = null,
    val isLoading: Boolean = false,
    val registeredRole: UserRole? = null,
)

class RegisterViewModel(
    private val repository: AuthRepository = AuthRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onNameChange(name: String) =
        _uiState.update { it.copy(name = name, nameError = null, errorMessage = null) }

    fun onEmailChange(email: String) =
        _uiState.update { it.copy(email = email, emailError = null, errorMessage = null) }

    fun onPasswordChange(password: String) =
        _uiState.update { it.copy(password = password, passwordError = null, errorMessage = null) }

    fun onRoleChange(role: UserRole) =
        _uiState.update { it.copy(role = role, roleError = null, errorMessage = null) }

    fun register() {
        val state = _uiState.value
        val nameError = if (state.name.isBlank()) R.string.auth_error_name_required else null
        val emailError = if (isValidEmail(state.email)) null else R.string.auth_error_invalid_email
        val passwordError = if (isValidPassword(state.password)) null else R.string.auth_error_weak_password
        val roleError = if (state.role == null) R.string.auth_error_role_required else null
        if (nameError != null || emailError != null || passwordError != null || roleError != null) {
            _uiState.update {
                it.copy(nameError = nameError, emailError = emailError, passwordError = passwordError, roleError = roleError)
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val role = repository.register(state.name.trim(), state.email.trim(), state.password, state.role!!)
                _uiState.update { it.copy(isLoading = false, registeredRole = role) }
            } catch (e: AuthException) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.error.messageRes()) }
            }
        }
    }
}
