package me.kavishdevar.librepods.core.bluetooth.att

import org.junit.Assert.*
import org.junit.Test

class ATTResponseTest {
    @Test fun onlyMatchingSuccessResponseExposesPayload() {
        assertArrayEquals(byteArrayOf(1, 2), ATTResponse.payload(byteArrayOf(0x0B, 1, 2), 0x0B))
        assertArrayEquals(byteArrayOf(), ATTResponse.payload(byteArrayOf(0x13), 0x13))
        assertNull(ATTResponse.payload(byteArrayOf(), 0x0B))
        assertNull(ATTResponse.payload(byteArrayOf(0x01, 0x0A, 0x18, 0, 5), 0x0B))
        assertNull(ATTResponse.payload(byteArrayOf(0x13), 0x0B))
    }
}
