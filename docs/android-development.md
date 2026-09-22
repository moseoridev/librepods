# Android development

Use this guide for local setup and builds. It describes the maintainer's environment; other machines can use their own SDK location.

## Toolchain and local storage

Use JDK 21 and the checked-in Gradle wrapper. Consult these files for current requirements rather than copying their version values into this guide:

- [App build configuration](../android/app/build.gradle.kts): SDK, NDK, CMake, flavors, and signing configuration.
- [Version catalog](../android/gradle/libs.versions.toml): plugins and dependencies.
- [Gradle wrapper properties](../android/gradle/wrapper/gradle-wrapper.properties): Gradle distribution.

On the maintainer's Mac, SDK/NDK/CMake are installed at `/Volumes/TESSERACT/Android/sdk`, on an external SSD. Check that the volume is mounted before using or installing tools there. Do not recreate this directory on the internal disk if the drive is absent. Gradle caches remain in `~/.gradle`.

Each worktree needs an ignored `android/local.properties` containing:

```properties
sdk.dir=/Volumes/TESSERACT/Android/sdk
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

## Emulator

Use the external Android 17 (API 37.0) Google APIs ARM64 AVD `librepods-ui` for ordinary layout iteration. It has two CPU cores and 2 GiB RAM. Finish Gradle builds before starting it on the 16 GiB laptop. Start it with:

```sh
ANDROID_AVD_HOME=/Volumes/TESSERACT/Android/avd \
  /Volumes/TESSERACT/Android/sdk/emulator/emulator @librepods-ui \
  -port 5580 -no-window -no-audio -no-boot-anim -no-snapshot \
  -gpu swiftshader -memory 2048 -cores 2
```

Wait for `adb -s emulator-5580 shell getprop sys.boot_completed` to return `1`. Inspect the visible result, not just Activity status; an error dialog can cover a resumed Activity. Stop the emulator when idle with `adb -s emulator-5580 emu kill`. AOSP does not reproduce Samsung fonts/framework behavior or phone performance. Galaxy access is for bounded OEM and real Bluetooth checks, not routine layout iteration.

## Reverse engineering and local evidence

Use the existing ASC wrapper for targeted APK/JAR code queries:

```sh
ASC=/Volumes/TESSERACT/programming/android/tools/ASC/bin/droidasc
"$ASC" --help
"$ASC" getclass "$APK" 'Lfully/qualified/Class;' --threads 4 -o "$OUTPUT"
```

Set `APK` to the specific input and `OUTPUT` to its investigation output path. The [ASC guide](/Volumes/TESSERACT/programming/android/tools/ASC/README.md) owns installation/version details and command examples. ASC's DAD output can contain inferred-type/control-flow errors; follow references and inspect related classes rather than treating generated Java as recompilable truth. Fuzzy or empty reference results do not prove an exact match or absence. Use aapt2 for compiled resources and archive/extraction tools for firmware inputs; do not silently substitute JADX for code decompilation.

Maintainer-local inputs and evidence live on TESSERACT. They are optional investigation tools, not build dependencies or required reading for ordinary edits:

| Work | Location under `/Volumes/TESSERACT/programming/android/` |
| --- | --- |
| User-supplied iOS settings source archive, images and OCR | [librepodsDesignReference/ios-airpods/2026-09-21/README.md](/Volumes/TESSERACT/programming/android/librepodsDesignReference/ios-airpods/2026-09-21/README.md); read the [checked menu catalog](android-settings.md) first. |
| Input/evidence index, firmware and Samsung investigations | [librepodsSamsungBatteryInvestigation/README.md](/Volumes/TESSERACT/programming/android/librepodsSamsungBatteryInvestigation/README.md) |

For device work, select the device explicitly and verify the installed/local APK identity. Device operations still require task authorization.

Do not commit extracted vendor artwork, fonts, APKs, decompiled source or screenshots containing those assets. Reuse upstream LibrePods resources for product artwork; use locally authored primitives where no resource exists. Keep vendor inputs and rendering evidence in the external investigation directories.

Retain original inputs, extraction/hash manifests, useful tool sources and final evidence. Generated build intermediates and superseded scratch outputs can be regenerated; do not delete the only input or evidence copy as routine cleanup. Large APKs, firmware and captures stay outside Git. Update the topic's current evidence summary when a result changes it; use Git history for the narrative of past implementation steps.

## CI and remotes

The current checkout uses `origin` for the maintainer's standalone public repository, `moseoridev/librepods`, and `upstream` for `librepods-org/librepods`. Verify the actual remotes and branch tracking when doing Git work; these are environment details, not fixed role names. The maintainer has authorized scoped commits and pushes to a working branch on the personal repository after proportionate verification. This is the default completion workflow and needs no repeated approval. Do not infer an upstream contribution, merge, release, or external notification from that authorization.

[Android CI](../.github/workflows/ci-android.yml) is manual-only (`workflow_dispatch`). Push, pull-request, and reusable-workflow triggers have been removed at the maintainer's request. Do not dispatch CI or restore automatic triggers as part of routine completion; use proportionate local validation, then commit and push. An explicitly requested manual fork run uses `:app:testFossDebugUnitTest :app:assembleFossDebug` and uploads a debug APK, without release signing secrets, Play compilation, or full lint.

The retained upstream signing/artifact steps are restricted to `librepods-org/librepods`. The release and Discord job also requires an upstream push, which this workflow no longer listens for. Recheck these guards if the workflow or remotes change. After pushing, verify the remote branch matches the intended commit; no Android CI run is expected for a normal push on this branch.
