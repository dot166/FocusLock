package io.github.dot166.focuslock.core

data class DurationOption(
    val label: String,
    val minutes: Long
) {
    override fun toString(): String {
        return label
    }
}

val durationOptions = listOf(
    DurationOption("Allowed", -1),
    DurationOption("Blocked", 0),
    DurationOption("5m", 5),
    DurationOption("10m", 10),
    DurationOption("15m", 15),
    DurationOption("30m", 30),
    DurationOption("1h", 60),
    DurationOption("1h 30m", 90),
    DurationOption("2h", 120),
    DurationOption("2h 30m", 150),
    DurationOption("3h", 180),
    DurationOption("3h 30m", 210),
    DurationOption("4h", 240),
    DurationOption("4h 30m", 270),
    DurationOption("5h", 300),
    DurationOption("5h 30m", 330),
    DurationOption("6h", 360),
    DurationOption("6h 30m", 390),
    DurationOption("7h", 420),
    DurationOption("7h 30m", 450)
)

fun findDuration(allowedTimeInMinutes: Long): DurationOption {
    for (i in 0 until durationOptions.size) {
        if (durationOptions[i].minutes == allowedTimeInMinutes) {
            return durationOptions[i]
        }
    }
    return durationOptions[0]
}

fun getDuration(position: Int): DurationOption {
    return durationOptions[position]
}

fun getPosition(durationOption: DurationOption): Int {
    for (i in 0 until durationOptions.size) {
        if (durationOptions[i] == durationOption) {
            return i
        }
    }
    return 0
}
