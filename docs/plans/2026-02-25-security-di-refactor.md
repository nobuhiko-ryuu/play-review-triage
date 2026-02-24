# Security & DI Refactor Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** ログ漏洩を塞ぎ（P0）、Fake実装を internal ソースセットに隔離して本番混入を根絶する（P1）。

**Architecture:** P0はNetworkModuleの条件分岐修正のみ。P1はHilt ModuleをbuildTypeソースセット（main/debug/release/internal）に分割し、BuildConfig分岐を廃止する。

**Tech Stack:** Hilt / OkHttp HttpLoggingInterceptor / Android build variants (buildType source sets)

---

## 前提知識

### buildType ソースセットの仕組み
```
app/src/main/          ← 全buildTypeで共通
app/src/debug/         ← debug buildTypeのみ
app/src/release/       ← release buildTypeのみ
app/src/internal/      ← internal buildTypeのみ（initWith(debug)でdebugのfallback）
```
`internal` の `matchingFallbacks = listOf("debug")` は **ライブラリ依存の解決用** であり、
ソースセット (`src/internal/`) は `src/debug/` とは独立している（自動マージされない）。

### 変更対象ファイルの現状
- `NetworkModule.kt:26-28` — HttpLoggingInterceptor が常時 BODY で有効（漏洩リスク）
- `RepositoryModule.kt` — BuildConfig.USE_FAKE_DATA で Real/Fake を分岐（main に Fake クラスが混入）
- `data/fake/Fake*.kt` — `src/main/` に存在（release にも含まれてしまう）

---

## Task 1: P0 — NetworkModule ログ漏洩修正

**Files:**
- Modify: `app/src/main/java/app/playreviewtriage/di/NetworkModule.kt`

**Step 1: NetworkModule を編集**

`provideOkHttpClient` を以下に置き換える：

```kotlin
@Provides
@Singleton
fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
    val builder = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)

    if (BuildConfig.DEBUG) {
        builder.addInterceptor(
            HttpLoggingInterceptor().apply {
                redactHeader("Authorization")
                level = HttpLoggingInterceptor.Level.HEADERS
            }
        )
    }

    return builder.build()
}
```

import に `app.playreviewtriage.BuildConfig` を追加。

**Step 2: ビルド確認**

```
./gradlew :app:assembleDebug :app:assembleRelease :app:assembleInternal
```

3つ全てエラーなし。

**Step 3: コミット**

```bash
git add app/src/main/java/app/playreviewtriage/di/NetworkModule.kt
git commit -m "fix(security): HttpLoggingInterceptor を DEBUG のみ有効化・Authorization を redact・BODY→HEADERS"
```

---

## Task 2: P1-a — internal ソースセットのディレクトリ作成

**Files:**
- Create dirs:
  - `app/src/internal/java/app/playreviewtriage/data/fake/`
  - `app/src/internal/java/app/playreviewtriage/di/`
  - `app/src/debug/java/app/playreviewtriage/di/`
  - `app/src/release/java/app/playreviewtriage/di/`

**Step 1: ディレクトリ作成**

```bash
mkdir -p app/src/internal/java/app/playreviewtriage/data/fake
mkdir -p app/src/internal/java/app/playreviewtriage/di
mkdir -p app/src/debug/java/app/playreviewtriage/di
mkdir -p app/src/release/java/app/playreviewtriage/di
```

（ファイルは次タスクで作成するのでコミット不要）

---

## Task 3: P1-b — Fake クラスを internal ソースセットへ移動

**Files:**
- Create: `app/src/internal/java/app/playreviewtriage/data/fake/FakeAuthRepository.kt`
- Create: `app/src/internal/java/app/playreviewtriage/data/fake/FakeReviewRepository.kt`
- Delete: `app/src/main/java/app/playreviewtriage/data/fake/FakeAuthRepository.kt`
- Delete: `app/src/main/java/app/playreviewtriage/data/fake/FakeReviewRepository.kt`

**Step 1: FakeAuthRepository を internal へコピー**

`app/src/internal/java/app/playreviewtriage/data/fake/FakeAuthRepository.kt` を作成。
内容は `src/main/` の既存ファイルと同一（package宣言含め変更なし）。

**Step 2: FakeReviewRepository を internal へコピー**

`app/src/internal/java/app/playreviewtriage/data/fake/FakeReviewRepository.kt` を作成。
内容は `src/main/` の既存ファイルと同一（package宣言含め変更なし）。

**Step 3: main の Fake ファイルを削除**

```bash
git rm app/src/main/java/app/playreviewtriage/data/fake/FakeAuthRepository.kt
git rm app/src/main/java/app/playreviewtriage/data/fake/FakeReviewRepository.kt
```

**Step 4: コミット（まだビルドは壊れていてOK — 次タスクで修正）**

```bash
git add app/src/internal/java/app/playreviewtriage/data/fake/
git commit -m "refactor: Fake実装を src/internal へ移動（src/main から削除）"
```

---

## Task 4: P1-c — DI Module をソースセット別に分割

### 4-1) main に RepositoryBindingsModule を作成

**File:** `app/src/main/java/app/playreviewtriage/di/RepositoryBindingsModule.kt`

```kotlin
package app.playreviewtriage.di

import app.playreviewtriage.data.repository.ConfigRepositoryImpl
import app.playreviewtriage.domain.repository.ConfigRepository
import app.playreviewtriage.domain.triage.RuleBasedTriageEngineV1
import app.playreviewtriage.domain.triage.TriageEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindingsModule {

    @Binds @Singleton
    abstract fun bindConfigRepository(impl: ConfigRepositoryImpl): ConfigRepository

    @Binds @Singleton
    abstract fun bindTriageEngine(impl: RuleBasedTriageEngineV1): TriageEngine
}
```

### 4-2) internal に RepositoryImplModule を作成（Fake）

**File:** `app/src/internal/java/app/playreviewtriage/di/RepositoryImplModule.kt`

```kotlin
package app.playreviewtriage.di

import app.playreviewtriage.data.fake.FakeAuthRepository
import app.playreviewtriage.data.fake.FakeReviewRepository
import app.playreviewtriage.domain.repository.AuthRepository
import app.playreviewtriage.domain.repository.ReviewRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryImplModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: FakeAuthRepository): AuthRepository

    @Binds @Singleton
    abstract fun bindReviewRepository(impl: FakeReviewRepository): ReviewRepository
}
```

### 4-3) debug に RepositoryImplModule を作成（Real）

**File:** `app/src/debug/java/app/playreviewtriage/di/RepositoryImplModule.kt`

```kotlin
package app.playreviewtriage.di

import app.playreviewtriage.data.repository.AuthRepositoryImpl
import app.playreviewtriage.data.repository.ReviewRepositoryImpl
import app.playreviewtriage.domain.repository.AuthRepository
import app.playreviewtriage.domain.repository.ReviewRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryImplModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindReviewRepository(impl: ReviewRepositoryImpl): ReviewRepository
}
```

### 4-4) release に RepositoryImplModule を作成（Real・debug と同内容）

**File:** `app/src/release/java/app/playreviewtriage/di/RepositoryImplModule.kt`

debug と全く同じ内容をコピー。

**Step 1: 上記4ファイルを全て作成してコミット**

```bash
git add app/src/main/java/app/playreviewtriage/di/RepositoryBindingsModule.kt
git add app/src/internal/java/app/playreviewtriage/di/RepositoryImplModule.kt
git add app/src/debug/java/app/playreviewtriage/di/RepositoryImplModule.kt
git add app/src/release/java/app/playreviewtriage/di/RepositoryImplModule.kt
git commit -m "feat(di): Hilt Module をソースセット別に分割（main/debug/release/internal）"
```

---

## Task 5: P1-d — 旧 RepositoryModule を削除

**Files:**
- Delete: `app/src/main/java/app/playreviewtriage/di/RepositoryModule.kt`

**Step 1: RepositoryModule.kt を削除**

```bash
git rm app/src/main/java/app/playreviewtriage/di/RepositoryModule.kt
git commit -m "refactor: 旧 RepositoryModule（BuildConfig分岐）を削除"
```

---

## Task 6: P1-e — USE_FAKE_DATA の削除

**Files:**
- Modify: `app/build.gradle.kts`

**Step 1: buildConfigField の USE_FAKE_DATA を全buildTypeから削除**

`app/build.gradle.kts` の以下の行を削除：
```kotlin
buildConfigField("Boolean", "USE_FAKE_DATA", "false")  // debug の行
buildConfigField("Boolean", "USE_FAKE_DATA", "false")  // release の行
buildConfigField("Boolean", "USE_FAKE_DATA", "true")   // internal の行
```

**Step 2: buildConfig = true の要否を確認**

他に `BuildConfig.*` を参照しているかチェック（NetworkModule の `BuildConfig.DEBUG` は残すべき — これは自動生成されるので `buildConfig = true` は維持する）。

```bash
grep -r "BuildConfig\." app/src/main/
```

`BuildConfig.DEBUG` だけが残っていることを確認。

**Step 3: コミット**

```bash
git add app/build.gradle.kts
git commit -m "refactor: BuildConfig.USE_FAKE_DATA を削除（ソースセット分割で不要に）"
```

---

## Task 7: 全buildType ビルド確認

**Step 1: 3バリアント全てをビルド**

```bash
./gradlew clean :app:assembleInternal :app:assembleDebug :app:assembleRelease
```

全て `BUILD SUCCESSFUL` であること。

**Step 2: Unit Test 実行**

```bash
./gradlew testDebugUnitTest
```

**Step 3: 最終コミット & push**

```bash
git push
```

---

## 完了チェックリスト

- [ ] release ビルドで `HttpLoggingInterceptor` が含まれない
- [ ] debug/internal の Logcat に `Authorization:` が出ない
- [ ] release ビルドに `FakeAuthRepository` / `FakeReviewRepository` が含まれない
- [ ] internal ビルドで Fake データが正常に表示される
- [ ] `BuildConfig.USE_FAKE_DATA` の参照がコードベースにゼロ
- [ ] 全3バリアント BUILD SUCCESSFUL
