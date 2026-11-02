package com.kapos.mobile.core.data.remote

import com.kapos.mobile.core.data.remote.dto.SupabaseCategoryDto
import com.kapos.mobile.core.data.remote.dto.SupabaseOrderItemPayload
import com.kapos.mobile.core.data.remote.dto.SupabaseOrderPayload
import com.kapos.mobile.core.data.remote.dto.SupabaseProductDto
import com.kapos.mobile.core.data.remote.dto.SupabaseShiftPayload
import com.kapos.mobile.core.data.remote.dto.SupabaseModifierDto
import com.kapos.mobile.core.data.remote.dto.SupabaseRawMaterialDto
import com.kapos.mobile.core.data.remote.dto.SupabaseMaterialBatchDto
import com.kapos.mobile.core.data.remote.dto.SupabaseRecipeDto
import com.kapos.mobile.core.data.remote.dto.SupabaseRecipeItemDto
import com.kapos.mobile.core.data.remote.dto.SupabaseCustomerDto
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

    suspend fun deleteProduct(id: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured() || id.isBlank()) return@withContext false
        try {
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/products?id=eq.$id")
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteCategory(id: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured() || id.isBlank()) return@withContext false
        try {
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/categories?id=eq.$id")
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteModifier(id: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured() || id.isBlank()) return@withContext false
        try {
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/modifiers?id=eq.$id")
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun fetchModifiers(): List<SupabaseModifierDto> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext emptyList()
        try {
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/modifiers?is_active=eq.true&order=sort_order.asc")
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                json.decodeFromString<List<SupabaseModifierDto>>(body)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun upsertModifiers(modifiers: List<SupabaseModifierDto>): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured() || modifiers.isEmpty()) return@withContext false
        try {
            val jsonBody = json.encodeToString(modifiers)
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/modifiers")
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

    // --- RAW MATERIALS ---
    suspend fun fetchRawMaterials(): List<SupabaseRawMaterialDto> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext emptyList()
        try {
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/raw_materials?is_active=eq.true&order=name.asc")
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                json.decodeFromString<List<SupabaseRawMaterialDto>>(body)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun upsertRawMaterials(materials: List<SupabaseRawMaterialDto>): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured() || materials.isEmpty()) return@withContext false
        try {
            val jsonBody = json.encodeToString(materials)
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/raw_materials")
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

    suspend fun deleteRawMaterial(id: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext false
        try {
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/raw_materials?id=eq.$id")
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    // --- MATERIAL BATCHES ---
    suspend fun fetchMaterialBatches(): List<SupabaseMaterialBatchDto> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext emptyList()
        try {
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/material_batches?order=created_at.desc")
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                json.decodeFromString<List<SupabaseMaterialBatchDto>>(body)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun upsertMaterialBatches(batches: List<SupabaseMaterialBatchDto>): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured() || batches.isEmpty()) return@withContext false
        try {
            val jsonBody = json.encodeToString(batches)
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/material_batches")
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

    suspend fun deleteMaterialBatch(id: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext false
        try {
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/material_batches?id=eq.$id")
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    // --- RECIPES & RECIPE ITEMS ---
    suspend fun fetchRecipes(): List<SupabaseRecipeDto> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext emptyList()
        try {
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/recipes?is_active=eq.true")
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                json.decodeFromString<List<SupabaseRecipeDto>>(body)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun upsertRecipes(recipes: List<SupabaseRecipeDto>): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured() || recipes.isEmpty()) return@withContext false
        try {
            val jsonBody = json.encodeToString(recipes)
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/recipes")
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

    suspend fun fetchRecipeItems(): List<SupabaseRecipeItemDto> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext emptyList()
        try {
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/recipe_items")
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                json.decodeFromString<List<SupabaseRecipeItemDto>>(body)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun upsertRecipeItems(items: List<SupabaseRecipeItemDto>): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured() || items.isEmpty()) return@withContext false
        try {
            val jsonBody = json.encodeToString(items)
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/recipe_items")
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

    suspend fun deleteRecipeItemsByRecipe(recipeId: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext false
        try {
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/recipe_items?recipe_id=eq.$recipeId")
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    // --- CUSTOMERS ---
    suspend fun fetchCustomers(): List<SupabaseCustomerDto> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext emptyList()
        try {
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/customers?order=name.asc")
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                json.decodeFromString<List<SupabaseCustomerDto>>(body)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun upsertCustomers(customers: List<SupabaseCustomerDto>): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured() || customers.isEmpty()) return@withContext false
        try {
            val jsonBody = json.encodeToString(customers)
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/customers")
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

    suspend fun deleteCustomer(id: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext false
        try {
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/customers?id=eq.$id")
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun clearAllRemoteData(): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext false
        try {
            val tables = listOf(
                "order_items", "orders", "cash_movements", "shifts",
                "material_batches", "recipe_items", "recipes", "raw_materials",
                "customers", "modifiers", "products", "categories"
            )
            for (table in tables) {
                val request = Request.Builder()
                    .url("$baseUrl/rest/v1/$table?id=not.is.null")
                    .addHeader("apikey", apiKey)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .delete()
                    .build()
                client.newCall(request).execute().close()
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}

