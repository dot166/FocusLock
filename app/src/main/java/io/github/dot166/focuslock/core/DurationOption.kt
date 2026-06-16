package io.github.dot166.focuslock.core

import com.android.settingslib.spa.framework.common.SpaEnvironmentFactory
import io.github.dot166.focuslock.R

data class DurationOption(
    val label: String,
    val minutes: Long
) {
    override fun toString(): String {
        return label
    }
}

val durationOptions = listOf(
    DurationOption(SpaEnvironmentFactory.instance.appContext.getString(R.string.allowed), -1),
    DurationOption(SpaEnvironmentFactory.instance.appContext.getString(R.string.blocked), 0),
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
    for (element in durationOptions) {
        if (element.minutes == allowedTimeInMinutes) {
            return element
        }
    }
    return durationOptions[0]
}

fun getDuration(position: Int): DurationOption {
    return durationOptions[position]
}

fun getPosition(durationOption: DurationOption): Int {
    for ((i, element) in durationOptions.withIndex()) {
        if (element == durationOption) {
            return i
        }
    }
    return 0
}
