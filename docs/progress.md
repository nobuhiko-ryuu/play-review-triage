# 進捗状況：Play Review Triage
最終更新: 2026-02-23
プロジェクト: `C:\Users\my\claude_code\Projects\Play Review Triage`

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
Phase 1（Team Lead単独）
  ├─ Gradle設定 / build variant / 依存ライブラリ追加
  ├─ Hilt初期化（App.kt / di/ module群）
  ├─ Navigation骨格（NavRoutes / AppNavHost）
  └─ Domain interfaces・AppError・Result型を確定（他エージェントへの入力）

Phase 2（3エージェント並列）
  ├─ Domain Agent: Entity / UseCase / TriageEngineV1 実装 + 単体テスト
  ├─ Data Agent: Room / Retrofit / DataStore / Mapper 実装
  └─ UI Agent: 5画面Compose UIをモックデータで先行実装

Phase 3（統合）
  └─ Team Leadが各層を結合・DailySyncWorker組み込み・E2E確認
```

---

## 実施済み

### セッション 1（2026-02-22）
- [x] docsフォルダ内の全ドキュメントを読み込み、開発内容を把握
- [x] Teams構成を検討し、案A（レイヤー分割型 4エージェント）をユーザーが採用決定
- [x] `progress.md`（本ファイル）を作成
- [x] パッケージ名を `app.playreviewtriage` に決定・変更
- [x] Android Studioでプロジェクト雛形作成（ユーザー実施）
- [x] **Phase 1（Team Lead）完了** — 以下をすべて実装
  - `gradle/libs.versions.toml`：全依存ライブラリ・バージョン追加
  - `build.gradle.kts`（root）：Hilt・KSPプラグイン追加
  - `app/build.gradle.kts`：全プラグイン・依存・build variant（debug/internal/release）追加
  - `AndroidManifest.xml`：INTERNET / POST_NOTIFICATIONS権限 / WorkManager初期化無効化
  - `App.kt`：@HiltAndroidApp + HiltWorkerFactory設定
  - `core/result/AppError.kt` / `AppException.kt`
  - `core/time/Clock.kt`：Clock interface + SystemClock実装
  - `domain/entity/`：Importance / ReasonTag / Review / AppConfig / SyncSummary
  - `domain/repository/`：AuthRepository / ConfigRepository / ReviewRepository（interface）
  - `ui/navigation/NavRoutes.kt` / `AppNavHost.kt`（骨格）
  - `di/AppModule.kt` / `NetworkModule.kt` / `DatabaseModule.kt` / `WorkerModule.kt`（骨格）
  - `MainActivity.kt`

### セッション 2（2026-02-22〜23）
- [x] **Phase 2 Domain Agent 完了**
  - `domain/triage/`：TriageEngine / TriageResult / RuleBasedTriageEngineV1
  - `domain/usecase/`：LoadStartupRouteUseCase（StartupRoute sealed class含む）/ SetPackageNameUseCase / SyncReviewsUseCase / GetTop3UseCase / GetReviewDetailUseCase / ScheduleDailySyncUseCase / RunDailySyncUseCase
  - Unit test 2ファイル
- [x] **Phase 2 Data Agent 完了**（トークン切れ後に手動で補完）
  - `data/api/service/PublisherService.kt`
  - `data/api/dto/ReviewsDto.kt`
  - `data/api/mapper/ReviewDtoMapper.kt`
  - `data/api/interceptor/AuthInterceptor.kt`
  - `data/api/ErrorMapper.kt`
  - `data/db/AppDatabase.kt` / `ReviewDao.kt` / `ReviewEntity.kt` / `ReviewEntityMapper.kt`
  - `data/prefs/datastore/TokenStore.kt` / `SettingsStore.kt`
  - `data/repository/AuthRepositoryImpl.kt` / `ConfigRepositoryImpl.kt` / `ReviewRepositoryImpl.kt`
- [x] **Phase 2 UI Agent 完了**（トークン切れ後に手動で補完）
  - `ui/component/`：LoadingView / ErrorView / EmptyView
  - `presentation/uistate/`：SignInUiState / SetupUiState / TodayUiState / DetailUiState / SettingsUiState
  - `presentation/viewmodel/`：MainViewModel / SignInViewModel / SetupViewModel / TodayViewModel / DetailViewModel / SettingsViewModel
  - `ui/screen/signin/SignInScreen.kt`
  - `ui/screen/setup/SetupScreen.kt`
  - `ui/screen/today/TodayScreen.kt`
  - `ui/screen/detail/ReviewDetailScreen.kt`
  - `ui/screen/settings/SettingsScreen.kt`
  - `worker/DailySyncWorker.kt`
- [x] **DIモジュール補完**
  - `di/DatabaseModule.kt`：Room.databaseBuilder + provideReviewDao 完成
  - `di/NetworkModule.kt`：AuthInterceptor + PublisherService 追加
  - `di/RepositoryModule.kt`：新規作成（全リポジトリ・TriageEngine バインド）
- [x] **AppNavHost.kt 更新**：PlaceholderScreen → 実スクリーンに差し替え + MainViewModel連携

---

## 残タスク

### Phase 3（統合）— **次のステップ**
- [ ] Gradle sync + ビルド確認（コンパイルエラーがないか検証）
- [ ] 導線テスト（SignIn → Setup → Today → Detail → Settings）
- [ ] エラー表示確認（401 / 403 / ネットワークエラー）
- [ ] Google OAuth の実装（`AuthRepositoryImpl.signIn()` が現在スタブ）
  - Credential Manager または `play-services-auth` を使った実装
- [ ] Internal testing ビルド確認
- [ ] CI設定（`.github/workflows/ci.yml`）

---

## 未解決の問題・確認事項

1. **Google OAuth クライアントID**
   → Google Cloud Console での設定が必要（`google-services.json` は別途ユーザーが取得）
   → `AuthRepositoryImpl.signIn()` は現在 `TODO: Google Sign-In` スタブ。Phase 3で実装予定。

2. **Firebase / Crashlytics 設定**
   → `google-services.json` が必要。エージェントはFirebase SDKの追加とコード実装のみ担当
   → 実際のプロジェクト接続はユーザーが別途行う

3. **Android Publisher API スコープ確認**
   → `https://www.googleapis.com/auth/androidpublisher` を使用予定
   → Play ConsoleでのAPIアクセス権設定はユーザー側作業

---

## 参照ドキュメント（実装時に参照すべきもの）

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
- **Domain層はAndroid依存禁止**（純Kotlin）
- **トークン保存**：MVP段階はDataStoreで可（β前にEncrypted DataStore検討）
- **WorkManagerの精度**：「だいたい9:00頃」で許容
- **Top3選定ロジック**：HIGH（新しい順）→ MID（新しい順）補完、LOWは原則除外
- **AuthInterceptor**：runBlockingでDataStore読み取り（MVP許容、必要に応じてキャッシュ化）
- **Hilt deprecation note**：hiltJavaCompileDebugで出るConfiguration.Provider deprecation警告は自動生成コードによるもの。修正不要。
