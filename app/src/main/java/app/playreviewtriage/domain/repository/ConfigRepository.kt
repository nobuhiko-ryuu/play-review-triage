package app.playreviewtriage.domain.repository

import app.playreviewtriage.domain.entity.AppConfig
import kotlinx.coroutines.flow.Flow

interface ConfigRepository {
    val configFlow: Flow<AppConfig>
    suspend fun setPackageName(packageName: String)
    suspend fun setRetentionDays(days: Int)
    suspend fun updateLastSync(epochSec: Long)
}
