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

/**
 * Glance [GlanceAppWidget] that displays a single random highlight on the home screen.
 *
 * Each time [provideGlance] is called (on placement, update, or tap) the widget:
 * 1. Reads all appearance and filter settings from [SettingsDataStore].
 * 2. Selects a random highlight from [HighlightRepository] using the current filters.
 * 3. Renders the result via the stateless [WidgetContent] composable.
 *
 * Tapping the widget triggers [RefreshAction], which calls [update] to pick a new
 * random highlight.
 */
class HighlightWidget : GlanceAppWidget() {

    /**
     * Called by Glance to supply the widget's UI content.
     *
     * Suspends while reading settings and querying the database, then hands off
     * to [provideContent] to render the composable tree.
     */
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

        // Get a random highlight based on the active filters
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

/**
 * Stateless composable that renders a highlight inside the widget frame.
 *
 * Highlights longer than 500 characters are truncated with an ellipsis to avoid
 * overflowing the widget's fixed dimensions. When no highlight is available a
 * prompt message guides the user to open the app and sync.
 *
 * @param highlight The highlight to display, or `null` if the database is empty.
 * @param fontSize Text size in SP for the main highlight body.
 * @param backgroundColor Widget background color as a packed ARGB long.
 * @param textColor Highlight body text color as a packed ARGB long.
 * @param sourceColor Book/author attribution text color as a packed ARGB long.
 * @param cornerRadius Corner radius of the widget background in DP.
 * @param borderWidth Border stroke width in DP (currently approximated; see inline note).
 * @param borderColor Border stroke color as a packed ARGB long.
 * @param padding Inner content padding in DP.
 * @param fontFamily Font family identifier for the widget text.
 */
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
    // Convert packed ARGB longs to Glance ColorProviders
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

    // The entire widget is tappable; a tap triggers RefreshAction to show a new highlight
    Column(
        modifier = widgetModifier.then(
            GlanceModifier.clickable(actionRunCallback<RefreshAction>())
        ),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
    ) {
        if (highlight != null) {
            // Truncate very long highlights to prevent layout overflow
            val displayText = if (highlight.text.length > 500) {
                highlight.text.take(497) + "..."
            } else {
                highlight.text
            }

            // Render the highlight wrapped in typographic quotation marks
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

            // Build the attribution line: "Book Title — Author" (author omitted if blank)
            val sourceLine = buildString {
                append(highlight.bookTitle)
                if (!highlight.bookAuthor.isNullOrBlank()) {
                    append(" \u2014 ")  // em dash separator
                    append(highlight.bookAuthor)
                }
            }

            // Render the attribution at a slightly smaller size than the body text
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
            // Fallback message shown when no highlights have been synced yet
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

/**
 * Glance [ActionCallback] triggered when the user taps the widget.
 *
 * Forces the widget to re-run [HighlightWidget.provideGlance], which picks a new
 * random highlight and redraws the UI.
 */
class RefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        HighlightWidget().update(context, glanceId)
    }
}
