package ru.chsu.studentmap

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.modules.OfflineTileProvider
import org.osmdroid.tileprovider.tilesource.FileBasedTileSource
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import ru.chsu.studentmap.data.local.OfflineAddressDatabase
import ru.chsu.studentmap.data.remote.dto.PlaceCreateDto
import ru.chsu.studentmap.data.remote.dto.PlaceResponseDto
import java.util.Locale

@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    targetLocation: GeoPoint? = null,
    onTargetLocationHandled: (() -> Unit)? = null,
    allPoints: List<MapPoint> = sampleMapPoints,
    favoritePlaces: List<PlaceResponseDto> = emptyList(),
    onAddFavorite: ((PlaceCreateDto, (String) -> Unit) -> Unit)? = null,
    onRemoveFavorite: ((Int, (String) -> Unit) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedCategory by remember { mutableStateOf(PlaceCategory.ALL) }

    // Выбранная точка (существующая метка кампуса, избранное место или произвольная точка)
    var selectedPoint by remember { mutableStateOf<MapPoint?>(null) }
    var selectedAddress by remember { mutableStateOf<String?>(null) }
    var isResolvingAddress by remember { mutableStateOf(false) }

    // Метка произвольной точки, поставленной пользователем по клику
    var customMarkerPoint by remember { mutableStateOf<GeoPoint?>(null) }

    // Диалог ввода названия при добавлении в избранное
    var showAddDialog by remember { mutableStateOf(false) }
    var newPlaceName by remember { mutableStateOf("") }

    val filteredPoints = remember(selectedCategory, allPoints) {
        if (selectedCategory == PlaceCategory.ALL) allPoints
        else allPoints.filter { it.category == selectedCategory }
    }

    // База данных офлайн-тайлов карты
    val sqliteFile = remember {
        OsmTileHelper.getOfflineDatabase(context)
    }

    val mapView = remember {
        Configuration.getInstance().userAgentValue = context.packageName

        MapView(context).apply {
            setMultiTouchControls(true)
            minZoomLevel = 10.0
            maxZoomLevel = 15.0
            isTilesScaledToDpi = true

            val cityBounds = BoundingBox(
                59.18, // North
                38.05, // East
                59.08, // South
                37.80  // West
            )
            setScrollableAreaLimitDouble(cityBounds)

            controller.setZoom(15.0)
            controller.setCenter(GeoPoint(59.1245, 37.9275))
        }
    }

    // Геопозиция пользователя
    val locationOverlay = remember(mapView) {
        MyLocationNewOverlay(
            GpsMyLocationProvider(context),
            mapView
        ).apply {
            setDrawAccuracyEnabled(true)
        }
    }

    // Пользователь нажал на карту или мы перешли сюда программно
    fun onSelectPoint(lat: Double, lon: Double) {
        customMarkerPoint = GeoPoint(lat, lon)

        // 1. Проверяем: вдруг это место уже есть у пользователя в избранном
        val existingFav = favoritePlaces.firstOrNull { fav ->
            Math.abs(fav.latitude - lat) < 0.0003 && Math.abs(fav.longitude - lon) < 0.0003
        }

        if (existingFav != null) {
            selectedPoint = MapPoint(
                id = "fav-${existingFav.id}",
                title = existingFav.name,
                category = PlaceCategory.OTHER,
                latitude = existingFav.latitude,
                longitude = existingFav.longitude,
                address = existingFav.address,
                isCampus = false
            )
            selectedAddress = existingFav.address
            isResolvingAddress = false
            return
        }

        // 2. Проверяем: вдруг кликнули прямо в корпус ЧГУ
        val campusMatch = sampleMapPoints.firstOrNull { p ->
            Math.abs(p.latitude - lat) < 0.0004 && Math.abs(p.longitude - lon) < 0.0004
        }

        if (campusMatch != null) {
            selectedPoint = campusMatch
            selectedAddress = campusMatch.address
            isResolvingAddress = false
            return
        }

        // 3. Если точка произвольная — ищем её адрес в нашей локальной sqlite базе
        selectedPoint = MapPoint(
            id = "custom-$lat-$lon",
            title = "Точка на карте",
            category = PlaceCategory.OTHER,
            latitude = lat,
            longitude = lon,
            address = null,
            isCampus = false
        )
        selectedAddress = null
        isResolvingAddress = true

        scope.launch {
            // Запрашиваем адрес из офлайн базы без интернета
            val placeInfo = OfflineAddressDatabase.resolvePlace(context, lat, lon)
            val addr = placeInfo?.address
            val title = placeInfo?.title
            selectedAddress = addr
            isResolvingAddress = false
            if (selectedPoint?.latitude == lat && selectedPoint?.longitude == lon) {
                if (title != null) {
                    selectedPoint = selectedPoint?.copy(
                        title = title,
                        address = addr
                    )
                } else if (!addr.isNullOrBlank()) {
                    selectedPoint = selectedPoint?.copy(
                        title = addr,
                        address = addr
                    )
                }
            }
        }
    }

    // Если передали координаты снаружи (например, нажали на избранное место в профиле),
    // то плавно двигаем карту к этой точке и открываем снизу её карточку
    LaunchedEffect(targetLocation) {
        targetLocation?.let { target ->
            mapView.controller.animateTo(target, 15.0, 1000L)
            onSelectPoint(target.latitude, target.longitude)
            onTargetLocationHandled?.invoke()
        }
    }

    // Перехват нажатий на карту для установки произвольных меток
    val eventsOverlay = remember(mapView) {
        MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                onSelectPoint(p.latitude, p.longitude)
                return true
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                onSelectPoint(p.latitude, p.longitude)
                return true
            }
        })
    }

    // Инициализация карты и слушателей
    DisposableEffect(mapView, sqliteFile) {
        val registerReceiver = SimpleRegisterReceiver(context)
        val offlineProvider = OfflineTileProvider(
            registerReceiver,
            arrayOf(sqliteFile)
        )

        mapView.setTileProvider(offlineProvider)

        val archives = offlineProvider.archives
        if (archives.isNotEmpty() && archives[0].tileSources.isNotEmpty()) {
            val sourceName = archives[0].tileSources.first()
            mapView.setTileSource(FileBasedTileSource.getSource(sourceName))
        } else {
            mapView.setTileSource(FileBasedTileSource.getSource("Mapnik") ?: mapView.tileProvider.tileSource)
        }

        // Добавляем слой обработки кликов
        mapView.overlays.add(0, eventsOverlay)

        val hasFineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFineLocation || hasCoarseLocation) {
            mapView.overlays.remove(locationOverlay)
            mapView.overlays.add(locationOverlay)
            locationOverlay.enableMyLocation()
        }

        onDispose {
            locationOverlay.disableMyLocation()
            mapView.overlays.remove(locationOverlay)
            mapView.overlays.remove(eventsOverlay)
            mapView.onDetach()
            offlineProvider.detach()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                // Очищаем старые метки
                view.overlays.removeAll { it is Marker }

                // 1. Отображаем объекты кампуса ЧГУ
                filteredPoints.forEach { point ->
                    val marker = Marker(view).apply {
                        position = GeoPoint(point.latitude, point.longitude)
                        title = point.title
                        snippet = point.address
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        val iconRes = when (point.category) {
                            PlaceCategory.BUILDINGS -> R.drawable.ic_marker_red
                            PlaceCategory.DORMITORIES -> R.drawable.ic_marker_yellow
                            else -> R.drawable.ic_marker_blue
                        }
                        icon = ContextCompat.getDrawable(context, iconRes)
                        // Отключаем стандартный белый балун OSMdroid, чтобы не открывалось 2 карточки
                        infoWindow = null
                        setOnMarkerClickListener { _, _ ->
                            customMarkerPoint = null
                            selectedPoint = point
                            selectedAddress = point.address
                            isResolvingAddress = false
                            true
                        }
                    }
                    view.overlays.add(marker)
                }

                // 2. Отображаем сохранённые избранные места пользователя
                favoritePlaces.forEach { fav ->
                    val marker = Marker(view).apply {
                        position = GeoPoint(fav.latitude, fav.longitude)
                        title = fav.name
                        snippet = fav.address
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        icon = ContextCompat.getDrawable(context, R.drawable.ic_marker_yellow)
                        // Отключаем стандартный белый балун OSMdroid
                        infoWindow = null
                        setOnMarkerClickListener { _, _ ->
                            customMarkerPoint = null
                            selectedPoint = MapPoint(
                                id = "fav-${fav.id}",
                                title = fav.name,
                                category = PlaceCategory.OTHER,
                                latitude = fav.latitude,
                                longitude = fav.longitude,
                                address = fav.address,
                                isCampus = false
                            )
                            selectedAddress = fav.address
                            isResolvingAddress = false
                            true
                        }
                    }
                    view.overlays.add(marker)
                }

                // 3. Отображаем выбранную произвольную точку
                customMarkerPoint?.let { customGeoPoint ->
                    val marker = Marker(view).apply {
                        position = customGeoPoint
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        icon = ContextCompat.getDrawable(context, R.drawable.ic_marker_red)
                        infoWindow = null
                        setOnMarkerClickListener { _, _ -> true }
                    }
                    view.overlays.add(marker)
                }

                view.invalidate()
            }
        )

        // Верхняя панель фильтров по категориям
        MapTopBarWithFilters(
            modifier = Modifier.align(Alignment.TopCenter),
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it }
        )

        // Фирменная нижняя карточка выбранного объекта ЧГУ / произвольной точки
        selectedPoint?.let { point ->
            val matchingFav = favoritePlaces.firstOrNull { fav ->
                Math.abs(fav.latitude - point.latitude) < 0.0003 &&
                    Math.abs(fav.longitude - point.longitude) < 0.0003
            }
            val isFavorite = matchingFav != null

            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Верхняя строка: бейдж типа точки и кнопка закрытия
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when {
                            point.isCampus -> {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = when (point.category) {
                                            PlaceCategory.DORMITORIES -> "Общежитие ЧГУ"
                                            PlaceCategory.FOOD -> "Столовая ЧГУ"
                                            else -> "Корпус ЧГУ"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            isFavorite -> {
                                Surface(
                                    color = Color(0xFFFFF8E1),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "★ В вашем избранном",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB78103),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            else -> {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Точка на карте",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                selectedPoint = null
                                customMarkerPoint = null
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Закрыть", modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Название места
                    Text(
                        text = point.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(4.dp))

                    // Адрес места
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        if (isResolvingAddress) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Определение адреса...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Text(
                                text = selectedAddress ?: point.address ?: "${String.format(Locale.US, "%.5f", point.latitude)}, ${String.format(Locale.US, "%.5f", point.longitude)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Кнопка добавления или удаления из избранного
                    if (isFavorite) {
                        OutlinedButton(
                            onClick = {
                                onRemoveFavorite?.invoke(matchingFav!!.id) { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    selectedPoint = null
                                    customMarkerPoint = null
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Удалить из избранного", fontWeight = FontWeight.Medium)
                        }
                    } else {
                        Button(
                            onClick = {
                                val currentAddr = selectedAddress ?: point.address
                                newPlaceName = if (point.title != "Точка на карте") {
                                    point.title
                                } else {
                                    currentAddr ?: ""
                                }
                                showAddDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FavoriteBorder,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("В избранное", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        // Диалог ввода названия при добавлении в избранное
        if (showAddDialog && selectedPoint != null) {
            val point = selectedPoint!!
            val currentAddr = selectedAddress ?: point.address
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Text(
                        text = "Добавить в избранное",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (!currentAddr.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = currentAddr,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedTextField(
                            value = newPlaceName,
                            onValueChange = { newPlaceName = it },
                            label = { Text("Название места") },
                            placeholder = { Text("Например: Корпус №1 или Мой дом") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val finalName = newPlaceName.trim().ifBlank {
                                currentAddr ?: "Место на карте"
                            }
                            val dto = PlaceCreateDto(
                                name = finalName,
                                address = currentAddr,
                                latitude = point.latitude,
                                longitude = point.longitude,
                                category = if (point.isCampus) point.category.name.lowercase() else "custom"
                            )
                            showAddDialog = false
                            onAddFavorite?.invoke(dto) { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Добавить")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showAddDialog = false }
                    ) {
                        Text("Отмена", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }

        // Кнопка геопозиции (при появлении карточки плавно поднимается выше)
        FloatingActionButton(
            onClick = {
                val location = locationOverlay.myLocation
                if (location != null) {
                    mapView.controller.animateTo(location)
                } else {
                    locationOverlay.enableMyLocation()
                    locationOverlay.runOnFirstFix {
                        mapView.post {
                            locationOverlay.myLocation?.let { geoPoint ->
                                mapView.controller.animateTo(geoPoint)
                            }
                        }
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    bottom = if (selectedPoint != null) 210.dp else 16.dp,
                    end = 16.dp
                )
                .size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.MyLocation,
                contentDescription = "Геопозиция"
            )
        }
    }
}
