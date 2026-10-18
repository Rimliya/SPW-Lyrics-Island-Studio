package dev.gaboron.spwstudio.playback

import kotlin.math.roundToLong

enum class PlaybackAction {
    PREVIOUS,
    PLAY_PAUSE,
    NEXT,
}

data class PlaybackControlState(
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val isHovered: Boolean = false,
    val isDragging: Boolean = false,
    val pendingSeekMs: Long? = null,
) {
    val canSeek: Boolean get() = durationMs > 0

    val displayPositionMs: Long get() = pendingSeekMs ?: positionMs

    val progressFraction: Double
        get() = if (durationMs <= 0) 0.0 else (displayPositionMs.toDouble() / durationMs).coerceIn(0.0, 1.0)

    fun hovered(value: Boolean): PlaybackControlState = copy(isHovered = value)

    fun positionChanged(value: Long): PlaybackControlState =
        if (isDragging) this else copy(positionMs = value.coerceAtLeast(0))

    fun durationChanged(value: Long): PlaybackControlState = copy(durationMs = value.coerceAtLeast(0))

    fun playingChanged(value: Boolean): PlaybackControlState = copy(isPlaying = value)

    fun beginDrag(x: Double, trackWidth: Double): PlaybackControlState =
        if (!canSeek || trackWidth <= 0.0) this else copy(isDragging = true, pendingSeekMs = seekPositionFor(x, trackWidth))

    fun dragTo(x: Double, trackWidth: Double): PlaybackControlState =
        if (!isDragging || !canSeek || trackWidth <= 0.0) this else copy(pendingSeekMs = seekPositionFor(x, trackWidth))

    /** Returns the state after the drag and the position to seek to, or null when nothing was dragged. */
    fun endDrag(): Pair<PlaybackControlState, Long?> {
        if (!isDragging) return this to null
        val target = pendingSeekMs
        return copy(isDragging = false, pendingSeekMs = null, positionMs = target ?: positionMs) to target
    }

    fun seekPositionFor(x: Double, trackWidth: Double): Long {
        if (!canSeek || trackWidth <= 0.0) return 0
        val fraction = (x / trackWidth).coerceIn(0.0, 1.0)
        return (fraction * durationMs).roundToLong().coerceIn(0, durationMs)
    }
}
