# LibrePods contributor guidance

## Working principles

Keep Android improvements usable without root while preserving existing optional root features. Respect Android version and flavor compatibility. Treat battery efficiency and UI responsiveness as primary constraints, and make latency or feature tradeoffs explicit.

For settings UI work, preserve the referenced iOS AirPods information structure and supported behavior while reproducing the actual Galaxy Buds 4 Manager appearance, not a generic One UI-inspired design. Derive layout, typography, colors and control geometry from the reference implementation; distinguish source-backed matches from unverified adaptations. Keep redesign changes in presentation and reuse upstream state and command paths so upstream updates remain practical. Reference screenshots describe observed UI, not proof of Android feature support; do not package vendor artwork from them.

Infer intent and scope from the request and session context. Treat requests for action as authorization to do the work, and carry implementation through integration and proportionate verification. Investigations should produce concrete findings unless implementation is requested. Make routine, reversible local decisions without repeated approval; existing authorization carries forward. Ask only when missing information materially changes the outcome or the next action exceeds that authorization. Complete independent authorized preparation before asking.

Perform investigation, implementation and verification directly. Delegate to subagents only when the user explicitly requests it. Incorporate corrections and status questions without losing the active task; replace it only when the user cancels it or requests an incompatible objective.

Follow explicit user instructions over this file and skill guidance. If a skill causes a pause, approval request or unfinished work, link the exact skill file, quote the instruction and explain how it applies. Distinguish a written requirement from an inferred restriction; do not invent approval steps for hypothetical risks.

Communicate in Korean unless requested otherwise. Lead with the result, use plain language and concise paragraphs, and report relevant evidence and limitations. Give short progress updates for meaningful findings or changes in direction. Use lists when they make steps or comparisons easier to follow.

## Read according to the task

- Android setup or builds: [development guide](docs/android-development.md).
- Settings structure, iOS screenshot sources, or One UI redesign: [settings reference](docs/android-settings.md).
- Android connection lifetime, reconnects, or background resource use: [lifecycle notes](docs/android-background-lifecycle.md).
- Widget state, rendering, or Samsung parity: [widget notes](docs/android-widgets.md).
- APK/JAR investigation: use ASC for code decompilation; use resource tools for resources. See [local tools and evidence](docs/android-development.md#reverse-engineering-and-local-evidence).
- Protocol changes: relevant notes in [opcodes](docs/opcodes.md), [control commands](docs/control_commands.md), and [device information](docs/device-info.md), checked against the implementation.
- Installation and feature availability: [Android README](android/README.md) or [Linux README](linux/README.md).
- CI or publication: the relevant workflow under `.github/workflows/` and the actual Git remotes.

Use code and configuration as the source of truth. Keep durable working agreements here, current behavior and limits in the relevant topic document, and environment/tool details in the development guide. Update existing sections when behavior or evidence changes; do not append build timings or a report for every investigation. Keep historical execution evidence outside the repository and link to its location when needed.

## Git completion

After proportionate verification, finish implementation work with coherent commits and push them to a working branch on the maintainer's fork without asking again, unless the user requests otherwise. Keep unrelated work out of those commits; an investigation alone does not require a commit. Check the actual destination and triggered workflow before pushing; this standing authorization does not include upstream pushes, merges, releases, or external notifications. See the development guide for current remotes and CI behavior.

## Verification and boundaries

Use the validation scope authorized for the current task. Code-only work does not require device access; an attached device alone does not authorize installations, settings changes, or data collection. Publication also follows the task's authorization.

Choose the smallest checks that address the change's risks and complete them before finishing. Add or retain tests for a meaningful behavioral contract or plausible regression; avoid tests that restate implementation or duplicate coverage. Reversible, low-impact changes do not need new tests that mirror the edit. Once the relevant checks pass, broaden or repeat them only for new changes, failures, unresolved risks or required CI. Fix regressions introduced by the change and report unrelated failures without silently expanding scope. See the development guide for task selection; documentation-only edits need no APK rebuild.

Distinguish static findings, documented behavior, firmware-specific evidence, and runtime measurements. Build success does not establish battery savings or OEM behavior. Separate demonstrated defects from optimization candidates, and report what remains unverified.
