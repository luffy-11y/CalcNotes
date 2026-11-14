package com.calcnotes.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.calcnotes.app.MainActivity
import com.calcnotes.app.data.CalcNotesDatabase

class CalcNotesWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = CalcNotesDatabase.getInstance(context)
        val notes = db.noteDao().getAllNotesOnce().filter { !it.isTrashed && !it.isLocked }
        val topNote = notes.firstOrNull { it.isPinned } ?: notes.firstOrNull()

        provideContent {
            GlanceTheme {
                WidgetBody(
                    noteTitle = topNote?.title?.ifBlank { "Untitled Note" } ?: "CalcNotes",
                    noteTotal = topNote?.cachedTotal ?: "0"
                )
            }
        }
    }

    @Composable
    private fun WidgetBody(noteTitle: String, noteTotal: String) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFF0F172A)))
                .padding(14.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📌 $noteTitle",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF94A3B8)),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            Spacer(modifier = GlanceModifier.height(6.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "= $noteTotal",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF38BDF8)),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(modifier = GlanceModifier.height(6.dp))
            Text(
                text = "+ Tap to open or add calculation note",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF64748B)),
                    fontSize = 11.sp
                )
            )
        }
    }
}

class CalcNotesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CalcNotesWidget()
}

object CalcNotesWidgetUpdater {
    suspend fun updateWidget(context: Context) {
        runCatching {
            CalcNotesWidget().updateAll(context)
        }
    }
}
