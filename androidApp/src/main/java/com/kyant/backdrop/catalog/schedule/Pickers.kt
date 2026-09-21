package com.kyant.backdrop.catalog.schedule

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import java.util.Calendar

fun showDatePicker(context: Context, initialEpochDay: Long, onPicked: (Long) -> Unit) {
    val (y, m, d) = epochDayToCivil(initialEpochDay)
    DatePickerDialog(
        context,
        { _, year, month, day ->
            onPicked(civilToEpochDay(year, month + 1, day))
        },
        y,
        m - 1,
        d
    ).show()
}

fun showTimePicker(context: Context, initial: String, onPicked: (String) -> Unit) {
    val minutes = minutesOf(initial) ?: 8 * 60
    TimePickerDialog(
        context,
        { _, hour, minute ->
            onPicked("%02d:%02d".format(hour, minute))
        },
        minutes / 60,
        minutes % 60,
        true
    ).show()
}

fun nowCalendar(): Calendar = Calendar.getInstance()
