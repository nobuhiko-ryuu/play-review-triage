了解。番号が「UI-002の中が4.x」みたいにズレていたので、**章番号を完全に一貫**させて再整理しました（UI番号はそのまま、章番号は 1, 1.1… で統一）。

以下が修正版のMarkdownです。

````markdown id="exd_v2"
# 外部設計書：Play Review Triage（仮称）
最終更新: 2026-02-22  
対象: Android（Kotlin + Jetpack Compose）  
目的: **画面・操作・入出力・データ・API・通知・エラー文言**を具体化し、実装にそのまま落とせる状態にする

---

## 1. 画面一覧と遷移

### 1.1 画面一覧（MVP）
| 画面ID | 画面名 | 目的 |
|---|---|---|
| UI-001 | SignIn | Google連携を開始し、API利用可能な状態にする |
| UI-002 | Setup | 監視対象 `packageName` を設定する（1つ） |
| UI-003 | Today | 今日やるべきTop3（重要レビュー）を提示する |
| UI-004 | Review Detail | レビュー本文・重要度・理由を確認する |
| UI-005 | Settings | packageName変更、ログアウト、データ保持方針表示 |

### 1.2 遷移図（状態で分岐）
```text
起動
 ├─ 未ログイン → UI-001 SignIn
 └─ ログイン済
      ├─ packageName 未設定 → UI-002 Setup
      └─ packageName 設定済 → UI-003 Today
                             └→ UI-004 Review Detail
UI-005 Settings は Today から入る
````

---

## 2. 共通UI/UX方針（新人でも迷わないルール）

### 2.1 共通状態

* **Loading**：通信/同期中はボタン無効化 + 進捗表示（スピナー）
* **Offline**：キャッシュを表示しつつ「更新できませんでした（通信）」を出す
* **Empty**：レビューが0件の場合は“異常”ではなく通常状態として扱う

### 2.2 文言ポリシー

* **401/403は数字だけ出さない**（意味+次の行動を書く）
* “できない理由”と“次の一手”を必ずセットで表示

---

## 3. 画面仕様（MVP）

### 3.1 UI-001 SignIn（Google連携）

#### 3.1.1 目的

* Googleアカウント連携（OAuth）を行い、Android Publisher APIを呼べる状態にする

#### 3.1.2 表示項目

* タイトル: `Connect to Google Play`
* 説明: `レビュー取得のためにGoogleアカウント連携が必要です。`
* ボタン: `Googleで連携`
* エリア: エラー表示（赤）

#### 3.1.3 操作

* `Googleで連携` → OAuthフロー開始
* 成功 → UI-002 もしくは UI-003（packageName状態で分岐）
* キャンセル → 何もせず戻る（エラー扱いにしない）

#### 3.1.4 エラー表示（例）

* `連携に失敗しました。通信状況を確認してもう一度お試しください。`
* `このアカウントでは連携できません。別のGoogleアカウントでお試しください。`

---

### 3.2 UI-002 Setup（監視対象 packageName 設定）

#### 3.2.1 入力

* 入力欄：`packageName（例: com.example.app）`
* 保存ボタン：`保存して続ける`

#### 3.2.2 バリデーション（MVP）

* 空欄不可
* 文字種の簡易チェック：`[a-zA-Z0-9_.]` のみ（厳密でなくてよい）

#### 3.2.3 保存成功時

* `保存しました` → UI-003 Todayへ

#### 3.2.4 保存後の疎通チェック（重要）

* 保存直後に **API疎通（軽い呼び出し）**を試す
* 403なら「権限不足」をここで確定し、Todayへ行かずに案内を出す

#### 3.2.5 403案内文（固定テンプレ）

* タイトル: `権限がありません`
* 本文:

  * `このGoogleアカウントは、指定したアプリ（packageName）にアクセスできません。`
  * `Play Consoleで権限が付与されているか確認するか、別のアカウントでログインしてください。`
* ボタン:

  * `packageNameを変更`
  * `アカウントを変更（ログアウト）`

---

### 3.3 UI-003 Today（Top3提示）

#### 3.3.1 表示項目

* ヘッダ: `Today`
* サブ: `最終同期: YYYY/MM/DD HH:mm`
* リスト: Top3カード（最大3件）
* アクション:

  * `更新`（手動同期）
  * `設定`（UI-005へ）

#### 3.3.2 Top3カード（1件あたり）

* ★（rating）
* 重要度バッジ：`HIGH / MID / LOW`
* 理由タグ（複数可・短く）

  * 例：`CRASH` `BILLING` `UI` `NOISE`
* 本文プレビュー（先頭N文字、例: 60文字）
* 日時（相対 or 絶対）
* タップで UI-004 へ

#### 3.3.3 Top3選定ルール（外部仕様として固定）

1. `HIGH` を新しい順に取る
2. 3件に満たなければ `MID` を新しい順に補う
3. `LOW` は原則Top3に入れない（レビューが少なすぎる場合は将来検討）

#### 3.3.4 空状態（レビュー0件）

* 表示:

  * `重要レビューはありません`
  * `更新`ボタン
* 注意: 「失敗」扱いにしない（普通にあり得る）

---

### 3.4 UI-004 Review Detail（レビュー詳細）

#### 3.4.1 表示項目

* rating（★）
* 重要度（HIGH/MID/LOW）
* 理由タグ
* レビュー本文（全文）
* メタ情報（任意・MVPでは表示を絞る）

  * `appVersionName`（あれば）
  * `androidOsVersion`（あれば）
  * `deviceMetadata.manufacturer/productName`（あれば）
* ボタン:

  * `Todayへ戻る`
  * `Play Consoleで対応する`（ブラウザ起動・汎用リンクでOK）

#### 3.4.2 “Play Consoleで対応する”仕様（MVP）

* 正確なレビュー直リンクはアカウント/URL構造で変わりやすいので、MVPでは以下のいずれかで実装

  * Play Consoleトップ or 対象アプリのフィードバック画面を開く（可能な範囲）
  * `reviewId` をコピーできる（クリップボード）

---

### 3.5 UI-005 Settings（設定）

#### 3.5.1 表示項目

* 現在の `packageName`
* `packageName変更`
* `ログアウト`
* データ保持方針

  * `レビューは端末内に最大30日間保存します（変更可能予定）`
* （Android 13+）通知が未許可の場合：`通知を許可する` ボタンを表示し、許可要求を行う

---

## 4. API外部インターフェース設計（Android Publisher API）

### 4.1 基本

* Base: `https://androidpublisher.googleapis.com/`
* 認証: OAuth 2.0 Bearer Token
* スコープ（MVP想定）: `https://www.googleapis.com/auth/androidpublisher`

参考:

* `reviews.list`: [https://developers.google.com/android-publisher/api-ref/rest/v3/reviews/list](https://developers.google.com/android-publisher/api-ref/rest/v3/reviews/list)
* `reviews`: [https://developers.google.com/android-publisher/api-ref/rest/v3/reviews](https://developers.google.com/android-publisher/api-ref/rest/v3/reviews)

### 4.2 使用API（MVP）

#### 4.2.1 API-REV-001: reviews.list（レビュー一覧）

* Method: `GET /androidpublisher/v3/applications/{packageName}/reviews`
* Query（必要に応じて）

  * `translationLanguage`（例: `ja`）
  * `maxResults`
  * `token` / `startIndex`（ページング）
* Response（代表）

  * `reviews[]`
  * `tokenPagination` / `pageInfo`

> 注：レビュー取得範囲が「直近中心」になり得るため、MVPは“最近のレビュー運用”に割り切り、端末キャッシュで補う。

#### 4.2.2 API-REV-002: reviews.get（単一レビュー取得）

* Purpose: Detail表示を確実に最新化したい場合に使用（MVPでは任意）
* Method: `GET /androidpublisher/v3/applications/{packageName}/reviews/{reviewId}`

※ 返信（reviews.reply）はMVP外（将来）

### 4.3 Reviewデータの解釈（重要）

* `reviewId`, `authorName`, `comments[]` を持つ
* `comments[]` は会話形式で `userComment` と `developerComment` のいずれか
* MVPでは **最新の userComment を主データ**として扱う

---

## 5. ローカルデータ設計（Room想定）

### 5.1 テーブル一覧（MVP）

#### 5.1.1 DB-001: app_config

| カラム              | 型      | 説明         |
| ---------------- | ------ | ---------- |
| id               | Int    | 固定1行       |
| packageName      | String | 対象アプリ      |
| lastSyncEpochSec | Long   | 最終同期       |
| retentionDays    | Int    | 保持日数（初期30） |

#### 5.1.2 DB-002: review

| カラム                  | 型           | 説明                               |
| -------------------- | ----------- | -------------------------------- |
| reviewId             | String (PK) | ReviewのID                        |
| authorName           | String?     | 表示名（任意）                          |
| starRating           | Int         | 1-5（userComment由来）               |
| text                 | String      | userComment.text                 |
| lastModifiedEpochSec | Long        | userComment.lastModified.seconds |
| appVersionName       | String?     | 任意                               |
| androidOsVersion     | Int?        | 任意                               |
| deviceManufacturer   | String?     | deviceMetadata.manufacturer      |
| deviceModel          | String?     | deviceMetadata.productName       |
| importance           | String      | HIGH/MID/LOW                     |
| reasonTags           | String      | CSV等（例: "CRASH,BILLING"）         |
| fetchedAtEpochSec    | Long        | 取得日時                             |

### 5.2 保持ポリシー

* `fetchedAtEpochSec` が **retentionDays** を超えたレビューは削除
* APIが直近中心でも、端末キャッシュで **最大30日分** を保持してUXを安定させる

---

## 6. 重要度判定（トリアージ）外部仕様

### 6.1 入力

* `review.text`（本文）
* `starRating`
* キーワード辞書（クラッシュ/課金/UI/ノイズ 等）

### 6.2 出力

* `importance`: `HIGH | MID | LOW`
* `reasonTags`: `CRASH, BILLING, UI, NOISE, ...`

### 6.3 ルール v1（MVP固定）

* HIGH（今すぐ対応）

  * `クラッシュ / 落ちる / 起動できない / 立ち上がらない / crash / ANR / freeze` 等
  * `課金 / 購入 / 支払い / 返金 / サブスク / subscription` 等
* MID（次の改善候補）

  * `使いにくい / UI / 見づらい / 追加して / 欲しい / 要望` 等
* LOW（無視してよい可能性が高い）

  * 文字数が極端に短い（例: 3文字以下）
  * 罵倒のみ、情報量がない

> 辞書（キーワードリスト）はアプリ内定数として管理（将来リモート更新に備えて差し替え可能な設計）

---

## 7. 同期仕様（ユーザー操作とバックグラウンド）

### 7.1 手動同期（Todayの更新）

* 仕様:

  1. `reviews.list` を呼ぶ
  2. `reviews[]` を `reviewId` で upsert
  3. トリアージを実施し保存
  4. `lastSyncEpochSec` を更新
* UI:

  * 成功 → `最終同期`更新
  * 失敗 → エラー表示（セクション9参照）

### 7.2 自動同期（通知のため）

* 方式: WorkManagerで日次実行（端末制約に強い）
* タイミング: 毎日 09:00（端末ローカル）
* 失敗時: 次回に繰り越し（ユーザーには通知しない）

---

## 8. 通知仕様

### 8.1 通知トリガ

* 日次Worker起動
* 同期 → 重要レビュー数算出（`importance == HIGH` の件数）
* **HIGHが1件以上**なら通知、0件なら通知しない

### 8.2 通知文言テンプレ

* タイトル: `重要レビューがあります`
* 本文（例）:

  * `今日の重要レビュー：{count}件（クラッシュ/課金など）`
* タップ遷移:

  * UI-003 Today

### 8.3 通知権限・通知OFF時の扱い

* 事前条件：通知がOS設定で無効、またはAndroid 13+（API 33+）でPOST_NOTIFICATIONSが未許可の場合は通知を送出しない
* 動作：通知送信は安全にreturnし、Workerは正常完了（Success）扱いとする（例外で落としてはならない）
* UI：Settings画面にてAndroid 13+で通知が未許可の場合、`通知を許可する` ボタンを表示して許可要求できる

---

## 9. エラー表示文言（固定テンプレ集）

MVPではこの文言を共通コンポーネントで使い回す。

### 9.1 401 Unauthorized

* `認証が期限切れです。再ログインしてください。`
* ボタン: `再ログイン`

### 9.2 403 Forbidden

* `このアカウントは対象アプリにアクセスできません。`
* `Play Consoleで権限を確認するか、別アカウントでログインしてください。`
* ボタン: `ログアウト` / `packageName変更`

### 9.3 Network

* `通信できませんでした。電波状況を確認して再試行してください。`
* ボタン: `再試行`

### 9.4 Unknown

* `予期しないエラーが発生しました。しばらくしてから再試行してください。`
* 注意: 内部ログに原因を残すが、トークン等の機微情報は出さない

---

## 10. 実装に渡す「I/Oまとめ」（AIに投げやすい形）

### 10.1 画面別 入出力

* UI-001 SignIn

  * IN: なし
  * OUT: token（保存）、authState
* UI-002 Setup

  * IN: packageName入力
  * OUT: app_config保存（疎通チェック含む）
* UI-003 Today

  * IN: review一覧（DB）
  * OUT: Top3（表示）、同期トリガ
* UI-004 Detail

  * IN: reviewId
  * OUT: review詳細（DB or API）
* UI-005 Settings

  * IN: app_config
  * OUT: packageName変更、logout

### 10.2 API I/F

* `reviews.list`（必須）: [https://developers.google.com/android-publisher/api-ref/rest/v3/reviews/list](https://developers.google.com/android-publisher/api-ref/rest/v3/reviews/list)
* Review/Comment/UserComment構造: [https://developers.google.com/android-publisher/api-ref/rest/v3/reviews](https://developers.google.com/android-publisher/api-ref/rest/v3/reviews)

```

必要なら次に、これをそのまま **アーキ設計書**へ繋げるために、  
- どの層（UI/ViewModel/UseCase/Repo/API/DB）で何をやるか  
- WorkManagerの責務  
- 例外型（sealed class）  
も外部設計の末尾に「実装割り当て表」として付けられます。
::contentReference[oaicite:0]{index=0}
```
