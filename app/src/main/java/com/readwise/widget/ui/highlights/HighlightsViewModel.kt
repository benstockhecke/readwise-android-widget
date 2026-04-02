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

enum class SortField { BOOK, AUTHOR, LENGTH, DATE }
enum class SortDirection { ASC, DESC }
enum class InclusionFilter { ALL, INCLUDED, EXCLUDED }

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

class HighlightsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ReadwiseApp
    private val repository = app.highlightRepository

    private val _allHighlights = MutableStateFlow<List<HighlightWithBookTuple>>(emptyList())
    private val _filter = MutableStateFlow(HighlightsFilter())
    val filter: StateFlow<HighlightsFilter> = _filter.asStateFlow()

    private val _books = MutableStateFlow<List<BookEntity>>(emptyList())
    val books: StateFlow<List<BookEntity>> = _books.asStateFlow()

    private val _tags = MutableStateFlow<List<TagEntity>>(emptyList())
    val tags: StateFlow<List<TagEntity>> = _tags.asStateFlow()

    private val _maxTextLength = MutableStateFlow(1000)
    val maxTextLength: StateFlow<Int> = _maxTextLength.asStateFlow()

    // Tags cache for detail view
    private val _highlightTags = MutableStateFlow<Map<Long, List<TagEntity>>>(emptyMap())
    val highlightTags: StateFlow<Map<Long, List<TagEntity>>> = _highlightTags.asStateFlow()

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
                        val tagHighlightIds = _highlightTags.value
                            .filter { (_, tags) -> tags.any { it.name == filter.tagName } }
                            .keys
                        list.filter { it.id in tagHighlightIds }
                    } else {
                        list
                    }
                }
                .let { list ->
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
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _allHighlights.value = repository.getAllHighlightsWithBook()
            _maxTextLength.value = _allHighlights.value.maxOfOrNull { it.text.length } ?: 1000
            _books.value = repository.getBooks()
            _tags.value = repository.getTags()
            // Preload tags for all highlights
            val tagsMap = mutableMapOf<Long, List<TagEntity>>()
            for (h in _allHighlights.value) {
                tagsMap[h.id] = repository.getTagsForHighlight(h.id)
            }
            _highlightTags.value = tagsMap
        }
    }

    fun updateFilter(update: (HighlightsFilter) -> HighlightsFilter) {
        _filter.value = update(_filter.value)
    }

    fun toggleExcluded(id: Long) {
        viewModelScope.launch {
            val current = _allHighlights.value.find { it.id == id } ?: return@launch
            val newExcluded = !current.excluded
            repository.setExcluded(id, newExcluded)
            _allHighlights.value = _allHighlights.value.map {
                if (it.id == id) it.copy(excluded = newExcluded) else it
            }
        }
    }

    fun setFilteredIncluded() {
        viewModelScope.launch {
            val visibleIds = filteredHighlights.value.map { it.id }.toSet()
            for (id in visibleIds) {
                repository.setExcluded(id, false)
            }
            _allHighlights.value = _allHighlights.value.map {
                if (it.id in visibleIds) it.copy(excluded = false) else it
            }
        }
    }

    fun setFilteredExcluded() {
        viewModelScope.launch {
            val visibleIds = filteredHighlights.value.map { it.id }.toSet()
            for (id in visibleIds) {
                repository.setExcluded(id, true)
            }
            _allHighlights.value = _allHighlights.value.map {
                if (it.id in visibleIds) it.copy(excluded = true) else it
            }
        }
    }
}
