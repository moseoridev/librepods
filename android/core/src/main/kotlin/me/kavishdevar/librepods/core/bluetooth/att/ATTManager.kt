/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package me.kavishdevar.librepods.core.bluetooth.att

import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.kavishdevar.librepods.core.bluetooth.ConnectionSession
import me.kavishdevar.librepods.core.devices.AppleDevice

enum class ATTHandle(val value: Int) {
    TRANSPARENCY(0x18), LOUD_SOUND_REDUCTION(0x1B), HEARING_AID(0x2A)
}

/** Short ATT transactions; no permanently connected ATT channel or idle polling owner. */
class ATTManager(private val device: AppleDevice) {
    private val mutex = Mutex()

    suspend fun readCharacteristic(handle: ATTHandle): ByteArray? =
        exchange(byteArrayOf(0x0A, handle.value.toByte(), 0x00), 0x0B)

    suspend fun writeCharacteristic(handle: ATTHandle, data: ByteArray): Boolean =
        exchange(byteArrayOf(0x12, handle.value.toByte(), 0x00) + data, 0x13) != null

    private suspend fun exchange(request: ByteArray, expected: Int): ByteArray? = mutex.withLock {
        withContext(Dispatchers.Main.immediate) {
            val parent = device.aacp.currentSession() ?: return@withContext null
            val operation = parent.own(ConnectionSession(parent.scope))
            try {
                val response = operation.connect(4_000) {
                    val socket = operation.own(device.createSocket(
                        ParcelUuid.fromString("00000000-0000-0000-0000-000000000000"), 31))
                    socket.connect()
                    socket.outputStream.write(request)
                    socket.outputStream.flush()
                    val buffer = ByteArray(512)
                    val count = socket.inputStream.read(buffer)
                    if (count > 0) buffer.copyOf(count) else byteArrayOf()
                }
                if (device.aacp.currentSession() !== parent) null
                else ATTResponse.payload(response, expected)
            } catch (error: Exception) {
                if (error is CancellationException && error !is TimeoutCancellationException) throw error
                Log.w("ATTManager", "ATT transaction failed", error)
                null
            } finally {
                operation.close()
                parent.release(operation)
            }
        }
    }
}

internal object ATTResponse {
    fun payload(response: ByteArray, expected: Int): ByteArray? =
        if (response.isNotEmpty() && (response[0].toInt() and 0xff) == expected)
            response.copyOfRange(1, response.size) else null
}
