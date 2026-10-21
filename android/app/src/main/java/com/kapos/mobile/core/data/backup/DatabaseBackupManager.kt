package com.kapos.mobile.core.data.backup

import android.content.Context
import com.kapos.mobile.core.data.local.AppDatabase
import com.kapos.mobile.core.data.local.entity.CashMovementEntity
import com.kapos.mobile.core.data.local.entity.CategoryEntity
import com.kapos.mobile.core.data.local.entity.CustomerEntity
import com.kapos.mobile.core.data.local.entity.OrderEntity
import com.kapos.mobile.core.data.local.entity.OrderItemEntity
import com.kapos.mobile.core.data.local.entity.ProductEntity
import com.kapos.mobile.core.data.local.entity.RawMaterialEntity
import com.kapos.mobile.core.data.local.entity.RecipeItemEntity
import com.kapos.mobile.core.data.local.entity.ShiftEntity
import com.kapos.mobile.core.data.local.entity.StockMovementEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DatabaseBackupManager(private val database: AppDatabase) {

    suspend fun exportToJson(context: Context): File = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("app", "KA POS Mobile")
        val timestamp = System.currentTimeMillis()
        root.put("exportedAt", timestamp)

        // 1. Categories
        val categories = database.categoryDao().getAll()
        val catArray = JSONArray()
        categories.forEach { c ->
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("name", c.name)
            obj.put("emoji", c.emoji)
            obj.put("color", c.color)
            obj.put("sortOrder", c.sortOrder)
            obj.put("isActive", c.isActive)
            catArray.put(obj)
        }
        root.put("categories", catArray)

        // 2. Products
        val products = database.productDao().getAll()
        val prodArray = JSONArray()
        products.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("categoryId", p.categoryId ?: "")
            obj.put("name", p.name)
            obj.put("price", p.price)
            obj.put("costPrice", p.costPrice)
            obj.put("emoji", p.emoji)
            obj.put("spiceLevel", p.spiceLevel)
            obj.put("description", p.description)
            obj.put("isActive", p.isActive)
            prodArray.put(obj)
        }
        root.put("products", prodArray)

        // 3. Customers
        val customers = database.customerDao().getAll()
        val custArray = JSONArray()
        customers.forEach { cust ->
            val obj = JSONObject()
            obj.put("id", cust.id)
            obj.put("name", cust.name)
            obj.put("phone", cust.phone)
            obj.put("points", cust.points)
            obj.put("totalSpend", cust.totalSpend)
            obj.put("orderCount", cust.orderCount)
            obj.put("createdAt", cust.createdAt)
            custArray.put(obj)
        }
        root.put("customers", custArray)

        // 4. Raw Materials & Recipes
        val rawMaterials = database.rawMaterialDao().getAll()
        val rawArray = JSONArray()
        rawMaterials.forEach { r ->
            val obj = JSONObject()
            obj.put("id", r.id)
            obj.put("name", r.name)
            obj.put("category", r.category)
            obj.put("unit", r.unit)
            obj.put("currentStock", r.currentStock)
            obj.put("minStock", r.minStock)
            obj.put("costPerUnit", r.costPerUnit)
            obj.put("lastRestockedAt", r.lastRestockedAt)
            rawArray.put(obj)
        }
        root.put("rawMaterials", rawArray)

        val recipes = database.recipeDao().getAll()
        val recipeArray = JSONArray()
        recipes.forEach { rc ->
            val obj = JSONObject()
            obj.put("id", rc.id)
            obj.put("productId", rc.productId)
            obj.put("materialId", rc.materialId)
            obj.put("quantityRequired", rc.quantityRequired)
            recipeArray.put(obj)
        }
        root.put("recipeItems", recipeArray)

        // 5. Orders & Order Items
        val orders = database.orderDao().getAll()
        val orderArray = JSONArray()
        orders.forEach { o ->
            val obj = JSONObject()
            obj.put("id", o.id)
            obj.put("orderNo", o.orderNo)
            obj.put("customerName", o.customerName ?: "")
            obj.put("customerPhone", o.customerPhone ?: "")
            obj.put("orderType", o.orderType)
            obj.put("tableNo", o.tableNo ?: "")
            obj.put("subtotal", o.subtotal)
            obj.put("discount", o.discount)
            obj.put("total", o.total)
            obj.put("paymentMethod", o.paymentMethod)
            obj.put("paymentStatus", o.paymentStatus)
            obj.put("cashierName", o.cashierName)
            obj.put("notes", o.notes ?: "")
            obj.put("createdAt", o.createdAt)
            obj.put("paidAt", o.paidAt ?: 0L)
            obj.put("syncStatus", o.syncStatus)
            obj.put("voidReason", o.voidReason ?: "")
            obj.put("voidedAt", o.voidedAt ?: 0L)
            orderArray.put(obj)
        }
        root.put("orders", orderArray)

        val orderItems = database.orderDao().getAllOrderItems()
        val orderItemArray = JSONArray()
        orderItems.forEach { oi ->
            val obj = JSONObject()
            obj.put("id", oi.id)
            obj.put("orderId", oi.orderId)
            obj.put("productId", oi.productId)
            obj.put("productName", oi.productName)
            obj.put("price", oi.price)
            obj.put("quantity", oi.quantity)
            obj.put("subtotal", oi.subtotal)
            obj.put("spiceLevel", oi.spiceLevel)
            obj.put("notes", oi.notes)
            orderItemArray.put(obj)
        }
        root.put("orderItems", orderItemArray)

        // Write file
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date(timestamp))
        val backupDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "backups")
        if (!backupDir.exists()) backupDir.mkdirs()
        val file = File(backupDir, "ka_pos_backup_$dateStr.json")
        file.writeText(root.toString(2))
        file
    }

    suspend fun importFromJson(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            var count = 0

            // 1. Categories
            if (root.has("categories")) {
                val array = root.getJSONArray("categories")
                val list = mutableListOf<CategoryEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        CategoryEntity(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            emoji = obj.optString("emoji", "🍽️"),
                            color = obj.optString("color", "#0B5C73"),
                            sortOrder = obj.optInt("sortOrder", 0),
                            isActive = obj.optBoolean("isActive", true)
                        )
                    )
                }
                database.categoryDao().upsertAll(list)
                count += list.size
            }

            // 2. Products
            if (root.has("products")) {
                val array = root.getJSONArray("products")
                val list = mutableListOf<ProductEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        ProductEntity(
                            id = obj.getString("id"),
                            categoryId = obj.optString("categoryId").takeIf { it.isNotBlank() },
                            name = obj.getString("name"),
                            price = obj.getDouble("price"),
                            costPrice = obj.optDouble("costPrice", 0.0),
                            emoji = obj.optString("emoji", "🍢"),
                            spiceLevel = obj.optString("spiceLevel", ""),
                            description = obj.optString("description", ""),
                            isActive = obj.optBoolean("isActive", true)
                        )
                    )
                }
                database.productDao().upsertAll(list)
                count += list.size
            }

            // 3. Customers
            if (root.has("customers")) {
                val array = root.getJSONArray("customers")
                val list = mutableListOf<CustomerEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        CustomerEntity(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            phone = obj.getString("phone"),
                            points = obj.optInt("points", 0),
                            totalSpend = obj.optDouble("totalSpend", 0.0),
                            orderCount = obj.optInt("orderCount", 0),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                database.customerDao().upsertAll(list)
                count += list.size
            }

            // 4. Raw Materials
            if (root.has("rawMaterials")) {
                val array = root.getJSONArray("rawMaterials")
                val list = mutableListOf<RawMaterialEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        RawMaterialEntity(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            category = obj.getString("category"),
                            unit = obj.getString("unit"),
                            currentStock = obj.getDouble("currentStock"),
                            minStock = obj.getDouble("minStock"),
                            costPerUnit = obj.optDouble("costPerUnit", 0.0),
                            lastRestockedAt = obj.optLong("lastRestockedAt", System.currentTimeMillis())
                        )
                    )
                }
                database.rawMaterialDao().upsertAll(list)
                count += list.size
            }

            // 5. Recipe Items
            if (root.has("recipeItems")) {
                val array = root.getJSONArray("recipeItems")
                val list = mutableListOf<RecipeItemEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        RecipeItemEntity(
                            id = obj.getString("id"),
                            productId = obj.getString("productId"),
                            materialId = obj.getString("materialId"),
                            quantityRequired = obj.getDouble("quantityRequired")
                        )
                    )
                }
                database.recipeDao().upsertAll(list)
                count += list.size
            }

            // 6. Orders
            if (root.has("orders")) {
                val array = root.getJSONArray("orders")
                val list = mutableListOf<OrderEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        OrderEntity(
                            id = obj.getString("id"),
                            orderNo = obj.getString("orderNo"),
                            customerName = obj.optString("customerName").takeIf { it.isNotBlank() },
                            customerPhone = obj.optString("customerPhone").takeIf { it.isNotBlank() },
                            orderType = obj.optString("orderType", "DINE_IN"),
                            tableNo = obj.optString("tableNo").takeIf { it.isNotBlank() },
                            subtotal = obj.getDouble("subtotal"),
                            discount = obj.optDouble("discount", 0.0),
                            total = obj.getDouble("total"),
                            paymentMethod = obj.optString("paymentMethod", "CASH"),
                            paymentStatus = obj.optString("paymentStatus", "PAID"),
                            cashierName = obj.optString("cashierName", "Kasir"),
                            notes = obj.optString("notes").takeIf { it.isNotBlank() },
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            paidAt = obj.optLong("paidAt").takeIf { it > 0 },
                            syncStatus = obj.optString("syncStatus", "SYNCED"),
                            voidReason = obj.optString("voidReason").takeIf { it.isNotBlank() },
                            voidedAt = obj.optLong("voidedAt").takeIf { it > 0 }
                        )
                    )
                }
                database.orderDao().upsertAll(list)
                count += list.size
            }

            // 7. Order Items
            if (root.has("orderItems")) {
                val array = root.getJSONArray("orderItems")
                val list = mutableListOf<OrderItemEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        OrderItemEntity(
                            id = obj.getString("id"),
                            orderId = obj.getString("orderId"),
                            productId = obj.getString("productId"),
                            productName = obj.getString("productName"),
                            price = obj.getDouble("price"),
                            quantity = obj.getInt("quantity"),
                            subtotal = obj.getDouble("subtotal"),
                            spiceLevel = obj.optString("spiceLevel", ""),
                            notes = obj.optString("notes", "")
                        )
                    )
                }
                database.orderDao().insertOrderItems(list)
                count += list.size
            }

            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
