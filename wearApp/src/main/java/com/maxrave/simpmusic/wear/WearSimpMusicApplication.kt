package com.maxrave.simpmusic.wear

import android.app.Application
import com.maxrave.data.di.loader.loadAllModules
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.repository.AccountRepository
import com.maxrave.domain.repository.CommonRepository
import com.maxrave.simpmusic.wear.auth.WearAccountManager
import com.maxrave.simpmusic.wear.net.ProcessNetworkBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.maxrave.logger.Logger
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

/**
 * Wear entrypoint.
 *
 * We keep this separate from the phone app Application so we don’t inherit
 * phone-specific crash/restart wiring or UI assumptions.
 */
class WearSimpMusicApplication : Application() {
    private var networkBinder: ProcessNetworkBinder? = null
    private val startupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Prefer validated Wi-Fi when available to avoid YouTube signed URL 403s caused by
        // network path/IP mismatch (Wi-Fi vs phone-proxied Bluetooth).
        networkBinder = ProcessNetworkBinder(this).also { it.start() }

        startKoin {
            androidContext(this@WearSimpMusicApplication)
            loadAllModules()
        }

        startupScope.launch {
            runCatching {
                val koin = GlobalContext.get()
                val dataStoreManager: DataStoreManager = koin.get()
                val accountRepository: AccountRepository = koin.get()
                val commonRepository: CommonRepository = koin.get()

                WearAccountManager(
                    context = applicationContext,
                    dataStoreManager = dataStoreManager,
                    accountRepository = accountRepository,
                    commonRepository = commonRepository,
                ).bootstrapSessionAtStartup()
            }.onFailure {
                Logger.e("WearStartup", "Session bootstrap failed: ${it.message}", it)
            }
        }
    }

    override fun onTerminate() {
        networkBinder?.stop()
        networkBinder = null
        super.onTerminate()
    }
}
