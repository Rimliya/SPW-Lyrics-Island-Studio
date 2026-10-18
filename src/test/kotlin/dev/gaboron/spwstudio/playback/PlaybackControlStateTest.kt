package dev.gaboron.spwstudio.playback

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlaybackControlStateTest {
    @Test
    fun `progress and seek conversion clamp to the track`() {
        val state = PlaybackControlState(positionMs = 30_000, durationMs = 60_000)
        assertEquals(0.5, state.progressFraction, 0.0001)
        assertEquals(30_000, state.seekPositionFor(100.0, 200.0))
        assertEquals(0, state.seekPositionFor(-10.0, 200.0))
        assertEquals(60_000, state.seekPositionFor(300.0, 200.0))
    }

    @Test
    fun `dragging previews a seek and commits on release`() {
        val state = PlaybackControlState(positionMs = 0, durationMs = 60_000)
        val dragging = state.beginDrag(50.0, 200.0)
        assertTrue(dragging.isDragging)
        assertEquals(15_000, dragging.pendingSeekMs)
        assertEquals(15_000, dragging.positionChanged(1_000).displayPositionMs)

        val (released, target) = dragging.dragTo(150.0, 200.0).endDrag()
        assertEquals(45_000, target)
        assertEquals(45_000, released.positionMs)
        assertFalse(released.isDragging)
        assertNull(released.pendingSeekMs)
    }

    @Test
    fun `unknown duration disables seeking`() {
        val state = PlaybackControlState(durationMs = 0)
        assertFalse(state.canSeek)
        assertEquals(state, state.beginDrag(10.0, 100.0))
    }
}
