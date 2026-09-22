# Android background lifecycle

The branch implements event-driven companion lifetime and idle shutdown. The maintainer's target is an unrooted Galaxy S26+ / One UI 9 with AirPods 5; Nearby detection normally stays off. Short target-device trials cover recovery and teardown. Long-term reliability, power savings and jank are not established.

This document describes current ownership and validation limits. Widget rendering and phone-battery refresh are in [widget notes](android-widgets.md); tool locations and evidence retention are in the [development guide](android-development.md#reverse-engineering-and-local-evidence).

## Connection ownership

Paths here are relative to `android/app/src/main/java/me/kavishdevar/librepods/`.

| Owner | Responsibility |
| --- | --- |
| `bluetooth/ConnectionSession.kt` | In-flight/connected sockets, cancellable work and bounded ordered AACP writes. |
| `services/AirPodsService.kt` | Bound engine; serialized session transitions and packet delivery on main, blocking socket I/O on workers. |
| `bluetooth/CompanionConnection.kt` | Actual connection queries, query generations and presence-observation registration. |
| `services/AirPodsCompanionService.kt` | Separate CDM binding; holds the engine while the associated device is connected. |
| `bluetooth/NearbyDetection.kt` | Optional system scan registration and admission of finite takeover jobs, independent of the engine. |

Bluetooth audio/ACL connectivity, AACP session state, BLE proximity and playback are distinct. AACP failure does not itself mean the audio link disconnected. Duplicate events cannot create duplicate attempts or accept obsolete completions. Closing owned sockets interrupts blocked I/O; coroutine cancellation alone is insufficient. The AACP send result means acceptance into the queue; overflow or asynchronous write failure ends that session.

Session work includes initialization/takeover delays and ATT sockets. ATT gets a new manager/socket per connection and the UI reattaches its observer. Session cleanup is repeatable across timeout, classic disconnect, Bluetooth shutdown, explicit app disconnect, EOF, I/O failure and service destruction. It clears local head tracking, notification and pending work. Service destruction also clears the singleton and cancels its scope.

The engine uses `START_NOT_STICKY`. Media/phone observers and runtime receivers run for a connection or a finite eligible Nearby attempt. Disconnection releases them and gesture feedback. UI and quick-settings bindings can access an idle engine without starting detection; UI subscriptions detach on stop. A cached process is not evidence of ongoing work. Default connection handling starts neither a foreground service nor an app BLE scanner.

Automatic session recovery allows at most three retries, delayed by 1, 3 and 10 seconds. Duplicate events do not refill the budget. Explicit app disconnect suppresses automatic recovery until a new classic link or manual retry.

## Companion and platform entry points

App settings requests a companion association for the selected AirPods. Without approval, the UI can connect while open; there is no resident fallback. Android's companion service owns its binding interface, so it remains separate from the engine's UI `LocalBinder`.

Manifest Bluetooth events, boot/unlock, package replacement, app resume and companion callbacks reconcile actual connectivity. Companion-service creation also requests a finite reconciliation: CDM can rebind after process loss without replaying appearance. Presence observation is enabled for the connected association and stopped for disconnected/other associations, including leases left by a previous process. Static inspection of the sampled One UI firmware found system BLE scanning for disconnected associations whose presence observation stays enabled. Ignoring callbacks alone would not stop that scan; transient scans during transitions remain possible.

On API 36.1+, queries use public per-device classic transport state. Older versions first accept an existing connected AACP socket for the same device, then use bounded A2DP/HEADSET probes; a second socket check handles a connection established during probing. This preserves control traffic when optional root features intentionally disconnect audio profiles. After process loss, a control-only link with neither an app-owned socket nor an audio profile is still not reliably discoverable on older APIs. Probes have a four-second asynchronous timeout, but synchronous platform calls depend on Android responding. Generations invalidate old results; explicit classic disconnect and adapter shutdown tear down the session.

Contracts: [Bluetooth sockets](https://developer.android.com/reference/android/bluetooth/BluetoothSocket), [connection broadcast exceptions](https://developer.android.com/develop/background-work/background-tasks/broadcasts/broadcast-exceptions), [CompanionDeviceService](https://developer.android.com/reference/android/companion/CompanionDeviceService). The address-based presence API remains for Android 13–15 compatibility. User force-stop is distinct from system process reclamation.

## Secondary work and popups

The notification updates for connection, battery, name and Reverse/Reconnect changes. Identical visible content is skipped while the notification exists; disconnect clears the cache. Accepted battery packets publish once through the shared battery path. Repeated packets still reach metadata/audio consumers; unchanged widget content is skipped. Phone-battery changes refresh only widgets. No widget views are built when none are installed.

The unused per-packet preference archive and its formatter/state flow are removed. Parsing, packet broadcasts and the separate `LogCollector` Logcat/file-export path remain. Old `packet_logs` preferences are not read, updated or migrated. Play billing initializes only on first UI access; releasing an already-created billing connection remains separate work. These are reductions in code-side work, not measured energy savings.

The bottom popup uses the first usable battery snapshot after physical connection, not BLE lid-open. A per-address marker survives service/process recreation. Retry, EOF and reconciliation failure do not clear it; classic disconnect, Bluetooth shutdown and boot do. A disabled, unpermitted or screen-off/locked request is consumed without deferral. Overlays close on disconnect, destruction and screen-off; their videos play once.

The top connected island instead permits one memory-only, session-owned pending request for thirty seconds while the display is hidden. Screen-on/unlock consumes it only while interactive/unlocked and after rechecking session, preference and permission. Duplicates do not extend the deadline. Session end, teardown, both earbuds removed with ear detection enabled, or a superseding popup clears it; other island types are skipped while hidden. Screen-off removes an existing island and cancels its video, receivers, timeout and animations. The service retains the actual window until removal. Animations stay inside the window's bounds and system insets remain reserved. This adds no wake lock, alarm, polling or process-death persistence.

## Optional Nearby detection

Nearby remains opt-in. Boot/unlock, package replacement, app resume, Bluetooth and preference/key changes reconcile one explicit mutable PendingIntent targeting a non-exported receiver. The same identity stops it; engine teardown does not. Permissions, enabled Bluetooth/Nearby, selected-device IRK and controller batching support are required. Failure does not fall back to per-packet callbacks, foreground services or timed retry loops.

The scan requests LOW_POWER, five-second batches and an Apple proximity manufacturer filter. Own-device RPA verification precedes takeover work, but unrelated matching Apple advertisements can still wake the process. The newest verified observation uses its elapsed-realtime scan timestamp; stale/out-of-order data is rejected and at most sixteen verified addresses are cached. Meaningful state changes exclude timestamps/private-address rotation. AACP takes precedence over the thirty-second BLE snapshot shared with UI/widgets. Reads reject expired values. A cancellable in-process expiry can clear rendered state when scheduled, but does not keep the process alive; frozen/killed processes can leave a widget until its next system update.

Disconnected scan handling does not create the engine. Eligible takeover checks current system media/call state on each own-device batch and queues an interruptible `services/NearbyTakeoverService.kt` job. It carries the raw verified observation, original timestamp, boot identity and key digest, then revalidates selected device/keys, freshness, preferences, phone state and manual-disconnect suppression. A newer observation takes precedence.

A job binds for at most ten seconds. It releases its exact session on cancellation/timeout, preserving any replacement manual session, or hands ownership to CDM after connection. An association is required. Persisted admission and actual-attempt intervals are each at least sixty seconds. At most three actual attempts are allowed per selected device and observed eligibility; rejected/expired queued work does not consume them. Device changes reset the count while retaining global intervals. There is no periodic job or idle media/phone listener. Batching, job delay and missing advertisements can miss short transitions; immediate takeover/disappearance detection is not promised. See [Android background BLE guidance](https://developer.android.com/develop/connectivity/bluetooth/ble/background).

## Evidence and remaining work

Host tests cover session cancellation/ownership, ordered writes, stale callbacks/queries, retry and presence-observation limits, and BLE identity/freshness/replay. They do not exercise Android binding, broadcasts, real sockets or power. Choose checks for the changed behavior using the [development guide](android-development.md#build-and-verify).

The following are short 2026-09-20 trials on SM-S947N, Android 17/API 37, One UI 9; most used USB, while cold-process/toggle checks used wireless ADB without external power. Post-reboot observation reported external power. They are historical observations of the tested APKs, not a fresh certification of each later commit.

| Scenario | Established evidence | Limit |
| --- | --- | --- |
| Closed-case disconnect | Both services absent, presence observation off, no ongoing app scan; One UI active-app dialog omitted LibrePods. Cached-process CPU counters unchanged over the sampled interval. | Does not exclude brief system scans or measure battery use. |
| Process loss while Bluetooth stayed connected | `run-as` process kill followed by CDM/engine/AACP recovery without opening the app, about eleven seconds in this trial. | Simulated abrupt loss, not a measured low-memory kill or force-stop. |
| Screen-off and initially absent process | Connection event restored bindings/AACP/notification without launching the UI; foreground-start counters remained zero. | Limited successful cycles, not long-term reconnect reliability. |
| Bluetooth off/on and reboot/unlock | Services/notification removed on normal Bluetooth disable, restored on enable; real reboot plus user unlock restored connection/notification. | `BLE_ON` remained for system/other clients. Reboot test does not isolate the initiating callback. |
| Notification changes | Duplicate battery values skipped reposting; changed values posted; disconnect removed notification and manual connection restored it. | Name, Reverse/Reconnect, permission/channel recovery and isolated phone-battery receiver transitions remain statically reviewed. |
| Bottom popup / Nearby | Visible connection popup with Nearby off; enabling Nearby registered/delivered batched LOW_POWER results. Registration survived engine teardown and stopped when disabled. | No BLE-only rendering proof, process-loss/screen-off scan trial or automatic takeover/handoff validation. |
| Island screen-off/unlock | Synthetic connected-island request while dozing created no window; unlock within the deadline showed the whole pill and the user confirmed no clipping. Screen-off removed it before normal timeout. | Physical reconnect was not repeated in this test; expiry, stale sessions and gesture cancellation were statically reviewed. |

Automatic Bluetooth connection previously failed before LibrePods could recover. The user reported recovery after resetting AirPods without an app-code change. Apple-device pairing as the cause is an unconfirmed hypothesis.

Association removal, rapid reconnects, repeated lifecycle cycles, legacy root audio-profile transitions, long-term reliability and power/jank remain unverified. The unused diagnostic head-tracking screen fix is deferred. Galaxy Watch integration, including a separate watch app, is excluded by maintainer choice. These are scope/evidence boundaries, not a required suite for unrelated edits.

Detailed earlier chronology is retained in Git (this file at `6b62289`); firmware and recovery captures are indexed with the [local evidence](android-development.md#reverse-engineering-and-local-evidence). Update the relevant row when evidence changes rather than appending a new execution diary.
