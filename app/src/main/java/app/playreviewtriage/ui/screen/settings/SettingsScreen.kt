package app.playreviewtriage.ui.screen.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.playreviewtriage.presentation.viewmodel.SettingsViewModel
import app.playreviewtriage.ui.component.InspectionPanel

private const val TAG = "SettingsScreen"

/** 通知の現在状態 */
private enum class NotifState { PERMISSION_DENIED, NOTIFICATIONS_OFF, ENABLED }

private fun currentNotifState(context: android.content.Context): NotifState {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return NotifState.PERMISSION_DENIED
    }
    if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
        return NotifState.NOTIFICATIONS_OFF
    }
    return NotifState.ENABLED
}

private fun openNotificationSettings(context: android.content.Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Log.w(TAG, "通知設定画面を開けませんでした", e)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onLogout: () -> Unit,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var notifState by remember { mutableStateOf(currentNotifState(context)) }

    // OS設定から戻ったときに状態を再評価
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notifState = currentNotifState(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> notifState = currentNotifState(context) }

    LaunchedEffect(uiState.message) {
        if (uiState.message != null) viewModel.clearMessage()
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("ログアウト") },
            text = { Text("ログアウトしますか？") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.logout()
                    onLogout()
                }) { Text("ログアウト") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("キャンセル") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "戻る")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 監視アプリ
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("監視アプリ", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = uiState.packageName.ifBlank { "未設定" },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            // 通知セクション
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("通知", style = MaterialTheme.typography.titleSmall)
                    val stateLabel = when (notifState) {
                        NotifState.PERMISSION_DENIED -> "未許可"
                        NotifState.NOTIFICATIONS_OFF -> "通知OFF"
                        NotifState.ENABLED -> "許可済み"
                    }
                    Text(
                        "現在の状態：$stateLabel",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    // Android 13+ かつ未許可 → OSダイアログで許可要求
                    if (notifState == NotifState.PERMISSION_DENIED) {
                        OutlinedButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("通知を許可する") }
                    }
                    // 状態に関わらず常にOS設定への導線を提供
                    OutlinedButton(
                        onClick = { openNotificationSettings(context) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("通知設定を開く") }
                }
            }

            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("ログアウト")
            }

            HorizontalDivider()

            InspectionPanel()

            Text(
                "レビューは端末内に最大${uiState.retentionDays}日間保存されます。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
