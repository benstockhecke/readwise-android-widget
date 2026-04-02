package com.readwise.widget.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Paginated response wrapper returned by the Readwise `/highlights/` endpoint.
 *
 * @property count Total number of highlights available across all pages.
 * @property next URL of the next page, or `null` if this is the last page.
 * @property results Highlights on the current page.
 */
@Serializable
data class HighlightResult(
    val count: Int,
    val next: String? = null,
    val results: List<HighlightDto>,
)

/**
 * A single highlight as returned by the Readwise API.
 *
 * @property id Unique identifier for the highlight.
 * @property text The highlighted passage.
 * @property note User-written note attached to the highlight.
 * @property location Position of the highlight within the source document.
 * @property locationType How [location] should be interpreted (e.g. "page", "order").
 * @property highlightUrl Deep-link URL back to the highlight in Readwise.
 * @property url Source URL of the document the highlight came from.
 * @property bookId ID of the parent book/article this highlight belongs to.
 * @property tags Tags assigned to this highlight.
 * @property highlightedAt ISO-8601 timestamp of when the highlight was created.
 * @property isDiscard Whether this highlight has been archived/discarded in Readwise.
 */
@Serializable
data class HighlightDto(
    val id: Long,
    val text: String,
    val note: String,
    val location: Int,
    @SerialName("location_type") val locationType: String,
    @SerialName("highlight_url") val highlightUrl: String? = null,
    val url: String? = null,
    @SerialName("book_id") val bookId: Long,
    val tags: List<TagDto>,
    @SerialName("highlighted_at") val highlightedAt: String? = null,
    @SerialName("is_discard") val isDiscard: Boolean = false,
)

/**
 * Paginated response wrapper returned by the Readwise `/books/` endpoint.
 *
 * @property count Total number of books available across all pages.
 * @property next URL of the next page, or `null` if this is the last page.
 * @property results Books on the current page.
 */
@Serializable
data class BookResult(
    val count: Int,
    val next: String? = null,
    val results: List<BookDto>,
)

/**
 * A single book (or article/podcast/etc.) as returned by the Readwise API.
 *
 * @property id Unique identifier for the book.
 * @property title Title of the source document.
 * @property author Author name, if available.
 * @property category Content category (e.g. "books", "articles", "tweets").
 * @property sourceUrl Original URL of the source document, if available.
 * @property coverImageUrl URL of the cover thumbnail image, if available.
 * @property numHighlights Number of highlights the user has saved from this book.
 */
@Serializable
data class BookDto(
    val id: Long,
    val title: String,
    val author: String? = null,
    val category: String,
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("cover_image_url") val coverImageUrl: String? = null,
    @SerialName("num_highlights") val numHighlights: Int,
)

/**
 * A tag as returned by the Readwise API.
 *
 * @property id Unique identifier for the tag.
 * @property name Human-readable tag label.
 */
@Serializable
data class TagDto(
    val id: Long,
    val name: String,
)
