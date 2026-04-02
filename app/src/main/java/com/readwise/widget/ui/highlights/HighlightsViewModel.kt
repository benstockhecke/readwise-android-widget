package com.readwise.widget.ui.highlights

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.readwise.widget.ReadwiseApp
import com.readwise.widget.data.BookEntity
import com.readwise.widget.data.HighlightWithBookTuple
import com.readwise.widget.data.TagEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Fields by which the highlight list can be sorted. */
enum class SortField { BOOK, AUTHOR, LENGTH, DATE }

/** Direction for sorting the highlight list. */
enum class SortDirection { ASC, DESC }

/** Controls which highlights are shown based on their excluded/included status. */
enum class InclusionFilter { ALL, INCLUDED, EXCLUDED }

/**
 * Immutable snapshot of all active filter and sort options for the highlight list.
 *
 * @property query Free-text search string matched against text, book title, and author.
 * @property bookId If non-null, only highlights from this book are shown.
 * @property tagName If non-null, only highlights tagged with this name are shown.
 * @property inclusion Filters highlights by their excluded/included status.
 * @property minLength Minimum character length for included highlights.
 * @property maxLength Maximum character length for included highlights.
 * @property sortField The field used to sort the list.
 * @property sortDirection Ascending or descending sort order.
 */
data class HighlightsFilter(
    val query: String = "",
    val bookId: Long? = null,
    val tagName: String? = null,
    val inclusion: InclusionFilter = InclusionFilter.ALL,
    val minLength: Int = 0,
    val maxLength: Int = Int.MAX_VALUE,
    val sortField: SortField = SortField.BOOK,
    val sortDirection: SortDirection = SortDirection.ASC,
)

/**
 * [AndroidViewModel] that backs the highlight management screen.
 *
 * Loads all highlights with their book metadata from the local database, then
 * derives [filteredHighlights] reactively by combining the full list with the
 * current [filter]. Tags are eagerly pre-loaded into [highlightTags] so that
 * tag-based filtering and detail views are fast.
 *
 * @param application Application instance used to access app-level singletons.
 */
class HighlightsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ReadwiseApp
    private val repository = app.highlightRepository

    /** The complete unfiltered list of highlights with book metadata. */
    private val _allHighlights = MutableStateFlow<List<HighlightWithBookTuple>>(emptyList())

    /** Currently active filter/sort configuration. */
    private val _filter = MutableStateFlow(HighlightsFilter())
    val filter: StateFlow<HighlightsFilter> = _filter.asStateFlow()

    /** All books available in the local database, used to populate the book filter picker. */
    private val _books = MutableStateFlow<List<BookEntity>>(emptyList())
    val books: StateFlow<List<BookEntity>> = _books.asStateFlow()

    /** All tags available in the local database, used to populate the tag filter picker. */
    private val _tags = MutableStateFlow<List<TagEntity>>(emptyList())
    val tags: StateFlow<List<TagEntity>> = _tags.asStateFlow()

    /**
     * The character length of the longest highlight in the dataset. Used to set
     * the upper bound of the length-range filter slider in the UI.
     */
    private val _maxTextLength = MutableStateFlow(1000)
    val maxTextLength: StateFlow<Int> = _maxTextLength.asStateFlow()

    // Tags cache for detail view
    /**
     * Map of highlight ID to its associated tags, pre-loaded at startup.
     * Used for tag-based filtering and the highlight detail view.
     */
    private val _highlightTags = MutableStateFlow<Map<Long, List<TagEntity>>>(emptyMap())
    val highlightTags: StateFlow<Map<Long, List<TagEntity>>> = _highlightTags.asStateFlow()

    /**
     * The filtered and sorted list of highlights, derived reactively from
     * [_allHighlights] and [_filter].
     *
     * Filtering applies in this order:
     * 1. Free-text query (text, book title, author).
     * 2. Book ID filter.
     * 3. Inclusion/exclusion status.
     * 4. Character length range.
     * 5. Tag name (requires a lookup in [_highlightTags]).
     *
     * Sorting is applied after all filters.
     */
    val filteredHighlights: StateFlow<List<HighlightWithBookTuple>> =
        combine(_allHighlights, _filter) { highlights, filter ->
            highlights
                .filter { h ->
                    val matchesQuery = filter.query.isBlank() ||
                        h.text.contains(filter.query, ignoreCase = true) ||
                        (h.bookTitle?.contains(filter.query, ignoreCase = true) == true) ||
                        (h.bookAuthor?.contains(filter.query, ignoreCase = true) == true)
                    val matchesBook = filter.bookId == null || h.bookId == filter.bookId
                    val matchesInclusion = when (filter.inclusion) {
                        InclusionFilter.ALL -> true
                        InclusionFilter.INCLUDED -> !h.excluded
                        InclusionFilter.EXCLUDED -> h.excluded
                    }
                    val matchesLength = h.text.length in filter.minLength..filter.maxLength
                    matchesQuery && matchesBook && matchesInclusion && matchesLength
                }
                .let { list ->
                    // Tag filter requires async lookup, handled separately via tagName
                    if (filter.tagName != null) {
                        // Find highlight IDs that have the requested tag in the pre-loaded cache
                        val tagHighlightIds = _highlightTags.value
                            .filter { (_, tags) -> tags.any { it.name == filter.tagName } }
                            .keys
                        list.filter { it.id in tagHighlightIds }
                    } else {
                        list
                    }
                }
                .let { list ->
                    // Build a comparator for the chosen sort field, then optionally reverse it
                    val comparator: Comparator<HighlightWithBookTuple> = when (filter.sortField) {
                        SortField.BOOK -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.bookTitle ?: "" }
                        SortField.AUTHOR -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.bookAuthor ?: "" }
                        SortField.LENGTH -> compareBy { it.text.length }
                        SortField.DATE -> compareBy { it.highlightedAt ?: "" }
                    }
                    if (filter.sortDirection == SortDirection.DESC) {
                        list.sortedWith(comparator.reversed())
                    } else {
                        list.sortedWith(comparator)
                    }
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Load all data when the ViewModel is first created
        loadData()
    }

    /**
     * Loads highlights, books, tags, and the highlight-tags map from the local database.
     *
     * Also computes [_maxTextLength] so the UI length filter slider has the correct range.
     */
    fun loadData() {
        viewModelScope.launch {
            _allHighlights.value = repository.getAllHighlightsWithBook()
            // Compute the length of the longest highlight for the slider upper bound
            _maxTextLength.value = _allHighlights.value.maxOfOrNull { it.text.length } ?: 1000
            _books.value = repository.getBooks()
            _tags.value = repository.getTags()
            // Preload tags for all highlights so the tag filter and detail view are instant
            val tagsMap = mutableMapOf<Long, List<TagEntity>>()
            for (h in _allHighlights.value) {
                tagsMap[h.id] = repository.getTagsForHighlight(h.id)
            }
            _highlightTags.value = tagsMap
        }
    }

    /**
     * Applies a transformation to the current [filter] and emits the updated value.
     *
     * @param update Lambda that receives the current [HighlightsFilter] and returns
     *   the new one with the desired changes applied.
     */
    fun updateFilter(update: (HighlightsFilter) -> HighlightsFilter) {
        _filter.value = update(_filter.value)
    }

    /**
     * Toggles the excluded flag of the highlight with the given [id] and
     * immediately reflects the change in [_allHighlights] for a snappy UI response.
     */
    fun toggleExcluded(id: Long) {
        viewModelScope.launch {
            val current = _allHighlights.value.find { it.id == id } ?: return@launch
            val newExcluded = !current.excluded
            repository.setExcluded(id, newExcluded)
            // Optimistically update the in-memory list without a full reload
            _allHighlights.value = _allHighlights.value.map {
                if (it.id == id) it.copy(excluded = newExcluded) else it
            }
        }
    }

    /**
     * Marks all currently visible (filtered) highlights as included (not excluded).
     * Persists the change to the database and updates the in-memory list.
     */
    fun setFilteredIncluded() {
        viewModelScope.launch {
            val visibleIds = filteredHighlights.value.map { it.id }.toSet()
            for (id in visibleIds) {
                repository.setExcluded(id, false)
            }
            // Reflect bulk change in memory to avoid a full database reload
            _allHighlights.value = _allHighlights.value.map {
                if (it.id in visibleIds) it.copy(excluded = false) else it
            }
        }
    }

    /**
     * Marks all currently visible (filtered) highlights as excluded.
     * Persists the change to the database and updates the in-memory list.
     */
    fun setFilteredExcluded() {
        viewModelScope.launch {
            val visibleIds = filteredHighlights.value.map { it.id }.toSet()
            for (id in visibleIds) {
                repository.setExcluded(id, true)
            }
            // Reflect bulk change in memory to avoid a full database reload
            _allHighlights.value = _allHighlights.value.map {
                if (it.id in visibleIds) it.copy(excluded = true) else it
            }
        }
    }
}
