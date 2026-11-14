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
| Home and Back | Eight display/font settings with the shared-column implementation: both fade bands 0/0; seven app regions 0/0, 720-light 3/0 at the documented native Lottie edge. Product Back has 0 unequal pixels throughout. Product Home also has 0 old/new unequal pixels at 420 dark. | [Current captures/analysis](/Users/moseoridev/programming/android/librepods/evidence/compose-residuals-cold-20261113/shared-column-spacing-20261113/home-back-r2/), [original matrix](/Users/moseoridev/programming/android/librepods/evidence/compose-residuals-cold-20261113/final-render-matrix/session-attempt-0/) |
| Back cache | Both paths use software VectorPainter caches and filtered hardware draw with final SRC_IN tint. Omitting the invisible stroke restores ALPHA_8 instead of ARGB_8888;72×72 at480dpi saves15,552 cache bytes. | [Cache metadata](/Users/moseoridev/programming/android/librepods/evidence/compose-residuals-cold-20261113/cache-metadata.json) |
| Master switch | Source `ww.s.a → ww.q.a → s2.z.p` and Compose use the same unshadowed round rectangle, with the column spacing/header split and `u1.q` nested centering. The debug Manager-resource Head fixture matches native ON/OFF app frames and switch ROIs exactly at 420/480 dark, 560 dark and 720×1280/450 light (eight state pairs). At 480 both use top+bottom fade passes; 420/560 use top only. Raw full-frame differences at 420 ON (2367px) and 450 light ON (1278px) are status-clock pixels only. Fresh native processes at 420/560 avoid bitmap artifacts from a Lottie composition retained across density changes. No library fix was needed. Earlier native-versus-shorter-product 480 62/6 residuals do not establish a switch defect; product Left/Right y294 and the earlier eight-setting Home/Back results remain the prior verification scope. | [Head results and captures](/Users/moseoridev/programming/android/librepods/evidence/compose-residuals-cold-20261113/head-manager-fixture/final-results.json), [literal size sources](/Users/moseoridev/programming/android/librepods/evidence/compose-residuals-cold-20261113/head-manager-fixture/size-provenance.md), [product placement](/Users/moseoridev/programming/android/librepods/evidence/compose-residuals-cold-20261113/shared-column-spacing-20261113/) |
| Title inset | No-navigation title reserves the source28dp slot; the unsupported extra8dp is removed. | [Final title matrix](/Users/moseoridev/programming/android/librepods/evidence/title-runtime-20261112/final-matrix/title-analysis.json) |
| Motion | Push450ms cubic(.22,.5,0,1), pop400ms cubic(.22,.25,0,1), density-correct120dp parallax;150ms linear black0↔.18 dim. Direction follows system resources, not app locale alone. Warm paired trajectories are bounded; cold timing remains open. Predictive Back is retained. | [Page motion](/Users/moseoridev/programming/android/librepods/evidence/animation-parity-20261112/page-transition-results.json) |
| Switch, sheet, mode strip, dialog | Source switch300ms × remaining travel; mode-line500ms cubic(.33,0,.67,1). Default sheet uses the native Window dismissal order and long-sheet focus handling; seven own contracts passed. Dialog phase includes Window startup. Other modes/fonts/hover and full per-frame RGB parity remain unverified. | [Motion/contract summary](/Users/moseoridev/programming/android/librepods/evidence/animation-parity-20261112/followup-results.json) |
| Product state and navigation | Six state scenarios plus normal16 pages inspected. Gesture/3-button native/fixture Home pairs0 RGB differences. Own RTL and IME states inspected; native rename IME unpaired. Taskbar, split window, other OEMs and vendor themes remain unverified. | [Product/navigation summary](/Users/moseoridev/programming/android/librepods/evidence/animation-parity-20261112/product-elements-navigation-results.json) |

The backdrop is a500×500 native PNG, not a gradient or Lottie. The maintainer authorized these two rasters; [direct APK/product asset hashes](/Users/moseoridev/programming/android/librepods/evidence/device-baseline/backdrop-provenance.json) establish their identity. Other product artwork and static mode glyphs remain adaptations. No vendor tutorial/preview or unsupported Apple service is added.

## Rendering cost and cold transitions

The adopted full-height nested top/bottom nodes record content once. Bottom is enabled from actual scroll capacity. Inclusive band boundaries, native cubic/dither math and API36 transfer behavior are retained. Current shader code is locally authored. Historical band-sized and rejected renderers have been retired; their results are not current parity claims.

Release-like product Home at `2d9c2d2` was compared with Manager release: FOSS, R8/minification/resource shrinking on, debuggable=false, shell-profileable, same synthetic state and gesture scripts.12 runs/build/gesture were retained. Metric is Flags=0 `FrameCompleted−HandleInputStart`; these different page compositions are not an isolated shader benchmark.

| Gesture | Manager p50/p90/p99 ms | Product p50/p90/p99 ms | Manager/product GPU-cache median MiB |
| --- | ---: | ---: | ---: |
| Continuous3s/500px |15.114/17.198/21.055 |12.086/12.466/12.883 |70.26/71.39 |
| Ten short80px/180ms swipes |14.862/17.574/21.996 |11.661/12.190/12.599 |67.23/68.15 |

The recorded period is about16.66ms; actual deadline budgets vary8.33–41.70ms. Completion overruns are not late-presentation counts. Under the common approximately25ms continuous stratum, product/Manager overruns are0/1285 and1/1190. Dominant FrameTimeline labels are Buffer Stuffing/On-time Present. Product RenderThread mean Drawing/flush/Vulkan finish is8.722/5.402/1.287ms continuous and8.606/5.345/1.267ms short; corresponding Manager means are5.797/3.184/1.403 and5.819/3.483/1.397ms. These nested/wall slices are not additive and do not establish battery savings. [Per-run data](/Users/moseoridev/programming/android/librepods/evidence/release-home-scroll-20261112/analysis/per-run.csv), [timing/deadline summary](/Users/moseoridev/programming/android/librepods/evidence/release-home-scroll-20261112/analysis/timing-analysis.json) and [trace summary](/Users/moseoridev/programming/android/librepods/evidence/release-home-scroll-20261112/analysis/trace-summary.json) preserve the denominators and measured limits.

Cold-transition work is measured separately from warmed scrolling. The R8/profileable, nondebuggable FOSS APK already contains ProfileInstaller and a bundled library profile. [Real-device attribution traces](/Users/moseoridev/programming/android/librepods/evidence/compose-residuals-cold-20261113/cold-attribution-20261113/attribution-r5/attribution.json) at accepted revision `0a13cfd` identify destination composition during Material Scaffold subcomposition/measurement as the large first-frame cost. Navigation first prepares the new scene; the next frame composes and measures its body inside `Record View#draw()`. Returning Home also reconstructs its body. These are trace-only compiler-composable markers, not candidate timing results; retaining the markers increases DEX by201,844 bytes and adds tracing overhead. Inclusive slices below nest and must not be added.

| System-RTL route, real S25 | First three main-frame wall times | Expensive frame: measure/layout | Body composition | Text-node measure |
| --- | ---: | ---: | ---: | ---: |
| Controls→Left push |5.15 /39.32 /3.97ms |27.81ms |`LongPressPage`9.98ms |13 nodes,5.14ms |
| Home→Accessibility push |1.83 /20.27 /2.11ms |16.25ms |outer group7.18ms |28 nodes,4.26ms |
| Accessibility→Home pop |7.08 /36.72 /9.66ms |26.51ms |`DeviceHome`12.00ms, including batteries3.36ms |30 nodes,3.10ms |

The large Left/Home frames spend36.99/36.26ms running on the main thread. Record View wall times29.36/28.45ms largely contain the measure/subcomposition work above, rather than an independent display-list cost. Left font loading is0.17ms; vector-painter setup totals0.05ms on Left and0.08ms on returning Home, and Home backdrop composition is0.09ms. They do not explain the stall. A later Home-pop frame waits7.12ms in `postAndWait`, separately from the rebuild frame. Fade geometry state writes schedule another composition: unused zero-height construction takes2.28ms on Left and1.01ms on Home, followed next frame by real-height construction0.99/0.56ms. This confirms redundant work, but not a qualifying optimization. The earlier [construction summary](/Users/moseoridev/programming/android/librepods/evidence/compose-residuals-cold-20261113/fade-timing-system-rtl/fade-creation-cost.json) measured0.35–2.26ms; no dominant class-loading cause is established.

[Controlled process-cold comparisons](/Users/moseoridev/programming/android/librepods/evidence/compose-residuals-cold-20261113/cold-final-summary.json) use identical DEX/resources for the profile candidates, explicit ProfileInstaller installation and `speed-profile` compilation, alternating builds, system Arabic, fixed450/400ms source-duration fits and input/BOOTTIME alignment. Each push median uses four captures; each Home-pop median uses two. PSS/startup are sampled after the same routes. Only valid trace windows contribute to the recorded CPU summaries; those older rejected APKs and raw traces have been retired.

| Local candidate (rejected) | RTL push median RMS, before→candidate | Accessibility→Home pop median RMS, before→candidate | Controls cold startup median, before→candidate | Home settled PSS median, before→candidate |
| --- | ---: | ---: | ---: | ---: |
| Route-generated profile |8.29→8.48px |8.19→7.12px |122→125ms |202.92→203.46MiB |
| Observed route methods explicitly marked hot |7.39→7.58px |7.38→8.70px |125→121ms |203.85→203.19MiB |
| Remembered draw-phase fade cache |7.42→16.22px |10.61→8.77px |117.5→126ms |202.80→204.12MiB |
| Skip unused zero-height fade construction |7.00→8.61px |6.52→7.37px |127→126.5ms |206.30→205.91MiB |

The first three experiments above use12,176,506-byte APKs (ZIP alignment absorbs small profile/code differences). Their profile assets increase by135/221 bytes; the draw-cache candidate changes DEX by−140 bytes. The route profile worsens Home-pop peak main-frame median25.09→37.55ms, explicit hot marking does not improve push motion, and a stable draw cache worsens cold push. An initial unremembered cache was worse too and is discarded.

The attribution-led [zero-height comparison](/Users/moseoridev/programming/android/librepods/evidence/compose-residuals-cold-20261113/cold-attribution-20261113/no-zero-comparison/comparison-summary.json) uses uninstrumented builds, process-cold starts, ABBA/ABBA build order, four Left pushes and two Home pops per build, the same input/BOOTTIME alignment and450/400ms fitter, ProfileInstaller and `speed-profile`. It skips only an object never drawn; positive-height construction and both draw branches are unchanged. Push peak main-frame median improves32.12→26.60ms and Home-pop peak34.73→31.77ms, but push RMS worsens7.00→8.61px (individual runs6.81/5.90/8.67/7.19 versus8.31/10.01/7.93/8.91), so it is rejected. Controls/Home startup medians are127→126.5ms and160.5→154.5ms; settled PSS182.53→182.30MiB and206.30→205.91MiB. Both APKs are12,176,598 bytes, candidate DEX adds8 bytes, and profile assets are unchanged. Lower CPU frame cost alone does not establish a smoother fixed-duration trajectory.

No performance code, profile or build configuration change is retained. The remaining measured cost is first composition/measurement of the destination and returning Home; reducing that through precomposition would require a separate retained-PSS/idle-CPU comparison, which has not been done. No precomposition or battery claim is made. No rendering change is retained, so the accepted Home/Back eight-setting pixel matrix remains the rendering evidence. The [latest restoration/readback](/Users/moseoridev/programming/android/librepods/evidence/compose-residuals-cold-20261113/cold-attribution-20261113/no-zero-comparison/safe-final-restore.json) confirms1080×2340/480 without overrides, font1.0/font-size2, night yes, gesture navigation, Korean locale/empty app locales, Bluetooth on, all four original APK hashes and no forwards.


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
| iPhone Audio & Visual Settings | Navigation/action link | Additional phone audio accessibility settings, including Mono Audio. Destination is not captured. |
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

## Coverage and use during implementation

All 20 supplied images were visually inspected. This catalog intentionally preserves unavailable/uncaptured entries rather than filling them from memory. It is a baseline for this model/firmware/OS, not an inventory of every AirPods model or state.

Before implementing a page, map each listed control to the current upstream state/capability/action. Distinguish an existing setting, a phone-side feature, an external Apple service and an unimplemented feature based on evidence; the screenshot alone does not assign support status. In particular, the presence of Siri, personalized spatial audio, translation, Find My or warranty UI is not evidence of an Android implementation.

Uncaptured areas include Name editing, Right long-press details, Custom EQ editing, adaptive-mode options, gesture tutorial, spatial-audio demo/enrollment, the remaining translation list, phone Audio & Visual settings, link destinations and confirmation/error/disconnected states. Do not assume these pages are absent in iOS. Obtain only the missing reference needed for the next implementation rather than requiring a complete screenshot collection in advance.

Galaxy Buds provides the visual reference; these iOS screenshots provide information hierarchy, choices and visible explanations. Do not copy iOS chrome or infer One UI spacing from these images. Keep new captures under a separate dated external baseline, update the relevant catalog entries with their source filenames, and retain old provenance. Keep runtime validation in the appropriate topic document rather than turning this catalog into a build diary.
