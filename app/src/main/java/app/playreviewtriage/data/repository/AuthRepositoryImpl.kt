package app.playreviewtriage.data.repository

import app.playreviewtriage.data.prefs.datastore.TokenStore
import app.playreviewtriage.domain.repository.AuthRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AuthRepository の実装。
 *
 * signIn() は現時点ではスタブ実装。
 * 実際の Google Sign-In には play-services-auth または Credential Manager の追加が必要。
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val tokenStore: TokenStore,
) : AuthRepository {

    /**
     * TODO: Google Sign-In の実際の実装は別途追加が必要。
     * play-services-auth または Credential Manager の依存関係を追加し、
     * 取得したトークンを tokenStore.saveToken() で保存すること。
     */
    override suspend fun signIn(): Result<Unit> =
        Result.failure(NotImplementedError("Google Sign-In の実装が必要です。play-services-auth または Credential Manager の追加が必要です。"))

    override suspend fun signOut() {
        tokenStore.clearToken()
    }

    override suspend fun getValidAccessTokenOrNull(): String? =
        tokenStore.getAccessToken().first()?.takeIf { it.isNotEmpty() }

    override fun isSignedIn(): Boolean = runBlocking {
        tokenStore.getAccessToken().first()?.isNotEmpty() == true
    }
}
