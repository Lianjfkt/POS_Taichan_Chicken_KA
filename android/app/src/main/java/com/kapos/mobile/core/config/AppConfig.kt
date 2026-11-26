package com.kapos.mobile.core.config

import com.kapos.mobile.BuildConfig

/**
 * Konfigurasi aplikasi. Semua credentials dibaca dari BuildConfig
 * yang di-inject saat build dari local.properties (tidak hardcoded di source code).
 */
object AppConfig {
    val DEFAULT_SUPABASE_URL: String get() = BuildConfig.SUPABASE_URL
    val DEFAULT_SUPABASE_ANON_KEY: String get() = BuildConfig.SUPABASE_ANON_KEY
}
