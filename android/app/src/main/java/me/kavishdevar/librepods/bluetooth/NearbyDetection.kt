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
import java.security.MessageDigest
import android.telecom.TelecomManager
import android.util.Base64
import android.util.Log
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.kavishdevar.librepods.receivers.NearbyScanReceiver
import me.kavishdevar.librepods.services.NearbyTakeoverService
import me.kavishdevar.librepods.services.AirPodsService
import me.kavishdevar.librepods.services.ServiceManager

/** System-owned scan registration; receiving an advertisement does not start the engine. */
object NearbyDetection {
    const val ACTION_SCAN = "me.kavishdevar.librepods.NEARBY_SCAN"
    const val TAKEOVER_JOB_ID = 7
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val lock = Mutex()
    private var initialized = false
    @Volatile private var registered = false
    private var identityRevision = 0L
    private var expiry: Job? = null
    private lateinit var app: Context
    private lateinit var preferences: SharedPreferences
    lateinit var decoder: BLEManager
        private set
    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key in setOf(CompanionConnection.DETECTION_PREFERENCE, "IRK", "ENC_KEY", "mac_address", "proximity_key_address")) {
            identityRevision++
            decoder.clear()
            synchronize()
        }
    }

    fun initialize(context: Context) {
        if (initialized) return
        initialized = true
        app = context.applicationContext
        preferences = app.getSharedPreferences("settings", Context.MODE_PRIVATE)
        // Existing installations stored one device's keys without a separate owner field.
        if (!preferences.contains("proximity_key_address") && preferences.contains("IRK")) {
            preferences.edit { putString("proximity_key_address", preferences.getString("mac_address", "")) }
        }
        decoder = BLEManager({ key("IRK") to key("ENC_KEY") }, SystemClock::elapsedRealtime)
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
    }

    private fun key(name: String): ByteArray? = try {
        if (preferences.getString("proximity_key_address", null) != preferences.getString("mac_address", "")) null
        else preferences.getString(name, null)?.let { Base64.decode(it, Base64.DEFAULT) }
    } catch (_: IllegalArgumentException) { null }

    fun isEnabled(): Boolean = initialized &&
        preferences.getBoolean(CompanionConnection.DETECTION_PREFERENCE, false)

    private fun pendingIntent(flags: Int): PendingIntent? = PendingIntent.getBroadcast(
        app, 0, Intent(app, NearbyScanReceiver::class.java).setAction(ACTION_SCAN),
        flags or PendingIntent.FLAG_MUTABLE
    )

    /** Called at user/system entry points, never on a retry timer or every scan delivery. */
    @SuppressLint("MissingPermission")
    fun synchronize(context: Context? = null, completed: () -> Unit = {}) {
        if (context != null) initialize(context)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    lock.withLock {
                        try {
                            val adapter = app.getSystemService(BluetoothManager::class.java)?.adapter
                            val permitted = app.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                                app.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                            val enabled = isEnabled() && permitted && adapter?.isEnabled == true &&
                                key("IRK")?.size == 16 && !preferences.getString("mac_address", "").isNullOrEmpty()
                            if (!enabled) {
                                if (permitted && adapter?.isEnabled == true) {
                                    pendingIntent(PendingIntent.FLAG_NO_CREATE)?.let { adapter.bluetoothLeScanner?.stopScan(it) }
                                }
                                registered = false
                                decoder.clear()
                                app.getSystemService(JobScheduler::class.java).cancel(TAKEOVER_JOB_ID)
                                return@withLock
                            }
                            if (registered) return@withLock
                            // No unbatched fallback: busy surroundings must not wake us for every packet.
                            if (!adapter.isOffloadedScanBatchingSupported) {
                                Log.w("NearbyDetection", "Controller does not support batched nearby scanning")
                                return@withLock
                            }
                            val filter = ScanFilter.Builder().setManufacturerData(76,
                                byteArrayOf(7, 25), byteArrayOf(-1, -1)).build()
                            val settings = ScanSettings.Builder()
                                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                                .setReportDelay(5_000)
                                .build()
                            val result = adapter.bluetoothLeScanner?.startScan(listOf(filter), settings,
                                checkNotNull(pendingIntent(PendingIntent.FLAG_UPDATE_CURRENT)))
                            registered = result == 0 || result == ScanCallback.SCAN_FAILED_ALREADY_STARTED
                            if (!registered) Log.w("NearbyDetection", "Nearby scan registration failed: $result")
                        } catch (e: Exception) {
                            registered = false
                            Log.w("NearbyDetection", "Cannot update nearby scan registration", e)
                        }
                    }
                }
                if (decoder.getMostRecentStatus() == null) {
                    expiry?.cancel()
                    publishExpired()
                }
            } finally { completed() }
        }
    }

    fun scanFailed(error: Int) {
        registered = false
        Log.w("NearbyDetection", "Nearby scan failed: $error; waiting for a user/system entry point")
    }

    fun receive(results: List<ScanResult>, completed: () -> Unit) {
        scope.launch {
            try {
                if (!isEnabled()) return@launch
                val previous = decoder.getMostRecentStatus()
                val revision = identityRevision
                val status = withTimeout(5_000) {
                    withContext(Dispatchers.Default) {
                        // Only the newest verified snapshot is useful; never replay an old batch's transitions.
                        results.sortedByDescending { it.timestampNanos }.firstNotNullOfOrNull { result ->
                            ensureActive()
                            result.scanRecord?.getManufacturerSpecificData(76)?.let {
                                decoder.accept(result.device.address, it, result.timestampNanos / 1_000_000)
                            }
                        }
                    }
                } ?: return@launch
                if (!isEnabled() || identityRevision != revision) return@launch
                expiry?.cancel()
                expiry = scope.launch {
                    // One cancellable expiry, not a poll, alarm, wake lock or reason to keep the process alive.
                    delay((status.lastSeen + BLEManager.MAX_AGE_MS + 1 - SystemClock.elapsedRealtime()).coerceAtLeast(0))
                    if (decoder.getMostRecentStatus() == null) publishExpired()
                }
                ServiceManager.getService()?.onNearbyStatus(status)
                if (ServiceManager.getService() == null && !BLEManager.sameContent(previous, status)) {
                    me.kavishdevar.librepods.presentation.widgets.BatteryWidget.update(app, batteries())
                }
                maybeScheduleTakeover(status)
            } catch (e: Exception) {
                Log.w("NearbyDetection", "Cannot process nearby scan", e)
            } finally {
                completed()
            }
        }
    }

    private fun publishExpired() {
        ServiceManager.getService()?.onNearbyStatus(null)
        if (ServiceManager.getService() == null && BluetoothConnectionManager.aacpSocket?.isConnected != true) {
            me.kavishdevar.librepods.presentation.widgets.BatteryWidget.update(app)
        }
    }

    fun batteries(): List<me.kavishdevar.librepods.data.Battery> {
        if (!isEnabled()) return emptyList()
        val status = decoder.getMostRecentStatus() ?: return emptyList()
        return me.kavishdevar.librepods.data.AirPodsNotifications.BatteryNotification().apply {
            setBatteryDirect(status.leftBattery, status.isLeftCharging, status.rightBattery,
                status.isRightCharging, status.caseBattery, status.isCaseCharging)
        }.getBattery().filter { it.status != me.kavishdevar.librepods.data.BatteryStatus.DISCONNECTED }
    }

    @SuppressLint("MissingPermission")
    fun takeoverReason(status: BLEManager.AirPodsStatus): String? {
        if (!isEnabled() || preferences.getBoolean("ble_only_mode", false) ||
            (!status.isLeftInEar && !status.isRightInEar)) return null
        val allowed = when (status.connectionState) {
            "Disconnected" -> preferences.getBoolean("takeover_when_disconnected", false)
            "Idle" -> preferences.getBoolean("takeover_when_idle", false)
            "Music" -> preferences.getBoolean("takeover_when_music", false)
            "Call", "Ringing", "Hanging Up" -> preferences.getBoolean("takeover_when_call", false)
            else -> false
        }
        if (!allowed) return null
        if (preferences.getBoolean("takeover_when_ringing_call", false) &&
            app.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
            app.getSystemService(TelecomManager::class.java)?.isInCall == true) return "call"
        if (preferences.getBoolean("takeover_when_media_start", false) &&
            app.getSystemService(AudioManager::class.java).isMusicActive) return "music"
        return null
    }

    private fun keyIdentity(): String? {
        val irk = key("IRK") ?: return null
        val encryption = key("ENC_KEY") ?: byteArrayOf()
        return Base64.encodeToString(MessageDigest.getInstance("SHA-256").digest(irk + encryption), Base64.NO_WRAP)
    }

    /** A queued job can outlive this process; carry the observed packet, never the private keys. */
    fun restoreObservation(extras: PersistableBundle): Boolean {
        if (!isEnabled() || extras.getString("address") != preferences.getString("mac_address", "") ||
            extras.getString("identity") != keyIdentity() ||
            extras.getInt("boot", -1) != Settings.Global.getInt(app.contentResolver, Settings.Global.BOOT_COUNT, -2)) return false
        if (decoder.getMostRecentStatus() != null) return true
        val address = extras.getString("rpa") ?: return false
        val encoded = extras.getString("advertisement") ?: return false
        val packet = try { Base64.decode(encoded, Base64.NO_WRAP) } catch (_: IllegalArgumentException) { return false }
        // Re-verifies RPA with the current key and the original observation's freshness.
        return decoder.accept(address, packet, extras.getLong("observed", -1)) != null
    }

    private fun observeBudget(address: String, reason: String?): NearbyAttemptBudget {
        val state = app.getSharedPreferences("nearby_attempt", Context.MODE_PRIVATE)
        val saved = NearbyAttemptBudget(state.getString("address", null), state.getString("reason", null),
            state.getInt("attempts", 0), state.getLong("last", -60_000))
        val observed = saved.observe(address, reason)
        if (observed != saved) state.edit {
            putString("address", observed.address)
            putString("reason", observed.reason)
            putInt("attempts", observed.attempts)
        }
        return observed
    }

    /** Admission failures do not spend attempts; only an engine that starts a session does. */
    internal fun startTakeover(engine: AirPodsService, address: String, reason: String): ConnectionSession? {
        val next = observeBudget(address, reason).reserve(SystemClock.elapsedRealtime()) ?: return null
        val attempt = engine.beginNearbyTakeover(reason) ?: return null
        app.getSharedPreferences("nearby_attempt", Context.MODE_PRIVATE).edit {
            putLong("last", next.lastAttempt)
            putInt("attempts", next.attempts)
        }
        return attempt
    }

    private fun maybeScheduleTakeover(status: BLEManager.AirPodsStatus) {
        if (BluetoothConnectionManager.aacpSocket?.isConnected == true) return
        val address = preferences.getString("mac_address", "").orEmpty()
        if (preferences.getString("nearby_suppressed_address", null) == address) return
        val reason = takeoverReason(status)
        val observed = observeBudget(address, reason)
        if (reason == null || CompanionConnection.selectedAssociation(app) == null) return
        val now = SystemClock.elapsedRealtime()
        if (observed.reserve(now) == null) return
        val state = app.getSharedPreferences("nearby_attempt", Context.MODE_PRIVATE)
        val scheduled = state.getLong("last_scheduled", -60_000)
        // Also bound failed admissions, without consuming the three actual connection attempts.
        if (now >= scheduled && now - scheduled < 60_000) return
        val scheduler = app.getSystemService(JobScheduler::class.java)
        if (scheduler.getPendingJob(TAKEOVER_JOB_ID) != null) return
        val observation = decoder.getObservation() ?: return
        val identity = keyIdentity() ?: return
        val extras = PersistableBundle().apply {
            putString("address", address)
            putString("reason", reason)
            putString("identity", identity)
            putInt("boot", Settings.Global.getInt(app.contentResolver, Settings.Global.BOOT_COUNT, -1))
            putString("rpa", observation.address)
            putString("advertisement", Base64.encodeToString(observation.data, Base64.NO_WRAP))
            putLong("observed", observation.seen)
        }
        val job = JobInfo.Builder(TAKEOVER_JOB_ID, ComponentName(app, NearbyTakeoverService::class.java))
            .setExtras(extras).setOverrideDeadline(0).build()
        if (scheduler.schedule(job) == JobScheduler.RESULT_SUCCESS) state.edit { putLong("last_scheduled", now) }
    }
}
