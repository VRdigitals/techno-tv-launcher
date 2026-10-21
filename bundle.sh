#!/bin/zsh
# Builds release/techno-tv.aab (Android App Bundle) for Google Play, signed with your upload key.
# Version: VERSION_CODE=2 VERSION_NAME=1.0.1 ./bundle.sh
set -euo pipefail
cd "${0:A:h}"
export VERSION_CODE="${VERSION_CODE:-1}" VERSION_NAME="${VERSION_NAME:-1.0.0}"

# 1. Normal build (compiles code and resources; also gives a test APK in build/)
[[ -f assets/voice/index.txt ]] || ./make_voice.sh
./build.sh >/dev/null
echo "Built test APK; making the bundle"

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
JAR="$SDK/platforms/android-36/android.jar"
BT=$(ls -d "$SDK"/build-tools/3[6-9]* | sort -V | tail -1)
[[ -n "${JAVA_HOME:-}" ]] || JAVA_HOME=$([[ -x "$HOME/jdk-17/bin/java" ]] && echo "$HOME/jdk-17" || /usr/libexec/java_home -v 17)
export PATH="$JAVA_HOME/bin:$PATH"

# 2. bundletool (Google's official tool), downloaded once
BUNDLETOOL=tools/bundletool.jar
if [[ ! -f "$BUNDLETOOL" ]]; then
  url=$(curl -fsSL https://api.github.com/repos/google/bundletool/releases/latest \
        | grep -o '"browser_download_url": *"[^"]*bundletool-all-[^"]*\.jar"' | head -1 | cut -d'"' -f4)
  echo "Downloading $url"
  curl -fsSL -o "$BUNDLETOOL" "$url"
fi

# 3. Resources in the protobuf format bundles use, then the module layout bundletool expects
rm -rf build/aab && mkdir -p build/aab/base/{manifest,dex}
"$BT/aapt2" link --proto-format -o build/aab/proto.zip -I "$JAR" --manifest AndroidManifest.xml -A assets \
  --min-sdk-version 31 --target-sdk-version 36 \
  --version-code "$VERSION_CODE" --version-name "$VERSION_NAME" build/res.zip
(cd build/aab/base && unzip -q ../proto.zip && mv AndroidManifest.xml manifest/)
cp build/dex/classes.dex build/aab/base/dex/
(cd build/aab/base && zip -q -r ../base.zip .)
mkdir -p release
rm -f release/techno-tv.aab
java -jar "$BUNDLETOOL" build-bundle --modules=build/aab/base.zip --output=release/techno-tv.aab

# 4. Sign with the upload key (made on first run; password kept next to it)
mkdir -p keystore
if [[ ! -f keystore/upload.jks ]]; then
  openssl rand -base64 24 | tr -d '\n' > keystore/upload-password.txt
  chmod 600 keystore/upload-password.txt
  keytool -genkeypair -keystore keystore/upload.jks -alias upload -keyalg RSA -keysize 4096 \
    -validity 10000 -storepass:file keystore/upload-password.txt -keypass:file keystore/upload-password.txt \
    -dname "CN=VR Digitals, O=VR Digitals" >/dev/null
  echo "Created your upload key: keystore/upload.jks. BACK UP the keystore/ folder."
fi
jarsigner -keystore keystore/upload.jks -storepass:file keystore/upload-password.txt \
  -sigalg SHA256withRSA -digestalg SHA-256 release/techno-tv.aab upload >/dev/null
jarsigner -verify release/techno-tv.aab >/dev/null && echo "Signed: release/techno-tv.aab ($(du -h release/techno-tv.aab | cut -f1)), version $VERSION_NAME ($VERSION_CODE)"
