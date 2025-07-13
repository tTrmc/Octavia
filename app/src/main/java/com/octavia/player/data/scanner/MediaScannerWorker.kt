package com.octavia.player.data.scanner

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Background worker for scanning music library
 */
class MediaScannerWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // For now, just return success
            // The actual scanning will be done by the ViewModel using MediaScanner directly
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
