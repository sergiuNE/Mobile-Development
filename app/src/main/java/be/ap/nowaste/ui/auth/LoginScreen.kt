package be.ap.nowaste.ui.auth

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import be.ap.nowaste.R
import be.ap.nowaste.data.auth.UserRole
import kotlinx.coroutines.Dispatchers

@Composable
fun LoginScreen(
    onLoggedIn: (UserRole) -> Unit,
    onRegisterClick: () -> Unit,
    viewModel: LoginViewModel = viewModel(),
) {
    // Main.immediate: text field updates arrive in the same frame, so fast typing loses no characters
    val state by viewModel.uiState.collectAsStateWithLifecycle(context = Dispatchers.Main.immediate)

    LaunchedEffect(state.loggedInRole) {
        state.loggedInRole?.let(onLoggedIn)
    }

    AuthLayout {
        Text(stringResource(R.string.login_title), style = MaterialTheme.typography.headlineSmall)

        AuthTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            label = R.string.auth_email,
            error = state.emailError,
            enabled = !state.isLoading,
            keyboardType = KeyboardType.Email,
        )
        AuthTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = R.string.auth_password,
            error = state.passwordError,
            enabled = !state.isLoading,
            keyboardType = KeyboardType.Password,
            isPassword = true,
            imeAction = ImeAction.Done,
            onDone = viewModel::login,
        )

        FormMessage(state.errorMessage, isError = true)
        FormMessage(state.infoMessage, isError = false)

        LoadingButton(R.string.login_button, state.isLoading, viewModel::login)

        TextButton(onClick = viewModel::sendPasswordReset, enabled = !state.isLoading) {
            Text(stringResource(R.string.login_forgot_password))
        }
        TextButton(onClick = onRegisterClick, enabled = !state.isLoading) {
            Text(stringResource(R.string.login_to_register))
        }
    }
}
