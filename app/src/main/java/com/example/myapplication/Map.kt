package ru.chsu.studentmap

enum class PlaceCategory(val title: String) {
    ALL("Все"),
    BUILDINGS("Корпуса"),
    DORMITORIES("Общежития"),
    FOOD("Быстрое питание"),
    OTHER("Другое")
}

data class MapPoint(
    val id: String,
    val title: String,
    val category: PlaceCategory,
    val latitude: Double,
    val longitude: Double
)

val sampleMapPoints = listOf(
    MapPoint("1", "Главный корпус ЧГУ", PlaceCategory.BUILDINGS, 59.1235, 37.9290),
    MapPoint("2", "Корпус на Горького", PlaceCategory.BUILDINGS, 59.1252, 37.9220),
    MapPoint("3", "Общежитие №1", PlaceCategory.DORMITORIES, 59.1280, 37.9330),
    MapPoint("4", "Общежитие №2", PlaceCategory.DORMITORIES, 59.1265, 37.9325),
    MapPoint("5", "Студенческая столовая", PlaceCategory.FOOD, 59.1205, 37.9150)
)
