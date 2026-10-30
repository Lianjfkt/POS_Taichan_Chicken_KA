package com.kapos.mobile.core.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Semua Room migrations didefinisikan di sini secara eksplisit.
 *
 * ATURAN PENTING:
 * - Setiap kali versi database di AppDatabase.kt naik (misal 6 → 7),
 *   WAJIB tambahkan MIGRATION_X_Y di sini.
 * - JANGAN gunakan fallbackToDestructiveMigration() di produksi.
 * - Daftarkan migration baru di AppDatabase.getInstance() → addMigrations(...)
 *
 * History:
 * v1–v5 : Tahap development awal (destructive migration digunakan, data tidak dipreservasi)
 * v6    : Versi release pertama dengan ModifierEntity + MaterialBatchEntity
 *         Semua user yang upgrade dari v1-v5 harus install ulang (data direset)
 */
object AppMigrations {

    /**
     * Placeholder migration v6 → v7.
     * Isi dengan ALTER TABLE / CREATE TABLE sesuai perubahan schema ketika versi dinaikkan.
     */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Kosong — tidak ada perubahan schema di v7
            // Contoh pengisian jika ada perubahan:
            // database.execSQL("ALTER TABLE products ADD COLUMN barcode TEXT DEFAULT '' NOT NULL")
        }
    }

    /** Semua migrations yang aktif — daftarkan semua di sini secara berurutan */
    val ALL = arrayOf(
        MIGRATION_6_7
    )
}
