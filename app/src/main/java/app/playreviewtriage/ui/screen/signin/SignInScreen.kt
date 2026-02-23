package app.playreviewtriage.ui.screen.signin

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.playreviewtriage.presentation.uistate.SignInUiState
import app.playreviewtriage.presentation.viewmodel.SignInViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun SignInScreen(
    viewModel: SignInViewModel = hiltViewModel(),
    onSuccess: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Google アカウント選択後の結果処理
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    .getResult(ApiException::class.java)
                val accountName = account.account?.name
                if (accountName != null) {
                    viewModel.completeSignIn(accountName)
                } else {
                    viewModel.onSignInFailed()
                }
            } catch (e: ApiException) {
                viewModel.onSignInFailed()
            }
        } else {
            viewModel.onSignInFailed()
        }
    }

    // スコープ未許可時のリカバリダイアログ結果処理
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.retryAfterPermissionGrant()
        } else {
            viewModel.onSignInFailed()
        }
    }

    // recoveryIntent が来たらリカバリダイアログを起動
    LaunchedEffect(Unit) {
        viewModel.recoveryIntent.collect { intent ->
            permissionLauncher.launch(intent)
        }
    }

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
                onClick = {
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .build()
                    val signInClient = GoogleSignIn.getClient(context, gso)
                    // 前回のセッションをクリアしてアカウント選択画面を強制表示
                    signInClient.signOut().addOnCompleteListener {
                        signInLauncher.launch(signInClient.signInIntent)
                    }
                },
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
