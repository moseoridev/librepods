package me.kavishdevar.librepods.services

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.kavishdevar.librepods.bluetooth.CompanionConnection
import me.kavishdevar.librepods.bluetooth.ConnectionSession
import me.kavishdevar.librepods.bluetooth.NearbyDetection

/** A system-owned, interruptible ten-second opportunity to hand a takeover to CDM. */
class NearbyTakeoverService : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var parameters: JobParameters? = null
    private var bound = false
    private var engine: AirPodsService? = null
    private var startedAttempt: ConnectionSession? = null
    private var timeout: Job? = null
    private var ownership: Job? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val params = parameters ?: return
            engine = (binder as AirPodsService.LocalBinder).getService()
            if (!eligible(params)) { finish(); return }
            startedAttempt = NearbyDetection.startTakeover(checkNotNull(engine),
                checkNotNull(params.extras.getString("address")), checkNotNull(params.extras.getString("reason")))
            if (startedAttempt == null) finish()
        }
        override fun onServiceDisconnected(name: ComponentName?) { finish() }
        override fun onBindingDied(name: ComponentName?) { finish() }
        override fun onNullBinding(name: ComponentName?) { finish() }
    }

    private fun eligible(params: JobParameters): Boolean {
        val preferences = getSharedPreferences("settings", MODE_PRIVATE)
        val address = params.extras.getString("address") ?: return false
        if (!NearbyDetection.restoreObservation(params.extras)) return false
        val status = NearbyDetection.decoder.getMostRecentStatus() ?: return false
        return NearbyDetection.isEnabled() &&
            preferences.getString("mac_address", "") == address &&
            preferences.getString("nearby_suppressed_address", null) != address &&
            CompanionConnection.selectedAssociation(this) != null &&
            NearbyDetection.takeoverReason(status) == params.extras.getString("reason")
    }

    override fun onStartJob(params: JobParameters): Boolean {
        NearbyDetection.initialize(this)
        parameters = params
        scope.launch {
            if (parameters !== params) return@launch
            if (!eligible(params)) { finish(); return@launch }
            timeout = scope.launch { delay(10_000); finish() }
            ownership = scope.launch {
                CompanionConnection.engineOwner.collect { address ->
                    if (address != null && address == params.extras.getString("address")) finish()
                }
            }
            bound = try {
                bindService(Intent(this@NearbyTakeoverService, AirPodsService::class.java), connection, BIND_AUTO_CREATE)
            } catch (_: SecurityException) { false }
            if (!bound) finish()
        }
        return true
    }

    private fun finish(notify: Boolean = true) {
        val params = parameters ?: return
        parameters = null
        timeout?.cancel()
        ownership?.cancel()
        startedAttempt?.let { engine?.endNearbyTakeover(it) }
        startedAttempt = null
        engine = null
        if (bound) unbindService(connection)
        bound = false
        if (notify) jobFinished(params, false)
    }

    override fun onStopJob(params: JobParameters): Boolean {
        finish(notify = false)
        return false
    }

    override fun onDestroy() {
        finish(notify = false)
        scope.cancel()
        super.onDestroy()
    }
}
