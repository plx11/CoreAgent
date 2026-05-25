package com.coreagent.core.memory

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "workspaces")
data class WorkspaceEntity(
    @PrimaryKey val workspaceId: String,
    val globalContext: String
)

@Entity(
    tableName = "task_steps",
    foreignKeys = [ForeignKey(
        entity = WorkspaceEntity::class,
        parentColumns = ["workspaceId"],
        childColumns = ["workspaceId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class TaskStepEntity(
    @PrimaryKey val stepId: String,
    val workspaceId: String,
    val description: String,
    val status: String,
    val result: String?,
    val dependenciesJson: String,
    val assignedAgent: String,
    val requiredToolsJson: String
)

@Entity(
    tableName = "chat_messages",
    foreignKeys = [ForeignKey(
        entity = WorkspaceEntity::class,
        parentColumns = ["workspaceId"],
        childColumns = ["workspaceId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workspaceId: String,
    val role: String,
    val content: String,
    val citationsJson: String
)

@Entity(
    tableName = "produced_artifacts",
    foreignKeys = [ForeignKey(
        entity = WorkspaceEntity::class,
        parentColumns = ["workspaceId"],
        childColumns = ["workspaceId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class ProducedArtifactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workspaceId: String,
    val stepId: String,
    val artifactName: String,
    val content: String
)

@Dao
interface MemoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspace(workspace: WorkspaceEntity)

    @Query("SELECT * FROM workspaces")
    fun getAllWorkspacesFlow(): Flow<List<WorkspaceEntity>>

    @Query("SELECT * FROM workspaces WHERE workspaceId = :id")
    suspend fun getWorkspace(id: String): WorkspaceEntity?

    @Query("SELECT COUNT(*) FROM workspaces")
    suspend fun getWorkspaceCount(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM workspaces WHERE workspaceId = :id)")
    suspend fun workspaceExists(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskSteps(steps: List<TaskStepEntity>)

    @Query("UPDATE task_steps SET status = :status WHERE stepId = :id")
    suspend fun updateStepStatus(id: String, status: String)

    @Query("UPDATE task_steps SET result = :result WHERE stepId = :id")
    suspend fun updateStepResult(id: String, result: String?)

    @Query("SELECT * FROM task_steps WHERE workspaceId = :workspaceId ORDER BY stepId ASC")
    fun getWorkspaceStepsFlow(workspaceId: String): Flow<List<TaskStepEntity>>
    
    @Query("SELECT * FROM task_steps WHERE workspaceId = :workspaceId AND status = 'PENDING' LIMIT 1")
    suspend fun getNextPendingStep(workspaceId: String): TaskStepEntity?

    @Query("SELECT * FROM task_steps WHERE stepId = :id")
    suspend fun getTaskStep(id: String): TaskStepEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)
    
    @Query("SELECT * FROM chat_messages WHERE workspaceId = :workspaceId ORDER BY id ASC")
    fun getMessagesFlow(workspaceId: String): Flow<List<ChatMessageEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtifact(artifact: ProducedArtifactEntity)
    
    @Query("SELECT * FROM produced_artifacts WHERE workspaceId = :workspaceId")
    suspend fun getArtifacts(workspaceId: String): List<ProducedArtifactEntity>
}

@Database(entities = [WorkspaceEntity::class, TaskStepEntity::class, ChatMessageEntity::class, ProducedArtifactEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
}
