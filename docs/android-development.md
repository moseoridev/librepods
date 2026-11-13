# Android development

Use this guide for local setup and builds. It describes the maintainer's environment; other machines can use their own SDK location.

## Toolchain and local storage

Use the checked-in Gradle wrapper. The rewrite daemon requests JDK25 (`/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home`); Android bytecode targets Java21. Consult these files for current requirements rather than copying their version values into this guide:

- [App build configuration](../android/app/build.gradle.kts): SDK, NDK, CMake, flavors, and signing configuration.
- [Version catalog](../android/gradle/libs.versions.toml): plugins and dependencies.
- [Gradle wrapper properties](../android/gradle/wrapper/gradle-wrapper.properties): Gradle distribution.

On the maintainer's Mac, the active SDK/NDK/CMake root is the internal `/Users/moseoridev/Library/Android/sdk`. Builds, the comparison fixture, ASC and retained reference evidence run from internal storage without TESSERACT. Gradle caches remain in `~/.gradle`.

Each worktree needs an ignored `android/local.properties` containing:

```properties
sdk.dir=/Users/moseoridev/Library/Android/sdk
```

Preserve other existing properties when setting this value. If necessary, set `JAVA_HOME` for the build command to the Homebrew JDK at `/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home`. Keep local configuration and signing credentials untracked. Installing a newer SDK does not require changing the project's selected versions.

## Build and verify

Choose tasks for the changed behavior; the commands below are options, not a checklist. To produce a FOSS debug APK, from the repository root:

```sh
cd android
./gradlew :app:assembleFossDebug --console=plain --no-daemon --max-workers=2 \
  '-Dorg.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8'
```

The worker and heap limits reduce pressure on the maintainer's laptop; adjust them to the machine when appropriate. The APK is written to `android/app/build/outputs/apk/foss/debug/app-foss-debug.apk`, relative to the repository root. Local debug builds do not need release signing credentials. Building does not install the APK on a device.

The maintainer's default local validation scope is **FOSS only**. Run Play build, compile, or test tasks only when the user explicitly requests them. Use explicit FOSS task names instead of all-flavor aggregate tasks such as `assemble` or `test`.

Choose the validation scope as follows:

| Change or unresolved risk | Useful local check |
| --- | --- |
| Documentation only | Review the diff and relevant references; no Gradle task. |
| A lifecycle helper or its host tests | `:app:testFossDebugUnitTest`, optionally filtered with `--tests 'me.kavishdevar.librepods.bluetooth.ConnectionSessionTest'` (substitute the affected class). No APK packaging is needed for test-only edits. |
| Android service/UI integration or build configuration | `:app:assembleFossDebug`, plus affected host tests when they cover changed behavior. Combine needed tasks in one Gradle invocation. |
| Android API compatibility, permissions, manifest, or resource risks | Review the changed declarations/API guards; use `:app:lintFossDebug` when static analysis can resolve an outstanding risk. This is a broad, slower variant analysis, not an automatic gate after every edit. |

Keep the same worker/heap limits for these commands. Preserve Gradle's incremental outputs; do not use `clean` or `--rerun-tasks` without a cache or stale-output problem. Reuse relevant successful results until a change invalidates them. A failed broad lint run does not justify repeating every build/test after an unrelated fix; investigate the relevant finding and rerun only what can establish the correction. Do not suppress existing findings merely to obtain a green report.

Host tests protect cancellation, stale callbacks, ordered writes, retry limits, and presence-observation cleanup using fake resources and virtual time. They do not execute Android service bindings, broadcasts, real Bluetooth I/O, or power behavior. Add coverage for a concrete failure that the existing tests would miss, not to increase the count or check incidental collection order. Cleanup performed by a test harness does not prove the production caller performs that cleanup. A Gradle task reporting `NO-SOURCE` does not validate behavior.

The existing narrow `UseAppTint` exclusions cover framework RemoteViews and the framework overlay button. Preserve their required `android:tint`; do not replace them with AppCompat attributes or broaden the exclusions just to silence lint. Runtime evidence and open limits belong in the [lifecycle](android-background-lifecycle.md) and [widget](android-widgets.md) notes, not in a mandatory test checklist.

## Physical-device verification and emulator removal

The maintainer removed the `librepods-ui` AVD, its Android 17 Google APIs system image, the Android Emulator SDK package and its auto-start job on 2026-11-04 to recover disk space. **Do not assume this AVD exists or reinstall/start an emulator without a new explicit request.** Preserve the SDK platforms/build tools, source tree and pinned evidence.

Use local JVM/Compose compilation for source checks. For Samsung One UI rendering comparisons, use the already authorized physical Galaxy S25 with an explicit ADB serial and the separate `:settings-ui` reference harness. AOSP emulator renders do not establish Samsung-native pixel parity. When permitted, keep comparisons bounded to independently observed source state, display configuration and card coordinates; retain exact pixel deltas and non-matching states.

The physical S25 may have temporarily loaded KernelSU root. Do not reboot it just for validation. Stop temporary Frida processes and port forwards after investigations, restore any replaced standalone preview APK by hash, and follow the active rewrite migration for product integration; device installation is a separate authorization. The current private capture supervisor is `/Users/moseoridev/programming/android/librepods/evidence/compose-residuals-cold-20261113/safe_run.py`: finally/atexit and SIGINT/SIGTERM cleanup are backed by a separate-session owner-death/stall watchdog. Serial-qualified wait-for-device and restore precede any resumed atomic capture. SIGINT/SIGKILL host contracts and a live SIGKILL/font-setting recovery are retained under the same evidence root. Every run must finish with display overrides reset, original navigation/locale/fonts/night/Bluetooth/APK hashes read back, helpers stopped and forwards removed; a lost transport is a restoration wait, not permission to end with test state.

## Reverse engineering and local evidence

Use the existing ASC wrapper for targeted APK/JAR code queries:

```sh
ASC=/Users/moseoridev/programming/android/tools/ASC/bin/droidasc
"$ASC" --help
"$ASC" getclass "$APK" 'Lfully/qualified/Class;' --threads 4 -o "$OUTPUT"
```

Set `APK` to the specific input and `OUTPUT` to its investigation output path. The [ASC guide](/Users/moseoridev/programming/android/tools/ASC/README.md) owns installation/version details and command examples. `disassemble` returns smali (optionally one `--method`) when decompiled Java is ambiguous, and `findrefs extends|impl` maps class hierarchies without a code scan. ASC's DAD output can contain inferred-type/control-flow errors; follow references and inspect related classes rather than treating generated Java as recompilable truth. Fuzzy or empty reference results do not prove an exact match or absence. Use aapt2 for compiled resources and archive/extraction tools for firmware inputs; do not silently substitute JADX for code decompilation.

Maintainer-local inputs and evidence live under the internal `/Users/moseoridev/programming/android/`. They are optional investigation inputs, not product dependencies or required reading for ordinary edits. Since 2026-11-13 the main checkout is `librepods/repo` and LibrePods data is under `librepods/`; the shared ASC install stays at `tools/ASC`. Archived JSON, logs and scripts keep their original paths; [local_reference_paths.py](/Users/moseoridev/programming/android/librepods/evidence/buds4-20261104/recovery/local_reference_paths.py) resolves both former layouts in memory. The former TESSERACT copies are retained as backups:

| Work | Location under `/Users/moseoridev/programming/android/` |
| --- | --- |
| User-supplied iOS settings source archive, images and OCR | [librepods/designReference/ios-airpods/2026-09-21/README.md](/Users/moseoridev/programming/android/librepods/designReference/ios-airpods/2026-09-21/README.md); read the [checked menu catalog](android-settings.md) first. |
| Input/evidence index, firmware and Samsung investigations | [librepods/samsungBatteryInvestigation/README.md](/Users/moseoridev/programming/android/librepods/samsungBatteryInvestigation/README.md) |
| Product Home frame/deadline attribution, traces, temporary route injection and rejected backdrop candidate | [librepods/evidence/product-home-scroll-20261111](/Users/moseoridev/programming/android/librepods/evidence/product-home-scroll-20261111/); `summary.json`, external capture/analysis scripts, diagnostic APKs/patch; Perfetto Trace Processor v59.1 launcher `/tmp/librepods-trace-processor` downloads to `~/.local/share/perfetto/prebuilts/`. |

The settings comparison harness is [librepods/settingsReferenceHarness](/Users/moseoridev/programming/android/librepods/settingsReferenceHarness/README.md). The current fixture-only Gradle root is `/Users/moseoridev/programming/android/librepods/evidence/buds4-20261104/recovery/fixture-build`; it includes this worktree’s `:settings-ui` and the local harness without configuring `:app`. Historical manifests retain original paths/hashes: map their `/Volumes/TESSERACT/programming/android/` prefix to `/Users/moseoridev/programming/android/` when reading retained inputs, rather than rewriting archived evidence. Migration integrity records are in `/Users/moseoridev/programming/android/librepods/migration-20261104/`.

For device work, select the device explicitly and verify the installed/local APK identity. Device operations still require task authorization.

For the current Android17 S25 reference launcher, private tooling and evidence are in [the recovery README](/Users/moseoridev/programming/android/librepods/evidence/buds4-20261104/recovery/combined/README.md). Frida 17.17 failed Java bridge initialization; stock 17.25.2 still produced ART CMC crashes. The bounded working run uses 17.25.2 with reviewed, unmerged Java bridge [PR410](https://github.com/frida/frida-java-bridge/pull/410) for LinearAlloc method clones, plus code-only [PR407](https://github.com/frida/frida-java-bridge/pull/407) and JVMTI disabled only in Settings. It also avoids heap scans and waits for bootstrap destruction before the native Settings launch. These are experimental investigation tools, not application dependencies or evidence that Android17 alone caused every earlier script failure.

Product artwork comes from upstream LibrePods resources, or is locally authored where no resource exists. Investigation inputs, binaries and captures use the local investigation directories listed above; vendor APKs/artwork are not added to the public repository.

Retain original inputs, extraction/hash manifests, useful tool sources and final evidence. Generated build intermediates and superseded scratch outputs can be regenerated; do not delete the only input or evidence copy as routine cleanup. Update the topic's current evidence summary when a result changes it; use Git history for the narrative of past implementation steps.

## Settings UI without a Bluetooth connection

The debug APK includes `SettingsFixtureActivity`. It renders the **same** `BudsDeviceSettings` product composition with synthetic `AppleUiState` and an in-memory action adapter. The production adapter still forwards to `AppleViewModel`. Fixture actions do not create a device, bind the engine, persist settings or change system media volume. External feature destinations are inert. The fixture classes and activity registration live only in `src/debug`; they are excluded from release builds.

After installing a FOSS debug APK within authorized device work, open a fixture on a selected phone or emulator:

```sh
python3 tools/settings_ui_fixture.py --serial DEVICE_SERIAL --scenario charging --theme dark
python3 tools/settings_ui_fixture.py --serial DEVICE_SERIAL --scenario zero_unknown --width-dp 320 --font-scale 1.5
python3 tools/settings_ui_fixture.py --serial DEVICE_SERIAL --scenario headset
python3 tools/settings_ui_fixture.py --serial DEVICE_SERIAL --page Version
```

Use `--help` for pages, models and scenarios, including `pending` (requests never become acknowledgements), `disconnected` and `long_name`. Width and font overrides affect only this composition, including its layout configuration; they do not change the device's density/font settings. They simulate logical viewports, not different physical devices or OEM font renderers. The host application's ordinary Application initialization still runs; this entry does not instantiate the real connection route.

`BudsManagerFixtureActivity` is a second, separate debug entry: the installed Manager's Home rebuilt from settings-ui, for comparing that library against a native capture. It reads `theme` (`dark`/`light`) and `font_scale` (%f) from the intent and resolves every string, drawable, colour and Lottie mode asset from the installed `com.samsung.accessory.budsunitemgr` package at runtime, so it requires that package to be present and packages none of its content. It is not the product UI and is not a Bluetooth state; it draws one fixed Home.

```sh
adb -s DEVICE_SERIAL shell am start -n me.kavishdevar.librepods/.presentation.screens.apple.BudsManagerFixtureActivity \
  --es theme dark --ef font_scale 1.0
```

When comparing it with a Manager capture, hold the two capture sessions to the same conditions. `screencap` tags a frame with the profile of what it composited, so two frames from different sessions can arrive in different colour spaces and a raw diff then reports the colour transform as layout; compare within one session, or normalise both through their embedded ICC profile. The Manager also follows `settings get global font_size` (the One UI font-size level) on top of `font_scale`; capture both sides at the same level. Both points were each worth hundreds of thousands of false differing pixels before they were corrected.

`DeviceSettingsScreenTest` is the behavioral regression suite. Its14 checks cover battery alignment and zero/missing/headset states; request/acknowledgement separation; actual menu navigation and return paths; coupled call choices; independent long-press bits; disconnected EQ reset; committed Korean/blank/long name editing with the real IME visible, save/dismiss/cancel; media/tone command separation; horizontal/vertical touch, EQ reset and pending values on navigation; scroll-versus-tap behavior; grouped ear-detection command/local-preference updates; and the Head master state/mapping. Name editing uses the source TextEntry dialog, including its separate window/IME insets. These checks use synthetic state/actions, not a fake Bluetooth engine. The system media-volume API is replaced by in-memory caller state.

Build `:app:assembleFossDebug :app:assembleFossDebugAndroidTest` with the options above, then run:

```sh
python3 tools/settings_ui_fixture.py --serial DEVICE_SERIAL --run-tests
```

`DeviceSettingsCaptureTest` is a **separate evidence-generation job**, not a visual regression test. It renders16 pages at320/360/412/600dp × light/dark × font1.0/1.5:256 conditions,512 top/bottom captures and overlapping intermediate scroll frames for long pages. Capture-only checks bound scroll progress; they do not assert visual equality. Screenshots include native dialog/system windows through UiAutomation; the capture window sets status/navigation icon contrast for the selected theme without changing system theme. The system redraws those icons on a later traversal than the `WindowCompat` call, so the job now waits for Compose idle, the instrumentation idle sync and250ms before the first capture of a condition. Without that wait, the first light-theme capture after a dark condition can retain the previous light-on-light icons: reproduced 2026-11-07 on `R3CY406Y74P` (three dark→light transitions in one run), fixed by the wait (eight of eight light frames correct), and the product `MainActivity` path was verified correct throughout. Treat such a frame as a capture artifact, not product rendering. Name-dialog transitions wait400ms for the platform window animation, which Compose idle does not track. Inspect those captures against the retained iOS hierarchy and Buds source-role evidence; a successful render/capture does not establish correct pixels or absence of all clipping.

```sh
python3 tools/settings_ui_fixture.py --serial DEVICE_SERIAL --run-tests --capture-matrix
# After a page-specific visual change:
python3 tools/settings_ui_fixture.py --serial DEVICE_SERIAL --run-tests --capture-matrix --capture-page Name
```

Use `--test-method METHOD` to rerun one behavior while resolving a failure; it is mutually exclusive with `--capture-matrix`. The helper updates both APKs with `adb install -r -t`, invokes the selected instrumentation class directly and pulls captures to `android/app/build/outputs/settings-fixtures/` (`--output` overrides it). Both apps/data stay installed. Test windows hold `FLAG_KEEP_SCREEN_ON` only for their lifetime; no timeout, density or font setting is changed. The phone must be unlocked before starting. Do not use Gradle's `connectedFossDebugAndroidTest` on the maintainer's configured phone: this AGP runner uninstalls the target app after testing. Its first run removed the main app/local data on2026-11-06; the direct runner avoids that cleanup. Device execution requires authorized scope. UI results do not establish firmware command acceptance, connection lifetime, power savings or universal pixel parity.

## S25 root access for authorized device investigation

The maintainer's Galaxy S25 (`SM-S931N`) can run root commands through the temporary `dev.moseori.ksuprobe` app. This is a diagnostic path for an already loaded KernelSU environment, not a rooting procedure or a LibrePods runtime dependency. A failed `adb shell su` does not establish whether this app can obtain root.

The helper source, build instructions and host runner are included in [`tools/ksu_probe`](../tools/ksu_probe/README.md). Use helper version code 10 or later for `KSUProbe` exception, stderr and exit-code logging. Build with its `build.sh` using an existing SDK `apksigner` (`APKSIGNER`) or existing `apksig` jar (`APKSIG_JAR`); install its generated `dist/KernelSUProbe.apk` only within authorized device work. The APK and signing key are ignored local outputs. A fresh checkout generates a different signing key unless the existing local key is supplied; consult the helper README before replacing an installed helper.

1. Select the phone explicitly with `adb devices -l` and record its model, firmware and installed KernelSU/helper versions. The wireless ADB address can change.
2. Check KernelSU's driver and manager **UAPI** versions, shown after the hyphen in their version labels. On the verified setup, driver `32601-2` worked with official manager `v3.2.5 (32525-2)`. Manager `v3.3.1 (32680-5)` showed “kernel update required” and hid the superuser menus because UAPI 5 differs from 2. Matching UAPI restores compatibility; it does not itself prove root access. See the official [menu condition](https://github.com/tiann/KernelSU/blob/v3.3.1/manager/app/src/main/java/me/weishu/kernelsu/Natives.kt) and [3.2.5 release](https://github.com/tiann/KernelSU/releases/tag/v3.2.5).
3. If a manager replacement is necessary, verify the official APK's release hash and signing certificate against the installed APK. Data-preserving `adb install -r -d` was rejected with `INSTALL_FAILED_VERSION_DOWNGRADE` on this phone. Removing the manager first can reset its app data/settings and requires explicit authorization; do not automate that removal or update the kernel as part of a root check. The verified replacement APK SHA-256 was `1417081413bf7ab1de8e440ecbcb62685037c8f28f048f0f8b79e305b31ab916`.
4. In KernelSU's **Superuser** tab, select **KernelSU Probe** (`dev.moseori.ksuprobe`) and enable its superuser permission. Have the maintainer operate the UI when needed.
5. Run the read-only identity probe from the LibrePods repository root, substituting the current serial:

   ```sh
   tools/ksu_probe/probe.sh --serial DEVICE_SERIAL --prime --wait-timeout 10 -- \
     'id; getenforce; cat /proc/1/attr/current'
   ```

On 2026-11-04, Android 17 build `S931NKSUCDZIF_OKRCDZIF` returned `uid=0(root) gid=0(root) groups=0(root) context=u:r:ksu:s0`, `Enforcing`, and completion marker `__KSU_PROBE_RC__=0`. The `/proc/1/attr/current` line was `u:r:init:s0`; that is PID 1's context, not the probe's. The helper and its grant were retained for follow-up verification. Recheck after firmware, KernelSU, helper reinstall or reboot; these observations do not establish persistent root across those changes or validate the old UI captures on the new firmware.

A timeout alone is inconclusive. Inspect the helper PID's `adb -s DEVICE_SERIAL logcat -d --pid=PROBE_PID` output for `KSUProbe` errors. Before permission was granted, both helper activities reported `Cannot run program "su": error=2, No such file or directory`. The script wraps each command with a unique output path and completion marker; the helper's additional static `ksu_probe.txt` redirection is not by itself a path mismatch. A timeout can leave a command running; do not blindly repeat mutating commands. Keep any required cleanup limited to the helper and the specific run's files. Root availability does not authorize captures, instrumentation or settings changes beyond the current task.

## CI and remotes

The current checkout uses `origin` for the maintainer's standalone public repository, `moseoridev/librepods`, and `upstream` for `librepods-org/librepods`. Verify the actual remotes and branch tracking when doing Git work; these are environment details, not fixed role names. The maintainer has authorized scoped commits and pushes to a working branch on the personal repository after proportionate verification. This is the default completion workflow and needs no repeated approval. Do not infer an upstream contribution, merge, release, or external notification from that authorization.

[Android CI](../.github/workflows/ci-android.yml) is manual-only (`workflow_dispatch`). Push, pull-request, and reusable-workflow triggers have been removed at the maintainer's request. Do not dispatch CI or restore automatic triggers as part of routine completion; use proportionate local validation, then commit and push. An explicitly requested manual fork run uses `:app:testFossDebugUnitTest :app:assembleFossDebug` and uploads a debug APK, without release signing secrets, Play compilation, or full lint.

The retained upstream signing/artifact steps are restricted to `librepods-org/librepods`. The release and Discord job also requires an upstream push, which this workflow no longer listens for. Recheck these guards if the workflow or remotes change. After pushing, verify the remote branch matches the intended commit; no Android CI run is expected for a normal push on this branch.
