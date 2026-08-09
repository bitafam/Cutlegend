package com.example.db

import androidx.room.*
import com.example.model.Part
import com.example.model.ScrapPiece
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val timestamp: Long = System.currentTimeMillis(),
    val slabLength: Float = 3000f,
    val slabWidth: Float = 1800f,
    val slabThickness: Float = 20f,
    val diskThickness: Float = 3.5f,
    val trimMargin: Float = 10f,
    val useScrap: Boolean = true,
    val parts: List<Part>,
    val scrap: List<ScrapPiece>
)

class RoomConverters {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    
    @TypeConverter
    fun fromPartsList(parts: List<Part>?): String {
        if (parts == null) return "[]"
        val type = Types.newParameterizedType(List::class.java, Part::class.java)
        val adapter = moshi.adapter<List<Part>>(type)
        return adapter.toJson(parts)
    }

    @TypeConverter
    fun toPartsList(partsJson: String?): List<Part> {
        if (partsJson.isNullOrEmpty()) return emptyList()
        val type = Types.newParameterizedType(List::class.java, Part::class.java)
        val adapter = moshi.adapter<List<Part>>(type)
        return adapter.fromJson(partsJson) ?: emptyList()
    }

    @TypeConverter
    fun fromScrapList(scrap: List<ScrapPiece>?): String {
        if (scrap == null) return "[]"
        val type = Types.newParameterizedType(List::class.java, ScrapPiece::class.java)
        val adapter = moshi.adapter<List<ScrapPiece>>(type)
        return adapter.toJson(scrap)
    }

    @TypeConverter
    fun toScrapList(scrapJson: String?): List<ScrapPiece> {
        if (scrapJson.isNullOrEmpty()) return emptyList()
        val type = Types.newParameterizedType(List::class.java, ScrapPiece::class.java)
        val adapter = moshi.adapter<List<ScrapPiece>>(type)
        return adapter.fromJson(scrapJson) ?: emptyList()
    }
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY timestamp DESC")
    suspend fun getAllProjects(): List<ProjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Int): ProjectEntity?
}

@Database(entities = [ProjectEntity::class], version = 1, exportSchema = false)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
}
