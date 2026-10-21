package com.kapos.mobile.feature.checkout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class OrderNoGenerationTest {

    @Test
    fun testGenerateOrderNoFormatAndUniqueness() {
        val date = Date()
        val generated = (1..50).map { generateOrderNo(date) }

        // All should match pattern KA-yyyyMMdd-HHmmss-XXX
        val pattern = Regex("^KA-\\d{8}-\\d{6}-\\d{3}$")
        for (orderNo in generated) {
            assertTrue("OrderNo '$orderNo' should match pattern KA-yyyyMMdd-HHmmss-XXX", pattern.matches(orderNo))
        }

        // Even with the same date/timestamp, all 50 generated order numbers should have distinct randomness
        // Note: With 900 possibilities (100..999), 50 random samples should have high diversity (> 20 unique values)
        val uniqueCount = generated.toSet().size
        assertTrue("Expected high diversity in generated order numbers, got $uniqueCount / 50", uniqueCount > 25)
    }
}
