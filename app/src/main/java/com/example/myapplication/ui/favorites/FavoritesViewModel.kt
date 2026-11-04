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

    fun loadPlaces() {
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, error = null) }
            try {
                val places = container.favoriteRepository.getPlaces()
                _ui.update { it.copy(isLoading = false, places = places) }
            } catch (e: Exception) {
                _ui.update { it.copy(isLoading = false, error = e.toUserMessage()) }
            }
        }
    }

    fun loadMyEvents() {
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, error = null) }
            try {
                val session = container.sessionStore.session.first()
                val userId = session.userId
                    ?: throw IllegalStateException("Войдите в аккаунт")
                val ids = container.favoriteRepository.getEventIds(userId)
                val catalog = container.eventRepository.getEvents().first
                _ui.update {
                    it.copy(
                        isLoading = false,
                        events = catalog.filter { event -> event.id in ids }
                    )
                }
            } catch (e: Exception) {
                _ui.update { it.copy(isLoading = false, error = e.toUserMessage()) }
            }
        }
    }

    fun removePlace(id: Int) {
        viewModelScope.launch {
            runCatching { container.favoriteRepository.removePlace(id) }
            loadPlaces()
        }
    }
}
