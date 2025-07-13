package com.octavia.player.data.scanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint

/**
 * Broadcast receiver for MediaStore changes
 */
@AndroidEntryPoint
class MediaStoreBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_MEDIA_SCANNER_FINISHED,
            Intent.ACTION_MEDIA_MOUNTED -> {
                // Trigger a media scan when external storage is mounted or scan is finished
                triggerMediaScan(context)
            }
        }
    }

    private fun triggerMediaScan(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<MediaScannerWorker>()
            .addTag(MEDIA_SCAN_WORK_TAG)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                MEDIA_SCAN_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
    }

    companion object {
        const val MEDIA_SCAN_WORK_NAME = "media_scan_work"
        const val MEDIA_SCAN_WORK_TAG = "media_scan"
    }
}
