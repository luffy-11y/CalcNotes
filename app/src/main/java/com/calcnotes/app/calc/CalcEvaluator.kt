package com.calcnotes.app.calc

import java.time.LocalDate
import java.util.Locale
import kotlin.math.PI
import kotlin.math.E
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

class CalcEvaluator(
    private val referenceDate: LocalDate = LocalDate.now()
) {

    private data class CacheKey(
        val rawLine: String,
        val prevValue: Double?,
        val sectionValuesHash: Int,
        val variablesSnapshotHash: Int,
        val config: CalcFormatConfig
    )

    private val lineCache = LinkedHashMap<CacheKey, LineResult>(2048, 0.75f, true)

    fun evaluateNote(
        text: String,
        config: CalcFormatConfig = CalcFormatConfig()
    ): NoteEvaluationSummary {
        val lines = text.split('\n')
        val mutableResults = MutableList<LineResult?>(lines.size) { null }
        val variables = mutableMapOf<String, Double>()

        var previousLineNumeric: Double? = null
        val currentBlockValues = mutableListOf<Double>()
        var currentHeadingIndex: Int? = null
        var currentHeadingSubtotal = 0.0

        for (i in lines.indices) {
            val rawLine = lines[i]
            val trimmed = rawLine.trim()

            if (trimmed.isEmpty()) {
                currentBlockValues.clear()
                mutableResults[i] = LineResult(
                    lineIndex = i,
                    rawLine = rawLine,
                    kind = LineKind.EMPTY,
                    numericValue = null,
                    displayText = ""
                )
                continue
            }

            if (trimmed.startsWith("#")) {
                if (currentHeadingIndex != null) {
                    val prevIdx = currentHeadingIndex
                    mutableResults[prevIdx] = LineResult(
                        lineIndex = prevIdx,
                        rawLine = lines[prevIdx],
                        kind = LineKind.HEADING,
                        numericValue = currentHeadingSubtotal,
                        displayText = formatValue(currentHeadingSubtotal, config)
                    )
                }
                currentHeadingIndex = i
                currentHeadingSubtotal = 0.0
                currentBlockValues.clear()
                mutableResults[i] = LineResult(
                    lineIndex = i,
                    rawLine = rawLine,
                    kind = LineKind.HEADING,
                    numericValue = 0.0,
                    displayText = formatValue(0.0, config)
                )
                continue
            }

            val key = CacheKey(
                rawLine = rawLine,
                prevValue = previousLineNumeric,
                sectionValuesHash = currentBlockValues.hashCode(),
                variablesSnapshotHash = variables.hashCode(),
                config = config
            )

            val result = lineCache[key]?.copy(lineIndex = i) ?: evaluateSingleLine(
                lineIndex = i,
                rawLine = rawLine,
                prevValue = previousLineNumeric,
                blockValues = currentBlockValues,
                variables = variables,
                config = config
            ).also { computed ->
                if (lineCache.size > 4000) {
                    val firstKey = lineCache.keys.firstOrNull()
                    if (firstKey != null) lineCache.remove(firstKey)
                }
                lineCache[key] = computed
            }

            if (result.assignedVariable != null && result.numericValue != null) {
                variables[result.assignedVariable.lowercase(Locale.US)] = result.numericValue
            }

            if (result.numericValue != null && result.kind != LineKind.HEADING) {
                previousLineNumeric = result.numericValue
                if (result.kind != LineKind.SUMMARY_KEYWORD) {
                    currentBlockValues.add(result.numericValue)
                    currentHeadingSubtotal += result.numericValue
                }
            }

            mutableResults[i] = result
        }

        if (currentHeadingIndex != null) {
            val idx = currentHeadingIndex
            mutableResults[idx] = LineResult(
                lineIndex = idx,
                rawLine = lines[idx],
                kind = LineKind.HEADING,
                numericValue = currentHeadingSubtotal,
                displayText = formatValue(currentHeadingSubtotal, config)
            )
        }

        val finalResults = mutableResults.mapIndexed { idx, item ->
            item ?: LineResult(idx, lines[idx], LineKind.EMPTY)
        }

        val contributingValues = finalResults.mapNotNull { res ->
            if (res.kind == LineKind.HEADING || res.kind == LineKind.SUMMARY_KEYWORD || res.kind == LineKind.DATE_OR_TIME) {
                null
            } else {
                res.numericValue
            }
        }

        val grandTotal = contributingValues.sum()
        val count = contributingValues.size
        val avg = if (count > 0) grandTotal / count else 0.0
        val minVal = contributingValues.minOrNull() ?: 0.0
        val maxVal = contributingValues.maxOrNull() ?: 0.0

        return NoteEvaluationSummary(
            lineResults = finalResults,
            grandTotal = grandTotal,
            formattedGrandTotal = formatValue(grandTotal, config),
            activeLineCount = count,
            average = avg,
            min = minVal,
            max = maxVal,
            variables = variables.toMap(),
            hasCurrencyEstimate = finalResults.any { it.isCurrencyEstimate }
        )
    }

    private fun evaluateSingleLine(
        lineIndex: Int,
        rawLine: String,
        prevValue: Double?,
        blockValues: List<Double>,
        variables: Map<String, Double>,
        config: CalcFormatConfig
    ): LineResult {
        return try {
            val cleanedPrefix = rawLine
                .replaceFirst(Regex("""^\s*[-*]\s*\[[ xX]\]\s*"""), "")
                .replaceFirst(Regex("""^\s*[-*•]\s+"""), "")
                .trim()

            if (cleanedPrefix.isEmpty()) {
                return LineResult(lineIndex, rawLine, LineKind.EMPTY)
            }

            val assignMatch = Regex("""^\s*([a-zA-Z_][a-zA-Z0-9_]*)\s*=\s*(.+)$""").matchEntire(cleanedPrefix)
            if (assignMatch != null) {
                val varName = assignMatch.groupValues[1]
                val rhs = assignMatch.groupValues[2]
                val rhsEval = evaluateSingleLine(
                    lineIndex = lineIndex,
                    rawLine = rhs,
                    prevValue = prevValue,
                    blockValues = blockValues,
                    variables = variables,
                    config = config
                )
                if (rhsEval.numericValue != null) {
                    return LineResult(
                        lineIndex = lineIndex,
                        rawLine = rawLine,
                        kind = LineKind.VARIABLE_ASSIGNMENT,
                        numericValue = rhsEval.numericValue,
                        displayText = formatValue(rhsEval.numericValue, config),
                        assignedVariable = varName,
                        referencedVariables = rhsEval.referencedVariables,
                        isCurrencyEstimate = rhsEval.isCurrencyEstimate
                    )
                }
            }

            val lower = cleanedPrefix.lowercase(Locale.US)
            if (Regex("""^(?:sum|total|subtotal)(?:\s*:.*)?$""").matches(lower)) {
                val s = blockValues.sum()
                return LineResult(
                    lineIndex = lineIndex,
                    rawLine = rawLine,
                    kind = LineKind.SUMMARY_KEYWORD,
                    numericValue = s,
                    displayText = formatValue(s, config)
                )
            }
            if (Regex("""^(?:avg|average|mean)(?:\s*:.*)?$""").matches(lower)) {
                val a = if (blockValues.isNotEmpty()) blockValues.average() else 0.0
                return LineResult(
                    lineIndex = lineIndex,
                    rawLine = rawLine,
                    kind = LineKind.SUMMARY_KEYWORD,
                    numericValue = a,
                    displayText = formatValue(a, config)
                )
            }

            val special = UnitAndDateConverter.tryEvaluateSpecial(
                line = cleanedPrefix,
                formatNumber = { num -> formatRawNumber(num, config) },
                referenceDate = referenceDate
            )
            if (special != null) {
                return LineResult(
                    lineIndex = lineIndex,
                    rawLine = rawLine,
                    kind = special.kind,
                    numericValue = special.numericValue,
                    displayText = special.displayText,
                    isCurrencyEstimate = special.isCurrencyEstimate
                )
            }

            val referencedVars = mutableListOf<String>()
            val preprocessed = preprocessNumbersAndPercentages(
                text = cleanedPrefix,
                prevValue = prevValue,
                variables = variables,
                referencedVars = referencedVars
            )

            val tokens = tokenizeMath(preprocessed)
            if (tokens.isEmpty()) {
                return LineResult(lineIndex, rawLine, LineKind.TEXT_ONLY)
            }

            val numberTokens = tokens.filterIsInstance<MathToken.NumberLiteral>()
            val hasOperatorsOrFunctions = tokens.any {
                it is MathToken.Operator || it is MathToken.FunctionName
            }

            if (numberTokens.isEmpty()) {
                return LineResult(lineIndex, rawLine, LineKind.TEXT_ONLY)
            }

            if (!hasOperatorsOrFunctions && numberTokens.size > 1) {
                val nums = numberTokens.map { it.value }
                return when (config.multiNumberMode) {
                    MultiNumberMode.SUM -> {
                        val total = nums.sum()
                        LineResult(
                            lineIndex = lineIndex,
                            rawLine = rawLine,
                            kind = LineKind.CALCULATION,
                            numericValue = total,
                            displayText = formatValue(total, config),
                            referencedVariables = referencedVars
                        )
                    }
                    MultiNumberMode.SHOW_EACH -> {
                        val total = nums.sum()
                        val joined = nums.joinToString(" | ") { formatValue(it, config) }
                        LineResult(
                            lineIndex = lineIndex,
                            rawLine = rawLine,
                            kind = LineKind.CALCULATION,
                            numericValue = total,
                            displayText = joined,
                            referencedVariables = referencedVars
                        )
                    }
                    MultiNumberMode.FIRST_ONLY -> {
                        val first = nums.first()
                        LineResult(
                            lineIndex = lineIndex,
                            rawLine = rawLine,
                            kind = LineKind.CALCULATION,
                            numericValue = first,
                            displayText = formatValue(first, config),
                            referencedVariables = referencedVars
                        )
                    }
                }
            }

            if (!hasOperatorsOrFunctions && numberTokens.size == 1) {
                val single = numberTokens.first().value
                return LineResult(
                    lineIndex = lineIndex,
                    rawLine = rawLine,
                    kind = LineKind.CALCULATION,
                    numericValue = single,
                    displayText = formatValue(single, config),
                    referencedVariables = referencedVars
                )
            }

            val value = evaluateTokens(tokens)
            if (value == null || value.isNaN() || value.isInfinite()) {
                LineResult(
                    lineIndex = lineIndex,
                    rawLine = rawLine,
                    kind = LineKind.INVALID,
                    numericValue = null,
                    displayText = if (config.showDashOnInvalid) "–" else ""
                )
            } else {
                LineResult(
                    lineIndex = lineIndex,
                    rawLine = rawLine,
                    kind = LineKind.CALCULATION,
                    numericValue = value,
                    displayText = formatValue(value, config),
                    referencedVariables = referencedVars
                )
            }
        } catch (_: Exception) {
            LineResult(
                lineIndex = lineIndex,
                rawLine = rawLine,
                kind = LineKind.INVALID,
                numericValue = null,
                displayText = if (config.showDashOnInvalid) "–" else ""
            )
        }
    }

    private fun preprocessNumbersAndPercentages(
        text: String,
        prevValue: Double?,
        variables: Map<String, Double>,
        referencedVars: MutableList<String>
    ): String {
        var s = text
        s = s.replace('×', '*').replace('÷', '/')
        s = s.replace(
            Regex("""(?<=[\d)\w%])\s+[xX]\s+(?=[\d(\w+-])"""),
            " * "
        )
        s = s.replace(Regex("""(\d),(\d{3})(?!\d)"""), "$1$2")
        s = s.replace(Regex("""(\d),(\d{3})(?!\d)"""), "$1$2")
        s = s.replace(Regex("""\b(\d{1,3})\s(000)\b"""), "$1$2")

        s = s.replace(Regex("""\b(\d+(?:\.\d+)?)([kmbKMB])\b""")) { match ->
            val base = match.groupValues[1].toDoubleOrNull() ?: return@replace match.value
            val mult = when (match.groupValues[2].lowercase(Locale.US)) {
                "k" -> 1_000.0
                "m" -> 1_000_000.0
                "b" -> 1_000_000_000.0
                else -> 1.0
            }
            val expanded = base * mult
            if (expanded % 1.0 == 0.0) expanded.toLong().toString() else expanded.toString()
        }

        if (prevValue != null) {
            s = s.replace(Regex("""\b(?:prev|above)\b""", RegexOption.IGNORE_CASE)) {
                prevValue.toString()
            }
        }

        if (variables.isNotEmpty()) {
            val sortedVars = variables.entries.sortedByDescending { it.key.length }
            for ((varKey, varVal) in sortedVars) {
                val pattern = Regex("""\b${Regex.escape(varKey)}\b""", RegexOption.IGNORE_CASE)
                if (pattern.containsMatchIn(s)) {
                    referencedVars.add(varKey)
                    s = s.replace(pattern, varVal.toString())
                }
            }
        }

        s = s.replace(
            Regex("""(\d+(?:\.\d+)?)\s*%\s*of\s*(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE),
            "(($1 / 100.0) * $2)"
        )
        s = s.replace(
            Regex("""(\d+(?:\.\d+)?)\s*([+-])\s*(\d+(?:\.\d+)?)\s*%"""),
            "($1 $2 ($1 * $3 / 100.0))"
        )
        s = s.replace(
            Regex("""(\d+(?:\.\d+)?)\s*([*/])\s*(\d+(?:\.\d+)?)\s*%"""),
            "($1 $2 ($3 / 100.0))"
        )

        return s
    }

    private sealed class MathToken {
        data class NumberLiteral(val value: Double) : MathToken()
        data class Operator(val symbol: Char) : MathToken()
        data class FunctionName(val name: String) : MathToken()
        data object LeftParen : MathToken()
        data object RightParen : MathToken()
        data object Comma : MathToken()
    }

    private val supportedFunctions = setOf(
        "sqrt", "abs", "round", "floor", "ceil",
        "sin", "cos", "tan", "log", "ln", "min", "max"
    )

    private fun tokenizeMath(input: String): List<MathToken> {
        val rawTokens = mutableListOf<MathToken>()
        var i = 0
        val n = input.length

        while (i < n) {
            val c = input[i]
            when {
                c.isWhitespace() -> i++
                c.isDigit() || (c == '.' && i + 1 < n && input[i + 1].isDigit()) -> {
                    val start = i
                    var hasDot = (c == '.')
                    i++
                    while (i < n && (input[i].isDigit() || (!hasDot && input[i] == '.'))) {
                        if (input[i] == '.') hasDot = true
                        i++
                    }
                    val num = input.substring(start, i).toDoubleOrNull()
                    if (num != null) {
                        rawTokens.add(MathToken.NumberLiteral(num))
                    }
                }
                c.isLetter() || c == '_' -> {
                    val start = i
                    while (i < n && (input[i].isLetterOrDigit() || input[i] == '_')) {
                        i++
                    }
                    val word = input.substring(start, i).lowercase(Locale.US)
                    when {
                        word in supportedFunctions -> rawTokens.add(MathToken.FunctionName(word))
                        word == "pi" -> rawTokens.add(MathToken.NumberLiteral(PI))
                        word == "e" -> {
                            val prevIsOp = rawTokens.lastOrNull() is MathToken.Operator || rawTokens.lastOrNull() is MathToken.LeftParen
                            val nextNonSpace = input.substring(i).trimStart().firstOrNull()
                            val nextIsOp = nextNonSpace != null && nextNonSpace in listOf('+', '-', '*', '/', '^', ')')
                            if (prevIsOp || nextIsOp) {
                                rawTokens.add(MathToken.NumberLiteral(E))
                            }
                        }
                        else -> {}
                    }
                }
                c == '(' -> {
                    rawTokens.add(MathToken.LeftParen)
                    i++
                }
                c == ')' -> {
                    rawTokens.add(MathToken.RightParen)
                    i++
                }
                c == ',' -> {
                    rawTokens.add(MathToken.Comma)
                    i++
                }
                c in listOf('+', '-', '*', '/', '^', '%') -> {
                    rawTokens.add(MathToken.Operator(c))
                    i++
                }
                else -> i++
            }
        }

        return sanitizeTokenStream(rawTokens)
    }

    private fun sanitizeTokenStream(tokens: List<MathToken>): List<MathToken> {
        if (tokens.isEmpty()) return emptyList()
        val result = tokens.toMutableList()
        while (result.isNotEmpty() && result.last() is MathToken.Operator) {
            result.removeAt(result.lastIndex)
        }
        while (result.isNotEmpty()) {
            val first = result.first()
            if (first is MathToken.Operator && first.symbol in listOf('*', '/', '^', '%')) {
                result.removeAt(0)
            } else {
                break
            }
        }
        return result
    }

    private fun evaluateTokens(tokens: List<MathToken>): Double? {
        var pos = 0

        fun peek(): MathToken? = tokens.getOrNull(pos)
        fun consume(): MathToken? = tokens.getOrNull(pos++)

        fun parseExpression(): Double? {
            var left = parseTerm() ?: return null
            while (true) {
                val next = peek()
                if (next is MathToken.Operator && (next.symbol == '+' || next.symbol == '-')) {
                    consum
