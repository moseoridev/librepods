# AirPods settings reference and UI direction

## Product boundary

Preserve iOS AirPods settings structure and the semantics of supported features while reproducing Galaxy Buds 4 Manager visual design. A generic One UI interpretation is not the target. Use upstream LibrePods artwork or locally authored resources, not extracted vendor assets. Existing battery-efficiency, responsiveness and nonroot constraints still apply.

Keep future redesign changes concentrated in presentation. Reuse upstream state, capabilities and command paths; do not copy the connection engine or duplicate its state to implement a theme. Existing `Styled*` component signatures provide useful integration points. Some screens still access preferences or construct protocol commands: isolate those dependencies where needed instead of spreading One UI branches into transport code. An upstream update can still change UI contracts and require integration; a presentation boundary does not guarantee conflict-free merges.

The catalog below describes the target information structure, not a claim that LibrePods implements every listed feature. Check each action against code and platform support before making it interactive. Keep supported behavior when moving rows; absent screenshot evidence is not permission to remove capabilities for other AirPods models. Feature gaps require explicit implementation scope, not decorative controls that pretend to work.

## Current implementation boundary

The connected settings entry now links to `Audio & Routing`, `Controls & Gestures`, `Accessibility`, and `Battery`. The first, second and fourth destinations reuse the existing audio/routing, stem/call/head-gesture and optimized-charging controls, respectively. They share `AirPodsUiState` and the existing ViewModel actions; no additional engine, service, polling or preference listener is introduced. Model-specific hearing features remain on the main page. Disconnecting while in a section uses the existing disconnected/reconnect UI.

The shared theme, scaffold, lists, toggles and buttons now use one implementation. The Apple/Material appearance selector and its state/listener were removed; existing saved preference values are ignored. Shared list summaries appear below titles, and standard Compose click/toggle semantics replace custom press handlers. Glass shaders, drag scaling and backdrop rendering were removed from the shared button/scaffold. `Styled*` call signatures remain to limit churn at upstream call sites; compatibility-only glass/row-orientation parameters no longer select a second renderer. The theme's legacy `m3eEnabled` argument and the `LocalDesignSystem`/`DesignSystem` pair are gone: no component selects between two product themes any more, and the dead `DesignSystem.Apple` branches of the migrated renderers (`StyledSlider`, `StyledIconButton`, `ConfirmationDialog`, `AirPodsSettingsScreen`, `AboutCard`, release-note demos) were deleted rather than kept as an alternative look. The one intentionally preserved legacy renderer is `StyledInputField`, selected by its own `InputFieldStyle` argument — the contact form pins `InputFieldStyle.Legacy` to keep the pre-migration field look, and the remaining callers use `InputFieldStyle.Standard`.

This is a **partial visual migration**, not Buds pixel parity or full iOS feature parity. ANC, battery artwork, EQ, dialogs and screen-specific typography/layout still need dedicated comparison. The shared menu, section and choice components now follow the original common renderer; specialized controls and complete page composition are not all verified. No One UI parity, jank or power measurements establish this change's effects. The supplied iOS hierarchy is also partial: volume, charging notifications, case sounds and Apple-service destinations are not added by this move, and existing connection/EQ/head-tracking behavior is not redefined to claim iOS equivalence.

### Common rows, switches and compact header

The common menu path is `gm.t1.I → gm.k1 → gm.t1.d` (`gm.u1.a` is a `gm.t1` instance). It supplies 18dp horizontal row padding and 12dp vertical **text** padding. Padding the whole row changes the alignment of trailing controls. The source's 28dp minimum is applied before text padding; titles/summaries use native 17sp/13sp text metrics without an imposed line height. Compose click/toggle handlers retain the framework's expanded minimum touch target; this is not a claim that every visual row is at least 48dp tall. Explicit caller minimum heights still work.

`tp.d` and `u3.a` select the optional on-device `sec` font family at weights 100, 300, 400, 500, 600 and 700. `BudsFontFamily` uses Compose's public `DeviceFontFamilyName` API with those weights and empty variation settings. No font file is extracted, downloaded or packaged. If the family is unavailable, Compose resolves the system fallback. The original runtime family list was checked because ASC's inferred Java omitted intermediate entries. The same family now reaches row, choice, section and scaffold text; an unspecified row line height is not inherited from the Material theme.

`BudsStyle` owns the shared roles and geometry. `gm.s1` supplies section text at 13sp/600 with 18dp horizontal, 16dp top and 8dp bottom padding; footer text uses 13sp regular with 14dp top padding and no bottom padding. Cards use the three-segment smooth corner from `qu.b` at 26dp, rather than a circular `RoundedCornerShape`. Normalization, separate scale/pivot-rotation/placement transforms and path-segment order are preserved: collapsing them into absolute-coordinate formulas changed floating-point rounding at the clipped boundary. The 1dp group divider is inset 16dp and composites the original translucent black/white over the surface. Choice summaries use the separate primary-high text color instead of the radio accent. The selected radio center is 5/32 of its width.

The migrated detail containers own 20dp section spacing once, with bottom padding but no extra gap before the first group, instead of each lazy section adding its own spacer. `gm.g2 → m3.i.x` supplies the responsive outer inset: 10dp on narrow screens, an 86% content width from 589dp when height exceeds 411dp, and 75% from 990dp. This page inset is independent of the 18dp text inset inside each card. The connected Home still has its specialized artwork/listening-mode spacing; complete page/header positioning is not established by the shared-component work.

`ku.i0.N → wu.x/wu.u` defines the 32×20dp switch and its 16dp thumb with 2dp padding. Track and padded thumb are measured independently before placement; at fractional densities their rounded pixel heights can differ. Thumb travel uses the measured widths and integer placement, with `placeRelative` preserving RTL. The One UI 8+ default path uses the primary blue checked track and bright thumb in both themes; the inverse palette belongs to the older `up.e.O()` branch. Disabled contents use 40% alpha, and the containing row owns the accessible toggle. A bounded 200ms transition remains an adaptation; custom Samsung theme drawables and their drag/press effects are not reproduced.

Master switches use `gm.c1`'s separate card presentation: checked/unchecked surfaces, semibold 17sp text with 16.5dp vertical padding, and 17dp switch padding. Ordinary settings rows keep the shared row geometry. This uses the same upstream checked/enabled state and callback.

The compact header follows `ku.i0.J/R`, `ku.w2` and the SESL top-padding resource: 64dp content plus 8dp above it, or 56dp with no extra padding in landscape below 580dp height. `gm.w2 → ak.t` offsets the compact header, including its background, upward by 6dp in portrait without moving the body; the collapsed landscape header has no such offset. The title is 21sp bold with 28sp line height; the optional subtitle is 13sp semibold with a separate secondary title color. A title/subtitle pair uses unscaled type; a lone title caps font scaling at 1.3. System insets remain separate. Back artwork occupies 24dp with a 20dp trailing gap, while Compose expands its touch target. Product artwork remains upstream-owned. Custom themes, header expansion transitions and performance are not established as equivalent.

The visible scaffold composites its transparent content over the page background before applying system/header padding. A locally authored linear top fade runs over 56dp from 12% to full content opacity; the compact header background is then drawn above it. Moving the effect to an individual card, painting an opaque detail background inside it, or offsetting only title text changes the result. The shader and effect are remembered by color and density. This requires one viewport-sized foreground offscreen layer, but introduces no frame clock, timer or background work. GPU memory/scrolling cost remains unmeasured. Bottom-edge fading and Samsung window/custom-theme effects are not implemented by this change.

### Sliders

`StyledSlider` now renders through the SESL seekbar rather than the stock Material `Slider`. `SeslAbsSeekBar` composes two drawables: `n2` (track) and `p2` (thumb). `n2` is a round-capped `STROKE` paint of `sesl_seekbar_track_height` = 3dp that animates to `sesl_seekbar_track_height_expand` = 13dp over 250ms under `sesl_seekbar_sliding_animation` = true; `p2` fills a circle of `sesl_seekbar_thumb_radius` = 6.5dp and a concentric inner circle inset by `sesl_seekbar_thumb_stroke` = 2dp. `BudsSeekBar` supplies only the `thumb`/`track` slots of Material's `Slider`, which keeps its gesture and semantics. Measured upstream, `SliderImpl` already places the thumb at `trackWidth * fraction` with the track offset by half the thumb core, which is the same layout the native widget draws — but it mirrors both placements as a whole under RTL, so the custom track paints its own mirrored endpoints. Without that, an RTL layout would move the thumb leftwards while the fill grew rightwards; this is an adaptation, not a reproduced native path. The 16dp `sesl_seekbar_padding_horizontal` belongs to the native widget, not the drawables; the containing row owns it here so the control lines up with the other cards.

Colors come from the resolved SESL resources, added as separate `ColorScheme` roles rather than by overloading Material containers. Active track and thumb ring are `sesl_seekbar_control_color_activated` → `sesl_blue_color_light` (`#387AFF`, the theme primary). The inactive track is `sesl_seekbar_control_color_default(_dark)` → `basic_token_sys_tone_on_tone_high_light/_dark`, a translucent role that must composite over the card surface: multiplying the disabled alpha by the role's own alpha, rather than replacing it, is what keeps the light-theme line at (228,228,232) over `#FCFCFF` instead of an opaque `#17171A`. The thumb core is `sesl_thumb_control_fill_color_activated` → `sesl_gray_L1` (light) / `sesl_gray_D1` (dark). Only `enabled` on the Material `Slider` is used for the disabled state; the native `sesl_gray_L7`/`D7` disabled color role is not applied.

The zero-length active segment is skipped at the low endpoint, because a round-capped `drawLine` with identical endpoints still paints a dot where the native `ClipDrawable` paints nothing at level 0. Verified by reading pixels on the API 37 emulator in both themes: 8px (3dp at density 420) released and 34px pressed, no gap between the active track and the thumb ring, the correct per-theme thumb core, and the translucent inactive track over the card. Not verified: the native 62ms-stepped `PathInterpolator` is not reproduced (the expansion is a plain 250ms `tween`), the thumb's 100ms press-in / 300ms release-inner-circle animation is not implemented, TalkBack behavior of the new renderer is unexercised, the disabled and maximum-value states and RTL layout were not captured, and no Samsung-device or per-call-site Buds screenshot comparison exists. The EQ screen has no slider at all — iOS shows a LOW/MID/HIGH graph there and LibrePods presents no editable EQ — and the quick-settings dialog's `VerticalVolumeSlider` is a separate, uncompared renderer.

### Expandable detail headers

The Buds home path `ml.k.p → gm.u1.b` explicitly disables header expansion. Detail pages use `gm.u1.c`, which resolves its expansion policy before the One UI 8.5+ path `gm.u1.d → gm.y2.a → bl.h(case 4)`. LibrePods enables the new presentation-only header for the migrated Audio & Routing, Controls & Gestures, Battery, long-press, call-choice and head-gesture pages; Home and unmigrated destinations keep their compact header.

Accessibility and microphone details also use this scaffold and its shared inset/scroll ownership. The header starts collapsed. At the top of a scrollable detail page, pulling down expands it; upward scrolling collapses it before the list moves. Dragging the header also works. Source geometry is a 262dp expanded area with 198dp travel over the 64dp compact bar, centered 34sp/600-weight title with 24dp horizontal inset, a 3/5 title translation and half-range alpha transitions (`gm.m2`/`gm.n2`). `gm.t2` supplies the nearest-endpoint 500ms settling duration. Compact system insets remain separate.

The implementation consumes only actual header travel and leaves the remainder to the existing scroll container. Its animation is bounded, interrupted by a new drag and canceled when the destination or usable range changes or the scaffold leaves composition. It adds no polling, engine state or background work. This is a Compose adaptation of the observed behavior: all landscape layouts stay collapsed, expanded titles are capped at three lines with ellipsis, and Samsung-specific window policy, touch effects and exact animation easing are not reproduced. TalkBack interaction, interrupted-fling behavior, whole-screen pixel parity and performance remain unverified.

### Main listening-mode and battery presentation

The listening-mode card uses a single Compose renderer and the shared group shape. The active home path `ml.k.o → vb.q.a/b → gl.s` centers a 320dp strip below 589dp screen width and 392dp on wider screens, with 10dp internal horizontal padding. The 44dp buttons span the available strip while captions have their own integer-dp width (20% of the strip, capped at 80dp), 52dp top origin and natural 12sp text metrics. Selected captions use weight 600. The current One UI path selects the primary blue for the button and caption; the older inverse surface palette is not this branch. `NoiseControlSeekBarBackgroundView` draws a 6dp line and native oval ticks beneath the Compose button backgrounds, so the layering also matters at antialiased edges. The locally authored tick drawable is remembered by color; there is no new animation clock, service or polling.

The implementation keeps iOS ordering and upstream LibrePods icons. Three visible modes when Off is unavailable use the same measured endpoint distribution; this is a LibrePods extension to the four-mode reference. Labels grow vertically at enlarged font scales. Click/radio semantics render the existing UiState, which the ViewModel updates optimistically after sending a command; selection does not guarantee an AirPods acknowledgement. No proprietary Lottie is included, and Samsung icon animation, drag behavior and custom themes are not reproduced. Missing/invalid ANC values leave all visible modes unselected. Icon pixels, full-home composition, interactions and performance are separate from source-backed control geometry.

The battery header now uses bounded upstream product artwork and compact locally drawn indicators instead of the legacy animated rings and SF private-use glyphs. Optimized charging also has a visible text label distinct from ordinary charging. Its artwork bounds, label arrangement and indicator are authored adaptations, not a pixel-verified reproduction of Buds battery UI. Missing, disconnected, out-of-range or unsupported-status readings display `—`, while a valid 0% remains 0%. The existing within-three-percent earbud merge is preserved only for two valid readings with identical charge statuses. Rendering no longer writes Compose state. This normalizes the supplied snapshot; it does not introduce a transport freshness guarantee or change battery acquisition. Focused JVM tests cover unknown/zero handling, charge-sensitive merging and removed components. Long mode labels wrap at enlarged font scales; Bluetooth behavior, TalkBack and full Samsung parity remain unverified.

### Controls and gestures

Long-press and call-choice pages now use presentation-only content composables with their existing ViewModel entry points. Call selections and parent summaries derive directly from current UiState rather than remembered initial labels. The call mute/end mapping remains coupled through the original `CALL_MANAGEMENT_CONFIG` bytes; listening-mode bits and the two-mode minimum remain owned by the existing ViewModel.

The Buds pinch-and-hold path `ht.a.k → bl.o → gm.t1.B` defines a leading radio, 18dp horizontal/14dp vertical row padding, 14dp radio gap, optional 28dp artwork plus 16dp gap, and 17sp/13sp title/summary. `ku.i0.H → m2.d.o/p` and `ku.k2` supply the 32dp radio and its drawn circle proportions. `BudsChoiceRow` reproduces this measured static structure using locally drawn circles; extracted artwork is not included. A trailing action reserves its own 28dp slot after a 1×22dp separator; grouped choice dividers start 62dp from the leading edge and end 16dp from the trailing edge. Original animation, custom-theme resources and whole-screen pixel equivalence are not established. The default unselected radio colors are light `#A3A3A7` and dark `#636368` from `ku.i0.H`. Sources remain outside Git under `/Volumes/TESSERACT/programming/android/librepodsDesignReference/buds4-settings/2026-09-22/`.

Head gestures preserve the upstream fixed nod-to-answer/shake-to-decline behavior. Configurable accept/reply/dismiss mappings and Camera Remote are not implemented by the existing backend and are not presented as working controls. The old toolbar tracking toggle is removed so the diagnostic expansion is the only tracking entry point on this settings screen. Sensor diagnostics remain available through an explicit test expansion; the settings landing page no longer starts tracking. Closing the expansion, leaving the screen or disconnecting disposes the diagnostic tracking path. Once opened, it retains the existing start/stop API; Activity pause and concurrent call ownership are not redesigned here. The existing graph/test is a LibrePods diagnostic extension, not a copied Buds screen. Real tracking lifecycle, Bluetooth commands, TalkBack and whole-screen Buds equivalence remain unverified.

### Accessibility and microphone choices

Press speed, hold duration, volume-swipe interval and microphone choices reuse the source-derived `BudsChoiceRow` geometry above and the shared detail column. Labels and ordering follow the iOS reference: hold duration uses Default/Shorter/Shortest, and microphone uses Automatic/Left/Right while preserving upstream wire values 0/2/1. Choices derive from current UiState; a missing or unrecognized report leaves them unselected. Selecting the current value is a no-op. Disconnected controls are disabled, and the previous premium/model/vendor-hook conditions remain in place.

Opening Accessibility no longer sends the disabled, hidden phone/media EQ defaults or a tone-volume command. The unused EQ state, unscoped job and commented-out renderer were removed. Tone-volume edits retain the 100 ms quiet period and existing `[volume, 0x50]` command via the ViewModel; incoming snapshots only update the display. Pending edits are canceled when the tone-volume content leaves composition or its device serial, connection or entitlement key changes. A navigation transition can retain outgoing content long enough for a pending edit to complete; pressing Back does not guarantee immediate cancellation. The existing 0–100 range and 75 fallback/snap point remain; these are upstream behavior, not ranges inferred from the iOS screenshot.

Device-switch cancellation, external reports during a gesture, TalkBack and whole-screen Samsung parity remain statically reviewed only.

### Buds theme evidence and migration policy

ASC follows `HomeScreenActivity` → `tk.b` → `tp.c.b` → `xu.f.a`. `BaseActivity.x` selects separate home renderers across the One UI 8 boundary. The common theme resolves `nu.j` → `ju.b`/`ju.a` (case 12) → SESL color resources, merged by `ru.c`/`ru.b` with the fallback token scheme. `Theme.kt` uses the resolved default primary, background, main/secondary text and divider colors; specialized surfaces and complete pages are not all validated. Custom Galaxy themes and specialized typography remain outside the shared default-theme contract. The common type roles now request the same optional system family as the original; never package an extracted Samsung font.

Code output, resource dump and provenance stay outside Git at `/Volumes/TESSERACT/programming/android/librepodsDesignReference/buds4-settings/2026-09-21/`. Do not substitute similarly named legacy resource values without tracing the active renderer. No vendor code or artwork is packaged.

Migrate and verify a component, then remove its replaced visual implementation in the same change. Preserve live feature behavior and useful upstream state/action interfaces. Keep unfinished specialized renderers until replaced; do not leave a permanent old/new theme switch or a second state model. Upstream updates may still need presentation conflict resolution.

## Source and authority

- Baseline: **iPhone 17, iOS 27.0**, confirmed by the maintainer.
- Visible device: **AirPods 5 (Wireless Charging)**, model **A3440**; earbuds and case firmware **9A350**.
- English text, light appearance. Received 2026-09-21 as `Photos-1-001.zip`: 20 PNGs, including four stitched Picsew captures.
- Original archive, extracted images, SHA-256 manifest and searchable OCR are preserved outside Git at [the reference directory](/Volumes/TESSERACT/programming/android/librepodsDesignReference/ios-airpods/2026-09-21/README.md). Storage/tool details belong in the [development guide](android-development.md#reverse-engineering-and-local-evidence).

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
