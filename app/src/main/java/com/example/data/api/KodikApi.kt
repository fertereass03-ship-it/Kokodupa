package com.example.data.api

import com.example.data.api.models.KodikGetPlayerResponse
import com.example.data.api.models.KodikSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface KodikApi {
    @GET("get-player")
    suspend fun getPlayerByShikimori(
        @Query("token") token: String = KodikService.KODIK_PUBLIC_TOKEN,
        @Query("shikimoriID") shikimoriId: String
    ): KodikGetPlayerResponse

    @GET("get-player")
    suspend fun getPlayerByTitle(
        @Query("token") token: String = KodikService.KODIK_PUBLIC_TOKEN,
        @Query("title") title: String
    ): KodikGetPlayerResponse

    @GET("search")
    suspend fun searchByShikimori(
        @Query("token") token: String = KodikService.KODIK_PUBLIC_TOKEN,
        @Query("shikimori_id") shikimoriId: Long
    ): KodikSearchResponse

    @GET("search")
    suspend fun searchByTitle(
        @Query("token") token: String = KodikService.KODIK_PUBLIC_TOKEN,
        @Query("title") title: String
    ): KodikSearchResponse
}

