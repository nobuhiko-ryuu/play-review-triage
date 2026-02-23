package app.playreviewtriage.data.repository

import android.accounts.Account
import android.content.Context
import app.playreviewtriage.core.result.AppError
import app.playreviewtriage.core.result.toFailure
import app.playreviewtriage.data.prefs.datastore.TokenStore
import app.playreviewtriage.domain.repository.AuthRepository
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenStore: TokenStore,
) : AuthRepository {

    companion object {
        private const val PUBLISHER_SCOPE =
            "oauth2:https://www.googleapis.com/auth/androidpublisher"
    }

    override suspend fun completeSignIn(accountName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val account = Account(accountName, "com.google")
                val token = GoogleAuthUtil.getToken(context, account, PUBLISHER_SCOPE)
                // Google OAuth2 アクセストークンの有効期限は通常 3600 秒
                val expiryEpochSec = System.currentTimeMillis() / 1000 + 3600L
                tokenStore.saveToken(token, expiryEpochSec)
                Result.success(Unit)
            } catch (e: UserRecoverableAuthException) {
                AppError.AuthExpired.toFailure()
            } catch (e: GoogleAuthException) {
                AppError.Forbidden.toFailure()
            } catch (e: Exception) {
                AppError.Network.toFailure()
            }
        }

    override suspend fun signOut() {
        tokenStore.clearToken()
    }

    override suspend fun getValidAccessTokenOrNull(): String? =
        tokenStore.getAccessToken().first()?.takeIf { it.isNotEmpty() }

    override fun isSignedIn(): Boolean = runBlocking {
        tokenStore.getAccessToken().first()?.isNotEmpty() == true
    }
}
