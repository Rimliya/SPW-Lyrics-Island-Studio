package dev.gaboron.spwstudio.clock

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object ClockFormat {
    fun format(
        epochMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
        use24Hour: Boolean = true,
        showSeconds: Boolean = false,
    ): String {
        val pattern = when {
            use24Hour && showSeconds -> "HH:mm:ss"
            use24Hour -> "HH:mm"
            showSeconds -> "hh:mm:ss"
            else -> "hh:mm"
        }
        return DateTimeFormatter.ofPattern(pattern, Locale.ROOT)
            .withZone(zone)
            .format(Instant.ofEpochMilli(epochMillis))
    }
}
