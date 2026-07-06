# Agent Notes – Yabune Editor 1.21.10 NeoForge Port

## Project Context

- **Root:** `E:\RTM_MOD\mods\mcte_1.21.1_port\`
- **Target:** Minecraft 1.21.10 + NeoForge 21.10.64
- **Build:** Gradle 9.2.1 + NeoGradle 7.1.38, JDK 21
- **Mappings:** Parchment 2025.10.12
- **Output JAR:** `build/libs/ye-2.4.12-1.21.10-alpha*.jar`
- **Prism instance:** `C:\Users\tomam\AppData\Roaming\PrismLauncher\instances\MINATO MIRAI`

## Build

```bash
cd /e/RTM_MOD/mods/mcte_1.21.1_port
export JAVA_HOME="/c/Users/tomam/.gradle/jdks/eclipse_adoptium-21-amd64-windows.2"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew clean build
```

Then copy the new `ye-...-alpha*.jar` into `PrismLauncher/instances/MINATO MIRAI/minecraft/mods/` and remove any older `ye-...` jars.

## Active / Recent Issues

### 1. Text invisible in HUD and custom screens
**Root cause:** Minecraft 1.21.10 switched `GuiGraphics`/`Font` color ints to **ARGB** (alpha in the high byte). All old `0xRRGGBB` colors became transparent.
**Fix:** Convert every text color to `0xFFRRGGBB`:
- `0xFFFFFF` → `0xFFFFFFFF`
- `0xAAAAAA` → `0xFFAAAAAA`
- `0xFF0000` → `0xFFFF0000`
- `0x00FF00` → `0xFF00FF00`
- `0xFFFF55` → `0xFFFFFF55`
- `0xFF5555` → `0xFFFF5555`
- etc.

Affected files (verify all text colors):
- `client/EditorHud.java`
- `client/EntityEditorRenderer.java` (text labels, not the box fill colors which take a separate alpha argument)
- `gui/StructureBrowserScreen.java`
- `gui/LibraryScreen.java`
- `gui/EditorScreen.java`
- `gui/ChangeScreen.java`
- `gui/ExportScreen.java`
- `gui/BlockPickerScreen.java`
- `gui/SimpleStringList.java`

### 2. Save / Load / Library screens empty
**Root causes:**
- `Screen.isPauseScreen()` defaults to `true` in 1.21.10, pausing the integrated server while the screen is open so request packets are not handled.
- The vanilla `ObjectSelectionList` widget did not render entries reliably in this version.
**Fix:**
- Override `isPauseScreen()` → `false` in `StructureBrowserScreen` and `LibraryScreen`.
- Replaced the vanilla list with direct screen rendering (`render()` draws the list background + rows + text + selection/hover highlights; `mouseClicked`/`mouseScrolled` handle input).

### 3. Mode key shows `.` or `,` instead of `M`
**Root cause:** The code binds Mode to physical key `GLFW_KEY_M`. Minecraft displays the character produced by that physical key for the current keyboard layout (e.g., on AZERTY/Belgian layout the physical M key produces `.` or `,`).
**Code location:** `YEKeyHandlerClient.java`:
```java
KEY_EDIT_MODE = new KeyMapping("key.ye.edit_mode", ..., GLFW.GLFW_KEY_M, YE_CATEGORY);
```
**Fix options:**
- Keep `GLFW_KEY_M` and tell the user to rebind in **Options → Controls → Yabune Editor → Mode** to whichever physical key has `M` on their keyboard.
- Or change `GLFW.GLFW_KEY_M` to a different default (e.g., `GLFW.GLFW_KEY_SEMICOLON` produces `M` on French AZERTY but `;` on QWERTY – not ideal globally).

## Important File Paths

- Mod JAR (source of truth): `E:\RTM_MOD\mods\mcte_1.21.1_port\build\libs\`
- Prism mods folder: `C:\Users\tomam\AppData\Roaming\PrismLauncher\instances\MINATO MIRAI\minecraft\mods\`
- Prism logs: `C:\Users\tomam\AppData\Roaming\PrismLauncher\instances\MINATO MIRAI\minecraft\logs\latest.log`
- Prism options: `C:\Users\tomam\AppData\Roaming\PrismLauncher\instances\MINATO MIRAI\minecraft\options.txt`
- Structures saved by the mod: `C:\Users\tomam\AppData\Roaming\PrismLauncher\instances\MINATO MIRAI\minecraft\yabune_editor\structures\`

## Key Classes

- `YE.java` – main mod class, version string
- `YEKeyHandlerClient.java` – key bindings
- `YEClientEvents.java` – client tick key handling, startup chat debug
- `client/EditorHud.java` – in-game HUD popup
- `client/EntityEditorRenderer.java` – editor entity overlay boxes + labels
- `gui/EditorScreen.java` – main editor menu (K key)
- `gui/StructureBrowserScreen.java` – Save/Load structure list
- `gui/LibraryScreen.java` – Library list
- `gui/NonBlockingScreen.java` – base screen that does not pause and keeps movement keys alive
- `network/YENetwork.java` + packet classes – NeoForge payload network
- `editor/StructureManager.java` – reads/writes `yabune_editor/structures/*.nbt`

## Minimal-Change Rule

Keep edits minimal and consistent with the existing style. When adding debug logs, prefer `com.mojang.logging.LogUtils` + SLF4J. Do not bundle dependencies; the JAR should stay ~150 KB.
