package app.playreviewtriage.data.api

import app.playreviewtriage.core.result.AppError

fun httpCodeToAppError(code: Int): AppError = when (code) {
    401 -> AppError.AuthExpired
    403 -> AppError.Forbidden
    404 -> AppError.Unknown("アプリが見つかりません。パッケージ名を確認してください。")
    429 -> AppError.RateLimited
    else -> AppError.Unknown("予期しないエラーが発生しました。(HTTP $code)")
}
