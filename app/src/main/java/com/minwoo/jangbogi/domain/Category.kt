package com.minwoo.jangbogi.domain

enum class Category(val display: String, val emoji: String) {
    VEGETABLE("채소", "🥬"),
    FRUIT("과일", "🍎"),
    MEAT_EGG("정육·계란", "🥩"),
    SEAFOOD("수산", "🐟"),
    DAIRY("유제품", "🥛"),
    FROZEN("냉동·간편식", "🧊"),
    SNACK("과자·간식", "🍪"),
    BEVERAGE("음료", "🧃"),
    SEASONING("조미료·소스", "🧂"),
    HOUSEHOLD("생활용품", "🧻"),
    ETC("기타", "🛒")
}
