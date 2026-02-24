# Internal 検査モード強化 実装プラン

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** internal buildType で 401/403/Network/Empty/RateLimit シナリオを再現できる検査モードを追加し、Setup保存時に checkAccess 疎通チェックを本番・Fake 共通で入れる。

**Architecture:**
- `ReviewRepository.checkAccess()` をドメイン層に追加し、Real/Fake 両実装に入れる
- `FakeScenario` enum と `InternalTestStore`（DataStore）を `src/internal/` に置きシナリオを永続化
- `InspectionPanel` Composable をソースセット分割（main=no-op / internal=切替UI）
- `SetPackageNameUseCase` を「形式チェック → checkAccess → 保存」に強化

**Tech Stack:** Kotlin / Hilt / Jetpack Compose / DataStore Preferences / Retrofit

---

## 現状の重要ファイル

```
src/main/.../domain/repository/ReviewRepository.kt       ← checkAccess を追加する
src/main/.../data/repository/ReviewRepositoryImpl.kt     ← checkAccess を実装する
src/internal/.../data/fake/FakeReviewRepository.kt       ← シナリオ駆動に書き換え
src/main/.../domain/usecase/SetPackageNameUseCase.kt     ← checkAccess ステップ追加
src/main/.../presentation/viewmodel/SetupViewModel.kt    ← エラー分類の拡充
src/main/.../ui/screen/settings/SettingsScreen.kt        ← InspectionPanel 呼び出し追加
src/main/.../data/api/service/PublisherService.kt        ← listReviews(maxResults=1) を使う
```

---

## Task A: ReviewRepository に checkAccess を追加し Real 実装

### A-1) ReviewRepository.kt にメソッド追加

**File:** `app/src/main/java/app/playreviewtriage/domain/repository/ReviewRepository.kt`

```kotlin
interface ReviewRepository {
    val reviewsFlow: Flow<List<Review>>
    suspend fun syncNow(packageName: String): Result<SyncSummary>
    suspend fun getReview(reviewId: String): Review?
    suspend fun deleteExpired(retentionDays: Int)
    /** パッケージ名のアクセス権を疎通チェック（DB保存なし） */
    suspend fun checkAccess(packageName: String): Result<Unit>
}
```

### A-2) ReviewRepositoryImpl.kt に実装追加

**File:** `app/src/main/java/app/playreviewtriage/data/repository/ReviewRepositoryImpl.kt`

既存の `deleteExpired` の後に追加：

```kotlin
override suspend fun checkAccess(packageName: String): Result<Unit> {
    return try {
        val response = service.listReviews(packageName, maxResults = 1)
        if (response.isSuccessful) Result.success(Unit)
        else httpCodeToAppError(response.code()).toFailure()
    } catch (e: IOException) {
        AppError.Network.toFailure()
    }
}
```

### A-3) コミット
```bash
git add app/src/main/java/app/playreviewtriage/domain/repository/ReviewRepository.kt
git add app/src/main/java/app/playreviewtriage/data/repository/ReviewRepositoryImpl.kt
git commit -m "feat(domain): ReviewRepository に checkAccess(packageName) を追加"
```

---

## Task B: FakeScenario enum と InternalTestStore を src/internal に作成

### B-1) FakeScenario.kt

**File:** `app/src/internal/java/app/playreviewtriage/data/fake/FakeScenario.kt`

```kotlin
package app.playreviewtriage.data.fake

enum class FakeScenario(val displayName: String) {
    SUCCESS("✅ 成功（正常5件）"),
    EMPTY("📭 成功（0件）"),
    AUTH_401("🔑 401 認証エラー"),
    FORBIDDEN_403("🚫 403 権限なし"),
    NETWORK_ERROR("📡 ネットワークエラー"),
    RATE_LIMIT("⏱ 429 レート制限"),
}
```

### B-2) InternalTestStore.kt

**File:** `app/src/internal/java/app/playreviewtriage/data/fake/InternalTestStore.kt`

```kotlin
package app.playreviewtriage.data.fake

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.internalTestDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "internal_test")

@Singleton
class InternalTestStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val KEY_SCENARIO = stringPreferencesKey("scenario")

    val scenario: Flow<FakeScenario> = context.internalTestDataStore.data.map { prefs ->
        prefs[KEY_SCENARIO]?.let { runCatching { FakeScenario.valueOf(it) }.getOrNull() }
            ?: FakeScenario.SUCCESS
    }

    suspend fun setScenario(scenario: FakeScenario) {
        context.internalTestDataStore.edit { prefs ->
            prefs[KEY_SCENARIO] = scenario.name
        }
    }
}
```

### B-3) コミット
```bash
git add app/src/internal/java/app/playreviewtriage/data/fake/FakeScenario.kt
git add app/src/internal/java/app/playreviewtriage/data/fake/InternalTestStore.kt
git commit -m "feat(internal): FakeScenario enum と InternalTestStore を追加"
```

---

## Task C: FakeReviewRepository をシナリオ駆動に書き換え

**File:** `app/src/internal/java/app/playreviewtriage/data/fake/FakeReviewRepository.kt`

`InternalTestStore` を inject し、`syncNow` と `checkAccess` をシナリオ応答に変更。

```kotlin
package app.playreviewtriage.data.fake

import app.playreviewtriage.core.result.AppError
import app.playreviewtriage.core.result.toFailure
import app.playreviewtriage.domain.entity.Importance
import app.playreviewtriage.domain.entity.ReasonTag
import app.playreviewtriage.domain.entity.Review
import app.playreviewtriage.domain.entity.SyncSummary
import app.playreviewtriage.domain.repository.ReviewRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeReviewRepository @Inject constructor(
    private val testStore: InternalTestStore,
) : ReviewRepository {

    private val _reviews = MutableStateFlow(SEED_REVIEWS)
    override val reviewsFlow: Flow<List<Review>> = _reviews.asStateFlow()

    override suspend fun checkAccess(packageName: String): Result<Unit> =
        when (testStore.scenario.first()) {
            FakeScenario.AUTH_401    -> AppError.AuthExpired.toFailure()
            FakeScenario.FORBIDDEN_403 -> AppError.Forbidden.toFailure()
            FakeScenario.NETWORK_ERROR -> AppError.Network.toFailure()
            FakeScenario.RATE_LIMIT  -> AppError.RateLimited.toFailure()
            else                     -> Result.success(Unit)
        }

    override suspend fun syncNow(packageName: String): Result<SyncSummary> {
        return when (testStore.scenario.first()) {
            FakeScenario.SUCCESS -> {
                val current = _reviews.value.toMutableList()
                current.add(
                    Review(
                        reviewId = "fake-new-${System.currentTimeMillis()}",
                        authorName = "新規ユーザー",
                        starRating = 2,
                        text = "アップデートしたら動作が重くなりました。改善をお願いします。",
                        lastModifiedEpochSec = System.currentTimeMillis() / 1000,
                        appVersionName = "2.1.0",
                        androidOsVersion = 14,
                        deviceManufacturer = "Google",
                        deviceModel = "Pixel 8",
                        importance = Importance.MID,
                        reasonTags = setOf(ReasonTag.UI),
                        fetchedAtEpochSec = System.currentTimeMillis() / 1000,
                    )
                )
                _reviews.value = current
                Result.success(SyncSummary(fetchedCount = 1, highCount = 0))
            }
            FakeScenario.EMPTY -> {
                _reviews.value = emptyList()
                Result.success(SyncSummary(fetchedCount = 0, highCount = 0))
            }
            FakeScenario.AUTH_401    -> AppError.AuthExpired.toFailure()
            FakeScenario.FORBIDDEN_403 -> AppError.Forbidden.toFailure()
            FakeScenario.NETWORK_ERROR -> AppError.Network.toFailure()
            FakeScenario.RATE_LIMIT  -> AppError.RateLimited.toFailure()
        }
    }

    override suspend fun getReview(reviewId: String): Review? =
        _reviews.value.find { it.reviewId == reviewId }

    override suspend fun deleteExpired(retentionDays: Int) { /* no-op */ }

    companion object {
        private val BASE_TIME = System.currentTimeMillis() / 1000
        val SEED_REVIEWS = listOf(
            Review(
                reviewId = "fake-001",
                authorName = "田中 太郎",
                starRating = 1,
                text = "起動直後にクラッシュします。Pixel 7で再現しました。早急に修正をお願いします。バックグラウンドから復帰するたびに落ちるので使い物になりません。",
                lastModifiedEpochSec = BASE_TIME - 3600,
                appVersionName = "2.0.1",
                androidOsVersion = 14,
                deviceManufacturer = "Google",
                deviceModel = "Pixel 7",
                importance = Importance.HIGH,
                reasonTags = setOf(ReasonTag.CRASH),
                fetchedAtEpochSec = BASE_TIME,
            ),
            Review(
                reviewId = "fake-002",
                authorName = "鈴木 花子",
                starRating = 1,
                text = "課金したのにアイテムが付与されませんでした。サポートに連絡しても返答がありません。返金を求めます。",
                lastModifiedEpochSec = BASE_TIME - 7200,
                appVersionName = "2.0.0",
                androidOsVersion = 13,
                deviceManufacturer = "Samsung",
                deviceModel = "Galaxy S23",
                importance = Importance.HIGH,
                reasonTags = setOf(ReasonTag.BILLING),
                fetchedAtEpochSec = BASE_TIME,
            ),
            Review(
                reviewId = "fake-003",
                authorName = "佐藤 次郎",
                starRating = 2,
                text = "ボタンが小さくて押しにくいです。特にホーム画面の右下のアイコンはタップしにくい。UIの改善をお願いします。",
                lastModifiedEpochSec = BASE_TIME - 10800,
                appVersionName = "2.0.1",
                androidOsVersion = 13,
                deviceManufacturer = "Sony",
                deviceModel = "Xperia 1 V",
                importance = Importance.MID,
                reasonTags = setOf(ReasonTag.UI),
                fetchedAtEpochSec = BASE_TIME,
            ),
            Review(
                reviewId = "fake-004",
                authorName = "山田 美咲",
                starRating = 5,
                text = "とても使いやすいアプリです！毎日使っています。",
                lastModifiedEpochSec = BASE_TIME - 14400,
                appVersionName = "2.0.1",
                androidOsVersion = 14,
                deviceManufacturer = "Google",
                deviceModel = "Pixel 8 Pro",
                importance = Importance.LOW,
                reasonTags = setOf(ReasonTag.NOISE),
                fetchedAtEpochSec = BASE_TIME,
            ),
            Review(
                reviewId = "fake-005",
                authorName = "伊藤 健一",
                starRating = 3,
                text = "機能は良いのですが、通知が来ない場合があります。バグかもしれません。",
                lastModifiedEpochSec = BASE_TIME - 18000,
                appVersionName = "1.9.5",
                androidOsVersion = 12,
                deviceManufacturer = "SHARP",
                deviceModel = "AQUOS sense7",
                importance = Importance.MID,
                reasonTags = setOf(ReasonTag.OTHER),
                fetchedAtEpochSec = BASE_TIME,
            ),
        )
    }
}
```

コミット:
```bash
git add app/src/internal/java/app/playreviewtriage/data/fake/FakeReviewRepository.kt
git commit -m "feat(internal): FakeReviewRepository をシナリオ駆動に変更（checkAccess / syncNow 対応）"
```

---

## Task D: SetPackageNameUseCase と SetupViewModel を更新

### D-1) SetPackageNameUseCase.kt

**File:** `app/src/main/java/app/playreviewtriage/domain/usecase/SetPackageNameUseCase.kt`

```kotlin
package app.playreviewtriage.domain.usecase

import app.playreviewtriage.domain.repository.ConfigRepository
import app.playreviewtriage.domain.repository.ReviewRepository
import javax.inject.Inject

class SetPackageNameUseCase @Inject constructor(
    private val configRepository: ConfigRepository,
    private val reviewRepository: ReviewRepository,
) {
    suspend fun invoke(packageName: String): Result<Unit> {
        if (packageName.isBlank()) {
            return Result.failure(IllegalArgumentException("Package name must not be blank."))
        }
        val pattern = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")
        if (!pattern.matches(packageName)) {
            return Result.failure(
                IllegalArgumentException("正しいパッケージ名を入力してください（例: com.example.app）")
            )
        }
        val accessResult = reviewRepository.checkAccess(packageName)
        if (accessResult.isFailure) return accessResult
        configRepository.setPackageName(packageName)
        return Result.success(Unit)
    }
}
```

### D-2) SetupViewModel.kt の全エラー型対応

**File:** `app/src/main/java/app/playreviewtriage/presentation/viewmodel/SetupViewModel.kt`

`save()` の `onFailure` ブロックを以下に置き換える：

```kotlin
onFailure = { e ->
    when {
        e is IllegalArgumentException ->
            SetupUiState.ValidationError(e.message ?: "入力値が正しくありません。")
        e is AppException -> when (e.error) {
            AppError.AuthExpired ->
                SetupUiState.ApiError("認証が切れています。ログアウトして再度サインインしてください。")
            AppError.Forbidden ->
                SetupUiState.ApiError("このアカウントは対象アプリにアクセスできません。\nPlay Consoleで権限を確認してください。")
            AppError.Network ->
                SetupUiState.ApiError("通信エラーが発生しました。ネットワーク接続を確認してください。")
            AppError.RateLimited ->
                SetupUiState.ApiError("リクエスト制限に達しました。しばらく待ってから再試行してください。")
            is AppError.Unknown ->
                SetupUiState.ApiError(e.error.message ?: "予期しないエラーが発生しました。")
        }
        else -> SetupUiState.ApiError("予期しないエラーが発生しました。")
    }
}
```

コミット:
```bash
git add app/src/main/java/app/playreviewtriage/domain/usecase/SetPackageNameUseCase.kt
git add app/src/main/java/app/playreviewtriage/presentation/viewmodel/SetupViewModel.kt
git commit -m "feat: SetPackageNameUseCase に checkAccess 疎通チェックを追加・SetupViewModel 全エラー型対応"
```

---

## Task E: InspectionPanel をソースセット分割で作成

### E-1) src/main: no-op 版

**File:** `app/src/main/java/app/playreviewtriage/ui/component/InspectionPanel.kt`

```kotlin
package app.playreviewtriage.ui.component

import androidx.compose.runtime.Composable

@Composable
fun InspectionPanel() {
    // debug / release では表示なし
}
```

### E-2) src/internal: InspectionPanelViewModel

**File:** `app/src/internal/java/app/playreviewtriage/ui/component/InspectionPanelViewModel.kt`

```kotlin
package app.playreviewtriage.ui.component

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.playreviewtriage.data.fake.FakeScenario
import app.playreviewtriage.data.fake.InternalTestStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InspectionPanelViewModel @Inject constructor(
    private val store: InternalTestStore,
) : ViewModel() {

    val scenario = store.scenario.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        FakeScenario.SUCCESS,
    )

    fun setScenario(scenario: FakeScenario) {
        viewModelScope.launch { store.setScenario(scenario) }
    }
}
```

### E-3) src/internal: 実際の InspectionPanel UI

**File:** `app/src/internal/java/app/playreviewtriage/ui/component/InspectionPanel.kt`

```kotlin
package app.playreviewtriage.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.playreviewtriage.data.fake.FakeScenario

@Composable
fun InspectionPanel(viewModel: InspectionPanelViewModel = hiltViewModel()) {
    val current by viewModel.scenario.collectAsState()

    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("🔧 検査パネル（internal専用）", style = MaterialTheme.typography.titleSmall)
            Text(
                "現在のシナリオ: ${current.displayName}",
                style = MaterialTheme.typography.bodyMedium,
            )
            HorizontalDivider()
            FakeScenario.entries.forEach { scenario ->
                OutlinedButton(
                    onClick = { viewModel.setScenario(scenario) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (current == scenario)
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        )
                    else ButtonDefaults.outlinedButtonColors(),
                ) {
                    Text(scenario.displayName)
                }
            }
        }
    }
}
```

コミット:
```bash
git add app/src/main/java/app/playreviewtriage/ui/component/InspectionPanel.kt
git add app/src/internal/java/app/playreviewtriage/ui/component/
git commit -m "feat(internal): InspectionPanel 検査パネルをソースセット分割で追加"
```

---

## Task F: SettingsScreen に InspectionPanel を追加

**File:** `app/src/main/java/app/playreviewtriage/ui/screen/settings/SettingsScreen.kt`

1. import 追加: `import app.playreviewtriage.ui.component.InspectionPanel`
2. `Column` の末尾（`HorizontalDivider()` と保存日数テキストの後）に追加:

```kotlin
            HorizontalDivider()

            InspectionPanel()

            Text(...)  // 既存の保存日数テキスト
```

実際には `HorizontalDivider()` の後に `InspectionPanel()` を挿入する。

コミット:
```bash
git add app/src/main/java/app/playreviewtriage/ui/screen/settings/SettingsScreen.kt
git commit -m "feat: SettingsScreen に InspectionPanel を追加（internal では検査パネルが表示される）"
```

---

## Task G: 全 buildType クリーンビルド確認 & push

```bash
./gradlew clean :app:assembleInternal :app:assembleDebug :app:assembleRelease
./gradlew testDebugUnitTest
git push
```

全て BUILD SUCCESSFUL であること。

---

## 完了チェックリスト

- [ ] `ReviewRepository.checkAccess()` が interface に存在する
- [ ] `ReviewRepositoryImpl.checkAccess()` が listReviews maxResults=1 で実装されている
- [ ] `FakeReviewRepository.checkAccess()` がシナリオに応じて success/failure を返す
- [ ] `SetPackageNameUseCase` が「形式チェック → checkAccess → 保存」の順で動く
- [ ] `SetupViewModel` が 401/403/Network/RateLimit を個別メッセージで表示する
- [ ] internal ビルドの Settings 画面に検査パネルが表示される
- [ ] debug/release ビルドの Settings 画面に検査パネルが表示されない
- [ ] 全3バリアント BUILD SUCCESSFUL
