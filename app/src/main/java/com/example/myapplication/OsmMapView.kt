package com.example.myapplication

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.modules.OfflineTileProvider
import org.osmdroid.tileprovider.tilesource.FileBasedTileSource
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import android.content.pm.PackageManager
import android.Manifest
import androidx.core.content.ContextCompat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color

@Composable
fun OsmMapView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    
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

            // Начальное положение камеры и зум
            controller.setZoom(13.0)
            controller.setCenter(cityBounds.centerWithDateLine)
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
            modifier = Modifier.fillMaxSize()
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
            Text(" ")
        }
    }
}