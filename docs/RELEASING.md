# Making a release

1. Raise `versionCode` by one and set the new `versionName` in `app/build.gradle.kts`.
2. In `CHANGELOG.md`, put the notes under the new version and today's date.
3. Check that everything passes: `./gradlew test lintDebug spotlessCheck`, and run the UI
   tests on a phone.
4. Build the app: `./gradlew assembleRelease`. The signed APK is
   `app/build/outputs/apk/release/app-release.apk`; install it and try it once, since release
   builds are shrunk by R8.
5. Tag the release (for example `git tag v0.2.0`) and publish it on GitHub with the APK
   attached, named like `ZexDeck-0.2.0.apk`.

## About signing

Every release must be signed with the same key, or Android won't install it as an update.
The key is **not** in the repository. It's read from `~/.gradle/gradle.properties`:

```properties
BTDECK_RELEASE_STORE_FILE=/path/to/btdeck-release.jks
BTDECK_RELEASE_STORE_PASSWORD=…
BTDECK_RELEASE_KEY_ALIAS=btdeck
BTDECK_RELEASE_KEY_PASSWORD=…
```

Without these, `assembleRelease` makes an unsigned APK. Keep a safe backup of the keystore
and its password: if they're lost, users can't update to newer versions. Debug builds use a
different key, so uninstall a debug build before installing a release one.

For the Play Store, this key can be the upload key, with Play App Signing managing the key
users' devices see.
