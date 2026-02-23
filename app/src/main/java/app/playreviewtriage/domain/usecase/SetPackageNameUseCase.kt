package app.playreviewtriage.domain.usecase

import app.playreviewtriage.domain.repository.ConfigRepository
import javax.inject.Inject

class SetPackageNameUseCase @Inject constructor(
    private val configRepository: ConfigRepository,
) {
    suspend fun invoke(packageName: String): Result<Unit> {
        if (packageName.isBlank()) {
            return Result.failure(IllegalArgumentException("Package name must not be blank."))
        }
        val pattern = Regex("^[a-zA-Z0-9_.]+$")
        if (!pattern.matches(packageName)) {
            return Result.failure(
                IllegalArgumentException(
                    "Package name must match ^[a-zA-Z0-9_.]+\$. Got: \"$packageName\""
                )
            )
        }
        configRepository.setPackageName(packageName)
        return Result.success(Unit)
    }
}
