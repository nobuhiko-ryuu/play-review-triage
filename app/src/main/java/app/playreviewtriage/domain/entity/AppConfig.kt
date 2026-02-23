package app.playreviewtriage.domain.entity

data class AppConfig(
    val packageName: String = "",
    val lastSyncEpochSec: Long = 0L,
    val retentionDays: Int = 30,
)
