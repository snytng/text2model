# Text2Modelプラグイン

## 説明
文章からモデルをモデルから文章を作るプラグインです。

# ダウンロード
- [ここ](https://github.com/snytng/text2model/releases/latest)から`text2model-<version>.jar`をダウンロードして下さい。

# インストール
- ダウンロードしたプラグインファイルをastah*アプリケーションにドラッグドロップするか、Program Files\asta-professionals\pluginsに置いて下さい。

# 機能説明
- `Add All +`：入力された文章をモデルに変換します。
- `Replace ==>`: モデルを文章に変換して右のエリアを置き換えます。
- `名詞モードOFF`：文章を形態素解析します。
- `名詞モードON`：文章から名詞を抜き出します。
- `属性追加`: 文章の中で、～の～となっている時に、属性として追加します。
- `操作追加`: 文章の述語を操作として追加します。
- `シーケンス図作成`: 文書の上から順に操作を呼び出すシーケンス図を作成します。
    -  `操作追加`が有効なときだけ有効になります。
## 使い方
- 新しいクラス図を開く
- Text2Modelプラグインの右下のテキストエリアに文章を入力する（例：運転手は車を運転する）
    - 左側の表のエリアに分割された文章が表示される
- 左下の`Add All +`ボタンを押す
    - 関連編集は要素を一つだけ選択しているときしか実行できません（ボタンが有効になりません）
- クラス図に運転手クラス、車クラス、関連が作成される

## 変更履歴
- V0.1
    - 初版

以上