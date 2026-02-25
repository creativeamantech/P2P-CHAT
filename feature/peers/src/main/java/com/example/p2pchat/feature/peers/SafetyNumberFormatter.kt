package com.example.p2pchat.feature.peers

fun formatSafetyNumber(number: String?): String {
    if (number == null) return "N/A"
    // Assuming format "12345 67890 ..." already done by generator
    // But if we want to display it as blocks:
    // It is already space separated by generator.
    // We can add newlines for better display if it's long.
    // 12 groups of 5 digits = 60 digits + spaces.
    // Let's break it into 4 lines of 3 groups.

    val parts = number.split(" ")
    return parts.chunked(3).joinToString("\n") { it.joinToString(" ") }
}
