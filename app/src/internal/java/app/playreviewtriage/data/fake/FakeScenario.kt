package app.playreviewtriage.data.fake

enum class FakeScenario(val displayName: String) {
    SUCCESS("✅ 成功（正常5件）"),
    EMPTY("📭 成功（0件）"),
    AUTH_401("🔑 401 認証エラー"),
    FORBIDDEN_403("🚫 403 権限なし"),
    NETWORK_ERROR("📡 ネットワークエラー"),
    RATE_LIMIT("⏱ 429 レート制限"),
}
