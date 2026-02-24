package com.example.p2pchat.core.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class FingerprintGeneratorTest {

    @Test
    fun testSafetyNumberConsistency() {
        val generator = FingerprintGenerator()
        val keyA = ByteArray(32) { it.toByte() }
        val keyB = ByteArray(32) { (it + 1).toByte() }

        val sn1 = generator.generateSafetyNumber(keyA, keyB)
        val sn2 = generator.generateSafetyNumber(keyB, keyA)

        // Should be order independent
        assertEquals(sn1, sn2)

        // Should look like 12 groups of 5 digits
        // Example: "12345 67890 ..."
        // Total 12 groups separated by spaces -> 11 spaces.
        // Total chars = 12*5 + 11 = 60 + 11 = 71.

        val parts = sn1.split(" ")
        assertEquals(12, parts.size)
        parts.forEach {
            assertEquals(5, it.length)
            assert(it.all { c -> c.isDigit() })
        }
    }

    @Test
    fun testSafetyNumberUniqueness() {
        val generator = FingerprintGenerator()
        val keyA = ByteArray(32) { 1 }
        val keyB = ByteArray(32) { 2 }
        val keyC = ByteArray(32) { 3 }

        val sn1 = generator.generateSafetyNumber(keyA, keyB)
        val sn2 = generator.generateSafetyNumber(keyA, keyC)

        assertNotEquals(sn1, sn2)
    }
}
