package com.kapos.mobile

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.kapos.mobile.core.data.local.AppDatabase
import com.kapos.mobile.core.data.remote.SupabaseClient
import com.kapos.mobile.core.data.repository.PosRepository
import com.kapos.mobile.core.data.repository.SettingsRepository
import com.kapos.mobile.core.data.repository.ShiftRepository
import com.kapos.mobile.core.sync.SyncOrdersWorker
import com.kapos.mobile.core.utils.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

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

    lateinit var networkMonitor: NetworkMonitor
        private set

    /**
     * Scope dengan lifecycle yang terikat ke Application process.
     * Gunakan SupervisorJob agar satu coroutine gagal tidak membatalkan yang lain.
     * Dibatalkan di onTerminate() agar tidak ada leak saat proses dihentikan.
     */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        instance = this

        networkMonitor = NetworkMonitor(this)

        database = AppDatabase.getInstance(this)
        backupManager = com.kapos.mobile.core.data.backup.DatabaseBackupManager(database)
        promotionRepository = com.kapos.mobile.core.data.repository.PromotionRepository()
        settingsRepository = SettingsRepository(this)

        val settings = settingsRepository.settingsFlow.value
        supabaseClient = SupabaseClient(
            baseUrl = settings.supabaseUrl,
            apiKey = settings.supabaseAnonKey
        )

        customerRepository = com.kapos.mobile.core.data.repository.CustomerRepository(
            database = database,
            supabaseClient = supabaseClient,
            scope = applicationScope
        )
        inventoryRepository = com.kapos.mobile.core.data.repository.InventoryRepository(
            database = database,
            supabaseClient = supabaseClient,
            scope = applicationScope
        )
        stockRepository = com.kapos.mobile.core.data.repository.StockRepository(this)
        savedOrderRepository = com.kapos.mobile.core.data.repository.SavedOrderRepository(this)

        bluetoothPrinterService = com.kapos.mobile.core.printer.BluetoothPrinterService(this)

        shiftRepository = ShiftRepository(
            database = database,
            supabaseClient = supabaseClient,
            scope = applicationScope
        )

        posRepository = PosRepository(
            database = database,
            supabaseClient = supabaseClient,
            inventoryRepository = inventoryRepository,
            customerRepository = customerRepository,
            settingsRepository = settingsRepository,
            shiftRepository = shiftRepository
        )

        // Sinkronkan SupabaseClient credentials saat settings berubah
        // Menggunakan applicationScope (bukan anonymous scope) agar dapat dicancel
        applicationScope.launch(Dispatchers.IO) {
            settingsRepository.settingsFlow.collect { s ->
                supabaseClient.updateConfig(s.supabaseUrl, s.supabaseAnonKey)
            }
        }

        // Setup WorkManager periodic background sync
        setupWorkManager()

        // Observe network changes for instant reconnect sync
        observeNetworkChanges()
    }

    private fun setupWorkManager() {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<SyncOrdersWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                SyncOrdersWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        } catch (e: Exception) {
            // Ignore in testing environments without WorkManager initialization
        }
    }

    private fun observeNetworkChanges() {
        applicationScope.launch(Dispatchers.IO) {
            var wasOnline: Boolean? = null
            // StateFlow is inherently distinctUntilChanged
            networkMonitor.isOnlineFlow.collect { isOnline ->
                // Trigger syncAll() immediately when transitioning from offline to online
                if (wasOnline == false && isOnline) {
                    posRepository.syncAll()
                }
                wasOnline = isOnline
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        try {
            networkMonitor.unregister()
        } catch (e: Exception) {
            // Ignore
        }
        // Batalkan semua coroutine saat proses dihentikan
        applicationScope.cancel()
    }

    companion object {
        lateinit var instance: KaPosApplication
            private set
    }
}
