package app.playreviewtriage.ui.screen.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.playreviewtriage.domain.entity.Importance
import app.playreviewtriage.domain.entity.ReasonTag
import app.playreviewtriage.domain.entity.Review
import app.playreviewtriage.presentation.uistate.TodayUiState
import app.playreviewtriage.presentation.viewmodel.TodayViewModel
import app.playreviewtriage.ui.component.EmptyView
import app.playreviewtriage.ui.component.ErrorView
import app.playreviewtriage.ui.component.LoadingView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    viewModel: TodayViewModel = hiltViewModel(),
    onNavigateToDetail: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Today") },
                actions = {
                    IconButton(onClick = { viewModel.sync() }) { Icon(Icons.Default.Refresh, "更新") }
                    IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, "設定") }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is TodayUiState.Loading -> LoadingView()
                is TodayUiState.Success -> {
                    Column(Modifier.fillMaxSize()) {
                        Text(
                            text = state.lastSyncLabel,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                        if (state.isSyncing) LinearProgressIndicator(Modifier.fillMaxWidth())
                        if (state.top3.isEmpty()) {
                            EmptyView(onRefresh = { viewModel.sync() })
                        } else {
                            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(state.top3) { review ->
                                    ReviewCard(review = review, onClick = { onNavigateToDetail(review.reviewId) })
                                }
                            }
                        }
                    }
                }
                is TodayUiState.Error -> {
                    Column(Modifier.fillMaxSize()) {
                        if (state.cachedTop3.isNotEmpty()) {
                            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(state.cachedTop3) { review ->
                                    ReviewCard(review = review, onClick = { onNavigateToDetail(review.reviewId) })
                                }
                            }
                        }
                        ErrorView(message = state.message, onRetry = { viewModel.sync() })
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewCard(review: Review, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("★${review.starRating}", style = MaterialTheme.typography.titleSmall)
                ImportanceBadge(review.importance)
                review.reasonTags.forEach { tag -> TagChip(tag) }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = review.text.take(60) + if (review.text.length > 60) "…" else "",
                style = MaterialTheme.typography.bodyMedium,
            )
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
        Text(label, Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun TagChip(tag: ReasonTag) {
    val label = when (tag) {
        ReasonTag.CRASH -> "クラッシュ"
        ReasonTag.BILLING -> "課金"
        ReasonTag.UI -> "UI"
        ReasonTag.NOISE -> "ノイズ"
        ReasonTag.OTHER -> "その他"
    }
    SuggestionChip(onClick = {}, label = { Text(label, style = MaterialTheme.typography.labelSmall) })
}
