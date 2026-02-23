package app.playreviewtriage.core.result

/**
 * AppError をラップした例外。
 * UseCase / Repository は Result<T> を返し、失敗時はこの例外を使う。
 * UI 層では AppException.error を取り出して文言を決定する。
 */
class AppException(val error: AppError) : Exception(error.toString())

fun <T> AppError.toFailure(): Result<T> = Result.failure(AppException(this))
