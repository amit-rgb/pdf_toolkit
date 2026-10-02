package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PdfAppDatabase
import com.example.data.PdfRepository
import com.example.data.RecentFileEntity
import com.example.engine.PageAnnotations
import com.example.engine.PageSizePreset
import com.example.engine.PdfEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class ScreenDestination {
    HOME,
    VIEWER,
    IMAGE_TO_PDF,
    MERGE,
    EDIT
}

class PdfViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PdfRepository

    init {
        val db = PdfAppDatabase.getDatabase(application)
        repository = PdfRepository(db.recentFilesDao(), db.bookmarksDao())
    }

    val recentFiles: StateFlow<List<RecentFileEntity>> = repository.allRecentFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Navigation State
    private val _currentScreen = MutableStateFlow(ScreenDestination.HOME)
    val currentScreen: StateFlow<ScreenDestination> = _currentScreen.asStateFlow()

    // Loading State
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    // Active PDF File & Viewer State
    private val _activePdfFile = MutableStateFlow<File?>(null)
    val activePdfFile: StateFlow<File?> = _activePdfFile.asStateFlow()

    private val _currentPageIndex = MutableStateFlow(0)
    val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()

    private val _totalPages = MutableStateFlow(0)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()

    private val _currentPageBitmap = MutableStateFlow<Bitmap?>(null)
    val currentPageBitmap: StateFlow<Bitmap?> = _currentPageBitmap.asStateFlow()

    private val _pageThumbnails = MutableStateFlow<List<Bitmap>>(emptyList())
    val pageThumbnails: StateFlow<List<Bitmap>> = _pageThumbnails.asStateFlow()

    private val _viewerRotation = MutableStateFlow(0)
    val viewerRotation: StateFlow<Int> = _viewerRotation.asStateFlow()

    fun navigateTo(screen: ScreenDestination) {
        _currentScreen.value = screen
    }

    fun navigateBack(): Boolean {
        if (_currentScreen.value != ScreenDestination.HOME) {
            _currentScreen.value = ScreenDestination.HOME
            return true
        }
        return false
    }

    /**
     * Opens an external PDF URI, saves a local copy, loads its pages and switches to viewer.
     */
    fun openPdfFromUri(uri: Uri, operation: String = "View") {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _statusMessage.value = "Opening document..."
                val file = PdfEngine.copyUriToTempFile(getApplication(), uri, "opened")
                setDirectActiveFile(file)

                val count = PdfEngine.getPageCount(file)
                repository.insertRecentFile(
                    RecentFileEntity(
                        title = file.name,
                        filePath = file.absolutePath,
                        pageCount = count,
                        fileSizeBytes = file.length(),
                        operationType = operation
                    )
                )

                _currentScreen.value = ScreenDestination.VIEWER
            } catch (e: Exception) {
                Toast.makeText(getApplication(), "Error opening PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Sets the active file directly (e.g. after generation) and loads pages.
     */
    fun setDirectActiveFile(file: File) {
        _activePdfFile.value = file
        _currentPageIndex.value = 0
        _viewerRotation.value = 0
        loadActiveDocumentData(file)
    }

    private fun loadActiveDocumentData(file: File) {
        viewModelScope.launch {
            try {
                val count = PdfEngine.getPageCount(file)
                _totalPages.value = count

                // Render current page bitmap (high resolution ARGB_8888)
                val pageBmp = PdfEngine.renderPageBitmap(getApplication(), file, 0, 2.0f)
                _currentPageBitmap.value = pageBmp

                // Render thumbnails filmstrip
                val thumbs = PdfEngine.getPageThumbnails(getApplication(), file, 50)
                _pageThumbnails.value = thumbs
            } catch (e: Exception) {
                Toast.makeText(getApplication(), "Error rendering pages: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun setCurrentPageIndex(index: Int) {
        val total = _totalPages.value
        if (total > 0 && index in 0 until total) {
            _currentPageIndex.value = index
            val file = _activePdfFile.value ?: return
            viewModelScope.launch {
                val pageBmp = PdfEngine.renderPageBitmap(getApplication(), file, index, 2.0f)
                _currentPageBitmap.value = pageBmp
            }
        }
    }

    fun rotateViewerClockwise() {
        _viewerRotation.value = (_viewerRotation.value + 90) % 360
    }

    // --- Core Operations ---

    fun createImagesPdf(
        uris: List<Uri>,
        pageSize: PageSizePreset,
        isLandscape: Boolean,
        margin: Int,
        rotations: List<Int>,
        onComplete: (File) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _statusMessage.value = "Converting ${uris.size} images to PDF..."
                val file = PdfEngine.createPdfFromImages(getApplication(), uris, pageSize, isLandscape, margin, rotations)
                recordRecent(file, "Images → PDF")
                setDirectActiveFile(file)
                onComplete(file)
                Toast.makeText(getApplication(), "PDF created successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(getApplication(), "Conversion failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun mergePdfFiles(files: List<File>, onComplete: (File) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _statusMessage.value = "Merging ${files.size} PDFs..."
                val file = PdfEngine.mergePdfs(getApplication(), files)
                recordRecent(file, "Merged PDF")
                setDirectActiveFile(file)
                onComplete(file)
                Toast.makeText(getApplication(), "Merged into 1 PDF successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(getApplication(), "Merge failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveEditedPdf(
        sourceFile: File,
        annotationsMap: Map<Int, PageAnnotations>,
        onComplete: (File) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _statusMessage.value = "Baking annotations & saving PDF..."
                val file = PdfEngine.saveAnnotatedPdf(getApplication(), sourceFile, annotationsMap)
                recordRecent(file, "Edited PDF")
                setDirectActiveFile(file)
                onComplete(file)
                Toast.makeText(getApplication(), "PDF saved successfully with your drawings & text!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(getApplication(), "Save failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createBlankDocumentToEdit(onComplete: (File) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _statusMessage.value = "Creating blank document..."
                val file = PdfEngine.createBlankPdf(getApplication(), PageSizePreset.A4)
                onComplete(file)
            } catch (e: Exception) {
                Toast.makeText(getApplication(), "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun shareFile(context: Context, file: File, mimeType: String = "application/pdf") {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Document"))
        } catch (e: Exception) {
            Toast.makeText(context, "Share error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openPdfInSystemViewer(context: Context, file: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open with"))
        } catch (e: Exception) {
            Toast.makeText(context, "No external PDF app found or error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun clearRecentFiles() {
        viewModelScope.launch {
            repository.clearRecentFiles()
        }
    }

    private suspend fun recordRecent(file: File, opType: String) {
        try {
            val count = PdfEngine.getPageCount(file)
            repository.insertRecentFile(
                RecentFileEntity(
                    title = file.name,
                    filePath = file.absolutePath,
                    pageCount = count,
                    fileSizeBytes = file.length(),
                    operationType = opType
                )
            )
        } catch (_: Exception) {}
    }
}
