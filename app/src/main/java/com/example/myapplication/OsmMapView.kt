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
            //controller.setCenter(cityBounds.centerWithDateLine)
            //controller.setCenter(cityBounds.)
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

        // Ищем источник тайлов внутри базы данных
        val archives = offlineProvider.archives
        if (archives.isNotEmpty() && archives[0].tileSources.isNotEmpty()) {
            val sourceName = archives[0].tileSources.first()
            mapView.setTileSource(FileBasedTileSource.getSource(sourceName))
        } else {
            // Резервный вариант, если в БД не найдены тайлы
            mapView.setTileSource(FileBasedTileSource.getSource("Mapnik") ?: mapView.tileProvider.tileSource)
        }

        // Отключаем использование сети
        mapView.setUseDataConnection(false)

        // Очищаем ресурсы при удалении карты с экрана
        onDispose {
            mapView.onDetach()
            offlineProvider.detach()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier.fillMaxSize()
    )
}