package com.example.data

import com.example.model.Script
import kotlinx.coroutines.flow.Flow

class ScriptRepository(private val scriptDao: ScriptDao) {
    val allScripts: Flow<List<Script>> = scriptDao.getAllScripts()

    fun getScriptById(id: Long): Flow<Script?> = scriptDao.getScriptById(id)

    suspend fun getScriptByIdOnce(id: Long): Script? = scriptDao.getScriptByIdOnce(id)

    fun searchScripts(query: String): Flow<List<Script>> = scriptDao.searchScripts(query)

    suspend fun saveScript(script: Script): Long {
        val count = Script.calculateWordCount(script.content)
        val duration = Script.calculateDurationSec(count)
        val enriched = script.copy(
            wordCount = count,
            estimatedDurationSec = duration,
            updatedAt = System.currentTimeMillis()
        )
        return scriptDao.insertScript(enriched)
    }

    suspend fun deleteScript(script: Script) = scriptDao.deleteScript(script)

    suspend fun deleteById(id: Long) = scriptDao.deleteScriptById(id)

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) = scriptDao.toggleFavorite(id, isFavorite)

    suspend fun updateLastReadPosition(id: Long, position: Int) = scriptDao.updateLastReadPosition(id, position)
}
