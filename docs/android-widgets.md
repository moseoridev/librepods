# Android widgets

The branch supplies its own battery and controls widgets. It does not inject AirPods into Samsung's battery widget or integrate Galaxy Watch. The inspected Samsung provider/central-reader paths require privileged access; this does not rule out every possible nonroot watch integration. Watch support is excluded by maintainer choice.

## State and publication

Implementation lives under `android/app/src/main/java/me/kavishdevar/librepods/presentation/widgets/`. `WidgetState` normalizes component batteries and confirmed noise mode; `WidgetPublisher` constructs complete RemoteViews containing both state and click actions. Providers and `WidgetSettingsActivity` share that path. Connection ownership and optional BLE snapshots are described in [lifecycle notes](android-background-lifecycle.md).

- A new AACP session waits for an accepted battery packet. Rejected packet lengths cannot make cached data fresh or trigger popup/audio decisions. Noise mode stays unselected until confirmed.
- Disconnection clears mode selection and disables commands; the header opens the app. Automatic component selection omits unknown values; manually selected unknown/invalid/disconnected values show a dash. Genuine zero stays zero.
- Connection, battery, mode/allowed-mode and relevant setting changes drive publication. The cache includes content, settings, geometry and appearance. New instances, system updates and resize force a complete publication. Missing widgets return early; bitmap/host-metric caches are bounded.
- Phone-battery broadcasts are registered only while connection runtime is active and an installed battery widget requests phone display. Widget additions/deletions and settings reconcile registration. While disconnected, Android's requested thirty-minute widget updates read the phone battery without starting the engine. Every-percent idle refresh is not guaranteed.

Widget state updates and direct mode commands start no scanner, polling, wake lock or engine binding. Header/battery clicks open MainActivity, whose normal UI lifecycle binds the engine and reconciles connection/Nearby settings. Optional Nearby data can expire while the process is frozen; a previously published widget may then persist until its next update.

## Layout and appearance

Both providers support 2×1, 4×1, 2×2 and 4×2. Exact host sizes select geometry; hosts without size lists get portrait/landscape alternatives from min/max bounds. Enlarged configuration previews preserve the original layout type and scale seams and metrics together. Configuration uses explicit Save/Cancel, provider-separated instance preferences and snapshot-based restored-ID migration.

Battery layouts use two, four or eight open-bottom arcs; unused slots remain empty. Components can be selected/reordered. Controls use one mode-cycle button in narrow layouts or linked mode buttons, omitting Off when unavailable. Compact controls omit the case; taller layouts include title/case. Tall layouts measure a wrap-content header with natural font padding, then distribute remaining space through weighted containers; wide tall controls distribute remaining row width between buttons. This preserves Android measurement/rounding instead of estimating absolute text positions. Equal valid earbud levels and charging states combine into one display; charging has a separate icon.

The inspected Buds 4 Manager 9.0.00.1604 home-content path informs geometry and layout behavior. Packaged mode icons now reuse the upstream LibrePods `noise_cancellation`, `transparency` and `adaptive` resources (including its Off mapping); charging reuses `ic_power`. The separator is a locally authored rectangle. Extracted Samsung glyphs and divider images are not packaged. Battery glyphs are locally drawn; `sec` requests the system font without bundling a Samsung font file. Sequential control/battery placement, bounded icon rasterization, natural font padding, logical RTL padding/end masks and the native link seam matter to alignment. Joined masks avoid doubled alpha at intersections. Final button colors are rasterized into cached bitmaps; the first link rectangle uses `DST_OVER`, matching the original. Post-raster tinting is cleared on reapplication because it changes premultiplied edge values. Day/night Icons let the host select appearance without waking the app. Configuration previews preserve these proportions.

Each instance controls background visibility, phone/light/dark color choice and opacity; square widgets have corner choices. Battery opacity is 30/65/100%, controls 50/75/100%. The surface carries alpha once. Day/night colors and arcs are supplied in RemoteViews so the host can choose without starting the app. Battery text/padding can use One UI Home's size-info tables, with bounded caching and a proportional fallback when lookup fails.

## One UI surface ownership

`WidgetSurface` follows the inspected BZI6 launcher contract. On API 37+ in the system user profile, home hosts supplying `semHostType=1` and supported `semWidgetSize` values own the final outline. The transparent outer GradientDrawable supplies the host-default sentinel or original 32/12dp custom corners, while the inner ColorDrawable supplies color/alpha. Local clipping is disabled only for that path; complete publication explicitly restores it for fallbacks. The cache includes ownership.

All non-system profiles conservatively keep local clipping because the launcher skips its outline for Knox/dual-app providers. Other hosts retain the previous mask. The finite settings preview uses the original smooth-corner curve locally and scales it with the preview; static selector artwork remains a fallback.

Samsung metadata and `android.R.id.background` participate in native launcher handling. The inspected host can read drawable alpha/corners without private string tags. Wallpaper blur is produced by the launcher's shared cache, not by this app. Eligibility depends on firmware, host state and settings; it is an undocumented OEM integration, not a public Android guarantee. The app adds no wallpaper capture or blur computation.

## Verification limits

Packaged artwork intentionally differs from Samsung. Layout and surface behavior follow the inspected renderer and launcher contracts; this is not a claim of pixel identity across devices, states or themes.

JVM regressions cover battery/mode normalization, rejected packet freshness and layout selection. Target-device trials covered controls, battery/ANC display, widget dimensions and clipping. Full restored-ID behavior, actual Knox installation, RTL rendering, theme transitions and every size/state combination remain unverified. Build success does not establish Android receiver lifetime, power savings or responsiveness. Power/jank and long-term OEM behavior remain unmeasured.
