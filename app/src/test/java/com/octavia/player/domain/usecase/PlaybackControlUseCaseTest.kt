package com.octavia.player.domain.usecase

import com.octavia.player.data.model.RepeatMode
import com.octavia.player.data.model.ShuffleMode
import com.octavia.player.data.model.Track
import com.octavia.player.domain.repository.MediaPlaybackRepository
import com.octavia.player.domain.repository.TrackRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class PlaybackControlUseCaseTest {

    private lateinit var mediaPlaybackRepository: MediaPlaybackRepository
    private lateinit var trackRepository: TrackRepository
    private lateinit var useCase: PlaybackControlUseCase

    @Before
    fun setup() {
        mediaPlaybackRepository = mockk()
        trackRepository = mockk()
        useCase = PlaybackControlUseCase(mediaPlaybackRepository, trackRepository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `playTrack calls repository and increments play count`() = runTest {
        // Given
        val track = createTestTrack(1, "Test Track")
        coEvery { mediaPlaybackRepository.playTrack(track) } just Runs
        coEvery { trackRepository.incrementPlayCount(track.id) } just Runs

        // When
        useCase.playTrack(track)

        // Then
        coVerify { mediaPlaybackRepository.playTrack(track) }
        coVerify { trackRepository.incrementPlayCount(track.id) }
    }

    @Test
    fun `playTracks calls repository and increments play count for start track`() = runTest {
        // Given
        val tracks = listOf(
            createTestTrack(1, "Track 1"),
            createTestTrack(2, "Track 2"),
            createTestTrack(3, "Track 3")
        )
        val startIndex = 1
        coEvery { mediaPlaybackRepository.playTracks(tracks, startIndex) } just Runs
        coEvery { trackRepository.incrementPlayCount(tracks[startIndex].id) } just Runs

        // When
        useCase.playTracks(tracks, startIndex)

        // Then
        coVerify { mediaPlaybackRepository.playTracks(tracks, startIndex) }
        coVerify { trackRepository.incrementPlayCount(tracks[startIndex].id) }
    }

    @Test
    fun `playTracks handles invalid start index gracefully`() = runTest {
        // Given
        val tracks = listOf(createTestTrack(1, "Track 1"))
        val invalidStartIndex = 5
        coEvery { mediaPlaybackRepository.playTracks(tracks, invalidStartIndex) } just Runs

        // When
        useCase.playTracks(tracks, invalidStartIndex)

        // Then
        coVerify { mediaPlaybackRepository.playTracks(tracks, invalidStartIndex) }
        coVerify(exactly = 0) { trackRepository.incrementPlayCount(any()) }
    }

    @Test
    fun `togglePlayPause calls repository`() {
        // Given
        every { mediaPlaybackRepository.togglePlayPause() } just Runs

        // When
        useCase.togglePlayPause()

        // Then
        verify { mediaPlaybackRepository.togglePlayPause() }
    }

    @Test
    fun `skipToNext calls repository`() = runTest {
        // Given
        coEvery { mediaPlaybackRepository.skipToNext() } just Runs

        // When
        useCase.skipToNext()

        // Then
        coVerify { mediaPlaybackRepository.skipToNext() }
    }

    @Test
    fun `skipToPrevious calls repository`() = runTest {
        // Given
        coEvery { mediaPlaybackRepository.skipToPrevious() } just Runs

        // When
        useCase.skipToPrevious()

        // Then
        coVerify { mediaPlaybackRepository.skipToPrevious() }
    }

    @Test
    fun `seekTo calls repository with position`() {
        // Given
        val position = 30000L
        every { mediaPlaybackRepository.seekTo(position) } just Runs

        // When
        useCase.seekTo(position)

        // Then
        verify { mediaPlaybackRepository.seekTo(position) }
    }

    @Test
    fun `setRepeatMode calls repository`() {
        // Given
        val repeatMode = RepeatMode.ALL
        every { mediaPlaybackRepository.setRepeatMode(repeatMode) } just Runs

        // When
        useCase.setRepeatMode(repeatMode)

        // Then
        verify { mediaPlaybackRepository.setRepeatMode(repeatMode) }
    }

    @Test
    fun `setShuffleMode calls repository`() {
        // Given
        val shuffleMode = ShuffleMode.ON
        every { mediaPlaybackRepository.setShuffleMode(shuffleMode) } just Runs

        // When
        useCase.setShuffleMode(shuffleMode)

        // Then
        verify { mediaPlaybackRepository.setShuffleMode(shuffleMode) }
    }

    @Test
    fun `setPlaybackSpeed calls repository`() {
        // Given
        val speed = 1.5f
        every { mediaPlaybackRepository.setPlaybackSpeed(speed) } just Runs

        // When
        useCase.setPlaybackSpeed(speed)

        // Then
        verify { mediaPlaybackRepository.setPlaybackSpeed(speed) }
    }

    @Test
    fun `setVolume calls repository`() {
        // Given
        val volume = 0.8f
        every { mediaPlaybackRepository.setVolume(volume) } just Runs

        // When
        useCase.setVolume(volume)

        // Then
        verify { mediaPlaybackRepository.setVolume(volume) }
    }

    @Test
    fun `toggleFavorite calls repository with opposite status`() = runTest {
        // Given
        val track = createTestTrack(1, "Test Track").copy(isFavorite = false)
        coEvery { trackRepository.updateFavoriteStatus(track.id, true) } just Runs

        // When
        useCase.toggleFavorite(track)

        // Then
        coVerify { trackRepository.updateFavoriteStatus(track.id, true) }
    }

    private fun createTestTrack(id: Long, title: String) = Track(
        id = id,
        filePath = "/path/to/$title.mp3",
        fileName = "$title.mp3",
        fileSize = 1024,
        lastModified = System.currentTimeMillis(),
        title = title,
        durationMs = 180000L,
        isFavorite = false
    )
}