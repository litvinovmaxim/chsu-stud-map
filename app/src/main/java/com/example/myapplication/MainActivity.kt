package ru.chsu.studentmap

import android.Manifest
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import ru.chsu.studentmap.data.local.UserSession
import ru.chsu.studentmap.data.remote.toUserMessage
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
import ru.chsu.studentmap.ui.welcome.WelcomeScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Проверяем, показывался ли уже вступительный экран
        val prefs = getSharedPreferences("chsu_map_prefs", MODE_PRIVATE)
        val isWelcomeCompletedInitial = prefs.getBoolean("welcome_completed", false)

        setContent {
            MyApplicationTheme {
                // Если первый запуск — сначала показываем вступительный экран с рассказом о приложении
                // и запросом разрешения на геолокацию
                var welcomeCompleted by rememberSaveable { mutableStateOf(isWelcomeCompletedInitial) }

                if (!welcomeCompleted) {
                    WelcomeScreen(
                        onContinue = {
                            prefs.edit().putBoolean("welcome_completed", true).apply()
                            welcomeCompleted = true
                        }
                    )
                } else {
                    MyApplicationApp()
                }
            }
        }
    }
}

/**
 * Диалоговое окно для редактирования данных профиля.
 * Отправляет PUT /users/update/{userId} на бэкенд с измененными полями.
 */
@Composable
fun EditProfileDialog(
    initialName: String,
    initialSurname: String,
    initialEmail: String,
    onDismiss: () -> Unit,
    onSave: (name: String, surname: String, email: String, password: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var surname by remember { mutableStateOf(initialSurname) }
    var email by remember { mutableStateOf(initialEmail) }
    var newPassword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактирование профиля") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = surname,
                    onValueChange = { surname = it },
                    label = { Text("Фамилия") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Электронная почта") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("Новый пароль (оставьте пустым, если не меняете)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name, surname, email, newPassword)
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

/**
 * Экран профиля пользователя.
 *
 * Отображает:
 * 1. Карточку авторизованного пользователя с именем, фамилией, почтой и ID из базы данных бэкенда.
 * 2. Если пользователь не вошел — блок с кнопкой перехода на экран регистрации и входа.
 * 3. Кнопку "Редактировать" для отправки изменений через PUT /users/update/{userId}.
 * 4. Навигацию к разделам "Избранные места" и "Мои события".
 * 5. Кнопку выхода из аккаунта (POST /auth/logout).
 */
@Composable
fun ProfileScreen(
    session: UserSession,
    onLoginClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onFavoritePlacesClick: () -> Unit = {},
    onMyEventsClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (session.isLoggedIn) {
            // ==========================================
            // Состояние: ПОЛЬЗОВАТЕЛЬ АВТОРИЗОВАН
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Круглый аватар с инициалами пользователя
                        Surface(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = session.initials,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Имя, почта и ID пользователя
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = session.displayName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (!session.email.isNullOrBlank()) {
                                Text(
                                    text = session.email,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (session.userId != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "ID аккаунта: #${session.userId}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Кнопка редактирования данных профиля
                    OutlinedButton(
                        onClick = onEditProfileClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Редактировать данные профиля")
                    }
                }
            }
        } else {
            // ==========================================
            // Состояние: ПОЛЬЗОВАТЕЛЬ НЕ АВТОРИЗОВАН
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Вы не авторизованы",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Войдите или зарегистрируйтесь, чтобы сохранять избранное и синхронизировать профиль с сервером",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Главная кнопка перехода к форме входа/регистрации
                    Button(
                        onClick = onLoginClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Login,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Войти или Зарегистрироваться")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Пункты меню: Избранные места и Мои события
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp)
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

        Spacer(modifier = Modifier.height(20.dp))

        // Кнопка выхода из системы (видна только авторизованным пользователям)
        if (session.isLoggedIn) {
            OutlinedButton(
                onClick = onLogoutClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Выйти из аккаунта")
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
    // Храним состояние текущей вкладки нижней навигации
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.MAP) }
    // Подэкран вкладки профиля (меню, авторизация, избранные места, мои события)
    var profileSubScreen by rememberSaveable { mutableStateOf(ProfileSubScreen.MENU) }

    // Состояние открытия диалога редактирования профиля
    var showEditProfileDialog by remember { mutableStateOf(false) }
    // Координаты точки, на которую нужно навести карту (например, когда нажали на избранное место в профиле)
    var targetMapLocation by remember { mutableStateOf<GeoPoint?>(null) }

    val session by authViewModel.session.collectAsStateWithLifecycle()
    val authForm by authViewModel.form.collectAsStateWithLifecycle()
    val buildingsState by buildingsViewModel.ui.collectAsStateWithLifecycle()
    val eventsState by eventsViewModel.ui.collectAsStateWithLifecycle()
    val favoritesState by favoritesViewModel.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = context.applicationContext as StudentMapApplication
    val container = app.container

    // При успешной авторизации возвращаемся из экрана входа/регистрации в меню профиля
    LaunchedEffect(session.isLoggedIn) {
        if (session.isLoggedIn && profileSubScreen == ProfileSubScreen.AUTH) {
            profileSubScreen = ProfileSubScreen.MENU
        }
    }

    // Загрузка списков избранного при переходе на соответствующие экраны
    LaunchedEffect(profileSubScreen) {
        when (profileSubScreen) {
            ProfileSubScreen.FAVORITES -> favoritesViewModel.loadPlaces()
            ProfileSubScreen.MY_EVENTS -> favoritesViewModel.loadMyEvents()
            ProfileSubScreen.MENU -> {
                // Если пользователь авторизован, обновляем данные профиля с бэкенда
                if (session.isLoggedIn) {
                    authViewModel.refreshProfile()
                }
            }
            else -> Unit
        }
    }

    LaunchedEffect(currentDestination, session.isLoggedIn) {
        if (session.isLoggedIn && (currentDestination == AppDestinations.LIST || currentDestination == AppDestinations.MAP)) {
            buildingsViewModel.refreshFavorites()
            favoritesViewModel.loadPlaces()
        }
    }

    // Диалог редактирования профиля
    if (showEditProfileDialog) {
        EditProfileDialog(
            initialName = session.name.orEmpty(),
            initialSurname = session.surname.orEmpty(),
            initialEmail = session.email.orEmpty(),
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, surname, email, password ->
                showEditProfileDialog = false
                authViewModel.updateProfile(
                    name = name,
                    surname = surname,
                    email = email,
                    password = password.ifBlank { null }
                ) { success, message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    val navSuiteItemColors = NavigationSuiteDefaults.itemColors(
        navigationBarItemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        navigationRailItemColors = NavigationRailItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )

    // Контейнер с навигацией
    NavigationSuiteScaffold(
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = MaterialTheme.colorScheme.surface,
            navigationBarContentColor = MaterialTheme.colorScheme.onSurface
        ),
        navigationSuiteItems = {
            AppDestinations.entries.forEach { destination ->
                val isSelected = destination == currentDestination
                item(
                    icon = {
                        Icon(
                            painter = painterResource(destination.iconRes),
                            contentDescription = destination.label,
                            modifier = Modifier.size(26.dp)
                        )
                    },
                    label = {
                        Text(
                            text = destination.label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    selected = isSelected,
                    onClick = { currentDestination = destination },
                    colors = navSuiteItemColors
                )
            }
        }
    ) {
        // Содержимое вкладок
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
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
                                    onTogglePasswordVisibility = authViewModel::togglePasswordVisibility,
                                    onToggleMode = authViewModel::toggleMode,
                                    onSubmit = authViewModel::submit,
                                    onBack = { profileSubScreen = ProfileSubScreen.MENU }
                                )
                            }

                            ProfileSubScreen.FAVORITES -> {
                                BackHandler { profileSubScreen = ProfileSubScreen.MENU }
                                FavoritePlacesScreen(
                                    state = favoritesState,
                                    onBack = { profileSubScreen = ProfileSubScreen.MENU },
                                    onRemove = favoritesViewModel::removePlace,
                                    // Кликнули по месту — летим на карту к этим координатам
                                    onPlaceClick = { place ->
                                        targetMapLocation = GeoPoint(place.latitude, place.longitude)
                                        currentDestination = AppDestinations.MAP
                                    }
                                )
                            }

                            ProfileSubScreen.MY_EVENTS -> {
                                BackHandler { profileSubScreen = ProfileSubScreen.MENU }
                                MyEventsScreen(
                                    state = favoritesState,
                                    onBack = { profileSubScreen = ProfileSubScreen.MENU },
                                    // Убираем/добавляем событие в избранное и обновляем список
                                    onToggleFavorite = { event ->
                                        eventsViewModel.toggleFavorite(event) { message ->
                                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                            favoritesViewModel.loadMyEvents()
                                        }
                                    }
                                )
                            }

                            ProfileSubScreen.MENU -> {
                                ProfileScreen(
                                    session = session,
                                    onLoginClick = {
                                        profileSubScreen = ProfileSubScreen.AUTH
                                    },
                                    onLogoutClick = {
                                        authViewModel.logout()
                                        Toast.makeText(context, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show()
                                    },
                                    onEditProfileClick = {
                                        showEditProfileDialog = true
                                    },
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
                            }
                        }
                    }

                    AppDestinations.MAP -> {
                        OsmMapView(
                            targetLocation = targetMapLocation,
                            onTargetLocationHandled = { targetMapLocation = null },
                            favoritePlaces = favoritesState.places,
                            onAddFavorite = { placeDto, callback ->
                                if (!session.isLoggedIn) {
                                    callback("Войдите в аккаунт")
                                } else {
                                    scope.launch {
                                        try {
                                            container.favoriteRepository.addPlace(placeDto)
                                            buildingsViewModel.refreshFavorites()
                                            favoritesViewModel.loadPlaces()
                                            callback("Добавлено в избранное")
                                        } catch (e: Exception) {
                                            callback(e.toUserMessage())
                                        }
                                    }
                                }
                            },
                            onRemoveFavorite = { placeId, callback ->
                                scope.launch {
                                    try {
                                        container.favoriteRepository.removePlace(placeId)
                                        buildingsViewModel.refreshFavorites()
                                        favoritesViewModel.loadPlaces()
                                        callback("Удалено из избранного")
                                    } catch (e: Exception) {
                                        callback(e.toUserMessage())
                                    }
                                }
                            }
                        )
                    }

                    AppDestinations.LIST -> {
                        val selectedBuilding = buildingsState.selected
                        if (selectedBuilding == null) {
                            BuildingsScreen(
                                state = buildingsState,
                                onQueryChange = buildingsViewModel::onQueryChange,
                                onCategorySelected = buildingsViewModel::onCategorySelected,
                                onPlaceClick = buildingsViewModel::select,
                                onToggleFavorite = { place ->
                                    if (!session.isLoggedIn) {
                                        Toast.makeText(context, "Войдите в аккаунт", Toast.LENGTH_SHORT).show()
                                    } else {
                                        buildingsViewModel.toggleFavorite(place) { message ->
                                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onRetry = buildingsViewModel::load
                            )
                        } else {
                            BackHandler { buildingsViewModel.select(null) }
                            BuildingDetailScreen(
                                place = selectedBuilding,
                                isFavorite = buildingsState.isFavorite(selectedBuilding),
                                onBack = { buildingsViewModel.select(null) },
                                onAddFavorite = { place, callback ->
                                    if (!session.isLoggedIn) {
                                        callback("Войдите в аккаунт")
                                    } else {
                                        buildingsViewModel.toggleFavorite(place, callback)
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

// Список всех доступных вкладок нижней панели навигации
enum class AppDestinations(val label: String) {
    PROFILE("Профиль"),
    MAP("Карта"),
    LIST("Корпуса"),
    EVENTS("События");

    // Возвращает нужную иконку для каждой вкладки
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
        ProfileScreen(
            session = UserSession(
                accessToken = "sample_token",
                userId = 1,
                name = "Иван",
                surname = "Иванов",
                email = "ivan@example.com"
            )
        )
    }
}
