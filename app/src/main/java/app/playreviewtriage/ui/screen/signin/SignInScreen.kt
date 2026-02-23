package app.playreviewtriage.ui.screen.signin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.playreviewtriage.presentation.uistate.SignInUiState
import app.playreviewtriage.presentation.viewmodel.SignInViewModel

@Composable
fun SignInScreen(
    viewModel: SignInViewModel = hiltViewModel(),
    onSuccess: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is SignInUiState.Success) {
            viewModel.resetState()
            onSuccess()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Connect to Google Play",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "レビュー取得のためにGoogleアカウント連携が必要です。",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(32.dp))

        if (uiState is SignInUiState.Loading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = { viewModel.signIn() },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is SignInUiState.Loading,
            ) {
                Text("Googleで連携")
            }
        }

        if (uiState is SignInUiState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = (uiState as SignInUiState.Error).message,
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}
