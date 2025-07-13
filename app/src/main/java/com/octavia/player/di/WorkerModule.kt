package com.octavia.player.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Module for providing worker dependencies
 * Currently simplified since MediaScannerWorker doesn't require dependency injection
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class WorkerModule {
    // No bindings needed for the current simplified MediaScannerWorker
}
