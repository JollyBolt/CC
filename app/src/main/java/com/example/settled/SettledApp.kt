package com.example.settled

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.settled.data.auth.AuthManager
import com.example.settled.data.repository.CardRepositoryImpl
import com.example.settled.data.worker.scheduleCycleResetWorker
import com.example.settled.data.worker.scheduleDueReminderWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SettledApp : Application(), Configuration.Provider {

    @Inject lateinit var authManager: AuthManager
    @Inject lateinit var cardRepository: CardRepositoryImpl
    @Inject lateinit var workerFactory: HiltWorkerFactory

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleCycleResetWorker(this)
        scheduleDueReminderWorker(this)
        appScope.launch {
            runCatching {
                if (authManager.isSignedIn()) {
                    cardRepository.initialSyncFromFirestore()
                }
            }
        }
    }
}
