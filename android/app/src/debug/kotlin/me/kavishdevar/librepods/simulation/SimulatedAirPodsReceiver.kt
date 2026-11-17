package me.kavishdevar.librepods.simulation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Debug-only adb entry point for the simulated accessory.
 *
 *   adb shell am broadcast -a me.kavishdevar.librepods.debug.SIMULATED_AIRPODS \
 *       -n me.kavishdevar.librepods/me.kavishdevar.librepods.simulation.SimulatedAirPodsReceiver \
 *       --es command add --es scenario pro3
 *
 * Commands: add, remove, status, scenarios.
 */
class SimulatedAirPodsReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION = "me.kavishdevar.librepods.debug.SIMULATED_AIRPODS"
        const val EXTRA_COMMAND = "command"
        const val EXTRA_SCENARIO = "scenario"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).launch {
            try {
                val result = when (intent.getStringExtra(EXTRA_COMMAND)?.lowercase()) {
                    "add" -> SimulatedAirPodsController.add(app, intent.getStringExtra(EXTRA_SCENARIO))
                    "remove" -> SimulatedAirPodsController.remove(app)
                    "status" -> SimulatedAirPodsController.status()
                    "scenarios" -> SimulatedAirPodsController.scenarios()
                    else -> "error: use --es command add|remove|status|scenarios"
                }
                android.util.Log.i("SimulatedAirPods", result)
            } finally {
                pending.finish()
            }
        }
    }
}
