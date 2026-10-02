package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentFilesDao {
    @Query("SELECT * FROM recent_files ORDER BY timestamp DESC")
    fun getAllRecentFiles(): Flow<List<RecentFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentFile(file: RecentFileEntity): Long

    @Delete
    suspend fun deleteRecentFile(file: RecentFileEntity)

    @Query("DELETE FROM recent_files")
    suspend fun clearAll()
}

@Dao
interface BookmarksDao {
    @Query("SELECT * FROM bookmarks WHERE filePath = :filePath ORDER BY pageIndex ASC")
    fun getBookmarksForFile(filePath: String): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)
}

@Database(
    entities = [RecentFileEntity::class, BookmarkEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PdfAppDatabase : RoomDatabase() {
    abstract fun recentFilesDao(): RecentFilesDao
    abstract fun bookmarksDao(): BookmarksDao

    companion object {
        @Volatile
        private var INSTANCE: PdfAppDatabase? = null

        fun getDatabase(context: Context): PdfAppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PdfAppDatabase::class.java,
                    "pdf_toolkit_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
