package com.readwise.widget.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.readwise.widget.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Thrown when the Readwise API rejects the request due to an invalid or expired token.
 */
class ReadwiseAuthException(message: String) : Exception(message)

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
 * - Returns HTTP 401/403 responses as [ReadwiseAuthException] for clear error reporting.
 * - Logs request/response metadata at BASIC level in debug builds only.
 * - Deserializes JSON with `ignoreUnknownKeys = true` so new API fields
 *   don't break parsing.
 *
 * @param token Readwise API access token obtained from the user's account settings.
 * @return A ready-to-use [ReadwiseApi] targeting `https://readwise.io/api/v2/`.
 */
fun createReadwiseApi(token: String): ReadwiseApi {
    // Allow unknown JSON keys so future API additions don't cause parse errors
    val json = Json { ignoreUnknownKeys = true }

    val clientBuilder = OkHttpClient.Builder()
        .addInterceptor { chain ->
            // Attach the API token to every outgoing request
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Token $token")
                .build()
            val response = chain.proceed(request)
            // Surface auth failures as a typed exception
            if (response.code == 401 || response.code == 403) {
                response.close()
                throw ReadwiseAuthException(
                    "Invalid or expired API token (HTTP ${response.code})"
                )
            }
            response
        }

    // Only log HTTP traffic in debug builds to avoid leaking URLs in production
    if (BuildConfig.DEBUG) {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        clientBuilder.addInterceptor(loggingInterceptor)
    }

    val client = clientBuilder.build()
    val contentType = "application/json".toMediaType()

    val retrofit = Retrofit.Builder()
        .baseUrl("https://readwise.io/api/v2/")
        .client(client)
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()

    return retrofit.create(ReadwiseApi::class.java)
}
