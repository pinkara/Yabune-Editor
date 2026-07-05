# Yabune Editor

A world editing mod for Minecraft 1.21.1 (NeoForge 21.1.234).  
Created by **Pinkara**.

## Features

- **Selection box** with start/end markers and size display
- **Copy / Cut / Paste** blocks with rotation and mirror support
- **Clone** selections with offset, count and live preview
- **Fill** a selection with the held block
- **Change** multiple block types at once via an inventory-style picker
- **Miniature block** that stores a copied structure in an item
- **Structure save/load** to disk so you can paste builds across worlds
- **Mesh export** selected areas to `.obj` or `.stl`
- Non-blocking editor GUI (move, change hotbar, open inventory while the menu is open)
- Numpad shortcuts for quick rotations and mirrors

## Default Controls

| Key | Action |
|-----|--------|
| `K` | Open editor menu |
| `M` | Cycle edit mode |
| `N` | Clear editor entity |
| `DELETE` | Delete selected blocks |
| `Z` | Undo |
| `X` | Cut |
| `C` | Copy |
| `V` | Paste |
| `B` | Fill selection |
| Numpad `8/2` | Rotate X +/- |
| Numpad `4/6` | Rotate Y +/- |
| Numpad `7/9` | Rotate Z +/- |
| Numpad `1/5/3` | Mirror X / Y / Z |

## Structure Save / Load

1. Copy a selection with `C`.
2. In the editor menu click **Structures**.
3. Enter a name and click **Save**.

Saved structures are written to `<game directory>/yabune_editor/structures/<name>.nbt`.

To load, open the **Structures** screen, select a file and click **Load**. The structure is placed on your clipboard; use `V` to paste it where you are looking.

## Mesh Export (.obj / .stl)

1. Set a selection.
2. In the editor menu click **Export**.
3. Choose a file name and format, then click **Export**.

Exported meshes are written to `<game directory>/yabune_editor/exports/<name>.obj` or `<name>.stl`. Only solid (non-air) blocks are exported, with exterior faces only.

## Building

```bash
export JAVA_HOME="/path/to/jdk-21-or-higher"
./gradlew build
```

The built JAR will be in `build/libs/`.

## Running the test client

```bash
export JAVA_HOME="/path/to/jdk-21-or-higher"
./gradlew runClient
```

---

# Yabune Editor （やぶねエディタ）

Minecraft 1.21.1（NeoForge 21.1.234）用のワールド編集MOD。  
作成者：**Pinkara**

## 機能

- 開始/終了マーカーとサイズ表示付きの**選択ボックス**
- 回転・ミラー対応の**コピー / 切り取り / 貼り付け**
- オフセット、個数、リアルタイムプレビュー付きの**クローン**
- 手持ちブロックで選択範囲を埋める**フィル**
- インベントリー風ピッカーで複数ブロックを置き換える**チェンジ**
- コピーした構造物をアイテムに保存する**ミニチュアブロック**
- ワールド間で使える**構造物の保存/読み込み**
- 選択範囲を `.obj` / `.stl` メッシュに**エクスポート**
- 非ブロッキングエディタGUI（メニュー表示中も移動、ホットバー変更、インベントリが開ける）
- テンキーによる素早い回転・ミラー操作

## デフォルト操作

| キー | 操作 |
|------|------|
| `K` | エディタメニューを開く |
| `M` | 編集モードを切り替え |
| `N` | エンティティをクリア |
| `DELETE` | 選択範囲を削除 |
| `Z` | 元に戻す |
| `X` | 切り取り |
| `C` | コピー |
| `V` | 貼り付け |
| `B` | フィル |
| テンキー `8/2` | X軸回転 +/- |
| テンキー `4/6` | Y軸回転 +/- |
| テンキー `7/9` | Z軸回転 +/- |
| テンキー `1/5/3` | X / Y / Z ミラー |

## 構造物の保存 / 読み込み

1. `C` で選択範囲をコピーします。
2. エディタメニューで **Structures** をクリックします。
3. 名前を入力して **Save** をクリックします。

保存された構造物は `<ゲームディレクトリ>/yabune_editor/structures/<名前>.nbt` に書き込まれます。

読み込むには **Structures** 画面を開き、ファイルを選択して **Load** をクリックします。構造物はクリップボードに読み込まれ、見ている場所に `V` で貼り付けられます。

## メッシュエクスポート（.obj / .stl）

1. 選択範囲を設定します。
2. エディタメニューで **Export** をクリックします。
3. ファイル名と形式を選んで **Export** をクリックします。

書き出されたメッシュは `<ゲームディレクトリ>/yabune_editor/exports/<名前>.obj` または `<名前>.stl` に出力されます。空気以外のブロックのみが書き出され、外部に見える面のみが含まれます。

## ビルド

```bash
export JAVA_HOME="/path/to/jdk-21-or-higher"
./gradlew build
```

ビルドされたJARは `build/libs/` に出力されます。

## テストクライアントの実行

```bash
export JAVA_HOME="/path/to/jdk-21-or-higher"
./gradlew runClient
```
