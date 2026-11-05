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
import me.kavishdevar.librepods.bluetooth.NearbyDetection
import me.kavishdevar.librepods.core.devices.AppleDevice
import java.io.Closeable

/** A ten-second, interruptible opportunity; only an actual CDM binding completes handoff. */
class NearbyTakeoverService : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var parameters: JobParameters? = null
    private var bound = false
    private var timeout: Job? = null
    private var admission: Job? = null
    private var ownership: Job? = null
    private var lease: Closeable? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val params = parameters ?: return
            val engine = (binder as LibrePodsService.LocalBinder).getService()
            admission = scope.launch {
                val sample = NearbyDetection.eligible(params.extras)
                if (parameters !== params) return@launch
                val device = sample?.let { engine.devices.value[it.address] as? AppleDevice }
                if (device == null) { finish(); return@launch }
                device.awaitInitialized()
                if (parameters !== params) return@launch
                val current = NearbyDetection.eligible(params.extras)
                if (current == null || parameters !== params) { finish(); return@launch }
                device.acceptNearby(current.batteries, current.status)
                lease = NearbyDetection.begin(device, current)
                if (lease == null) finish()
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) { finish() }
        override fun onBindingDied(name: ComponentName?) { finish() }
        override fun onNullBinding(name: ComponentName?) { finish() }
    }

    override fun onStartJob(params: JobParameters): Boolean {
        finish()
        NearbyDetection.initialize(this)
        parameters = params
        timeout = scope.launch { delay(10_000); finish() }
        ownership = scope.launch {
            CompanionConnection.engineOwners.collect { owners ->
                if (params.extras.getString("address") in owners) finish()
            }
        }
        admission = scope.launch {
            if (NearbyDetection.eligible(params.extras) == null) { finish(); return@launch }
            if (parameters !== params) return@launch
            bound = try {
                bindService(Intent(this@NearbyTakeoverService, LibrePodsService::class.java), connection, BIND_AUTO_CREATE)
            } catch (_: SecurityException) { false }
            if (!bound) finish()
        }
        return true
    }

    private fun finish(notify: Boolean = true) {
        val params = parameters ?: return
        parameters = null
        timeout?.cancel()
        admission?.cancel()
        ownership?.cancel()
        lease?.close()
        lease = null
        if (bound) unbindService(connection)
        bound = false
        if (notify) jobFinished(params, false)
    }
    override fun onStopJob(params: JobParameters): Boolean { finish(false); return false }
    override fun onDestroy() { finish(false); scope.cancel(); super.onDestroy() }
}
