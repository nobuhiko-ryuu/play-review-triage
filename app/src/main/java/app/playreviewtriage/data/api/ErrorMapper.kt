package app.playreviewtriage.data.api

import app.playreviewtriage.core.result.AppError

fun httpCodeToAppError(code: Int): AppError = when (code) {
    401 -> AppError.AuthExpired
    403 -> AppError.Forbidden
    429 -> AppError.RateLimited
    else -> AppError.Unknown("HTTP $code")
}
