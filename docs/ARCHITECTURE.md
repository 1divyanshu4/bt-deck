# How the code is organised

KeySet is split into small modules so each part is easy to find and change on its own. The
layout follows Google's [modularization guide](https://developer.android.com/topic/modularization/patterns),
kept small to match the size of the app.

## The modules

**`app`** starts the app. It has the main screen, the background service that keeps the
Bluetooth connection alive, and the UI tests.

**`core/model`** describes the basic things: keys, buttons, layouts, and the message sent
to the other device when a key is pressed. It's plain Kotlin with no Android code, so it's
quick to test.

**`core/data`** saves layouts and the user's custom icon images.

**`core/bluetooth`** does the Bluetooth work. `HidKeyboard` makes the phone act as a
keyboard, and `DeviceScanner` finds and pairs tablets and laptops.

**`core/designsystem`** holds the colours, the icon set, and small UI pieces used in several
places, like the "?" help button.

**`feature/pad`** is the grid of buttons, plus fullscreen mode.

**`feature/editor`** is where you change a button or a layout.

**`feature/connect`** is the connection screen and the status bar at the top.

**`feature/keyboard`** is the full on-screen keyboard and the "type text" box.

**`feature/layouts`** is the list of layouts, where you pick, rename, copy or delete them.

**`feature/settings`** is the settings screen and the connection help page.

The app has four tabs at the bottom (Pad, Layouts, Keyboard, Settings). `app/AppShell.kt`
switches between them with a simple `tab` variable instead of a navigation library, because
there's only one level of screens.

## A few rules

- Feature modules never use each other. `app` puts them together.
- `core` modules never use feature modules.
- Keep code `private` unless another module actually needs it.

## Where things are stored

- `HidKeyboard` knows whether the phone is connected, to what, and which keys are held down.
- `LayoutStore` has the layouts and saves them whenever they change.
- `SettingsStore` has the settings, saved with DataStore.
- Screens only keep short-lived things, like which dialog is open.

## Folders

Files go straight into each module's `src/main/kotlin/` folder, as the
[Kotlin style guide](https://kotlinlang.org/docs/coding-conventions.html#directory-structure)
recommends. For example, `feature/editor/src/main/kotlin/ButtonEditor.kt` belongs to the
`dev.keyset.feature.editor` package. Only smaller groups inside a module get their own
folder, like `theme/` and `icon/` in the design system.

## Build setup

Settings shared by all modules live in `build-logic/`, and library versions in
`gradle/libs.versions.toml`. That keeps each module's `build.gradle.kts` down to a few lines.

## Tests

- Unit tests sit next to the code they test, in `src/test`.
- UI tests are in `app/src/androidTest`. They run the app in a test mode that never uses
  Bluetooth, so running them can't send keys to whatever the test phone is paired with.
- Debug builds can be controlled over `adb` for testing between real devices. See
  [BLUETOOTH.md](BLUETOOTH.md#debugging).
