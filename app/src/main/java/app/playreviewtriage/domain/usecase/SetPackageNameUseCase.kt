package app.playreviewtriage.domain.usecase

import app.playreviewtriage.domain.repository.ConfigRepository
import app.playreviewtriage.domain.repository.ReviewRepository
import javax.inject.Inject

class SetPackageNameUseCase @Inject constructor(
    private val configRepository: ConfigRepository,
    private val reviewRepository: ReviewRepository,
) {
    suspend fun invoke(packageName: String): Result<Unit> {
        if (packageName.isBlank()) {
            return Result.failure(IllegalArgumentException("Package name must not be blank."))
        }
        val pattern = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")
        if (!pattern.matches(packageName)) {
            return Result.failure(
                IllegalArgumentException("正しいパッケージ名を入力してください（例: com.example.app）")
            )
        }
        val accessResult = reviewRepository.checkAccess(packageName)
        if (accessResult.isFailure) return accessResult
        configRepository.setPackageName(packageName)
        return Result.success(Unit)
    }
}
