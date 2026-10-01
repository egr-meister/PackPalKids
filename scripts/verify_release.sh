#!/usr/bin/env bash
# Builds and verifies the signed PackPal Kids release locally (macOS or Linux).
# Usage: ./scripts/verify_release.sh            (non-minified release, the default first pass)
#        PACKPAL_MINIFY=true ./scripts/verify_release.sh   (R8 + resource shrinking pass)
# Output: build/verification/report.txt (+ apksigner/jarsigner/permission dumps)
set -uo pipefail
cd "$(dirname "$0")/.."
ROOT=$(pwd)
OUT="$ROOT/build/verification"; mkdir -p "$OUT"
REPORT="$OUT/report.txt"; : > "$REPORT"
log() { echo "$*" | tee -a "$REPORT"; }
fail() { log "FAIL: $*"; log "Result: FAILED"; exit 1; }

# --- JDK 17+ ---
if [ -z "${JAVA_HOME:-}" ]; then
  for c in "/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
           "$(/usr/libexec/java_home -v 17+ 2>/dev/null || true)"; do
    [ -n "$c" ] && [ -x "$c/bin/java" ] && export JAVA_HOME="$c" && break
  done
fi
[ -n "${JAVA_HOME:-}" ] || fail "JDK 17+ not found. Install Temurin 17 or Android Studio."
export PATH="$JAVA_HOME/bin:$PATH"
log "JDK: $("$JAVA_HOME/bin/java" -version 2>&1 | head -1)"

# --- Android SDK 36 ---
export ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/android-sdk}}"
[ -d "$ANDROID_HOME" ] || ANDROID_HOME="$HOME/Library/Android/sdk"
[ -d "$ANDROID_HOME" ] || fail "Android SDK not found (set ANDROID_HOME)."
echo "sdk.dir=$ANDROID_HOME" > local.properties
SDKM=$(ls "$ANDROID_HOME"/cmdline-tools/*/bin/sdkmanager 2>/dev/null | head -1)
BT=36.0.0
if [ ! -d "$ANDROID_HOME/platforms/android-36" ] || [ ! -d "$ANDROID_HOME/build-tools/$BT" ]; then
  [ -n "$SDKM" ] || fail "sdkmanager not found; install Android SDK Platform 36 and Build-Tools $BT."
  yes | "$SDKM" --licenses >/dev/null 2>&1 || true
  "$SDKM" "platforms;android-36" "build-tools;$BT" "platform-tools" | tail -2
fi
BTD="$ANDROID_HOME/build-tools/$BT"
log "SDK: $ANDROID_HOME (platform 36, build-tools $BT)"

MINIFY=${PACKPAL_MINIFY:-false}
log "Minify/shrink: $MINIFY"

./gradlew --no-daemon testDebugUnitTest lintRelease 2>&1 | tee "$OUT/gradle-test-lint.log" | tail -25
[ "${PIPESTATUS[0]}" -eq 0 ] || fail "unit tests or release lint (see $OUT/gradle-test-lint.log)"
log "Unit tests: PASSED"; log "Release lint: PASSED"

./gradlew --no-daemon -PpackpalMinify="$MINIFY" assembleRelease bundleRelease 2>&1 | tee "$OUT/gradle-release.log" | tail -25
[ "${PIPESTATUS[0]}" -eq 0 ] || fail "release build (see $OUT/gradle-release.log)"
APK=app/build/outputs/apk/release/app-release.apk
AAB=app/build/outputs/bundle/release/app-release.aab
[ -f "$APK" ] && [ -f "$AAB" ] || fail "APK/AAB not produced"
log "APK: $APK ($(du -h "$APK" | cut -f1))"; log "AAB: $AAB ($(du -h "$AAB" | cut -f1))"

"$BTD/apksigner" verify --verbose --print-certs "$APK" > "$OUT/apksigner.txt" 2>&1 || fail "apksigner verify"
grep -qi "CN=Android Debug" "$OUT/apksigner.txt" && fail "APK signed with debug certificate"
log "apksigner: $(grep -m1 'Signer #1 certificate DN' "$OUT/apksigner.txt")"
log "apksigner: $(grep -m1 'SHA-256 digest' "$OUT/apksigner.txt")"
grep "Verified using v" "$OUT/apksigner.txt" | tee -a "$REPORT"

jarsigner -verify -verbose -certs "$AAB" > "$OUT/jarsigner.txt" 2>&1
grep -q "jar verified" "$OUT/jarsigner.txt" || fail "AAB jarsigner verification"
grep -qi "CN=Android Debug" "$OUT/jarsigner.txt" && fail "AAB signed with debug certificate"
log "AAB signer: $(keytool -printcert -jarfile "$AAB" | grep -m1 'Owner:')"
log "AAB signer: $(keytool -printcert -jarfile "$AAB" | grep -m1 'SHA256:')"

MERGED=$(find app/build/intermediates -path '*merged_manifest*release*' -name AndroidManifest.xml | head -1)
"$BTD/aapt2" dump permissions "$APK" > "$OUT/permissions.txt"
"$BTD/aapt2" dump badging "$APK" | grep -E "^package|sdkVersion|targetSdkVersion" | tee -a "$REPORT"
grep -E "INTERNET|ACCESS_NETWORK_STATE" "$MERGED" "$OUT/permissions.txt" && fail "network permission present"
log "Merged manifest ($MERGED) permissions: $(grep -c 'uses-permission' "$MERGED" || true)"
log "Packaged APK permissions:"; sed 's/^/  /' "$OUT/permissions.txt" | tee -a "$REPORT" >/dev/null
grep -q 'allowBackup="false"' "$MERGED" && log "allowBackup=false: yes" || fail "allowBackup not false"
grep -q 'dataExtractionRules' "$MERGED" && log "dataExtractionRules: set" || fail "dataExtractionRules missing"

SO_APK=$(unzip -Z1 "$APK" | grep '\.so$' || true)
SO_AAB=$(unzip -Z1 "$AAB" | grep '\.so$' || true)
if [ -z "$SO_APK$SO_AAB" ]; then
  log "16 KB: no native .so libraries in APK or AAB (ELF alignment not applicable)."
else
  log "16 KB: native libraries found:"; echo "$SO_APK $SO_AAB" | tee -a "$REPORT"
  "$BTD/zipalign" -c -P 16 -v 4 "$APK" > "$OUT/zipalign.txt" 2>&1 && log "zipalign -P 16: OK" || fail "zipalign 16 KB"
fi
"$BTD/zipalign" -c -v 4 "$APK" > /dev/null 2>&1 && log "zipalign (4-byte): OK" || fail "zipalign"
[ "$MINIFY" = "true" ] && [ -f app/build/outputs/mapping/release/mapping.txt ] && log "R8 mapping: app/build/outputs/mapping/release/mapping.txt"
log "Result: PASSED"
