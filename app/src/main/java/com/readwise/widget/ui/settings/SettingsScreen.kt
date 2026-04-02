package com.readwise.widget.ui.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt

// ── Helpers ────────────────────────────────────────────────────────────

private fun Long.toComposeColor(): Color = Color(this)

private fun Color.toLongArgb(): Long {
    val a = (alpha * 255).roundToInt()
    val r = (red * 255).roundToInt()
    val g = (green * 255).roundToInt()
    val b = (blue * 255).roundToInt()
    return (a.toLong() shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
}

private fun String.toFontFamily(): FontFamily = when (this) {
    "serif" -> FontFamily.Serif
    "sans-serif" -> FontFamily.SansSerif
    "monospace" -> FontFamily.Monospace
    "cursive" -> FontFamily.Cursive
    else -> FontFamily.Default
}

private val fontFamilyOptions = listOf(
    "default" to "Default",
    "serif" to "Serif",
    "sans-serif" to "Sans-Serif",
    "monospace" to "Monospace",
    "cursive" to "Cursive",
)

private data class RefreshOption(val minutes: Int, val label: String)

private val refreshOptions = listOf(
    RefreshOption(15, "15 minutes"),
    RefreshOption(30, "30 minutes"),
    RefreshOption(60, "1 hour"),
    RefreshOption(120, "2 hours"),
    RefreshOption(240, "4 hours"),
    RefreshOption(360, "6 hours"),
    RefreshOption(720, "12 hours"),
    RefreshOption(1440, "24 hours"),
)

// ── Main Screen ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToHighlights: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // Collect all state
    val apiToken by viewModel.apiToken.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val highlightCount by viewModel.highlightCount.collectAsStateWithLifecycle()
    val books by viewModel.books.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val filterBookId by viewModel.filterBookId.collectAsStateWithLifecycle()
    val filterTagName by viewModel.filterTagName.collectAsStateWithLifecycle()
    val fontSize by viewModel.fontSize.collectAsStateWithLifecycle()
    val fontFamily by viewModel.fontFamily.collectAsStateWithLifecycle()
    val bgColor by viewModel.backgroundColor.collectAsStateWithLifecycle()
    val textColor by viewModel.textColor.collectAsStateWithLifecycle()
    val sourceColor by viewModel.sourceColor.collectAsStateWithLifecycle()
    val cornerRadius by viewModel.cornerRadius.collectAsStateWithLifecycle()
    val borderWidth by viewModel.borderWidth.collectAsStateWithLifecycle()
    val borderColor by viewModel.borderColor.collectAsStateWithLifecycle()
    val padding by viewModel.padding.collectAsStateWithLifecycle()
    val useDynamicColors by viewModel.useDynamicColors.collectAsStateWithLifecycle()
    val maxHighlightLength by viewModel.maxHighlightLength.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val refreshInterval by viewModel.refreshIntervalMinutes.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Readwise Widget") },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ── Preview ────────────────────────────────────────────
            SectionHeader("Preview")
            WidgetPreview(
                fontSize = fontSize,
                fontFamily = fontFamily,
                bgColor = bgColor,
                textColor = textColor,
                sourceColor = sourceColor,
                cornerRadius = cornerRadius,
                borderWidth = borderWidth,
                borderColor = borderColor,
                padding = padding,
            )

            Spacer(Modifier.height(8.dp))

            // ── API Configuration ──────────────────────────────────
            SectionHeader("API Configuration")
            ApiSection(
                token = apiToken,
                onTokenChange = viewModel::setApiToken,
                syncState = syncState,
                highlightCount = highlightCount,
                onSync = viewModel::sync,
            )

            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = onNavigateToHighlights,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Manage Highlights")
            }

            Spacer(Modifier.height(8.dp))

            // ── Filter ─────────────────────────────────────────────
            SectionHeader("Filter")
            FilterSection(
                books = books,
                tags = tags,
                selectedBookId = filterBookId,
                selectedTagName = filterTagName,
                onBookSelected = viewModel::setFilterBookId,
                onTagSelected = viewModel::setFilterTagName,
            )
            Spacer(Modifier.height(4.dp))
            LabeledSlider(
                label = "Max Highlight Length",
                value = maxHighlightLength.toFloat(),
                onValueChange = { viewModel.setMaxHighlightLength(it.roundToInt()) },
                valueRange = 50f..1000f,
                steps = 18,
                valueLabel = "$maxHighlightLength chars",
            )

            Spacer(Modifier.height(8.dp))

            // ── Appearance ─────────────────────────────────────────
            SectionHeader("Appearance")
            AppearanceSection(
                fontSize = fontSize,
                onFontSizeChange = viewModel::setFontSize,
                fontFamily = fontFamily,
                onFontFamilyChange = viewModel::setFontFamily,
                bgColor = bgColor,
                onBgColorChange = viewModel::setBackgroundColor,
                textColor = textColor,
                onTextColorChange = viewModel::setTextColor,
                sourceColor = sourceColor,
                onSourceColorChange = viewModel::setSourceColor,
                cornerRadius = cornerRadius,
                onCornerRadiusChange = viewModel::setCornerRadius,
                borderWidth = borderWidth,
                onBorderWidthChange = viewModel::setBorderWidth,
                borderColor = borderColor,
                onBorderColorChange = viewModel::setBorderColor,
                padding = padding,
                onPaddingChange = viewModel::setPadding,
                useDynamicColors = useDynamicColors,
                onUseDynamicColorsChange = viewModel::setUseDynamicColors,
            )

            Spacer(Modifier.height(8.dp))

            // ── Refresh ────────────────────────────────────────────
            SectionHeader("Refresh")
            RefreshSection(
                selectedMinutes = refreshInterval,
                onSelected = viewModel::setRefreshIntervalMinutes,
            )

            Spacer(Modifier.height(16.dp))

            // ── Save Button ───────────────────────────────────────
            Button(
                onClick = viewModel::save,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = if (saveState) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                    )
                } else {
                    ButtonDefaults.buttonColors()
                },
            ) {
                if (saveState) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Saved!", style = MaterialTheme.typography.titleMedium)
                } else {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save & Apply to Widget", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Section Header ─────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Column {
        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        HorizontalDivider(Modifier.padding(top = 4.dp, bottom = 8.dp))
    }
}

// ── Widget Preview ─────────────────────────────────────────────────────

@Composable
private fun WidgetPreview(
    fontSize: Float,
    fontFamily: String,
    bgColor: Long,
    textColor: Long,
    sourceColor: Long,
    cornerRadius: Float,
    borderWidth: Float,
    borderColor: Long,
    padding: Float,
) {
    val shape = RoundedCornerShape(cornerRadius.dp)
    val borderMod = if (borderWidth > 0f) {
        Modifier.border(borderWidth.dp, borderColor.toComposeColor(), shape)
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(borderMod)
            .background(bgColor.toComposeColor(), shape)
            .padding(padding.dp),
    ) {
        Column {
            Text(
                text = "\u201CThis is a sample highlight text to preview your widget design.\u201D",
                color = textColor.toComposeColor(),
                fontSize = fontSize.sp,
                fontFamily = fontFamily.toFontFamily(),
                lineHeight = (fontSize * 1.4f).sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Book Title \u2014 Author Name",
                color = sourceColor.toComposeColor(),
                fontSize = (fontSize * 0.8f).sp,
                fontFamily = fontFamily.toFontFamily(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ── API Section ────────────────────────────────────────────────────────

@Composable
private fun ApiSection(
    token: String,
    onTokenChange: (String) -> Unit,
    syncState: SyncState,
    highlightCount: Int,
    onSync: () -> Unit,
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = token,
        onValueChange = onTokenChange,
        label = { Text("API Token") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (passwordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    imageVector = if (passwordVisible) {
                        Icons.Default.VisibilityOff
                    } else {
                        Icons.Default.Visibility
                    },
                    contentDescription = if (passwordVisible) "Hide token" else "Show token",
                )
            }
        },
    )

    Spacer(Modifier.height(8.dp))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = onSync,
            enabled = token.isNotBlank() && syncState !is SyncState.Syncing,
        ) {
            if (syncState is SyncState.Syncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(8.dp))
                Text("Syncing...")
            } else {
                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sync")
            }
        }

        Text(
            text = "$highlightCount highlights cached",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    // Status message
    val statusModifier = Modifier
        .fillMaxWidth()
        .animateContentSize()
    when (syncState) {
        is SyncState.Success -> {
            Text(
                text = "Synced ${syncState.count} highlights successfully.",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
                modifier = statusModifier,
            )
        }
        is SyncState.Error -> {
            Text(
                text = "Error: ${syncState.message}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = statusModifier,
            )
        }
        else -> { /* Idle / Syncing handled above */ }
    }
}

// ── Filter Section ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSection(
    books: List<com.readwise.widget.data.BookEntity>,
    tags: List<com.readwise.widget.data.TagEntity>,
    selectedBookId: Long?,
    selectedTagName: String?,
    onBookSelected: (Long?) -> Unit,
    onTagSelected: (String?) -> Unit,
) {
    // Book dropdown
    var bookExpanded by remember { mutableStateOf(false) }
    val selectedBookTitle = books.find { it.id == selectedBookId }?.title ?: "All Books"

    ExposedDropdownMenuBox(
        expanded = bookExpanded,
        onExpandedChange = { bookExpanded = it },
    ) {
        OutlinedTextField(
            value = selectedBookTitle,
            onValueChange = {},
            readOnly = true,
            label = { Text("Book") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bookExpanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = bookExpanded,
            onDismissRequest = { bookExpanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("All Books") },
                onClick = {
                    onBookSelected(null)
                    bookExpanded = false
                },
            )
            books.forEach { book ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = book.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    onClick = {
                        onBookSelected(book.id)
                        bookExpanded = false
                    },
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    // Tag dropdown
    var tagExpanded by remember { mutableStateOf(false) }
    val selectedTagDisplay = selectedTagName ?: "All Tags"

    ExposedDropdownMenuBox(
        expanded = tagExpanded,
        onExpandedChange = { tagExpanded = it },
    ) {
        OutlinedTextField(
            value = selectedTagDisplay,
            onValueChange = {},
            readOnly = true,
            label = { Text("Tag") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tagExpanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = tagExpanded,
            onDismissRequest = { tagExpanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("All Tags") },
                onClick = {
                    onTagSelected(null)
                    tagExpanded = false
                },
            )
            tags.forEach { tag ->
                DropdownMenuItem(
                    text = { Text(tag.name) },
                    onClick = {
                        onTagSelected(tag.name)
                        tagExpanded = false
                    },
                )
            }
        }
    }
}

// ── Appearance Section ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceSection(
    fontSize: Float,
    onFontSizeChange: (Float) -> Unit,
    fontFamily: String,
    onFontFamilyChange: (String) -> Unit,
    bgColor: Long,
    onBgColorChange: (Long) -> Unit,
    textColor: Long,
    onTextColorChange: (Long) -> Unit,
    sourceColor: Long,
    onSourceColorChange: (Long) -> Unit,
    cornerRadius: Float,
    onCornerRadiusChange: (Float) -> Unit,
    borderWidth: Float,
    onBorderWidthChange: (Float) -> Unit,
    borderColor: Long,
    onBorderColorChange: (Long) -> Unit,
    padding: Float,
    onPaddingChange: (Float) -> Unit,
    useDynamicColors: Boolean,
    onUseDynamicColorsChange: (Boolean) -> Unit,
) {
    // Font size slider
    LabeledSlider(
        label = "Font Size",
        value = fontSize,
        onValueChange = onFontSizeChange,
        valueRange = 10f..32f,
        steps = 21,
        valueLabel = "${fontSize.roundToInt()} sp",
    )

    // Font family dropdown
    var fontExpanded by remember { mutableStateOf(false) }
    val fontLabel = fontFamilyOptions.find { it.first == fontFamily }?.second ?: "Default"

    ExposedDropdownMenuBox(
        expanded = fontExpanded,
        onExpandedChange = { fontExpanded = it },
    ) {
        OutlinedTextField(
            value = fontLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Font Family") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fontExpanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = fontExpanded,
            onDismissRequest = { fontExpanded = false },
        ) {
            fontFamilyOptions.forEach { (key, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onFontFamilyChange(key)
                        fontExpanded = false
                    },
                )
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    // Color pickers
    ColorPickerRow(
        label = "Background",
        currentColor = bgColor,
        onColorChange = onBgColorChange,
    )
    ColorPickerRow(
        label = "Text",
        currentColor = textColor,
        onColorChange = onTextColorChange,
    )
    ColorPickerRow(
        label = "Source Text",
        currentColor = sourceColor,
        onColorChange = onSourceColorChange,
    )

    Spacer(Modifier.height(4.dp))

    // Corner radius
    LabeledSlider(
        label = "Corner Radius",
        value = cornerRadius,
        onValueChange = onCornerRadiusChange,
        valueRange = 0f..32f,
        steps = 31,
        valueLabel = "${cornerRadius.roundToInt()} dp",
    )

    // Border width
    LabeledSlider(
        label = "Border Width",
        value = borderWidth,
        onValueChange = onBorderWidthChange,
        valueRange = 0f..8f,
        steps = 15,
        valueLabel = "${"%.1f".format(borderWidth)} dp",
    )

    // Border color
    ColorPickerRow(
        label = "Border",
        currentColor = borderColor,
        onColorChange = onBorderColorChange,
    )

    // Padding
    LabeledSlider(
        label = "Padding",
        value = padding,
        onValueChange = onPaddingChange,
        valueRange = 4f..32f,
        steps = 27,
        valueLabel = "${padding.roundToInt()} dp",
    )

    // Dynamic colors toggle
    Spacer(Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Use Dynamic Colors (Material You)",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "Apply system accent colors to the app theme",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = useDynamicColors,
            onCheckedChange = onUseDynamicColorsChange,
        )
    }
}

// ── Refresh Section ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RefreshSection(
    selectedMinutes: Int,
    onSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = refreshOptions.find { it.minutes == selectedMinutes }?.label ?: "$selectedMinutes min"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Refresh Interval") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            refreshOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSelected(option.minutes)
                        expanded = false
                    },
                )
            }
        }
    }
}

// ── Labeled Slider ─────────────────────────────────────────────────────

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
        )
    }
}

// ── Color Picker Row ───────────────────────────────────────────────────

private val presetColors = listOf(
    0xFFFFFFFFL, // White
    0xFF000000L, // Black
    0xFFF44336L, // Red
    0xFFE91E63L, // Pink
    0xFF9C27B0L, // Purple
    0xFF673AB7L, // Deep Purple
    0xFF3F51B5L, // Indigo
    0xFF2196F3L, // Blue
    0xFF03A9F4L, // Light Blue
    0xFF009688L, // Teal
    0xFF4CAF50L, // Green
    0xFFFFEB3BL, // Yellow
    0xFFFF9800L, // Orange
    0xFF795548L, // Brown
    0xFF607D8BL, // Blue Grey
    0xFF9E9E9EL, // Grey
    0xFFFFF8E1L, // Cream
    0xFFE8EAF6L, // Lavender
    0xFFE0F2F1L, // Mint
    0xFFFCE4ECL, // Light Pink
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPickerRow(
    label: String,
    currentColor: Long,
    onColorChange: (Long) -> Unit,
) {
    var hexInput by remember(currentColor) {
        mutableStateOf(
            "%06X".format(currentColor and 0xFFFFFFL)
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label Color",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            presetColors.forEach { color ->
                val isSelected = color == currentColor
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color.toComposeColor(), CircleShape)
                        .then(
                            if (isSelected) {
                                Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            } else {
                                Modifier.border(1.dp, Color.Gray.copy(alpha = 0.3f), CircleShape)
                            }
                        )
                        .clickable { onColorChange(color) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            modifier = Modifier.size(16.dp),
                            tint = if (color == 0xFFFFFFFFL || color == 0xFFFFEB3BL || color == 0xFFFFF8E1L) {
                                Color.Black
                            } else {
                                Color.White
                            },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("#", style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = hexInput,
                onValueChange = { value ->
                    val filtered = value.filter { it.isLetterOrDigit() }.take(6).uppercase()
                    hexInput = filtered
                    if (filtered.length == 6) {
                        try {
                            val parsed = 0xFF000000L or filtered.toLong(16)
                            onColorChange(parsed)
                        } catch (_: NumberFormatException) {
                            // ignore invalid input
                        }
                    }
                },
                modifier = Modifier.width(120.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(currentColor.toComposeColor(), CircleShape)
                    .border(1.dp, Color.Gray.copy(alpha = 0.3f), CircleShape),
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}
