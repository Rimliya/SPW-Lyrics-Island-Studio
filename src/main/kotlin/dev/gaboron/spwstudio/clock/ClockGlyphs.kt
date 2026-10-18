package dev.gaboron.spwstudio.clock

enum class SevenSegment { A, B, C, D, E, F, G }

object SevenSegmentGlyphs {
    private val patterns = mapOf(
        '0' to setOf(SevenSegment.A, SevenSegment.B, SevenSegment.C, SevenSegment.D, SevenSegment.E, SevenSegment.F),
        '1' to setOf(SevenSegment.B, SevenSegment.C),
        '2' to setOf(SevenSegment.A, SevenSegment.B, SevenSegment.G, SevenSegment.E, SevenSegment.D),
        '3' to setOf(SevenSegment.A, SevenSegment.B, SevenSegment.C, SevenSegment.D, SevenSegment.G),
        '4' to setOf(SevenSegment.F, SevenSegment.G, SevenSegment.B, SevenSegment.C),
        '5' to setOf(SevenSegment.A, SevenSegment.F, SevenSegment.G, SevenSegment.C, SevenSegment.D),
        '6' to setOf(SevenSegment.A, SevenSegment.F, SevenSegment.E, SevenSegment.D, SevenSegment.C, SevenSegment.G),
        '7' to setOf(SevenSegment.A, SevenSegment.B, SevenSegment.C),
        '8' to setOf(SevenSegment.A, SevenSegment.B, SevenSegment.C, SevenSegment.D, SevenSegment.E, SevenSegment.F, SevenSegment.G),
        '9' to setOf(SevenSegment.A, SevenSegment.B, SevenSegment.C, SevenSegment.D, SevenSegment.F, SevenSegment.G),
        '-' to setOf(SevenSegment.G),
    )

    fun segmentsFor(character: Char): Set<SevenSegment> = patterns[character].orEmpty()
}

object DotMatrixGlyphs {
    private val patterns = mapOf(
        '0' to listOf("01110", "10001", "10011", "10101", "11001", "10001", "01110"),
        '1' to listOf("00100", "01100", "00100", "00100", "00100", "00100", "01110"),
        '2' to listOf("01110", "10001", "00001", "00010", "00100", "01000", "11111"),
        '3' to listOf("11110", "00001", "00001", "01110", "00001", "00001", "11110"),
        '4' to listOf("00010", "00110", "01010", "10010", "11111", "00010", "00010"),
        '5' to listOf("11111", "10000", "11110", "00001", "00001", "10001", "01110"),
        '6' to listOf("00110", "01000", "10000", "11110", "10001", "10001", "01110"),
        '7' to listOf("11111", "00001", "00010", "00100", "01000", "01000", "01000"),
        '8' to listOf("01110", "10001", "10001", "01110", "10001", "10001", "01110"),
        '9' to listOf("01110", "10001", "10001", "01111", "00001", "00010", "01100"),
        ':' to listOf("00000", "00100", "00100", "00000", "00100", "00100", "00000"),
    )

    fun patternFor(character: Char): List<String> = patterns[character].orEmpty()
}

object PixelGlyphs {
    private val patterns = mapOf(
        '0' to listOf("111", "101", "101", "101", "111"),
        '1' to listOf("010", "110", "010", "010", "111"),
        '2' to listOf("111", "001", "111", "100", "111"),
        '3' to listOf("111", "001", "111", "001", "111"),
        '4' to listOf("101", "101", "111", "001", "001"),
        '5' to listOf("111", "100", "111", "001", "111"),
        '6' to listOf("111", "100", "111", "101", "111"),
        '7' to listOf("111", "001", "010", "010", "010"),
        '8' to listOf("111", "101", "111", "101", "111"),
        '9' to listOf("111", "101", "111", "001", "111"),
        ':' to listOf("0", "1", "0", "1", "0"),
    )

    fun patternFor(character: Char): List<String> = patterns[character].orEmpty()
}
