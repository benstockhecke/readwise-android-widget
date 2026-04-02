package com.readwise.widget.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction

/**
 * Room entity representing a single saved highlight stored locally.
 *
 * @property id Readwise highlight ID (primary key).
 * @property text The highlighted passage.
 * @property note User note attached to the highlight.
 * @property bookId Foreign key referencing the parent [BookEntity].
 * @property highlightedAt ISO-8601 timestamp of when the highlight was created.
 * @property excluded When `true` this highlight is suppressed from widget rotation.
 */
@Entity(tableName = "highlights")
data class HighlightEntity(
    @PrimaryKey val id: Long,
    val text: String,
    val note: String,
    @ColumnInfo(name = "book_id") val bookId: Long,
    @ColumnInfo(name = "highlighted_at") val highlightedAt: String?,
    @ColumnInfo(name = "excluded", defaultValue = "0") val excluded: Boolean = false,
)

/**
 * Room entity representing a book (or article, tweet, etc.) that contains highlights.
 *
 * @property id Readwise book ID (primary key).
 * @property title Title of the source document.
 * @property author Author name, if available.
 * @property category Content category (e.g. "books", "articles").
 * @property coverImageUrl URL of the cover image, if available.
 */
@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val author: String?,
    val category: String,
    @ColumnInfo(name = "cover_image_url") val coverImageUrl: String?,
)

/**
 * Room entity representing a user-defined tag.
 *
 * @property id Readwise tag ID (primary key).
 * @property name Human-readable tag label.
 */
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val id: Long,
    val name: String,
)

/**
 * Junction table that models the many-to-many relationship between highlights and tags.
 *
 * Cascade-deletes ensure rows are removed automatically when the referenced
 * highlight or tag is deleted. Indices on both columns speed up lookups in
 * either direction.
 *
 * @property highlightId Foreign key referencing [HighlightEntity.id].
 * @property tagId Foreign key referencing [TagEntity.id].
 */
@Entity(
    tableName = "highlight_tag_cross_ref",
    primaryKeys = ["highlight_id", "tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = HighlightEntity::class,
            parentColumns = ["id"],
            childColumns = ["highlight_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["highlight_id"]),
        Index(value = ["tag_id"]),
    ],
)
data class HighlightTagCrossRef(
    @ColumnInfo(name = "highlight_id") val highlightId: Long,
    @ColumnInfo(name = "tag_id") val tagId: Long,
)

/**
 * Room DAO providing all database operations for highlights, books, tags, and their
 * relationships.
 */
@Dao
interface HighlightDao {

    // ── Random highlight queries (exclude excluded) ───────────────────

    /**
     * Returns a single random highlight whose text does not exceed [maxLength] characters,
     * excluding any highlights marked as excluded.
     */
    @Query("SELECT * FROM highlights WHERE excluded = 0 AND LENGTH(text) <= :maxLength ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomHighlight(maxLength: Int): HighlightEntity?

    /**
     * Returns a random non-excluded highlight from a specific book, filtered by [maxLength].
     */
    @Query("SELECT * FROM highlights WHERE excluded = 0 AND book_id = :bookId AND LENGTH(text) <= :maxLength ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomHighlightByBookId(bookId: Long, maxLength: Int): HighlightEntity?

    /**
     * Returns a random non-excluded highlight that has a tag matching [tagName],
     * filtered by [maxLength]. Uses a JOIN through the cross-reference table.
     */
    @Query(
        """
        SELECT h.* FROM highlights h
        INNER JOIN highlight_tag_cross_ref cr ON h.id = cr.highlight_id
        INNER JOIN tags t ON cr.tag_id = t.id
        WHERE h.excluded = 0 AND t.name = :tagName AND LENGTH(h.text) <= :maxLength
        ORDER BY RANDOM() LIMIT 1
        """
    )
    suspend fun getRandomHighlightByTag(tagName: String, maxLength: Int): HighlightEntity?

    // ── Single highlight ──────────────────────────────────────────────

    /** Returns the highlight with the given [id], or `null` if not found. */
    @Query("SELECT * FROM highlights WHERE id = :id")
    suspend fun getHighlightById(id: Long): HighlightEntity?

    // ── List all highlights (for management screen) ───────────────────

    /**
     * Returns all highlights joined with their parent book, ordered by book title
     * then highlight ID. Used by the highlight management screen.
     */
    @Query(
        """
        SELECT h.*, b.title AS book_title, b.author AS book_author
        FROM highlights h
        LEFT JOIN books b ON h.book_id = b.id
        ORDER BY b.title ASC, h.id ASC
        """
    )
    suspend fun getAllHighlightsWithBook(): List<HighlightWithBookTuple>

    // ── Tags for a highlight ──────────────────────────────────────────

    /** Returns all tags associated with the given highlight, ordered alphabetically. */
    @Query(
        """
        SELECT t.* FROM tags t
        INNER JOIN highlight_tag_cross_ref cr ON t.id = cr.tag_id
        WHERE cr.highlight_id = :highlightId
        ORDER BY t.name ASC
        """
    )
    suspend fun getTagsForHighlight(highlightId: Long): List<TagEntity>

    // ── Toggle excluded ───────────────────────────────────────────────

    /** Sets the excluded flag for a single highlight by [id]. */
    @Query("UPDATE highlights SET excluded = :excluded WHERE id = :id")
    suspend fun setExcluded(id: Long, excluded: Boolean)

    /** Sets the excluded flag for every highlight in the database. */
    @Query("UPDATE highlights SET excluded = :excluded")
    suspend fun setAllExcluded(excluded: Boolean)

    /** Returns the IDs of all highlights currently marked as excluded. */
    @Query("SELECT id FROM highlights WHERE excluded = 1")
    suspend fun getExcludedHighlightIds(): List<Long>

    /** Marks the given highlight IDs as excluded. */
    @Query("UPDATE highlights SET excluded = 1 WHERE id IN (:ids)")
    suspend fun restoreExcludedFlags(ids: List<Long>)

    // ── Insert ────────────────────────────────────────────────────────

    /** Inserts or replaces a batch of highlights. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlights(highlights: List<HighlightEntity>)

    /** Inserts or replaces a batch of books. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    /** Inserts or replaces a batch of tags. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<TagEntity>)

    /** Inserts or replaces a batch of highlight-tag cross-reference rows. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(refs: List<HighlightTagCrossRef>)

    // ── Query lists ───────────────────────────────────────────────────

    /** Returns all books ordered alphabetically by title. */
    @Query("SELECT * FROM books ORDER BY title ASC")
    suspend fun getAllBooks(): List<BookEntity>

    /** Returns all tags ordered alphabetically by name. */
    @Query("SELECT * FROM tags ORDER BY name ASC")
    suspend fun getAllTags(): List<TagEntity>

    /** Returns the book with the given [id], or `null` if not found. */
    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: Long): BookEntity?

    // ── Clear ─────────────────────────────────────────────────────────

    /**
     * Deletes all data from every table inside a single transaction.
     * Cross-refs are removed first to satisfy foreign key constraints.
     */
    @Transaction
    suspend fun clearAll() {
        deleteAllCrossRefs()
        deleteAllHighlights()
        deleteAllBooks()
        deleteAllTags()
    }

    /** Deletes all rows from the highlight-tag cross-reference table. */
    @Query("DELETE FROM highlight_tag_cross_ref")
    suspend fun deleteAllCrossRefs()

    /** Deletes all rows from the highlights table. */
    @Query("DELETE FROM highlights")
    suspend fun deleteAllHighlights()

    /** Deletes all rows from the books table. */
    @Query("DELETE FROM books")
    suspend fun deleteAllBooks()

    /** Deletes all rows from the tags table. */
    @Query("DELETE FROM tags")
    suspend fun deleteAllTags()

    /** Returns the total number of highlights currently stored in the database. */
    @Query("SELECT COUNT(*) FROM highlights")
    suspend fun getHighlightCount(): Int
}

/**
 * Flat query result combining a highlight's own columns with its parent book's
 * title and author. Returned by [HighlightDao.getAllHighlightsWithBook].
 *
 * @property id Highlight ID.
 * @property text The highlighted passage.
 * @property note User note on the highlight.
 * @property bookId ID of the parent book.
 * @property highlightedAt ISO-8601 timestamp of the highlight.
 * @property excluded Whether this highlight is excluded from widget rotation.
 * @property bookTitle Title of the parent book, or `null` if the book is missing.
 * @property bookAuthor Author of the parent book, or `null` if unavailable.
 */
data class HighlightWithBookTuple(
    val id: Long,
    val text: String,
    val note: String,
    @ColumnInfo(name = "book_id") val bookId: Long,
    @ColumnInfo(name = "highlighted_at") val highlightedAt: String?,
    @ColumnInfo(name = "excluded") val excluded: Boolean,
    @ColumnInfo(name = "book_title") val bookTitle: String?,
    @ColumnInfo(name = "book_author") val bookAuthor: String?,
)

/**
 * Room database for the Readwise widget, containing highlights, books, tags,
 * and the junction table linking highlights to tags.
 *
 * Access the singleton instance via [getInstance] to ensure only one connection
 * is open at a time. Schema migrations fall back to a destructive rebuild so
 * that a fresh sync can repopulate the database.
 */
@Database(
    entities = [
        HighlightEntity::class,
        BookEntity::class,
        TagEntity::class,
        HighlightTagCrossRef::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    /** Returns the DAO used to interact with all tables in this database. */
    abstract fun highlightDao(): HighlightDao

    companion object {
        // Volatile ensures the value is always read from main memory, not a CPU cache
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns the singleton [AppDatabase] instance, creating it if necessary.
         *
         * Uses double-checked locking to avoid redundant synchronisation after
         * the instance has been initialised.
         *
         * @param context Used to locate the database file; the application context
         *   is used internally to avoid leaking Activity contexts.
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "readwise_widget.db",
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
