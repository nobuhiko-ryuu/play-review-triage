package app.playreviewtriage.data.fake

import app.playreviewtriage.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake実装：internal ビルド (USE_FAKE_DATA=true) 用。
 * 常にサインイン済みとして扱い、SignIn 画面をスキップする。
 */
@Singleton
class FakeAuthRepository @Inject constructor() : AuthRepository {

    override suspend fun completeSignIn(accountName: String): Result<Unit> =
        Result.success(Unit)

    override fun consumeRecoveryIntent(): android.content.Intent? = null

    override suspend fun signOut() { /* no-op */ }

    override suspend fun getValidAccessTokenOrNull(): String = "fake_token"

    override fun isSignedIn(): Boolean = true
}
