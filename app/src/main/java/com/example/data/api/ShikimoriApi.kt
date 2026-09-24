package com.example.data.api

import com.example.data.api.models.ShikimoriAnimeDetailDto
import com.example.data.api.models.ShikimoriAnimeDto
import com.example.data.api.models.ShikimoriCalendarDto
import com.example.data.api.models.ShikimoriFranchiseDto
import com.example.data.api.models.ShikimoriGenreDto
import com.example.data.api.models.ShikimoriRelatedDto
import com.example.data.api.models.ShikimoriScreenshotDto
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

interface ShikimoriApi {

    @Headers("User-Agent: ANIWERTI-Android-App/1.0")
    @GET("api/animes")
    suspend fun getAnimes(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("order") order: String? = null,
        @Query("kind") kind: String? = null,
        @Query("status") status: String? = null,
        @Query("season") season: String? = null,
        @Query("score") score: Int? = null,
        @Query("genre") genre: String? = null,
        @Query("search") search: String? = null
    ): List<ShikimoriAnimeDto>

    @Headers("User-Agent: Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 ANIWERTI-App/1.0")
    @GET("api/animes/{id}")
    suspend fun getAnimeDetails(
        @Path("id") id: Long
    ): ShikimoriAnimeDetailDto

    @Headers("User-Agent: Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 ANIWERTI-App/1.0")
    @GET("api/animes/{id}/screenshots")
    suspend fun getAnimeScreenshots(
        @Path("id") id: Long
    ): List<ShikimoriScreenshotDto>

    @Headers("User-Agent: Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 ANIWERTI-App/1.0")
    @GET("api/animes/{id}/related")
    suspend fun getAnimeRelated(
        @Path("id") id: Long
    ): List<ShikimoriRelatedDto>

    @Headers("User-Agent: Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 ANIWERTI-App/1.0")
    @GET("api/animes/{id}/franchise")
    suspend fun getAnimeFranchise(
        @Path("id") id: Long
    ): ShikimoriFranchiseDto

    @Headers("User-Agent: Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 ANIWERTI-App/1.0")
    @GET("api/animes/{id}/similar")
    suspend fun getAnimeSimilar(
        @Path("id") id: Long
    ): List<ShikimoriAnimeDto>

    @Headers("User-Agent: Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 ANIWERTI-App/1.0")
    @GET("api/genres")
    suspend fun getGenres(): List<ShikimoriGenreDto>

    @Headers("User-Agent: Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 ANIWERTI-App/1.0")
    @GET("api/calendar")
    suspend fun getCalendar(): List<ShikimoriCalendarDto>
}
