package com.kapos.mobile

import android.app.Application
import com.kapos.mobile.core.data.local.AppDatabase
import com.kapos.mobile.core.data.remote.SupabaseClient
import com.kapos.mobile.core.data.repository.PosRepository
import com.kapos.mobile.core.data.repository.SettingsRepository
import com.kapos.mobile.core.data.repository.ShiftRepository
import kotlinx.coroutines.launch

class KaPosApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var supabaseClient: SupabaseClient
        private set

    lateinit var posRepository: PosRepository
        private set

    lateinit var shiftRepository: ShiftRepository
        private set

    lateinit var customerRepository: com.kapos.mobile.core.data.repository.CustomerRepository
        private set

    lateinit var inventoryRepository: com.kapos.mobile.core.data.repository.InventoryRepository
        private set

    lateinit var stockRepository: com.kapos.mobile.core.data.repository.StockRepository
        private set

    lateinit var savedOrderRepository: com.kapos.mobile.core.data.repository.SavedOrderRepository
        private set

    lateinit var bluetoothPrinterService: com.kapos.mobile.core.printer.BluetoothPrinterService
        private set

    lateinit var backupManager: com.kapos.mobile.core.data.backup.DatabaseBackupManager
        private set

    lateinit var promotionRepository: com.kapos.mobile.core.data.repository.PromotionRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        backupManager = com.kapos.mobile.core.data.backup.DatabaseBackupManager(database)
        promotionRepository = com.kapos.mobile.core.data.repository.PromotionRepository()
        settingsRepository = SettingsRepository(this)
        customerRepository = com.kapos.mobile.core.data.repository.CustomerRepository(database = database)
        inventoryRepository = com.kapos.mobile.core.data.repository.InventoryRepository(database = database)
        stockRepository = com.kapos.mobile.core.data.repository.StockRepository(this)
        savedOrderRepository = com.kapos.mobile.core.data.repository.SavedOrderRepository(this)

        val settings = settingsRepository.settingsFlow.value
        supabaseClient = SupabaseClient(
            baseUrl = settings.supabaseUrl,
            apiKey = settings.supabaseAnonKey
        )

        bluetoothPrinterService = com.kapos.mobile.core.printer.BluetoothPrinterService(this)

        posRepository = PosRepository(
            database = database,
            supabaseClient = supabaseClient,
            inventoryRepository = inventoryRepository,
            customerRepository = customerRepository,
            settingsRepository = settingsRepository
        )

        shiftRepository = ShiftRepository(database = database)

        // Keep SupabaseClient credentials in sync with Settings
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            settingsRepository.settingsFlow.collect { s ->
                supabaseClient.updateConfig(s.supabaseUrl, s.supabaseAnonKey)
            }
        }
    }

    companion object {
        lateinit var instance: KaPosApplication
            private set
    }
}

