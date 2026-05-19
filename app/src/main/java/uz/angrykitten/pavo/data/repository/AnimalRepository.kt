package uz.angrykitten.pavo.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.angrykitten.pavo.data.SupabaseClientProvider
import uz.angrykitten.pavo.data.model.AppData
import uz.angrykitten.pavo.data.model.City
import uz.angrykitten.pavo.data.model.District
import uz.angrykitten.pavo.data.model.Animal
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class AnimalRepository(private val context: Context) {

    private val client = SupabaseClientProvider.client
    private var appData: AppData? = null
    private var loadedFromSupabase = false

    /** Returns cached data or an empty placeholder — never crashes. */
    private fun currentData(): AppData =
        appData ?: AppData(cities = emptyList(), districts = emptyList(), animals = emptyList())

    suspend fun refreshFromSupabase(force: Boolean = false): Result<Unit> {
        if (!force && loadedFromSupabase && appData != null) {
            return Result.success(Unit)
        }
        return try {
            val cities = client.postgrest["cities"].select().decodeList<City>().sortedBy { it.id }
            val districts = client.postgrest["districts"].select().decodeList<District>().sortedBy { it.id }
            val animals = client.postgrest["animals"].select().decodeList<Animal>()
            appData = AppData(
                cities = cities,
                districts = districts,
                animals = animals
            )
            loadedFromSupabase = true
            Result.success(Unit)
        } catch (e: Exception) {
            if (appData == null) {
                appData = AppData(cities = emptyList(), districts = emptyList(), animals = emptyList())
            }
            loadedFromSupabase = false
            Result.failure(e)
        }
    }

    fun getCities(): List<City> = currentData().cities.sortedBy { it.id }

    fun getDistricts(cityId: Int? = null): List<District> {
        val all = currentData().districts.sortedBy { it.id }
        return if (cityId != null) all.filter { it.city_id == cityId } else all
    }

    fun getAnimals(
        listingType: String? = null,
        animalType: String? = null,
        cityId: Int? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null,
        query: String? = null,
        savedIds: Set<String> = emptySet(),
        onlySaved: Boolean = false
    ): List<Animal> {
        var list = currentData().animals.filter { it.is_active }
        if (onlySaved) {
            list = list.filter { it.id in savedIds }
        }
        listingType?.let { lt -> list = list.filter { it.listing_type == lt } }
        animalType?.let { at -> list = list.filter { it.animal_type == at } }
        cityId?.let { cid -> list = list.filter { it.city_id == cid } }
        minPrice?.let { min -> list = list.filter { it.price >= min } }
        maxPrice?.let { max -> list = list.filter { it.price <= max } }
        query?.let { q ->
            if (q.isNotBlank()) {
                val lower = q.lowercase()
                list = list.filter {
                    it.title.lowercase().contains(lower) ||
                    it.breed.lowercase().contains(lower) ||
                    it.city_name.lowercase().contains(lower) ||
                    it.district_name.lowercase().contains(lower) ||
                    it.address.lowercase().contains(lower)
                }
            }
        }
        return list
    }

    fun getAnimalById(id: String): Animal? =
        currentData().animals.find { it.id == id }

    suspend fun addAnimal(animal: Animal): Result<Unit> {
        val data = currentData()
        appData = data.copy(animals = listOf(animal) + data.animals)

        return try {
            client.postgrest["animals"].upsert(animal)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAnimal(id: String): Result<Unit> {
        return try {
            client.postgrest["animals"].delete {
                filter {
                    eq("id", id)
                }
            }
            refreshFromSupabase(force = true)
        } catch (e: Exception) {
            val data = currentData()
            appData = data.copy(animals = data.animals.filter { it.id != id })
            Result.failure(e)
        }
    }

    fun getUserAnimals(userId: String): List<Animal> =
        currentData().animals.filter { it.user_id == userId }

    /**
     * Uploads an image to Supabase Storage "animals" bucket using a direct
     * HTTP POST — bypasses the supabase-kt Storage SDK which requires a valid
     * Supabase Auth JWT (the app uses Firebase Auth, not Supabase Auth, so the
     * SDK always throws "Invalid Compact JWS URL").
     *
     * Pre-requisites in Supabase Dashboard:
     *   Storage → New bucket → name: "animals" → Public: ON
     *   (The pavo_setup.sql script does this automatically via SQL.)
     */
    suspend fun uploadImage(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            // ── Read & compress ───────────────────────────────────────
            val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext Result.failure(
                    IllegalStateException("Rasm faylini o'qib bo'lmadi. Qayta urinib ko'ring.")
                )
            val bytes = compressToJpeg(rawBytes, maxBytes = 1_024 * 1_024)  // ≤ 1 MB

            // ── Build upload URL ──────────────────────────────────────
            val objectPath = "uploads/${UUID.randomUUID()}.jpg"
            val uploadUrl  = "${SupabaseClientProvider.SUPABASE_URL}/storage/v1/object/animals/$objectPath"

            // ── Direct HTTP POST ──────────────────────────────────────
            val conn = URL(uploadUrl).openConnection() as HttpURLConnection
            try {
                conn.requestMethod  = "POST"
                conn.connectTimeout = 15_000        // 15 s to connect
                conn.readTimeout    = 5 * 60_000    // 5 min for large files
                conn.doOutput       = true
                conn.setRequestProperty("Authorization", "Bearer ${SupabaseClientProvider.ANON_KEY}")
                conn.setRequestProperty("Content-Type",  "image/jpeg")
                conn.setRequestProperty("x-upsert",      "true")   // overwrite if same UUID somehow repeats
                conn.setFixedLengthStreamingMode(bytes.size)
                conn.outputStream.use { it.write(bytes) }

                val code = conn.responseCode
                if (code in 200..299) {
                    // Public URL — no JWT needed to read from a public bucket
                    val publicUrl = "${SupabaseClientProvider.SUPABASE_URL}/storage/v1/object/public/animals/$objectPath"
                    Result.success(publicUrl)
                } else {
                    val body = runCatching {
                        (conn.errorStream ?: conn.inputStream).bufferedReader().readText()
                    }.getOrDefault("")
                    val hint = when (code) {
                        401, 403 -> " (Storage → anon INSERT siyosati kerak — pavo_setup.sql ni ishga tushiring)"
                        404      -> " ('animals' bucket topilmadi — Supabase'da yarating)"
                        else     -> ""
                    }
                    Result.failure(Exception("HTTP $code$hint\n$body"))
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Result.failure(Exception("Rasm yuklashda xatolik: ${e.message?.take(200)}", e))
        }
    }

    /** Compresses a bitmap to JPEG, reducing quality until it fits [maxBytes]. */
    private fun compressToJpeg(input: ByteArray, maxBytes: Int): ByteArray {
        val bitmap = BitmapFactory.decodeByteArray(input, 0, input.size)
            ?: return input   // not a bitmap — return raw (will fail at upload if wrong type)

        // Scale down if resolution is huge (cap at 1920px on longest side)
        val maxDim = 1920
        val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
            val scale = maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)
            val w = (bitmap.width * scale).toInt()
            val h = (bitmap.height * scale).toInt()
            Bitmap.createScaledBitmap(bitmap, w, h, true).also {
                if (it !== bitmap) bitmap.recycle()
            }
        } else bitmap

        val out = ByteArrayOutputStream()
        var quality = 85
        do {
            out.reset()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            quality -= 10
        } while (out.size() > maxBytes && quality > 20)

        if (scaled !== bitmap) scaled.recycle()
        return out.toByteArray()
    }
}
