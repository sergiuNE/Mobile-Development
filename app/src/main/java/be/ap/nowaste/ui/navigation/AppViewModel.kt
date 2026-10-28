package be.ap.nowaste.ui.navigation

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.ap.nowaste.data.auth.AuthException
import be.ap.nowaste.data.auth.AuthRepository
import be.ap.nowaste.data.auth.UserRole
import be.ap.nowaste.ui.auth.messageRes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AppUiState {
    data object Loading : AppUiState
    data object LoggedOut : AppUiState
    data class LoggedIn(val role: UserRole) : AppUiState
    data class Error(@StringRes val message: Int) : AppUiState
}

/** Decides the start screen at launch (A2) and handles logout. */
class AppViewModel(
    private val repository: AuthRepository = AuthRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<AppUiState>(AppUiState.Loading)
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        loadSession()
    }

    fun loadSession() {
        _uiState.value = AppUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                repository.currentUserRole()?.let { AppUiState.LoggedIn(it) } ?: AppUiState.LoggedOut
            } catch (e: AuthException) {
                AppUiState.Error(e.error.messageRes())
            }
        }
    }

    fun logout() = repository.logout()
}
