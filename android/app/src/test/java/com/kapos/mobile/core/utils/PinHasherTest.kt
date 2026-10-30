package com.kapos.mobile.core.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {

    @Test
    fun testHashProduces64CharHexString() {
        val hash = PinHasher.hash("1234")
        assertEquals(64, hash.length)
        assertTrue(PinHasher.isHashed(hash))
    }

    @Test
    fun testDifferentPinsProduceDifferentHashes() {
        val hash1 = PinHasher.hash("1234")
        val hash2 = PinHasher.hash("1111")
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun testVerifySucceedsWithCorrectPin() {
        val hash = PinHasher.hash("5678")
        assertTrue(PinHasher.verify("5678", hash))
    }

    @Test
    fun testVerifyFailsWithIncorrectPin() {
        val hash = PinHasher.hash("5678")
        assertFalse(PinHasher.verify("0000", hash))
    }

    @Test
    fun testIsHashedDetection() {
        assertFalse(PinHasher.isHashed("1234"))
        assertFalse(PinHasher.isHashed("1111"))
        assertFalse(PinHasher.isHashed(""))
        val validHash = PinHasher.hash("9999")
        assertTrue(PinHasher.isHashed(validHash))
    }
}
