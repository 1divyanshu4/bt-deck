# Contributing

Thanks for helping out. Bug reports, notes about how KeySet works on your devices, and pull
requests are all welcome.

## Reporting a bug

Open an issue using the bug report form. Bluetooth problems depend a lot on the exact
devices, so please tell us:

- which phone runs KeySet, and its Android version
- which tablet or computer you're connecting to
- what you did, what you expected, and what happened instead

If you can, also attach logs from the phone:

```bash
adb logcat -s HidKeyboard HidDeviceService BTIF_HD bt_hid
```

## Sending a change

1. Fork the repo and make a branch.
2. Keep each pull request to one fix or feature.
3. Before you push, run:
   ```bash
   ./gradlew spotlessApply      # formats the code
   ./gradlew test lintDebug     # runs the checks
   ```
   If you changed the UI, also run `./gradlew connectedDebugAndroidTest` on a phone or
   emulator.
4. If users will notice the change, add a line to [CHANGELOG.md](CHANGELOG.md).
5. Open the pull request.

## Writing code

- **Keep it easy to read.** Short functions, clear names, not too much nesting. If a
  function gets long, split it into smaller named pieces.
- Explain *why* in comments, not *what*. This matters most for Bluetooth workarounds: say
  which device or log message made it necessary.
- Features don't depend on each other. Shared code goes in `core`.
- Put new files straight into the module's `src/main/kotlin/` folder. Don't create
  `dev/keyset/...` folders.
- Add tests: logic gets unit tests in `src/test`, screen behaviour gets UI tests in
  `app/src/androidTest`.

## License

Anything you contribute is shared under the [Apache License 2.0](LICENSE), the same as the
rest of the project.
