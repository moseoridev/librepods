package me.kavishdevar.librepods.bluetooth

import me.kavishdevar.librepods.utils.BluetoothCryptography
import org.junit.Assert.*
import org.junit.Test

class BLEManagerTest {
    private val irk = ByteArray(16) { it.toByte() }
    private var now = 10_000L
    private fun decoder() = BLEManager({ irk to null }, { now })
    private fun address(seed: Int = 1): String {
        val random = byteArrayOf(seed.toByte(), 2, 0x40)
        return (BluetoothCryptography.ah(irk, random) + random).reversedArray()
            .joinToString(":") { "%02X".format(it) }
    }
    private fun advertisement() = ByteArray(27).apply {
        this[0] = 7; this[1] = 25; this[2] = 1
        this[3] = 0x14; this[4] = 0x20
        this[5] = 0x28; this[6] = 0x76; this[7] = 5; this[10] = 4
    }

    @Test fun foreignAndMalformedAdvertisementsCannotReplaceTheSelectedDevice() {
        val decoder = decoder()
        val own = decoder.accept(address(), advertisement(), now)
        assertNotNull(own)
        now++
        assertNull(decoder.accept("00:00:00:00:00:00", advertisement(), now))
        assertNull(decoder.accept(address(), advertisement().copyOf(20), now))
        assertNull(decoder.accept(address(), advertisement().apply { this[0] = 8 }, now))
        assertEquals(own, decoder.getMostRecentStatus())
    }

    @Test fun delayedBatchesCannotRollBackStateAndFreshnessExpiresWithoutPolling() {
        val decoder = decoder()
        val latest = decoder.accept(address(), advertisement(), now)
        assertNull(decoder.accept(address(), advertisement().apply { this[6] = 0x11 }, now - 1))
        assertNull(decoder.accept(address(), advertisement(), now + 1))
        assertEquals(latest, decoder.getMostRecentStatus())
        now += BLEManager.MAX_AGE_MS + 1
        assertNull(decoder.getMostRecentStatus())
        assertNull(decoder.accept(address(), advertisement(), 10_000))
    }

    @Test fun repeatedAdvertisementsAndPrivateAddressRotationDoNotCountAsContentChanges() {
        val decoder = decoder()
        val first = decoder.accept(address(), advertisement(), now)
        now++
        val repeated = decoder.accept(address(), advertisement(), now)
        now++
        val rotated = decoder.accept(address(3), advertisement(), now)
        assertNotNull(rotated)
        assertTrue(BLEManager.sameContent(first, repeated))
        assertTrue(BLEManager.sameContent(first, rotated))
        now++
        val charging = decoder.accept(address(3), advertisement().apply { this[7] = 0x15 }, now)
        assertFalse(BLEManager.sameContent(rotated, charging))
    }

    @Test fun changingIdentityInvalidatesPreviouslyTrustedAddresses() {
        var key = irk
        val decoder = BLEManager({ key to null }, { now })
        assertNotNull(decoder.accept(address(), advertisement(), now))
        key = ByteArray(16) { 42 }
        now++
        assertNull(decoder.accept(address(), advertisement(), now))
        assertNull(decoder.getMostRecentStatus())
    }
    @Test fun queuedObservationSurvivesDecoderRecreationButStillNeedsFreshnessAndTheSameKey() {
        val original = decoder()
        val status = original.accept(address(), advertisement(), now)
        val observation = checkNotNull(original.getObservation())
        val recreated = decoder()
        assertNull(recreated.getMostRecentStatus())
        assertEquals(status, recreated.accept(observation.address, observation.data, observation.seen))
        val otherDevice = BLEManager({ ByteArray(16) { 42 } to null }, { now })
        assertNull(otherDevice.accept(observation.address, observation.data, observation.seen))
        now += BLEManager.MAX_AGE_MS + 1
        assertNull(decoder().accept(observation.address, observation.data, observation.seen))
    }

}
