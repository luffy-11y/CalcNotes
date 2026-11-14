package com.calcnotes.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calcnotes.app.calc.LineKind
import com.calcnotes.app.calc.NoteEvaluationSummary
import com.calcnotes.app.ui.theme.GlassCard
import com.calcnotes.app.ui.theme.LocalGlassColors
import com.calcnotes.app.ui.theme.NoteTagColors
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartBreakdownSheet(
    summary: NoteEvaluationSummary,
    currencySymbol: String,
    onDismiss: () -> Unit
) {
    val glass = LocalGlassColors.current
    val contributingLines = summary.lineResults.filter {
        it.numericValue != null &&
            it.numericValue != 0.0 &&
            it.kind != LineKind.HEADING &&
            it.kind != LineKind.SUMMARY_KEYWORD &&
            it.kind != LineKind.DATE_OR_TIME
    }
    val absSum = contributingLines.sumOf { abs(it.numericValue ?: 0.0) }.coerceAtLeast(0.0001)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = glass.surfaceFallback,
        contentColor = glass.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Note Stats & Line Breakdown",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = glass.textPrimary
            )

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatColumn("Count", "${summary.activeLineCount}", glass.accent)
                    StatColumn("Average", "$currencySymbol${String.format(Locale.US, "%.2f", summary.average)}", glass.textPrimary)
                    StatColumn("Min", "$currencySymbol${String.format(Locale.US, "%.2f", summary.min)}", glass.textPrimary)
                    StatColumn("Max", "$currencySymbol${String.format(Locale.US, "%.2f", summary.max)}", glass.textPrimary)
                }
            }

            if (contributingLines.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Canvas(modifier = Modifier.size(130.dp)) {
                        var startAngle = -90f
                        contributingLines.forEachIndexed { idx, line ->
                            val proportion = (abs(line.numericValue ?: 0.0) / absSum).toFloat()
                            val sweep = proportion * 360f
                            val sliceColor = NoteTagColors[idx % NoteTagColors.size]
                            drawArc(
                                color = sliceColor,
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = true
                            )
                            startAngle += sweep
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(contributingLines) { idx, item ->
                        val share = (abs(item.numericValue ?: 0.0) / absSum * 100.0)
                        val barColor = NoteTagColors[idx % NoteTagColors.size]
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(barColor)
                                    )
                                    Text(
                                        text = "  L${item.lineIndex + 1}: ${item.rawLine.trim()}",
                                        color = glass.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 14.sp
                                    )
                                }
                                Text(
                                    text = "${item.displayText} (${String.format(Locale.US, "%.1f", share)}%)",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = barColor
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth((share / 100.0).toFloat().coerceIn(0.04f, 1f))
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(barColor)
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Type numbers on any line to see their visual breakdown.",
                    color = glass.textDimmed
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String, valueColor: Color) {
    val glass = LocalGlassColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 12.sp, color = glass.textDimmed)
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = valueColor
        )
    }
}
