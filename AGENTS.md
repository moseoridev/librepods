# LibrePods contributor guidance

## Working principles

Keep Android improvements usable without root while preserving existing optional root features. Respect Android version and flavor compatibility. Treat battery efficiency and UI responsiveness as primary constraints, and make latency or feature tradeoffs explicit.

Carry authorized implementation through integration and proportionate verification. Investigations and design reviews should produce findings unless implementation is requested. Make routine local decisions without repeated approval; ask when missing information materially changes the outcome or the action exceeds the task's authorization. Existing authorization carries forward.

The primary agent performs implementation, investigation and verification directly. Reuse a dedicated reviewer only for the final review after implementation and proportionate verification, immediately before committing. Do not request intermediate design or partial-patch reviews unless the user explicitly asks. The reviewer never implements changes; the primary agent applies review fixes and verifies them before any necessary final re-review.

Follow explicit user instructions over this file and skill guidance. If a skill blocks authorized work, identify the exact instruction. Communicate concise results, evidence, and limitations in Korean unless requested otherwise.

## Read according to the task

- Android setup or builds: [development guide](docs/android-development.md).
- Android connection lifetime, reconnects, or background resource use: [lifecycle notes](docs/android-background-lifecycle.md).
- Widget state, rendering, or Samsung parity: [widget notes](docs/android-widgets.md).
- APK/JAR investigation: use ASC for code decompilation; use resource tools for resources. See [local tools and evidence](docs/android-development.md#reverse-engineering-and-local-evidence).
- Protocol changes: relevant notes in [opcodes](docs/opcodes.md), [control commands](docs/control_commands.md), and [device information](docs/device-info.md), checked against the implementation.
- Installation and feature availability: [Android README](android/README.md) or [Linux README](linux/README.md).
- CI or publication: the relevant workflow under `.github/workflows/` and the actual Git remotes.

Use code and configuration as the source of truth. Keep durable working agreements here, current behavior and limits in the relevant topic document, and environment/tool details in the development guide. Update existing sections when behavior or evidence changes; do not append build timings, review transcripts, or a report for every investigation. Keep historical execution evidence outside the repository and link to its location when needed.

## Git completion

After proportionate verification, finish implementation work with coherent commits and push them to a working branch on the maintainer's fork without asking again, unless the user requests otherwise. Keep unrelated work out of those commits; an investigation alone does not require a commit. Check the actual destination and triggered workflow before pushing; this standing authorization does not include upstream pushes, merges, releases, or external notifications. See the development guide for current remotes and CI behavior.

## Verification and boundaries

Use the validation scope authorized for the current task. Code-only work does not require device access; an attached device alone does not authorize installations, settings changes, or data collection. Publication also follows the task's authorization.

Add or retain tests that protect a meaningful behavioral contract or plausible regression; avoid tests that merely restate implementation or duplicate coverage. Choose the smallest checks that address the change's risks, and stop once the relevant evidence is sufficient. Repeat or broaden checks only for new changes, failures, unresolved risks, or required CI. Fix regressions introduced by the change and report unrelated failures without silently expanding scope. See the development guide for task selection; documentation-only edits need no APK rebuild.

Distinguish static findings, documented behavior, firmware-specific evidence, and runtime measurements. Build success does not establish battery savings or OEM behavior. Separate demonstrated defects from optimization candidates, and report what remains unverified.
