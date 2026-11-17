package me.kavishdevar.librepods.presentation.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.ui.components.SettingsButton
import me.kavishdevar.librepods.ui.components.SettingsButtonStyle
import me.kavishdevar.librepods.ui.components.SettingsIconButton

/**
 * Samsung first-run values shared by every onboarding page. Anchors are the reference captures
 * under evidence/onboarding-samsung-20261116 and their resources:
 * - 24sp header title, left-aligned after a 60dp back slot: wm-10 `권한` bounds `[180,163][284,246]`
 *   (83px cap height) and Buds `개인정보 처리방침` `[180,165][651,256]`.
 * - 30sp welcome heading: GalaxyWearableTitleStyle (watchmanager 0x7f1201b5), drawn in the body.
 * - 32dp page inset: title and row bounds start at x=96px at 480dpi.
 * - 16sp/13sp rows, 24dp icon + 18dp gap, 62dp minimum row: wm_permission_guide_item_layout.xml.
 * - 14sp group label: permissions_required_title; 14sp body: permission_link_textview;
 *   12sp footnote: the Buds consent footnote.
 * - Primary pills keep the fill of the reference that owns the role:
 *   Wearable `positive_button` is monochrome — `black_button_background_color`
 *   (#252528 day / #E9E9EC night) over `black_button_text_color` (#FAFAFF day / #17171A night).
 *   Verified by sampling the 480dpi captures: night `wm-01-launch.png` and `wm-09` = #E9E9EC,
 *   light `captures-light/wm-01-launch.png` and `wm-09` = #252528. Used for welcome and permissions.
 *   Buds consent `동의` is `blue_button_background_color` #387AFF — the theme primary, not a
 *   separate blue. Every capture is Display-P3 tagged, so its raw bytes read #4978F6 (73,120,246)
 *   and the disabled pill reads #1D3062, which is that value at the 40% alpha the shared button
 *   already applies. Used for the privacy consent only.
 * The text styles keep the family of the enclosing SettingsTheme typography (on-device `sec`),
 * so sizes are the only onboarding-specific input.
 */
internal object OnboardingStyle {
    /** The welcome heading; the reference draws it in the body, not in a toolbar. */
    val Title: TextStyle
        @Composable get() = MaterialTheme.typography.titleLarge.copy(fontSize = 30.sp, lineHeight = 40.sp)
    /**
     * Buds body-hero title: `activity_notice` declares `textSize 27dp`, `textStyle 1` (bold),
     * `lineSpacingExtra 5dp`, centred. Measured on `buds-02`: ink `968–1039` (72px) in a
     * `946–1058` box, i.e. 27sp × 3 = 81px cap box + 5dp spacing = 112px total line box.
     */
    val BudsHeroTitle: TextStyle
        @Composable get() = MaterialTheme.typography.titleLarge.copy(
            fontSize = 27.sp,
            lineHeight = 37.sp,
            fontWeight = FontWeight.Bold
        )
    val PageTitle: TextStyle
        @Composable get() = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp, lineHeight = 32.sp)
    val RowTitle: TextStyle
        @Composable get() = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp, lineHeight = 22.sp)
    val RowSummary: TextStyle
        @Composable get() = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp)
    val GroupLabel: TextStyle
        @Composable get() = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp)
    val Body: TextStyle
        @Composable get() = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 21.sp)
    val Footnote: TextStyle
        @Composable get() = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp)

    val Inset = 32.dp
    /** Reference header: toolbar row 103→295px and a back button `[0,115][180,295]` at 480dpi. */
    val HeaderHeight = 64.dp
    val HeaderBackSlot = 60.dp
    /**
     * Body-hero heading colour: Wearable `gray_scale_text_color2` `#EAEAEB` night / `#252528` day,
     * sampled on the wm-10 hero title (ink `#EAEAEB`) and in `captures-light/wm-09` (`#2D2D2D`, the
     * same value through Display-P3). It is one step below the toolbar title's `text_color1`.
     */
    val HeadingText: Color
        @Composable get() = if (settingsSurfaceIsDark()) Color(0xFFEAEAEB) else Color(0xFF252528)
    /**
     * Hero mark size. The reference `wm_permission_detail_fragment` declares a 46dp ImageView
     * (`header_icon_width_height`) whose `ic_permission` drawable fills **71%** of its box — the
     * drawn glyph is 98px inside the 138px box at `[471,463][609,601]`. The launcher monochrome
     * fills only **45%** of its viewport (measured: 56x62px in the same 138px box), because the
     * adaptive-icon safe zone is larger than `ic_permission`'s padding. The box is therefore scaled
     * by 0.710/0.449 = 1.58 to 73dp so the *drawn mark* is the reference's 98px, not the reference's
     * invisible box. This is a measured deviation from the declared 46dp; see the evidence README.
     */
    val HeroIconSize = 73.dp
    /**
     * Hero box top below the header: set so the scaled box still draws its ink at the reference's
     * 483px. The declared `layout_marginTop 32dp` counts from a scroll origin hidden under the
     * floating toolbar, so it does not describe the drawn gap; 43dp here reproduces the captured
     * pixel row.
     */
    val HeroTop = 43.dp
    /**
     * Box→heading gap: tuned so the 30sp heading's ink lands on the reference's 674px row after the
     * larger box; the declared `title_top_margin 16dp` is the box gap for a 46dp box whose ink
     * already reaches its bottom edge.
     */
    val HeroHeadingGap = 4.dp
    /**
     * Buds hero mark→title gap: `activity_notice` declares `layout_marginBottom 11dp` on the
     * illustration above `text_title`. Buds' mark is a vendor illustration, so the LibrePods
     * launcher monochrome stands in for it; only the gap is source-backed.
     */
    val BudsHeroGap = 11.dp
    /**
     * Reference heading→first-group gap: 32dp (heading box bottom 1013px, `필수 권한` ink top
     * 1109px). [OnboardingGroupLabel] already contributes its own 21dp, so callers add the rest.
     */
    val HeroToGroup = 11.dp
    /** Buds hero-block→document gap: 38dp (heading box bottom 1058px, body ink 1172px). */
    val HeroToDocument = 26.dp
    /**
     * Buds privacy hero top: 34dp, both declared (`activity_notice` `layout_marginTop 34dp`) and
     * measured (toolbar bottom 307px → illustration top 409px = 102px). Buds draws a 168dp vendor
     * illustration there; that artwork is not packaged, so the LibrePods app icon takes the slot at
     * the Wearable 46dp box size.
     */
    val HeroTopBuds = 34.dp
    /**
     * Welcome heading origin: the reference `갤럭시 웨어러블` starts at y=360px = 120dp from the screen
     * top at 480dpi. The welcome page has no header row, so the body starts under the status bar and
     * this offset carries the whole distance to the greeting block.
     */
    val WelcomeHeadingTop = 120.dp
    /** Gap between the greeting line and the 30sp app name, sized like the body's own line rhythm. */
    val WelcomeGreetingGap = 8.dp
    val RowIconSize = 24.dp
    val RowIconGap = 18.dp
    val RowMinHeight = 62.dp
    val RowVerticalPadding = 10.dp
    val BottomButtonHeight = 52.dp
    val BottomButtonWidth = 200.dp
    val BottomButtonGap = 8.dp
    val BottomInset = 20.dp

    /**
     * Reference page background: Wearable and Buds pages sample #010101 night and #FFFFFF day.
     * The settings light `background` token is #F3F3F5, visibly greyer than the reference, so the
     * sampled value is used for this surface only.
     */
    val PageBackground: Color
        @Composable get() = if (settingsSurfaceIsDark()) Color(0xFF010101) else Color(0xFFFFFFFF)

    /** Wearable `black_button_background_color`; monochrome primary for welcome and permissions. */
    val WearablePrimaryFill: Color
        @Composable get() = if (settingsSurfaceIsDark()) Color(0xFFE9E9EC) else Color(0xFF252528)

    /** Wearable `black_button_text_color`, paired with [WearablePrimaryFill]. */
    val WearablePrimaryLabel: Color
        @Composable get() = if (settingsSurfaceIsDark()) Color(0xFF17171A) else Color(0xFFFAFAFF)

}

/**
 * Reads the rendered SettingsTheme surface instead of re-querying the system night mode, so an
 * explicitly themed host and the system default cannot disagree.
 */
@Composable
private fun settingsSurfaceIsDark(): Boolean =
    MaterialTheme.colorScheme.surfaceContainer.luminance() < 0.5f

/**
 * One onboarding page: the reference header, the page body and an optional bottom bar.
 *
 * A null [title] means the page has no header, as on the Samsung welcome page, whose heading sits in
 * the body. A header with a back button is the Wearable `권한` / Buds policy shape: a 60dp back slot
 * (button bounds `[0,115][180,295]` at 480dpi) followed by a 24sp left-aligned title starting at
 * x=180px = 60dp, not a centred display title.
 */
@Composable
internal fun OnboardingPageScaffold(
    title: String?,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    bottom: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(OnboardingStyle.PageBackground)
    ) {
        if (title != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .height(OnboardingStyle.HeaderHeight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(OnboardingStyle.HeaderBackSlot), contentAlignment = Alignment.Center) {
                    if (onBack != null) {
                        SettingsIconButton(
                            onClick = onBack,
                            contentDescription = stringResource(R.string.back),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = OnboardingStyle.PageTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
        if (bottom != null) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = OnboardingStyle.Inset)
                    .padding(top = 12.dp, bottom = OnboardingStyle.BottomInset)
            ) { bottom() }
        }
    }
}

/**
 * The reference bottom pill at the measured 52dp height. The default is the Buds consent role,
 * which uses the theme primary; the Wearable pages pass [OnboardingStyle.WearablePrimaryFill]
 * and [OnboardingStyle.WearablePrimaryLabel] because their `positive_button` is monochrome.
 */
@Composable
internal fun OnboardingPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: SettingsButtonStyle = SettingsButtonStyle.Filled,
    fill: Color = Color.Unspecified,
    label: Color = Color.Unspecified,
) {
    val filled = style == SettingsButtonStyle.Filled
    val container = when {
        !filled -> Color.Unspecified
        fill != Color.Unspecified -> fill
        else -> MaterialTheme.colorScheme.primary
    }
    val contentColor = when {
        !filled -> Color.Unspecified
        label != Color.Unspecified -> label
        else -> MaterialTheme.colorScheme.onPrimary
    }
    SettingsButton(
        onClick = onClick,
        modifier = modifier.height(OnboardingStyle.BottomButtonHeight),
        enabled = enabled,
        style = style,
        containerColor = container,
        tint = contentColor,
    ) {
        Text(text = text, textAlign = TextAlign.Center)
    }
}

/** The reference `시작` pill is 200dp wide on the screen centre. */
@Composable
internal fun OnboardingCentredPrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    fill: Color = Color.Unspecified,
    label: Color = Color.Unspecified,
) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        OnboardingPrimaryButton(
            text = text,
            onClick = onClick,
            modifier = Modifier.width(OnboardingStyle.BottomButtonWidth),
            enabled = enabled,
            fill = fill,
            label = label,
        )
    }
}

/** Horizontal action row, as in the Buds consent bottom bar. */
@Composable
internal fun OnboardingBottomButtonRow(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(OnboardingStyle.BottomButtonGap),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** Flat permission row: 24dp icon, 16sp/13sp text, no card, divider or icon container. */
@Composable
internal fun OnboardingPermissionRow(
    title: String,
    summary: String,
    onClick: (() -> Unit)?,
    icon: @Composable () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = OnboardingStyle.RowMinHeight)
            .clickable(enabled = onClick != null, role = Role.Button) { onClick?.invoke() }
            .padding(
                horizontal = OnboardingStyle.Inset,
                vertical = OnboardingStyle.RowVerticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(OnboardingStyle.RowIconSize),
            contentAlignment = Alignment.Center
        ) { icon() }
        Spacer(Modifier.width(OnboardingStyle.RowIconGap))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = OnboardingStyle.RowTitle,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = summary,
                style = OnboardingStyle.RowSummary,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Group label above a flat row run: 14sp with the reference 21dp/8dp vertical space. */
@Composable
internal fun OnboardingGroupLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .padding(horizontal = OnboardingStyle.Inset)
            .padding(top = 21.dp, bottom = 8.dp),
        style = OnboardingStyle.GroupLabel,
        color = MaterialTheme.colorScheme.onSurface
    )
}

/** Page-body paragraph at the reference 14sp body size. */
@Composable
internal fun OnboardingBodyText(
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    textAlign: TextAlign? = null,
) {
    Text(text = text, style = OnboardingStyle.Body, color = color, textAlign = textAlign)
}

/** Subheading inside a document page, matching the 16sp permission row title. */
@Composable
internal fun OnboardingSectionHeading(text: String) {
    Text(
        text = text,
        style = OnboardingStyle.RowTitle.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface
    )
}

/**
 * The reference body hero: a centred mark above a centred heading, both on the page background
 * with no card or container. Wearable (`wm-10`) draws a 46dp `header_icon_width_height` icon and a
 * centred 30sp/40sp title; Buds (`activity_notice`) draws its illustration and a centred 27sp bold
 * `text_title`. The supply is the app's own launcher monochrome, so no vendor artwork is used.
 *
 * The heading style is the page's own: [OnboardingStyle.Title] for Wearable's 30sp role and
 * [OnboardingStyle.BudsHeroTitle] for Buds' 27sp bold one. Only the toolbar title is 24sp.
 */
@Composable
internal fun OnboardingHero(
    title: String,
    modifier: Modifier = Modifier,
    top: Dp = OnboardingStyle.HeroTop,
    inset: Dp = OnboardingStyle.Inset,
    titleStyle: TextStyle = OnboardingStyle.Title,
    showIcon: Boolean = true,
    markGap: Dp = OnboardingStyle.HeroHeadingGap,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = inset)
            .padding(top = top),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showIcon) {
            Icon(
                painter = painterResource(R.drawable.ic_launcher_monochrome),
                contentDescription = null,
                modifier = Modifier.size(OnboardingStyle.HeroIconSize),
                tint = OnboardingStyle.HeadingText
            )
            Spacer(Modifier.height(markGap))
        }
        Text(
            text = title,
            modifier = Modifier.fillMaxWidth(),
            style = titleStyle,
            color = OnboardingStyle.HeadingText,
            textAlign = TextAlign.Center
        )
    }
}
