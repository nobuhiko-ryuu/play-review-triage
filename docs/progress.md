# 進捗状況：Play Review Triage
最終更新: 2026-02-25（セッション6）
プロジェクト: `C:\Users\my\claude_code\Projects\Play Review Triage`
GitHub: https://github.com/nobuhiko-ryuu/play-review-triage（Public）

---

## 運用ルール（必読）
- **トークン使用量が90%を超えたら、きりの良いところでストップし、このファイルを更新してから終了する**
- 次セッション開始時は、まずこのファイルを読んで状況を把握してから再開する

---

## 採用Teams構成（案A：レイヤー分割型）

| エージェント | 担当範囲 |
|---|---|
| **Team Lead** | プロジェクトセットアップ・Gradle・Hilt DI modules・Navigation骨格・統合・レビュー |
| **Domain Agent** | Domain層全体（Entity / Repository Interface / UseCase / TriageEngine）+ Unit テスト |
| **Data Agent** | Data層全体（Retrofit / Room / DataStore / Mapper / ErrorMapper） |
| **UI Agent** | UI + Presentation層全体（Compose 5画面 / ViewModel / WorkManager） |

### 実行フェーズ
```
Phase 1（Team Lead単独）      ✅ 完了
Phase 2（3エージェント並列）   ✅ 完了
Phase 3（統合・品質確認）      🔄 進行中
```

---

## 実施済み

### Phase 1（Team Lead）— 完了
- Gradle設定 / build variant（debug/internal/release）/ 全依存ライブラリ
- Hilt初期化（App.kt / di/ module群）
- Navigation骨格（NavRoutes / AppNavHost）
- Domain interfaces・AppError・Result型

### Phase 2（並列エージェント）— 完了
- **Domain Agent**：TriageEngine / TriageResult / RuleBasedTriageEngineV1 / UseCase 7本 / Unit test 2本
- **Data Agent**：Retrofit（PublisherService）/ Room / DataStore（TokenStore・SettingsStore）/ Mapper / ErrorMapper / Repository 実装3本
- **UI Agent**：Compose 5画面 / ViewModel 6本 / UiState 5本 / Component 3本 / DailySyncWorker
- **手動補完**：DIモジュール完成・AppNavHost 実スクリーン差し替え・MainViewModel

### Phase 3（統合・品質確認）— 進行中

#### セッション 6（2026-02-25）
- [x] **EMPTY シナリオで再読み込みが無限ローディングになるバグを修正**（TodayViewModel）
  - 連打ガード：`isSyncing=true` の間は `sync()` を即リターン
  - 成功時に `isSyncing=false` を確実に反映：`result.fold(onSuccess=...)` で直接 state を書き換え
  - 成功時は `configFlow.first()` で最新の lastSyncLabel を取得し、`getTop3UseCase.invoke().first()` で top3 を取得して UI を確定
- [x] **DailySyncWorker 実装完成（日次同期＋条件付き通知）**
  - `NotificationHelper`：Channel 作成（`daily_review`）・HIGH件数通知・タップで Today 遷移
  - `App.onCreate()`：`notificationHelper.createChannel()` を呼んで Channel を初期化
  - `RunDailySyncUseCase`：同期成功後に `reviewsFlow.first()` から HIGH 件数を正確に集計し `SyncSummary.highCount` に返す
  - `DailySyncWorker`：`NotificationHelper` を注入、`highCount > 0` のときのみ通知
  - `ScheduleDailySyncUseCase`：`ExistingPeriodicWorkPolicy.KEEP` → `UPDATE` に変更
  - `SetupViewModel`：Setup 成功後に `scheduleDailySyncUseCase.invoke()` を呼びスケジュール登録
  - `SettingsScreen`：Android 13+ 向け「通知を許可する」ボタンを追加（未許可時のみ表示）
- [x] **InspectionPanel が internal ビルドで表示されないバグを修正**
  - 原因：internal 側 `InspectionPanel(viewModel: ... = hiltViewModel())` のデフォルト引数があっても、JVM クラス解決で main の no-op が優先されていた
  - 対応：internal 側の引数を削除し、関数内で `val viewModel: InspectionPanelViewModel = hiltViewModel()` として取得する形に変更

#### セッション 5（2026-02-25）
- [x] **internal 検査モード強化**
  - `ReviewRepository.checkAccess(packageName)` をドメイン層に追加（Real: listReviews maxResults=1 / Fake: シナリオ応答）
  - `FakeScenario` enum（SUCCESS / EMPTY / AUTH_401 / FORBIDDEN_403 / NETWORK_ERROR / RATE_LIMIT）を `src/internal/` に追加
  - `InternalTestStore`（DataStore）でシナリオを永続化
  - `FakeReviewRepository` をシナリオ駆動に改修（checkAccess / syncNow がシナリオに応じて成功/失敗/空を返す）
  - `SetPackageNameUseCase`：形式チェック → checkAccess → 成功なら保存（失敗なら保存しない）
  - `SetupViewModel`：401/403/Network/RateLimit/Unknown を個別メッセージで表示
  - `InspectionPanel`：ソースセット分割（main=no-op / internal=シナリオ切替UI）、Settings画面に組み込み
  - `GetTop3UseCaseTest`：checkAccess を mock に追加（コンパイルエラー修正）
  - 全3バリアント（assembleInternal / Debug / Release）+ testDebugUnitTest 全成功
- [x] **internal ビルドを端末に共存インストール可能化**（applicationIdSuffix / versionNameSuffix）

#### セッション 4（2026-02-25）
- [x] **P0: NetworkModule ログ漏洩対策**
  - `HttpLoggingInterceptor` を `BuildConfig.DEBUG` のときのみ有効化
  - `redactHeader("Authorization")` 設定 → Bearer トークンがログに出ない
  - レベルを `BODY` → `HEADERS` に変更 → レビュー本文がログに出ない
- [x] **P1: Fake 実装を src/internal ソースセットへ隔離**
  - `FakeAuthRepository` / `FakeReviewRepository` を `src/main/` から `src/internal/` へ移動
  - Hilt Module をソースセット別に分割（RepositoryBindingsModule は main / RepositoryImplModule は debug・release・internal それぞれ独立）
  - 旧 `RepositoryModule.kt`（BuildConfig 分岐）を削除
  - `BuildConfig.USE_FAKE_DATA` を全 buildType から削除（ソースセット分割で不要に）
- [x] **全 buildType ビルド確認**：assembleInternal / assembleDebug / assembleRelease / testDebugUnitTest 全て成功

#### セッション 3（2026-02-23〜24）
- [x] **Google Sign-In 実装**（`play-services-auth 21.3.0`）
  - `AuthRepository.completeSignIn(accountName)` インターフェース化
  - `AuthRepositoryImpl`：`GoogleAuthUtil.getToken()` でアクセストークン取得・DataStore保存
  - `SignInScreen`：`ActivityResultLauncher` でアカウント選択画面起動
  - `UserRecoverableAuthException` 対応：リカバリIntent を自動起動して許可後にリトライ
  - `signInClient.signOut()` で前回キャッシュをクリアしてアカウント選択を強制表示
- [x] **Google Cloud Console / OAuth 設定**（ユーザー実施）
  - OAuth クライアントID（Android）作成・SHA-1登録
  - `google-services.json` を `app/` 直下に配置
  - `google-services` プラグイン（4.4.2）追加
  - OAuth同意画面：外部・テストユーザーに自アカウント追加
- [x] **GitHub リポジトリ作成・構成管理開始**
  - `git init` → 初回コミット（112ファイル）→ GitHub push
  - `.gitignore` 整備（`local.properties` / `build/` / `google-services.json` 除外）
- [x] **バグ修正・品質改善**
  - `AuthRepositoryImpl.isSignedIn()`：suspend関数の不正呼び出しを `runBlocking` で修正
  - `SetPackageNameUseCase`：パッケージ名バリデーション強化（ドット区切り2セグメント以上必須）
  - `ErrorMapper`：404 → 「アプリが見つかりません」メッセージ追加
  - `TodayViewModel`：`AppError.Unknown.message` を画面に表示するよう修正
  - `DatabaseModule`：`fallbackToDestructiveMigration(dropAllTables = true)` deprecation 修正
  - `TokenStore.saveToken()` の `expiryEpochSec` 引数欠落を修正
- [x] **Fake Data Mode 実装**（internal ビルド用）
  - `BuildConfig.USE_FAKE_DATA`（internal=true / debug・release=false）
  - `FakeAuthRepository`：常にサインイン済み・トークン固定
  - `FakeReviewRepository`：HIGH×2・MID×2・LOW×1 の5件をシード、sync()で1件追加
  - `RepositoryModule`：`@Provides` でフラグに応じて Real/Fake を切り替え
- [x] **認証の実機動作確認** ✅
  - アカウント選択画面表示 ✅
  - テストユーザー追加後に OAuth 通過 ✅
- [x] **internal ビルドでの UI 全画面確認**（Fake Data Mode）✅
  - Setup：バリデーションエラー・正常保存・Today 遷移 ✅
  - Today：Top3 表示・更新ボタン・空状態・エラー状態 ✅
  - Detail：レビュー詳細・タグ・デバイス情報・Play Console ボタン ✅
  - Settings：パッケージ名表示・ログアウトダイアログ ✅
- [x] **バグ修正**：`FakeReviewRepository` の `androidOsVersion` 型不一致（String→Int）

---

## 残タスク

### Phase 3 残作業
- [x] **DailySyncWorker の実装**（WorkManager スケジューリング + 通知）
- [x] **CI設定**（`.github/workflows/ci.yml`）：Unit test の自動実行（push/PR で testDebugUnitTest 実行）
- [ ] **実 API E2E テスト**（自アプリを Play Console に登録後に実施）
  - 401 / 403 / 404 / ネットワークエラーの各エラー表示確認

### 将来対応（MVP後）
- [ ] `GoogleSignIn` / `GoogleSignInOptions` deprecation 対応（Credential Manager への移行）
- [ ] Encrypted DataStore 移行（現在は平文 DataStore）
- [ ] Firebase / Crashlytics 導入（`google-services.json` の本番設定が必要）
- [ ] トークン自動更新（現在は `GoogleAuthUtil.getToken()` が都度更新。期限切れ時の UX 改善）

---

## 未解決の問題・確認事項

1. **実 API テストは自アプリ公開後**
   → Play Console にアプリが登録されるまで Fake Data Mode で品質確認を継続

2. **Firebase / Crashlytics**
   → `google-services.json` の本番接続はユーザー作業。未着手。

3. **`GoogleSignIn` 系 API の deprecation 警告**
   → ビルドは通る。`play-services-auth 21.x` で deprecated。MVP 後に Credential Manager へ移行予定。

---

## 参照ドキュメント

| ドキュメント | 参照タイミング |
|---|---|
| `03_external_design.md` | API I/F・画面仕様・エラー文言・DBスキーマ |
| `04_architecture.md` | レイヤ責務・Repository interface・AppError・TriageEngine設計 |
| `folder_structure.md` | ファイル配置・命名ルール |
| `DEVELOPMENT_RULES.md` | Git運用・PRルール・AI依頼テンプレ |
| `DEFINITION_OF_READY_DONE.md` | PR作成前のチェックリスト |

---

## メモ・決定事項

- **MVPはサーバレス（端末完結）**：レビュー本文・トークンの外部送信は禁止
- **Domain層はAndroid依存禁止**（純Kotlin）。ただし `AuthRepository.consumeRecoveryIntent()` のみ MVP 妥協として `android.content.Intent` を返す
- **トークン保存**：MVP段階はDataStoreで可（β前にEncrypted DataStore検討）
- **WorkManagerの精度**：「だいたい9:00頃」で許容
- **Top3選定ロジック**：HIGH（新しい順）→ MID（新しい順）補完、LOWは原則除外
- **AuthInterceptor**：runBlockingでDataStore読み取り（MVP許容）
- **Fake Data Mode**：`internal` ビルドで `BuildConfig.USE_FAKE_DATA=true`。`FakeAuthRepository` + `FakeReviewRepository` で API・認証をスタブ化
- **パッケージ名バリデーション**：`^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z][a-zA-Z0-9_]*)+$`（ドット区切り2セグメント以上必須）
- **Hilt deprecation note**：hiltJavaCompileDebugで出るConfiguration.Provider deprecation警告は自動生成コードによるもの。修正不要。
