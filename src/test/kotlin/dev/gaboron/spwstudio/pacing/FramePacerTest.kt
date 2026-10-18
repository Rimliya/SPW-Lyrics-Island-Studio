package dev.gaboron.spwstudio.pacing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FramePacerTest {
    @Test
    fun `pacer gates frames to the target interval`() {
        val pacer = FramePacer(FrameRateTier.STANDARD.fps)
        assertTrue(pacer.shouldRender(0))
        assertFalse(pacer.shouldRender(10_000_000))
        assertEquals(24, pacer.delayUntilNextFrameMs(10_000_000))
        assertTrue(pacer.shouldRender(34_000_000))
    }

    @Test
    fun `monitor reports average fps and dropped frames`() {
        val monitor = FramePacingMonitor(capacity = 16)
        var now = 0L
        monitor.record(now)
        repeat(5) {
            now += 16_666_667
            monitor.record(now)
        }
        assertEquals(60.0, monitor.averageFps(), 0.5)
        assertEquals(0, monitor.droppedFrames(60))

        monitor.record(now + 50_000_000)
        assertEquals(1, monitor.droppedFrames(60))
        assertTrue(monitor.p95FrameMs() > 0.0)
    }
}
