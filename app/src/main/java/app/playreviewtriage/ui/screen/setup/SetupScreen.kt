package app.playreviewtriage.ui.screen.setup

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.playreviewtriage.presentation.uistate.SetupUiState
import app.playreviewtriage.presentation.viewmodel.SetupViewModel

@Composable
fun SetupScreen(
    viewModel: SetupViewModel = hiltViewModel(),
    onSuccess: () -> Unit,
    onLogout: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var packageNameInput by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        if (uiState is SetupUiState.Success) {
            viewModel.resetState()
            onSuccess()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("監視するアプリを設定", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = packageNameInput,
            onValueChange = { packageNameInput = it },
            label = { Text("packageName（例: com.example.app）") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(Modifier.height(8.dp))

        if (uiState is SetupUiState.ValidationError) {
            Text(
                text = (uiState as SetupUiState.ValidationError).message,
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(8.dp))
        }

        if (uiState is SetupUiState.ApiError) {
            val apiError = uiState as SetupUiState.ApiError
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text(apiError.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(4.dp))
                    Text(apiError.message, style = MaterialTheme.typography.bodySmall)
                    if (apiError.showLogout) {
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = onLogout) { Text("ログアウト") }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = { viewModel.save(packageNameInput) },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState !is SetupUiState.Loading,
        ) {
            if (uiState is SetupUiState.Loading) CircularProgressIndicator(Modifier.size(18.dp))
            else Text("保存して続ける")
        }
    }
}
