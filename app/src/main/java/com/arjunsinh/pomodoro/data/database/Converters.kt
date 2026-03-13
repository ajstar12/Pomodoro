package com.arjunsinh.pomodoro.data.database

import androidx.room.TypeConverter
import com.arjunsinh.pomodoro.utils.Constants

class Converters {
    @TypeConverter
    fun fromSessionType(value: Constants.SessionType): String {
        return value.name
    }

    @TypeConverter
    fun toSessionType(value: String): Constants.SessionType {
        return Constants.SessionType.valueOf(value)
    }
}
