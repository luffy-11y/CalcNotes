package com.calcnotes.app.calc

enum class MultiNumberMode {
    SUM,        // "rent 12000 and food 4500" -> 16500
    SHOW_EACH,  // "rent 12000 and food 4500" -> "12,000 | 4,500" (numeric value 16500 for totals)
    FIRST_ONLY  // "rent 12000 and food 4500" -> 12000
}

enum class ThousandsSeparatorStyle(val char: Char?) {
    COMMA(','),
    SPACE(' '),
    PERIOD('.'),
    NONE(null)
}

data class CalcFormatConfig(
    val decimalPlaces: Int = 2,
    val separatorStyle: ThousandsSeparatorStyle = ThousandsSeparatorStyle.COMMA,
    val currencySymbol: String = "",
    val multiNumberMode: MultiNumberMode = MultiNumberMode.SUM,
    val showDashOnInvalid: Boolean = false
)

enum class LineKind {
    EMPTY,
    TEXT_ONLY,
    HEADING,
    CALCULATION,
    VARIABLE_ASSIGNMENT,
    SUMMARY_KEYWORD,
    UNIT_OR_CURRENCY,
    DATE_OR_TIME,
    INVALID
}

data class LineResult(
    val lineIndex: Int,
    val rawLine: String,
    val kind: LineKind,
    val numericValue: Double? = null,
    val displayText: String = "",
    val assignedVariable: String? = null,
    val referencedVariables: List<String> = emptyList(),
    val isCurrencyEstimate: Boolean = false
)

data class NoteEvaluationSummary(
    val lineResults: List<LineResult>,
    val grandTotal: Double,
    val formattedGrandTotal: String,
    val activeLineCount: Int,
    val average: Double,
    val min: Double,
    val max: Double,
    val variables: Map<String, Double>,
    val hasCurrencyEstimate: Boolean
)
