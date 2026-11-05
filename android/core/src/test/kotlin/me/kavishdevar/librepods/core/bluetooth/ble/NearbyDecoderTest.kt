package me.kavishdevar.librepods.core.bluetooth.ble

import me.kavishdevar.librepods.core.bluetooth.ah
import org.junit.Assert.*
import org.junit.Test

class NearbyDecoderTest {
    @Test fun caseAdvertUsesItsOwnFreshnessAndRetainsRewriteBatteryFormat() {
        val key = ByteArray(16) { (it + 30).toByte() }
        val decoder = NearbyDecoder({ irk to key }, { now })
        val plain = ByteArray(16).apply { this[1] = 0x20; this[3] = 0xC8.toByte(); this[4] = 60; this[5] = 0xFF.toByte() }
        val cipher = javax.crypto.Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, javax.crypto.spec.SecretKeySpec(key, "AES"))
        val data = byteArrayOf(7, 17, 6) + cipher.doFinal(plain)
        val status = checkNotNull(decoder.acceptCase("case", data, now))
        assertEquals(72, status.caseBattery)
        assertTrue(status.isCaseCharging)
        assertEquals(60, status.leftBattery)
        assertNull(status.rightBattery)
        assertNull(decoder.acceptCase("case", data, now - 1))
        assertNull(decoder.acceptCase("case", data.copyOf(10), ++now))
        now += NearbyDecoder.MAX_AGE_MS + 1
        assertNull(decoder.getCaseStatus())
    }

    @Test fun casePacketWithWrongKeyOrInvalidBatteryIsRejected() {
        val key = ByteArray(16)
        val decoder = NearbyDecoder({ irk to key }, { now })
        val plain = ByteArray(16).apply { this[1] = 0x20; this[3] = 101 }
        val cipher = javax.crypto.Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, javax.crypto.spec.SecretKeySpec(key, "AES"))
        assertNull(decoder.acceptCase("case", byteArrayOf(7, 17, 6) + cipher.doFinal(plain), now))
    }

    private val irk = ByteArray(16) { it.toByte() }
    private var now = 10_000L
    private fun decoder() = NearbyDecoder({ irk to null }, { now })
    private fun address(seed: Int = 1): String {
        val random = byteArrayOf(seed.toByte(), 2, 0x40)
        return (ah(irk, random) + random).reversedArray()
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
        now += NearbyDecoder.MAX_AGE_MS + 1
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
        assertTrue(NearbyDecoder.sameContent(first, repeated))
        assertTrue(NearbyDecoder.sameContent(first, rotated))
        now++
        val charging = decoder.accept(address(3), advertisement().apply { this[7] = 0x15 }, now)
        assertFalse(NearbyDecoder.sameContent(rotated, charging))
    }

    @Test fun changingIdentityInvalidatesPreviouslyTrustedAddresses() {
        var key = irk
        val decoder = NearbyDecoder({ key to null }, { now })
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
        val otherDevice = NearbyDecoder({ ByteArray(16) { 42 } to null }, { now })
        assertNull(otherDevice.accept(observation.address, observation.data, observation.seen))
        now += NearbyDecoder.MAX_AGE_MS + 1
        assertNull(decoder().accept(observation.address, observation.data, observation.seen))
    }

}
