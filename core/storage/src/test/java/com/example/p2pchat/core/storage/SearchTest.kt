package com.example.p2pchat.core.storage

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchTest {
    @Test
    fun testSearchQueryFormatting() {
        val query = "hello"
        val ftsQuery = "*$query*"
        assertEquals("*hello*", ftsQuery)

        val sqlQuery = "%$query%"
        assertEquals("%hello%", sqlQuery)
    }
}
