package com.octavia.player.data.repository

import androidx.paging.PagingSource
import com.octavia.player.data.database.dao.TrackDao
import com.octavia.player.data.model.Track
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

class TrackRepositoryImplTest {

    private lateinit var trackDao: TrackDao
    private lateinit var repository: TrackRepositoryImpl

    @Before
    fun setup() {
        trackDao = mockk()
        repository = TrackRepositoryImpl(trackDao)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getAllTracks returns tracks from dao`() = runTest {
        // Given
        val expectedTracks = listOf(
            createTestTrack(1, "Track 1"),
            createTestTrack(2, "Track 2")
        )
        every { trackDao.getAllTracksFlow() } returns flowOf(expectedTracks)

        // When
        val result = repository.getAllTracks().first()

        // Then
        assertEquals(expectedTracks, result)
        verify { trackDao.getAllTracksFlow() }
    }

    @Test
    fun `getTrackById returns track when found`() = runTest {
        // Given
        val trackId = 1L
        val expectedTrack = createTestTrack(trackId, "Test Track")
        coEvery { trackDao.getTrackById(trackId) } returns expectedTrack

        // When
        val result = repository.getTrackById(trackId)

        // Then
        assertEquals(expectedTrack, result)
        coVerify { trackDao.getTrackById(trackId) }
    }

    @Test
    fun `getTrackById returns null when not found`() = runTest {
        // Given
        val trackId = 999L
        coEvery { trackDao.getTrackById(trackId) } returns null

        // When
        val result = repository.getTrackById(trackId)

        // Then
        assertNull(result)
        coVerify { trackDao.getTrackById(trackId) }
    }

    @Test
    fun `insertTrack calls dao insertTrack`() = runTest {
        // Given
        val track = createTestTrack(1, "New Track")
        val expectedId = 1L
        coEvery { trackDao.insertTrack(track) } returns expectedId

        // When
        val result = repository.insertTrack(track)

        // Then
        assertEquals(expectedId, result)
        coVerify { trackDao.insertTrack(track) }
    }

    @Test
    fun `incrementPlayCount calls dao with current timestamp`() = runTest {
        // Given
        val trackId = 1L
        coEvery { trackDao.incrementPlayCount(trackId, any()) } just Runs

        // When
        repository.incrementPlayCount(trackId)

        // Then
        coVerify { trackDao.incrementPlayCount(trackId, any()) }
    }

    @Test
    fun `updateFavoriteStatus calls dao with correct parameters`() = runTest {
        // Given
        val trackId = 1L
        val isFavorite = true
        coEvery { trackDao.updateFavoriteStatus(trackId, isFavorite) } just Runs

        // When
        repository.updateFavoriteStatus(trackId, isFavorite)

        // Then
        coVerify { trackDao.updateFavoriteStatus(trackId, isFavorite) }
    }

    @Test
    fun `getTrackCount returns dao count`() = runTest {
        // Given
        val expectedCount = 42
        coEvery { trackDao.getTrackCount() } returns expectedCount

        // When
        val result = repository.getTrackCount()

        // Then
        assertEquals(expectedCount, result)
        coVerify { trackDao.getTrackCount() }
    }

    @Test
    fun `getTotalDuration returns dao duration or zero if null`() = runTest {
        // Given
        val expectedDuration = 120000L
        coEvery { trackDao.getTotalDuration() } returns expectedDuration

        // When
        val result = repository.getTotalDuration()

        // Then
        assertEquals(expectedDuration, result)
        coVerify { trackDao.getTotalDuration() }
    }

    @Test
    fun `getTotalDuration returns zero when dao returns null`() = runTest {
        // Given
        coEvery { trackDao.getTotalDuration() } returns null

        // When
        val result = repository.getTotalDuration()

        // Then
        assertEquals(0L, result)
        coVerify { trackDao.getTotalDuration() }
    }

    private fun createTestTrack(id: Long, title: String) = Track(
        id = id,
        filePath = "/path/to/$title.mp3",
        fileName = "$title.mp3",
        fileSize = 1024,
        lastModified = System.currentTimeMillis(),
        title = title,
        durationMs = 180000L
    )
}