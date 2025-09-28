package com.octavia.player.data.model

import org.junit.Test
import org.junit.Assert.assertEquals

class TrackTest {

    @Test
    fun `displayTitle returns title when not blank`() {
        // Given
        val track = createTestTrack(title = "My Song")

        // When
        val displayTitle = track.displayTitle

        // Then
        assertEquals("My Song", displayTitle)
    }

    @Test
    fun `displayTitle returns filename without extension when title is blank`() {
        // Given
        val track = createTestTrack(title = "", fileName = "my_song.mp3")

        // When
        val displayTitle = track.displayTitle

        // Then
        assertEquals("my_song", displayTitle)
    }

    @Test
    fun `displayArtist returns artist when not blank`() {
        // Given
        val track = createTestTrack(artist = "The Beatles")

        // When
        val displayArtist = track.displayArtist

        // Then
        assertEquals("The Beatles", displayArtist)
    }

    @Test
    fun `displayArtist returns Unknown Artist when blank`() {
        // Given
        val track = createTestTrack(artist = "")

        // When
        val displayArtist = track.displayArtist

        // Then
        assertEquals("Unknown Artist", displayArtist)
    }

    @Test
    fun `displayAlbum returns album when not blank`() {
        // Given
        val track = createTestTrack(album = "Abbey Road")

        // When
        val displayAlbum = track.displayAlbum

        // Then
        assertEquals("Abbey Road", displayAlbum)
    }

    @Test
    fun `displayAlbum returns Unknown Album when blank`() {
        // Given
        val track = createTestTrack(album = null)

        // When
        val displayAlbum = track.displayAlbum

        // Then
        assertEquals("Unknown Album", displayAlbum)
    }

    @Test
    fun `formattedDuration formats minutes and seconds correctly`() {
        // Given - 3 minutes 45 seconds (225 seconds = 225000 ms)
        val track = createTestTrack(durationMs = 225000L)

        // When
        val formatted = track.formattedDuration

        // Then
        assertEquals("3:45", formatted)
    }

    @Test
    fun `formattedDuration formats hours minutes and seconds correctly`() {
        // Given - 1 hour 30 minutes 45 seconds (5445 seconds = 5445000 ms)
        val track = createTestTrack(durationMs = 5445000L)

        // When
        val formatted = track.formattedDuration

        // Then
        assertEquals("1:30:45", formatted)
    }

    @Test
    fun `formattedDuration pads seconds with zero`() {
        // Given - 2 minutes 5 seconds (125 seconds = 125000 ms)
        val track = createTestTrack(durationMs = 125000L)

        // When
        val formatted = track.formattedDuration

        // Then
        assertEquals("2:05", formatted)
    }

    @Test
    fun `qualityDescription includes codec when available`() {
        // Given
        val track = createTestTrack(codecName = "FLAC")

        // When
        val quality = track.qualityDescription

        // Then
        assertEquals("FLAC", quality)
    }

    @Test
    fun `qualityDescription includes lossless when true`() {
        // Given
        val track = createTestTrack(isLossless = true)

        // When
        val quality = track.qualityDescription

        // Then
        assertEquals("Lossless", quality)
    }

    @Test
    fun `qualityDescription includes sample rate`() {
        // Given
        val track = createTestTrack(sampleRateHz = 96000)

        // When
        val quality = track.qualityDescription

        // Then
        assertEquals("96 kHz", quality)
    }

    @Test
    fun `qualityDescription includes bit depth`() {
        // Given
        val track = createTestTrack(bitDepth = 24)

        // When
        val quality = track.qualityDescription

        // Then
        assertEquals("24-bit", quality)
    }

    @Test
    fun `qualityDescription combines all available info`() {
        // Given
        val track = createTestTrack(
            codecName = "FLAC",
            isLossless = true,
            sampleRateHz = 192000,
            bitDepth = 24
        )

        // When
        val quality = track.qualityDescription

        // Then
        assertEquals("FLAC • Lossless • 192 kHz • 24-bit", quality)
    }

    @Test
    fun `qualityDescription handles 44_1 kHz correctly`() {
        // Given
        val track = createTestTrack(sampleRateHz = 44100)

        // When
        val quality = track.qualityDescription

        // Then
        assertEquals("44.1 kHz", quality)
    }

    private fun createTestTrack(
        id: Long = 1L,
        title: String = "Test Track",
        artist: String? = "Test Artist",
        album: String? = "Test Album",
        fileName: String = "test.mp3",
        durationMs: Long = 180000L,
        codecName: String? = null,
        isLossless: Boolean = false,
        sampleRateHz: Int? = null,
        bitDepth: Int? = null
    ) = Track(
        id = id,
        filePath = "/path/to/$fileName",
        fileName = fileName,
        fileSize = 1024,
        lastModified = System.currentTimeMillis(),
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        codecName = codecName,
        isLossless = isLossless,
        sampleRateHz = sampleRateHz,
        bitDepth = bitDepth
    )
}