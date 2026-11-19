package me.kavishdevar.librepods.presentation.overlays

import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.core.devices.AirPodsModel
import me.kavishdevar.librepods.core.devices.AirPodsSpec
import me.kavishdevar.librepods.core.devices.AirPodsSpecs
import me.kavishdevar.librepods.core.devices.Battery
import me.kavishdevar.librepods.core.devices.BatteryComponent
import me.kavishdevar.librepods.core.devices.BatterySource
import me.kavishdevar.librepods.core.devices.BatteryStatus
import me.kavishdevar.librepods.data.app.Design
import me.kavishdevar.librepods.data.app.DesignSystem
import me.kavishdevar.librepods.data.app.NightTheme
import me.kavishdevar.librepods.database.app.AppSettingsEntity
import me.kavishdevar.librepods.presentation.design.LibrePodsTheme
import me.kavishdevar.librepods.services.LibrePodsAccessibilityService
import kotlin.math.roundToInt

enum class ProximityCardState {
    VISIBLE,
    CLOSED
}

@Suppress("unused")
class ProximityCardWindow(
    private var context: Context,
    private var appSettings: AppSettingsEntity
) {
    private var windowManager: WindowManager = context.getSystemService(WindowManager::class.java)
    private var composeView: ComposeView? = null
    private var lifecycleOwner: ProxCardLifecycleOwner? = null

    private var screenContext: Context? = null
    private val screenReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: android.content.Intent?) {
            if (intent?.action == android.content.Intent.ACTION_SCREEN_OFF) forceClose()
        }
    }
    private fun observeScreen() {
        if (screenContext != null) return
        context.registerReceiver(screenReceiver, android.content.IntentFilter(android.content.Intent.ACTION_SCREEN_OFF), Context.RECEIVER_NOT_EXPORTED)
        screenContext = context
    }

    private val autoCloseHandler = Handler(Looper.getMainLooper())
    private var autoCloseRunnable: Runnable? = null

    private var currentAppSettings by mutableStateOf(appSettings)

    private var name by mutableStateOf("AirPods Pro2")
    private var batteries by mutableStateOf(emptySet<Battery>())
    private var spec by mutableStateOf(AirPodsSpecs.getSpec(AirPodsModel.AIRPODS_PRO_3))
    private var onConnect: () -> Unit = {}
    private var airPodsConnected by mutableStateOf(false)

    var usingAccessibilityService by mutableStateOf(false)
        private set

    init {
        usingAccessibilityService = context is LibrePodsAccessibilityService
    }

    var cardState by mutableStateOf(ProximityCardState.CLOSED)
        private set

    val isVisible: Boolean
        get() = composeView?.parent != null && composeView?.visibility == View.VISIBLE

    val isSamsungBannerVisible: Boolean
        get() = isVisible && appSettings.designSystem == DesignSystem.Samsung

    fun updateContext(context: Context) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Handler(Looper.getMainLooper()).post { updateContext(context) }
            return
        }

        forceClose()
        this.context = context
        this.usingAccessibilityService = context is LibrePodsAccessibilityService
        this.windowManager = context.getSystemService(WindowManager::class.java)
    }

    fun updateSettings(appSettings: AppSettingsEntity) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Handler(Looper.getMainLooper()).post { updateSettings(appSettings) }
            return
        }

        val designChanged = this.appSettings.designSystem != appSettings.designSystem
        this.appSettings = appSettings
        this.currentAppSettings = appSettings

        updateWindowParams()
        if (designChanged) scheduleBannerDismissal()
    }

    fun updateBatteries(batteries: Set<Battery>) {
        this.batteries = batteries
    }

    fun updateConnectionState(airPodsConnected: Boolean) {
        this.airPodsConnected = airPodsConnected
    }

    fun previewConnectionBanner() {
        if (appSettings.designSystem != DesignSystem.Samsung) return
        val timestamp = System.currentTimeMillis()
        show(
            name = context.getString(R.string.airpods_label),
            batteries = setOf(
                Battery(BatteryComponent.LEFT, 86, BatteryStatus.NOT_CHARGING, BatterySource.AACP, timestamp),
                Battery(BatteryComponent.RIGHT, 86, BatteryStatus.NOT_CHARGING, BatterySource.AACP, timestamp),
                Battery(BatteryComponent.CASE, 63, BatteryStatus.NOT_CHARGING, BatterySource.AACP, timestamp),
            ),
            airPodsConnected = true,
        )
    }

    fun show(
        name: String = this.name,
        batteries: Set<Battery> = this.batteries,
        spec: AirPodsSpec = AirPodsSpecs.getSpec(AirPodsModel.AIRPODS_PRO_3),
        onConnect: () -> Unit = this.onConnect,
        airPodsConnected: Boolean = this.airPodsConnected
    ) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Handler(Looper.getMainLooper()).post {
                show(name, batteries, spec, onConnect, airPodsConnected)
            }
            return
        }

        if (!context.getSystemService(android.os.PowerManager::class.java).isInteractive ||
            context.getSystemService(android.app.KeyguardManager::class.java).isKeyguardLocked ||
            (!usingAccessibilityService && !android.provider.Settings.canDrawOverlays(context))) return

        this.name = name
        this.batteries = batteries
        this.spec = spec
        this.onConnect = onConnect
        this.cardState = ProximityCardState.VISIBLE
        this.airPodsConnected = airPodsConnected

        if (composeView == null) {
            setupComposeView()
        }

        if (composeView?.parent == null) {
            val params = createLayoutParams()
            try {
                lifecycleOwner?.start()
                windowManager.addView(composeView, params)
                composeView?.visibility = View.VISIBLE
                observeScreen()
            } catch (e: Exception) {
                Log.e("ProximityCardWindow", "Error adding overlay view: $e")
                forceClose()
            }
        } else {
            updateWindowParams()
        }
        scheduleBannerDismissal()
    }

    private fun scheduleBannerDismissal() {
        autoCloseRunnable?.let(autoCloseHandler::removeCallbacks)
        autoCloseRunnable = if (isSamsungBannerVisible) {
            Runnable(::close).also { autoCloseHandler.postDelayed(it, 5_000L) }
        } else null
    }

    private fun updateWindowParams() {
        if (composeView?.parent != null) {
            val params = createLayoutParams()
            windowManager.updateViewLayout(composeView, params)
        }
    }

    private fun isTablet(): Boolean {
        return (context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        val windowType = if (context is android.accessibilityservice.AccessibilityService) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }

        val cardSettings = appSettings.proximityCardSettings
        val density = context.resources.displayMetrics.density
        val margin = (16 * density).roundToInt()
        val availableWidth = windowManager.currentWindowMetrics.bounds.width() - 2 * margin
        val samsung = appSettings.designSystem == DesignSystem.Samsung
        val width = if (samsung) WindowManager.LayoutParams.WRAP_CONTENT
            else minOf(cardSettings.width.toInt() - 2 * margin, availableWidth).coerceAtLeast(1)
        val height = if (samsung) WindowManager.LayoutParams.WRAP_CONTENT
            else (cardSettings.height.toInt() - 2 * margin).coerceAtLeast(1)
        val isTabletDevice = isTablet() && cardSettings.centerOnTablet

        return WindowManager.LayoutParams(
            width,
            height,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                (if (samsung) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else WindowManager.LayoutParams.FLAG_SPLIT_TOUCH),
            PixelFormat.TRANSPARENT
        ).apply {
            setFitInsetsTypes(android.view.WindowInsets.Type.systemBars() or android.view.WindowInsets.Type.displayCutout())
            setFitInsetsIgnoringVisibility(true)
            gravity = when {
                samsung -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
                isTabletDevice -> Gravity.CENTER
                else -> Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }

            x = 0
            y = when {
                // Easy Setup adds 16 raw pixels to the 16dp top inset of its drawable.
                samsung -> margin + 16
                isTabletDevice -> 0
                else -> cardSettings.offsetY.roundToInt() + margin
            }

            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
        }
    }

    private fun setupComposeView() {
        lifecycleOwner = ProxCardLifecycleOwner()

        composeView = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)

            setContent {
                val darkTheme = when (currentAppSettings.nightMode) {
                    NightTheme.Dark -> true
                    NightTheme.Light -> false
                    NightTheme.System -> isSystemInDarkTheme()
                }

                LibrePodsTheme(
                    design = Design(
                        system = currentAppSettings.designSystem,
                        colorStyle = currentAppSettings.colorStyle,
                        overrideMaterialColor = currentAppSettings.overrideMaterialColor,
                        contrastLevel = currentAppSettings.contrastLevel,
                        iconStyle = currentAppSettings.iconStyle
                    ),
                    accessibilitySettings = currentAppSettings.accessibilitySettings,
                    fontSettings = currentAppSettings.fontSettings,
                    darkTheme = darkTheme
                ) {
                    val onClosed = ::cleanupAndRemoveView
                    if (currentAppSettings.designSystem == DesignSystem.Samsung) {
                        SamsungConnectionBanner(
                            name = name,
                            batteries = batteries,
                            darkTheme = darkTheme,
                            cardState = cardState,
                            onClosed = onClosed,
                        )
                    } else {
                        ProximityCardContent(
                            name = name,
                            batteries = batteries,
                            settings = currentAppSettings.proximityCardSettings,
                            primaryImageRes = spec.primaryImageRes,
                            caseImageRes = spec.caseImageRes,
                            videoResId = spec.videoResId,
                            cardState = cardState,
                            onConnect = onConnect,
                            onClosed = onClosed,
                            airPodsConnected = airPodsConnected,
                        )
                    }
                }
            }
        }
    }

    fun close() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Handler(Looper.getMainLooper()).post { close() }
            return
        }

        autoCloseRunnable?.let(autoCloseHandler::removeCallbacks)
        autoCloseRunnable = null
        cardState = ProximityCardState.CLOSED
    }

    /** Immediate teardown for screen-off/disconnect; animated close is only for ordinary dismissal. */
    fun forceClose() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Handler(Looper.getMainLooper()).post { forceClose() }
            return
        }
        cleanupAndRemoveView()
    }

    private fun cleanupAndRemoveView() {
        autoCloseRunnable?.let(autoCloseHandler::removeCallbacks)
        autoCloseRunnable = null
        screenContext?.let { runCatching { it.unregisterReceiver(screenReceiver) } }
        screenContext = null
        val view = composeView
        val owner = lifecycleOwner
        composeView = null
        lifecycleOwner = null
        cardState = ProximityCardState.CLOSED
        onConnect = {}
        if (view?.parent != null) runCatching { windowManager.removeView(view) }
            .onFailure { Log.w("OverlayWindow", "Could not remove overlay", it) }
        runCatching { view?.disposeComposition() }
        runCatching { owner?.stop() }
    }

}

private class ProxCardLifecycleOwner: LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle
        field = LifecycleRegistry(this)

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    fun start() {
        savedStateRegistryController.performRestore(null)
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun stop() {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}
