# Gemma 3n Chatbot

**Google Gemma 3n-E4B**を使用したAndroid用オンデバイスAIチャットアプリ

MediaPipe GenAI APIを使用してGemma 3nモデルをAndroid端末上で直接実行します。

## 主な特徴

- **モデル**: `google/gemma-3n-E4B-it-litert-lm` (Hugging Face)
- **形式**: LiteRT LM (.litertlm)
- **サイズ**: INT4量子化 (4.4GB)
- **API**: MediaPipe GenAI v0.10.27
- **フレームワーク**: Kotlin + Jetpack Compose
- **Min SDK**: 30 (Android 11)

## 機能

- Hugging Faceトークン認証によるモデルダウンロード
- 再開可能なダウンロード (中断してもやり直し不要)
- セキュアなトークン保存 (EncryptedSharedPreferences)
- リアルタイムストリーミング生成
- パフォーマンス測定
  - 初回トークン生成時間 (ms)
  - 生成速度 (tokens/sec)
  - ハードウェアアクセラレーション検出 (GPU/NNAPI/XNNPACK)
- Material3デザインのモダンUI

## 技術スタック

| 技術 | バージョン |
|------|-----------|
| Kotlin | 1.9.22 |
| Compose BOM | 2024.02.00 |
| MediaPipe GenAI | 0.10.27 |
| OkHttp | 4.12.0 |
| Security Crypto | 1.1.0-alpha06 |
| Gradle | 8.2+ |
| Target SDK | 35 |
| Compile SDK | 36 |

## セットアップ手順

### 1. 必要環境

- **Android Studio**: Hedgehog (2023.1.1) 以上
- **JDK**: 17以上
- **テスト端末**: Android 11以上 (Pixel 8+, Samsung S23+推奨)
- **ストレージ**: 最低5GB以上の空き容量

### 2. リポジトリのクローン

```bash
git clone <your-repo-url>
cd gemma_chat
```

### 3. ビルド

**重要**: JDK 17 環境が必須です

```bash
# JDK 17 環境設定
export JAVA_HOME="C:\Program Files\Java\jdk-17"  # Windows

# ビルド実行
./gradlew clean assembleDebug
```

または Android Studio の `Run` ボタンから実行

**ビルド結果**:
- 出力: `app/build/outputs/apk/debug/app-debug.apk` (84MB)
- ビルド時間: ~13秒
- コンパイル警告: ゼロ（Material3準拠、最新API対応）

### 4. インストール

```bash
./gradlew installDebug
```

または生成されたAPKを手動インストール:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 使用方法

### 初回起動時の設定

1. **Hugging Faceトークンの取得**
   - https://huggingface.co/settings/tokens にアクセス
   - 「Create new token」をクリック
   - Token名を入力 (例: gemma-chat-app)
   - Repositories permissions で `gemma-3n-E4B-it-litert-lm` を選択
   - Read access to contents of selected repos を選択
   - 「Create token」をクリック
   - トークンをコピー (hf_で始まる文字列)

2. **モデルのライセンス同意**
   - https://huggingface.co/google/gemma-3n-E4B-it-litert-lm にアクセス
   - ログイン後、「Agree and access repository」をクリック
   - Googleのライセンス条項に同意

3. **アプリでトークン入力**
   - アプリを起動
   - トークン入力画面が表示される
   - コピーしたトークン (hf_...) を貼り付け
   - 「Save Token & Download Model」をタップ

4. **モデルダウンロード**
   - 4.4GBのダウンロードが開始 (Wi-Fi推奨)
   - 進捗バーで進行状況を確認
   - ~~中断しても次回起動時に続きから再開~~ → 実行不可能になるバグを修正予定

5. **モデル初期化**
   - ダウンロード完了後、自動的にモデルを読み込み (30-60秒)
   - 初期化完了後、チャット画面が表示される

### テキスト生成

1. テキスト入力欄にプロンプトを入力
   - 例: 「Kotlinの特徴を教えて」「量子コンピュータとは何か説明して」
2. 「Generate」ボタンをタップ
3. リアルタイムで生成されるテキストを確認
4. 生成を途中で止めたい場合は「Stop」をタップ

### パフォーマンス情報

画面下部にパフォーマンス指標が表示されます:
```
Delegate: Auto (GPU/NNAPI/XNNPACK) | First token: 1200ms | Speed: 8.5 tok/s
```

### デバッグログ

詳細なログはLogcatで確認できます:
```bash
adb logcat -s GemmaBench:V
```

## プロジェクト構成

```
gemma_chat/
├─ app/
│  ├─ build.gradle.kts          # 依存関係設定
│  ├─ proguard-rules.pro        # ProGuard設定
│  └─ src/main/
│     ├─ AndroidManifest.xml   # パーミッション設定
│     └─ java/com/example/gemmabench/
│        ├─ MainActivity.kt                 # エントリーポイント
│        ├─ ui/
│        │  ├─ GemmaScreen.kt             # メインUI
│        │  ├─ GemmaViewModel.kt          # 状態管理
│        │  └─ theme/                     # Material3テーマ
│        ├─ inference/
│        │  ├─ GemmaInference.kt          # 推論エンジン
│        │  └─ GenerationConfig.kt        # 生成パラメータ
│        └─ utils/
│           ├─ Constants.kt               # 定数定義
│           ├─ TokenManager.kt            # トークン管理
│           └─ ModelDownloader.kt         # モデルダウンロード
├─ build.gradle.kts              # プロジェクト設定
└─ README.md                     # このファイル
```

## パラメータ調整

`Constants.kt` で生成パラメータを調整できます:

```kotlin
// 生成パラメータ
const val MAX_TOKENS = 1024        // 最大出力トークン数
const val TOP_K = 40               // Top-k サンプリング
const val TEMPERATURE = 0.8f       // 温度 (高いほど創造的)
const val RANDOM_SEED = 101        // 乱数シード
```

### 所要時間

- モデルダウンロード: 10-20分 (4.4GB、Wi-Fi環境)
- 初期化時間: 30-60秒 (端末による)
- 初回トークン生成: 5-10秒 (プロンプト長による)

## トラブルシューティング

### ダウンロードエラー

**症状**: `Download failed: HTTP 401`

**解決方法**:
1. トークンが正しいか確認 (hf_で始まる)
2. ライセンス同意済みか確認
3. トークンの権限がReadになっているか確認
4. トークンを削除して再入力

### 初期化エラー

**症状**: `Model initialization failed: Insufficient memory`

**解決方法**:
1. 他のアプリを終了してメモリを確保 (5GB以上推奨)
2. 端末を再起動
3. ストレージ容量を確認

### OOM (Out of Memory)

**症状**: アプリがクラッシュする

**解決方法**:
1. 他のアプリを終了
2. 端末を再起動
3. より高性能な端末を使用

### ビルドエラー

**症状**: `Unresolved reference: LlmInference`

**解決方法**:
1. Gradle Sync実行: `File > Sync Project with Gradle Files`
2. Clean Build: `./gradlew clean`
3. 依存関係確認: `implementation("com.google.mediapipe:tasks-genai:0.10.27")`

### JDK バージョンエラー

**症状**: `The supplied javaHome seems to be invalid. I cannot find the java executable.`

**解決方法**:
```bash
# JDK 17 をインストール: C:\Program Files\Java\jdk-17
# または環境変数設定
export JAVA_HOME="C:\Program Files\Java\jdk-17"

# Gradle デーモンリセット
./gradlew --stop

# ビルド再実行
./gradlew clean assembleDebug
```

### Material3 アイコンエラー

**症状**: `Unresolved reference 'ExpandMore'` / `'ExpandLess'`

**解決方法** (既に修正済み):
- ExpandMore/ExpandLess → KeyboardArrowDown/KeyboardArrowUp に変更
- これらのアイコンは `material.icons.filled` パッケージに存在
- コミット: 399461c で修正完了

## 技術詳細

### MediaPipe GenAI API

LiteRT LM形式の大規模言語モデルを効率的に実行するAPI:

- **トークナイザー**: SentencePiece統合
- **KVキャッシュ管理**: Sessionベースで自動管理
- **ハードウェア最適化**: GPU/NNAPI/XNNPACK自動選択
- **ストリーミング**: `generateResponseAsync()`で逐次出力

### アーキテクチャ

- **MVVM**: ViewModel + StateFlowで状態管理
- **Coroutines**: 非同期処理 (ダウンロード、推論)
- **Compose**: 宣言的UIフレームワーク
- **Lifecycle**: ViewModelでライフサイクル管理

### セキュリティ

- **トークン暗号化**: EncryptedSharedPreferences使用
- **ハードウェア保護**: Keystoreによる鍵管理
- **HTTPS通信**: すべての通信がHTTPS
- **ログ保護**: トークンや機密情報をログ出力しない

## セキュリティとプライバシー

- Hugging Faceトークンは暗号化して端末内に保存
- モデルは端末内で実行されネットワーク送信なし
- テキスト生成は完全オフライン (ダウンロード後)
- プライバシーが重要な用途に最適

## ライセンス

このプロジェクトはMITライセンスです。

モデル: [Gemma 3n-E4B](https://huggingface.co/google/gemma-3n-E4B-it-litert-lm) - GoogleのGemma Licenseに従う

## 謝辞

- **Google AI Edge**: MediaPipe GenAI API
- **Hugging Face**: モデルホスティング
- **Google Research**: Gemma 3nモデル

## サポート

問題が発生した場合:

1. LogcatでGemmaBenchタグのログを確認
2. GitHub Issuesに報告
3. [MediaPipe Documentation](https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android)を参照

## 最新ビルド情報

**更新日**: 2025-11-07
**バージョン**: v1.2 (versionCode=3, versionName="1.2")
**ビルド状態**: ✅ 成功 - 警告ゼロ

### v1.2 完成内容
- ✅ JDK 17 環境設定完了
- ✅ Material3 アイコン インポート修正 (ExpandMore/ExpandLess → KeyboardArrowDown/KeyboardArrowUp)
- ✅ 非推奨 API 修正 (Divider → HorizontalDivider)
- ✅ Gradle 8.13 で完全互換確認
- ✅ Kotlin 2.0.21 対応確認
- ✅ APK 生成完了 (84MB、警告ゼロ)

### 次のリリース予定
- **v1.3**: ハードウェアアクセラレーション詳細表示
- **v2.0**: チャット履歴保存、複数モデル対応

詳細は [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md) を参照してください。

---

**注意事項**: このアプリは実験的なプロジェクトです。本番環境での使用は自己責任でお願いします。
