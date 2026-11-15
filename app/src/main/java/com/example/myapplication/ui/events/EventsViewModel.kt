package ru.chsu.studentmap.ui.events

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.chsu.studentmap.StudentMapApplication
import ru.chsu.studentmap.data.remote.dto.EventDto
import ru.chsu.studentmap.data.remote.toUserMessage

data class EventsUiState(
    val query: String = "",
    val filter: String = ALL_FILTER,
    val isLoading: Boolean = false,
    val fromBackend: Boolean = false,
    val error: String? = null,
    val events: List<EventDto> = emptyList(),
    val selected: EventDto? = null,
    val favoriteIds: Set<Int> = emptySet()
) {
    val filters: List<String>
        get() = listOf(ALL_FILTER) + events.mapNotNull { it.type }.distinct()

    val filtered: List<EventDto>
        get() {
            val byType = if (filter == ALL_FILTER) events else events.filter { it.type == filter }
            val q = query.trim()
            if (q.isEmpty()) return byType
            return byType.filter {
                it.title.contains(q, ignoreCase = true) ||
                    it.description.orEmpty().contains(q, ignoreCase = true)
            }
        }

    companion object {
        const val ALL_FILTER = "Все"
    }
}

class EventsViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as StudentMapApplication).container

    private val _ui = MutableStateFlow(EventsUiState())
    val ui: StateFlow<EventsUiState> = _ui.asStateFlow()

    init {
        load()
    }

    fun onQueryChange(value: String) = _ui.update { it.copy(query = value) }

    fun onFilterChange(value: String) = _ui.update { it.copy(filter = value) }

    fun select(event: EventDto?) = _ui.update { it.copy(selected = event) }

    // Загружаем все мероприятия с сервера
    fun load() {
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, error = null) }
            try {
                // Запрашиваем актуальный список с бэкенда
                val events = container.eventRepository.getEvents()
                val favIds = events.filter { it.isFavorite == true }.map { it.id }.toSet()
                _ui.update {
                    it.copy(
                        isLoading = false,
                        events = events,
                        fromBackend = true,
                        favoriteIds = if (favIds.isNotEmpty()) favIds else it.favoriteIds,
                        error = null
                    )
                }
                // Если пользователь авторизован, подтягиваем его избранные события
                refreshFavorites()
            } catch (e: Exception) {
                _ui.update {
                    it.copy(
                        isLoading = false,
                        events = emptyList(),
                        fromBackend = false,
                        error = e.toUserMessage()
                    )
                }
            }
        }
    }

    // Добавляем или удаляем мероприятие из "Моих событий"
    fun toggleFavorite(event: EventDto, onMessage: (String) -> Unit) {
        viewModelScope.launch {
            // Без аккаунта сохранять нельзя — просим войти
            val session = container.sessionStore.session.first()
            if (!session.isLoggedIn) {
                onMessage("Войдите в аккаунт, чтобы сохранять события")
                return@launch
            }
            try {
                val already = event.id in _ui.value.favoriteIds
                if (already) {
                    container.favoriteRepository.removeEvent(event.id)
                } else {
                    container.favoriteRepository.addEvent(event.id)
                }
                refreshFavorites()
                onMessage(if (already) "Удалено из моих событий" else "Добавлено в мои события")
            } catch (e: Exception) {
                onMessage(e.toUserMessage())
            }
        }
    }

    // Обновляем список id избранных событий авторизованного пользователя
    private suspend fun refreshFavorites() {
        val session = container.sessionStore.session.first()
        if (!session.isLoggedIn) return
        runCatching {
            container.favoriteRepository.getEventIds()
        }.onSuccess { ids ->
            _ui.update { it.copy(favoriteIds = ids) }
        }
    }
}
