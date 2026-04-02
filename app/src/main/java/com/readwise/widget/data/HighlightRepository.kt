package com.readwise.widget.data

import com.readwise.widget.api.ReadwiseApi

/**
 * Flattened view of a highlight combined with its source book's metadata,
 * suitable for display in the widget or UI lists.
 *
 * @property text The highlighted passage.
 * @property note User note attached to the highlight.
 * @property bookTitle Title of the source book.
 * @property bookAuthor Author of the source book, or `null` if unavailable.
 * @property highlightedAt ISO-8601 timestamp of when the highlight was created.
 */
data class HighlightWithBook(
    val text: String,
    val note: String,
    val bookTitle: String,
    val bookAuthor: String?,
    val highlightedAt: String?,
)

/**
 * Single source of truth for highlight data, coordinating between the remote
 * Readwise API and the local Room database.
 *
 * The [apiProvider] lambda is called lazily so that a new [ReadwiseApi] instance
 * (with the current API token) is created on each sync, avoiding stale credentials.
 *
 * @param db Local Room database instance.
 * @param apiProvider Returns a configured [ReadwiseApi] when a valid token is
 *   available, or `null` if no token has been set.
 */
class HighlightRepository(
    private val db: AppDatabase,
    private val apiProvider: () -> ReadwiseApi?,
) {
    /** Convenience accessor for the DAO. */
    private val dao get() = db.highlightDao()

    /**
     * Fetches all highlights and books from the Readwise API (paginated)
     * and stores them in the local Room database.  Tags and cross-refs are
     * extracted from each highlight's tag list.
     *
     * Preserves user-set "excluded" flags across syncs by saving them before
     * clearing and restoring them afterwards. Data is written page-by-page
     * to reduce peak memory usage for large libraries.
     *
     * If [apiProvider] returns `null` (no token configured) the function
     * returns immediately without modifying the database.
     */
    suspend fun syncHighlights() {
        val api = apiProvider() ?: return

        // Save excluded highlight IDs so they survive the full re-sync
        val excludedIds = dao.getExcludedHighlightIds()

        // Clear existing data so removed highlights/books are not retained
        dao.clearAll()

        // ── Fetch and persist books page by page ───────────────────────
        var bookPage = 1
        while (true) {
            val result = api.getBooks(page = bookPage)
            dao.insertBooks(result.results.map { dto ->
                BookEntity(
                    id = dto.id,
                    title = dto.title,
                    author = dto.author,
                    category = dto.category,
                    coverImageUrl = dto.coverImageUrl,
                )
            })
            if (result.next == null) break
            bookPage++
        }

        // ── Fetch and persist highlights page by page ──────────────────
        var highlightPage = 1
        while (true) {
            val result = api.getHighlights(page = highlightPage)

            val pageHighlights = mutableListOf<HighlightEntity>()
            val pageTags = mutableMapOf<Long, TagEntity>()
            val pageCrossRefs = mutableListOf<HighlightTagCrossRef>()

            for (dto in result.results) {
                if (dto.isDiscard) continue

                pageHighlights += HighlightEntity(
                    id = dto.id,
                    text = dto.text,
                    note = dto.note,
                    bookId = dto.bookId,
                    highlightedAt = dto.highlightedAt,
                )
                for (tag in dto.tags) {
                    pageTags[tag.id] = TagEntity(id = tag.id, name = tag.name)
                    pageCrossRefs += HighlightTagCrossRef(
                        highlightId = dto.id,
                        tagId = tag.id,
                    )
                }
            }

            // Write this page's data to the database immediately
            dao.insertHighlights(pageHighlights)
            dao.insertTags(pageTags.values.toList())
            dao.insertCrossRefs(pageCrossRefs)

            if (result.next == null) break
            highlightPage++
        }

        // Restore excluded flags that were set before the sync
        if (excludedIds.isNotEmpty()) {
            dao.restoreExcludedFlags(excludedIds)
        }
    }

    /**
     * Returns a random highlight together with its book metadata.
     * Optionally filtered by [bookId] or [tagName].
     *
     * The selection strategy is chosen in order of specificity:
     * book filter → tag filter → no filter. Both filters cannot be active
     * simultaneously; [bookId] takes precedence.
     *
     * @param bookId If non-null, only highlights from this book are considered.
     * @param tagName If non-null (and [bookId] is null), only highlights with this tag are considered.
     * @param maxLength Maximum character length of eligible highlights.
     * @return A [HighlightWithBook] for the selected highlight, or `null` if the
     *   database is empty or no highlight matches the current filters.
     */
    suspend fun getRandomHighlight(
        bookId: Long? = null,
        tagName: String? = null,
        maxLength: Int = Int.MAX_VALUE,
    ): HighlightWithBook? {
        val highlight = when {
            bookId != null -> dao.getRandomHighlightByBookId(bookId, maxLength)
            tagName != null -> dao.getRandomHighlightByTag(tagName, maxLength)
            else -> dao.getRandomHighlight(maxLength)
        } ?: return null

        val book = dao.getBookById(highlight.bookId)

        return HighlightWithBook(
            text = highlight.text,
            note = highlight.note,
            bookTitle = book?.title ?: "Unknown",
            bookAuthor = book?.author,
            highlightedAt = highlight.highlightedAt,
        )
    }

    /** Returns all books stored in the local database, ordered by title. */
    suspend fun getBooks(): List<BookEntity> = dao.getAllBooks()

    /** Returns all tags stored in the local database, ordered by name. */
    suspend fun getTags(): List<TagEntity> = dao.getAllTags()

    /** Returns the total number of highlights currently in the local database. */
    suspend fun getHighlightCount(): Int = dao.getHighlightCount()

    /** Returns all highlights joined with their book metadata, for the management UI. */
    suspend fun getAllHighlightsWithBook(): List<HighlightWithBookTuple> =
        dao.getAllHighlightsWithBook()

    /** Returns all tags associated with a specific highlight. */
    suspend fun getTagsForHighlight(highlightId: Long): List<TagEntity> =
        dao.getTagsForHighlight(highlightId)

    /** Sets the excluded flag on a single highlight. */
    suspend fun setExcluded(id: Long, excluded: Boolean) =
        dao.setExcluded(id, excluded)

    /** Sets the excluded flag on every highlight in the database. */
    suspend fun setAllExcluded(excluded: Boolean) =
        dao.setAllExcluded(excluded)
}
