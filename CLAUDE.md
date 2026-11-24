# CLAUDE.md: Proof of Encounter (UWB) test app

## Goal

Build an Android test app where two phones produce a **proof that they were physically close at the same moment**, which a third party can verify later without trusting either phone's owner.

The cheat to defeat: two people in different places relaying every message between their phones over the internet. Secrets alone can't stop that; timing can. Radio covers about 30 cm per nanosecond, while any internet relay adds milliseconds. So:

- **Proximity** comes from UWB secure ranging: time of flight, protected by a scrambled timestamp sequence (STS) keyed per session.
- **The proof** is a record co-signed by both phones' hardware-backed keys.
- **Trust in the phones** comes from Android Key Attestation: the keys live in genuine hardware, on a locked device with verified boot, inside this app.

This is a prototype for learning what works on real hardware. Honest measurements beat polish.

## Scope

- In: Android to Android, Kotlin, offline peer-to-peer, plus a JVM verifier CLI.
- Out for now: iOS, servers, accounts, payments, blockchain, analytics.
  - iOS note: Android 16's Ranging module can range with iPhones through Apple's Nearby Interaction Accessory Protocol, but only with **static STS** (`CONFIG_UNICAST_DS_TWR`), which Android's UWB docs list as susceptible to relay attacks. Not good enough for this protocol; at most a future, clearly labelled insecure demo.

## Hardware and environment

- Two UWB Android phones on **Android 14 or later** (provisioned STS needs 14+), ideally the same model: Pixel 6 Pro or a later Pro, Pixel Fold, Galaxy S21+/Ultra or a later Plus/Ultra, Galaxy Z Fold2 or later. Set `minSdk = 34`.
- Global-market phones with Google Play services: Nearby Connections needs it. Check whether the Jetpack UWB library does too.
- UWB must be switched on in system settings.
- Assume emulators can't do UWB. Everything that doesn't need the radio must run against fakes (see Architecture).
- The user installs builds on both phones and reports back logs. When you need device results, say exactly what to do and which logcat filter to use.

## Rules for you (Claude Code)

1. **Check the docs before coding.** The UWB, Nearby and Keystore APIs change. Before writing that code, read the current developer.android.com pages (UWB, Ranging, Key Attestation) and the official UWB sample (`android/connectivity-samples`, folder `UwbRanging`, which uses Nearby Connections as the out-of-band channel). Use the latest `androidx.core.uwb` release. If anything in this file conflicts with the docs, follow the docs and record the difference in `PROGRESS.md`.
2. **Never fake security.** If provisioned STS isn't available, don't silently fall back. Static STS is allowed only behind an explicit `INSECURE_STATIC_STS` debug flag, with a red banner in the UI and `"insecure": true` in any record produced.
3. **Don't claim what you haven't seen.** "Builds" and "tests pass" are not "works on the phones". Track hardware-verified items separately in `PROGRESS.md`.
4. Work one milestone at a time. At the end of each: run the tests, update `PROGRESS.md`, commit, then stop and give the user a short on-device checklist.
5. No secrets in logs (STS key, ECDH output, derived keys). A `DEBUG_LOG_SECRETS` flag may exist, off by default.
6. The app makes no internet requests and has no telemetry.
7. Kotlin, coroutines and Flow, Jetpack Compose, single activity, small files. Use the log tag `POE` everywhere.

## Architecture

Gradle multi-module project (Kotlin DSL):

```
app/           Android app: UI, UWB, Nearby Connections, Android Keystore
protocol/      Pure Kotlin/JVM: messages, canonical encoding, HKDF, codes,
               record model, signature and attestation verification
verifier-cli/  JVM command-line tool that verifies a proof bundle
```

- `protocol` has no Android dependencies, so it's unit-tested on the JVM and shared by `app` and `verifier-cli`.
- Hide the radios behind interfaces so the whole flow runs without hardware:
  - `RangingProvider`: start a session, emit `RangingSample(distanceMm, timestampMs)` as a Flow, stop. Implementations: `JetpackUwbRangingProvider` and `FakeRangingProvider` (scripted distances).
  - `PeerChannel`: send and receive framed byte messages. Implementations: `NearbyConnectionsChannel` and `InMemoryChannelPair`.

## Protocol v0

Roles: on the first screen the user picks **Host** (A: initiator, UWB controller) or **Join** (B: responder, UWB controlee). Every message carries `v: 0` and a type.

Encoding: deterministic CBOR (RFC 8949 section 4.2), or JSON with sorted keys and **no floats** (integer millimeters, integer milliseconds). Pick one, use it everywhere, and test that it's deterministic.

Crypto: EC P-256 throughout, ECDSA with SHA-256, HKDF-SHA256 (RFC 5869), HMAC-SHA256.

### Identity (once per install)

Generate an EC P-256 signing key in Android Keystore with an attestation challenge (for example SHA-256 of a random enrollment nonce). Prefer StrongBox and fall back to the TEE; record which one was used. Keep its certificate chain (`KeyStore.getCertificateChain`). Its public key is the phone's identity.

### Session

1. **Connect** over `PeerChannel` (Nearby Connections). Treat the channel as untrusted.
2. **Hello.** A to B: `{nonce_A (32 bytes), eph_pub_A, uwb_A}`. B to A: `{nonce_B, eph_pub_B, uwb_B}`. `eph_*` are fresh P-256 key pairs for this session. `uwb_*` are the non-secret ranging parameters each side needs (UWB address, plus complex channel and session id from the controller).
3. **Key schedule.** `Z = ECDH(own eph_priv, peer eph_pub)` and `TH = SHA-256(canonical(hello_A) || canonical(hello_B))`. Run HKDF with IKM `Z` and salt `TH`:
   - `sts_key`: 16 bytes, info `"poe/v0/sts"`. This becomes the UWB `sessionKeyInfo` for provisioned STS.
   - `mac_key`: 32 bytes, info `"poe/v0/mac"`. HMAC on every later message.
   - `code_A` and `code_B`: 4 bytes each, info `"poe/v0/code/A"` and `"poe/v0/code/B"`, read as a big-endian uint32 mod 1,000,000 and zero-padded to 6 digits.
4. **Identity proof.** Each side sends `{cert_chain, sig = Sign_identity(TH)}`. Verify the peer's signature over `TH` and its attestation (see below). This ties each identity to this session's keys and blocks a man in the middle.
5. **Codes (requested by the user).** Each phone shows its own code. Each person types the *other* phone's code into their own phone, which continues only if it matches. Codes depend on the session secret, so a code read out from a different session fails. Optional alternative mode: both phones show one shared code and both users tap "Match".
6. **Ranging.** Start UWB with `RangingParameters.CONFIG_PROVISIONED_UNICAST_DS_TWR` and `sessionKeyInfo = sts_key`; use the frequent update rate if available. Check whether both roles receive distance results. If only the controller does, record that in `PROGRESS.md` and run a second session with the roles swapped, so each phone measures for itself.
   - **Acceptance rule** (configurable): within 15 s, at least **20 consecutive** valid readings at **500 mm or less**. Readings at contact distance may be noisy, so log raw values for tuning.
7. **Summaries.** Each side sends, HMAC-protected: `{count, max_consecutive_ok, min_mm, median_mm, max_mm, window_ms, threshold_mm, wall_clock_start_ms}`.
8. **Record and signatures.** Both build the identical canonical record. Each phone signs only if **its own** summary passes; never rely on the peer's summary for your own decision. Exchange signatures.
9. **Save** the bundle locally and offer export through the share sheet.

### Record and bundle

```
record = {
  v: 0, protocol: "poe/v0",
  session_id: first 16 bytes of TH,
  nonce_a, nonce_b,
  identity_pub_a, identity_pub_b,   // SPKI DER
  eph_pub_a, eph_pub_b,
  uwb: { config_id, sts: "provisioned" },
  summary_a, summary_b,
  insecure: false
}
bundle = { record, sig_a, sig_b, cert_chain_a, cert_chain_b }
```

### Attestation checks

These live in `protocol`. The app uses them on the peer's chain; the CLI uses them on both.

- The chain must verify up to one of Google's **current** hardware attestation roots. Load the official list (the Key Attestation docs publish it as a JSON array). In 2026 Google added a new ECDSA P-384 root alongside the old RSA one, so trust every root on the list and don't hardcode a single certificate.
- Find the key attestation extension (OID `1.3.6.1.4.1.11129.2.1.17`) in the certificate nearest the root that has it; it isn't always the leaf. Parse it with Bouncy Castle's ASN.1 classes (as Google's attestation sample does) or a maintained library.
- Require:
  - `attestationSecurityLevel` is TrustedEnvironment or StrongBox
  - `attestationApplicationId` lists our package and an allowed signing-certificate SHA-256 (a configurable list covering debug and release)
  - `verifiedBootState` is Verified and `deviceLocked` is true
  - the attested public key equals the `identity_pub` in the record
- Following Google's guidance, never trust a device's verdict on its own attestation: the peer phone and the CLI do the checking.
- Revocation: the CLI can optionally fetch Google's attestation status list. The app skips it (offline).

## Milestones

- **M0: scaffold.** Modules, Gradle setup, `PROGRESS.md`, fakes. `./gradlew test` passes.
- **M1: raw UWB.** Nearby Connections discovery and connection; exchange UWB parameters; range with provisioned STS using a **hardcoded test key** (clearly marked as such). Debug screen: UWB capabilities (log the supported config IDs), role, live distance, update rate. Then stop: the user measures at 0, 10, 30, 100 and 300 cm and sends logs.
- **M2: handshake and codes.** Identity keys, attestation chains, hello, key schedule, identity proof, code entry. The STS key now comes from HKDF. Unit tests: RFC 5869 HKDF test vectors, code derivation, transcript binding, a tampered message aborts, a wrong code aborts.
- **M3: acceptance and co-signed record.** Summaries, record, signatures, storage, export. Full-flow tests on fakes: pass, too far, flapping distance, peer lying in its summary.
- **M4: verifier CLI.** `verify <bundle> --package <name> --cert-sha256 <hex>... [--roots <file>] [--check-revocation]` prints PASS, or FAIL with every failed check. Test with real bundles from M3 and with tampered copies.
- **M5: optional hardening.**
  - Per-use biometric gating: the identity key requires user authentication for every use (`BiometricPrompt` with a `CryptoObject`), so each owner must be present when signing.
  - Fresh attestation per session: create the ECDH key inside Keystore (`PURPOSE_AGREE_KEY`) with attestation challenge `SHA-256(nonce_A || nonce_B)`. This needs a nonce round before the key round.
  - Experiments to log in `PROGRESS.md`: measured vs. true distance; a hand, pocket or wall in between; two phones given different STS keys (ranging should fail).

## Known limits (show these on an About screen)

- Proves two **devices** were within about half a meter, not who held them. M5 biometrics narrows this.
- Everything rests on attestation; an exploited phone could lie about distances.
- Researchers have shown physical-layer attacks that shrink UWB distances. The consecutive-readings rule is a mitigation, not a guarantee.
- Wall-clock times are self-reported.

## Commands

- Tests: `./gradlew test`
- Install on each phone: `./gradlew :app:installDebug`
- Logs: `adb logcat -s POE`
- Verify a bundle: `./gradlew :verifier-cli:run --args="verify path/to/bundle.json --package <name> --cert-sha256 <hex>"`

## PROGRESS.md

Short dated entries: what changed; what's verified on hardware (phone models and Android versions); open questions; differences between this file and the official docs.
