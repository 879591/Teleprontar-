package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.Script
import kotlinx.coroutines.flow.Flow

@Dao
interface ScriptDao {
    @Query("SELECT * FROM scripts ORDER BY updatedAt DESC")
    fun getAllScripts(): Flow<List<Script>>

    @Query("SELECT * FROM scripts WHERE id = :id")
    fun getScriptById(id: Long): Flow<Script?>

    @Query("SELECT * FROM scripts WHERE id = :id")
    suspend fun getScriptByIdOnce(id: Long): Script?

    @Query("SELECT * FROM scripts WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchScripts(query: String): Flow<List<Script>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScript(script: Script): Long

    @Update
    suspend fun updateScript(script: Script)

    @Delete
    suspend fun deleteScript(script: Script)

    @Query("DELETE FROM scripts WHERE id = :id")
    suspend fun deleteScriptById(id: Long)

    @Query("UPDATE scripts SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE scripts SET lastReadPosition = :position WHERE id = :id")
    suspend fun updateLastReadPosition(id: Long, position: Int)
}
