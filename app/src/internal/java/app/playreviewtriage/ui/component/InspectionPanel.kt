package app.playreviewtriage.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.playreviewtriage.data.fake.FakeScenario

@Composable
fun InspectionPanel(viewModel: InspectionPanelViewModel = hiltViewModel()) {
    val current by viewModel.scenario.collectAsState()

    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("🔧 検査パネル（internal専用）", style = MaterialTheme.typography.titleSmall)
            Text(
                "現在のシナリオ: ${current.displayName}",
                style = MaterialTheme.typography.bodyMedium,
            )
            HorizontalDivider()
            FakeScenario.entries.forEach { scenario ->
                OutlinedButton(
                    onClick = { viewModel.setScenario(scenario) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (current == scenario)
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        )
                    else ButtonDefaults.outlinedButtonColors(),
                ) {
                    Text(scenario.displayName)
                }
            }
        }
    }
}
