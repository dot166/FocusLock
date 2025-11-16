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
    DurationOption("5 minutes", 5),
    DurationOption("10 minutes", 10),
    DurationOption("15 minutes", 15),
    DurationOption("30 minutes", 30),
    DurationOption("1 hour", 60),
    DurationOption("1 hour 30 minutes", 90),
    DurationOption("2 hours", 120),
    DurationOption("2 hours 30 minutes", 150),
    DurationOption("3 hours", 180),
    DurationOption("3 hours 30 minutes", 210),
    DurationOption("4 hours", 240),
    DurationOption("4 hours 30 minutes", 270),
    DurationOption("5 hours", 300),
    DurationOption("5 hours 30 minutes", 330),
    DurationOption("6 hours", 360),
    DurationOption("6 hours 30 minutes", 390),
    DurationOption("7 hours", 420),
    DurationOption("7 hours 30 minutes", 450)
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
