package com.example.myapplication

import android.content.Context
import android.util.Log
import java.io.File

object OsmTileHelper {
    private const val DB_NAME = "city.sqlite"
    private const val ASSETS_PATH = "tiles/city.sqlite"

    // Кэшируем ссылку на файл, чтобы не проверять файловую систему
    // при каждом переключении вкладок в Compose
    private var cachedDbFile: File? = null

    /**
     * Копирует файл тайлов из assets во внутреннее хранилище, если его там ещё нет.
     * Возвращает итоговый File.
     */
    fun getOfflineDatabase(context: Context): File {
        cachedDbFile?.let {
            if (it.exists() && it.isFile) return it
        }

        val outFile = File(context.filesDir, DB_NAME)

        try {
            if (outFile.exists() && (outFile.isDirectory || outFile.length() == 0L)) {
                outFile.deleteRecursively()
            }

            if (!outFile.exists()) {
                context.assets.open(ASSETS_PATH).use { input ->
                    context.openFileOutput(DB_NAME, Context.MODE_PRIVATE).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            
            cachedDbFile = outFile
        } catch (e: Exception) {
            Log.e("OsmTileHelper", "Ошибка при распаковке базы данных карты: ${e.message}", e)
        }
        
        return outFile
    }
}
