# PackPal Kids

Offline backpack checklist for children and parents (Kotlin + Jetpack Compose).
Kids pack items compartment by compartment, then run a separate **Check before leaving** pass.
Parents manage lists in a parent area. Everything stays on the device.

## Features

- Home screen shaped like an **open backpack**: five compartment panels (Books rear pocket, Food small pocket,
  Clothes folded-fabric section, Tools narrow organizer, Important zipped front pocket), a luggage tag on the
  handle that opens the list selector, and a vertical stitched progress strip with the text "6 of 9 packed".
- Five editable starter lists: School, Sport, Art Class, Weekend, Trip (seeded once on first launch).
- Large item cards with bundled vector icons (31 item icons), packed/unpacked state with text + checkmark,
  saved immediately.
- Independent packing session per list; switching lists resumes that list's session. "Start fresh" (with
  confirmation when progress exists) never touches history.
- **Check before leaving**: warning for unpacked items ("Continue packing" / "Check anyway"), one item at a time
  ("Item 3 of 8", "It's here" / "Still missing"), result screen ("Everything checked!" or a missing list),
  "Recheck missing" within the same attempt, duplicate-safe "Save check". Every answer is persisted; an
  unfinished check shows "Resume check" after relaunch and can be discarded with confirmation.
- History of the latest 50 saved checks with immutable name/category/icon snapshots and per-check detail.
- Parent area (3-second press-and-hold, or tap → simple sum question as an accessible alternative; this is an
  accidental-entry safeguard, not security): list create/duplicate/delete, draft editor with Save/Cancel,
  icon picker, compartment chooser, move up/down reordering, notes, inline validation, session-restart
  confirmation, history management, animation preference, privacy screen, restore starter lists,
  clear all local data.

## Architecture

Single `app` module, manual dependency injection (`AppContainer`).

| Package | Contents |
|---|---|
| `domain` | Models, `PackingRules`, `VerificationRules`, `Validation`, `HistoryRules`, starter lists (pure Kotlin, unit-tested) |
| `data/local` | Room entities, DAOs, `PackPalDatabase`, `Migrations` |
| `data/repository` | `TemplateRepository`, `PackingRepository`, `HistoryRepository` (transactions for saves, edits, resets) |
| `data/prefs` | DataStore: selected list, animations, one-time seed flag |
| `ui/*` | `backpack`, `templates`, `compartment`, `verification`, `history`, `parent`, `components`, `theme` |

ViewModel + StateFlow, `collectAsStateWithLifecycle`, Navigation Compose, coroutines; Room work runs on
Room's background executors. The editor draft is kept in `SavedStateHandle` (JSON) so unsaved edits survive
process death.

Room entities: `TemplateEntity`, `ItemEntity`, `PackingSessionEntity`, `PackingItemStateEntity`,
`VerificationAttemptEntity`, `VerificationItemEntity`, `CheckHistoryEntity`, `CheckHistoryItemEntity`.
Schemas are exported to `app/schemas/`. Destructive migration is never enabled; add `Migration` objects to
`Migrations.ALL` for future versions.

## Toolchain

| Component | Version |
|---|---|
| JDK | 17 (Temurin in CI) |
| Gradle (wrapper) | 8.14.3 |
| Android Gradle Plugin | 8.13.0 |
| Kotlin / Compose compiler plugin | 2.2.10 |
| KSP | 2.2.10-2.0.2 |
| Compose BOM | 2025.09.00 |
| Room | 2.7.2 |
| DataStore | 1.1.7 |
| Navigation Compose | 2.9.4 |
| Lifecycle | 2.9.3 |
| Build tools | 36.0.0 |

`compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`. Do not lower compile/target SDK to work around failures.

Android 16 notes: edge-to-edge is enforced at targetSdk 36 (handled with `enableEdgeToEdge` and
`WindowInsets.safeDrawing`); predictive Back is enabled (`enableOnBackInvokedCallback`, Compose `BackHandler`);
no orientation lock or resizability restriction, so large screens get the wider backpack layout.

## Build

```bash
./gradlew testDebugUnitTest lintRelease     # tests + release lint
./gradlew assembleDebug                      # debug build, no release credentials needed
./gradlew assembleRelease bundleRelease      # signed release (needs credentials, see below)
./scripts/verify_release.sh                  # local build + full signature/permission/16 KB checks
```

Outputs:

- APK: `app/build/outputs/apk/release/app-release.apk` (local install and verification)
- AAB: `app/build/outputs/bundle/release/app-release.aab` (**upload only this to Google Play**)

R8 and resource shrinking are **off by default**. After the non-minified signed release has been verified,
build with `-PpackpalMinify=true` (or `PACKPAL_MINIFY=true ./scripts/verify_release.sh`), repeat the device
checks below and keep `app/build/outputs/mapping/release/mapping.txt`.

## Release signing (PKCS12)

`app/build.gradle.kts` defines a `release` signing config with `storeType = "PKCS12"`, assigned explicitly to
the release build type. Credentials come from environment variables or an uncommitted `signing.properties`:

| Env variable | signing.properties key |
|---|---|
| `ANDROID_KEYSTORE_PATH` | `storeFile` (relative to project root, or absolute) |
| `ANDROID_KEYSTORE_PASSWORD` | `storePassword` |
| `ANDROID_KEY_ALIAS` | `keyAlias` |
| `ANDROID_KEY_PASSWORD` | `keyPassword` |

Any release assemble/bundle/package task **fails** when credentials are missing; there is no debug-signing
fallback. Debug builds need nothing. `*.p12`, `*.jks`, `signing.properties` are git-ignored.

GitHub Secrets required by the workflow:

- `ANDROID_KEYSTORE_BASE64` – `base64` of the `.p12` file
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

**Google Play App Signing:** the key in this keystore is the *upload key*. On first upload Play generates and
keeps a separate *app signing key*; devices installing from Play see that Play-managed certificate, while
locally installed APKs carry the upload certificate. Register the upload certificate SHA-256 in Play Console
and keep the `.p12` + passwords backed up privately.

## GitHub Actions (`.github/workflows/release.yml`)

1. Checkout, JDK 17, Android SDK Platform 36 + Build-Tools 36.0.0, Gradle Wrapper validation.
2. `testDebugUnitTest testReleaseUnitTest`, `lintRelease`.
3. Decode keystore to `$RUNNER_TEMP`, `assembleRelease bundleRelease`.
4. `apksigner verify --print-certs` (fails on `CN=Android Debug`).
5. `jarsigner -verify` on the AAB plus signer SHA-256 match against the keystore (a self-signed upload
   certificate is expected and is not treated as invalid).
6. Merged + packaged manifest permission check (no `INTERNET` / `ACCESS_NETWORK_STATE`).
7. Native-library inspection; ELF/zip 16 KB checks run only if `.so` files exist.
8. Upload APK, AAB and verification logs; delete the temporary keystore.

No emulator test is required in CI.

## Privacy, offline behaviour, storage, backup

- No `INTERNET` or `ACCESS_NETWORK_STATE` permission (also defensively removed with `tools:node="remove"`).
  No backend, Firebase, ads, analytics, payments, accounts, remote content, location or contacts.
- No runtime permissions are requested. All illustrations are bundled vector drawables or Compose drawing.
- Data lives only in app-private storage (Room database + DataStore).
- `android:allowBackup="false"`, `fullBackupContent` excludes all domains (Android ≤ 11) and
  `dataExtractionRules` excludes everything from cloud backup **and** device transfer (Android 12+).
- No export or sharing. The in-app Privacy screen states that lists and history stay on the device.
- "Clear all local data" explains what is deleted, requires confirmation, wipes all tables and preferences,
  and restores the five starter lists with empty history.

## Permission check

```bash
$ANDROID_HOME/build-tools/36.0.0/aapt2 dump permissions app/build/outputs/apk/release/app-release.apk
grep uses-permission app/build/intermediates/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml
```

AndroidX core may add its own non-exported `…DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (signature-level,
app-internal); it is the only permission tolerated by CI.

## 16 KB page size

The app declares no native code and none of its dependencies (Compose, Room runtime, DataStore, Navigation,
Lifecycle, coroutines) ship `.so` files. Verify on the actual artifacts with
`unzip -l app-release.apk | grep '\.so$'` (and the same on the AAB). See `docs/VERIFICATION.md` for the
recorded result. Targeting API 36 alone is not treated as evidence of 16 KB compatibility.

## Local device verification

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
adb logcat -c && adb logcat --pid=$(adb shell pidof -s com.packpal.kids) '*:W'
```

Checklist and results: `docs/VERIFICATION.md`.
