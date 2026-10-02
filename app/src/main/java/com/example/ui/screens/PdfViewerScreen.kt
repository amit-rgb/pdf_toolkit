package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import com.example.engine.PdfEngine
import com.example.ui.components.PdfTopBar
import com.example.ui.theme.PdfPrimary
import com.example.ui.viewmodel.PdfViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

@Composable
fun PdfViewerScreen(viewModel: PdfViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    BackHandler { viewModel.navigateBack() }

    val activeFile by viewModel.activePdfFile.collectAsState()
    val totalPages by viewModel.totalPages.collectAsState()
    val thumbnails by viewModel.pageThumbnails.collectAsState()
    val rotation by viewModel.viewerRotation.collectAsState()

    var isFilmstripVisible by remember { mutableStateOf(false) }
    var isFullScreen by remember { mutableStateOf(false) }

    // Multi-touch pinch-to-zoom & pan states
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    val listState = rememberLazyListState()

    // Dynamically calculate current visible page based on vertical scroll position
    val currentVisiblePage by remember {
        derivedStateOf {
            if (totalPages > 0) {
                (listState.firstVisibleItemIndex + 1).coerceIn(1, totalPages)
            } else 1
        }
    }

    val openPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.openPdfFromUri(it, "Viewer") }
    }

    Scaffold(
        topBar = {
            if (!isFullScreen) {
                PdfTopBar(
                    title = activeFile?.name ?: "PDF Viewer",
                    subtitle = if (totalPages > 0) "Page $currentVisiblePage of $totalPages • Pinch to zoom" else null,
                    onBack = { viewModel.navigateBack() },
                    actions = {
                        // Rotate 90 CW
                        IconButton(onClick = { viewModel.rotateViewerClockwise() }) {
                            Icon(Icons.Default.RotateRight, contentDescription = "Rotate Clockwise")
                        }
                        // Open in external system viewer
                        activeFile?.let { file ->
                            IconButton(onClick = { viewModel.openPdfInSystemViewer(context, file) }) {
                                Icon(Icons.Default.OpenInNew, contentDescription = "Open in External App")
                            }
                            IconButton(onClick = { viewModel.shareFile(context, file) }) {
                                Icon(Icons.Default.Share, contentDescription = "Share Document")
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (!isFullScreen && totalPages > 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(bottom = 8.dp)
                ) {
                    // Expandable Thumbnails Filmstrip
                    AnimatedVisibility(
                        visible = isFilmstripVisible,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        ) {
                            itemsIndexed(thumbnails) { idx, thumb ->
                                val isSelected = idx == (currentVisiblePage - 1)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable {
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(idx)
                                        }
                                    }
                                ) {
                                    Image(
                                        bitmap = thumb.asImageBitmap(),
                                        contentDescription = "Page ${idx + 1}",
                                        modifier = Modifier
                                            .height(78.dp)
                                            .width(56.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .border(
                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                color = if (isSelected) PdfPrimary else Color.LightGray,
                                                shape = RoundedCornerShape(6.dp)
                                            ),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${idx + 1}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) PdfPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Navigation Bar with Quick Actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Scroll to top
                        IconButton(
                            onClick = {
                                coroutineScope.launch { listState.animateScrollToItem(0) }
                            },
                            enabled = currentVisiblePage > 1
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Top Page")
                        }

                        // Page indicator pill (tap to toggle filmstrip)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { isFilmstripVisible = !isFilmstripVisible }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ViewCarousel,
                                    contentDescription = "Thumbnails",
                                    tint = PdfPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$currentVisiblePage / $totalPages",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        // Scroll to bottom
                        IconButton(
                            onClick = {
                                coroutineScope.launch { listState.animateScrollToItem(totalPages - 1) }
                            },
                            enabled = currentVisiblePage < totalPages
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Bottom Page")
                        }

                        // Full Screen Toggle
                        IconButton(onClick = { isFullScreen = !isFullScreen }) {
                            Icon(
                                if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Toggle Fullscreen"
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0F172A))
        ) {
            if (activeFile == null) {
                // Empty state: pick a PDF
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .background(
                                Brush.linearGradient(listOf(PdfPrimary, Color(0xFF8B5CF6))),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "No PDF",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "No Document Selected",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Choose a PDF file to view with continuous vertical paging & pinch zoom",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(22.dp))
                    Button(
                        onClick = { openPicker.launch("application/pdf") },
                        colors = ButtonDefaults.buttonColors(containerColor = PdfPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Select PDF File", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Main Multi-Touch Pinch & Pan Canvas with Continuous Vertical Pages
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        // 1. Double tap to zoom in / reset
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (zoomScale > 1.05f) {
                                        zoomScale = 1.0f
                                        panOffsetX = 0f
                                        panOffsetY = 0f
                                    } else {
                                        zoomScale = 2.0f
                                    }
                                }
                            )
                        }
                        // 2. Pinch with two fingers to zoom in/out naturally & Pan
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (zoomScale * zoom).coerceIn(1.0f, 4.0f)
                                zoomScale = newScale

                                if (newScale > 1.0f) {
                                    // Panning when zoomed in
                                    panOffsetX += pan.x
                                    panOffsetY += pan.y
                                } else {
                                    // Snap back when zoomed out
                                    panOffsetX = 0f
                                    panOffsetY = 0f
                                }
                            }
                        }
                ) {
                    LazyColumn(
                        state = listState,
                        userScrollEnabled = zoomScale <= 1.05f, // Allow free vertical page scroll when at normal zoom
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = zoomScale,
                                scaleY = zoomScale,
                                translationX = panOffsetX,
                                translationY = panOffsetY
                            ),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(totalPages) { pageIndex ->
                            PdfPageCard(
                                file = activeFile!!,
                                pageIndex = pageIndex,
                                rotation = rotation,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp)
                            )
                        }
                    }
                }

                // Floating Active Zoom Reset Pill (appears whenever user pinches to zoom > 100%)
                AnimatedVisibility(
                    visible = zoomScale > 1.05f,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.Black.copy(alpha = 0.85f),
                        tonalElevation = 6.dp,
                        modifier = Modifier
                            .clickable {
                                zoomScale = 1.0f
                                panOffsetX = 0f
                                panOffsetY = 0f
                            }
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "Reset Zoom",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${(zoomScale * 100).toInt()}% • Tap to Reset Zoom",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }

                // Floating Current Page Badge (top-right)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Page $currentVisiblePage of $totalPages",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                // Fullscreen Exit Overlay Button
                if (isFullScreen) {
                    IconButton(
                        onClick = { isFullScreen = false },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(Icons.Default.FullscreenExit, contentDescription = "Exit Fullscreen", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun PdfPageCard(
    file: File,
    pageIndex: Int,
    rotation: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(file.absolutePath, pageIndex) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(file.absolutePath, pageIndex) { mutableStateOf(true) }

    LaunchedEffect(file.absolutePath, pageIndex) {
        isLoading = true
        bitmap = withContext(Dispatchers.IO) {
            PdfEngine.renderPageBitmap(context, file, pageIndex, 2.0f)
        }
        isLoading = false
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Page Number Indicator Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC))
                    .padding(vertical = 4.dp, horizontal = 12.dp)
            ) {
                Text(
                    text = "Page ${pageIndex + 1}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp
                    ),
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }

            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Page ${pageIndex + 1}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .rotate(rotation.toFloat()),
                    contentScale = ContentScale.FillWidth
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = PdfPrimary,
                            modifier = Modifier.size(34.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Rendering page ${pageIndex + 1}...",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8))
                        )
                    }
                }
            }
        }
    }
}
