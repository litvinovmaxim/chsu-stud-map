package ru.chsu.studentmap.ui.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.chsu.studentmap.data.remote.dto.EventDto
import ru.chsu.studentmap.data.remote.dto.PlaceResponseDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritePlacesScreen(
    state: FavoritesUiState,
    onBack: () -> Unit,
    onRemove: (Int) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Избранные места") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        FavoritesBody(
            modifier = Modifier.padding(padding),
            loading = state.isLoading,
            error = state.error,
            empty = state.places.isEmpty(),
            emptyText = "Пока нет избранных мест"
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(state.places, key = { it.id }) { place ->
                    PlaceRow(place, onRemove)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyEventsScreen(
    state: FavoritesUiState,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мои события") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        FavoritesBody(
            modifier = Modifier.padding(padding),
            loading = state.isLoading,
            error = state.error,
            empty = state.events.isEmpty(),
            emptyText = "Вы ещё не добавили события"
        ) {
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                items(state.events, key = { it.id }) { event ->
                    EventRow(event)
                }
            }
        }
    }
}

@Composable
private fun PlaceRow(place: PlaceResponseDto, onRemove: (Int) -> Unit) {
    ListItem(
        headlineContent = { Text(place.name) },
        supportingContent = { Text(place.address ?: place.category.orEmpty()) },
        trailingContent = {
            IconButton(onClick = { onRemove(place.id) }) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить")
            }
        }
    )
}

@Composable
private fun EventRow(event: EventDto) {
    ListItem(
        headlineContent = { Text(event.title) },
        supportingContent = { Text(event.location ?: event.type.orEmpty()) }
    )
}

@Composable
private fun FavoritesBody(
    modifier: Modifier,
    loading: Boolean,
    error: String?,
    empty: Boolean,
    emptyText: String,
    content: @Composable () -> Unit
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            loading -> CircularProgressIndicator()
            error != null -> Text(error, modifier = Modifier.padding(24.dp), color = MaterialTheme.colorScheme.error)
            empty -> Text(emptyText, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> Box(Modifier.fillMaxWidth()) { content() }
        }
    }
}
