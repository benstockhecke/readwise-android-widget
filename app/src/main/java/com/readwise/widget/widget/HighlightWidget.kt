package com.readwise.widget.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import com.readwise.widget.ReadwiseApp
import com.readwise.widget.data.HighlightWithBook
import kotlinx.coroutines.flow.first

class HighlightWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as ReadwiseApp
        val repository = app.highlightRepository
        val settings = app.settingsDataStore

        // Read appearance settings
        val fontSize = settings.widgetFontSize.first()
        val backgroundColor = settings.widgetBackgroundColor.first()
        val textColor = settings.widgetTextColor.first()
        val sourceColor = settings.widgetSourceColor.first()
        val cornerRadius = settings.widgetCornerRadius.first()
        val borderWidth = settings.widgetBorderWidth.first()
        val borderColor = settings.widgetBorderColor.first()
        val padding = settings.widgetPadding.first()
        val fontFamily = settings.widgetFontFamily.first()

        // Read filter settings
        val bookId = settings.filterBookId.first()
        val tagName = settings.filterTagName.first()
        val maxLength = settings.maxHighlightLength.first()

        // Get a random highlight
        val highlight = repository.getRandomHighlight(bookId = bookId, tagName = tagName, maxLength = maxLength)

        provideContent {
            GlanceTheme {
                WidgetContent(
                    highlight = highlight,
                    fontSize = fontSize,
                    backgroundColor = backgroundColor,
                    textColor = textColor,
                    sourceColor = sourceColor,
                    cornerRadius = cornerRadius,
                    borderWidth = borderWidth,
                    borderColor = borderColor,
                    padding = padding,
                    fontFamily = fontFamily,
                )
            }
        }
    }
}

@Composable
private fun WidgetContent(
    highlight: HighlightWithBook?,
    fontSize: Float,
    backgroundColor: Long,
    textColor: Long,
    sourceColor: Long,
    cornerRadius: Float,
    borderWidth: Float,
    borderColor: Long,
    padding: Float,
    fontFamily: String,
) {
    val bgColor = ColorProvider(Color(backgroundColor.toInt()))
    val txtColor = ColorProvider(Color(textColor.toInt()))
    val srcColor = ColorProvider(Color(sourceColor.toInt()))

    val baseModifier = GlanceModifier
        .fillMaxSize()
        .cornerRadius(cornerRadius.toInt().dp)
        .background(bgColor)
        .padding(padding.toInt().dp)

    val widgetModifier = if (borderWidth > 0f) {
        // Glance does not have a direct border modifier; we rely on cornerRadius + background
        // The border effect can be approximated with a nested layout if needed,
        // but for simplicity we apply cornerRadius and background only.
        baseModifier
    } else {
        baseModifier
    }

    Column(
        modifier = widgetModifier.then(
            GlanceModifier.clickable(actionRunCallback<RefreshAction>())
        ),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
    ) {
        if (highlight != null) {
            val displayText = if (highlight.text.length > 500) {
                highlight.text.take(497) + "..."
            } else {
                highlight.text
            }

            Text(
                text = "\u201C$displayText\u201D",
                style = TextStyle(
                    color = txtColor,
                    fontSize = fontSize.sp,
                    fontWeight = FontWeight.Normal,
                    fontStyle = FontStyle.Italic,
                ),
                maxLines = 12,
            )

            Spacer(modifier = GlanceModifier.height(8.dp))

            val sourceLine = buildString {
                append(highlight.bookTitle)
                if (!highlight.bookAuthor.isNullOrBlank()) {
                    append(" \u2014 ")
                    append(highlight.bookAuthor)
                }
            }

            Text(
                text = sourceLine,
                style = TextStyle(
                    color = srcColor,
                    fontSize = (fontSize - 2f).coerceAtLeast(10f).sp,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 2,
            )
        } else {
            Text(
                text = "No highlights yet. Open app to sync.",
                style = TextStyle(
                    color = txtColor,
                    fontSize = fontSize.sp,
                ),
            )
        }
    }
}

class RefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        HighlightWidget().update(context, glanceId)
    }
}
