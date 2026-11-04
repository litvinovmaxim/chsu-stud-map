package ru.chsu.studentmap

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.modules.OfflineTileProvider
import org.osmdroid.tileprovider.tilesource.FileBasedTileSource
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    allPoints: List<MapPoint> = sampleMapPoints
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf(PlaceCategory.ALL) }

    val filteredPoints = remember(selectedCategory, allPoints) {
        if (selectedCategory == PlaceCategory.ALL) allPoints
        else allPoints.filter { it.category == selectedCategory }
    }
    
    // Получаем файл базы данных тайлов (карты)
    val sqliteFile = remember {
        OsmTileHelper.getOfflineDatabase(context)
    }

    val mapView = remember {
        // Инициализируем настройки OSM
        Configuration.getInstance().userAgentValue = context.packageName

        MapView(context).apply {
            // Зум
            setMultiTouchControls(true)
            minZoomLevel = 10.0
            maxZoomLevel = 15.0
            isTilesScaledToDpi = true

            // Границы города
            val cityBounds = BoundingBox(
                59.18, // North
                38.05, // East
                59.08, // South
                37.80  // West
            )
            setScrollableAreaLimitDouble(cityBounds)

            // Начальное положение камеры и зум (Череповец, район ЧГУ)
            controller.setZoom(15.0)
            controller.setCenter(GeoPoint(59.1245, 37.9275))
        }
    }

    // Геопозиция
    val locationOverlay = remember(mapView) {
        MyLocationNewOverlay(
            GpsMyLocationProvider(context),
            mapView
        ).apply {
            setDrawAccuracyEnabled(true)
        }
    }

    // Офлайн карта
    DisposableEffect(mapView, sqliteFile) {
        val registerReceiver = SimpleRegisterReceiver(context)
        val offlineProvider = OfflineTileProvider(
            registerReceiver,
            arrayOf(sqliteFile) // Передаем нашу базу данных
        )

        // Привязываем провайдер к карте
        mapView.setTileProvider(offlineProvider)

        // Тайлы внутри бд
        val archives = offlineProvider.archives
        if (archives.isNotEmpty() && archives[0].tileSources.isNotEmpty()) {
            val sourceName = archives[0].tileSources.first()
            mapView.setTileSource(FileBasedTileSource.getSource(sourceName))
        } else {
            // Оработка исключения
            mapView.setTileSource(FileBasedTileSource.getSource("Mapnik") ?: mapView.tileProvider.tileSource)
        }

        // Геопозиция
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
            mapView.onDetach()
            offlineProvider.detach()
        }

        // Очищаем ресурсы при удалении карты с экрана
        onDispose {
            locationOverlay.disableMyLocation()
            mapView.overlays.remove(locationOverlay)
            mapView.onDetach()
            offlineProvider.detach()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                view.overlays.removeAll { it is Marker }
                filteredPoints.forEach { point ->
                    val marker = Marker(view).apply {
                        position = GeoPoint(point.latitude, point.longitude)
                        title = point.title
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        val iconRes = when (point.category) {
                            PlaceCategory.BUILDINGS -> R.drawable.ic_marker_red
                            PlaceCategory.DORMITORIES -> R.drawable.ic_marker_yellow
                            else -> R.drawable.ic_marker_blue
                        }
                        icon = ContextCompat.getDrawable(context, iconRes)
                    }
                    view.overlays.add(marker)
                }
                view.invalidate()
            }
        )

        MapTopBarWithFilters(
            modifier = Modifier.align(Alignment.TopCenter),
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it }
        )

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
                .padding(16.dp)
                .size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.MyLocation,
                contentDescription = "Геопозиция"
            )
        }
    }
}