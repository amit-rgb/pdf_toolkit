package com.example.data

import kotlinx.coroutines.flow.Flow

class PdfRepository(
    private val recentFilesDao: RecentFilesDao,
    private val bookmarksDao: BookmarksDao
) {
    val allRecentFiles: Flow<List<RecentFileEntity>> = recentFilesDao.getAllRecentFiles()

    suspend fun insertRecentFile(file: RecentFileEntity): Long {
        return recentFilesDao.insertRecentFile(file)
    }

    suspend fun deleteRecentFile(file: RecentFileEntity) {
        recentFilesDao.deleteRecentFile(file)
    }

    suspend fun clearRecentFiles() {
        recentFilesDao.clearAll()
    }

    fun getBookmarks(filePath: String): Flow<List<BookmarkEntity>> {
        return bookmarksDao.getBookmarksForFile(filePath)
    }

    suspend fun addBookmark(filePath: String, pageIndex: Int, label: String): Long {
        return bookmarksDao.insertBookmark(
            BookmarkEntity(
                filePath = filePath,
                pageIndex = pageIndex,
                label = label
            )
        )
    }

    suspend fun deleteBookmark(bookmark: BookmarkEntity) {
        bookmarksDao.deleteBookmark(bookmark)
    }
}
