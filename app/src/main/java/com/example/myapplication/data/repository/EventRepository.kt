package ru.chsu.studentmap.data.repository

import ru.chsu.studentmap.data.remote.StudMapApi
import ru.chsu.studentmap.data.remote.dto.EventDto

class EventRepository(private val api: StudMapApi) {

    /**
     * Пытаемся взять события с бэка. Если /events ещё нет отдаём из макета.
     * TODO: убрать sampleEvents(), когда event_router заработает.
     */
    suspend fun getEvents(): Pair<List<EventDto>, Boolean> {
        return try {
            api.getEvents() to true
        } catch (_: Exception) {
            sampleEvents() to false
        }
    }

    fun sampleEvents(): List<EventDto> = listOf(
        EventDto(
            id = 1,
            title = "День факультета",
            type = "Концерт",
            eventDate = "2026-04-12T18:00:00",
            location = "Корпус 1, актовый зал",
            description = "Ежегодный праздничный концерт и развлекательная программа."
        ),
        EventDto(
            id = 2,
            title = "Битва общежитий",
            type = "Спорт",
            eventDate = "2026-04-18T15:00:00",
            location = "Стадион ЧГУ",
            description = "Спортивно-развлекательные соревнования между общежитиями."
        ),
        EventDto(
            id = 3,
            title = "Ярмарка",
            type = "Карьера",
            eventDate = "2026-03-31T09:00:00",
            location = "МБУК «Дворец химиков», ул. Победы, 100, Череповец",
            description = "Студенческая ярмарка проектов и вакансий. Масштабная интерактивная площадка, объединяющая студентов, работодателей и молодежные объединения. На ярмарке представляются стенды крупных компаний, где можно пройти экспресс-собеседование, узнать о вакансиях и стажировках, а также получить советы по трудоустройству."
        ),
        EventDto(
            id = 4,
            title = "Хакатон",
            type = "Наука",
            eventDate = "2026-05-16T10:00:00",
            location = "Корпус 6",
            description = "Командный IT-марафон по разработке цифровых продуктов."
        ),
        EventDto(
            id = 5,
            title = "Студенческая весна",
            type = "Концерт",
            eventDate = "2026-04-25T17:00:00",
            location = "ДК металлургов",
            description = "Главный творческий фестиваль и конкурс талантов вуза."
        ),
        EventDto(
            id = 6,
            title = "Киберспортивный турнир",
            type = "Спорт",
            eventDate = "2026-05-02T12:00:00",
            location = "Коворкинг ЧГУ",
            description = "Соревнования по популярным дисциплинам на большом экране."
        ),
        EventDto(
            id = 7,
            title = "Ночь кино",
            type = "Культура",
            eventDate = "2026-04-04T21:00:00",
            location = "Кинозал корпуса 1",
            description = "Марафон культовых фильмов на большом экране до утра."
        )
    )
}
