package com.kapos.mobile.core.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.kapos.mobile.domain.model.CartItem
import com.kapos.mobile.domain.model.Product
import com.kapos.mobile.domain.model.SavedOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class SavedOrderRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ka_pos_saved_orders", Context.MODE_PRIVATE)
    private val _savedOrdersFlow = MutableStateFlow<List<SavedOrder>>(loadSavedOrders())
    val savedOrdersFlow: StateFlow<List<SavedOrder>> = _savedOrdersFlow.asStateFlow()

    private fun loadSavedOrders(): List<SavedOrder> {
        val rawJson = prefs.getString("saved_orders_list", null) ?: return emptyList()
        val list = mutableListOf<SavedOrder>()
        try {
            val jsonArray = JSONArray(rawJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val itemsArray = obj.getJSONArray("items")
                val items = mutableListOf<CartItem>()
                for (j in 0 until itemsArray.length()) {
                    val itemObj = itemsArray.getJSONObject(j)
                    val prodObj = itemObj.getJSONObject("product")
                    val product = Product(
                        id = prodObj.getString("id"),
                        categoryId = if (prodObj.has("categoryId") && !prodObj.isNull("categoryId")) prodObj.getString("categoryId") else null,
                        name = prodObj.getString("name"),
                        price = prodObj.getDouble("price"),
                        costPrice = prodObj.optDouble("costPrice", 0.0),
                        emoji = prodObj.optString("emoji", "🍢"),
                        spiceLevel = prodObj.optString("spiceLevel", ""),
                        description = prodObj.optString("description", ""),
                        isActive = prodObj.optBoolean("isActive", true)
                    )
                    items.add(
                        CartItem(
                            product = product,
                            quantity = itemObj.getInt("quantity"),
                            spiceLevel = itemObj.optString("spiceLevel", "Sedang"),
                            notes = itemObj.optString("notes", "")
                        )
                    )
                }
                list.add(
                    SavedOrder(
                        id = obj.getString("id"),
                        customerName = obj.optString("customerName", "Umum"),
                        tableNo = obj.optString("tableNo", ""),
                        items = items,
                        subtotal = obj.getDouble("subtotal"),
                        createdAt = obj.getLong("createdAt")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun persistOrders(list: List<SavedOrder>) {
        try {
            val jsonArray = JSONArray()
            for (order in list) {
                val orderObj = JSONObject()
                orderObj.put("id", order.id)
                orderObj.put("customerName", order.customerName)
                orderObj.put("tableNo", order.tableNo)
                orderObj.put("subtotal", order.subtotal)
                orderObj.put("createdAt", order.createdAt)

                val itemsArray = JSONArray()
                for (item in order.items) {
                    val itemObj = JSONObject()
                    itemObj.put("quantity", item.quantity)
                    itemObj.put("spiceLevel", item.spiceLevel)
                    itemObj.put("notes", item.notes)

                    val prodObj = JSONObject()
                    prodObj.put("id", item.product.id)
                    prodObj.put("categoryId", item.product.categoryId)
                    prodObj.put("name", item.product.name)
                    prodObj.put("price", item.product.price)
                    prodObj.put("costPrice", item.product.costPrice)
                    prodObj.put("emoji", item.product.emoji)
                    prodObj.put("spiceLevel", item.product.spiceLevel)
                    prodObj.put("description", item.product.description)
                    prodObj.put("isActive", item.product.isActive)

                    itemObj.put("product", prodObj)
                    itemsArray.put(itemObj)
                }
                orderObj.put("items", itemsArray)
                jsonArray.put(orderObj)
            }
            prefs.edit().putString("saved_orders_list", jsonArray.toString()).apply()
        } catch (_: Exception) {}
        _savedOrdersFlow.value = list
    }

    fun saveOrder(order: SavedOrder) {
        val current = _savedOrdersFlow.value.toMutableList()
        current.add(0, order)
        persistOrders(current)
    }

    fun removeSavedOrder(orderId: String) {
        val filtered = _savedOrdersFlow.value.filter { it.id != orderId }
        persistOrders(filtered)
    }

    fun clearAll() {
        persistOrders(emptyList())
    }

    fun clearAllSavedOrders() {
        clearAll()
    }
}
