# Verification notes

Status legend: PASSED / FAILED / PENDING (not yet performed — never reported as passed).

## Build & static checks

| Check | Status | Notes |
|---|---|---|
| Unit tests (`testDebugUnitTest`) | PENDING | Run by CI / `scripts/verify_release.sh` |
| Release lint (`lintRelease`) | PENDING | |
| Signed non-minified release APK + AAB | PENDING | |
| `apksigner verify --print-certs` (no debug cert) | PENDING | Expected signer: `CN=PackPal Kids, OU=Mobile, O=egr-meister, C=BY` |
| AAB `jarsigner -verify` + signer SHA-256 match | PENDING | |
| Merged + packaged manifest: no INTERNET / ACCESS_NETWORK_STATE | PENDING | |
| Native libraries in APK/AAB (16 KB) | PENDING | No `.so` expected; confirm on artifacts |
| R8 + resource shrinking pass | PENDING | Only after the non-minified release is verified on a device |

## Device checks (signed release APK via `adb install`)

Device / emulator: _not yet tested_ · Android version: — · Artifact: —

| Check | Status |
|---|---|
| First launch in airplane mode, no permission prompts | PENDING |
| Five starter lists and all bundled icons render | PENDING |
| Packed state persists after closing the app | PENDING |
| Independent sessions per list | PENDING |
| Resume unfinished final check after relaunch | PENDING |
| Missing-item results and "Recheck missing" | PENDING |
| Duplicate-save prevention | PENDING |
| Parent editing + session-restart confirmation | PENDING |
| History accurate after list deletion | PENDING |
| Rotation, font scaling (200%), keyboard insets, Android Back / predictive Back | PENDING |
| Clear all local data | PENDING |
| No crashes in `adb logcat`, no network-dependent behaviour | PENDING |
