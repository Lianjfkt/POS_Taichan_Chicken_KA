package com.kapos.mobile.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.kapos.mobile.core.data.local.dao.CashMovementDao
import com.kapos.mobile.core.data.local.dao.CategoryDao
import com.kapos.mobile.core.data.local.dao.CustomerDao
import com.kapos.mobile.core.data.local.dao.OrderDao
import com.kapos.mobile.core.data.local.dao.ProductDao
import com.kapos.mobile.core.data.local.dao.RawMaterialDao
import com.kapos.mobile.core.data.local.dao.RecipeDao
import com.kapos.mobile.core.data.local.dao.ShiftDao
import com.kapos.mobile.core.data.local.dao.StockMovementDao
import com.kapos.mobile.core.data.local.entity.CashMovementEntity
import com.kapos.mobile.core.data.local.entity.CategoryEntity
import com.kapos.mobile.core.data.local.entity.CustomerEntity
import com.kapos.mobile.core.data.local.entity.OrderEntity
import com.kapos.mobile.core.data.local.entity.OrderItemEntity
import com.kapos.mobile.core.data.local.entity.ProductEntity
import com.kapos.mobile.core.data.local.entity.RawMaterialEntity
import com.kapos.mobile.core.data.local.entity.RecipeItemEntity
import com.kapos.mobile.core.data.local.entity.ShiftEntity
import com.kapos.mobile.core.data.local.dao.MaterialBatchDao
import com.kapos.mobile.core.data.local.entity.MaterialBatchEntity
import com.kapos.mobile.core.data.local.entity.StockMovementEntity

import com.kapos.mobile.core.data.local.dao.ModifierDao
import com.kapos.mobile.core.data.local.entity.ModifierEntity

@Database(
    entities = [
        CategoryEntity::class,
        ProductEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        ShiftEntity::class,
        CashMovementEntity::class,
        CustomerEntity::class,
        RawMaterialEntity::class,
        RecipeItemEntity::class,
        StockMovementEntity::class,
        MaterialBatchEntity::class,
        ModifierEntity::class
    ],
    version = 7,          // Dinaikkan dari 6 → 7 bersama dengan penambahan AppMigrations
    exportSchema = true   // Ekspor schema ke /schemas/ untuk audit & debug migration
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
    abstract fun shiftDao(): ShiftDao
    abstract fun cashMovementDao(): CashMovementDao
    abstract fun customerDao(): CustomerDao
    abstract fun rawMaterialDao(): RawMaterialDao
    abstract fun recipeDao(): RecipeDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun materialBatchDao(): MaterialBatchDao
    abstract fun modifierDao(): ModifierDao

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
                    // Migrations eksplisit — tidak ada data loss saat update app
                    .addMigrations(*AppMigrations.ALL)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
