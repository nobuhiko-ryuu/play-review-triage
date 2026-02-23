package app.playreviewtriage.core.result

sealed class AppError {
    data object AuthExpired : AppError()   // 401
    data object Forbidden : AppError()    // 403
    data object Network : AppError()
    data object RateLimited : AppError()  // 429
    data class Unknown(val message: String?) : AppError()
}
