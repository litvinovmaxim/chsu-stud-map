package ru.chsu.studentmap.ui.buildings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.chsu.studentmap.StudentMapApplication
import ru.chsu.studentmap.data.remote.dto.OsmPlaceDto
import ru.chsu.studentmap.data.remote.toUserMessage

enum class CampusCategory(val title: String, val apiValue: String) {
    BUILDINGS("Корпуса", "buildings"),
    DORMS("Общежития", "dormitories"),
    SPORTS("ФОК", "sports"),
    CAFES("Кафе", "cafes")
}

data class BuildingsUiState(
    val category: CampusCategory = CampusCategory.BUILDINGS,
    val query: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val places: List<OsmPlaceDto> = emptyList(),
    val selected: OsmPlaceDto? = null
) {
    val filtered: List<OsmPlaceDto>
        get() {
            val q = query.trim()
            if (q.isEmpty()) return places
            return places.filter {
                it.name.contains(q, ignoreCase = true) ||
                    it.address.orEmpty().contains(q, ignoreCase = true)
            }
        }
}

class BuildingsViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as StudentMapApplication).container

    private val _ui = MutableStateFlow(BuildingsUiState())
    val ui: StateFlow<BuildingsUiState> = _ui.asStateFlow()

    init {
        // Список корпусов — с бэка (OSM), не путать с точками на карте
        load()
    }

    fun onQueryChange(value: String) {
        _ui.update { it.copy(query = value) }
    }

    fun onCategorySelected(category: CampusCategory) {
        if (category == _ui.value.category) return
        _ui.update { it.copy(category = category, selected = null) }
        load()
    }

    fun select(place: OsmPlaceDto?) {
        _ui.update { it.copy(selected = place) }
    }

    fun load() {
        val category = _ui.value.category
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, error = null) }
            try {
                val places = container.placeRepository.getCampusPlaces(category.apiValue)
                _ui.update { it.copy(isLoading = false, places = places) }
            } catch (e: Exception) {
                _ui.update { it.copy(isLoading = false, error = e.toUserMessage(), places = emptyList()) }
            }
        }
    }

    fun addToFavorites(place: OsmPlaceDto, onDone: (String) -> Unit) {
        viewModelScope.launch {
            try {
                container.favoriteRepository.addPlace(place)
                onDone("Добавлено в избранное")
            } catch (e: Exception) {
                onDone(e.toUserMessage())
            }
        }
    }
}
