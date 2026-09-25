package com.example

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import android.util.Log
import com.example.data.db.AnimeDatabase
import com.example.data.realtime.RealtimeSocialManager
import com.example.data.session.UserSessionManager
import com.example.data.settings.AppSettingsManager

class AniWertiApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        try {
            AppSettingsManager.init(this)
            UserSessionManager.getInstance(this)
            AnimeDatabase.getDatabase(this)
            RealtimeSocialManager.getInstance(this)
        } catch (e: Throwable) {
            Log.e("AniWertiApp", "Warm-up init exception", e)
        }
    }

    override fun newImageLoader(): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val host = originalRequest.url.host
                val requestBuilder = originalRequest.newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                if (host.contains("shikimori")) {
                    requestBuilder.header("Referer", "https://shikimori.io/")
                }
                if (host.contains("yani.tv")) {
                    requestBuilder.header("Referer", "https://yani.tv/")
                    requestBuilder.header("User-Agent", "AniWerti/1.0")
                }
                if (host.contains("anilist.co")) {
                    requestBuilder.header("Referer", "https://anilist.co/")
                }
                if (host.contains("myanimelist.net")) {
                    requestBuilder.header("Referer", "https://myanimelist.net/")
                }
                chain.proceed(requestBuilder.build())
            }
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100 * 1024 * 1024) // 100 MB
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
