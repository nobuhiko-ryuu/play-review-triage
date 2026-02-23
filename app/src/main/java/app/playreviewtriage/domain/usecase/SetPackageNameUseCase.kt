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
        // Androidパッケージ名は「英数字・_・.」のみ、かつドットで区切られた2セグメント以上必要
        val pattern = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")
        if (!pattern.matches(packageName)) {
            return Result.failure(
                IllegalArgumentException(
                    "正しいパッケージ名を入力してください（例: com.example.app）"
                )
            )
        }
        configRepository.setPackageName(packageName)
        return Result.success(Unit)
    }
}
