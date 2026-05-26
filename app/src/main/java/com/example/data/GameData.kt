package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Database
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "player_progress")
data class PlayerProgress(
    @PrimaryKey val id: Int = 1,
    val level: Int = 1,
    val exp: Int = 0,
    val currentHp: Int = 5,
    val maxHp: Int = 5,
    val gold: Int = 0,
    val score: Int = 0,
    val highScore: Int = 0,
    val defeatedMonsters: Int = 0
)

@Dao
interface PlayerProgressDao {
    @Query("SELECT * FROM player_progress WHERE id = 1 LIMIT 1")
    fun getPlayerProgress(): Flow<PlayerProgress?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePlayerProgress(progress: PlayerProgress)

    @Query("DELETE FROM player_progress")
    suspend fun clearProgress()
}

@Database(entities = [PlayerProgress::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playerProgressDao(): PlayerProgressDao
}

class GameRepository(private val dao: PlayerProgressDao) {
    fun getPlayerProgress(): Flow<PlayerProgress?> = dao.getPlayerProgress()
    
    suspend fun saveProgress(progress: PlayerProgress) {
        dao.savePlayerProgress(progress)
    }
}
