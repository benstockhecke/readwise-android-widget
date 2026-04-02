package com.readwise.widget.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * [GlanceAppWidgetReceiver] entry point for the highlight widget.
 *
 * Android uses this receiver (declared in `AndroidManifest.xml`) to route
 * widget lifecycle broadcasts — such as `APPWIDGET_UPDATE` and
 * `APPWIDGET_DELETED` — to the correct [GlanceAppWidget] implementation.
 *
 * [glanceAppWidget] returns a new [HighlightWidget] instance, which Glance
 * uses to provide and update the widget's UI.
 */
class HighlightWidgetReceiver : GlanceAppWidgetReceiver() {

    /** The [GlanceAppWidget] instance managed by this receiver. */
    override val glanceAppWidget: GlanceAppWidget
        get() = HighlightWidget()
}
