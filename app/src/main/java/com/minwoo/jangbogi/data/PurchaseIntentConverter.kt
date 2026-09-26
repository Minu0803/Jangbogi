package com.minwoo.jangbogi.data

import androidx.room.TypeConverter
import com.minwoo.jangbogi.domain.PurchaseIntent

class PurchaseIntentConverter {
    @TypeConverter fun fromIntent(value: PurchaseIntent): String = value.name
    @TypeConverter fun toIntent(value: String): PurchaseIntent =
        PurchaseIntent.entries.firstOrNull { it.name == value } ?: PurchaseIntent.BUY
}
