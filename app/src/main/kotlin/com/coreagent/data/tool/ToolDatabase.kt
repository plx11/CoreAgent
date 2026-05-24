package com.coreagent.data.tool

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "installed_tools")
data class InstalledToolEntity(
    @PrimaryKey val name: String,
    val version: String,
    val entryPoint: String,
    val sha256: String,
    val localPath: String,
    val downloadUrl: String,
    val installDate: Long = System.currentTimeMillis()
)

@Dao
interface ToolDao {
    @Query("SELECT * FROM installed_tools")
    fun getAllInstalledTools(): Flow<List<InstalledToolEntity>>

    @Query("SELECT * FROM installed_tools WHERE name = :name LIMIT 1")
    suspend fun getToolByName(name: String): InstalledToolEntity?

    @Insert
    suspend fun insertTool(tool: InstalledToolEntity)

    @Query("DELETE FROM installed_tools WHERE name = :name")
    suspend fun deleteToolByName(name: String)
}

@Database(entities = [InstalledToolEntity::class], version = 1, exportSchema = false)
abstract class ToolDatabase : RoomDatabase() {
    abstract fun toolDao(): ToolDao
}
