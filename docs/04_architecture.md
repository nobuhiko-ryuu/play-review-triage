````markdown id="arch_v1"
# アーキテクチャ設計書：Play Review Triage（仮称）
最終更新: 2026-02-22  
対象: Android（Kotlin + Jetpack Compose）  
目的: **実装が迷子にならないための“構造”と“責務分担”を固定**し、保守・拡張しやすい形でMVPを最短リリースする。

---

## 1. アーキ方針（最重要ルール）

### 1.1 設計思想
- **薄く、単純に、差し替え可能に**
- MVPでは「正確さ100%」より「時間を9割減らす」を優先
- 公式Play Consoleと正面衝突しない：**“読む量削減・優先度付け”が主役**

### 1.2 大原則（迷ったらここに戻る）
1. UIは「表示と操作」だけ（ビジネスロジック禁止）
2. 重要ロジックはDomainに集約（テストしやすい）
3. 外部要因（API/DB/Storage/Clock）はData層に隔離（差し替え可能）
4. 例外を投げっぱなしにしない（UIに出すメッセージへ変換）
5. トークンなど機微情報はログに出さない

---

## 2. 採用技術スタック（MVP）

### 2.1 UI/状態管理
- Jetpack Compose
- Navigation Compose（画面遷移）
- MVVM（ViewModelがUI状態を持つ）
- 状態: `StateFlow<UiState>` + イベント: `SharedFlow<UiEvent>`（最小）

### 2.2 非同期/DI/永続化
- Kotlin Coroutines + Flow
- Hilt（依存性注入）
- Room（レビューキャッシュ）
- DataStore（トークン/設定）

### 2.3 通信/同期/通知/観測
- Retrofit + OkHttp（Android Publisher API）
- WorkManager（日次同期 + 通知）
- Crashlytics（クラッシュ回収）
- ログ（Timber等は任意。トークンは絶対出さない）

> 注: サーバは作らない（MVPは端末完結）。レビュー本文は外部送信しない。

---

## 3. レイヤ構造（Clean寄りの現実解）

### 3.1 レイヤ概要
```text
UI (Compose Screen)
  ↓  (イベント/状態)
Presentation (ViewModel)
  ↓  (UseCase呼び出し)
Domain (Entity / UseCase / Repository Interface / Rules)
  ↓  (Interface経由)
Data (Repository Impl / API / DB / DataStore / Mapper)
  ↓
External (Android / Google APIs / Room / Retrofit / WorkManager)
````

### 3.2 依存関係ルール

* `ui` → `presentation` → `domain` → `data` の順で依存してよい
* `domain` は **Android依存禁止**（純Kotlin）
* `data` は `domain` のインターフェースを実装する（逆向き依存）

---

## 4. モジュール/パッケージ構成（推奨）

> まずは単一モジュールでもOK。分割は“肥大したら”で良い。
> ただしパッケージは最初から分ける（将来分割しやすい）。

```text
app/
  ui/
    navigation/
    screen/
    component/
  presentation/
    viewmodel/
    uistate/
  domain/
    entity/
    usecase/
    repository/
    triage/
    util/
  data/
    repository/
    api/
      dto/
      service/
      mapper/
    db/
      dao/
      entity/
      mapper/
    prefs/
      datastore/
  core/
    result/
    error/
    dispatcher/
    time/
    logging/
```

---

## 5. ドメイン設計（新人が迷わない粒度）

### 5.1 Domain Entity（例）

* `TargetApp(packageName: String)`
* `Review`（表示に必要な情報だけ持つ）
* `Importance(HIGH/MID/LOW)`
* `ReasonTag(CRASH/BILLING/UI/NOISE/...)`

### 5.2 Repository Interface（Domain側）

```kotlin
interface AuthRepository {
  suspend fun signIn(): Result<Unit>
  suspend fun signOut()
  suspend fun getValidAccessTokenOrNull(): String?
}

interface ConfigRepository {
  val configFlow: Flow<AppConfig>
  suspend fun setPackageName(packageName: String)
  suspend fun setRetentionDays(days: Int)
}

interface ReviewRepository {
  val reviewsFlow: Flow<List<Review>>
  suspend fun syncNow(): Result<SyncSummary>
  suspend fun getReview(reviewId: String): Review?
}
```

> Point: UIはRepositoryを直接触らず、UseCase経由で触るのが基本。

### 5.3 UseCase（MVPで必要な最小）

* `LoadStartupRouteUseCase`：起動時に SignIn/Setup/Today を決める
* `SetPackageNameUseCase`
* `SyncReviewsUseCase`（手動更新）
* `GetTop3UseCase`
* `GetReviewDetailUseCase`
* `ScheduleDailySyncUseCase`（WorkManager登録）
* `RunDailySyncUseCase`（Worker内部で使う）

---

## 6. Data層設計（外部依存の隔離）

### 6.1 APIクライアント（Retrofit）

* `PublisherService`（reviews.list, optional reviews.get）
* `AuthInterceptor`（Bearer付与）
* `ErrorMapper`（HTTP 401/403/429等を `AppError` に変換）

### 6.2 DB（Room）

* `ReviewEntity` / `ReviewDao`
* `AppConfigEntity` / `AppConfigDao`（またはDataStoreへ寄せてもOK）
* `upsert` 前提（`reviewId` PK）

### 6.3 DataStore

* `TokenStore`（access token + expiry）
* `SettingsStore`（packageName, retentionDays, lastSync）

> MVPは「設定」はDataStoreで十分。Roomに寄せるのは将来でよい。

### 6.4 Mapper

* API DTO → Domain Review
* DB Entity ↔ Domain Review
  （Mapperは `data/.../mapper/` に集中させる）

---

## 7. エラー設計（401/403を“ユーザー行動”に変換する）

### 7.1 AppError（共通エラー型）

```kotlin
sealed class AppError {
  data object AuthExpired : AppError()          // 401
  data object Forbidden : AppError()            // 403
  data object Network : AppError()
  data object RateLimited : AppError()          // 429など
  data class Unknown(val message: String?) : AppError()
}
```

### 7.2 Result型（成功/失敗の明示）

* UseCase/Repositoryの戻り値は例外ではなく `Result<T>` か `Either` で返す
  → UIでの分岐が読みやすくなる

### 7.3 UI表示メッセージの責務

* `presentation` 層に **AppError → UI文言** の変換関数を置く
  （Data層で文言を決めない。将来の文言変更が地獄になる）

---

## 8. 重要度判定（トリアージ）の配置

### 8.1 置き場所

* `domain/triage/` に配置（純ロジック）
* `TriageEngine`（interface） + `RuleBasedTriageEngineV1`（実装）

### 8.2 差し替え設計（将来AI化するため）

```kotlin
interface TriageEngine {
  fun evaluate(text: String, rating: Int): TriageResult
}

data class TriageResult(
  val importance: Importance,
  val tags: Set<ReasonTag>
)
```

> 将来、AI版を作るなら `AiTriageEngine` を追加して差し替えれば良い。

---

## 9. 同期/通知アーキ（WorkManager中心）

### 9.1 手動同期（UI → UseCase）

* Todayの「更新」押下
* `SyncReviewsUseCase` → `ReviewRepository.syncNow()`
* 成功: `lastSync` 更新、DB更新 → FlowでUIが自動更新
* 失敗: `AppError` をUIで表示

### 9.2 日次同期（WorkManager）

* `DailySyncWorker` を `UniquePeriodicWork` で登録
* 実処理は `RunDailySyncUseCase` に寄せる（Workerにロジックを書かない）

#### 実行フロー（Worker）

1. Token取得（期限切れなら更新/失敗なら終了）
2. `reviews.list` 取得（ページング）
3. DTO→Domain→Triage→DB upsert
4. retentionDays超過の削除
5. HIGH件数を集計
6. HIGH > 0 なら通知発行

> 注意: WorkManagerは“だいたいその時間帯”になる。MVPでは許容。
> 正確な9:00固定が必要になったら将来 `AlarmManager` を検討。

---

## 10. 画面遷移の実装方針（Navigation）

* 起動時に `LoadStartupRouteUseCase` で初期ルートを決定
* 画面遷移は `ui/navigation` に集約
* 画面は `UiState` のみを受け取り、イベントをViewModelへ投げる

---

## 11. セキュリティ/プライバシー方針（実装ルール化）

### 11.1 禁止事項

* トークンをログ出力しない（例外メッセージにも出さない）
* レビュー本文を外部送信しない（MVP）
* クリップボードへコピーする場合、ユーザー操作でのみ（自動コピー禁止）

### 11.2 保存方針

* トークン：DataStore（将来Encrypted検討）
* レビュー本文：Roomにキャッシュ（保持日数=30日、削除Jobあり）
* Analyticsイベント：個人情報/レビュー本文を含めない

---

## 12. テスト戦略（MVPで現実的な範囲）

### 12.1 Unit Test（最優先）

* `RuleBasedTriageEngineV1`：キーワードで HIGH/MID/LOW が出るか
* `Top3Selection`：HIGH優先/新しい順のロジック
* `ErrorMapper`：401/403/Networkが正しくAppErrorに変換されるか

### 12.2 Integration Test（余力）

* Room DAOのupsert/削除（retention）確認

### 12.3 UI Test（最低限）

* SignIn → Setup → Today の導線が崩れてないか（モックで可）

---

## 13. ビルド/リリース構成（最初から事故を減らす）

### 13.1 Build Variant（推奨）

* `debug`：ログ多め、デバッグ機能ON
* `internal`：Internal testing配布用（Crashlytics ON）
* `release`：ストア配布

### 13.2 観測（Crashlytics）

* internal以降はCrashlyticsをONにして“落ちたら分かる”を担保
* 重大例外は握り潰さない（握り潰すと再現不能になる）

---

## 14. データフローまとめ（AIが理解しやすい形）

### 14.1 Today表示まで

```text
UI(Today) -> ViewModel -> GetTop3UseCase
  -> ReviewRepository.reviewsFlow (Room)
  -> Top3Selection (Domain)
  -> UiState 更新 -> UI表示
```

### 14.2 同期

```text
UI(更新) -> ViewModel -> SyncReviewsUseCase
  -> ReviewRepository.syncNow()
    -> TokenStore(AuthRepository)
    -> PublisherService.reviews.list()
    -> Mapper + TriageEngine
    -> ReviewDao.upsert()
    -> SettingsStore.update(lastSync)
  -> Result をUIへ
```

### 14.3 日次通知

```text
WorkManager -> DailySyncWorker
  -> RunDailySyncUseCase
    -> sync (同上)
    -> count(HIGH)
    -> NotificationPublisher.notifyIfNeeded()
```

---

## 15. Codexに投げるための指示テンプレ（コピペ用）

> ※このテンプレには commit/push 手順は含めない（必要になったら別途あなたが実施）

```text
目的：アーキ設計書（arch_v1）に従って実装の骨格を作る。

制約：
- Kotlin + Jetpack Compose
- MVVM + UseCase
- Hilt / Coroutines / Flow
- DataStore(TokenStore/SettingsStore), Room(Review cache)
- Retrofit + OkHttp(AuthInterceptor)
- WorkManager(DailySyncWorker) は枠だけ先に作る

やること：
1) package構成を app/ui, app/presentation, app/domain, app/data, app/core で作成
2) Domain: Entity/Repository interface/UseCase/TriageEngine(v1) を作成
3) Data: TokenStore(DataStore), ReviewDao(Room), PublisherService(Retrofit) の雛形
4) Presentation: ViewModel + UiState 雛形（SignIn/Setup/Today）
5) UI: Navigation Composeで SignIn→Setup→Today→Detail→Settings の遷移枠

品質：
- トークンをログに出さない
- 401/403は AppError にマッピングしてUiStateで表示できるようにする
- 例外で落ちない（try/catch + Result返却）
```

---

## 16. 次の作業（このアーキ設計からの実装順）

1. **チケット#1 認証/疎通**：AuthRepository + TokenStore + 401/403表示
2. **チケット#2 レビュー同期**：reviews.list → Room保存 → Today表示
3. **チケット#3 トリアージ**：TriageEngine v1 + Top3Selection
4. **チケット#4 WorkManager通知**：日次同期 → HIGH件数で通知

---


