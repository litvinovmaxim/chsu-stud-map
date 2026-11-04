package ru.chsu.studentmap

import android.app.Application
import ru.chsu.studentmap.data.AppContainer

class StudentMapApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
