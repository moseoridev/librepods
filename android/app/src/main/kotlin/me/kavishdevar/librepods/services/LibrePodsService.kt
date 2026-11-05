package me.kavishdevar.librepods.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.ParcelUuid
import android.os.ext.SdkExtensions
import android.provider.Settings
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.kavishdevar.librepods.LibrePodsApplication
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.audio.EldDecoder
import me.kavishdevar.librepods.audio.WavWriter
import me.kavishdevar.librepods.core.bluetooth.MacAddress
import me.kavishdevar.librepods.core.bluetooth.aacp.types.ControlCommandIdentifier
import me.kavishdevar.librepods.core.bluetooth.aacp.types.MagicKeyType
import me.kavishdevar.librepods.core.data.apple.AccessoryState
import me.kavishdevar.librepods.core.data.heartrate.HeartRateSample
import me.kavishdevar.librepods.core.data.recording.Recording
import me.kavishdevar.librepods.core.devices.AirPodsSpecs
import me.kavishdevar.librepods.core.devices.AppleDevice
import me.kavishdevar.librepods.core.data.StemAction
import me.kavishdevar.librepods.core.bluetooth.aacp.types.AppleEvent
import me.kavishdevar.librepods.core.devices.handlesStemType
import me.kavishdevar.librepods.core.devices.stemAction
import me.kavishdevar.librepods.core.devices.configuredListeningModes
import me.kavishdevar.librepods.core.devices.BaseCapability
import me.kavishdevar.librepods.core.devices.AppleSettings
import me.kavishdevar.librepods.core.devices.AppleState
import me.kavishdevar.librepods.core.devices.BatteryComponent
import me.kavishdevar.librepods.core.devices.BatteryStatus
import me.kavishdevar.librepods.core.devices.ComponentStatus
import me.kavishdevar.librepods.core.devices.ConnectionState
import me.kavishdevar.librepods.core.devices.Device
import me.kavishdevar.librepods.core.devices.DeviceComponent
import me.kavishdevar.librepods.core.devices.DeviceComponentState
import me.kavishdevar.librepods.core.devices.format
import me.kavishdevar.librepods.core.devices.hasSameState
import me.kavishdevar.librepods.core.utils.redactMac
import me.kavishdevar.librepods.data.app.LibrePodsIntents
import me.kavishdevar.librepods.database.app.AppSettingsEntity
import me.kavishdevar.librepods.presentation.activities.MainActivity
import me.kavishdevar.librepods.presentation.overlays.IslandType
import me.kavishdevar.librepods.presentation.overlays.IslandWindow
import me.kavishdevar.librepods.presentation.overlays.ProximityCardWindow
import me.kavishdevar.librepods.presentation.widgets.BatteryWidget
import me.kavishdevar.librepods.utils.MediaController
import me.kavishdevar.librepods.utils.calculateLevel
import java.time.ZoneOffset
import kotlin.collections.getOrNull
import kotlin.time.Duration
import kotlin.time.toJavaInstant

private const val TAG = "LibrePodsService"

@SuppressLint("MissingPermission")
class LibrePodsService: Service() {
    inner class LocalBinder : Binder() {
        fun getService(): LibrePodsService = this@LibrePodsService
    }

    private val binder = LocalBinder()

    private val _devices = MutableStateFlow<Map<MacAddress, Device<*, *, *>>>(emptyMap())
    val devices = _devices.asStateFlow()

    private val serviceCoroutineScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var runtimeActive = false
    private var phoneRegistered = false
    private var destroying = false


    private val eldDecoder = EldDecoder()

    private val wavWriters = mutableMapOf<Recording, WavWriter>()

    private val appleRepository by lazy {
        (application as LibrePodsApplication).appleRepository
    }

    private val appDataRepository by lazy {
        (application as LibrePodsApplication).appDataRepository
    }

    private val widgetConfigRepository by lazy {
        (application as LibrePodsApplication).widgetConfigRepository
    }

    private val heartRateRepository by lazy {
        (application as LibrePodsApplication).heartRateRepository
    }


    @Suppress("unused")
    private val recordingRepository by lazy {
        (application as LibrePodsApplication).recordingRepository
    }

    private val healthConnectClient by lazy {
        (application as LibrePodsApplication).healthConnectClient
    }

    private val islandWindow: IslandWindow by lazy {
        (application as LibrePodsApplication).islandWindow
    }

    private val proximityCardWindow: ProximityCardWindow by lazy {
        (application as LibrePodsApplication).proximityCardWindow
    }

    @Suppress("unused")
    private fun observeCameraState() {
        serviceCoroutineScope.launch {
            LibrePodsAccessibilityService.isCameraOpen.collect { isCamera ->
                if (isCamera) {
                    Log.d(TAG, "Camera is open")
                } else {
                    Log.d(TAG, "Camera is closed")
                }
            }
        }
    }

    private var isCallRinging = false
    private var callGestureOwner: AppleDevice? = null
    private fun stopCallGesture() {
        callGestureOwner?.stopHeadTracking()
        callGestureOwner = null
    }

    private val telephonyCallback = object: TelephonyCallback(), TelephonyCallback.CallStateListener {
        override fun onCallStateChanged(state: Int) {
            if (state == TelephonyManager.CALL_STATE_RINGING) {
                stopCallGesture()
                isCallRinging = true
                attemptConnect("phone")
                _devices.value.values.firstOrNull { device ->
                    device is AppleDevice && device.settings.value.headGesturesEnabled &&
                    device.connectionState.value == ConnectionState.CONNECTED &&
                    device.state.value.componentState.any { it.status == ComponentStatus.IN_EAR }
                }?.let { device ->
                    callGestureOwner = device as AppleDevice
                    device.detectHeadGestures { accept ->
                        if (!isCallRinging || callGestureOwner !== device || !device.settings.value.headGesturesEnabled ||
                            device.connectionState.value != ConnectionState.CONNECTED) return@detectHeadGestures

                        try {
                            val telecomManager = getSystemService(TelecomManager::class.java)

                            @Suppress("DEPRECATION")
                            if (accept) {
                                telecomManager?.acceptRingingCall(
                                    VideoProfile.STATE_AUDIO_ONLY
                                )
                                Toast.makeText(
                                    this@LibrePodsService,
                                    getString(R.string.call_accepted),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    this@LibrePodsService,
                                    getString(R.string.call_rejected),
                                    Toast.LENGTH_SHORT
                                ).show()
                                telecomManager?.endCall()
                            }

                            isCallRinging = false
                            stopCallGesture()

                        } catch (e: Exception) {
                            Log.e(TAG, "Error accepting call", e)
                        }
                    }
                }
            } else {
                stopCallGesture()
                isCallRinging = false
            }
        }
    }

    private data class OverlayOwner(val device: AppleDevice, val generation: Int)
    private val pendingIsland = me.kavishdevar.librepods.presentation.overlays.policy.PendingIslandRequest<OverlayOwner>()
    private var pendingIslandExpiry: kotlinx.coroutines.Job? = null
    private var islandOwner: OverlayOwner? = null
    private var bottomOwner: OverlayOwner? = null
    private val connectionPopupPolicy by lazy { me.kavishdevar.librepods.presentation.overlays.policy.PopupMarkers.policy(this) }
    private var observingDisplay = false
    private val displayReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> { islandWindow.forceClose(); proximityCardWindow.forceClose(); islandOwner = null; bottomOwner = null }
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> showPendingIsland()
            }
        }
    }
    private fun displayReady() = getSystemService(android.os.PowerManager::class.java).isInteractive &&
        !getSystemService(android.app.KeyguardManager::class.java).isKeyguardLocked
    private fun current(owner: OverlayOwner) = devices.value[owner.device.macAddress] === owner.device &&
        owner.device.connectionState.value == ConnectionState.CONNECTED && owner.device.connectionNumber.value == owner.generation
    private fun clearPendingIsland() { pendingIsland.clear(); pendingIslandExpiry?.cancel(); pendingIslandExpiry = null }
    private fun islandBattery(device: AppleDevice): Int? {
        val state = device.state.value
        if (!state.batteryReceived) return null
        return state.battery.filter { it.component != BatteryComponent.CASE && it.level in 0..100 &&
            it.status in setOf(BatteryStatus.NOT_CHARGING, BatteryStatus.CHARGING, BatteryStatus.OPTIMIZED_CHARGING) }
            .minOfOrNull { it.level }
    }
    private fun showPendingIsland() {
        if (pendingIsland.owner == null) return
        val ready = displayReady() && pendingIsland.owner?.device?.let { islandBattery(it) } != null
        val owner = pendingIsland.consume(android.os.SystemClock.elapsedRealtime(), ready, ::current) ?: return
        pendingIslandExpiry?.cancel()
        showIsland(owner.device)
    }
    private fun closeOwnedPopups(device: AppleDevice) {
        pendingIsland.clearOwner { it.device === device }
        if (pendingIsland.owner == null) { pendingIslandExpiry?.cancel(); pendingIslandExpiry = null }
        if (islandOwner?.device === device) { islandWindow.forceClose(); islandOwner = null }
        if (bottomOwner?.device === device) { proximityCardWindow.forceClose(); bottomOwner = null }
    }
    private fun showConnectionPopup(device: AppleDevice, state: AppleState) {
        val usable = state.battery.any { it.source == me.kavishdevar.librepods.core.devices.BatterySource.AACP &&
            it.level in 0..100 && it.status in setOf(BatteryStatus.NOT_CHARGING, BatteryStatus.CHARGING, BatteryStatus.OPTIMIZED_CHARGING) }
        if (!connectionPopupPolicy.consumeReady(device.macAddress.value,
                device.connectionState.value == ConnectionState.CONNECTED, state.batteryReceived, usable)) return
        // Consumption precedes preference/permission/display checks: no hidden bottom-card replay.
        if (!device.settings.value.showBottomSheetPopup || !displayReady() ||
            (!proximityCardWindow.usingAccessibilityService && !Settings.canDrawOverlays(this))) return
        clearPendingIsland()
        islandWindow.forceClose()
        islandOwner = null
        proximityCardWindow.forceClose()
        bottomOwner = OverlayOwner(device, device.connectionNumber.value)
        proximityCardWindow.show(device.metadata.value.name, state.battery,
            AirPodsSpecs.getSpec(device.metadata.value.model), { device.connect() }, true)
    }

    private val releasedAudioDevices = mutableSetOf<MacAddress>()
    private val reversedDevices = mutableSetOf<MacAddress>()
    private val routingJobs = mutableMapOf<MacAddress, Job>()
    private val routingAttemptAt = mutableMapOf<MacAddress, Long>()

    private fun selfMacAddress(): String? {
        val saved = getSharedPreferences("settings", MODE_PRIVATE).getString("self_mac_address", null)
        val raw = saved ?: try { getSystemService(BluetoothManager::class.java).adapter?.address } catch (_: SecurityException) { null }
        return raw?.uppercase()?.takeIf { it.matches(Regex("([0-9A-F]{2}:){5}[0-9A-F]{2}")) &&
            it != "02:00:00:00:00:00" && it != "00:00:00:00:00:00" }
    }

    private fun attemptConnect(trigger: String) {
        devices.value.values.filterIsInstance<AppleDevice>().forEach { device ->
            val settings = device.settings.value
            val call = trigger == "phone"
            val allowed = when (device.state.value.state) {
                AccessoryState.Disconnected -> if (call) settings.connectOnPhoneWhenAccessoryDisconnected else settings.connectOnMediaWhenAccessoryDisconnected
                AccessoryState.Idle -> if (call) settings.connectOnPhoneWhenAccessoryIdle else settings.connectOnMediaWhenAccessoryIdle
                AccessoryState.Music -> if (call) settings.connectOnPhoneWhenAccessoryMusic else settings.connectOnMediaWhenAccessoryMusic
                else -> false
            }
            if (allowed) requestAudioTakeover(device, manual = false)
        }
        me.kavishdevar.librepods.bluetooth.NearbyDetection.considerTakeover(this)
    }

    fun requestAudioTakeover(device: AppleDevice, manual: Boolean = true, reverse: Boolean = false) {
        if (destroying || device.connectionState.value != ConnectionState.CONNECTED ||
            (!manual && device.macAddress in reversedDevices)) return
        val self = selfMacAddress() ?: return
        if (!reverse && !me.kavishdevar.librepods.data.xposed.XposedRemotePrefProvider.create().getBoolean("vendor_id_hook", false)) return
        val state = device.state.value
        if (!manual && state.owns && state.audioSource.mac.value.equals(self, true)) return
        val now = android.os.SystemClock.elapsedRealtime()
        // Bounded automatic work while ownership/OS state is stale. User actions bypass this cooldown.
        if (!manual && now - (routingAttemptAt[device.macAddress] ?: -60_000L) < 60_000) return
        if (routingJobs[device.macAddress]?.isActive == true) {
            if (!manual) return
            routingJobs.remove(device.macAddress)?.cancel()
        }
        routingAttemptAt[device.macAddress] = now
        val generation = device.connectionNumber.value
        val current = { !destroying && devices.value[device.macAddress] === device &&
            device.connectionState.value == ConnectionState.CONNECTED && device.connectionNumber.value == generation }
        routingJobs[device.macAddress] = serviceCoroutineScope.launch {
            if (!current()) return@launch
            device.setControlCommand(ControlCommandIdentifier.OWNS_CONNECTION, 1)
            device.aacp.sendMediaInformataion(self)
            if (reverse) device.aacp.sendHijackReversed(self) else {
                device.aacp.sendSmartRoutingShowUI(self)
                device.aacp.sendHijackRequest(self)
            }
            me.kavishdevar.librepods.bluetooth.setAudioRouting(device, true, current)
            // Ownership acknowledgement, not request acceptance, clears the reconnect state below.
        }
    }

    fun onNearbySnapshots(samples: Map<MacAddress, me.kavishdevar.librepods.bluetooth.NearbyDetection.Snapshot>) {
        devices.value.values.filterIsInstance<AppleDevice>().forEach { device ->
            samples[device.macAddress]?.let { device.acceptNearby(it.batteries, it.status) }
                ?: device.expireNearby()
        }
    }

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel("hrm_alert", getString(R.string.heart_rate_alert), NotificationManager.IMPORTANCE_HIGH))
        observeAppSettings()
        loadDevices()
        me.kavishdevar.librepods.bluetooth.CompanionConnection.attach(this)
        onNearbySnapshots(me.kavishdevar.librepods.bluetooth.NearbyDetection.currentSnapshots())
        me.kavishdevar.librepods.bluetooth.CompanionConnection.reconcile(this)
    }

    /** Called by finite, generation-checked classic queries and explicit disconnect events. */
    fun reconcileClassicConnections(connected: Map<String, BluetoothDevice>) {
        if (destroying) return
        connected.values.forEach { onDeviceConnected(it) }
        devices.value.values.filterIsInstance<AppleDevice>().filter { it.hasClassicConnection }.forEach { device ->
            if (connected.keys.none { it.equals(device.macAddress.value, true) })
                device.onClassicConnectionChanged(false)
        }
        reconcileRuntime()
    }

    private fun reconcileRuntime() {
        if (destroying) return
        val wanted = devices.value.values.any { device ->
            device.connectionState.value in setOf(ConnectionState.CONNECTING, ConnectionState.CONNECTED)
        }
        if (wanted == runtimeActive) return
        runtimeActive = wanted
        if (wanted) {
            me.kavishdevar.librepods.core.utils.GestureFeedback.init(this)
            synchronizeWidgetBatteryReceiver()
            if (!observingDisplay) {
                registerReceiver(displayReceiver, IntentFilter().apply {
                    addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_USER_PRESENT)
                }, RECEIVER_NOT_EXPORTED)
                observingDisplay = true
            }
            MediaController.initialize(getSystemService(AudioManager::class.java),
                getSharedPreferences("settings", MODE_PRIVATE), selfMacAddress()) { attemptConnect("media") }
            if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
                getSystemService(TelephonyManager::class.java).registerTelephonyCallback(mainExecutor, telephonyCallback)
                phoneRegistered = true
            }
        } else stopRuntime()
    }

    private fun stopRuntime() {
        isCallRinging = false
        stopCallGesture()
        runtimeActive = false
        clearPendingIsland()
        islandWindow.forceClose(); proximityCardWindow.forceClose()
        islandOwner = null; bottomOwner = null
        if (observingDisplay) { runCatching { unregisterReceiver(displayReceiver) }; observingDisplay = false }
        synchronizeWidgetBatteryReceiver()
        me.kavishdevar.librepods.core.utils.GestureFeedback.release()
        MediaController.release()
        if (phoneRegistered) {
            getSystemService(TelephonyManager::class.java).unregisterTelephonyCallback(telephonyCallback)
            phoneRegistered = false
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            when (intent.action) {
                LibrePodsIntents.ACTION_CONNECT -> {
                    val macAddress = intent.getStringExtra(LibrePodsIntents.Extras.MAC_ADDRESS)?.let { MacAddress(it) }
                    val device = devices.value[macAddress]
                    device?.connect()
                }
                LibrePodsIntents.ACTION_SET_ANC_MODE -> {
                    val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                    val macAddress = intent.getStringExtra(LibrePodsIntents.Extras.MAC_ADDRESS)?.let { MacAddress(it) }

                    val ancMode = intent.getIntExtra(LibrePodsIntents.Extras.ANC_MODE, -1)
                    val device =
                        widgetConfigRepository.widgetConfigs.value.find { it.appWidgetId == appWidgetId }
                            ?.let { config ->
                                devices.value[config.macAddress]
                            } ?: devices.value[macAddress]
                        ?: devices.value.values.firstOrNull { it.connectionState.value == ConnectionState.CONNECTED }

                    if (device != null && ancMode != -1) {
                        Log.i(
                            TAG,
                            "Setting ANC mode to $ancMode for device ${device.macAddress.toRedactedString()}"
                        )
                        when (device) {
                            is AppleDevice -> device.setControlCommand(
                                ControlCommandIdentifier.LISTENING_MODE,
                                ancMode
                            )
                        }
                    } else {
                        Log.w(TAG, "No connected Apple device found or invalid ANC mode: $ancMode")
                    }
                }
            }
        }

        stopSelf(startId) // UI/CDM bindings, not a sticky started service, own the engine.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        destroying = true
        me.kavishdevar.librepods.bluetooth.CompanionConnection.detach(this)
        stopRuntime()
        devices.value.values.filterIsInstance<AppleDevice>().forEach { it.close() }
        val notifications = getSystemService(NotificationManager::class.java)
        connectionNotifications.keys.forEach { notifications.cancel(it.value, 2) }
        connectionNotifications.clear()
        serviceCoroutineScope.cancel("Service stopped")
        me.kavishdevar.librepods.presentation.widgets.WidgetPublisher.update(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private fun onDeviceConnected(bluetoothDevice: BluetoothDevice) {
        if (devices.value[MacAddress(bluetoothDevice.address)]?.let { it is AppleDevice && it.hasClassicConnection } == true) {
            Log.d(TAG, "Device already connected: ${bluetoothDevice.address}")
            return
        }

        val device = devices.value[MacAddress(bluetoothDevice.address)] ?: createDevice(bluetoothDevice)

        if (device == null) {
            Log.d(TAG, "Unsupported device connected: ${bluetoothDevice.address}")
            return
        }

        _devices.update { it + (device.macAddress to device) }
        if (device is AppleDevice) device.onClassicConnectionChanged(true) else device.connect()

        Log.i(
            TAG,
            "Device connected: ${device.macAddress.toRedactedString()} (${device.javaClass.simpleName})"
        )

    }

    private fun onDeviceDisconnected(mac: MacAddress) {
        Log.i(TAG, "Device disconnected: $mac")
        val device = devices.value[mac]?: return
        if (device is AppleDevice) device.onClassicConnectionChanged(false) else device.disconnect()
        updateDeviceNotification(device = device)
    }

    private fun loadDevices() {
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return
        val bluetoothAdapter = getSystemService(BluetoothManager::class.java)?.adapter ?: return
        if (!bluetoothAdapter.isEnabled) return
        val bondedDevices = bluetoothAdapter.bondedDevices

        bondedDevices.forEach { bluetoothDevice ->
            val device = createDevice(bluetoothDevice)
            if (device != null) {
                val notificationManager = getSystemService(NotificationManager::class.java)
                val channel = NotificationChannel(
                    "device_${device.macAddress}",
                    "Device ${device.metadata.value.name}",
                    NotificationManager.IMPORTANCE_LOW
                )
                notificationManager.createNotificationChannel(channel)

                _devices.update { it + (device.macAddress to device) }
            }
        }
    }

    private fun createDevice(
        bluetoothDevice: BluetoothDevice
    ): Device<*, *, *>? {

        val aacpUuid = ParcelUuid.fromString("74ec2172-0bad-4d01-8f77-997b2be0722a")

        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter = bluetoothManager.adapter

        val device = when {
            bluetoothDevice.uuids?.contains(aacpUuid) == true ||
                me.kavishdevar.librepods.bluetooth.CompanionConnection.associations(this).any {
                    it.deviceMacAddress?.toString().equals(bluetoothDevice.address, true)
                } -> {
                Log.i(TAG, "Apple device detected: ${bluetoothDevice.address.redactMac()}")
                AppleDevice(
                    context = this,
                    bluetoothAdapter = bluetoothAdapter,
                    bluetoothDevice = bluetoothDevice,
                    currentState = ConnectionState.DISCONNECTED
                )
            }

            else -> null
        }

        when (device) {
            is AppleDevice -> serviceCoroutineScope.launch {
                Log.i(TAG, "Loading device ${device.macAddress.toRedactedString()} from db")

                appleRepository.load(device.macAddress)?.let { entity ->
                    val cache = entity.cache

                    // Cached keys are decoded by the independent Nearby path.
                    Log.i(
                        TAG,
                        "Loaded cached state for device ${device.macAddress.toRedactedString()}: $cache"
                    )
                    val settings = entity.settings
                    Log.i(
                        TAG,
                        "Loaded settings for device ${device.macAddress.toRedactedString()}: $settings"
                    )
                    val metadata = entity.metadata
                    Log.i(
                        TAG,
                        "Loaded metadata for device ${device.macAddress.toRedactedString()}: $metadata"
                    )

                    device.loadInitialState(
                        state = AppleState().copy(
                            capabilities = cache.capabilities,
                            magicKeys = cache.magicKeys,
                            controlStates = cache.controlStates,
                        ),
                        settings = settings,
                        metadata = metadata
                    )
                }

                if (!device.settingsInitialized) device.loadInitialState(AppleState(), AppleSettings(), device.metadata.value)
                device.onManualDisconnect = { me.kavishdevar.librepods.bluetooth.NearbyDetection.suppress(this@LibrePodsService, device.macAddress) }
                observeAppleEvents(device)
                observeAppleState(device)
                observeAppleSettings(device)
                observeAppleMetadata(device)
                observeAppleMicrophoneFrames(device)
            }
        }

        if (device != null) {
            observeConnectionState(device)
        }

        return device
    }

    fun observeConnectionState(device: Device<*, *, *>) {
        serviceCoroutineScope.launch {
            var previousConnectionState = ConnectionState.DISCONNECTED
            device.connectionState.collect { state ->
                reconcileRuntime()
                updateWidgets()
                updateDeviceNotification(device)
                if (device is AppleDevice && state != ConnectionState.CONNECTED) {
                    routingJobs.remove(device.macAddress)?.cancel()
                    if (state != ConnectionState.CONNECTING) {
                        reversedDevices.remove(device.macAddress); releasedAudioDevices.remove(device.macAddress)
                        routingAttemptAt.remove(device.macAddress); closeOwnedPopups(device)
                    }
                }
                if (bottomOwner?.device === device && proximityCardWindow.isVisible)
                    proximityCardWindow.updateConnectionState(state == ConnectionState.CONNECTED)

                previousConnectionState = state
            }
        }
    }

    fun observeAppSettings(): Job {
        var oldAppSettings: AppSettingsEntity = appDataRepository.settings.value

        return serviceCoroutineScope.launch {
            appDataRepository.settings.collect { settings ->
                islandWindow.updateSettings(settings)
                oldAppSettings = settings
            }
        }
    }

    private fun releaseAudioToPeer(device: AppleDevice) {
        if (!releasedAudioDevices.add(device.macAddress)) return
        val generation = device.connectionNumber.value
        routingJobs.remove(device.macAddress)?.cancel()
        routingJobs[device.macAddress] = serviceCoroutineScope.launch {
            me.kavishdevar.librepods.bluetooth.setAudioRouting(device, false) {
                !destroying && devices.value[device.macAddress] === device &&
                    device.connectionNumber.value == generation && device.connectionState.value == ConnectionState.CONNECTED
            }
        }
    }

    private fun observeAppleEvents(device: AppleDevice) {
        serviceCoroutineScope.launch {
            device.events.collect { event ->
                if (devices.value[device.macAddress] !== device || device.connectionState.value != ConnectionState.CONNECTED) return@collect
                if (event is AppleEvent.OwnershipToFalseRequest) {
                    device.setControlCommand(ControlCommandIdentifier.OWNS_CONNECTION, 0)
                    MediaController.sendPause()
                    if (event.reverseTapped) reversedDevices.add(device.macAddress)
                    releaseAudioToPeer(device)
                    updateDeviceNotification(device)
                    showIsland(device, IslandType.MOVED_TO_OTHER_DEVICE, event.reverseTapped,
                        device.state.value.connectedDevices.find { it.macAddress.value.equals(event.sender, true) }?.type)
                    return@collect
                }
                if (event is AppleEvent.ShowNearbyUi) {
                    showIsland(device, IslandType.MOVED_TO_OTHER_DEVICE, device.macAddress in reversedDevices,
                        device.state.value.connectedDevices.find { it.macAddress.value.equals(event.sender, true) }?.type)
                    return@collect
                }
                if (event !is AppleEvent.StemPress || !device.settings.value.handlesStemType(event.pressType)) return@collect
                when (device.settings.value.stemAction(event.bud, event.pressType)) {
                    StemAction.PLAY_PAUSE -> MediaController.sendPlayPause()
                    StemAction.NEXT_TRACK -> MediaController.sendNextTrack()
                    StemAction.PREVIOUS_TRACK -> MediaController.sendPreviousTrack()
                    StemAction.DIGITAL_ASSISTANT -> try {
                        startActivity(Intent(Intent.ACTION_VOICE_COMMAND).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (error: android.content.ActivityNotFoundException) {
                        Log.w(TAG, "No voice assistant available", error)
                    } catch (error: SecurityException) {
                        Log.w(TAG, "Voice assistant launch unavailable", error)
                    }
                    StemAction.CYCLE_listening_mode_MODES -> {
                        val state = device.state.value
                        val mask = state.controlStates[ControlCommandIdentifier.LISTENING_MODE_CONFIGS]?.firstOrNull()?.toInt() ?: return@collect
                        val modes = configuredListeningModes(mask,
                            BaseCapability.ADAPTIVE_AUDIO in AirPodsSpecs.getSpec(device.metadata.value.model).baseCapabilities,
                            state.controlStates[ControlCommandIdentifier.ALLOW_OFF_OPTION]?.firstOrNull()?.toInt() == 1)
                        val current = state.confirmedControlStates[ControlCommandIdentifier.LISTENING_MODE]?.firstOrNull()?.toInt()
                        if (modes.isNotEmpty() && current != null) device.setControlCommand(ControlCommandIdentifier.LISTENING_MODE,
                            modes[(modes.indexOf(current) + 1) % modes.size])
                    }
                }
            }
        }
    }

    fun observeAppleState(device: AppleDevice) {
        serviceCoroutineScope.launch {
            var previousState = device.state.value

            device.state.collect { state ->
                val deviceSettings = device.settings.value
                if (state.owns != previousState.owns) {
                    if (!state.owns) { MediaController.sendPause(); releaseAudioToPeer(device) }
                    else {
                        releasedAudioDevices.remove(device.macAddress)
                        reversedDevices.remove(device.macAddress)
                        updateDeviceNotification(device)
                    }
                }

                if (state.aacpPackets != previousState.aacpPackets) {
                    if (!appDataRepository.state.value.hasConnectedToAACP) {
                        appDataRepository.updateState { it.copy(hasConnectedToAACP = true) }
                    }
                    if (appDataRepository.state.value.firstSuccessfulConnectionTime == null) {
                        appDataRepository.updateState { it.copy(firstSuccessfulConnectionTime = System.currentTimeMillis()) }
                    }
                }

                if (state.state != previousState.state) {
                    if (state.state == AccessoryState.Disconnected) {
                        if (deviceSettings.alwaysConnectWhenAccessoryDisconnected && System.currentTimeMillis() - state.lastDisconnected > 5_000) {
                            Log.d(TAG, "device ${device.macAddress.toRedactedString()} is now disconnected, connecting")
                        }
                    }

                    if (MediaController.getMusicActive()) {
                        attemptConnect("media")
                    }
                }

                if (state.capabilities != previousState.capabilities || state.magicKeys != previousState.magicKeys) {
                    Log.i(
                        TAG,
                        "capabilities changed for device ${device.macAddress.toRedactedString()}"
                    )
                    Log.d(TAG, "capabilities: ${state.capabilities}")

                    if (state.capabilities.isNotEmpty() || state.magicKeys.isNotEmpty()) {
                        appleRepository.saveCacheFromState(device.macAddress, state)
                        me.kavishdevar.librepods.bluetooth.NearbyDetection.keysChanged(this@LibrePodsService)
                    }
                }

                if (state.batteryReceived && (!previousState.batteryReceived || !state.battery.hasSameState(previousState.battery))) {
                    showConnectionPopup(device, state)
                    showPendingIsland()
                }

                if (!state.battery.hasSameState(previousState.battery)) {
                    Log.i(
                        TAG,
                        "battery state changed for device ${device.macAddress.toRedactedString()}"
                    )
                    Log.d(TAG, "battery state: ${state.battery.format()}. prev: ${previousState.battery.format()}")

                    Log.d(TAG, "updating widgets")
                    updateWidgets()

                    if (islandOwner?.device === device && islandWindow.isVisible) {
                        Log.d(TAG, "updating island window")
                        val leftBattery = state.battery.find { it.component == BatteryComponent.LEFT }
                        val rightBattery = state.battery.find { it.component == BatteryComponent.RIGHT }

                        val leftLevel = leftBattery?.level ?: 0
                        val rightLevel = rightBattery?.level ?: 0

                        val displayBatteryLevel = when {
                            leftLevel > 0 && rightLevel > 0 -> minOf(leftLevel, rightLevel)
                            leftLevel > 0 -> leftLevel
                            rightLevel > 0 -> rightLevel
                            else -> 0
                        }

                        islandWindow.updateBattery(displayBatteryLevel)
                    }

                    if (bottomOwner?.device === device && proximityCardWindow.isVisible) proximityCardWindow.updateBatteries(state.battery)

                    updateDeviceNotification(device = device)

                    // TODO:Shizuku
                    /* Log.d(TAG, "updating bluetooth metadata")
                    shizuku.setMetadata(
                        device.macAddress.value,
                        BluetoothMetadata.METADATA_UNTETHERED_CASE_BATTERY,
                        state.battery.find { it.component == BatteryComponent.CASE }?.level.toString()
                            .toByteArray()
                    )
                    shizuku.setMetadata(
                        device.macAddress.value,
                        BluetoothMetadata.METADATA_UNTETHERED_CASE_CHARGING,
                        (if (state.battery.find { it.component == BatteryComponent.CASE }?.status == BatteryStatus.CHARGING
                            || state.battery.find { it.component == BatteryComponent.CASE }?.status == BatteryStatus.OPTIMIZED_CHARGING
                        ) "1" else "0").toByteArray()
                    )
                    shizuku.setMetadata(
                        device.macAddress.value,
                        BluetoothMetadata.METADATA_UNTETHERED_LEFT_BATTERY,
                        state.battery.find { it.component == BatteryComponent.LEFT }?.level.toString()
                            .toByteArray()
                    )
                    shizuku.setMetadata(
                        device.macAddress.value,
                        BluetoothMetadata.METADATA_UNTETHERED_LEFT_CHARGING,
                        (if (state.battery.find { it.component == BatteryComponent.LEFT }?.status == BatteryStatus.CHARGING
                            || state.battery.find { it.component == BatteryComponent.LEFT }?.status == BatteryStatus.OPTIMIZED_CHARGING
                        ) "1" else "0").toByteArray()
                    )
                    shizuku.setMetadata(
                        device.macAddress.value,
                        BluetoothMetadata.METADATA_UNTETHERED_RIGHT_BATTERY,
                        state.battery.find { it.component == BatteryComponent.RIGHT }?.level.toString()
                            .toByteArray()
                    )
                    shizuku.setMetadata(
                        device.macAddress.value,
                        BluetoothMetadata.METADATA_UNTETHERED_RIGHT_CHARGING,
                        (if (state.battery.find { it.component == BatteryComponent.RIGHT }?.status == BatteryStatus.CHARGING
                            || state.battery.find { it.component == BatteryComponent.RIGHT }?.status == BatteryStatus.OPTIMIZED_CHARGING
                        ) "1" else "0").toByteArray()
                    ) */
                }

                if (state.componentState != previousState.componentState) {
                    Log.i(
                        TAG,
                        "component state changed for device ${device.macAddress.toRedactedString()}"
                    )
                    Log.d(TAG, "component state: ${state.componentState}")

                    val earDetectionCtrlCmdValue =
                        state.controlStates[ControlCommandIdentifier.EAR_DETECTION_CONFIG]
                            ?: byteArrayOf(0x01.toByte())
                    Log.d(
                        TAG,
                        "ear detection control command value: ${earDetectionCtrlCmdValue.toHexString()}"
                    )
                    val earDetectionEnabled = earDetectionCtrlCmdValue[0] == 0x01.toByte() || deviceSettings.earDetectionEnabled // temporary, cache broken (?)
                    Log.d(TAG, "ear detection enabled: $earDetectionEnabled")
                    if (earDetectionEnabled && device.connectionState.value == ConnectionState.CONNECTED) {
                        processComponentStateChange(
                            device = device,
                            previousComponentState = previousState.componentState,
                            newComponentState = state.componentState,
                            disconnectWhenNotWearing = deviceSettings.disconnectWhenNotWearing
                        )
                    }

                    Log.d(TAG, "updating notification")
                    updateDeviceNotification(device = device)
                }

                if (state.controlStates != previousState.controlStates || state.confirmedControlStates != previousState.confirmedControlStates) {
                    updateWidgets()
                    Log.i(
                        TAG,
                        "control states changed for device ${device.macAddress.toRedactedString()}"
                    )
                    Log.d(TAG, "control states: ${state.controlStates}")

                    if (state.controlStates.isNotEmpty()) {
                        appleRepository.saveCacheFromState(device.macAddress, state)
                    }

                    Log.d(TAG, "updating notification")
                    updateDeviceNotification(device = device)
                }

                if (state.conversationAwarenessState != previousState.conversationAwarenessState) {
                    Log.i(
                        TAG,
                        "conversation awareness state changed for device ${device.macAddress.toRedactedString()}"
                    )
                    Log.d(
                        TAG,
                        "conversation awareness state: ${state.conversationAwarenessState}"
                    )

                    when (state.conversationAwarenessState) {
                        1 -> {
                            MediaController.startSpeaking()
                            MediaController.setVolume(
                                deviceSettings.conversationAwarenessVolume.toInt()
                            )
                        }

                        2 -> {
                            MediaController.setVolume(
                                deviceSettings.conversationAwarenessReducedVolume.toInt()
                            )
                        }

                        3 -> {
                            MediaController.setVolume(
                                deviceSettings.conversationAwarenessVolume.toInt()
                            )
                        }

                        6, 7, 8, 9 -> {
                            MediaController.stopSpeaking()
                        }
                    }
                }

                if (state.currentHeartRate?.timestamp != previousState.currentHeartRate?.timestamp) {
                    state.currentHeartRate?.let { heartRateSample ->
                        Log.i(
                            TAG,
                            "current heart rate changed from device ${device.macAddress.toRedactedString()}"
                        )
                        Log.d(
                            TAG,
                            "current heart rate: ${heartRateSample.bpm} bpm, timestamp: ${heartRateSample.timestamp.toJavaInstant()}. processing."
                        )

                        processHeartRateSample(
                            heartRateSample = heartRateSample,
                            interval = state.heartRateInterval,
                            alertThreshold = deviceSettings.hrmAlertThreshold
                        )
                    }
                }

                if (state.recordingState != previousState.recordingState) {
                    Log.i(
                        TAG,
                        "recording state changed for device ${device.macAddress.toRedactedString()}"
                    )
                    Log.d(TAG, "recording state: ${state.recordingState}")
                    if (previousState.recordingState.isRecording && !state.recordingState.isRecording) {
                        previousState.recordingState.recording?.let { recording ->
                            wavWriters[recording]?.close()
                        }
                    } else if (!previousState.recordingState.isRecording && state.recordingState.isRecording) {
                        state.recordingState.recording?.let { recording ->
                            try {
                                val wavWriter = WavWriter(recording.file)
                                wavWriters[recording] = wavWriter
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to create WavWriter for recording: ${recording.file}", e)
                            }
                        }
                    } else {
                        Log.w(TAG, "Recording state changed but no action taken. previous: ${previousState.recordingState}, current: ${state.recordingState}")
                    }
                }
                previousState = state
            }
        }
    }

    fun observeAppleSettings(device: AppleDevice) {
        serviceCoroutineScope.launch {
            var previousSettings = device.settings.value

            device.settings.collect { settings ->
                if (settings != previousSettings) {
                    if (!settings.headGesturesEnabled && callGestureOwner === device) stopCallGesture()
                    Log.i(TAG, "settings changed for device ${device.macAddress.toRedactedString()}")
                    Log.d(TAG, "settings: $settings")

                    if (!settings.showIslandPopup && islandOwner?.device === device) { islandWindow.forceClose(); islandOwner = null }
                    if (!settings.showBottomSheetPopup && bottomOwner?.device === device) { proximityCardWindow.forceClose(); bottomOwner = null }
                    if (!settings.showIslandPopup) {
                        pendingIsland.clearOwner { it.device === device }
                        if (pendingIsland.owner == null) { pendingIslandExpiry?.cancel(); pendingIslandExpiry = null }
                    }
                    appleRepository.saveSettings(device.macAddress, settings)
                    me.kavishdevar.librepods.bluetooth.NearbyDetection.keysChanged(this@LibrePodsService)
                }
                previousSettings = settings
            }
        }
    }

    fun observeAppleMetadata(device: AppleDevice) {
        serviceCoroutineScope.launch {
            var previousMetadata = device.metadata.value

            device.metadata.collect { metadata ->
                if (metadata != previousMetadata) {
                    Log.i(TAG, "metadata changed for device ${device.macAddress.toRedactedString()}")
                    Log.d(TAG, "metadata: $metadata")

                    appleRepository.saveMetadata(device.macAddress, metadata)

                    setAppleBluetoothMetadata(device)

                    val notificationManager = getSystemService(NotificationManager::class.java)
                    val channel =
                        notificationManager.getNotificationChannel("device_${device.macAddress}")
                    channel?.let { it.name = "Device ${metadata.name}"; notificationManager.createNotificationChannel(it) }
                    updateWidgets()
                    updateDeviceNotification(device)
                }
                previousMetadata = metadata
            }
        }
    }

    fun observeAppleMicrophoneFrames(device: AppleDevice) {
        serviceCoroutineScope.launch(Dispatchers.IO) {
            device.microphoneFrames.collect { frame ->
                val state = device.state.value

                if (state.recordingState.isRecording) {
                    eldDecoder.decode(
                        accessUnit = frame.accessUnit
                    ) { pcm ->
                        wavWriters[state.recordingState.recording]?.write(pcm)

                        device.updateMicrophoneState {
                            it.copy(
                                isActive = true,
                                packetsReceived = it.packetsReceived + 1,
                                durationMs = it.durationMs + 30,
                                level = calculateLevel(pcm)
                            )
                        }
                    }
                }
            }
        }
    }

    fun showIsland(
        device: Device<*, *, *>,
        type: IslandType = IslandType.CONNECTED,
        reversed: Boolean = false,
        otherDeviceName: String? = null
    ) {
        val apple = device as? AppleDevice ?: return
        val owner = OverlayOwner(apple, apple.connectionNumber.value)
        if (destroying || !current(owner)) return
        if (type != IslandType.CONNECTED) clearPendingIsland()
        if (!apple.settings.value.showIslandPopup ||
            (!islandWindow.usingAccessibilityService && !Settings.canDrawOverlays(this)) ||
            (type == IslandType.CONNECTED && proximityCardWindow.isVisible)) {
            clearPendingIsland(); return
        }
        if (!displayReady() || islandBattery(apple) == null) {
            if (type == IslandType.CONNECTED) {
                val changed = pendingIsland.owner != owner
                pendingIsland.request(owner, android.os.SystemClock.elapsedRealtime())
                if (changed) {
                    pendingIslandExpiry?.cancel()
                    pendingIslandExpiry = serviceCoroutineScope.launch { kotlinx.coroutines.delay(30_001); pendingIsland.clearOwner { it == owner } }
                }
            }
            return
        }
        clearPendingIsland()
        if (islandWindow.isVisible) return
        Log.d(TAG, "Showing island window")

        val state = device.state.value
        val settings = device.settings.value

        when (state) {
            is AppleState -> {
                val device = device as AppleDevice
                val state = device.state.value
                val settings = settings as AppleSettings
                val metadata = device.metadata.value

                if (state.componentState.isEmpty()) {
                    Log.w(TAG, "No component state available, can't show island")
                    return
                }

                if (settings.showIslandPopup) {
                    if (!Settings.canDrawOverlays(this)) {
                        Log.w(TAG, "No permission for SYSTEM_ALERT_WINDOW")
                        return
                    }

                    Log.i(TAG, "Showing island for device ${device.macAddress.toRedactedString()}")

                    val batteryPercentage = islandBattery(device) ?: return

                    islandOwner = owner
                    islandWindow.show(
                        name = metadata.name,
                        batteryLevel = batteryPercentage,
                        type = type,
                        reversed = reversed,
                        otherDeviceName = otherDeviceName,
                        onReverseAction = { if (current(owner)) requestAudioTakeover(device, reverse = true) },

                        openApp = {
                            val intent = Intent(
                                this,
                                MainActivity::class.java,
                            ).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(intent)
                        },

                        showOffListeningMode = state.controlStates[ControlCommandIdentifier.ALLOW_OFF_OPTION]?.getOrNull(0)?.toInt() == 1,
                        noiseControlModeValue = state.controlStates[ControlCommandIdentifier.LISTENING_MODE]?.getOrNull(0)?.toInt() ?: 3,
                        onNoiseControlModeChanged = {
                            device.setControlCommand(
                                ControlCommandIdentifier.LISTENING_MODE, it
                            )
                        },
                        adaptiveStrength = state.controlStates[ControlCommandIdentifier.AUTO_ANC_STRENGTH]?.getOrNull(0)?.toInt() ?: 50,
                        onAdaptiveStrengthChanged = {
                            device.setControlCommand(
                                ControlCommandIdentifier.AUTO_ANC_STRENGTH, it
                            )
                        }
                    )
                }
            }

            else -> {
                Log.d(
                    TAG,
                    "Unsupported device state: ${device.state.value.javaClass.simpleName}, not showing island"
                )
                return
            }
        }
    }

    private data class ConnectionNotificationContent(val name: String, val batteries: String,
        val connected: Boolean, val modes: List<Int>, val locale: String, val reconnect: Boolean)
    private val connectionNotifications = mutableMapOf<MacAddress, ConnectionNotificationContent>()

    fun updateDeviceNotification(device: Device<*, *, *>) {
        if (destroying || device !is AppleDevice) return
        val manager = getSystemService(NotificationManager::class.java)
        val address = device.macAddress
        val state = device.connectionState.value
        if (state != ConnectionState.CONNECTED && state != ConnectionState.AVAILABLE) {
            manager.cancel(address.value, 2)
            connectionNotifications.remove(address)
            return
        }
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val widget = me.kavishdevar.librepods.presentation.widgets.WidgetDataSource.state(this, null, address)
        val modes = if (state == ConnectionState.CONNECTED && widget.connected) widget.availableModes else emptyList()
        val batteries = device.state.value.battery.filter { it.level in 0..100 &&
            it.status in setOf(BatteryStatus.NOT_CHARGING, BatteryStatus.CHARGING, BatteryStatus.OPTIMIZED_CHARGING) }
            .sortedBy { it.component.ordinal }.joinToString(" ") { "${it.component.name[0]}: ${if (it.status == BatteryStatus.CHARGING) "⚡" else ""}${it.level}%" }
        val content = ConnectionNotificationContent(device.metadata.value.name, batteries,
            state == ConnectionState.CONNECTED, modes, resources.configuration.locales.toLanguageTags(), address in reversedDevices)
        if (connectionNotifications[address] == content && manager.activeNotifications.any { it.tag == address.value && it.id == 2 }) return
        val channelId = "device_$address"
        val channel = manager.getNotificationChannel(channelId) ?: NotificationChannel(channelId, content.name, NotificationManager.IMPORTANCE_LOW)
        channel.name = content.name
        manager.createNotificationChannel(channel)
        val open = Intent(this, MainActivity::class.java).setData(android.net.Uri.parse("librepods://device/$address"))
        val pending = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(this, channelId).setSmallIcon(R.drawable.ic_airpods)
            .setContentTitle(content.name).setContentText(content.batteries).setContentIntent(pending)
            .setCategory(Notification.CATEGORY_STATUS).setPriority(NotificationCompat.PRIORITY_LOW).setOngoing(true)
        val labels = mapOf(1 to R.string.off, 3 to R.string.transparency, 4 to R.string.adaptive, 2 to R.string.noise_cancellation)
        modes.forEach { mode ->
            val action = Intent(this, LibrePodsService::class.java).apply {
                this.action = LibrePodsIntents.ACTION_SET_ANC_MODE
                data = android.net.Uri.parse("librepods://device/$address/mode/$mode")
                putExtra(LibrePodsIntents.Extras.MAC_ADDRESS, address.value)
                putExtra(LibrePodsIntents.Extras.ANC_MODE, mode)
            }
            builder.addAction(R.drawable.ic_airpods, getString(checkNotNull(labels[mode])),
                PendingIntent.getService(this, mode, action, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        }
        if (content.reconnect) {
            val reconnect = Intent(this, MainActivity::class.java).apply {
                action = LibrePodsIntents.ACTION_RECONNECT_AUDIO
                data = android.net.Uri.parse("librepods://device/$address/reconnect-audio")
                putExtra(LibrePodsIntents.Extras.MAC_ADDRESS, address.value)
            }
            builder.addAction(R.drawable.ic_power, getString(R.string.reconnect_audio),
                PendingIntent.getActivity(this, 0, reconnect, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        }
        if (state == ConnectionState.AVAILABLE) {
            // Without a companion owner, the user's visible Activity owns this manual attempt.
            val connect = Intent(this, MainActivity::class.java).apply {
                action = LibrePodsIntents.ACTION_CONNECT
                data = android.net.Uri.parse("librepods://device/$address/connect")
                putExtra(LibrePodsIntents.Extras.MAC_ADDRESS, address.value)
            }
            builder.addAction(R.drawable.ic_power, getString(R.string.connect),
                PendingIntent.getActivity(this, 0, connect, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        }
        // Remove the old tagless rewrite notification, then publish with a per-device tag.
        manager.cancel(address.toNotificationId())
        manager.notify(address.value, 2, builder.build())
        connectionNotifications[address] = content
    }

    fun updateWidgets() {
        synchronizeWidgetBatteryReceiver()
        me.kavishdevar.librepods.presentation.widgets.WidgetPublisher.update(this)
    }

    private var widgetBatteryReceiver: BroadcastReceiver? = null
    fun synchronizeWidgetBatteryReceiver() {
        val publisher = me.kavishdevar.librepods.presentation.widgets.WidgetPublisher
        val needed = runtimeActive && publisher.hasBatteryWidgets(this) && publisher.needsPhoneBattery(this)
        if (needed && widgetBatteryReceiver == null) {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) { publisher.update(context) }
            }
            widgetBatteryReceiver = receiver
            registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), RECEIVER_NOT_EXPORTED)
        } else if (!needed && widgetBatteryReceiver != null) {
            runCatching { unregisterReceiver(checkNotNull(widgetBatteryReceiver)) }
            widgetBatteryReceiver = null
        }
    }

    private fun processComponentStateChange(
        device: Device<*, *, *>,
        previousComponentState: Set<DeviceComponentState>,
        newComponentState: Set<DeviceComponentState>,
        disconnectWhenNotWearing: Boolean
    ) {
        val old = earPresenceOf(previousComponentState)
        val new = earPresenceOf(newComponentState)
        if (old == new) return

        Log.d(TAG, "earPresence: $old -> $new")

        // new != NONE because old!=new
        if (old == EarPresence.NONE) {
            showIsland(
                device = device,
                type = IslandType.CONNECTED
            )
            Log.i(TAG, "User put in at least one component, showing island.")
        }

        if (new == EarPresence.NONE && device is AppleDevice) closeOwnedPopups(device)

        var justEnabledA2dp = false

        when {
            old == EarPresence.NONE -> {
                Log.d(
                    TAG,
                    "User put in at least one component, enabling audio for device ${device.macAddress.toRedactedString()}"
                )
                device.enableAudio()
                device.connectA2dp()
                justEnabledA2dp = true

                device.waitForA2dpConnection(this) {
                    MediaController.sendPlay()
                    MediaController.iPausedTheMedia = false
                }

                if (MediaController.getMusicActive()) {
                    MediaController.userPlayedTheMedia = true
                }
                if (new == EarPresence.PARTIAL) {
                    MediaController.sendPlay()
                    MediaController.iPausedTheMedia = false
                }
            }

            new == EarPresence.NONE -> {
                MediaController.sendPause(force = true)
                if (disconnectWhenNotWearing) {
                    Log.d(
                        TAG,
                        "Disconnecting audio for device ${device.macAddress.toRedactedString()} because user took out all components and disconnectWhenNotWearing is true"
                    )
                    device.disableAudio()
                    device.disconnectAudio()
                }
            }
        }

        when {
            new == EarPresence.FULL -> {
                Log.d("AirPodsParser", "User put in all components.")
                MediaController.userPlayedTheMedia = false
                if (!justEnabledA2dp) {
                    MediaController.sendPlay()
                    MediaController.iPausedTheMedia = false
                }
            }

            old == EarPresence.FULL -> {
                Log.d("AirPodsParser", "User took one out.")
                MediaController.userPlayedTheMedia = false
                if (new == EarPresence.PARTIAL) {
                    MediaController.sendPause()
                }
            }
        }
    }

    private fun processHeartRateSample(heartRateSample: HeartRateSample, interval: Duration, alertThreshold: Int) {
        serviceCoroutineScope.launch {
            Log.d(TAG, "inserting to local db")
            heartRateRepository.insert(heartRateSample)
        }

        serviceCoroutineScope.launch(Dispatchers.Default) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            if (heartRateSample.bpm > alertThreshold) {
                val notification = NotificationCompat.Builder(this@LibrePodsService, "hrm_alert")
                    .setSmallIcon(R.drawable.ic_pulse_alert)
                    .setContentTitle(getString(R.string.high_heart_rate))
                    .setContentText(
                        getString(
                            R.string.high_heart_rate_notification_text,
                            heartRateSample.bpm
                        )
                    )
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setOngoing(true)
                    .build()

                notificationManager.notify(999, notification)
            } else {
                if (notificationManager.activeNotifications.any { it.id == 999 }) {
                    notificationManager.cancel(999)
                }
            }
        }

        serviceCoroutineScope.launch {
            if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 7) {
                if (checkSelfPermission(HealthPermission.getWritePermission(HeartRateRecord::class)) != PackageManager.PERMISSION_GRANTED) return@launch
                val healthConnectHeartRateSample = HeartRateRecord.Sample(
                    time = heartRateSample.timestamp.toJavaInstant(),
                    beatsPerMinute = heartRateSample.bpm.toLong()
                )

                val zoneOffset =
                    ZoneOffset.systemDefault().rules.getOffset(heartRateSample.timestamp.toJavaInstant())

                val heartRateRecord = HeartRateRecord(
                    startTime = (heartRateSample.timestamp - interval).toJavaInstant(),
                    endTime = heartRateSample.timestamp.toJavaInstant(),
                    startZoneOffset = zoneOffset,
                    endZoneOffset = zoneOffset,
                    samples = listOf(healthConnectHeartRateSample),
                    metadata = androidx.health.connect.client.records.metadata.Metadata.autoRecorded(
                        device = androidx.health.connect.client.records.metadata.Device(type = androidx.health.connect.client.records.metadata.Device.TYPE_HEARABLE)
                    )
                )

                if (healthConnectClient != null) {
                    serviceCoroutineScope.launch {
                        try {
                            healthConnectClient!!.insertRecords(listOf(heartRateRecord))
                        } catch (e: Exception) {
                            Log.e(TAG, "Error inserting heart rate record", e)
                        }
                    }
                } else {
                    Log.w(TAG, "Health Connect client not available")
                }
            } else {
                Log.d(TAG, "U SDK Extension <7")
            }
        }
    }

    // TODO: Shizuku
    private fun setAppleBluetoothMetadata(
        @Suppress("unused") device: AppleDevice
    ) {
        /* val macAddress = device.macAddress.value
        val metadata = device.metadata.value
        val spec = AirPodsSpecs.getSpec(metadata.model)

        shizuku.setMetadata(
            macAddress,
            BluetoothMetadata.METADATA_MAIN_ICON,
            resToUri(spec.primaryImageRes).toString().toByteArray()
        )
        shizuku.setMetadata(
            macAddress, BluetoothMetadata.METADATA_MODEL_NAME, metadata.modelNumber.toByteArray()
        )
        shizuku.setMetadata(
            macAddress,
            BluetoothMetadata.METADATA_DEVICE_TYPE,
            BluetoothMetadata.DEVICE_TYPE_UNTETHERED_HEADSET.toByteArray()
        )
        spec.caseImageRes?.let {
            shizuku.setMetadata(
                macAddress,
                BluetoothMetadata.METADATA_UNTETHERED_CASE_ICON,
                resToUri(it).toString().toByteArray()
            )
        }
//        shizuku.setMetadata(
//            macAddress,
//            BluetoothMetadata.METADATA_UNTETHERED_RIGHT_ICON,
//            resToUri(spec.components.find { it.type == DeviceComponent.RIGHT }.imageRes).toString().toByteArray()
//        )
//        shizuku.setMetadata(
//            macAddress,
//            BluetoothMetadata.METADATA_UNTETHERED_LEFT_ICON,
//            resToUri(spec.components.find { it.type == DeviceComponent.LEFT }.imageRes).toString().toByteArray()
//        )
        shizuku.setMetadata(
            macAddress,
            BluetoothMetadata.METADATA_MANUFACTURER_NAME,
            metadata.manufacturer.toByteArray()
        )
        shizuku.setMetadata(
            macAddress, BluetoothMetadata.METADATA_COMPANION_APP, "me.kavishdevar.librepods".toByteArray()
        )
        shizuku.setMetadata(
            macAddress,
            BluetoothMetadata.METADATA_UNTETHERED_CASE_LOW_BATTERY_THRESHOLD,
            "20".toByteArray()
        )
        shizuku.setMetadata(
            macAddress,
            BluetoothMetadata.METADATA_UNTETHERED_LEFT_LOW_BATTERY_THRESHOLD,
            "20".toByteArray()
        )
        shizuku.setMetadata(
            macAddress,
            BluetoothMetadata.METADATA_UNTETHERED_RIGHT_LOW_BATTERY_THRESHOLD,
            "20".toByteArray()
        )
        Log.d(TAG, "Metadata set for apple device ${device.macAddress.toRedactedString()}")
     */
    }
}

// TODO: move out of this file
private enum class EarPresence { NONE, PARTIAL, FULL }

private fun earPresenceOf(components: Set<DeviceComponentState>): EarPresence {
    if (components.isEmpty()) return EarPresence.NONE
    val inEarCount = components.count { it.status == ComponentStatus.IN_EAR }
    return when (inEarCount) {
        0 -> EarPresence.NONE
        components.size -> EarPresence.FULL
        else -> EarPresence.PARTIAL
    }
}
