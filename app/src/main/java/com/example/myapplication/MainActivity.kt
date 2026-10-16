package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MyApplicationApp()
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun MyApplicationApp() {
    //Храним состояние текущей вкладки
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.MAP) }

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
                    AppDestinations.EVENTS -> {
                        Box(contentAlignment = Alignment.Center) {
                            //TODO: вкладка события
                            Text("Здесь будут события")
                        }
                    }
                    AppDestinations.MAP -> {
                        OsmMapView()
                    }
                    AppDestinations.PROFILE -> {
                        Box(contentAlignment = Alignment.Center) {
                            //TODO: вкладка профиль
                            Text("Здесь будет профиль пользователя")
                        }
                    }
                }
            }
        }
    }
}

//Список всех доступных вкладок
enum class AppDestinations(val label: String) {
    EVENTS("События"),
    MAP("Карта"),
    PROFILE("Профиль");

    //Возвращает нужную иконку для каждой вкладки
    val iconRes: Int
        get() = when (this) {
            EVENTS -> R.drawable.ic_calendar
            MAP -> R.drawable.ic_map
            PROFILE -> R.drawable.ic_account_box
        }
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    MyApplicationTheme {
        MyApplicationApp()
    }
}