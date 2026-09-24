# Bluetooth notes

KeySet uses Android's Bluetooth keyboard feature (the "HID Device" profile). It doesn't
always behave the same way on every device. This page writes down what we've learned, so
nobody has to figure it out again.

## How it works

- **Only one keyboard app at a time.** Android lets only one app per phone act as a
  keyboard. If another app is already doing it, KeySet can't.
- **The first connection has to come from the tablet or laptop.** Right after pairing, most
  devices won't accept a keyboard that tries to connect by itself. KeySet waits a few
  seconds and tries again. If that still fails, connect once from the tablet or laptop's
  Bluetooth settings. After that, KeySet can connect by itself.
- **One device at a time.** A keyboard only talks to one tablet or laptop. Connecting to a
  new one disconnects the old one.
- **Don't run KeySet on the tablet you want to type into.** A device that's acting as a
  keyboard can't receive keys from another keyboard. KeySet stops acting as a keyboard when
  you swipe it away while it's not connected.
- **KeySet shows a notification while it's running.** Android closes the Bluetooth
  keyboard connection when an app goes into the background, unless the app shows a
  notification like this.

## Problems KeySet fixes on its own

- **"Connect from … once"**: the tablet or laptop hasn't accepted the phone as a keyboard
  yet. See the first-connection note above.
- **"Restart Bluetooth"**: the phone is still holding on to an old connection, usually
  because KeySet was closed or updated while connected. Tap Restart (or use Restart in the
  Connect sheet). KeySet reconnects by itself.
- **Turning Bluetooth on and off**: since Android 13 apps can't do this silently.
  KeySet asks Android, which shows a small confirm dialog. Turning off uses Settings' hidden
  `REQUEST_DISABLE` request; phones without it open Bluetooth settings instead.
- **Caps Lock light**: the keyboard asks the host for its LED state (standard boot-keyboard
  output report). Hosts cache this when pairing, so devices paired before this was added
  need to be paired again to send it. Until then KeySet just flips Caps Lock itself.
- **Disconnect doesn't stick**: some tablets reconnect straight away. KeySet ignores those
  reconnects for 15 seconds, and doesn't reconnect by itself until you tap Connect again.

## Notes on specific devices

### Xiaomi / Redmi phones (MIUI 14, Android 13)

Tested on a Redmi Note 10S.

- When KeySet asks to become a keyboard, the phone says it failed, then does it anyway a
  moment later. KeySet waits for the real answer instead of believing the first one.
- When the phone disconnects, it never finishes the disconnect, and then refuses every new
  connection. KeySet notices this after 2.5 seconds and resets the connection itself.
- If KeySet is closed while connected, the phone may need Bluetooth turned off and on
  before KeySet works again.

### Samsung Galaxy Tab S9 FE+ (Android 16)

- Works both as the device you type into and as the keyboard.
- Connects to a newly paired keyboard by itself.
- Remembers old information about a device even after you unpair it.

### Linux laptops (BlueZ 5.87)

- Refuses a keyboard's first connection right after pairing. Waiting a few seconds fixes it,
  and KeySet does that for you.

## Debugging

To see what's happening on the phone:

```bash
adb logcat -s HidKeyboard HidDeviceService BTIF_HD bt_hid
```

On a Linux laptop, `journalctl -u bluetooth -f` shows why a connection was refused.

Debug builds can also be controlled from a computer, which helps when testing between two
real devices:

```bash
adb shell am broadcast -n dev.keyset/.DebugReceiver --es cmd status
adb shell am broadcast -n dev.keyset/.DebugReceiver --es cmd connect --es addr AA:BB:CC:DD:EE:FF
adb shell am broadcast -n dev.keyset/.DebugReceiver --es cmd button --es label Undo
```
