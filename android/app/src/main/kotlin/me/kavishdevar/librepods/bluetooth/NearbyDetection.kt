package me.kavishdevar.librepods.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.PersistableBundle
import android.os.SystemClock
import android.provider.Settings
import android.telecom.TelecomManager
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.kavishdevar.librepods.LibrePodsApplication
import me.kavishdevar.librepods.core.bluetooth.MacAddress
import me.kavishdevar.librepods.core.bluetooth.aacp.types.MagicKeyType
import me.kavishdevar.librepods.core.bluetooth.ble.NearbyDecoder
import me.kavishdevar.librepods.core.devices.*
import me.kavishdevar.librepods.database.apple.AppleEntity
import me.kavishdevar.librepods.receivers.NearbyScanReceiver
import me.kavishdevar.librepods.services.NearbyTakeoverService
import java.io.Closeable
import java.security.MessageDigest

/** The system owns scan delivery. No engine binding, callback scan or idle polling lives here. */
object NearbyDetection {
    const val PREFERENCE = "background_detection"
    const val ACTION_SCAN = "me.kavishdevar.librepods.NEARBY_SCAN"
    const val TAKEOVER_JOB_ID = 7
    data class Snapshot(val address: MacAddress, val batteries: Set<Battery>,
        val status: NearbyDecoder.AirPodsStatus?, val lastSeen: Long)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val lock = Mutex()
    private val keyLock = Mutex()
    private var keyRevision = 0L
    private lateinit var app: Context
    private lateinit var preferences: SharedPreferences
    private var registered = false
    private var loaded = false
    @Volatile private var entities = emptyMap<MacAddress, AppleEntity>()
    private val decoders = mutableMapOf<MacAddress, NearbyDecoder>()
    private var expiry: Job? = null
    private val _snapshots = MutableStateFlow<Map<MacAddress, Snapshot>>(emptyMap())
    val snapshots = _snapshots.asStateFlow()
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == PREFERENCE) synchronize()
    }

    fun initialize(context: Context) {
        if (::app.isInitialized) return
        app = context.applicationContext
        preferences = app.getSharedPreferences("settings", Context.MODE_PRIVATE)
        preferences.registerOnSharedPreferenceChangeListener(listener)
    }
    fun isEnabled() = ::preferences.isInitialized && preferences.getBoolean(PREFERENCE, false)
    private fun pending(flags: Int) = PendingIntent.getBroadcast(app, 0,
        Intent(app, NearbyScanReceiver::class.java).setAction(ACTION_SCAN), flags or PendingIntent.FLAG_MUTABLE)

    private fun keys(entity: AppleEntity?) =
        entity?.cache?.magicKeys?.get(MagicKeyType.IRK) to entity?.cache?.magicKeys?.get(MagicKeyType.ENC_KEY)
    private fun identity(entity: AppleEntity?): String {
        val (irk, enc) = keys(entity)
        return Base64.encodeToString(MessageDigest.getInstance("SHA-256")
            .digest((irk ?: byteArrayOf()) + (enc ?: byteArrayOf())), Base64.NO_WRAP)
    }
    private suspend fun refreshKeys() = keyLock.withLock {
        while (!loaded) {
            val revision = keyRevision
            val current = (app as LibrePodsApplication).database.appleDao().getAll().associateBy { it.macAddress }
            decoders.keys.toList().forEach { address ->
                if (address !in current || identity(entities[address]) != identity(current[address])) decoders.remove(address)
            }
            entities = current
            entities.keys.forEach { address ->
                decoders.getOrPut(address) { NearbyDecoder({ keys(entities[address]) }, SystemClock::elapsedRealtime) }
            }
            loaded = revision == keyRevision
        }
    }
    fun keysChanged(context: Context) {
        initialize(context)
        keyRevision++
        loaded = false
        synchronize()
    }

    @SuppressLint("MissingPermission")
    fun synchronize(context: Context? = null, completed: () -> Unit = {}) {
        if (context != null) initialize(context)
        scope.launch {
            try {
                lock.withLock {
                    val adapter = app.getSystemService(BluetoothManager::class.java)?.adapter
                    val permitted = app.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                        app.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                    if (isEnabled() && permitted && adapter?.isEnabled == true) { refreshKeys(); publish() }
                    val usableKeys = entities.values.any { keys(it).first?.size == 16 }
                    if (!isEnabled() || !permitted || adapter?.isEnabled != true || !usableKeys) {
                        if (permitted && adapter?.isEnabled == true) pending(PendingIntent.FLAG_NO_CREATE)?.let {
                            adapter.bluetoothLeScanner?.stopScan(it)
                        }
                        registered = false
                        expiry?.cancel()
                        decoders.values.forEach { it.clear() }
                        publish()
                        app.getSystemService(JobScheduler::class.java).cancel(TAKEOVER_JOB_ID)
                        return@withLock
                    }
                    if (registered) return@withLock
                    if (!adapter.isOffloadedScanBatchingSupported) {
                        Log.w("NearbyDetection", "Batched scanning unavailable; no callback fallback")
                        return@withLock
                    }
                    val buds = ScanFilter.Builder().setManufacturerData(76, byteArrayOf(7, 25), byteArrayOf(-1, -1)).build()
                    val case = ScanFilter.Builder().setManufacturerData(76, byteArrayOf(7, 0, 6), byteArrayOf(-1, 0, -1)).build()
                    val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                        .setReportDelay(5_000).setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES).build()
                    val result = adapter.bluetoothLeScanner?.startScan(listOf(buds, case), settings,
                        checkNotNull(pending(PendingIntent.FLAG_UPDATE_CURRENT)))
                    registered = result == 0 || result == ScanCallback.SCAN_FAILED_ALREADY_STARTED
                    if (!registered) Log.w("NearbyDetection", "Scan registration failed: $result")
                }
            } catch (error: Exception) {
                registered = false
                Log.w("NearbyDetection", "Scan reconciliation failed", error)
            } finally { completed() }
        }
    }

    fun considerTakeover(context: Context) {
        initialize(context)
        scope.launch {
            if (!isEnabled()) return@launch
            if (!loaded) refreshKeys()
            snapshots.value.values.forEach { sample -> maybeSchedule(sample) }
        }
    }

    fun scanFailed(error: Int) {
        registered = false
        Log.w("NearbyDetection", "Scan failed: $error; waiting for a system/user entry point")
    }

    @SuppressLint("MissingPermission")
    fun receive(results: List<ScanResult>, completed: () -> Unit) {
        scope.launch {
            try {
                if (!isEnabled()) return@launch
                if (!loaded) refreshKeys()
                // Bound receiver work and keep cryptography off main. One newest accepted packet
                // per device/source is enough; later entries in a batch cannot improve it.
                val current = decoders.toMap()
                kotlinx.coroutines.withTimeout(5_000) {
                    kotlinx.coroutines.withContext(Dispatchers.Default) {
                        val buds = mutableSetOf<MacAddress>()
                        val cases = mutableSetOf<MacAddress>()
                        for (result in results.sortedByDescending { it.timestampNanos }) {
                            kotlinx.coroutines.currentCoroutineContext().ensureActive()
                            val data = result.scanRecord?.getManufacturerSpecificData(76) ?: continue
                            val seen = result.timestampNanos / 1_000_000
                            current.forEach { (address, decoder) ->
                                if (address !in cases && decoder.acceptCase(result.device.address, data, seen) != null) cases += address
                                if (address !in buds && decoder.accept(result.device.address, data, seen) != null) buds += address
                            }
                        }
                    }
                }
                if (!isEnabled()) return@launch
                publish()
                snapshots.value.values.sortedByDescending { it.lastSeen }.forEach { maybeSchedule(it) }
            } catch (error: Exception) {
                Log.w("NearbyDetection", "Cannot process nearby batch", error)
            } finally { completed() }
        }
    }

    private fun snapshot(address: MacAddress, decoder: NearbyDecoder): Snapshot? {
        if (!isEnabled()) return null
        val buds = decoder.getMostRecentStatus()
        val case = decoder.getCaseStatus()
        if (buds == null && case == null) return null
        val readings = mutableListOf<Pair<Long, Battery>>()
        fun add(component: BatteryComponent, level: Int?, charging: Boolean, seen: Long, source: BatterySource) {
            if (level !in 0..100) return
            val wall = System.currentTimeMillis() - (SystemClock.elapsedRealtime() - seen)
            readings += seen to Battery(component, checkNotNull(level),
                if (charging) BatteryStatus.CHARGING else BatteryStatus.NOT_CHARGING, source, wall)
        }
        buds?.let {
            add(BatteryComponent.LEFT, it.leftBattery, it.isLeftCharging, it.lastSeen, BatterySource.BUDS_ADVERT)
            add(BatteryComponent.RIGHT, it.rightBattery, it.isRightCharging, it.lastSeen, BatterySource.BUDS_ADVERT)
            add(BatteryComponent.CASE, it.caseBattery, it.isCaseCharging, it.lastSeen, BatterySource.BUDS_ADVERT)
        }
        case?.let {
            add(BatteryComponent.LEFT, it.leftBattery, it.isLeftCharging, it.lastSeen, BatterySource.CASE_ADVERT)
            add(BatteryComponent.RIGHT, it.rightBattery, it.isRightCharging, it.lastSeen, BatterySource.CASE_ADVERT)
            add(BatteryComponent.CASE, it.caseBattery, it.isCaseCharging, it.lastSeen, BatterySource.CASE_ADVERT)
        }
        val battery = readings.groupBy { it.second.component }.values.map { group -> group.maxBy { it.first }.second }.toSet()
        return Snapshot(address, battery, buds, maxOf(buds?.lastSeen ?: -1, case?.lastSeen ?: -1))
    }

    fun currentSnapshots(): Map<MacAddress, Snapshot> = decoders.mapNotNull { (address, decoder) ->
        snapshot(address, decoder)?.let { address to it }
    }.toMap()

    private fun publish() {
        val next = currentSnapshots()
        _snapshots.value = next
        CompanionConnection.activeEngine?.onNearbySnapshots(next)
        expiry?.cancel()
        // One finite in-process expiry, no alarm/wake lock/process persistence.
        val firstExpiry = decoders.values.flatMap { listOfNotNull(it.getMostRecentStatus()?.lastSeen, it.getCaseStatus()?.lastSeen) }.minOrNull()
        if (firstExpiry != null) expiry = scope.launch {
            delay((firstExpiry + NearbyDecoder.MAX_AGE_MS + 1 - SystemClock.elapsedRealtime()).coerceAtLeast(1))
            publish()
        }
    }

    fun suppress(context: Context, address: MacAddress) {
        initialize(context)
        preferences.edit().putBoolean("nearby_suppressed_${address.value}", true).apply()
        app.getSystemService(JobScheduler::class.java).cancel(TAKEOVER_JOB_ID)
    }
    fun newClassicLink(context: Context, address: String) {
        initialize(context)
        preferences.edit().remove("nearby_suppressed_$address").apply()
    }

    @SuppressLint("MissingPermission")
    private fun reason(sample: Snapshot): String? {
        if (!isEnabled() || preferences.getBoolean("ble_only_mode", false) ||
            preferences.getBoolean("nearby_suppressed_${sample.address.value}", false)) return null
        val status = sample.status ?: return null // Case advertisements cannot admit takeover.
        if (!status.isLeftInEar && !status.isRightInEar) return null
        val settings = CompanionConnection.activeEngine?.devices?.value?.get(sample.address)
            ?.let { (it as? AppleDevice)?.takeIf { it.settingsInitialized }?.settings?.value } ?: entities[sample.address]?.settings ?: return null
        val call = app.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
            app.getSystemService(TelecomManager::class.java)?.isInCall == true
        val media = app.getSystemService(AudioManager::class.java).isMusicActive
        val allowed = when (status.connectionState) {
            "Disconnected" -> if (call) settings.connectOnPhoneWhenAccessoryDisconnected else media && settings.connectOnMediaWhenAccessoryDisconnected
            "Idle" -> if (call) settings.connectOnPhoneWhenAccessoryIdle else media && settings.connectOnMediaWhenAccessoryIdle
            "Music" -> if (call) settings.connectOnPhoneWhenAccessoryMusic else media && settings.connectOnMediaWhenAccessoryMusic
            else -> false
        }
        return if (allowed) (if (call) "call" else "media") else null
    }
    private fun budget(address: MacAddress, reason: String?): NearbyAttemptBudget {
        val state = app.getSharedPreferences("nearby_attempt:${address.value}", Context.MODE_PRIVATE)
        val previous = NearbyAttemptBudget(address.value, state.getString("reason", null), state.getInt("attempts", 0), state.getLong("last", -60_000))
        val next = previous.observe(address.value, reason)
        if (next != previous) state.edit().putString("reason", next.reason).putInt("attempts", next.attempts).apply()
        return next
    }
    private fun association(address: MacAddress) = CompanionConnection.associations(app).any {
        it.deviceMacAddress?.toString().equals(address.value, true)
    }
    private fun globalCooldown(key: String): Boolean {
        val last = app.getSharedPreferences("nearby_admission", Context.MODE_PRIVATE).getLong(key, -60_000)
        val now = SystemClock.elapsedRealtime()
        return now >= last && now - last < 60_000
    }
    private fun recordGlobal(key: String) = app.getSharedPreferences("nearby_admission", Context.MODE_PRIVATE)
        .edit().putLong(key, SystemClock.elapsedRealtime()).apply()

    private fun maybeSchedule(sample: Snapshot) {
        val reason = reason(sample)
        val budget = budget(sample.address, reason)
        if (reason == null || !association(sample.address) || budget.reserve(SystemClock.elapsedRealtime()) == null || globalCooldown("scheduled")) return
        val device = CompanionConnection.activeEngine?.devices?.value?.get(sample.address)
        if (device?.connectionState?.value in setOf(ConnectionState.CONNECTED, ConnectionState.CONNECTING)) return
        val scheduler = app.getSystemService(JobScheduler::class.java)
        if (scheduler.getPendingJob(TAKEOVER_JOB_ID) != null) return
        val observation = decoders[sample.address]?.getObservation() ?: return
        val extras = PersistableBundle().apply {
            putString("address", sample.address.value); putString("reason", reason)
            putString("identity", identity(entities[sample.address]))
            putInt("boot", Settings.Global.getInt(app.contentResolver, Settings.Global.BOOT_COUNT, -1))
            putString("rpa", observation.address); putLong("observed", observation.seen)
            putString("advertisement", Base64.encodeToString(observation.data, Base64.NO_WRAP))
        }
        if (scheduler.schedule(JobInfo.Builder(TAKEOVER_JOB_ID, ComponentName(app, NearbyTakeoverService::class.java))
            .setExtras(extras).setOverrideDeadline(0).build()) == JobScheduler.RESULT_SUCCESS) recordGlobal("scheduled")
    }

    suspend fun eligible(extras: PersistableBundle): Snapshot? {
        if (!isEnabled()) return null
        if (!loaded) refreshKeys()
        val address = extras.getString("address")?.let(::MacAddress) ?: return null
        if (!association(address) || extras.getString("identity") != identity(entities[address]) ||
            extras.getInt("boot", -1) != Settings.Global.getInt(app.contentResolver, Settings.Global.BOOT_COUNT, -2)) return null
        val decoder = decoders[address] ?: return null
        if (decoder.getMostRecentStatus() == null) {
            val data = try { Base64.decode(extras.getString("advertisement"), Base64.NO_WRAP) } catch (_: Exception) { return null }
            decoder.accept(extras.getString("rpa") ?: return null, data, extras.getLong("observed", -1)) ?: return null
        }
        val sample = snapshot(address, decoder) ?: return null
        return sample.takeIf { reason(it) == extras.getString("reason") }
    }

    fun begin(device: AppleDevice, sample: Snapshot): Closeable? {
        val reason = reason(sample) ?: return null
        if (globalCooldown("attempted")) return null
        val next = budget(sample.address, reason).reserve(SystemClock.elapsedRealtime()) ?: return null
        val lease = device.beginNearbyConnection() ?: return null
        app.getSharedPreferences("nearby_attempt:${sample.address.value}", Context.MODE_PRIVATE).edit()
            .putString("reason", next.reason).putInt("attempts", next.attempts).putLong("last", next.lastAttempt).apply()
        recordGlobal("attempted")
        return lease
    }
}
