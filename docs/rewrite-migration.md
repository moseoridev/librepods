# Rewrite migration — active work

## Goal and authorization

The maintainer authorized this migration on 2026-11-05 and explicitly requested persistence across context resets. Work directly, without review agents. Keep this file current at meaningful checkpoints.

Move to upstream `android/rewrite` while retaining our connection lifetime, cancellation, bounded recovery/Nearby, widgets and popup policies; then implement the screenshot-based AirPods settings structure with the already extracted Buds components. Preserve nonroot use and optional privileged paths. Commit coherent verified changes and push only to the maintainer fork. No upstream pushes, releases, CI dispatch or external notifications. No device installation/settings changes are authorized by this code task.

## Pinned branches

- Preserved old implementation: `moody-penguin`, `26e5601a2740d7cb51dcf1e3ef96969a882d7c61`. Recover old files with `git show moody-penguin:path`; do not wholesale cherry-pick its mixed 104-file lifecycle commit.
- New working branch: `rewrite-port`, based on upstream `android/rewrite` `3d4d2703da39e9903e9789484b838bd5a0930207`.
- Fork: `origin` = `git@github.com:moseoridev/librepods.git`. Upstream = `librepods-org/librepods`.
- Old lifecycle/widget documents copied here initially describe the preserved branch, not completed migration. Their claims must be updated as integration lands.

## Completion sequence

- [x] Create separate rewrite branch; preserve old branch.
- [x] Restore contributor guidance, independent settings-ui, source tools and evidence references.
- [x] Keep fork CI manual-only; configure library under rewrite build; establish local FOSS build.
- [x] Port per-device session ownership, cancellable socket connect, ordered bounded writes, EOF/failure teardown and finite recovery. Preserve meaningful old host tests.
- [x] Restore functional ATT reads/writes (rewrite currently returns null/false before its old code); bind work to device session.
- [x] Port CDM ownership/reconciliation, idle shutdown, receiver/media/phone observer lifetimes. Separate Bluetooth audio connectivity from AACP and BLE.
- [x] Port Nearby default-off, PendingIntent LOW_POWER batches, freshness/identity and bounded finite takeover jobs. Preserve rewrite case BLE decoding.
- [ ] Import old preferences/keys/device selection and per-instance widget configuration into rewrite state without overwriting existing Room data.
- [ ] Port own widgets, publication/phone-battery observer policy and host geometry to rewrite device state.
- [ ] Port popup/island lifecycle policies to rewrite overlays. Bound packet history and defer billing initialization.
- [ ] Add settings-ui as presentation dependency and build supported screenshot hierarchy with rewrite state/actions. No invented unsupported controls or vendor artwork.
- [ ] Run affected host tests and FOSS build; separate source/build proof from device/runtime evidence; commit and push to origin/rewrite-port.

## Current state / next action

Preservation scaffold committed/pushed as f15eeb0. settings-ui builds against rewrite Material3 beta01 after adapting SliderState trackRange/callback/topToBottom APIs. Historical rendering evidence remains historical, not upgraded-Compose parity.

Per-device AACP/ATT and CDM/runtime code are integrated. The local FOSS APK and core/app host tests pass (core16: session12, recovery3, ATT response1; app9: stale-query4, multi-association presence5). MainActivity binds only while STARTED; no default foreground start; media/phone/sound resources run only for connected/connecting devices. ATT uses serialized finite child transactions, closes late sockets, and UI polling follows screen lifecycle/reconnect. User association setup is available in app settings. Packet diagnostics retain at most64 packets per device. No Android binding/Bluetooth/power/device behavior is demonstrated by these host tests.

Core/CDM checkpoint committed/pushed as5a24cd0. Nearby is now integrated: scanner/service-independent NearbyDecoder preserves old RPA/freshness/replay protection and rewrite case TLV/AES battery decoding; explicit default-off preference, system LOW_POWER/5s PendingIntent delivery, finite expiry, per-device budgets with global60s admission/actual-attempt cooldowns, raw-observation/boot/key revalidation and10s JobService handoff are implemented. The old always-on callback BLEManager is removed. Do not restore it.

Local `:core:testDebugUnitTest :app:testFossDebugUnitTest :app:assembleFossDebug` passes after the final Nearby changes: core26 (session12, decoder7, recovery3, finite-owner3, ATT response1) + app12 (budget3, stale-query4, presence5), total38. Log `/tmp/librepods-rewrite-nearby.log`. This proves host contracts and compilation, not PendingIntent delivery, CDM binding, real Bluetooth, battery savings or OEM behavior. Private case advertisement matching retains the upstream heuristic; do not describe it as authenticated encryption.

Next: commit/push this checkpoint, implement legacy preferences/keys/settings import without overwriting existing Room rows, port own widgets/configuration to rewrite state, then popup policies and actual screenshot UI. No physical device operations performed.

## Resume constraints and references

- Read AGENTS.md and docs/android-development.md for tools, JDK/SDK, FOSS-only checks and Git policy. Rewrite Gradle daemon requests JDK25; Java target stays21.
- UI: docs/android-settings.md and docs/android-settings-components.md; original20 screenshots are at `/Users/moseoridev/programming/android/librepodsDesignReference/ios-airpods/2026-09-21/`. Existing screen differences are expected future implementation, not extraction defects. Contact/log/purchase/QS are outside screenshot UI scope.
- settings-ui has no product transport imports; retain tests and source-backed appearance. Historical pixel evidence does not automatically certify upgraded Compose.
- Do not repeat optional240dp stress or cold-shadow investigations. No emulator recreation. No root/kernel/manager changes.
- Rewrite has no host tests at baseline; latest CI cancelled, prior alpha4 succeeded. EOF loop has open upstream PR875; fix ownership rather than copying an unverified claim.
- Scope is implementation, not another broad documentation audit. Record blockers and next concrete action here; do not mark the overall goal complete while required phases remain.
