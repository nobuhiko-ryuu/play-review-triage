package app.playreviewtriage.domain.repository

interface AuthRepository {
    /** Google Sign-In で取得したアカウント名を受け取り、アクセストークンを取得・保存する */
    suspend fun completeSignIn(accountName: String): Result<Unit>
    /** スコープ許可が必要な場合に取得できるリカバリIntent（一度読んだらnullになる） */
    fun consumeRecoveryIntent(): android.content.Intent?
    suspend fun signOut()
    suspend fun getValidAccessTokenOrNull(): String?
    fun isSignedIn(): Boolean
}
