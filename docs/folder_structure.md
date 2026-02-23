````markdown id="folder_structure_v1"
# プロジェクト フォルダ構成（提案：MVP向け・単一モジュール）

> 方針：最初は **`:app` 単一モジュール**で最速に走り、肥大したら `:core / :domain / :data / :feature-*` に分割できるように “パッケージ境界” を先に切っておく。

---

## 1. リポジトリ直下（root）
```text
play-review-triage/
├─ README.md
├─ LICENSE
├─ .gitignore
├─ gradle.properties
├─ settings.gradle.kts
├─ build.gradle.kts
├─ gradle/
│  └─ wrapper/
│     ├─ gradle-wrapper.jar
│     └─ gradle-wrapper.properties
├─ app/                          # Androidアプリ本体（MVPはここだけでOK）
│  ├─ build.gradle.kts
│  ├─ proguard-rules.pro
│  └─ src/
│     ├─ main/
│     │  ├─ AndroidManifest.xml
│     │  ├─ java/ (or kotlin/)   # Kotlinコード
│     │  ├─ res/
│     │  └─ assets/
│     ├─ test/                   # JVM unit tests
│     └─ androidTest/            # Instrumentation tests
├─ docs/                         # 今作ってる設計書をここへ集約
│  ├─ 01_planning.md             # 企画書
│  ├─ 02_requirements.md         # 要件定義書
│  ├─ 03_external_design.md      # 外部設計書
│  └─ 04_architecture.md         # アーキ設計書
├─ .github/
│  └─ workflows/
│     ├─ ci.yml                  # Lint/Test/Build（任意）
│     └─ release.yml             # 署名・配布（任意：将来）
└─ tools/                        # スクリプトや検証用（任意）
   ├─ seed-data/                 # テスト用JSON等（任意）
   └─ scripts/
````

---

## 2. `app/src/main/java/...`（アプリ内パッケージ構成）

> 例：アプリIDを `com.nobuhiko.playreviewtriage` とした場合

```text
app/src/main/java/com/nobuhiko/playreviewtriage/
├─ App.kt                        # Application（Hilt初期化など）
├─ MainActivity.kt               # Composeの入口
│
├─ ui/                           # Compose画面（表示と入力だけ）
│  ├─ navigation/
│  │  ├─ NavRoutes.kt
│  │  └─ AppNavHost.kt
│  ├─ screen/
│  │  ├─ signin/
│  │  │  ├─ SignInScreen.kt
│  │  │  └─ SignInUi.kt          # 画面専用の小さな部品/モデル（任意）
│  │  ├─ setup/
│  │  │  └─ SetupScreen.kt
│  │  ├─ today/
│  │  │  └─ TodayScreen.kt
│  │  ├─ detail/
│  │  │  └─ ReviewDetailScreen.kt
│  │  └─ settings/
│  │     └─ SettingsScreen.kt
│  └─ component/
│     ├─ LoadingView.kt
│     ├─ ErrorView.kt
│     └─ EmptyView.kt
│
├─ presentation/                 # ViewModelとUiState（状態とイベント）
│  ├─ uistate/
│  │  ├─ SignInUiState.kt
│  │  ├─ SetupUiState.kt
│  │  ├─ TodayUiState.kt
│  │  ├─ DetailUiState.kt
│  │  └─ SettingsUiState.kt
│  └─ viewmodel/
│     ├─ SignInViewModel.kt
│     ├─ SetupViewModel.kt
│     ├─ TodayViewModel.kt
│     ├─ DetailViewModel.kt
│     └─ SettingsViewModel.kt
│
├─ domain/                       # 純Kotlin（Android依存なし）
│  ├─ entity/
│  │  ├─ AppConfig.kt
│  │  ├─ Review.kt
│  │  ├─ Importance.kt
│  │  └─ ReasonTag.kt
│  ├─ repository/                # interfaceのみ
│  │  ├─ AuthRepository.kt
│  │  ├─ ConfigRepository.kt
│  │  └─ ReviewRepository.kt
│  ├─ triage/
│  │  ├─ TriageEngine.kt
│  │  ├─ RuleBasedTriageEngineV1.kt
│  │  └─ TriageResult.kt
│  └─ usecase/
│     ├─ LoadStartupRouteUseCase.kt
│     ├─ SetPackageNameUseCase.kt
│     ├─ SyncReviewsUseCase.kt
│     ├─ GetTop3UseCase.kt
│     ├─ GetReviewDetailUseCase.kt
│     ├─ ScheduleDailySyncUseCase.kt
│     └─ RunDailySyncUseCase.kt
│
├─ data/                         # 外部依存（API/DB/Store）をdomainへ適合させる
│  ├─ repository/                # domain.repositoryの実装
│  │  ├─ AuthRepositoryImpl.kt
│  │  ├─ ConfigRepositoryImpl.kt
│  │  └─ ReviewRepositoryImpl.kt
│  ├─ api/
│  │  ├─ service/
│  │  │  └─ PublisherService.kt
│  │  ├─ dto/
│  │  │  └─ ReviewsDto.kt        # 必要になったら分割
│  │  ├─ mapper/
│  │  │  └─ ReviewDtoMapper.kt
│  │  └─ interceptor/
│  │     └─ AuthInterceptor.kt
│  ├─ db/
│  │  ├─ AppDatabase.kt
│  │  ├─ dao/
│  │  │  └─ ReviewDao.kt
│  │  ├─ entity/
│  │  │  └─ ReviewEntity.kt
│  │  └─ mapper/
│  │     └─ ReviewEntityMapper.kt
│  └─ prefs/
│     └─ datastore/
│        ├─ TokenStore.kt
│        └─ SettingsStore.kt
│
├─ worker/                       # WorkManager
│  └─ DailySyncWorker.kt
│
├─ di/                           # Hilt module（依存の束ね）
│  ├─ AppModule.kt
│  ├─ NetworkModule.kt
│  ├─ DatabaseModule.kt
│  └─ WorkerModule.kt
│
└─ core/                         # 共通（Result/Error/Timeなど）
   ├─ result/
   │  ├─ AppResult.kt            # Result/Eitherを使うならここ
   │  └─ AppError.kt
   ├─ time/
   │  └─ Clock.kt                # now()を注入（テスト容易）
   └─ logging/
      └─ LogSanitizer.kt         # トークン等のマスク（任意）
```

---

## 3. 命名・配置ルール（守ると迷子にならない）

* `ui/`：Compose表示のみ（API/DB/UseCase呼び出し禁止）
* `presentation/`：UseCase呼び出しと `UiState` 生成まで
* `domain/`：純ロジック（Android依存禁止）
* `data/`：Retrofit/Room/DataStoreなど “外部” を扱い、domain型に変換
* `worker/`：WorkManagerの“入口”。ロジックはUseCaseへ寄せる

---

## 4. 将来のマルチモジュール化（必要になったら）

次の段階で以下へ分割しやすい構造にしてあります：

* `:core`
* `:domain`
* `:data`
* `:feature-signin`, `:feature-today` …（必要なら）

※現時点では不要。まずは最速でMVPを出す。

```

この構成で進めます。次は、この構成に沿って **「実装チケット（Issue）一覧」**を切って、Codexにそのまま投げられる粒度に落とします。
::contentReference[oaicite:0]{index=0}
```
