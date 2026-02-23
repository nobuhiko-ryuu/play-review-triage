package app.playreviewtriage.ui.screen.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.playreviewtriage.domain.entity.Importance
import app.playreviewtriage.domain.entity.ReasonTag
import app.playreviewtriage.presentation.uistate.DetailUiState
import app.playreviewtriage.presentation.viewmodel.DetailViewModel
import app.playreviewtriage.ui.component.LoadingView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewDetailScreen(
    reviewId: String,
    viewModel: DetailViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(reviewId) { viewModel.load(reviewId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("詳細") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "戻る")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is DetailUiState.Loading -> LoadingView()
                is DetailUiState.NotFound -> {
                    Box(Modifier.fillMaxSize()) {
                        Text("レビューが見つかりませんでした。", Modifier.padding(16.dp))
                    }
                }
                is DetailUiState.Success -> {
                    val review = state.review
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("★${review.starRating}", style = MaterialTheme.typography.headlineMedium)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ImportanceBadge(review.importance)
                            review.reasonTags.forEach { tag -> TagChip(tag) }
                        }

                        HorizontalDivider()
                        Text(review.text, style = MaterialTheme.typography.bodyLarge)
                        HorizontalDivider()

                        review.appVersionName?.let { Text("バージョン: $it", style = MaterialTheme.typography.bodySmall) }
                        review.deviceManufacturer?.let { Text("メーカー: $it ${review.deviceModel ?: ""}", style = MaterialTheme.typography.bodySmall) }

                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/console"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Play Consoleで対応する")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportanceBadge(importance: Importance) {
    val (label, color) = when (importance) {
        Importance.HIGH -> "HIGH" to Color(0xFFB00020)
        Importance.MID -> "MID" to Color(0xFFE65100)
        Importance.LOW -> "LOW" to Color.Gray
    }
    Surface(color = color, shape = MaterialTheme.shapes.small) {
        Text(label, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Color.White, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun TagChip(tag: ReasonTag) {
    val label = when (tag) {
        ReasonTag.CRASH -> "クラッシュ"; ReasonTag.BILLING -> "課金"
        ReasonTag.UI -> "UI"; ReasonTag.NOISE -> "ノイズ"; ReasonTag.OTHER -> "その他"
    }
    SuggestionChip(onClick = {}, label = { Text(label) })
}
