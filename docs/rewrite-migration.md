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
- [ ] Port per-device session ownership, cancellable socket connect, ordered bounded writes, EOF/failure teardown and finite recovery. Preserve meaningful old host tests.
- [ ] Restore functional ATT reads/writes (rewrite currently returns null/false before its old code); bind work to device session.
- [ ] Port CDM ownership/reconciliation, idle shutdown, receiver/media/phone observer lifetimes. Separate Bluetooth audio connectivity from AACP and BLE.
- [ ] Port Nearby default-off, PendingIntent LOW_POWER batches, freshness/identity and bounded finite takeover jobs. Preserve rewrite case BLE decoding.
- [ ] Import old preferences/keys/device selection and per-instance widget configuration into rewrite state without overwriting existing Room data.
- [ ] Port own widgets, publication/phone-battery observer policy and host geometry to rewrite device state.
- [ ] Port popup/island lifecycle policies to rewrite overlays. Bound packet history and defer billing initialization.
- [ ] Add settings-ui as presentation dependency and build supported screenshot hierarchy with rewrite state/actions. No invented unsupported controls or vendor artwork.
- [ ] Run affected host tests and FOSS build; separate source/build proof from device/runtime evidence; commit and push to origin/rewrite-port.

## Current state / next action

Preservation scaffold is ready: settings-ui builds against rewrite Material3 beta01 after adapting SliderState trackRange/callback/topToBottom APIs. A local core test + settings-ui assemble + FOSS APK build passed before the subsequent CDM changes. Core host reports contain16 passing tests (session12, recovery3, ATT response1). This is not device proof.

Per-device AACP/ATT lifetime code and CDM/runtime integration are in the worktree and being verified together. Current command: `:core:testDebugUnitTest :app:testFossDebugUnitTest :app:assembleFossDebug`, log `/tmp/librepods-rewrite-runtime-build.log`; tool session94293 (recheck process/log if the handle is gone). Do not restart a still-running build. Next: finish that verification, fix failures, commit the core/runtime checkpoint, then implement association UI and PendingIntent Nearby. No physical device operations performed.

## Resume constraints and references

- Read AGENTS.md and docs/android-development.md for tools, JDK/SDK, FOSS-only checks and Git policy. Rewrite Gradle daemon requests JDK25; Java target stays21.
- UI: docs/android-settings.md and docs/android-settings-components.md; original20 screenshots are at `/Users/moseoridev/programming/android/librepodsDesignReference/ios-airpods/2026-09-21/`. Existing screen differences are expected future implementation, not extraction defects. Contact/log/purchase/QS are outside screenshot UI scope.
- settings-ui has no product transport imports; retain tests and source-backed appearance. Historical pixel evidence does not automatically certify upgraded Compose.
- Do not repeat optional240dp stress or cold-shadow investigations. No emulator recreation. No root/kernel/manager changes.
- Rewrite has no host tests at baseline; latest CI cancelled, prior alpha4 succeeded. EOF loop has open upstream PR875; fix ownership rather than copying an unverified claim.
- Scope is implementation, not another broad documentation audit. Record blockers and next concrete action here; do not mark the overall goal complete while required phases remain.
