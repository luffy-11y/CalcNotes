package com.calcnotes.app.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CalcEvaluatorTest {

    private val evaluator = CalcEvaluator(referenceDate = LocalDate.of(2026, 11, 14))
    private val defaultConfig = CalcFormatConfig()

    @Test
    fun `ignores words and sums lines accurately`() {
        val note = """
            56 deepak
            91 Rajan
        """.trimIndent()

        val summary = evaluator.evaluateNote(note, defaultConfig)
        assertEquals("56", summary.lineResults[0].displayText)
        assertEquals("91", summary.lineResults[1].displayText)
        assertEquals(147.0, summary.grandTotal, 0.001)
        assertEquals("147", summary.formattedGrandTotal)
    }

    @Test
    fun `handles multiple numbers on one line according to MultiNumberMode`() {
        val line = "rent 12000 and food 4500"

        val sumSummary = evaluator.evaluateNote(
            line,
            defaultConfig.copy(multiNumberMode = MultiNumberMode.SUM)
        )
        assertEquals("16,500", sumSummary.lineResults[0].displayText)
        assertEquals(16500.0, sumSummary.grandTotal, 0.001)

        val eachSummary = evaluator.evaluateNote(
            line,
            defaultConfig.copy(multiNumberMode = MultiNumberMode.SHOW_EACH)
        )
        assertEquals("12,000 | 4,500", eachSummary.lineResults[0].displayText)

        val firstSummary = evaluator.evaluateNote(
            line,
            defaultConfig.copy(multiNumberMode = MultiNumberMode.FIRST_ONLY)
        )
        assertEquals("12,000", firstSummary.lineResults[0].displayText)
    }

    @Test
    fun `handles single number in middle of sentence and explicit multiply`() {
        val note = """
            paid 250 to Sam
            12 x 3 apples
        """.trimIndent()

        val summary = evaluator.evaluateNote(note, defaultConfig)
        assertEquals("250", summary.lineResults[0].displayText)
        assertEquals("36", summary.lineResults[1].displayText)
        assertEquals(286.0, summary.grandTotal, 0.001)
    }

    @Test
    fun `handles variables, prev keyword, sum and avg keywords, and section subtotals`() {
        val note = """
            # Store Order
            price = 250
            price * 3
            prev + 100
            sum
            avg
        """.trimIndent()

        val summary = evaluator.evaluateNote(note, defaultConfig)
        assertEquals("1,850", summary.lineResults[0].displayText)
        assertEquals("250", summary.lineResults[1].displayText)
        assertEquals("750", summary.lineResults[2].displayText)
        assertEquals("850", summary.lineResults[3].displayText)
        assertEquals("1,850", summary.lineResults[4].displayText)
        assertEquals("616.67", summary.lineResults[5].displayText)
    }

    @Test
    fun `handles percentages, k-m-b suffixes, and math functions`() {
        val note = """
            20% of 500
            500 + 10%
            500 - 15%
            1.5k + 2,500
            sqrt(144) + max(10, 25)
        """.trimIndent()

        val summary = evaluator.evaluateNote(note, defaultConfig)
        assertEquals("100", summary.lineResults[0].displayText)
        assertEquals("550", summary.lineResults[1].displayText)
        assertEquals("425", summary.lineResults[2].displayText)
        assertEquals("4,000", summary.lineResults[3].displayText)
        assertEquals("37", summary.lineResults[4].displayText)
    }

    @Test
    fun `handles units, offline currency, time math, and date math`() {
        val note = """
            5 km in miles
            100 usd in inr
            2h 30m + 45m
            today + 30 days
            days until 25 dec
        """.trimIndent()

        val summary = evaluator.evaluateNote(note, defaultConfig)
        assertEquals("3.11 miles", summary.lineResults[0].displayText)
        assertEquals("8,400 INR", summary.lineResults[1].displayText)
        assertTrue(summary.hasCurrencyEstimate)
        assertEquals("3h 15m", summary.lineResults[2].displayText)
        assertEquals("14 Dec 2026", summary.lineResults[3].displayText)
        assertEquals("41 days", summary.lineResults[4].displayText)
    }

    @Test
    fun `never crashes on empty or invalid syntax lines`() {
        val note = """
            Just a regular text note with no numbers
            5 + * /
            
        """.trimIndent()

        val summary = evaluator.evaluateNote(note, defaultConfig)
        assertNotNull(summary)
        assertEquals("", summary.lineResults[0].displayText)
        assertEquals("", summary.lineResults[1].displayText)
        assertEquals("", summary.lineResults[2].displayText)
    }
}
