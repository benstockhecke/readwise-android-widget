package com.readwise.widget.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for the Readwise v2 REST API.
 *
 * All functions are suspending and must be called from a coroutine. Authentication
 * is injected at the HTTP client level — callers do not need to pass the token
 * manually to each function.
 */
interface ReadwiseApi {

    /**
     * Fetches a paginated list of the authenticated user's highlights.
     *
     * @param page 1-based page number to retrieve.
     * @param pageSize Number of results per page (max 1000 per Readwise docs).
     * @param bookId If provided, restricts results to highlights from this book.
     * @return A [HighlightResult] containing the page of highlights and pagination info.
     */
    @GET("highlights/")
    suspend fun getHighlights(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 100,
        @Query("book_id") bookId: Long? = null,
    ): HighlightResult

    /**
     * Fetches a paginated list of the authenticated user's books (source documents).
     *
     * @param page 1-based page number to retrieve.
     * @param pageSize Number of results per page.
     * @param category If provided, restricts results to this content category
     *   (e.g. "books", "articles", "tweets").
     * @return A [BookResult] containing the page of books and pagination info.
     */
    @GET("books/")
    suspend fun getBooks(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 100,
        @Query("category") category: String? = null,
    ): BookResult
}

/**
 * Constructs a fully configured [ReadwiseApi] instance backed by Retrofit.
 *
 * The returned client:
 * - Authenticates every request with the provided [token] via a Bearer-style
 *   `Authorization: Token <token>` header.
 * - Logs request/response metadata at BASIC level for debugging.
 * - Deserializes JSON with `ignoreUnknownKeys = true` so new API fields
 *   don't break parsing.
 *
 * @param token Readwise API access token obtained from the user's account settings.
 * @return A ready-to-use [ReadwiseApi] targeting `https://readwise.io/api/v2/`.
 */
fun createReadwiseApi(token: String): ReadwiseApi {
    // Allow unknown JSON keys so future API additions don't cause parse errors
    val json = Json { ignoreUnknownKeys = true }

    // Log HTTP method, URL, and response code (no headers or body)
    val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            // Attach the API token to every outgoing request
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Token $token")
                .build()
            chain.proceed(request)
        }
        .addInterceptor(loggingInterceptor)
        .build()

    val contentType = "application/json".toMediaType()

    val retrofit = Retrofit.Builder()
        .baseUrl("https://readwise.io/api/v2/")
        .client(client)
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()

    return retrofit.create(ReadwiseApi::class.java)
}
