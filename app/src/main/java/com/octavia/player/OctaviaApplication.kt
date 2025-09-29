package com.octavia.player

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.SingletonImageLoader
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Main application class for Octavia Hi-Fi Music Player
 * Initializes Hilt dependency injection, WorkManager, and optimized image loading
 */
@HiltAndroidApp
class OctaviaApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var imageLoader: ImageLoader

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Set the optimized ImageLoader as the singleton instance
        SingletonImageLoader.setSafe { imageLoader }
    }
}
