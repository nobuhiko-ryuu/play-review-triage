package app.playreviewtriage.domain.repository

interface AuthRepository {
    suspend fun signIn(): Result<Unit>
    suspend fun signOut()
    suspend fun getValidAccessTokenOrNull(): String?
    fun isSignedIn(): Boolean
}
