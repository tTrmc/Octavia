package com.octavia.player.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.playbackDataStore: DataStore<Preferences> by preferencesDataStore(name = "playback_state")

/**
 * DataStore for persisting playback state across app restarts
 */
@Singleton
class PlaybackStateDataStore @Inject constructor(
    private val context: Context
) {

    private object PreferenceKeys {
        val LAST_TRACK_ID = longPreferencesKey("last_track_id")
        val LAST_POSITION = longPreferencesKey("last_position")
        val LAST_QUEUE_IDS = stringPreferencesKey("last_queue_ids")
        val LAST_QUEUE_INDEX = longPreferencesKey("last_queue_index")
        val LAST_SESSION_TIME = longPreferencesKey("last_session_time")
    }

    /**
     * Save playback state
     */
    suspend fun savePlaybackState(
        trackId: Long,
        position: Long,
        queueTrackIds: List<Long>,
        queueIndex: Int
    ) {
        context.playbackDataStore.edit { preferences ->
            preferences[PreferenceKeys.LAST_TRACK_ID] = trackId
            preferences[PreferenceKeys.LAST_POSITION] = position
            preferences[PreferenceKeys.LAST_QUEUE_IDS] = queueTrackIds.joinToString(",")
            preferences[PreferenceKeys.LAST_QUEUE_INDEX] = queueIndex.toLong()
            preferences[PreferenceKeys.LAST_SESSION_TIME] = System.currentTimeMillis()
        }
    }

    /**
     * Get saved playback state as Flow
     */
    fun getPlaybackState(): Flow<SavedPlaybackState?> {
        return context.playbackDataStore.data.map { preferences ->
            val trackId = preferences[PreferenceKeys.LAST_TRACK_ID]
            val position = preferences[PreferenceKeys.LAST_POSITION]
            val queueIds = preferences[PreferenceKeys.LAST_QUEUE_IDS]
            val queueIndex = preferences[PreferenceKeys.LAST_QUEUE_INDEX]
            val sessionTime = preferences[PreferenceKeys.LAST_SESSION_TIME]

            if (trackId != null && sessionTime != null) {
                // Only return if session is within 7 days
                val daysSinceLastSession = (System.currentTimeMillis() - sessionTime) / (1000 * 60 * 60 * 24)
                if (daysSinceLastSession <= 7) {
                    SavedPlaybackState(
                        trackId = trackId,
                        position = position ?: 0L,
                        queueTrackIds = queueIds?.split(",")?.mapNotNull { it.toLongOrNull() } ?: emptyList(),
                        queueIndex = queueIndex?.toInt() ?: 0,
                        sessionTime = sessionTime
                    )
                } else {
                    null
                }
            } else {
                null
            }
        }
    }

    /**
     * Clear saved playback state
     */
    suspend fun clearPlaybackState() {
        context.playbackDataStore.edit { preferences ->
            preferences.remove(PreferenceKeys.LAST_TRACK_ID)
            preferences.remove(PreferenceKeys.LAST_POSITION)
            preferences.remove(PreferenceKeys.LAST_QUEUE_IDS)
            preferences.remove(PreferenceKeys.LAST_QUEUE_INDEX)
            preferences.remove(PreferenceKeys.LAST_SESSION_TIME)
        }
    }
}

/**
 * Data class representing saved playback state
 */
data class SavedPlaybackState(
    val trackId: Long,
    val position: Long,
    val queueTrackIds: List<Long>,
    val queueIndex: Int,
    val sessionTime: Long
)
