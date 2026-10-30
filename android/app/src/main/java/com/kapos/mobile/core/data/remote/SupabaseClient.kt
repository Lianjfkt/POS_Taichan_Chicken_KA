package com.kapos.mobile.core.data.remote

import com.kapos.mobile.core.data.remote.dto.SupabaseCategoryDto
import com.kapos.mobile.core.data.remote.dto.SupabaseOrderItemPayload
import com.kapos.mobile.core.data.remote.dto.SupabaseOrderPayload
import com.kapos.mobile.core.data.remote.dto.SupabaseProductDto
import com.kapos.mobile.core.data.remote.dto.SupabaseShiftPayload
import com.kapos.mobile.core.data.remote.dto.SupabaseCashMovementPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class SupabaseClient(
    private var baseUrl: String,
    private var apiKey: String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    fun updateConfig(newBaseUrl: String, newApiKey: String) {
        this.baseUrl = newBaseUrl.trim().removeSuffix("/")
        this.apiKey = newApiKey.trim()
    }

    fun isConfigured(): Boolean {
        return baseUrl.isNotBlank() && apiKey.isNotBlank()
    }

    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext false
        try {
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/categories?select=id&limit=1")
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun fetchCategories(): List<SupabaseCategoryDto> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext emptyList()
        val request = Request.Builder()
            .url("$baseUrl/rest/v1/categories?is_active=eq.true&order=sort_order.asc")
            .addHeader("apikey", apiKey)
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext emptyList()
            val body = response.body?.string() ?: return@withContext emptyList()
            json.decodeFromString<List<SupabaseCategoryDto>>(body)
        }
    }

    suspend fun fetchProducts(): List<SupabaseProductDto> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext emptyList()
        val request = Request.Builder()
            .url("$baseUrl/rest/v1/products?is_active=eq.true&order=name.asc")
            .addHeader("apikey", apiKey)
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext emptyList()
            val body = response.body?.string() ?: return@withContext emptyList()
            json.decodeFromString<List<SupabaseProductDto>>(body)
        }
    }

    suspend fun postOrder(order: SupabaseOrderPayload): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext false
        try {
            val jsonBody = json.encodeToString(listOf(order))
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/orders")
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun postOrderItems(items: List<SupabaseOrderItemPayload>): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured() || items.isEmpty()) return@withContext false
        try {
            val jsonBody = json.encodeToString(items)
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/order_items")
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun upsertCategories(categories: List<SupabaseCategoryDto>): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured() || categories.isEmpty()) return@withContext false
        try {
            val jsonBody = json.encodeToString(categories)
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/categories")
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun upsertProducts(products: List<SupabaseProductDto>): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured() || products.isEmpty()) return@withContext false
        try {
            val jsonBody = json.encodeToString(products)
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/products")
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun fetchOrders(limit: Int = 200): List<SupabaseOrderPayload> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext emptyList()
        try {
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/orders?order=created_at.desc&limit=$limit")
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                json.decodeFromString<List<SupabaseOrderPayload>>(body)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun fetchOrderItems(orderIds: List<String>): List<SupabaseOrderItemPayload> = withContext(Dispatchers.IO) {
        if (!isConfigured() || orderIds.isEmpty()) return@withContext emptyList()
        try {
            val idsFilter = orderIds.joinToString(",")
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/order_items?order_id=in.($idsFilter)")
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                json.decodeFromString<List<SupabaseOrderItemPayload>>(body)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun postShifts(shifts: List<SupabaseShiftPayload>): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured() || shifts.isEmpty()) return@withContext false
        try {
            val jsonBody = json.encodeToString(shifts)
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/shifts")
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun postCashMovements(movements: List<SupabaseCashMovementPayload>): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured() || movements.isEmpty()) return@withContext false
        try {
            val jsonBody = json.encodeToString(movements)
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/cash_movements")
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }
}
