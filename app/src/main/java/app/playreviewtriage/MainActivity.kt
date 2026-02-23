package app.playreviewtriage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.playreviewtriage.ui.navigation.AppNavHost
import app.playreviewtriage.ui.theme.PlayReviewTriageTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlayReviewTriageTheme {
                AppNavHost()
            }
        }
    }
}
