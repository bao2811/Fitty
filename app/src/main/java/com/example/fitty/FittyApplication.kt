package com.example.fitty

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FittyApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        installFirebaseAppCheck()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun installFirebaseAppCheck() {
        val providerFactory = if (BuildConfig.DEBUG) {
            debugAppCheckProviderFactory() ?: PlayIntegrityAppCheckProviderFactory.getInstance()
        } else {
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(providerFactory)
    }

    private fun debugAppCheckProviderFactory(): AppCheckProviderFactory? {
        return runCatching {
            Class.forName("com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory")
                .getMethod("getInstance")
                .invoke(null) as AppCheckProviderFactory
        }.getOrNull()
    }
}
