package com.crossk.data.db

import androidx.room.TypeConverter
import org.json.JSONArray

/**
 * Room TypeConverters for non-primitive types used in entities.
 */
class Converters {

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return JSONArray(value).toString()
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        if (value.isBlank() || value == "[]") return emptyList()
        val arr = JSONArray(value)
        return (0 until arr.length()).map { arr.getString(it) }
    }
}
