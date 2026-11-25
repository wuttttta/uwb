# PROGRESS

## Hardware-verified

Nothing yet. (Phone model + Android version for every item that lands here.)

## 2026-11-25: M0 scaffold

**Changed**
- Gradle multi-module project: `protocol` (pure Kotlin/JVM), `verifier-cli` (JVM, `application`), `app` (Android, Compose).
- Toolchain: Gradle 9.9.2, AGP 9.5.2 (built-in Kotlin, so no `kotlin-android` plugin), Kotlin 2.4.21, compileSdk/targetSdk 37, minSdk 34, Java 17 bytecode. Libraries: `androidx.core.uwb:uwb:1.1.0-alpha03`, `play-services-nearby:19.5.1`, Compose BOM 2026.11.01, coroutines 1.11.0.
- Encoding decision: **canonical JSON**, not CBOR. JCS-style rules restricted to integers: sorted keys (UTF-16 order), no whitespace, no floats, byte strings as unpadded base64url with strict decoding. Strict parser rejects floats, leading zeros, duplicate keys, lone surrogates, invalid UTF-8, trailing data, deep nesting. Signatures will always be over the re-encoded canonical bytes. Tests cover determinism (shuffled field order) and round-trips.
- Interfaces + fakes in `protocol`: `PeerChannel` / `InMemoryChannelPair` (with wire hooks for tamper/drop tests), `RangingProvider` / `FakeRangingProvider` (scripted mm values, virtual-time timestamps). `RangingSessionParams.toString()` redacts the STS key.
- App: single activity, Start screen (Host/Join, disabled until M1), About screen with the known limits, `POE` log tag, `INSECURE_STATIC_STS` and `DEBUG_LOG_SECRETS` BuildConfig flags (both false; a unit test enforces it). Manifest strips `INTERNET`.
- Verifier CLI: argument parsing per the spec; verification itself is M4 and it always exits FAIL until then.
- CI (`.github/workflows/build.yml`): tests + APK on every push; on `main`, when the signing secrets exist, a GitHub Release `v0.1.<run>` with `uwb-0.1.<run>.apk` for Obtainium.
- Release signing key: EC P-256, PKCS12, kept outside the repo. Certificate SHA-256 (this goes in the attestation allow-list):
  `DEEEE1B4A27299A1FA40AEFA90FDE1DB5ADCBC4A2E6FE655646C83C43745D12C`

**Tests**: `./gradlew test` on the dev Pi: 27 JVM tests pass (protocol + verifier-cli). The `app` module builds only where an Android SDK exists (CI); see below.

**Environment notes**
- Dev machine is a Raspberry Pi (aarch64). Google ships no ARM64 Linux Android build tools, so `settings.gradle.kts` includes `:app` only when an SDK is configured (`ANDROID_HOME`, `ANDROID_SDK_ROOT` or `local.properties`). CI always builds it. Local JDK: `~/.local/jdk/jdk-21*` (set `JAVA_HOME`).

**Differences from CLAUDE.md / docs (from reading developer.android.com, 2026-11-25)**
- The UWB overview's Jetpack snippet is outdated (uses deprecated `clientSessionScope()`, no session id, null key). Use the `RangingParameters` reference instead: `RangingParameters(uwbConfigType, sessionId, subSessionId, sessionKeyInfo, subSessionKeyInfo, complexChannel, peerDevices, updateRateType, uwbRangeDataNtfConfig, slotDurationMillis, isAoaDisabled)`.
- Provisioned STS `sessionKeyInfo` may be 16 **or 32** bytes; we use 16 as planned.
- `CONFIG_PROVISIONED_UNICAST_DS_TWR = 4`. There is no "secure ranging supported" flag: support is inferred from `RangingCapabilities.supportedConfigIds` containing 4.
- The official sample `android/connectivity-samples/UwbRanging` is archived (Jul 2025, uwb alpha05). The UWB page now links `android/platform-samples/.../connectivity/UwbRanging`; read that in M1.
- Android 16 `android.ranging` API exists (RANGING permission, API 36), but the pages read don't show provisioned STS with an app key there, and the UWB page still says "UWB is currently only available using Jetpack". Staying on Jetpack.

**Open questions (for M1)**
- Do both roles get distance results in unicast DS-TWR? Docs imply yes (the controlee snippet collects positions) but don't say so.
- Does Jetpack UWB need Play services? Release notes suggest it falls back to the AOSP backend without GMS.
- Update-rate constants: the reference text lists AUTOMATIC=240 ms, INFREQUENT=120 ms, FREQUENT=600 ms, which looks swapped. Measure the real interval on the phones.
- Minimum permission set on Android 14+ for Nearby + UWB (sample's list is old).
