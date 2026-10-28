package be.ap.nowaste.ui.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import be.ap.nowaste.R
import be.ap.nowaste.data.auth.UserRole

@Composable
fun RegisterScreen(
    onRegistered: (UserRole) -> Unit,
    onLoginClick: () -> Unit,
    viewModel: RegisterViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.registeredRole) {
        state.registeredRole?.let(onRegistered)
    }

    AuthLayout {
        Text(stringResource(R.string.register_title), style = MaterialTheme.typography.headlineSmall)

        AuthTextField(
            value = state.name,
            onValueChange = viewModel::onNameChange,
            label = R.string.auth_name,
            error = state.nameError,
            enabled = !state.isLoading,
        )
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
        )

        Text(stringResource(R.string.register_role_label), style = MaterialTheme.typography.titleSmall)
        RolePicker(selected = state.role, enabled = !state.isLoading, onSelect = viewModel::onRoleChange)
        FormMessage(state.roleError, isError = true)

        FormMessage(state.errorMessage, isError = true)

        LoadingButton(R.string.register_button, state.isLoading, viewModel::register)

        TextButton(onClick = onLoginClick, enabled = !state.isLoading) {
            Text(stringResource(R.string.register_to_login))
        }
    }
}

@Composable
private fun RolePicker(selected: UserRole?, enabled: Boolean, onSelect: (UserRole) -> Unit) {
    val roles = UserRole.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        roles.forEachIndexed { index, role ->
            SegmentedButton(
                selected = role == selected,
                onClick = { onSelect(role) },
                enabled = enabled,
                shape = SegmentedButtonDefaults.itemShape(index, roles.size),
                label = {
                    Text(
                        stringResource(
                            when (role) {
                                UserRole.CUSTOMER -> R.string.role_customer
                                UserRole.MERCHANT -> R.string.role_merchant
                            }
                        )
                    )
                },
            )
        }
    }
}
