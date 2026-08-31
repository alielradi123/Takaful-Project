package com.example.takaful.utils

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * مساعد لرفع الصور إلى Supabase Storage عبر REST API المباشر.
 * لا يحتاج SDK إضافي — يعمل باستخدام HttpURLConnection المدمج في Android.
 *
 * 🔧 الإعداد المطلوب:
 *   غيّر قيمة SUPABASE_URL و SUPABASE_ANON_KEY أدناه ببيانات مشروعك من:
 *   Supabase Dashboard → Settings → API
 */
object SupabaseStorageHelper {

    // ─── اضبط هذه القيم من لوحة Supabase ───────────────────────────────────
    private const val SUPABASE_URL      = "https://akvcfzbhyjwmpvbchsrl.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFrdmNmemJoeWp3bXB2YmNoc3JsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODI5MDY3NDgsImV4cCI6MjA5ODQ4Mjc0OH0.QOvfSzcvjSS_S7ROfHQi_XPDkPJ7LkCoEV7QmK8dN7k"
    private const val STORAGE_BUCKET    = "takaful-media"
    // ────────────────────────────────────────────────────────────────────────

    /**
     * يرفع صورة من Uri إلى Supabase Storage.
     *
     * @param context   context التطبيق لقراءة الملف
     * @param uri       Uri الصورة المحلية
     * @param path      المسار داخل الـ Bucket (مثال: profile_pictures/uid.jpg)
     * @param onSuccess يُستدعى مع الرابط العام للصورة عند النجاح
     * @param onFailure يُستدعى مع الاستثناء عند الفشل
     */
    fun uploadImage(
        context: Context,
        uri: Uri,
        path: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
        // نستخدم Thread لتجنب تعقيدات coroutines في الاستدعاء من callback
        Thread {
            try {
                val publicUrl = uploadSync(context, uri, path)
                mainHandler.post { onSuccess(publicUrl) }
            } catch (e: Exception) {
                mainHandler.post { onFailure(e) }
            }
        }.start()
    }

    /**
     * نسخة Coroutine من uploadImage (مناسبة للاستخدام من suspend functions).
     */
    suspend fun uploadImageSuspend(
        context: Context,
        uri: Uri,
        path: String
    ): String = withContext(Dispatchers.IO) {
        uploadSync(context, uri, path)
    }

    // ── التنفيذ الداخلي ───────────────────────────────────────────────────
    private fun uploadSync(context: Context, uri: Uri, path: String): String {
        // اقرأ بايتات الصورة من الـ Uri
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IOException("Could not open input stream for URI: $uri")

        // حدد Content-Type بناءً على نوع الملف
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"

        // أنشئ الرابط وارفع الملف
        val uploadUrl = "$SUPABASE_URL/storage/v1/object/$STORAGE_BUCKET/$path"
        val conn = (URL(uploadUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
            setRequestProperty("Content-Type", mimeType)
            setRequestProperty("x-upsert", "true")   // حدّث إذا كان موجوداً
            setRequestProperty("Content-Length", bytes.size.toString())
        }

        conn.outputStream.use { it.write(bytes) }

        val responseCode = conn.responseCode
        if (responseCode !in 200..299) {
            val errorBody = conn.errorStream?.use { it.bufferedReader().readText() } ?: "Unknown error"
            throw IOException("Supabase upload failed [$responseCode]: $errorBody")
        }

        conn.disconnect()

        // ارجع الرابط العام للصورة
        return "$SUPABASE_URL/storage/v1/object/public/$STORAGE_BUCKET/$path"
    }

    /**
     * حفظ الصورة محلياً في مجلد التطبيق (fallback للعرض بدون إنترنت).
     * نفس وظيفة FirebaseStorageHelper.saveImageLocally().
     */
    fun saveImageLocally(context: Context, uri: Uri, userId: String): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = java.io.File(context.filesDir, "profile_picture_$userId.jpg")
            val outputStream = java.io.FileOutputStream(file)
            inputStream?.use { input -> outputStream.use { output -> input.copyTo(output) } }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

