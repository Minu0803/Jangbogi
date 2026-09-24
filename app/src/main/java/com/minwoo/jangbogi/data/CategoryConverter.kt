package com.minwoo.jangbogi.data

import androidx.room.TypeConverter
import com.minwoo.jangbogi.domain.Category

class CategoryConverter {

    @TypeConverter
    fun fromCategory(value: Category): String = value.name

    @TypeConverter
    fun toCategory(value: String): Category =
        Category.entries.firstOrNull { it.name == value } ?: Category.ETC
}
