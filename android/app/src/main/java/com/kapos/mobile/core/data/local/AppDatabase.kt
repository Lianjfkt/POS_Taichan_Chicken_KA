package com.kapos.mobile.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.kapos.mobile.core.data.local.dao.CashMovementDao
import com.kapos.mobile.core.data.local.dao.CategoryDao
import com.kapos.mobile.core.data.local.dao.OrderDao
import com.kapos.mobile.core.data.local.dao.ProductDao
import com.kapos.mobile.core.data.local.dao.ShiftDao
import com.kapos.mobile.core.data.local.entity.CashMovementEntity
import com.kapos.mobile.core.data.local.entity.CategoryEntity
import com.kapos.mobile.core.data.local.entity.OrderEntity
import com.kapos.mobile.core.data.local.entity.OrderItemEntity
import com.kapos.mobile.core.data.local.entity.ProductEntity
import com.kapos.mobile.core.data.local.entity.ShiftEntity

@Database(
    entities = [
        CategoryEntity::class,
        ProductEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        ShiftEntity::class,
        CashMovementEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
    abstract fun shiftDao(): ShiftDao
    abstract fun cashMovementDao(): CashMovementDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ka_pos_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
