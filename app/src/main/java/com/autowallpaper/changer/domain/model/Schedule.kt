package com.autowallpaper.changer.domain.model

data class Schedule(
    val id: Long = 0,
    val isEnabled: Boolean = true,
    val intervalMinutes: Int = 30,
    val isCustomInterval: Boolean = false,
    val customIntervalMinutes: Int? = null,
    val timeSlots: List<TimeSlot> = emptyList(),
    val weekdayMask: Int = 0b1111111, // Sun-Sat, all enabled by default
    val shuffleEnabled: Boolean = true,
    val changeLockScreen: Boolean = true,
    val changeHomeScreen: Boolean = true
)

data class TimeSlot(
    val id: Long = 0,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val folderUri: String, // SAF URI for the image folder
    val label: String = ""
)
