package com.calcnotes.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calcnotes.app.ui.theme.GlassCard
import com.calcnotes.app.ui.theme.GlassChip
import com.calcnotes.app.ui.theme.LocalGlassColors
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitBillSheet(
    initialTotal: Double,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onInsertSplitSummary: (String) -> Unit
) {
    val glass = LocalGlassColors.current
    var billInput by remember {
        mutableStateOf(if (initialTotal > 0) String.format(Locale.US, "%.2f", initialTotal) else "1000")
    }
    var peopleCount by remember { mutableIntStateOf(2) }
    var tipPercent by remember { mutableFloatStateOf(10f) }

    val baseAmount = billInput.toDoubleOrNull() ?: 0.0
    val tipAmount = baseAmount * (tipPercent / 100.0)
    val totalWithTip = baseAmount + tipAmount
    val perPerson = if (peopleCount > 0) totalWithTip / peopleCount else totalWithTip
    val prefix = currencySymbol.ifBlank { "" }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = glass.surfaceFallback,
        contentColor = glass.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Split Bill & Tip Calculator",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = glass.textPrimary
            )

            OutlinedTextField(
                value = billInput,
                onValueChange = { billInput = it },
                label = { Text("Bill Total") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("People: $peopleCount", fontWeight = FontWeight.SemiBold, color = glass.textPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassChip(onClick = { if (peopleCount > 1) peopleCount-- }) {
                        Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = glass.textPrimary)
                    }
                    GlassChip(onClick = { if (peopleCount < 50) peopleCount++ }) {
                        Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = glass.textPrimary)
                    }
                }
            }

            Column {
                Text(
                    text = "Tip: ${tipPercent.toInt()}% ($prefix${String.format(Locale.US, "%.2f", tipAmount)})",
                    color = glass.textPrimary,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = tipPercent,
                    onValueChange = { tipPercent = it },
                    valueRange = 0f..30f,
                    steps = 5
                )
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total with Tip", color = glass.textDimmed)
                        Text(
                            text = "$prefix${String.format(Locale.US, "%.2f", totalWithTip)}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = glass.textPrimary
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Each Person Pays", color = glass.textDimmed)
                        Text(
                            text = "$prefix${String.format(Locale.US, "%.2f", perPerson)}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = glass.accent
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val snippet = "\n# Split Bill ($peopleCount people, ${tipPercent.toInt()}% tip)\n" +
                        "Bill $baseAmount + ${tipPercent.toInt()}%\n" +
                        "Each share: ${String.format(Locale.US, "%.2f", perPerson)}"
                    onInsertSplitSummary(snippet)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = glass.accent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Insert Split into Note", color = glass.surfaceFallback, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
