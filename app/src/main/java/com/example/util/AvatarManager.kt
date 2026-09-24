package com.example.util

import android.accounts.AccountManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale

data class GoogleAccountItem(
    val email: String,
    val displayName: String,
    val avatarUrl: String
)

data class AvatarTransform(
    val scale: Float = 1.0f,
    val panXRatio: Float = 0f,
    val panYRatio: Float = 0f
)

object AvatarManager {
    const val APP_ICON_AVATAR = "app_icon"
    const val MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024L // 5 MB
    const val MAX_GIF_SIZE_BYTES = 20 * 1024 * 1024L   // 20 MB
    const val MAX_VIDEO_SIZE_BYTES = 25 * 1024 * 1024L // 25 MB
    const val MAX_VIDEO_DURATION_MS = 5500L            // 5.5 seconds (up to 5s with container margin)

    fun getCleanAvatarUrl(avatarUrl: String?): String {
        if (avatarUrl.isNullOrBlank()) return ""
        return avatarUrl.substringBefore("?").substringBefore("#")
    }

    fun isVideoAvatar(avatarUrl: String?): Boolean {
        if (avatarUrl.isNullOrBlank()) return false
        val clean = getCleanAvatarUrl(avatarUrl).lowercase()
        return clean.endsWith(".mp4") || clean.endsWith(".webm") ||
                clean.endsWith(".mkv") || clean.endsWith(".mov") ||
                clean.endsWith(".3gp")
    }

    fun isGifAvatar(avatarUrl: String?): Boolean {
        if (avatarUrl.isNullOrBlank()) return false
        val clean = getCleanAvatarUrl(avatarUrl).lowercase()
        return clean.endsWith(".gif")
    }

    fun parseTransform(avatarUrl: String?): AvatarTransform {
        if (avatarUrl.isNullOrBlank() || !avatarUrl.contains("?")) {
            return AvatarTransform()
        }
        return try {
            val query = avatarUrl.substringAfter("?")
            val params = query.split("&").associate {
                val parts = it.split("=")
                parts[0] to (parts.getOrNull(1) ?: "")
            }
            val scale = params["scale"]?.toFloatOrNull() ?: 1.0f
            val panX = params["x"]?.toFloatOrNull() ?: 0f
            val panY = params["y"]?.toFloatOrNull() ?: 0f
            AvatarTransform(
                scale = scale.coerceIn(0.5f, 5.0f),
                panXRatio = panX.coerceIn(-1.5f, 1.5f),
                panYRatio = panY.coerceIn(-1.5f, 1.5f)
            )
        } catch (_: Exception) {
            AvatarTransform()
        }
    }

    fun buildAvatarUrlWithTransform(basePath: String, transform: AvatarTransform): String {
        val clean = getCleanAvatarUrl(basePath)
        if (transform.scale == 1.0f && transform.panXRatio == 0f && transform.panYRatio == 0f) {
            return clean
        }
        return String.format(
            Locale.US,
            "%s?scale=%.3f&x=%.3f&y=%.3f",
            clean,
            transform.scale,
            transform.panXRatio,
            transform.panYRatio
        )
    }

    suspend fun cropImageToSquare(
        context: Context,
        imagePath: String,
        scale: Float,
        panXRatio: Float,
        panYRatio: Float
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanPath = getCleanAvatarUrl(imagePath).removePrefix("file://")
            val file = File(cleanPath)
            if (!file.exists()) {
                return@withContext Result.failure(Exception("Файл не существует"))
            }

            // If it's a video or gif, we preserve the animated container and attach transform params
            if (isVideoAvatar(cleanPath) || isGifAvatar(cleanPath)) {
                val urlWithTransform = buildAvatarUrlWithTransform(
                    cleanPath,
                    AvatarTransform(scale, panXRatio, panYRatio)
                )
                return@withContext Result.success(urlWithTransform)
            }

            // Decode image bounds first
            val boundsOnly = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(cleanPath, boundsOnly)
            val origW = boundsOnly.outWidth
            val origH = boundsOnly.outHeight
            if (origW <= 0 || origH <= 0) {
                // If decoding bounds fails, fallback to transform query
                val fallbackUrl = buildAvatarUrlWithTransform(
                    cleanPath,
                    AvatarTransform(scale, panXRatio, panYRatio)
                )
                return@withContext Result.success(fallbackUrl)
            }

            val targetSize = 512
            val sampleSize = maxOf(1, minOf(origW, origH) / (targetSize * 2))
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val sourceBmp = BitmapFactory.decodeFile(cleanPath, decodeOpts)
                ?: return@withContext Result.failure(Exception("Не удалось декодировать изображение"))

            val w = sourceBmp.width.toFloat()
            val h = sourceBmp.height.toFloat()
            val aspect = w / h

            val outputBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(outputBitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

            val dispW = if (aspect >= 1f) targetSize * aspect else targetSize.toFloat()
            val dispH = if (aspect >= 1f) targetSize.toFloat() else targetSize / aspect
            val baseScaleX = dispW / w
            val baseScaleY = dispH / h

            val matrix = Matrix()
            matrix.postScale(baseScaleX, baseScaleY)
            matrix.postTranslate((targetSize - dispW) / 2f, (targetSize - dispH) / 2f)
            matrix.postScale(scale, scale, targetSize / 2f, targetSize / 2f)
            matrix.postTranslate(panXRatio * targetSize, panYRatio * targetSize)

            canvas.drawBitmap(sourceBmp, matrix, paint)
            sourceBmp.recycle()

            val avatarsDir = File(context.filesDir, "avatars").apply {
                if (!exists()) mkdirs()
            }
            val croppedFile = File(avatarsDir, "avatar_cropped_${System.currentTimeMillis()}.jpg")
            FileOutputStream(croppedFile).use { out ->
                outputBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                out.flush()
            }
            outputBitmap.recycle()

            Result.success(croppedFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveAvatarFromUri(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri)?.lowercase() ?: ""

            var isVideo = mimeType.startsWith("video/")
            var isGif = mimeType.contains("gif")

            // Check filename for video/gif indicators
            var displayName = ""
            try {
                contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        displayName = cursor.getString(0)?.lowercase() ?: ""
                    }
                }
            } catch (_: Exception) {}

            if (displayName.endsWith(".mp4") || displayName.endsWith(".webm") ||
                displayName.endsWith(".mkv") || displayName.endsWith(".mov") ||
                displayName.endsWith(".3gp")) {
                isVideo = true
            } else if (displayName.endsWith(".gif")) {
                isGif = true
            }

            // Check file header bytes if still uncertain
            if (!isVideo && !isGif) {
                var stream: InputStream? = null
                try {
                    stream = contentResolver.openInputStream(uri)
                    if (stream != null) {
                        val headerBytes = ByteArray(16)
                        val headerRead = stream.read(headerBytes, 0, 16)
                        if (headerRead >= 3) {
                            val headerStr = String(headerBytes, 0, headerRead)
                            if (headerStr.startsWith("GIF", ignoreCase = true)) {
                                isGif = true
                            } else if (headerRead >= 8 && (headerStr.contains("ftyp") || headerStr.contains("moov") || headerStr.contains("matroska"))) {
                                isVideo = true
                            }
                        }
                    }
                } catch (_: Exception) {
                } finally {
                    try { stream?.close() } catch (_: Exception) {}
                }
            }

            // If it is video, inspect duration with MediaMetadataRetriever
            if (isVideo) {
                val retriever = MediaMetadataRetriever()
                var videoDurationMs = 0L
                try {
                    retriever.setDataSource(context, uri)
                    val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    videoDurationMs = durStr?.toLongOrNull() ?: 0L
                } catch (e: Exception) {
                    // In case metadata extraction fails on some uri providers, proceed with stream validation
                } finally {
                    try { retriever.release() } catch (_: Exception) {}
                }

                if (videoDurationMs > MAX_VIDEO_DURATION_MS) {
                    val durationSec = videoDurationMs / 1000.0
                    return@withContext Result.failure(
                        IllegalArgumentException("Длительность видео превышает 5 секунд (${String.format(Locale.US, "%.1f", durationSec)} сек). Выберите видео длительностью до 5 секунд.")
                    )
                }
            }

            // Open stream to copy and check size
            val inputStream = contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Не удалось прочитать выбранный файл"))

            val avatarsDir = File(context.filesDir, "avatars").apply {
                if (!exists()) mkdirs()
            }

            val ext = if (isVideo) "mp4" else if (isGif) "gif" else "png"
            val targetFile = File(avatarsDir, "avatar_${System.currentTimeMillis()}.$ext")

            val buffer = ByteArray(8192)
            val outputStream = FileOutputStream(targetFile)
            var totalBytes = 0L

            inputStream.use { input ->
                outputStream.use { output ->
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        totalBytes += bytesRead
                        if (isVideo && totalBytes > MAX_VIDEO_SIZE_BYTES) {
                            output.close()
                            targetFile.delete()
                            return@withContext Result.failure(
                                IllegalArgumentException("Размер видеофайла превышает лимит (${String.format(Locale.US, "%.1f", totalBytes / (1024.0 * 1024.0))} МБ / макс. 25 МБ)")
                            )
                        } else if (isGif && totalBytes > MAX_GIF_SIZE_BYTES) {
                            output.close()
                            targetFile.delete()
                            return@withContext Result.failure(
                                IllegalArgumentException("Размер GIF анимации превышает лимит в 20 МБ (${String.format(Locale.US, "%.1f", totalBytes / (1024.0 * 1024.0))} МБ)")
                            )
                        } else if (!isVideo && !isGif && totalBytes > MAX_IMAGE_SIZE_BYTES) {
                            output.close()
                            targetFile.delete()
                            return@withContext Result.failure(
                                IllegalArgumentException("Размер изображения превышает лимит в 5 МБ (${String.format(Locale.US, "%.1f", totalBytes / (1024.0 * 1024.0))} МБ)")
                            )
                        }
                        output.write(buffer, 0, bytesRead)
                    }
                    output.flush()
                }
            }

            Result.success(targetFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getDeviceGoogleAccounts(context: Context): List<GoogleAccountItem> {
        val list = mutableListOf<GoogleAccountItem>()
        val seenEmails = mutableSetOf<String>()

        // Check if GET_ACCOUNTS permission is actually granted before querying system AccountManager
        val hasAccountsPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.GET_ACCOUNTS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasAccountsPermission) {
            try {
                val accountManager = AccountManager.get(context)
                val accounts = accountManager.getAccountsByType("com.google")
                for (acc in accounts) {
                    if (acc.name.isNotBlank() && seenEmails.add(acc.name.lowercase())) {
                        val rawName = acc.name.substringBefore("@")
                            .replace(".", " ")
                            .replace("_", " ")
                        val formattedName = rawName.split(" ")
                            .joinToString(" ") { part ->
                                part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                            }
                        list.add(
                            GoogleAccountItem(
                                email = acc.name,
                                displayName = formattedName.ifBlank { "Google User" },
                                avatarUrl = APP_ICON_AVATAR
                            )
                        )
                    }
                }
            } catch (_: Throwable) {
                // Ignore system resource query or security exception
            }
        }

        return list
    }

    fun getAvatarModel(avatarUrl: String?): Any {
        val cleanUrl = getCleanAvatarUrl(avatarUrl)
        if (cleanUrl.isBlank() || cleanUrl == APP_ICON_AVATAR || cleanUrl == "default" || cleanUrl.contains("picsum.photos")) {
            return R.drawable.ic_aniwerti_app_icon
        }
        if (cleanUrl.startsWith("android.resource://")) {
            return Uri.parse(cleanUrl)
        }
        if (cleanUrl.startsWith("/") || cleanUrl.startsWith("file://")) {
            val cleanPath = cleanUrl.removePrefix("file://")
            val file = File(cleanPath)
            if (file.exists()) return file
        }
        return cleanUrl
    }
}
