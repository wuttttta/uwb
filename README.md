# uwb: Proof of Encounter

An Android test app where two phones produce a **proof that they were physically close at the same moment**, which a third party can verify later without trusting either phone's owner.

- **Proximity** comes from UWB secure ranging (time of flight, protected by a per-session scrambled timestamp sequence).
- **The proof** is a record co-signed by both phones' hardware-backed keys.
- **Trust in the phones** comes from Android Key Attestation.

It also includes a JVM command-line verifier for proof bundles.

[<img src="https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png" alt="Get it on Obtainium" height="54">](https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/wuttttta/uwb)

## Status

Early prototype, nothing built yet. See `CLAUDE.md` for the design and `PROGRESS.md` (once it exists) for what has been verified on real hardware.

## Requirements

Two UWB-capable Android phones on Android 14 or later (Pixel 6 Pro or later Pro, Pixel Fold, Galaxy S21+/Ultra or later), with Google Play services and UWB switched on. The app works offline and makes no internet requests.

## Known limits

- Proves two **devices** were within about half a meter, not who held them.
- Everything rests on attestation; an exploited phone could lie about distances.
- Physical-layer attacks that shrink UWB distances exist; the consecutive-readings rule is a mitigation, not a guarantee.
- Wall-clock times are self-reported.
