package com.readwise.widget.ui.highlights

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.readwise.widget.data.HighlightWithBookTuple
import com.readwise.widget.data.TagEntity
import kotlinx.coroutines.launch

// ── Main Screen ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HighlightsListScreen(
    onBack: () -> Unit,
    viewModel: HighlightsViewModel = viewModel(),
) {
    val highlights by viewModel.filteredHighlights.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val books by viewModel.books.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val highlightTags by viewModel.highlightTags.collectAsStateWithLifecycle()
    val maxTextLength by viewModel.maxTextLength.collectAsStateWithLifecycle()

    var showFilterSheet by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedHighlight by remember { mutableStateOf<HighlightWithBookTuple?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Highlights")
                        Text(
                            "${highlights.size} shown",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.setFilteredIncluded() }) {
                        Icon(Icons.Default.SelectAll, contentDescription = "Include shown")
                    }
                    IconButton(onClick = { viewModel.setFilteredExcluded() }) {
                        Icon(Icons.Default.Deselect, contentDescription = "Exclude shown")
                    }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                        }
                        SortDropdown(
                            expanded = showSortMenu,
                            currentSort = filter.sortField,
                            currentDirection = filter.sortDirection,
                            onDismiss = { showSortMenu = false },
                            onSelect = { field, direction ->
                                viewModel.updateFilter { it.copy(sortField = field, sortDirection = direction) }
                                showSortMenu = false
                            },
                        )
                    }
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Search bar
            SearchBar(
                query = filter.query,
                onQueryChange = { q -> viewModel.updateFilter { it.copy(query = q) } },
            )

            // Active filter chips
            ActiveFilterChips(
                filter = filter,
                books = books,
                onClearBook = { viewModel.updateFilter { it.copy(bookId = null) } },
                onClearTag = { viewModel.updateFilter { it.copy(tagName = null) } },
                onClearInclusion = { viewModel.updateFilter { it.copy(inclusion = InclusionFilter.ALL) } },
                onClearLength = { viewModel.updateFilter { it.copy(minLength = 0, maxLength = Int.MAX_VALUE) } },
                maxTextLength = maxTextLength,
            )

            // Highlights list
            if (highlights.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No highlights match your filters.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(highlights, key = { it.id }) { highlight ->
                        HighlightRow(
                            highlight = highlight,
                            onClick = { selectedHighlight = highlight },
                            onToggleExcluded = { viewModel.toggleExcluded(highlight.id) },
                        )
                    }
                }
            }
        }
    }

    // Filter bottom sheet
    if (showFilterSheet) {
        FilterBottomSheet(
            filter = filter,
            books = books,
            tags = tags,
            maxTextLength = maxTextLength,
            onFilterChange = { newFilter -> viewModel.updateFilter { newFilter } },
            onDismiss = { showFilterSheet = false },
        )
    }

    // Detail bottom sheet
    selectedHighlight?.let { highlight ->
        DetailBottomSheet(
            highlight = highlight,
            tags = highlightTags[highlight.id] ?: emptyList(),
            onDismiss = { selectedHighlight = null },
        )
    }
}

// ── Search Bar ────────────────────────────────────────────────────────

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Search highlights...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        },
        singleLine = true,
    )
}

// ── Active Filter Chips ───────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveFilterChips(
    filter: HighlightsFilter,
    books: List<com.readwise.widget.data.BookEntity>,
    onClearBook: () -> Unit,
    onClearTag: () -> Unit,
    onClearInclusion: () -> Unit,
    onClearLength: () -> Unit,
    maxTextLength: Int,
) {
    val hasLengthFilter = filter.minLength > 0 || filter.maxLength < maxTextLength
    val hasFilters = filter.bookId != null || filter.tagName != null || filter.inclusion != InclusionFilter.ALL || hasLengthFilter
    if (!hasFilters) return

    FlowRow(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        filter.bookId?.let { bookId ->
            val bookTitle = books.find { it.id == bookId }?.title ?: "Book"
            AssistChip(
                onClick = onClearBook,
                label = { Text(bookTitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp)) },
            )
        }
        filter.tagName?.let { tag ->
            AssistChip(
                onClick = onClearTag,
                label = { Text(tag) },
                trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp)) },
            )
        }
        if (filter.inclusion != InclusionFilter.ALL) {
            AssistChip(
                onClick = onClearInclusion,
                label = { Text(if (filter.inclusion == InclusionFilter.INCLUDED) "Included" else "Excluded") },
                trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp)) },
            )
        }
        if (hasLengthFilter) {
            AssistChip(
                onClick = onClearLength,
                label = { Text("${filter.minLength}–${if (filter.maxLength == Int.MAX_VALUE) "∞" else filter.maxLength} chars") },
                trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp)) },
            )
        }
    }
}

// ── Highlight Row ─────────────────────────────────────────────────────

@Composable
private fun HighlightRow(
    highlight: HighlightWithBookTuple,
    onClick: () -> Unit,
    onToggleExcluded: () -> Unit,
) {
    val bgColor by animateColorAsState(
        if (highlight.excluded) {
            MaterialTheme.colorScheme.surfaceContainerLow
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "rowBg",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
        ) {
            IconButton(
                onClick = onToggleExcluded,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = if (highlight.excluded) {
                        Icons.Default.CheckBoxOutlineBlank
                    } else {
                        Icons.Default.CheckBox
                    },
                    contentDescription = if (highlight.excluded) "Include" else "Exclude",
                    tint = if (highlight.excluded) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }

            Spacer(Modifier.width(4.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (highlight.excluded) 0.5f else 1f),
            ) {
                Text(
                    text = highlight.text,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = buildString {
                            append(highlight.bookTitle ?: "Unknown")
                            if (!highlight.bookAuthor.isNullOrBlank()) {
                                append(" \u2014 ")
                                append(highlight.bookAuthor)
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${highlight.text.length} chars",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
    HorizontalDivider()
}

// ── Sort Dropdown ─────────────────────────────────────────────────────

@Composable
private fun SortDropdown(
    expanded: Boolean,
    currentSort: SortField,
    currentDirection: SortDirection,
    onDismiss: () -> Unit,
    onSelect: (SortField, SortDirection) -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        SortField.entries.forEach { field ->
            val label = when (field) {
                SortField.BOOK -> "Book Title"
                SortField.AUTHOR -> "Author"
                SortField.LENGTH -> "Length"
                SortField.DATE -> "Date"
            }
            val isActive = currentSort == field
            val arrow = if (isActive) {
                if (currentDirection == SortDirection.ASC) " \u2191" else " \u2193"
            } else ""

            DropdownMenuItem(
                text = {
                    Text(
                        text = "$label$arrow",
                        color = if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                    )
                },
                onClick = {
                    val newDirection = if (isActive && currentDirection == SortDirection.ASC) {
                        SortDirection.DESC
                    } else {
                        SortDirection.ASC
                    }
                    onSelect(field, newDirection)
                },
            )
        }
    }
}

// ── Filter Bottom Sheet ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterBottomSheet(
    filter: HighlightsFilter,
    books: List<com.readwise.widget.data.BookEntity>,
    tags: List<TagEntity>,
    maxTextLength: Int,
    onFilterChange: (HighlightsFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Filter", style = MaterialTheme.typography.titleLarge)

            // Book filter
            Text("Book", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { onFilterChange(filter.copy(bookId = null)) },
                    label = { Text("All Books") },
                )
                books.forEach { book ->
                    val selected = filter.bookId == book.id
                    AssistChip(
                        onClick = {
                            onFilterChange(filter.copy(bookId = if (selected) null else book.id))
                        },
                        label = {
                            Text(
                                book.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                            )
                        },
                    )
                }
            }

            // Tag filter
            Text("Tag", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { onFilterChange(filter.copy(tagName = null)) },
                    label = { Text("All Tags") },
                )
                tags.forEach { tag ->
                    val selected = filter.tagName == tag.name
                    AssistChip(
                        onClick = {
                            onFilterChange(filter.copy(tagName = if (selected) null else tag.name))
                        },
                        label = {
                            Text(
                                tag.name,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                            )
                        },
                    )
                }
            }

            // Inclusion filter
            Text("Status", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InclusionFilter.entries.forEach { option ->
                    val selected = filter.inclusion == option
                    val label = when (option) {
                        InclusionFilter.ALL -> "All"
                        InclusionFilter.INCLUDED -> "Included"
                        InclusionFilter.EXCLUDED -> "Excluded"
                    }
                    AssistChip(
                        onClick = { onFilterChange(filter.copy(inclusion = option)) },
                        label = {
                            Text(
                                label,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                            )
                        },
                    )
                }
            }

            // Length range filter
            val sliderMax = maxTextLength.coerceAtLeast(1).toFloat()
            val currentMin = filter.minLength.toFloat().coerceIn(0f, sliderMax)
            val currentMax = (if (filter.maxLength == Int.MAX_VALUE) sliderMax else filter.maxLength.toFloat()).coerceIn(currentMin, sliderMax)

            Text("Length", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("${currentMin.toInt()} chars", style = MaterialTheme.typography.bodySmall)
                Text("${currentMax.toInt()} chars", style = MaterialTheme.typography.bodySmall)
            }
            RangeSlider(
                value = currentMin..currentMax,
                onValueChange = { range ->
                    onFilterChange(
                        filter.copy(
                            minLength = range.start.toInt(),
                            maxLength = if (range.endInclusive >= sliderMax) Int.MAX_VALUE else range.endInclusive.toInt(),
                        )
                    )
                },
                valueRange = 0f..sliderMax,
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Detail Bottom Sheet ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DetailBottomSheet(
    highlight: HighlightWithBookTuple,
    tags: List<TagEntity>,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Highlight text
            Text(
                text = "\u201C${highlight.text}\u201D",
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = FontStyle.Italic,
            )

            HorizontalDivider()

            // Metadata
            DetailRow("Book", highlight.bookTitle ?: "Unknown")
            DetailRow("Author", highlight.bookAuthor ?: "Unknown")
            DetailRow("Length", "${highlight.text.length} characters")
            DetailRow("Status", if (highlight.excluded) "Excluded" else "Included")
            highlight.highlightedAt?.let { date ->
                DetailRow("Highlighted", date.take(10)) // show date part
            }
            if (highlight.note.isNotBlank()) {
                DetailRow("Note", highlight.note)
            }

            // Tags
            if (tags.isNotEmpty()) {
                Text("Tags", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tags.forEach { tag ->
                        AssistChip(
                            onClick = {},
                            label = { Text(tag.name) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}
