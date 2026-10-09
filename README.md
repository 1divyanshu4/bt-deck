# ZexDeck

ZexDeck turns your Android phone into a shortcut pad for drawing.

Your phone connects to your tablet or laptop as a normal Bluetooth keyboard. You fill the
screen with big buttons like Undo, Brush, Eraser or Pan, and tap them with one hand while
you draw with the other. Nothing needs to be installed on the tablet or laptop.

## What it can do

- Buttons that tap, stay pressed while you hold them, or toggle on and off
- Press several buttons at once, like holding Space to pan while tapping Undo
- Separate layouts for different apps, with 2 to 6 columns and your own colours
- About 110 drawing icons, or your own images
- A fullscreen mode that shows only the buttons
- A full keyboard (with Ctrl, Alt, arrows and F-keys), and a box for writing a whole
  sentence before sending it
- Settings for how buttons look (icons only, text only…), vibration and spacing
- Works with Android tablets, Windows, Mac, Linux and ChromeOS
- Works with TalkBack

## What you need

- A phone with Android 9 or newer
- A tablet or computer with Bluetooth

A few phone makers switch off the Bluetooth keyboard feature. If yours has, ZexDeck will
tell you.

## How to use it

1. Install ZexDeck on your phone (download it from [Releases](../../releases)).
2. Open it and allow Bluetooth access.
3. On your tablet or laptop, open the Bluetooth settings page.
4. In ZexDeck, tap the status button in the top left, then **Scan**, then **Pair** next to
   your device.
5. Start drawing. Next time, ZexDeck reconnects on its own.

If the first connection fails, connect once from the tablet or laptop's Bluetooth settings.
After that it works from ZexDeck too. [docs/BLUETOOTH.md](docs/BLUETOOTH.md) explains why and
lists known device quirks.

## Building it yourself

You need JDK 17 and the Android SDK.

```bash
./gradlew assembleDebug     # builds the app
./gradlew installDebug      # installs it on a phone connected by USB
```

To run the checks:

```bash
./gradlew test lintDebug spotlessCheck   # quick checks, no phone needed
./gradlew connectedDebugAndroidTest      # UI tests, needs a phone or emulator
```

## How the code is organised

The app is split into small parts:

- `app` starts the app and puts the screens together
- `core/model` describes keys, buttons and layouts
- `core/data` saves layouts and custom icons
- `core/bluetooth` makes the phone act as a keyboard
- `core/designsystem` holds colours, icons and shared pieces of UI
- `feature/…` holds one part of the app each: the pad, the button editor, the connection
  screen, the keyboard, the layouts list and the settings

[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) has more detail.

## Contributing

Bug reports and pull requests are welcome. Start with [CONTRIBUTING.md](CONTRIBUTING.md).

## License

Apache License 2.0. See [LICENSE](LICENSE).
