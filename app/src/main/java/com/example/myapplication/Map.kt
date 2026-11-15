package ru.chsu.studentmap

/**
 * Точки карты живут локально (сейчас — sampleMapPoints).
 * На карту список корпусов с бэка НЕ тянем.
 * TODO: при необходимости перенести sampleMapPoints в Room, не меняя OsmMapView.
 */
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
    val longitude: Double,
    // Адрес здания (если знаем)
    val address: String? = null,
    // Флаг, что это корпус или общежитие ЧГУ, а не случайная точка
    val isCampus: Boolean = false
)

val sampleMapPoints = listOf(
    MapPoint("1", "Корпус №1", PlaceCategory.BUILDINGS, 59.1203, 37.9304, isCampus = true),
    MapPoint("2", "Корпус №6", PlaceCategory.BUILDINGS, 59.1207, 37.9295, isCampus = true),
    MapPoint("3", "Общежитие №1", PlaceCategory.DORMITORIES, 59.1381, 37.9262, isCampus = true),
    MapPoint("4", "Общежитие №2", PlaceCategory.DORMITORIES, 59.1347, 37.8920, isCampus = true),
    MapPoint("5", "Студенческая столовая", PlaceCategory.FOOD, 59.1205, 37.9150, isCampus = true)
)
