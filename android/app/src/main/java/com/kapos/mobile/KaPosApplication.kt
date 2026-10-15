package com.kapos.mobile

import android.app.Application
import com.kapos.mobile.core.data.local.AppDatabase
import com.kapos.mobile.core.data.remote.SupabaseClient
import com.kapos.mobile.core.data.repository.PosRepository
import com.kapos.mobile.core.data.repository.SettingsRepository

class KaPosApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var supabaseClient: SupabaseClient
        private set

    lateinit var posRepository: PosRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        settingsRepository = SettingsRepository(this)

        val settings = settingsRepository.settingsFlow.value
        supabaseClient = SupabaseClient(
            baseUrl = settings.supabaseUrl,
            apiKey = settings.supabaseAnonKey
        )

        posRepository = PosRepository(
            database = database,
            supabaseClient = supabaseClient
        )
    }

    companion object {
        lateinit var instance: KaPosApplication
            private set
    }
}
