package ru.chsu.studentmap.ui.favorites

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
import ru.chsu.studentmap.data.remote.dto.PlaceResponseDto
import ru.chsu.studentmap.data.remote.toUserMessage

data class FavoritesUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val places: List<PlaceResponseDto> = emptyList(),
    val events: List<EventDto> = emptyList()
)

class FavoritesViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as StudentMapApplication).container

    private val _ui = MutableStateFlow(FavoritesUiState())
    val ui: StateFlow<FavoritesUiState> = _ui.asStateFlow()

    // Загружаем список избранных мест с сервера
    fun loadPlaces() {
        viewModelScope.launch {
            val session = container.sessionStore.session.first()
            if (!session.isLoggedIn) {
                _ui.update { it.copy(isLoading = false, places = emptyList(), error = null) }
                return@launch
            }
            _ui.update { it.copy(isLoading = true, error = null) }
            try {
                val places = container.favoriteRepository.getPlaces()
                _ui.update { it.copy(isLoading = false, places = places) }
            } catch (e: Exception) {
                _ui.update { it.copy(isLoading = false, error = e.toUserMessage()) }
            }
        }
    }

    // Загружаем сохранённые мероприятия пользователя
    fun loadMyEvents() {
        viewModelScope.launch {
            val session = container.sessionStore.session.first()
            if (!session.isLoggedIn) {
                _ui.update { it.copy(isLoading = false, events = emptyList(), error = "Войдите в аккаунт") }
                return@launch
            }
            _ui.update { it.copy(isLoading = true, error = null) }
            try {
                val myEvents = container.favoriteRepository.getFavoriteEvents()
                _ui.update {
                    it.copy(
                        isLoading = false,
                        events = myEvents
                    )
                }
            } catch (e: Exception) {
                _ui.update { it.copy(isLoading = false, error = e.toUserMessage()) }
            }
        }
    }

    // Удаляем место из избранного на сервере
    fun removePlace(id: Int) {
        viewModelScope.launch {
            _ui.update { state -> state.copy(places = state.places.filter { it.id != id }) }
            try {
                container.favoriteRepository.removePlace(id)
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.toUserMessage()) }
                loadPlaces()
            }
        }
    }
}
