package com.example.p2pchat.core.crypto.privacy

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessagePaddingTest {

    private val messagePadding = MessagePadding()

    @Test
    fun testPaddingAndUnpadding() {
        val original = "Hello World".toByteArray()
        val padded = messagePadding.pad(original)

        // Ensure size is multiple of 512
        assertEquals(0, padded.size % 512)
        assertTrue(padded.size >= 512)

        val unpadded = messagePadding.unpad(padded)
        assertArrayEquals(original, unpadded)
    }

    @Test
    fun testPaddingLargeMessage() {
        val original = ByteArray(1000) { 0x01 }
        val padded = messagePadding.pad(original)

        assertEquals(0, padded.size % 512)
        assertTrue(padded.size >= 1024) // 1000 + 2 + padding -> > 1024? No, 512*2=1024. 1000 < 1024.
        // 1000 + 2 = 1002. So 1024 should be enough.
        assertEquals(1024, padded.size)

        val unpadded = messagePadding.unpad(padded)
        assertArrayEquals(original, unpadded)
    }

    @Test
    fun testExactBlockSizeBoundary() {
        // 512 - 2 = 510 bytes + 2 length = 512.
        val original = ByteArray(510) { 0x02 }
        val padded = messagePadding.pad(original)

        assertEquals(512, padded.size)

        val unpadded = messagePadding.unpad(padded)
        assertArrayEquals(original, unpadded)
    }

    @Test
    fun testJustOverBlockSize() {
        // 511 bytes. +2 length = 513. Needs 1024.
        val original = ByteArray(511) { 0x03 }
        val padded = messagePadding.pad(original)

        assertEquals(1024, padded.size)

        val unpadded = messagePadding.unpad(padded)
        assertArrayEquals(original, unpadded)
    }
}
