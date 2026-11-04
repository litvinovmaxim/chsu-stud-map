package ru.chsu.studentmap

import android.Manifest
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.ui.theme.MyApplicationTheme
import ru.chsu.studentmap.ui.auth.AuthScreen
import ru.chsu.studentmap.ui.auth.AuthViewModel
import ru.chsu.studentmap.ui.buildings.BuildingDetailScreen
import ru.chsu.studentmap.ui.buildings.BuildingsScreen
import ru.chsu.studentmap.ui.buildings.BuildingsViewModel
import ru.chsu.studentmap.ui.events.EventDetailScreen
import ru.chsu.studentmap.ui.events.EventsScreen
import ru.chsu.studentmap.ui.events.EventsViewModel
import ru.chsu.studentmap.ui.favorites.FavoritePlacesScreen
import ru.chsu.studentmap.ui.favorites.FavoritesViewModel
import ru.chsu.studentmap.ui.favorites.MyEventsScreen

class MainActivity : ComponentActivity() {

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {         }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request Location permission
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            println("Location Permission GRANTED")
        } else {
            locationPermissionLauncher.launch(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        setContent {
            MyApplicationTheme {
                MyApplicationApp()
            }
        }
    }
}

@Composable
fun ProfileScreen(
    userName: String = "Иван Иванов",
    userEmail: String = "ivan@example.com",
    onFavoritePlacesClick: () -> Unit = {},
    onMyEventsClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Карточка пользователя / Аватарка и Имя
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Аватар-заглушка
                Surface(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Аватар",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = userEmail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Пункты меню
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                ListItem(
                    headlineContent = { Text("Избранные места") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable { onFavoritePlacesClick() }
                )

                HorizontalDivider()

                ListItem(
                    headlineContent = { Text("Мои события") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable { onMyEventsClick() }
                )
            }
        }
    }
}

private enum class ProfileSubScreen { MENU, AUTH, FAVORITES, MY_EVENTS }

@Composable
fun MyApplicationApp(
    authViewModel: AuthViewModel = viewModel(),
    buildingsViewModel: BuildingsViewModel = viewModel(),
    eventsViewModel: EventsViewModel = viewModel(),
    favoritesViewModel: FavoritesViewModel = viewModel()
) {
    //Храним состояние текущей вкладки
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.MAP) }
    var profileSubScreen by rememberSaveable { mutableStateOf(ProfileSubScreen.MENU) }

    val session by authViewModel.session.collectAsStateWithLifecycle()
    val authForm by authViewModel.form.collectAsStateWithLifecycle()
    val buildingsState by buildingsViewModel.ui.collectAsStateWithLifecycle()
    val eventsState by eventsViewModel.ui.collectAsStateWithLifecycle()
    val favoritesState by favoritesViewModel.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(session.isLoggedIn) {
        if (session.isLoggedIn && profileSubScreen == ProfileSubScreen.AUTH) {
            profileSubScreen = ProfileSubScreen.MENU
        }
    }

    LaunchedEffect(profileSubScreen) {
        when (profileSubScreen) {
            ProfileSubScreen.FAVORITES -> favoritesViewModel.loadPlaces()
            ProfileSubScreen.MY_EVENTS -> favoritesViewModel.loadMyEvents()
            else -> Unit
        }
    }

    //Контейнер с навигацией
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach { destination ->
                item(
                    icon = {
                        Icon(
                            painterResource(destination.iconRes),
                            contentDescription = destination.label,
                            modifier = Modifier.size(35.dp)
                        )
                    },
                    label = { Text(destination.label) },
                    selected = destination == currentDestination,
                    onClick = { currentDestination = destination }
                )
            }
        }
    ) {
        //Вкладки
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                //Переключаем содержимое экрана
                when (currentDestination) {
                    AppDestinations.PROFILE -> {
                        when (profileSubScreen) {
                            ProfileSubScreen.AUTH -> {
                                BackHandler { profileSubScreen = ProfileSubScreen.MENU }
                                AuthScreen(
                                    state = authForm,
                                    onName = authViewModel::onName,
                                    onSurname = authViewModel::onSurname,
                                    onEmail = authViewModel::onEmail,
                                    onPassword = authViewModel::onPassword,
                                    onToggleMode = authViewModel::toggleMode,
                                    onSubmit = authViewModel::submit
                                )
                            }

                            ProfileSubScreen.FAVORITES -> {
                                BackHandler { profileSubScreen = ProfileSubScreen.MENU }
                                FavoritePlacesScreen(
                                    state = favoritesState,
                                    onBack = { profileSubScreen = ProfileSubScreen.MENU },
                                    onRemove = favoritesViewModel::removePlace
                                )
                            }

                            ProfileSubScreen.MY_EVENTS -> {
                                BackHandler { profileSubScreen = ProfileSubScreen.MENU }
                                MyEventsScreen(
                                    state = favoritesState,
                                    onBack = { profileSubScreen = ProfileSubScreen.MENU }
                                )
                            }

                            ProfileSubScreen.MENU -> {
                                Column {
                                    ProfileScreen(
                                        userName = session.displayName,
                                        userEmail = session.email ?: "",
                                        onFavoritePlacesClick = {
                                            profileSubScreen = if (session.isLoggedIn) {
                                                ProfileSubScreen.FAVORITES
                                            } else {
                                                ProfileSubScreen.AUTH
                                            }
                                        },
                                        onMyEventsClick = {
                                            profileSubScreen = if (session.isLoggedIn) {
                                                ProfileSubScreen.MY_EVENTS
                                            } else {
                                                ProfileSubScreen.AUTH
                                            }
                                        }
                                    )
                                    if (session.isLoggedIn) {
                                        TextButton(
                                            onClick = { authViewModel.logout() },
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        ) {
                                            Text("Выход")
                                        }
                                    } else {
                                        TextButton(
                                            onClick = { profileSubScreen = ProfileSubScreen.AUTH },
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        ) {
                                            Text("Войти")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    AppDestinations.MAP -> {
                        OsmMapView()
                    }

                    AppDestinations.LIST -> {
                        val selectedBuilding = buildingsState.selected
                        if (selectedBuilding == null) {
                            BuildingsScreen(
                                state = buildingsState,
                                onQueryChange = buildingsViewModel::onQueryChange,
                                onCategorySelected = buildingsViewModel::onCategorySelected,
                                onPlaceClick = buildingsViewModel::select,
                                onRetry = buildingsViewModel::load
                            )
                        } else {
                            BackHandler { buildingsViewModel.select(null) }
                            BuildingDetailScreen(
                                place = selectedBuilding,
                                onBack = { buildingsViewModel.select(null) },
                                onAddFavorite = { place, callback ->
                                    if (!session.isLoggedIn) {
                                        callback("Войдите в аккаунт")
                                    } else {
                                        buildingsViewModel.addToFavorites(place, callback)
                                    }
                                }
                            )
                        }
                    }

                    AppDestinations.EVENTS -> {
                        val selectedEvent = eventsState.selected
                        if (selectedEvent == null) {
                            EventsScreen(
                                state = eventsState,
                                onQueryChange = eventsViewModel::onQueryChange,
                                onFilterChange = eventsViewModel::onFilterChange,
                                onEventClick = eventsViewModel::select
                            )
                        } else {
                            BackHandler { eventsViewModel.select(null) }
                            EventDetailScreen(
                                event = selectedEvent,
                                isFavorite = selectedEvent.id in eventsState.favoriteIds,
                                onBack = { eventsViewModel.select(null) },
                                onToggleFavorite = {
                                    eventsViewModel.toggleFavorite(selectedEvent) { message ->
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}


//Список всех доступных вкладок
enum class AppDestinations(val label: String) {
    PROFILE("Профиль"),
    MAP("Карта"),
    LIST("Корпуса"),
    EVENTS("События");


    //Возвращает нужную иконку для каждой вкладки
    val iconRes: Int
        get() = when (this) {
            PROFILE -> R.drawable.ic_account_box
            MAP -> R.drawable.ic_map
            LIST -> R.drawable.ic_school
            EVENTS -> R.drawable.ic_calendar
        }
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    MyApplicationTheme {
        ProfileScreen()
    }
}