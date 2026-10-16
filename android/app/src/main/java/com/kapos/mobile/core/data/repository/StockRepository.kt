package com.kapos.mobile.core.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.kapos.mobile.domain.model.ChickenBatch
import com.kapos.mobile.domain.model.ChickenPart
import com.kapos.mobile.domain.model.TaichanBatch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StockRepository(context: Context? = null) {
    private val prefs: SharedPreferences? = context?.getSharedPreferences("ka_daily_stock", Context.MODE_PRIVATE)

    private val _taichanBatches = MutableStateFlow<List<TaichanBatch>>(emptyList())
    val taichanBatches: StateFlow<List<TaichanBatch>> = _taichanBatches.asStateFlow()

    private val _chickenBatches = MutableStateFlow<List<ChickenBatch>>(emptyList())
    val chickenBatches: StateFlow<List<ChickenBatch>> = _chickenBatches.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        loadData()
    }

    private fun loadData() {
        if (prefs == null) {
            // Seed initial sample batches for testing/preview if empty
            val now = System.currentTimeMillis()
            val todayStr = dateFormat.format(Date(now))
            _taichanBatches.value = listOf(
                TaichanBatch(id = "TC-1", date = todayStr, timestamp = now, raw = 200, fried = 50, notes = "Batch pagi")
            )
            _chickenBatches.value = listOf(
                ChickenBatch(id = "CK-1", part = ChickenPart.SAYAP, date = todayStr, timestamp = now, raw = 30, fried = 10, notes = "Potongan segar")
            )
            return
        }

        try {
            val tcRaw = prefs.getString("taichan_batches", "[]") ?: "[]"
            val tcArray = JSONArray(tcRaw)
            val tcList = mutableListOf<TaichanBatch>()
            for (i in 0 until tcArray.length()) {
                val obj = tcArray.getJSONObject(i)
                tcList.add(
                    TaichanBatch(
                        id = obj.getString("id"),
                        date = obj.getString("date"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        raw = obj.getInt("raw"),
                        fried = obj.optInt("fried", 0),
                        notes = obj.optString("notes", "")
                    )
                )
            }
            _taichanBatches.value = tcList

            val ckRaw = prefs.getString("chicken_batches", "[]") ?: "[]"
            val ckArray = JSONArray(ckRaw)
            val ckList = mutableListOf<ChickenBatch>()
            for (i in 0 until ckArray.length()) {
                val obj = ckArray.getJSONObject(i)
                val partStr = obj.getString("part")
                val part = ChickenPart.values().find { it.id == partStr } ?: ChickenPart.SAYAP
                ckList.add(
                    ChickenBatch(
                        id = obj.getString("id"),
                        part = part,
                        date = obj.getString("date"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        raw = obj.getInt("raw"),
                        fried = obj.optInt("fried", 0),
                        notes = obj.optString("notes", "")
                    )
                )
            }
            _chickenBatches.value = ckList
        } catch (e: Exception) {
            _taichanBatches.value = emptyList()
            _chickenBatches.value = emptyList()
        }
    }

    private fun persistTaichan() {
        if (prefs == null) return
        val array = JSONArray()
        for (b in _taichanBatches.value) {
            val obj = JSONObject().apply {
                put("id", b.id)
                put("date", b.date)
                put("timestamp", b.timestamp)
                put("raw", b.raw)
                put("fried", b.fried)
                put("notes", b.notes)
            }
            array.put(obj)
        }
        prefs.edit().putString("taichan_batches", array.toString()).apply()
    }

    private fun persistChicken() {
        if (prefs == null) return
        val array = JSONArray()
        for (b in _chickenBatches.value) {
            val obj = JSONObject().apply {
                put("id", b.id)
                put("part", b.part.id)
                put("date", b.date)
                put("timestamp", b.timestamp)
                put("raw", b.raw)
                put("fried", b.fried)
                put("notes", b.notes)
            }
            array.put(obj)
        }
        prefs.edit().putString("chicken_batches", array.toString()).apply()
    }

    fun addTaichanBatch(raw: Int, notes: String = "") {
        val now = System.currentTimeMillis()
        val newBatch = TaichanBatch(
            id = "TC-$now",
            date = dateFormat.format(Date(now)),
            timestamp = now,
            raw = raw,
            fried = 0,
            notes = notes
        )
        _taichanBatches.value = _taichanBatches.value + newBatch
        persistTaichan()
    }

    fun updateTaichanFried(batchId: String, fried: Int) {
        _taichanBatches.value = _taichanBatches.value.map {
            if (it.id == batchId) it.copy(fried = fried.coerceIn(0, it.raw)) else it
        }
        persistTaichan()
    }

    fun deleteTaichanBatch(batchId: String) {
        _taichanBatches.value = _taichanBatches.value.filter { it.id != batchId }
        persistTaichan()
    }

    fun addChickenBatch(part: ChickenPart, raw: Int, notes: String = "") {
        val now = System.currentTimeMillis()
        val newBatch = ChickenBatch(
            id = "CK-$now",
            part = part,
            date = dateFormat.format(Date(now)),
            timestamp = now,
            raw = raw,
            fried = 0,
            notes = notes
        )
        _chickenBatches.value = _chickenBatches.value + newBatch
        persistChicken()
    }

    fun updateChickenFried(batchId: String, fried: Int) {
        _chickenBatches.value = _chickenBatches.value.map {
            if (it.id == batchId) it.copy(fried = fried.coerceIn(0, it.raw)) else it
        }
        persistChicken()
    }

    fun deleteChickenBatch(batchId: String) {
        _chickenBatches.value = _chickenBatches.value.filter { it.id != batchId }
        persistChicken()
    }
}
