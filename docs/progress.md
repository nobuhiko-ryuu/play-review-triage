# 進捗状況：Play Review Triage
最終更新: 2026-02-25
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
- [ ] **DailySyncWorker の動作確認**（WorkManager スケジューリング）
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
