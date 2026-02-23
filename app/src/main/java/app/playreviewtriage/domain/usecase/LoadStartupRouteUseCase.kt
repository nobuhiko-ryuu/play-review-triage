package app.playreviewtriage.domain.usecase

import app.playreviewtriage.domain.repository.AuthRepository
import app.playreviewtriage.domain.repository.ConfigRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

sealed class StartupRoute {
    data object SignIn : StartupRoute()
    data object Setup : StartupRoute()
    data object Today : StartupRoute()
}

class LoadStartupRouteUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val configRepository: ConfigRepository,
) {
    suspend fun invoke(): StartupRoute {
        if (!authRepository.isSignedIn()) {
            return StartupRoute.SignIn
        }
        val config = configRepository.configFlow.first()
        if (config.packageName.isBlank()) {
            return StartupRoute.Setup
        }
        return StartupRoute.Today
    }
}
