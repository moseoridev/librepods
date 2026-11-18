# AirPods settings reference and UI direction

## Product boundary

Preserve iOS AirPods settings structure and the semantics of supported features while reproducing Galaxy Buds 4 Manager visual design. A generic One UI interpretation is not the target. Product artwork comes from upstream LibrePods resources or locally authored primitives. Existing battery-efficiency, responsiveness and nonroot constraints still apply.

Keep future redesign changes concentrated in presentation. Reuse upstream state, capabilities and command paths; do not copy the connection engine or duplicate its state to implement a theme. Existing `Styled*` component signatures provide useful integration points. Some screens still access preferences or construct protocol commands: isolate those dependencies where needed instead of spreading One UI branches into transport code. An upstream update can still change UI contracts and require integration; a presentation boundary does not guarantee conflict-free merges.

The catalog below describes the target information structure, not a claim that LibrePods implements every listed feature. Check each action against code and platform support before making it interactive. Keep supported behavior when moving rows; absent screenshot evidence is not permission to remove capabilities for other AirPods models. Feature gaps require explicit implementation scope, not decorative controls that pretend to work.

## Screenshot implementation readiness

The product uses the16-page `BudsDeviceSettings` composition and upstream state/command paths. The iOS information structure is retained; Manager comparison is at the matching element level. AirPods artwork, Apple-only rows, three-band EQ, coupled call choices and read-only head mappings have no whole-screen Manager counterpart. Screenshot labels do not prove protocol support. Source screenshot coverage is catalogued below; current component contracts are in the [provenance ledger](android-settings-components.md).

## Current native reference and verification

Reference: **Manager9.0.10.1724**, SHA-256 `e00e1304f2d3b518b754d33d50c12b5b5f14df767890e8b889e9928431da8d91`, Samsung S25/API37. The [private launcher](/Users/moseoridev/programming/android/librepods/nativeCapture/launcher/README.md) supplies bounded synthetic inputs and leaves the vendor renderer unchanged. This is presentation evidence, not a hardware connection test.

| Area | Demonstrated scope | Evidence |
| --- | --- | --- |
| Home and Back | Eight-setting regression: fixture Home app regions, product Back and both fade bands are 0/0. The 420/450 Home references start native Manager at the target density. A live native process transitioned from 480dpi retains 2,578 dark/2,550 light artwork pixels at 420 and 3 delta-1 pixels at 450-light; fresh native references are 0/0, while fixture app pixels are identical across those contexts. This is native Lottie density history, a native artwork rendering effect. The activity-theme integration preserves all eight fixture app frames exactly against the accepted build; product Back and top/bottom fade ROIs remain0/0. | [Current activity-theme regressions](/Users/moseoridev/programming/android/librepods/evidence/compose-residuals-cold-20261113/shadow-dialog-final-20261115/final-results.json), [Landscape regressions](/Users/moseoridev/programming/android/librepods/evidence/compose-residuals-cold-20261113/landscape-parity-20261114/final-results.json), [Results/captures](/Users/moseoridev/programming/android/librepods/evidence/compose-residuals-cold-20261113/font-theme-matrix-20261114/final-results.json), [density controls](/Users/moseoridev/programming/android/librepods/evidence/compose-residuals-cold-20261113/font-theme-matrix-20261114/density-context-control.json) |
| Back cache | Both paths use software VectorPainter caches and filtered hardware draw with final SRC_IN tint. Omitting the invisible stroke restores ALPHA_8 instead of ARGB_8888;72×72 at 480dpi saves15,552 cache bytes. | [Cache metadata](/Users/moseoridev/programming/android/librepods/evidence/compose-residuals-cold-20261113/cache-metadata.json) |
| Master switch | Source `ww.s.a → ww.q.a → s2.z.p` retains the same unshadowed geometry. Manager-resource Home and Head ON/OFF match in all 48 app frame pairs at 1080×2340, 420/480dpi, dark/light and Samsung steps 0/2/5/8 (scales 0.8/1.0/1.3/2.0). General fixes preserve toggle padding on the text column, the 64/56dp toolbar centre and untrimmed Center title line height; debug inputs preserve 0.8 and native Center Lottie alignment. Head bottom fades agree: at 420 only scale 2.0 enables the bottom pass; at 480 all except 0.8 do. Top passes stay enabled. Default Head ON/OFF also remains 0/0 at 1440×3120/560 dark and 720×1280/450 light. Product 420dpi Head master ON/OFF is 0/0 and Left/Right first cards remain y294. Raw full-frame differences are status clock pixels. The earlier shorter product 480dpi 62/6 residual is not a switch defect. Landscape scroll-top Home and Head ON/OFF also match at 2340×1080/480 dark/font1.0 and light/font2.0, 2340×1080 at 320/296dpi dark/font1.0, and 1170×540/240 dark/font1.0. Manager activity `AppTheme.NoActionBar` inherits DeviceDefault lighting through `Platform.V7.AppCompat`, with `Base.V21` alpha tokens. `MainActivity`, `SettingsFixtureActivity` and the Manager-resource host inherit `SettingsWindowTheme` directly or through `Theme.LibrePods`. Home top/end and initial collapsed Head top/scrolled ON/OFF match in all30 app-frame pairs at font1.0/step2: portrait480 dark/light, portrait420 dark, landscape480 dark and1170×540/240 dark. Head420 has no scroll capacity; Home supplies its floating-shadow check. General dialog and base-context styles preserve the old AppCompat/Material geometry, elevation, insets, wrapping, clipping and dim0.6. The base context matters because Samsung caches DeviceDefault before resolving dialogTheme and then rewrites dim. All six dark/light dialog gates have0 unattributed pixels; dark Reset/Contact differences are control-attributed activity shadows, and light dialogs are raw0/0. No renderer setter is shipped. Product 420 Head, Left/Right y294 and eight-setting Home/Back/fade regressions pass. Native Name dim0.65 is intentionally not adopted. Clock pixels are recorded separately. Expanded portrait Head at 100/75/50/25% and collapsed Home/Head ON/OFF are 0/0 across 21 app-frame pairs at 480dpi dark/light and 420dpi dark. Title positions and the hidden 50% midpoint agree. The source 262/198dp travel is retained; opaque screen_background precedes title alpha, and the unpainted host box delegates rounding remainders to its window. The debug Manager-resource host reads AppThemeBase’s color_background override from installed resources. Navigation pixels also match; raw residuals are confined to the system status bar. Name/IME and night-mode scope is recorded in the product/navigation row. | [Header/IME/night checks](/Users/moseoridev/programming/android/librepods/evidence/compose-residuals-cold-20261113/header-ime-night-20261115/README.md), [Activity/dialog matrix and regressions](/Users/moseoridev/programming/android/librepods/evidence/compose-residuals-cold-20261113/shadow-dialog-final-20261115/final-results.json), [source and dialog-context contract](/Users/moseoridev/programming/android/librepods/evidence/compose-residuals-cold-20261113/shadow-dialog-final-20261115/source-provenance.md), [retained font/theme matrix](/Users/moseoridev/programming/android/librepods/evidence/compose-residuals-cold-20261113/font-theme-matrix-20261114/final-results.json), [landscape branches](/Users/moseoridev/programming/android/librepods/evidence/compose-residuals-cold-20261113/landscape-parity-20261114/final-results.json), [fixture size sources](/Users/moseoridev/programming/android/librepods/evidence/compose-residuals-cold-20261113/head-manager-fixture/size-provenance.md) |
| Title inset | Source 28dp no-navigation slot; unsupported extra 8dp removed. Full 64/56dp toolbar centering and the 21sp/28sp Center/None line-height role preserve fractional-density title placement at the real 0.8/default steps. | [Title inset](/Users/moseoridev/programming/android/librepods/evidence/title-runtime-20261112/final-matrix/title-analysis.json), [source and measurements](/Users/moseoridev/programming/android/librepods/evidence/compose-residuals-cold-20261113/font-theme-matrix-20261114/source-provenance.md) |
| Motion | Push450ms cubic(.22,.5,0,1), pop400ms cubic(.22,.25,0,1), density-correct120dp parallax;150ms linear black0↔.18 dim. Direction follows system resources, not app locale alone. Warm paired trajectories are bounded and cold timing remains open. Samsung predictive Back seeks the same close motion, with an opaque outgoing page and the back slot held for the lifetime of that entry. | [Page motion](/Users/moseoridev/programming/android/librepods/evidence/animation-parity-20261112/page-transition-results.json) |
| Switch, sheet, mode strip, dialog | Source switch300ms × remaining travel; mode-line500ms cubic(.33,0,.67,1). Default sheet uses the native Window dismissal order and long-sheet focus handling; seven own contracts passed. Dialog phase includes Window startup. Other modes/fonts/hover and full per-frame RGB parity remain unverified. | [Motion/contract summary](/Users/moseoridev/programming/android/librepods/evidence/animation-parity-20261112/followup-results.json) |
| Product state and navigation | Six state scenarios plus normal16 pages inspected. Gesture/3-button native/fixture Home pairs0 RGB differences. Own RTL and IME states inspected. At 480dpi dark/light, native rename and product Name use the same Samsung keyboard, with matched617px dialog height/position and exact field pixels; the field stays above the IME. The ordinary keyboard toolbar differs by1,048px because native rename disables rich input through private IME options; a diagnostic control using those source options is0/0. Product caller options and dim0.6 remain unchanged. Product Appearance exposes Light/System/Dark. With System, native Home and product Appearance match their own fresh recreations0/0 through dark→light→dark, including the corresponding window-lighting alpha changes. The existing persisted Appearance preference now drives `UiModeManager.setApplicationNightMode` at startup and on distinct changes (API31+, below minSdk33). Compose reads the resulting configuration. Opposite-system Light/Dark composition and Name dialogs match matching-system frames0/0; activity/window resources and lighting follow the choice. System still follows live changes0/0. One preference change causes at most one activity recreation; process restart preserves it. No polling or second preference store is added. Taskbar, split window, other OEMs and vendor themes remain unverified. | [Appearance/platform-night verification](/Users/moseoridev/programming/android/librepods/evidence/compose-residuals-cold-20261113/app-night-mode-20261115/README.md), [Name/IME comparison](/Users/moseoridev/programming/android/librepods/evidence/compose-residuals-cold-20261113/header-ime-night-20261115/README.md), [Product/navigation summary](/Users/moseoridev/programming/android/librepods/evidence/animation-parity-20261112/product-elements-navigation-results.json) |

The backdrop is a500×500 native PNG, not a gradient or Lottie. The maintainer authorized these two rasters; [direct APK/product asset hashes](/Users/moseoridev/programming/android/librepods/evidence/device-baseline/backdrop-provenance.json) establish their identity. Other product artwork and static mode glyphs remain adaptations. No vendor tutorial/preview or unsupported Apple service is added.

## Rendering cost and cold transitions

The adopted full-height nested top/bottom nodes record content once. Bottom is enabled from actual scroll capacity. Inclusive band boundaries, native cubic/dither math and API36 transfer behavior are retained. Current shader code is locally authored. Historical band-sized and rejected renderers have been retired; their results are not current parity claims.

Release-like product Home at `2d9c2d2` was compared with Manager release: FOSS, R8/minification/resource shrinking on, debuggable=false, shell-profileable, same synthetic state and gesture scripts.12 runs/build/gesture were retained. Metric is Flags=0 `FrameCompleted−HandleInputStart`; these different page compositions are not an isolated shader benchmark.

| Gesture | Manager p50/p90/p99 ms | Product p50/p90/p99 ms | Manager/product GPU-cache median MiB |
| --- | ---: | ---: | ---: |
| Continuous3s/500px |15.114/17.198/21.055 |12.086/12.466/12.883 |70.26/71.39 |
| Ten short80px/180ms swipes |14.862/17.574/21.996 |11.661/12.190/12.599 |67.23/68.15 |

The recorded period is about16.66ms; actual deadline budgets vary8.33–41.70ms. Completion overruns are not late-presentation counts. Under the common approximately25ms continuous stratum, product/Manager overruns are0/1285 and1/1190. Dominant FrameTimeline labels are Buffer Stuffing/On-time Present. Product RenderThread mean Drawing/flush/Vulkan finish is8.722/5.402/1.287ms continuous and8.606/5.345/1.267ms short; corresponding Manager means are5.797/3.184/1.403 and5.819/3.483/1.397ms. These nested/wall slices are not additive and do not establish battery savings. [Per-run data](/Users/moseoridev/programming/android/librepods/evidence/release-home-scroll-20261112/analysis/per-run.csv), [timing/deadline summary](/Users/moseoridev/programming/android/librepods/evidence/release-home-scroll-20261112/analysis/timing-analysis.json) and [trace summary](/Users/moseoridev/programming/android/librepods/evidence/release-home-scroll-20261112/analysis/trace-summary.json) preserve the denominators and measured limits.

Cold transitions remain open after a fresh real-device baseline at `b1eed76` (including the later scaffold, toggle, theme and night-mode fixes). [Manager/product traces and source](/Users/moseoridev/programming/android/librepods/evidence/compose-residuals-cold-20261113/manager-cold-20261116/README.md) establish a navigation difference: Manager renders Compose in separate `HomeScreenActivity` / `SubMenuScreenActivity` windows, then WM Shell animates their prepared surfaces. It does not avoid first composition. In the first cold Left capture its main frame costs256.50ms, including103.61ms in `AndroidOwner:onMeasure`; first layout ends263.20ms after input UP, WM Shell starts `playTransition: OPEN` at322.15ms, and the fitted motion starts349.30ms after UP. The expensive destination build precedes the system transition. Source `n00.e.p` starts the destination activity; `SubMenuScreenActivity.onCreate → BaseActivity.w` installs/selects its Compose content. `DefaultTransitionHandler` dispatches window animation on `mAnimExecutor`; `DefaultSurfaceAnimator.AnimationAdapter` samples `ValueAnimator.getCurrentPlayTime()` and applies a SurfaceControl transformation. This is time-based motion on another thread, not a Compose frame-count animation or a per-page delay.

All four native cold Left pushes pay two `AndroidOwner:onMeasure` passes and one `onLayout` before the fitted clock, then no text measurements during motion. Native starts only3–4 app main frames during the450ms fit, while product starts27–28 with25 `AndroidOwner:measureAndLayout` calls. Manager's source `wm.u1 → wm.l1.k → t0.v.a` uses an eager scrolling Column, not a lazy prefetch mechanism. Labels differ between Compose versions, so these counts are owner passes, not normalized intrinsic/subcomposition or per-node counts. On both native Home pops there is no Compose recomposition, owner measurement/layout or text measurement: Home's activity composition survives under the subpage. Product reconstructs Home (30 text measurements before the fitted clock, then22–23 owner measure/layout calls during motion). Manager's first page build is substantially more expensive; lower initial composition cost does not explain its smoother push.

| Current process-cold median, real S25 | Product `b1eed76` | Manager9.0.10.1724 |
| --- | ---: | ---: |
| Left push RMS, four samples |7.24px |3.82px |
| Left push peak main frame, first600ms after UP |30.85ms |256.62ms |
| Left push fitted start after UP |67.90ms |351.15ms |
| Accessibility→Home pop RMS, two samples |5.54px |7.89px |
| Home pop peak main frame, first600ms after UP |35.37ms |38.56ms |
| Home pop fitted start after UP |68.00ms |157.40ms |

Both subjects use physical1080×2340/480, system ar-SA, dark/font1.0/step2, gesture navigation, process-cold first-entry routes and the retained450/400ms fit/input-BOOTTIME alignment. The native Left entry comes from Home; product Left comes from Controls. These equivalent destination entries have different source-page structures and translations, so this is a navigation attribution baseline, not exact content-cost equivalence. Subjects were captured in separate blocks; no alternating before/after candidate trial or improvement is claimed. Native pop RMS is worse in these new samples, so a current pop-motion deficit is not established. Peak frame cost and motion smoothness must be reported separately: Manager's large build precedes its prepared-surface animation and also adds input-to-motion latency.

Compilation is matched at `speed-profile`: Manager reports `bg-dexopt`, product reports `cmdline` after explicit ProfileInstaller installation. Both APKs bundle baseline.prof/.profm (Manager12,822 bytes, product12,226 bytes); package dexopt does not establish whether Manager's installed profile also contains cloud/runtime contributions. Profile contents are not asserted equal. No profile or hot-marking experiment is repeated. The release-like product remains R8/profileable/nondebuggable, with a temporary fixture entry only for measurement.

Settled PSS medians after Left/Home routes are184.37/207.66MiB product and250.40/248.02MiB native. Quiet10s intervals with no recorder or app polling show product0.010/0.0045% of one CPU core and2/2 scheduler wakeups; native0.024/0.0215% and4.5/3 wakeups. Native includes the synthetic-input bridge. These are process scheduler wakeups, not hardware suspend wakeups or a battery-life measurement; cross-app PSS does not isolate the cost of retaining one page. No new retention or idle-work cost is introduced.

The primary cold configuration is now **ko-KR/LTR**, matching the device baseline; ar-SA/RTL is secondary confirmation. The [LTR-primary comparison](/Users/moseoridev/programming/android/librepods/evidence/compose-residuals-cold-20261113/settings-cold-ltr-20261116/summary.json) uses the same unchanged release-like APK and `speed-profile`, four process-cold seven-entry sequences per subject, plus two Home→Accessibility→Home pairs per subject. System configuration confirms `ko-rKR-ldltr`; native and product pages use Korean resources. Secondary RTL uses the four existing product sequences and two additional native Left1→Left2→Right1→Right2 sequences. Both retain the same450/400ms curves, BOOTTIME input alignment and600ms peak-frame window. These are baseline investigations in separate subject/configuration blocks, not an alternating candidate acceptance trial.

| First/later push entry | LTR product RMS / peak | LTR Manager RMS / peak | RTL product RMS / peak | RTL Manager RMS / peak |
| --- | ---: | ---: | ---: | ---: |
| Left first |7.16px /28.24ms |5.54px /254.99ms |8.37px /28.43ms |3.56px /257.89ms |
| Left second |6.56px /24.97ms |4.46px /217.45ms |4.54px /23.26ms |3.65px /224.94ms |
| Right first after Left |4.36px /23.84ms |3.57px /210.41ms |4.68px /25.45ms |4.41px /214.19ms |
| Right second |4.68px /24.64ms |4.55px /199.54ms |5.68px /26.71ms |4.16px /207.06ms |
| Head first |4.57px /22.02ms |6.96px /161.77ms |5.35px /21.96ms |— |
| Head second |4.96px /21.72ms |4.84px /125.43ms |5.48px /20.77ms |— |
| Left third |5.93px /22.18ms |3.73px /183.97ms |5.12px /24.91ms |— |

LTR columns use four samples per entry; RTL native is a two-process confirmation. The first-Left product-minus-native RMS gap is1.62px LTR versus4.81px RTL, with overlapping LTR sample ranges (product4.90–8.47, native4.54–8.00px). Left first→second improves only0.60px LTR versus3.82px RTL in product. Thus the inherited RTL residual exaggerates the primary cold-only gap; it is not a universal cold-navigation deficit. Both configurations still show recurring product peaks above20ms, including revisits. LTR Head first is smoother than native, and second Right is nearly equal. Configuration changes both language/text and direction; these captures do not isolate direction as the cause.

Home-pop medians are7.00px/35.43ms product versus9.61px/38.39ms native in LTR, and5.78px/35.37ms versus7.44px/38.56ms in the same-analysis RTL references (two samples each). Home→Accessibility push is1.68px/15.10ms product versus3.32px/176.21ms native in LTR. No Home-retention rationale is established. Native LTR first Left still pays97.55ms owner measurement and21.28ms layout, ending260.38ms after UP before Shell `playTransition: OPEN` at320.51ms; fitted motion begins347.80ms. Prepared-surface isolation is confirmed in LTR, despite the narrower motion gap.

The LTR native Home gradient creates a stationary false edge before push and after pop. The raw inherited edge fitter is retained but invalid on those affected movies. The common offline analysis now rejects edges already present in the last pre-UP frame and chooses the monotone edge path with the greatest spatial span (sample count breaks ties), independently of the fitted easing; same-codec outgoing references are used for pop. Curves, time/offset windows and fit are unchanged. Applied to all subjects/configurations, this leaves every product-sequence push result unchanged in both LTR and RTL. All source and filtered tracks are retained, with rejection counts; these analysis corrections are not product fixes. Historical Home-pop numbers above use this common analysis and therefore differ slightly from earlier unfiltered summaries.

No performance change is kept after the [recurring-layout investigation](/Users/moseoridev/programming/android/librepods/evidence/compose-residuals-cold-20261113/recurring-layout-20261116/README.md). Shared warm-up is partial, so idle prewarm is skipped. Owning/forking the navigation host and retaining Home remain rejected. Two general fade-work reductions fail alternating LTR acceptance; all product and diagnostic source changes are reverted. No startup/idle/background or retention change ships. The earlier RTL-only repeated-entry check follows as secondary evidence; current component attribution and candidate results follow it.

Owning/forking the navigation host and retaining Home are rejected for maintenance and RAM cost. The current pop-motion measurements do not justify retention. The [same-process repeated-entry check](/Users/moseoridev/programming/android/librepods/evidence/compose-residuals-cold-20261113/settings-warmup-20261116/summary.json) tests the shared-warm-up hypothesis without changing app code: four fresh `speed-profile` processes each run Left1→Left2→Right1→Right2→Head1→Head2→Left3, returning to Controls between entries, with PID continuity asserted. All28 push fits use the unchanged450ms fitter and all peak windows remain600ms after UP.

| Entry in each process, four real-device samples | Push median RMS | Peak main-frame median |
| --- | ---: | ---: |
| Left, first |8.37px |28.43ms |
| Left, second |4.54px |23.26ms |
| Right, first after Left |4.68px |25.45ms |
| Right, second |5.68px |26.71ms |
| Head, first after four earlier entries |5.35px |21.96ms |
| Head, second |5.48px |20.77ms |
| Left, third after six earlier entries |5.12px |24.91ms |

There is a shared cold contribution, but later entries are not consistently smooth. Right reuses Left's same `LongPressPage` code yet still costs25.45ms on its first visit; final Left still costs24.91ms. Following the conditional investigation rule, no idle prewarm is implemented. The measurements do not establish that moving first-use work to idle would remove the dominant recurring cost; no prewarm benefit, startup/background-path safety, retained-PSS or idle-cost improvement is claimed.

The current real-device component profile uses two warm ko-KR Left/Head sequences with compiler composition markers and explicit group/mode/fade sections. Marker-build times are attribution only, and inclusive slices overlap. Warm Left second-push heavy frames are31.99/33.05ms: row composition3.72/6.96ms, Scaffold body subcomposition7.38/12.56ms, group measure0.99/0.97ms, fade composition2.01/2.57ms and text measure1.24/1.38ms (13 nodes). Head second-push heavy frames are31.14/25.97ms: rows4.13/2.32ms, body8.65/5.25ms, group0.52/0.50ms, fade2.54/1.83ms and text0.97/0.96ms (7 nodes). Each group child and text node is measured once; no redundant full-body measure is demonstrated. Left/Head contain no mode strip. Home return measures its mode strip once (2.99ms), with1.55ms mode composition. ModeStrip's BoxWithConstraints/ConstraintLayout preserves intentional fractional solver geometry; no unnecessary replacement is established. These pages have no explicit IntrinsicSize; internal text intrinsic work is not separately proven absent. RememberUpdatedState unions0.22–0.35ms on these pushes are not dominant.

Fade feedback is demonstrated: one Left capture constructs the zero-height layer at UP+39.20ms (1.75ms), writes height at+48.05ms, constructs the real-height layer at+56.28ms (0.75ms), then first draws it at+57.78ms. A layout-aware modifier-node candidate removes snapshot geometry feedback and supplies the layer before the first body draw. It reduces third-frame composition but moves filtered-layer allocation/raster work earlier: candidate third frames17.61–27.75ms versus baseline2.55–3.34ms, including16.18–25.54ms main-thread postAndWait overlapping15.20–24.86ms RenderThread flush layers. One sample includes5.19ms Vulkan memory allocation. Baseline pays its16.55–20.89ms flush one frame later. This is measured render-thread back-pressure, not evidence that settled card geometry changed. The earlier rejected zero-height-constructor skip preserved the unfiltered first draw, so a late fade alone does not explain its older RMS result.

Both new candidates use uninstrumented R8/profileable/nondebuggable FOSS APKs, explicit ProfileInstaller plus speed-profile, fresh processes and alternating ABBA/ABBA order. Each has four first/second Left push/pop captures and two fresh Home/Accessibility pairs per build, with the same fit and peak windows as above. Each candidate has its own contemporaneous baseline:

| Rejected general reduction | First push RMS / peak, before→candidate | Second push RMS / peak | First pop RMS / peak | Home pop RMS / peak |
| --- | --- | --- | --- | --- |
| Layout-aware fade node; no geometry snapshot feedback |8.50→15.77px /28.95→28.57ms |6.56→4.26px /23.49→23.23ms |5.61→7.52px /20.42→21.10ms |6.85→4.71px /28.71→31.62ms |
| Omit bottom shader/node when its pass is disabled; preserve existing timing |9.00→8.15px /25.67→27.93ms |5.49→6.22px /23.27→21.85ms |5.05→5.92px /22.49→18.12ms |5.02→9.51px /32.45→34.89ms |

Node-candidate Controls/Home startup is118.5→126.5/145.5→177ms and settled PSS183.92→183.43/208.63→208.05MiB. Omitted-bottom startup is122→116/154.5→166ms and PSS184.22→188.57/208.54→208.56MiB. Quiet CPU remains0.0085–0.0124% of one core for the node trial and0.0091–0.0124% for omitted-bottom; all median scheduler wakeups are2 per10s. Full per-route medians/samples are retained. Neither qualifies: motion and/or peak/startup regress. No code remains; RTL candidate confirmation and full pixel gates were therefore not run, and no new pixel or background-launch claim is made. Final [baseline readback](/Users/moseoridev/programming/android/librepods/evidence/compose-residuals-cold-20261113/recurring-layout-20261116/trial-lazy-ltr/safe-final-restore.json) has errors[], original four APK hashes,1080×2340/480 without overrides, ko-KR/empty app locales, font1/step2, night yes, nav2, Bluetooth1, rotation0/auto0, no per-app night override and no forwards.

All28 return-to-Controls pops were also captured. The existing PNG reference is two grey levels brighter than encoded Head video (58 versus56), invalidating its ±1.1 edge gate on Head pops. A retained supplementary analysis uses the corresponding push movie's final decoded frame as the pop reference; curves, windows and fitter are unchanged, all56 fits are then valid, and all push results are identical. Pop medians by entry and both analyses are retained in the evidence. No Home-pop or Manager comparison was repeated in this check. The older [trace-only composable attribution](/Users/moseoridev/programming/android/librepods/evidence/compose-residuals-cold-20261113/cold-attribution-20261113/attribution-r5/attribution.json) remains historical evidence, with compiler-marker overhead, rather than the current baseline.

[Controlled process-cold comparisons](/Users/moseoridev/programming/android/librepods/evidence/compose-residuals-cold-20261113/cold-final-summary.json) use identical DEX/resources for the profile candidates, explicit ProfileInstaller installation and `speed-profile` compilation, alternating builds, system Arabic, fixed450/400ms source-duration fits and input/BOOTTIME alignment. Each push median uses four captures; each Home-pop median uses two. PSS/startup are sampled after the same routes. Only valid trace windows contribute to the recorded CPU summaries; those older rejected APKs and raw traces have been retired.

| Local candidate (rejected) | RTL push median RMS, before→candidate | Accessibility→Home pop median RMS, before→candidate | Controls cold startup median, before→candidate | Home settled PSS median, before→candidate |
| --- | ---: | ---: | ---: | ---: |
| Route-generated profile |8.29→8.48px |8.19→7.12px |122→125ms |202.92→203.46MiB |
| Observed route methods explicitly marked hot |7.39→7.58px |7.38→8.70px |125→121ms |203.85→203.19MiB |
| Remembered draw-phase fade cache |7.42→16.22px |10.61→8.77px |117.5→126ms |202.80→204.12MiB |
| Skip unused zero-height fade construction |7.00→8.61px |6.52→7.37px |127→126.5ms |206.30→205.91MiB |

The first three experiments above use12,176,506-byte APKs (ZIP alignment absorbs small profile/code differences). Their profile assets increase by135/221 bytes; the draw-cache candidate changes DEX by−140 bytes. The route profile worsens Home-pop peak main-frame median25.09→37.55ms, explicit hot marking does not improve push motion, and a stable draw cache worsens cold push. An initial unremembered cache was worse too and is discarded.

The attribution-led [zero-height comparison](/Users/moseoridev/programming/android/librepods/evidence/compose-residuals-cold-20261113/cold-attribution-20261113/no-zero-comparison/comparison-summary.json) uses uninstrumented builds, process-cold starts, ABBA/ABBA build order, four Left pushes and two Home pops per build, the same input/BOOTTIME alignment and450/400ms fitter, ProfileInstaller and `speed-profile`. It skips only an object never drawn; positive-height construction and both draw branches are unchanged. Push peak main-frame median improves32.12→26.60ms and Home-pop peak34.73→31.77ms, but push RMS worsens7.00→8.61px (individual runs6.81/5.90/8.67/7.19 versus8.31/10.01/7.93/8.91), so it is rejected. Controls/Home startup medians are127→126.5ms and160.5→154.5ms; settled PSS182.53→182.30MiB and206.30→205.91MiB. Both APKs are12,176,598 bytes, candidate DEX adds8 bytes, and profile assets are unchanged. Lower CPU frame cost alone does not establish a smoother fixed-duration trajectory.

No performance code, profile or build configuration change is retained. No rendering change is made, so no new pixel-regression run is claimed. The [latest restoration/readback](/Users/moseoridev/programming/android/librepods/evidence/compose-residuals-cold-20261113/settings-cold-ltr-20261116/native-rtl-secondary/safe-final-restore.json) confirms1080×2340/480 without overrides, font1.0/font-size2, night yes, gesture navigation, ko-KR/empty app locales, Bluetooth on, all four original APK hashes, no forwards, rotation0 with auto-rotate off, and product per-app night override none (package configuration absent).


## Source and authority

- Baseline: **iPhone 17, iOS 27.0**, confirmed by the maintainer.
- Visible device: **AirPods 5 (Wireless Charging)**, model **A3440**; earbuds and case firmware **9A350**.
- English text, light appearance. Received 2026-09-21 as `Photos-1-001.zip`: 20 PNGs, including four stitched Picsew captures.
- Original archive, extracted images, SHA-256 manifest and searchable OCR are preserved at [the reference directory](/Users/moseoridev/programming/android/librepods/designReference/ios-airpods/2026-09-21/README.md). Storage/tool details belong in the [development guide](android-development.md#reverse-engineering-and-local-evidence).

Read this catalog first for navigation, choices and observed states. Open the named original for visual details or ambiguous wording. The images are authoritative for what they show; the catalog is a visually checked transcription of structure, with explanatory text summarized. Raw OCR is only a search aid. If they disagree, correct the catalog from the image. User-confirmed device context supersedes guesses based on appearance.

All switch/checkmark values below are **capture-time observations, not factory defaults**. A chevron indicates a destination, not evidence that its interaction was tested. Parent-child mappings follow matching row/page titles; the archive contains still images, not a recorded navigation trace. Do not infer slider ranges, step sizes, mutual-exclusion rules or persistence from these captures. Serial number, personalized device name and personal warranty dates are intentionally omitted from this public catalog.

## Main screen

Source: `Picsew_20260921225117.PNG`. Preserve this observed top-to-bottom order and grouping:

1. Device title and artwork; combined earbud and case battery indicators. Captured earbuds 100% with charging mark, case 89%.
2. `Name` navigation row with the current device name.
3. `Listening Mode`: Off, Transparency, Adaptive, Noise Cancellation. Transparency is selected. Mode labels accompany icons.
4. `Volume` slider with speaker icon. No numeric value or range is shown.
5. One navigation group: `Audio & Routing`, `Controls & Gestures`, `Live Translation (Beta)`, `Accessibility`.
6. One navigation group: `Battery`, `Find My`.
7. `About`: Model Name, Model Number, Serial Number, Version. Version has a chevron and opens the firmware detail page.
8. `Limited Warranty` row with expiry summary and chevron.
9. One action group: `Disconnect`, `Forget This Device`.

The Name editor, listening-mode transitions, volume interaction and Disconnect/Forget confirmation dialogs are not captured.

### Home row icons

The iOS home capture determines which Samsung rows carry leading icons. Galaxy Buds4 Manager supplies the 24dp leading slot and 18dp gap before the label. Its `ic_sounds_ui8`, `ic_accessibility_ui8` and `ic_about_earbuds_ui8` resources use 24dp vectors with 1.5-unit outline strokes. LibrePods draws its own outlines with round caps and joins, tinted with `onSurface`. [Manager 9.0.10.1724 Home capture](/Users/moseoridev/programming/android/librepods/evidence/compose-residuals-cold-20261113/shadow-dialog-final-20261115/portrait-480-dark/session-attempt-0/captures/native-Home-scrolled-1080x2340-480-dark.png)

| Samsung home row or group | iOS capture and icon decision |
| --- | --- |
| Audio & Routing, Controls & Gestures, Accessibility | `Picsew_20260921225117.PNG` shows leading icons. Keep the Sound, Touch and Accessibility drawings. |
| Battery | `Picsew_20260921225117.PNG` shows a leading battery glyph. Use the LibrePods battery outline in the same slot as the adjacent menu icons. `IMG_0087.PNG` shows the destination. |
| Name, About, Disconnect | `Picsew_20260921225117.PNG` shows an iconless Name row, About heading and fields, and Disconnect action. Keep them iconless, including the Version row that opens `IMG_0089.PNG`. |
| Heart Rate, Hearing Health, Hearing Protection, Hearing Aid | These Pro features are outside the `Picsew_20260921225117.PNG` AirPods 5 capture. Icon presence is unverified. |
| LibrePods Options | The `Picsew_20260921225117.PNG` capture contains no iOS counterpart for this LibrePods destination. Icon presence is unverified. |

## Audio & Routing

Source: `Picsew_20260921225304.PNG`. Observed order:

| Row or section | Control / observed state | Meaning visible in the reference |
| --- | --- | --- |
| Personalized Volume | Switch, on | Adapts media volume to surroundings. |
| Conversation Awareness | Switch, off | Lowers media volume and reduces background noise when speaking. |
| Equalizer | Navigation | See the detail page below. |
| Automatic Ear Detection | Switch, on | Shares a group with the connection preference. |
| Connect to This iPhone | Navigation; When Last Connected to This iPhone | Group explanation describes routing audio to worn AirPods. |
| Spatial Audio | Section heading | Contains the next two rows. |
| Personalized Spatial Audio | Navigation; On | Personalized rendering for supported devices. |
| See & Hear How It Works… | Action | Demonstration destination is not captured. |
| Spatial Audio explanatory text | Two Learn More links | Describes immersive rendering and possible app access to head pose. |
| Microphone | Navigation; Automatic | See the detail page below. |
| Pause Media When Falling Asleep | Switch, off | No additional options are shown. |
| Enable Charging Case Sounds | Switch, on | No additional options are shown. |

| Detail page / source | Observed controls and state |
| --- | --- |
| Equalizer — `IMG_0070.PNG` | Recommended (selected), Custom. Explanation of changing the sound profile. A media artwork/play area and LOW/MID/HIGH graph appear subdued, with a flat dotted line. Reset is subdued. Custom-mode editing, numeric ranges and preview playback are not captured. |
| Connect to This iPhone — `IMG_0071.PNG` | Automatically; When Last Connected to This iPhone (selected). Explanation distinguishes always connecting from connecting when this phone was last used. |
| Personalized Spatial Audio — `IMG_0072.PNG` | Text says personalization is in use; About Personalized Spatial Audio & Privacy… link; Stop Using Personalized Spatial Audio… action. Enrollment and stop confirmation are not shown. |
| Microphone — `IMG_0073.PNG` | Automatically Switch AirPods (selected), Always Left AirPod, Always Right AirPod. |

## Controls & Gestures

Source: `IMG_0074.PNG`. Observed order:

1. `Press And Hold AirPod`: Left → Listening Mode; Right → Listening Mode.
2. `Call Controls`: Answer Call → Press Once (no chevron); Mute & Unmute → Press Once; End Call → Press Twice. The latter two have chevrons.
3. `Head Gestures` → Off.
4. `Camera Remote` → Off.
5. Camera explanation: capture photos/start or stop recording; Press Once makes media-control gestures unavailable, while Press and Hold makes listening-mode and Siri gestures unavailable. This is reference text, not tested Android behavior.

| Detail page / source | Observed controls and state |
| --- | --- |
| Left — `IMG_0075.PNG` | Listening Mode (selected), Siri. A second Listening Mode group offers Off, Transparency (checked), Adaptive, Noise Cancellation (checked), with an icon and short explanation per row. Footer says holding the stem cycles the selected modes. The Right detail page, Siri-selected view and selection constraints are not captured. |
| Mute & Unmute — `IMG_0076.PNG` | Press Once (selected), Press Twice. |
| End Call — `IMG_0077.PNG` | Press Once, Press Twice (selected). Cross-setting coupling is not demonstrated. |
| Head Gestures — `IMG_0078.PNG` | Illustration and explanatory text about accepting/declining announced calls and interacting with/dismissing announced notifications. Head Gestures switch off. Accept, Reply → Up and Down; Decline, Dismiss → Side to Side; Try Head Gestures… action. Child rows remain visible with the switch off; their enabled behavior is not proven. |
| Accept, Reply — `IMG_0079.PNG` | Up and Down (selected), Side to Side. |
| Decline, Dismiss — `IMG_0080.PNG` | Up and Down, Side to Side (selected). Relationship between the two selections is not demonstrated. |
| Camera Remote — `IMG_0081.PNG` | Off (selected), Press Once, Press and Hold. |

## Live Translation

Source: `IMG_0082.PNG`; title `Live Translation` (the parent row includes `(Beta)`).

An illustration and instruction say to wear both AirPods and hold both stems to start live translation. Under Supported Languages, the visible rows are Chinese (Mandarin, Simplified), Chinese (Mandarin, Traditional), English (UK), English (US), French, German, Italian, Japanese and a partially visible Korean row. Download symbols appear at the trailing edge. The screenshot ends within the list: this is not a complete language inventory. Download results, permissions and the translation session are not shown.

## Accessibility

Source: `Picsew_20260921225516.PNG`. Observed order:

| Section / row | Options or state | Explanation / limit |
| --- | --- | --- |
| Press Speed | Default (selected), Slower, Slowest | Timing for double/triple presses. |
| Press and Hold Duration | Default (selected), Shorter, Shortest | Duration needed for holding the stem. |
| Noise Control → Noise Cancellation with One AirPod | Switch, off | Allows cancellation with one earbud worn. |
| iPhone Audio & Visual Settings | Navigation/action link | Additional phone audio accessibility settings, including Mono Audio. Destination is not captured. The product reuses this position and label for transparency tuning, an inherited iOS structure rather than a reproduction of the phone-side destination. |
| Spatial Audio Head Tracking → Follow iPhone | Switch, on | Explanation associates supported spatial audio with the phone rather than following head movement. |
| Tone Volume | Slider, label 100% | AirPods sound-effect volume. The thumb is not at the far end; do not assume 100% is the maximum. Range/steps are not shown. |
| Volume Control → Volume Swipe | Switch, on | Adjust media/call volume by swiping the touch control. |
| Swipe wait-time choices | Default (selected), Longer, Longest | Delay between swipes to avoid unintended changes; appears below Volume Swipe. |

## Battery, Find My and device information

| Page / source | Observed controls and state |
| --- | --- |
| Battery — `IMG_0087.PNG` | Introductory battery-lifespan text. Optimized Battery Charging on; explanation describes learning the charging routine and delaying charging past 80%. Charging Notifications on; explanation mentions low-battery and fully charged reminders. Neither switch's confirmation/disabled states are captured. |
| Find My — `IMG_0088.PNG` | Find My Network on; Show in Find My action. Explanation describes locating individual AirPods on a map. No map or network operation is captured. |
| Version — `IMG_0089.PNG` | Version 9A350, Case Version 9A350; firmware-details link to support.apple.com. |
| Limited Warranty — `Picsew_20260921225559.PNG` | Device card (AirPods 5, serial field), Limited Warranty, expiry field, Hardware Service and Chat & Phone Support coverage, benefits link. Need Help? section with Apple Support app card/Get button, explanatory text and Get Support on Web action. Coverage/legal explanatory text includes further links. This is an external service flow, not an AirPods configuration command. |

AirPods 5 shows Optimized Battery Charging in `IMG_0087.PNG`. Apple documents Optimized Charge Limit separately for AirPods Pro 3. LibrePods shows the charge-limit switch when AACP reports the Dynamic End of Charge capability or the command has a value. AirPods 5 with Wireless Charging Case (A3439, firmware build 9442784) reported neither after a clean app setup. Samsung Home offers Battery when at least one battery control can be shown. Optimized Battery Charging and Charging Notifications are tracked in issue #4. [Apple AirPods User Guide](https://support.apple.com/en-lamr/guide/airpods/devde25a4bbe/27/web/27)

## LibrePods option placement

The product's `LibrePods options` page is now limited to model-specific pages. Entries with an iOS position moved to it; app-owned features moved to app settings.

| Former Extras entry | Now | Notes |
| --- | --- | --- |
| Automatic Connection | `Audio & Routing` → `Connect to This iPhone` | The page carries both `SMART_ROUTING_MODE` (On/Off) and the prox-connect rules (`alwaysConnectWhenAccessoryDisconnected`, the phone/media disconnected/idle/music booleans) from `AppleSettings`. The standalone prox-connect destination is no longer routed. |
| Disconnect AirPods when not wearing | `Audio & Routing`, immediately after Automatic Ear Detection | The switch uses `settings.disconnectWhenNotWearing` and its explanatory footer, matching the iOS grouping. |
| Customize Transparency Mode | `Accessibility`, after the one-bud noise-control switch and before Tone Volume | The iOS path is Accessibility → Audio & Visual → Headphone Accommodations → Transparency Mode. The row is labelled `iPhone Audio & Visual Settings` and opens the existing transparency-tuning screen; the vendor-ID-hook and not-hearing-aid-enabled gate is unchanged, and Extras no longer lists it. The row's description reuses the transparency-tuning text. |
| Overlays | App settings, below Island settings | Device-scoped (`showIslandPopup`, `showBottomSheetPopup`). The row appears only when an Apple device is known and resolves the connected device, else the first known one. No per-model list. |
| Debug | App settings → Advanced options | Still shown only while debug mode is on, and only with an Apple device to receive raw packets. |

Pending placement — settled: Apple documents both hearing entries and heart rate under the AirPods settings root, so they are no longer Extras entries. The Apple/Material surface already mirrored this hierarchy; the Samsung composition now does too.

| Entry | Gate | Destination | iOS path and source | Match |
| --- | --- | --- | --- | --- |
| Hearing Protection | `PPE` base capability | `HearingProtectionScreen` (loud sound reduction plus PPE toggle) | `Settings` → tap the AirPods name → `Hearing Protection` (if absent, first tap `Hearing Health`). [Use hearing health features with AirPods Pro 2 or AirPods Pro 3](https://support.apple.com/guide/airpods/hearing-health-features-airpodspro2-devd9aac5b42/web), "Use the Hearing Protection feature"; also [Use Hearing Protection with your AirPods Pro 2 or AirPods Pro 3](https://support.apple.com/120850) | Path and parent section match. The ordering claim (a `Hearing Health` group directly after the name row) is an adaptation: Apple presents `Hearing Health` as a submenu, and this surface flattens it into a titled group whose first row is the destination. |
| Hearing Aid | `HEARING_AID` capability and vendor-ID hook | `HearingAidScreen`, adjustments and hearing test | Same root and section: `Settings` → AirPods name → `Hearing Assistance` (if absent, first tap `Hearing Health`); hearing test at `Hearing Health` → `Take a Hearing Test`. Same guide, "Set up Hearing Aid" and "Take a Hearing Test"; [Use the Hearing Aid feature on your AirPods Pro 2 or AirPods Pro 3](https://support.apple.com/120992) | Path and parent section match; flattened into the same group. |
| Heart Rate | `HRM` base capability | `HeartRateRoute` | `Settings` → AirPods name → `Heart Rate` (if absent, first tap `Privacy`). [Track your heart rate during workouts with AirPods Pro 3](https://support.apple.com/guide/airpods/track-heart-rate-workouts-airpods-pro-3-dev1b40fb47d/web), "Turn AirPods Pro 3 in-ear heart rate monitoring on or off"; [How to check your heart rate with AirPods Pro 3](https://support.apple.com/en-us/123184) | Root-level placement matches. The exact position (above `About`, below the battery row) is an adaptation: Apple places it inside the `Privacy` submenu and does not publish row order. |
| Recorder | firmware major version 8 or later | `RecordingScreenRoute` | No iOS settings row. Apple documents recording as a capability of supported camera apps and of Voice Memos, not as an AirPods setting: [Use AirPods with supported camera apps on iPhone or iPad](https://support.apple.com/guide/airpods/use-airpods-with-camera-apps-dev8e2a2ba2e/web). | No iOS position exists to mirror, so the row keeps `LibrePods options`, which is now its only entry and shows a single `녹음기` row. |

The groups are computed from capabilities and the vendor-ID hook rather than a model list, so a model without them shows neither the `Hearing Health` group nor the heart-rate row, and a model without the recorder firmware hides the Home `LibrePods options` row entirely.

Spec-gated settings rows stay hidden while the model number is blank. With a model number, they use the resolved model's capabilities, including the full `UNKNOWN` fallback for unrecognized model numbers.

## Coverage and use during implementation

All 20 supplied images were visually inspected. This catalog intentionally preserves unavailable/uncaptured entries rather than filling them from memory. It is a baseline for this model/firmware/OS, not an inventory of every AirPods model or state.

Before implementing a page, map each listed control to the current upstream state/capability/action. Distinguish an existing setting, a phone-side feature, an external Apple service and an unimplemented feature based on evidence; the screenshot alone does not assign support status. In particular, the presence of Siri, personalized spatial audio, translation, Find My or warranty UI is not evidence of an Android implementation.

Uncaptured areas include Name editing, Right long-press details, Custom EQ editing, adaptive-mode options, gesture tutorial, spatial-audio demo/enrollment, the remaining translation list, phone Audio & Visual settings, link destinations and confirmation/error/disconnected states. Do not assume these pages are absent in iOS. Obtain only the missing reference needed for the next implementation rather than requiring a complete screenshot collection in advance.

Galaxy Buds provides the visual reference; these iOS screenshots provide information hierarchy, choices and visible explanations. Do not copy iOS chrome or infer One UI spacing from these images. Keep new captures under a separate dated external baseline, update the relevant catalog entries with their source filenames, and retain old provenance. Keep runtime validation in the appropriate topic document rather than turning this catalog into a build diary.

## First-run onboarding

Onboarding always uses the Samsung presentation. The button-driven sequence uses a 260 ms full-width slide. It shares the existing permission requests, Bluetooth completion gate, consent persistence and compatibility decision.

| Page | Reference | Adopted |
| --- | --- | --- |
| Welcome | Galaxy Wearable `wm-01` | Centered 30sp app name above the artwork, 14sp description and a 200×52dp monochrome Start pill. The artwork uses the AirPods silhouettes already shipped by LibrePods. |
| Privacy policy | Buds `buds-02` notice and `buds-04` consent | Regular 24sp toolbar title after the 60dp back slot, blue app mark and bold 27sp body title, followed by the scrollable policy and a 48dp blue consent pill. |
| Permissions | Wearable `wm-10` | A 30sp body heading, required and optional groups, and flat 16sp/13sp rows with 24dp icons. LibrePods adds a trailing Allow action or a check and Allowed status. The pending row opens the existing request, and Done activates once the required Bluetooth grants are present. |
| Compatibility notice | Wearable toolbar shape | The shared header frames the notice. The bypass control remains available in the FOSS build. |

The toolbar title starts at 60dp. Welcome and permissions use Wearable's monochrome pill colors, and privacy consent uses the Buds blue `#387AFF`. The page-by-page comparison records which other choices are adapted for LibrePods. [Onboarding comparison](/Users/moseoridev/programming/android/librepods/evidence/onboarding-polish-20261118/gaps.md).

The earlier carousel removal was measured on the S25 at 1080x2340/480, dark and light, against the retained upstream APK: cold launch 442/444 → 405/400 ms; privacy→permissions p90 63.3/84.7 → 9.4/9.1 ms and `>33 ms` frames 4 → 2; welcome→privacy p90 34.8/40.1 → 16.6/17.9 ms; Vulkan pipeline creations per transition 23–41 → 1–6; privacy scroll never exceeds 9 ms. p50 rises by 0.7–2.9 ms because the switch now runs its full animated duration instead of stalling. These timings predate the current slide. [Evidence and pairs](/Users/moseoridev/programming/android/librepods/evidence/onboarding-samsung-20261116/README.md).

## Design surface selection

`DesignSystem` has three values: `Samsung` (default), `Apple` and `Material`. The choice is a presentation switch only; state, capability checks, commands, routing targets and the permission/consent logic are shared by all three.

| Value | Device surface | Component answer |
| --- | --- | --- |
| `Samsung` | `BudsDeviceSettingsRoute` → `BudsDeviceSettings`, the Manager composition built from `:settings-ui` primitives | The device list and app settings use the Manager scaffold, icon actions, card groups, switches, seek bars and dialog surfaces through the shared styled primitives. App-specific rows and previews use those presentation components with LibrePods content. The device list caches the fixed Samsung squircle path and leaves the connection shape animation to the other styles. |
| `Apple` | `AppleSettingsScreen`, the iOS-structured device screen | Squircles, the iOS battery gauge and the shape morph, as before. |
| `Material` | `AppleSettingsScreen` | Material 3 Expressive: `MaterialShapes` morphing, `displayMedium`-class headings, segmented cards. |

The appearance picker offers all three. Under Samsung the font-family and icon-style groups are hidden because the Manager supplies both; choosing Apple or Material restores them. Onboarding remains Samsung-presentation-only and does not read this setting.

The device list opens App settings on the main navigation stack. Toolbar Back and system Back use that stack, and an automatic connection can open a device while the list is foregrounded. App-settings entry points retain their existing availability checks, including the successful AACP connection check for Appearance.

Samsung App settings composes groups as they approach the viewport. The background-connection association launcher and resume observer stay registered at the page level while the row scrolls. In matched S25 debug runs, the slowest opening frame took 137.56 ms with eager groups and 108.02 ms with lazy groups. FrameTimeline p99 improved and p95 increased. [Perfetto traces and deadline counts](/Users/moseoridev/programming/android/librepods/evidence/app-settings-predictive-20261118/README.md).

The background-connection title has a full text column, with the summary below it in the Samsung and Material rows and in the Apple row footer. The Galaxy S25 with paired AirPods 5 rendered all three styles and returned from App settings and Accessibility using their Back actions. [Earlier design fixture captures](/Users/moseoridev/programming/android/librepods/evidence/onboarding-samsung-20261116/session4/).
