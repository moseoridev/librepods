# KernelSU Probe

Temporary Android helper for authorized root diagnostics on the maintainer's S25. It executes `su -c` from an app UID, which can work when ADB shell has no `su` access. It requires an already working KernelSU setup and an explicit root grant for `dev.moseori.ksuprobe`; it does not install KernelSU or exploit the device.

The source is smali assembled by apktool. It was copied unchanged from [airdropReverseInvestigation at 65ea56b](https://github.com/moseoridev/airdropReverseInvestigation/tree/65ea56bc4f4ca31adf4d2b953d5cdb988211a9f6/tools/ksu_probe). This repository now contains the source, build script and host runner. Generated APKs and the local debug signing key are ignored.

## Build and run

From the LibrePods repository root, use JDK 21+, apktool, adb, and either an SDK `apksigner` or an existing Android build-tools `apksig` jar:

```sh
APKSIGNER=/path/to/android-sdk/build-tools/VERSION/apksigner tools/ksu_probe/build.sh
# Alternative when only the signing library is available:
APKSIG_JAR=/path/to/apksig.jar tools/ksu_probe/build.sh
```

The build writes `dist/KernelSUProbe.apk` and creates a local debug key in `build/` on its first run. A fresh key differs from an already installed helper signed elsewhere: an update will fail with a signature mismatch. To maintain that installation, place the original key at `tools/ksu_probe/build/debug.keystore` before building; replacing the app requires authorization and granting its root permission again. Do not commit the key or APK.

For an authorized installation, select the current phone serial explicitly:

```sh
adb devices -l
adb -s DEVICE_SERIAL install -r tools/ksu_probe/dist/KernelSUProbe.apk
```

In KernelSU, enable **Superuser → KernelSU Probe**. Then run:

```sh
tools/ksu_probe/probe.sh --serial DEVICE_SERIAL --prime --wait-timeout 10 -- \
  'id; getenforce; cat /proc/1/attr/current'
```

The default command is the same identity probe. Success requires `uid=0` and `__KSU_PROBE_RC__=0`. `/proc/1/attr/current` describes PID 1, not the probe. See the [development guide](../../docs/android-development.md#s25-root-access-for-authorized-device-investigation) for the verified firmware, manager/driver UAPI compatibility, and recovery limits.

Both exported activities require `android.permission.DUMP`, so ordinary third-party apps cannot invoke this root command entry point. Treat commands supplied through the host runner as root commands. Diagnostics use logcat tag `KSUProbe`; filter by the helper's current PID and run time. A polling timeout does not stop a running command. Inspect the reported output path before repeating or cleaning up a run. Remove the helper and its specific output files when it is no longer needed, within the task's authorization.
