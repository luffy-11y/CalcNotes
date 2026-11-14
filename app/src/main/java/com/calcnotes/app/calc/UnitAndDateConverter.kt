package com.calcnotes.app.calc

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object UnitAndDateConverter {

    private val usdRates = mapOf(
        "usd" to 1.0,
        "$" to 1.0,
        "inr" to 84.0,
        "rs" to 84.0,
        "₹" to 84.0,
        "eur" to 0.92,
        "€" to 0.92,
        "gbp" to 0.79,
        "£" to 0.79,
        "jpy" to 154.0,
        "¥" to 154.0,
        "cad" to 1.39,
        "aud" to 1.53,
        "sgd" to 1.34,
        "aed" to 3.67
    )

    private val lengthToMeters = mapOf(
        "m" to 1.0, "meter" to 1.0, "meters" to 1.0, "metre" to 1.0, "metres" to 1.0,
        "km" to 1000.0, "kilometer" to 1000.0, "kilometers" to 1000.0,
        "cm" to 0.01, "centimeter" to 0.01, "centimeters" to 0.01,
        "mm" to 0.001, "millimeter" to 0.001, "millimeters" to 0.001,
        "mi" to 1609.344, "mile" to 1609.344, "miles" to 1609.344,
        "ft" to 0.3048, "foot" to 0.3048, "feet" to 0.3048,
        "in" to 0.0254, "inch" to 0.0254, "inches" to 0.0254,
        "yd" to 0.9144, "yard" to 0.9144, "yards" to 0.9144
    )

    private val weightToKg = mapOf(
        "kg" to 1.0, "kilogram" to 1.0, "kilograms" to 1.0, "kgs" to 1.0,
        "g" to 0.001, "gram" to 0.001, "grams" to 0.001, "gm" to 0.001,
        "mg" to 0.000001, "milligram" to 0.000001, "milligrams" to 0.000001,
        "lb" to 0.45359237, "lbs" to 0.45359237, "pound" to 0.45359237, "pounds" to 0.45359237,
        "oz" to 0.0283495231, "ounce" to 0.0283495231, "ounces" to 0.0283495231
    )

    private val volumeToLiters = mapOf(
        "l" to 1.0, "liter" to 1.0, "liters" to 1.0, "litre" to 1.0, "litres" to 1.0,
        "ml" to 0.001, "milliliter" to 0.001, "milliliters" to 0.001,
        "gal" to 3.78541178, "gallon" to 3.78541178, "gallons" to 3.78541178,
        "cup" to 0.236588, "cups" to 0.236588
    )

    private val conversionPattern = Regex(
        """^\s*([+-]?\d+(?:[.,]\d+)?(?:\s*[kmb])?)\s*([a-zA-Z$€£¥₹]+)\s+(?:in|to|as)\s+([a-zA-Z$€£¥₹]+)\s*$""",
        RegexOption.IGNORE_CASE
    )

    private val timeTermPattern = Regex(
        """([+-])?\s*(?:(\d+(?:\.\d+)?)\s*h(?:ours?|rs?)?)?\s*(?:(\d+(?:\.\d+)?)\s*m(?:in(?:ute)?s?)?)?""",
        RegexOption.IGNORE_CASE
    )

    private val todayOffsetPattern = Regex(
        """^\s*today\s*([+-])\s*(\d+)\s*(day|days|d|week|weeks|w|month|months|year|years)\s*$""",
        RegexOption.IGNORE_CASE
    )

    private val daysUntilPattern = Regex(
        """^\s*days\s+(?:until|to|till)\s+(\d{1,2})\s+([a-zA-Z]{3,9})(?:\s+(\d{4}))?\s*$""",
        RegexOption.IGNORE_CASE
    )

    private val monthMap = mapOf(
        "jan" to 1, "january" to 1,
        "feb" to 2, "february" to 2,
        "mar" to 3, "march" to 3,
        "apr" to 4, "april" to 4,
        "may" to 5,
        "jun" to 6, "june" to 6,
        "jul" to 7, "july" to 7,
        "aug" to 8, "august" to 8,
        "sep" to 9, "sept" to 9, "september" to 9,
        "oct" to 10, "october" to 10,
        "nov" to 11, "november" to 11,
        "dec" to 12, "december" to 12
    )

    data class SpecialEvalResult(
        val numericValue: Double?,
        val displayText: String,
        val kind: LineKind,
        val isCurrencyEstimate: Boolean = false
    )

    fun tryEvaluateSpecial(
        line: String,
        formatNumber: (Double) -> String,
        referenceDate: LocalDate = LocalDate.now()
    ): SpecialEvalResult? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null

        val convMatch = conversionPattern.matchEntire(trimmed)
        if (convMatch != null) {
            val rawNum = parseScaledNumber(convMatch.groupValues[1]) ?: return null
            val fromUnit = convMatch.groupValues[2].lowercase(Locale.US)
            val toUnit = convMatch.groupValues[3].lowercase(Locale.US)

            if (usdRates.containsKey(fromUnit) && usdRates.containsKey(toUnit)) {
                val inUsd = rawNum / usdRates.getValue(fromUnit)
                val converted = inUsd * usdRates.getValue(toUnit)
                val label = toUnit.uppercase(Locale.US)
                return SpecialEvalResult(
                    numericValue = converted,
                    displayText = "${formatNumber(converted)} $label",
                    kind = LineKind.UNIT_OR_CURRENCY,
                    isCurrencyEstimate = true
                )
            }

            if (lengthToMeters.containsKey(fromUnit) && lengthToMeters.containsKey(toUnit)) {
                val meters = rawNum * lengthToMeters.getValue(fromUnit)
                val converted = meters / lengthToMeters.getValue(toUnit)
                return SpecialEvalResult(
                    numericValue = converted,
                    displayText = "${formatNumber(converted)} $toUnit",
                    kind = LineKind.UNIT_OR_CURRENCY
                )
            }

            if (weightToKg.containsKey(fromUnit) && weightToKg.containsKey(toUnit)) {
                val kg = rawNum * weightToKg.getValue(fromUnit)
                val converted = kg / weightToKg.getValue(toUnit)
                return SpecialEvalResult(
                    numericValue = converted,
                    displayText = "${formatNumber(converted)} $toUnit",
                    kind = LineKind.UNIT_OR_CURRENCY
                )
            }

            if (volumeToLiters.containsKey(fromUnit) && volumeToLiters.containsKey(toUnit)) {
                val liters = rawNum * volumeToLiters.getValue(fromUnit)
                val converted = liters / volumeToLiters.getValue(toUnit)
                return SpecialEvalResult(
                    numericValue = converted,
                    displayText = "${formatNumber(converted)} $toUnit",
                    kind = LineKind.UNIT_OR_CURRENCY
                )
            }

            if ((fromUnit == "c" || fromUnit == "celsius") && (toUnit == "f" || toUnit == "fahrenheit")) {
                val converted = (rawNum * 9.0 / 5.0) + 32.0
                return SpecialEvalResult(
                    numericValue = converted,
                    displayText = "${formatNumber(converted)} °F",
                    kind = LineKind.UNIT_OR_CURRENCY
                )
            }
            if ((fromUnit == "f" || fromUnit == "fahrenheit") && (toUnit == "c" || toUnit == "celsius")) {
                val converted = (rawNum - 32.0) * 5.0 / 9.0
                return SpecialEvalResult(
                    numericValue = converted,
                    displayText = "${formatNumber(converted)} °C",
                    kind = LineKind.UNIT_OR_CURRENCY
                )
            }
        }

        if (looksLikeTimeExpression(trimmed)) {
            val totalMinutes = evaluateTimeMinutes(trimmed)
            if (totalMinutes != null) {
                val sign = if (totalMinutes < 0) "-" else ""
                val absMin = abs(totalMinutes).roundToLong()
                val hours = absMin / 60
                val mins = absMin % 60
                val formatted = buildString {
                    append(sign)
                    if (hours > 0) append("${hours}h")
                    if (hours > 0 && mins > 0) append(" ")
                    if (mins > 0 || hours == 0L) append("${mins}m")
                }
                return SpecialEvalResult(
                    numericValue = totalMinutes / 60.0,
                    displayText = formatted,
                    kind = LineKind.DATE_OR_TIME
                )
            }
        }

        val todayMatch = todayOffsetPattern.matchEntire(trimmed)
        if (todayMatch != null) {
            val op = todayMatch.groupValues[1]
            val amount = todayMatch.groupValues[2].toLongOrNull() ?: return null
            val signed = if (op == "-") -amount else amount
            val unit = todayMatch.groupValues[3].lowercase(Locale.US)
            val targetDate = when {
                unit.startsWith("d") -> referenceDate.plusDays(signed)
                unit.startsWith("w") -> referenceDate.plusWeeks(signed)
                unit.startsWith("m") -> referenceDate.plusMonths(signed)
                unit.startsWith("y") -> referenceDate.plusYears(signed)
                else -> referenceDate.plusDays(signed)
            }
            val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US)
            return SpecialEvalResult(
                numericValue = signed.toDouble(),
                displayText = targetDate.format(fmt),
                kind = LineKind.DATE_OR_TIME
            )
        }

        val untilMatch = daysUntilPattern.matchEntire(trimmed)
        if (untilMatch != null) {
            val day = untilMatch.groupValues[1].toIntOrNull() ?: return null
            val monthStr = untilMatch.groupValues[2].lowercase(Locale.US)
            val month = monthMap[monthStr] ?: return null
            val explicitYear = untilMatch.groupValues[3].toIntOrNull()
            return try {
                var target = LocalDate.of(explicitYear ?: referenceDate.year, month, day)
                if (explicitYear == null && target.isBefore(referenceDate)) {
                    target = target.plusYears(1)
                }
                val days = ChronoUnit.DAYS.between(referenceDate, target)
                SpecialEvalResult(
                    numericValue = days.toDouble(),
                    displayText = "$days days",
                    kind = LineKind.DATE_OR_TIME
                )
            } catch (_: Exception) {
                null
            }
        }

        return null
    }

    private fun looksLikeTimeExpression(text: String): Boolean {
        val hasHourOrMinToken = Regex("""\b\d+(?:\.\d+)?\s*(?:h|hr|hrs|hour|hours|m|min|mins|minute|minutes)\b""", RegexOption.IGNORE_CASE)
            .containsMatchIn(text)
        if (!hasHourOrMinToken) return false
        val stripped = text.replace(
            Regex("""(\d+(?:\.\d+)?)\s*(?:h|hr|hrs|hour|hours|m|min|mins|minute|minutes)|[+\-\s]""", RegexOption.IGNORE_CASE),
            ""
        )
        return stripped.isEmpty()
    }

    private fun evaluateTimeMinutes(text: String): Double? {
        var index = 0
        var totalMinutes = 0.0
        val matches = timeTermPattern.findAll(text).filter {
            it.groupValues[2].isNotEmpty() || it.groupValues[3].isNotEmpty()
        }.toList()
        if (matches.isEmpty()) return null
        for (m in matches) {
            val sign = if (m.groupValues[1] == "-") -1.0 else 1.0
            val hrs = m.groupValues[2].toDoubleOrNull() ?: 0.0
            val mins = m.groupValues[3].toDoubleOrNull() ?: 0.0
            totalMinutes += sign * (hrs * 60.0 + mins)
            index++
        }
        return if (index > 0) totalMinutes else null
    }

    private fun parseScaledNumber(raw: String): Double? {
        val cleaned = raw.trim().lowercase(Locale.US).replace(",", "")
        val multiplier = when {
            cleaned.endsWith("k") -> 1_000.0
            cleaned.endsWith("m") -> 1_000_000.0
            cleaned.endsWith("b") -> 1_000_000_000.0
            else -> 1.0
        }
        val numPart = cleaned.trimEnd('k', 'm', 'b').trim()
        return numPart.toDoubleOrNull()?.times(multiplier)
    }
}
