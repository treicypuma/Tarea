package com.example.sqlitevsroom

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "notas")
data class Nota(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val titulo: String
)

@Dao
interface NotaDao {
    @Insert
    suspend fun insertar(nota: Nota)

    @Query("SELECT * FROM notas ORDER BY id DESC")
    fun todas(): Flow<List<Nota>>
}

@Database(entities = [Nota::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun notaDao(): NotaDao

    companion object {
        @Volatile private var instancia: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notas_room.db"
                ).build().also { instancia = it }
            }
    }
}