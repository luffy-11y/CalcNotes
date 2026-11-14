package com.calcnotes.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calcnotes.app.calc.MultiNumberMode
import com.calcnotes.app.calc.ThousandsSeparatorStyle
import com.calcnotes.app.data.AppThemeMode
import com.calcnotes.app.data.SettingsPreferences
import com.calcnotes.app.data.UserSettings
import com.calcnotes.app.ui.theme.AccentPalettes
import com.calcnotes.app.ui.theme.GlassCard
import com.calcnotes.app.ui.theme.GlassChip
import com.calcnotes.app.ui.theme.LocalGlassColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settings: UserSettings,
    prefs: SettingsPreferences,
    scope: CoroutineScope,
    onExportBackupJson: () -> Unit,
    onImportBackupJson: () -> Unit,
    onEmptyTrash: () -> Unit,
    onBack: () -> Unit
) {
    val glass = LocalGlassColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = glass.textPrimary)
            }
            Text(
                text = "Settings & Customization",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = glass.textPrimary
            )
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Theme & Frosted Glass UI", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = glass.accent)

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppThemeMode.entries.forEach { mode ->
                        GlassChip(
                            selected = settings.themeMode == mode,
                            onClick = { scope.launch { prefs.updateThemeMode(mode) } }
                        ) {
                            Text(mode.name, color = glass.textPrimary, fontSize = 13.sp)
                        }
                    }
                }

                Text("Accent Palette", fontSize = 13.sp, color = glass.textDimmed)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AccentPalettes.forEachIndexed { index, palette ->
                        Box(
                            modifier = Modifier
                                .size(if (settings.accentColorIndex == index) 34.dp else 28.dp)
                                .clip(CircleShape)
                                .background(palette.primary)
                                .border(
                                    width = if (settings.accentColorIndex == index) 2.5.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .clickable { scope.launch { prefs.updateAccentColorIndex(index) } }
                        )
                    }
                }

                Column {
                    Text(
                        text = "Background Blur Intensity: ${settings.blurIntensityDp.toInt()} dp",
                        fontSize = 13.sp,
                        color = glass.textPrimary
                    )
                    Slider(
                        value = settings.blurIntensityDp,
                        onValueChange = { scope.launch { prefs.updateBlurIntensity(it) } },
                        valueRange = 0f..40f
                    )
                }

                SettingToggleRow(
                    title = "Animated Mesh Background",
                    subtitle = "Automatically pauses in background to save battery",
                    checked = settings.animatedBackground,
                    onCheckedChange = { scope.launch { prefs.updateAnimatedBackground(it) } }
                )
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Calculation Engine & Formatting", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = glass.accent)

                Text(
                    text = "Multiple Numbers on One Line (with no operators)",
                    fontSize = 13.sp,
                    color = glass.textDimmed
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val modes = listOf(
                        MultiNumberMode.SUM to "Sum All (12000 + 4500 = 16500)",
                        MultiNumberMode.SHOW_EACH to "Show Each Number",
                        MultiNumberMode.FIRST_ONLY to "Use First Only"
                    )
                    modes.forEach { (mode, label) ->
                        GlassChip(
                            selected = settings.multiNumberMode == mode,
                            onClick = { scope.launch { prefs.updateMultiNumberMode(mode) } }
                        ) {
                            Text(label, fontSize = 12.sp, color = glass.textPrimary)
                        }
                    }
                }

                Text("Decimal Places: ${settings.decimalPlaces}", fontSize = 13.sp, color = glass.textPrimary)
                Slider(
                    value = settings.decimalPlaces.toFloat(),
                    onValueChange = { scope.launch { prefs.updateDecimalPlaces(it.toInt()) } },
                    valueRange = 0f..6f,
                    steps = 5
                )

                Text("Thousands Separator", fontSize = 13.sp, color = glass.textDimmed)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThousandsSeparatorStyle.entries.forEach { style ->
                        GlassChip(
                            selected = settings.separatorStyle == style,
                            onClick = { scope.launch { prefs.updateSeparatorStyle(style) } }
                        ) {
                            Text(style.name, fontSize = 12.sp, color = glass.textPrimary)
                        }
                    }
                }

                OutlinedTextField(
                    value = settings.currencySymbol,
                    onValueChange = { scope.launch { prefs.updateCurrencySymbol(it.take(4)) } },
                    label = { Text("Optional Currency Prefix (e.g. ₹, $, €)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Editor, Haptics & Biometric Security", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = glass.accent)

                Text("Editor Font Size: ${settings.fontSizeSp.toInt()} sp", fontSize = 13.sp, color = glass.textPrimary)
                Slider(
                    value = settings.fontSizeSp,
                    onValueChange = { scope.launch { prefs.updateFontSize(it) } },
                    valueRange = 13f..22f
                )

                Text(
                    text = "Line Spacing: ${String.format("%.2f", settings.lineSpacingMultiplier)}x",
                    fontSize = 13.sp,
                    color = glass.textPrimary
                )
                Slider(
                    value = settings.lineSpacingMultiplier,
                    onValueChange = { scope.launch { prefs.updateLineSpacing(it) } },
                    valueRange = 1.15f..1.75f
                )

                SettingToggleRow(
                    title = "Show Line Numbers",
                    subtitle = "Display line numbers and interactive checklist toggles",
                    checked = settings.showLineNumbers,
                    onCheckedChange = { scope.launch { prefs.updateShowLineNumbers(it) } }
                )

                SettingToggleRow(
                    title = "Subtle Haptic Feedback",
                    subtitle = "Vibrate softly when results update or copy",
                    checked = settings.hapticsEnabled,
                    onCheckedChange = { scope.launch { prefs.updateHaptics(it) } }
                )

                SettingToggleRow(
                    title = "Lock Whole App with Biometrics / PIN",
                    subtitle = "Require fingerprint, face, or device PIN on launch",
                    checked = settings.appLockEnabled,
                    onCheckedChange = { scope.launch { prefs.updateAppLock(it) } }
                )
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Backup, Restore & Trash", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = glass.accent)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onExportBackupJson,
                        colors = ButtonDefaults.buttonColors(containerColor = glass.accent),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Backup to JSON", color = Color(0xFF090D16), fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onImportBackupJson,
                        colors = ButtonDefaults.buttonColors(containerColor = glass.accentSecondary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Restore JSON", color = Color(0xFF090D16), fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onEmptyTrash,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFB7185).copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Empty Trash Now (Auto-deletes after 30 days)", color = Color(0xFFFB7185))
                }
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("About CalcNotes v1.0.0", fontWeight = FontWeight.Bold, color = glass.textPrimary)
                Text(
                    text = "Built with Kotlin, Jetpack Compose (Material 3), Room, DataStore, and Glance. 100% offline with zero trackers or internet permissions.",
                    fontSize = 13.sp,
                    color = glass.textDimmed
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val glass = LocalGlassColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, color = glass.textPrimary, fontSize = 14.sp)
            Text(subtitle, color = glass.textDimmed, fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
