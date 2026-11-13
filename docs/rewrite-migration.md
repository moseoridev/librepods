# Rewrite integration checkpoint

## Integration line

- Main checkout: `/Users/moseoridev/programming/android/librepods/repo`, on `rewrite-port`.
- `rewrite-port` is the integration base for new Paseo work. Local/origin `rewrite-port` and `short-chipmunk` are kept equal after this consolidation; `short-chipmunk` remains checked out in the existing Paseo worktree.
- Source checkpoint before this documentation cleanup: `edca427cbbd81ee5548d35577359d08b354890d1`.
- Upstream rewrite base: `3d4d2703da39e9903e9789484b838bd5a0930207` (`upstream/android/rewrite`). No upstream push, merge or PR was performed.
- `main` / `origin/main` remain untouched at `35cf8dc18215993391d991ad5a5c3f03f98c22a7`. They contain custom work and are not merely a stale upstream mirror. This is a retained historical line, not the new integration base.
- The superseded pre-rewrite `moody-penguin` line is preserved by annotated origin tag `archive/moody-penguin-20261113`, targeting `26e5601a2740d7cb51dcf1e3ef96969a882d7c61`. Recover files with `git show archive/moody-penguin-20261113:path`; the branch is retired.

The main checkout's uncommitted removal of the Korean-only instruction was superseded by `71b8dc3`; that redundant edit was discarded before fast-forwarding. Settings components/tools were preserved by `f15eeb0`, with the two slider differences being Compose API adaptation. Lifecycle and widgets were ported by `5a24cd0` and `a709df0`; legacy keys/settings import is in `3b7c390`. Later commits refine the integrated presentation. The archive is a recovery reference, not a set of commits to merge wholesale.

## Completed migration and current boundaries

The rewrite port retains finite per-device connection ownership/cancellation/recovery, companion presence handling, optional bounded Nearby behavior, legacy settings/address import, own widgets with per-instance device routing, bounded popup/island policies and the separate settings-ui presentation layer. The supported iOS information structure uses upstream state and command paths; screenshots do not establish Apple service or firmware support.

Current behavior and evidence belong in [settings](android-settings.md), [component provenance](android-settings-components.md), [lifecycle](android-background-lifecycle.md) and [widgets](android-widgets.md). Keep the migration note small instead of duplicating their investigation chronology. Open presentation limits include the context-dependent master-switch edge residual, cold-transition timing, unpaired native rename IME and unavailable taskbar/split-window/other-OEM/theme configurations.

## Local data and verification

LibrePods data is under `/Users/moseoridev/programming/android/librepods`; see the [development guide](android-development.md#reverse-engineering-and-local-evidence) and local `evidence/README.md`. Current1724 inputs, final captures/summaries, source tools and required restore APKs are retained.1704-era capture matrices, intermediate/rejected binaries and raw dumps whose conclusions are recorded were retired; old migration integrity listings are no longer operational inputs. Shared ASC stays outside this tree and was not modified.

The last parity-run baseline readback is retained with the evidence:1080×2340/480 without overrides, font_scale1.0/global font_size2, night yes, gesture navigation, ko-KR/empty app locales, Bluetooth on and all four original APK hashes. This cleanup performed **no device access**. Device operations, installations and settings changes require an authorized task scope; KernelSU kernel/manager changes remain excluded.

## Git completion

Use `origin` (`git@github.com:moseoridev/librepods.git`) for authorized working-branch pushes. Leave `upstream` untouched. Commit subjects use `type(scope): summary` and the requested co-author trailer. Android CI is manual-only; no dispatch, release, PR or external notification is implied. Do not use bare `git stash`.
