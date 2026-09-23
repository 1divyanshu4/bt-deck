# Making a release

1. Raise `versionCode` by one and set the new `versionName` in `app/build.gradle.kts`.
2. In `CHANGELOG.md`, move the notes from **Unreleased** under the new version and today's
   date.
3. Check that everything passes: `./gradlew test lintDebug spotlessCheck`, and run the UI
   tests on a phone.
4. Build the app: `./gradlew assembleRelease`.
5. Tag the release (for example `git tag v0.2.0`) and publish it on GitHub with the APK
   attached.

## About signing

For now, release builds are signed with the same test key as debug builds, so they install
over each other. Before publishing anywhere else, like the Play Store, make a real signing
key. Keep the key and its password out of the repository.
