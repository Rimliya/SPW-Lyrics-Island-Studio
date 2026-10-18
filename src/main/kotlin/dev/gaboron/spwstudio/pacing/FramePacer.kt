package dev.gaboron.spwstudio.pacing

enum class FrameRateTier(val fps: Int, val label: String) {
    LOW(15, "15 FPS"),
    STANDARD(30, "30 FPS"),
    HIGH(60, "60 FPS"),
}

class FramePacer(
    targetFps: Int = FrameRateTier.STANDARD.fps,
    private val nanoTime: () -> Long = System::nanoTime,
) {
    var targetFps: Int = targetFps.coerceAtLeast(1)
        private set
    private var lastFrameNanos: Long? = null

    fun setTargetFps(fps: Int) {
        targetFps = fps.coerceAtLeast(1)
        lastFrameNanos = null
    }

    fun reset() {
        lastFrameNanos = null
    }

    fun shouldRender(nowNanos: Long = nanoTime()): Boolean {
        val previous = lastFrameNanos
        if (previous == null || nowNanos - previous >= intervalNanos()) {
            lastFrameNanos = nowNanos
            return true
        }
        return false
    }

    fun delayUntilNextFrameMs(nowNanos: Long = nanoTime()): Long {
        val previous = lastFrameNanos ?: return 0
        val remaining = intervalNanos() - (nowNanos - previous)
        return if (remaining <= 0) 0 else (remaining + 999_999) / 1_000_000
    }

    private fun intervalNanos(): Long = 1_000_000_000L / targetFps
}

class FramePacingMonitor(private val capacity: Int = 120) {
    private val intervals = ArrayDeque<Long>()
    private var lastFrameNanos: Long? = null

    @Synchronized
    fun record(nowNanos: Long) {
        lastFrameNanos?.let { previous ->
            intervals.addLast((nowNanos - previous).coerceAtLeast(0))
            while (intervals.size > capacity) intervals.removeFirst()
        }
        lastFrameNanos = nowNanos
    }

    @Synchronized
    fun reset() {
        intervals.clear()
        lastFrameNanos = null
    }

    @Synchronized
    fun averageFps(): Double {
        if (intervals.isEmpty()) return 0.0
        val averageNanos = intervals.average()
        return if (averageNanos <= 0.0) 0.0 else 1_000_000_000.0 / averageNanos
    }

    @Synchronized
    fun p95FrameMs(): Double {
        if (intervals.isEmpty()) return 0.0
        val sorted = intervals.sorted()
        val index = ((sorted.size - 1) * 0.95).toInt().coerceIn(0, sorted.lastIndex)
        return sorted[index] / 1_000_000.0
    }

    @Synchronized
    fun droppedFrames(targetFps: Int): Int {
        if (targetFps <= 0 || intervals.isEmpty()) return 0
        val interval = 1_000_000_000L / targetFps
        return intervals.count { it > interval * 3 / 2 }
    }
}
