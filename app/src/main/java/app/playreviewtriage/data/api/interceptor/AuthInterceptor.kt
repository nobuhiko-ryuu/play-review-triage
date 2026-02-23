package app.playreviewtriage.data.api.interceptor

import app.playreviewtriage.data.prefs.datastore.TokenStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp インターセプター。
 * TokenStore からトークンを取得し、Bearer ヘッダーを付与する。
 *
 * runBlocking を使って DataStore から同期的にトークンを取得する（MVP では許容）。
 * トークンが null または空の場合はヘッダーなしでそのまま通す。
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenStore.getAccessToken().first() }

        val request = if (!token.isNullOrEmpty()) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }

        return chain.proceed(request)
    }
}
