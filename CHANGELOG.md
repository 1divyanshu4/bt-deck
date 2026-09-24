# Changelog

Notable changes to KeySet, newest first.

## Unreleased

### New

- Tabs at the bottom: Pad, Layouts, Keyboard and Settings.
- Keyboard tab: a full keyboard with sticky Ctrl, Shift, Alt and Meta (tap once for the next
  key, twice to lock, or hold one down while pressing another key), F-keys and navigation
  keys. Tap the text box to write there with the same keys (the phone's own keyboard stays
  closed), then Send or Enter types it on the other device. On a phone held upright the
  keyboard tab lays itself out sideways, while the tab bar stays put.
- Caps Lock lights up while it's on and the letter keys show capitals. KeySet reads the
  Caps Lock light from the tablet or laptop, so it stays right (re-pair devices paired with
  older versions to get this).
- Bluetooth on/off and Restart in the Connect sheet, without leaving the app. Android asks
  to confirm each one. "Restart Bluetooth" in the status bar now does it directly.
- In landscape (and on tablets) the tabs move to a rail on the left, so screens keep their height.
- Layouts tab: every layout as a card with a preview; rename, duplicate, change columns or
  delete (with a confirmation).
- "Restore defaults" on the Layouts tab adds the built-in layouts back, keeping your own.
- Share layouts as `.keyset` files (the layout plus any custom images), save them to a file,
  and import them from the Layouts tab or by opening the file from another app.
- Settings: show icons with labels, icons only, or text only; show or hide shortcuts; button
  spacing; vibration; keep the screen on. Version and open-source licenses are under About.
- Tutorials (in Settings): short step-by-step guides for connecting, making layouts, adding,
  editing and rearranging buttons, the keyboard, sharing layouts and fixing connection problems.
- Edit mode: drag a button by its dots to move it; ✕ cancels every change since editing
  started. The grid shows empty slots and an Add tile, and an empty layout shows Add straight away.
- Less rounded look: corners are 8 dp for small parts and 12 dp for larger ones.

## 0.1.0 (24 September 2026)

First release.

### New

- Your phone works as a Bluetooth keyboard. Nothing needs to be installed on the other
  device.
- A pad of buttons that tap, hold or toggle. You can press several at once.
- Layouts with 2 to 6 columns and colours, starting with a drawing layout.
- About 110 drawing icons, icon search, and your own images as icons.
- Fullscreen mode.
- Scan, pair and connect from inside the app. It reconnects to the last device by itself.
- Stays connected while you use other apps.
- Every button works with TalkBack and switch access.

### Fixed

- Connecting on Xiaomi and Redmi phones.
- Disconnecting and switching devices on Xiaomi and Redmi phones.
- The first connection right after pairing, on some tablets and laptops.
