package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.DrawingPoint
import com.example.engine.DrawingStroke
import com.example.engine.PageAnnotations
import com.example.engine.PdfEngine
import com.example.engine.TextAnnotation
import com.example.ui.components.PdfTopBar
import com.example.ui.components.PrivacyBadge
import com.example.ui.theme.CardBorderGradient
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.PdfPrimary
import com.example.ui.theme.ToolCoral
import com.example.ui.theme.ToolEmerald
import com.example.ui.theme.ToolIndigo
import com.example.ui.viewmodel.PdfViewModel
import com.example.ui.viewmodel.ScreenDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

enum class EditTool {
    NONE_SCROLL, // ✋ Pan & Zoom mode: drag in any direction (left, right, up, down) and pinch to zoom freely!
    COLORED_PENCIL,
    GRAPHITE_PENCIL,
    HIGHLIGHTER,
    ADD_TEXT
}

@Composable
fun PdfEditScreen(viewModel: PdfViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    BackHandler { viewModel.navigateBack() }

    var sourcePdfFile by remember { mutableStateOf<File?>(null) }
    var totalPages by remember { mutableIntStateOf(0) }
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var currentPageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingPage by remember { mutableStateOf(false) }

    // Annotations mapped by page index
    val annotationsPerPage = remember { mutableStateMapOf<Int, PageAnnotations>() }

    // Current tool settings - Default to NONE_SCROLL (Pan & Zoom mode)
    var activeTool by remember { mutableStateOf(EditTool.NONE_SCROLL) }
    var selectedColor by remember { mutableStateOf(Color(0xFFEF4444)) } // Default vibrant red
    var strokeThickness by remember { mutableFloatStateOf(4.0f) } // Points

    // 2D Viewport Pan & Zoom states (Allows full free dragging to any corner!)
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    // Active in-progress stroke points
    var currentStrokePoints by remember { mutableStateOf<List<DrawingPoint>>(emptyList()) }

    // Text Input Dialog state
    var showTextDialog by remember { mutableStateOf(false) }
    var pendingText by remember { mutableStateOf("") }
    var pendingTextSize by remember { mutableFloatStateOf(18.0f) }
    var pendingTextColor by remember { mutableStateOf(Color(0xFF1E293B)) }

    // Saved result
    var editedResultFile by remember { mutableStateOf<File?>(null) }

    // Load page bitmap whenever current page changes
    LaunchedEffect(sourcePdfFile, currentPageIndex) {
        val file = sourcePdfFile ?: return@LaunchedEffect
        isLoadingPage = true
        currentPageBitmap = withContext(Dispatchers.IO) {
            PdfEngine.renderPageBitmap(context, file, currentPageIndex, 2.0f)
        }
        isLoadingPage = false
    }

    val openPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    val file = PdfEngine.copyUriToTempFile(context, it, "edit_src")
                    sourcePdfFile = file
                    totalPages = PdfEngine.getPageCount(file)
                    currentPageIndex = 0
                    annotationsPerPage.clear()
                    editedResultFile = null
                    activeTool = EditTool.NONE_SCROLL
                    zoomScale = 1.0f
                    panOffsetX = 0f
                    panOffsetY = 0f
                } catch (_: Exception) {}
            }
        }
    }

    Scaffold(
        topBar = {
            PdfTopBar(
                title = "Edit & Annotate PDF",
                subtitle = if (totalPages > 0) {
                    val pct = (zoomScale * 100).toInt()
                    if (activeTool == EditTool.NONE_SCROLL) "✋ Pan & Zoom Mode • $pct%"
                    else "✏️ Drawing Mode • $pct%"
                } else "Draw, sketch & write on PDF",
                onBack = { viewModel.navigateBack() },
                actions = {
                    if (sourcePdfFile != null) {
                        // Reset View to fit center
                        IconButton(
                            onClick = {
                                zoomScale = 1.0f
                                panOffsetX = 0f
                                panOffsetY = 0f
                            }
                        ) {
                            Icon(Icons.Default.FitScreen, contentDescription = "Fit to Screen", tint = MaterialTheme.colorScheme.onSurface)
                        }

                        // Save action
                        IconButton(
                            onClick = {
                                val src = sourcePdfFile ?: return@IconButton
                                viewModel.saveEditedPdf(src, annotationsPerPage.toMap()) { result ->
                                    editedResultFile = result
                                }
                            }
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save PDF", tint = PdfPrimary)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0F172A)) // Professional dark canvas backdrop
        ) {
            if (sourcePdfFile == null) {
                // Initial selection screen
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
                                Brush.linearGradient(listOf(Color(0xFFE11D48), Color(0xFF6366F1))),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Create, contentDescription = null, tint = Color.White, modifier = Modifier.size(38.dp))
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Edit & Annotate PDF",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Full 2D pan & zoom, drag in any direction to see every corner, draw with colored pencils, and add custom notes.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { openPicker.launch("application/pdf") },
                            colors = ButtonDefaults.buttonColors(containerColor = PdfPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open PDF", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.createBlankDocumentToEdit { blankFile ->
                                    sourcePdfFile = blankFile
                                    totalPages = 1
                                    currentPageIndex = 0
                                    annotationsPerPage.clear()
                                    activeTool = EditTool.NONE_SCROLL
                                    zoomScale = 1.0f
                                    panOffsetX = 0f
                                    panOffsetY = 0f
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Blank Sheet")
                        }
                    }
                }
            } else {
                // Interactive 2D Full-Screen Viewport Canvas
                // When activeTool == NONE_SCROLL (Pan Mode): dragging moves/pans the document across the screen.
                // When activeTool is Pencil, Color, or Highlighter: dragging draws sketch lines, NEVER dragging the content!
                val viewportModifier = if (activeTool == EditTool.NONE_SCROLL) {
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                zoomScale = (zoomScale * zoom).coerceIn(0.7f, 4.5f)
                                panOffsetX += pan.x
                                panOffsetY += pan.y
                            }
                        }
                } else {
                    Modifier.fillMaxSize()
                }

                BoxWithConstraints(modifier = viewportModifier) {
                    val density = LocalDensity.current
                    val viewportWidthPx = constraints.maxWidth.toFloat()
                    val viewportHeightPx = constraints.maxHeight.toFloat()

                    val pageAspect = if (currentPageBitmap != null && currentPageBitmap!!.height > 0) {
                        currentPageBitmap!!.width.toFloat() / currentPageBitmap!!.height.toFloat()
                    } else 0.707f

                    // Calculate base page size fitted into viewport with generous margins
                    val basePageWidthPx: Float
                    val basePageHeightPx: Float
                    if (viewportWidthPx / viewportHeightPx < pageAspect) {
                        basePageWidthPx = viewportWidthPx * 0.90f
                        basePageHeightPx = basePageWidthPx / pageAspect
                    } else {
                        basePageHeightPx = viewportHeightPx * 0.76f
                        basePageWidthPx = basePageHeightPx * pageAspect
                    }

                    // Render Document Page centered with Pan Offset & Zoom Scale
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset { IntOffset(panOffsetX.roundToInt(), panOffsetY.roundToInt()) }
                            .graphicsLayer(
                                scaleX = zoomScale,
                                scaleY = zoomScale,
                                transformOrigin = TransformOrigin(0.5f, 0.5f)
                            )
                            .size(
                                width = with(density) { basePageWidthPx.toDp() },
                                height = with(density) { basePageHeightPx.toDp() }
                            )
                            .shadow(12.dp, RoundedCornerShape(4.dp))
                            .background(Color.White, RoundedCornerShape(4.dp))
                    ) {
                        if (currentPageBitmap != null) {
                            Image(
                                bitmap = currentPageBitmap!!.asImageBitmap(),
                                contentDescription = "PDF Page Canvas",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.FillBounds
                            )
                        } else if (isLoadingPage) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = PdfPrimary)
                            }
                        }

                        // Drawing / Annotation Canvas
                        // Touch coordinates are local to the Page Canvas, so precision is 100% exact at any pan position or zoom scale!
                        val pageCanvasModifier = when (activeTool) {
                            EditTool.NONE_SCROLL -> Modifier.fillMaxSize()
                            EditTool.ADD_TEXT -> {
                                Modifier
                                    .fillMaxSize()
                                    .pointerInput(activeTool) {
                                        detectTapGestures { tapOffset ->
                                            val xRatio = (tapOffset.x / size.width).coerceIn(0f, 1f)
                                            val yRatio = (tapOffset.y / size.height).coerceIn(0f, 1f)
                                            if (pendingText.isNotBlank()) {
                                                val current = annotationsPerPage[currentPageIndex] ?: PageAnnotations()
                                                val newText = TextAnnotation(
                                                    text = pendingText,
                                                    xRatio = xRatio,
                                                    yRatio = yRatio,
                                                    colorArgb = pendingTextColor.toArgb(),
                                                    fontSizePt = pendingTextSize
                                                )
                                                annotationsPerPage[currentPageIndex] = current.copy(
                                                    textItems = current.textItems + newText
                                                )
                                                pendingText = ""
                                            } else {
                                                showTextDialog = true
                                            }
                                        }
                                    }
                            }
                            else -> {
                                Modifier
                                    .fillMaxSize()
                                    .pointerInput(activeTool, selectedColor, strokeThickness) {
                                        detectDragGestures(
                                            onDragStart = { startOffset ->
                                                val pt = DrawingPoint(
                                                    xRatio = (startOffset.x / size.width).coerceIn(0f, 1f),
                                                    yRatio = (startOffset.y / size.height).coerceIn(0f, 1f)
                                                )
                                                currentStrokePoints = listOf(pt)
                                            },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                val pt = DrawingPoint(
                                                    xRatio = (change.position.x / size.width).coerceIn(0f, 1f),
                                                    yRatio = (change.position.y / size.height).coerceIn(0f, 1f)
                                                )
                                                currentStrokePoints = currentStrokePoints + pt
                                            },
                                            onDragEnd = {
                                                if (currentStrokePoints.size > 1) {
                                                    val isHighlight = activeTool == EditTool.HIGHLIGHTER
                                                    val stroke = DrawingStroke(
                                                        points = currentStrokePoints,
                                                        colorArgb = selectedColor.toArgb(),
                                                        strokeWidthPt = strokeThickness,
                                                        isHighlighter = isHighlight
                                                    )
                                                    val current = annotationsPerPage[currentPageIndex] ?: PageAnnotations()
                                                    annotationsPerPage[currentPageIndex] = current.copy(
                                                        strokes = current.strokes + stroke
                                                    )
                                                }
                                                currentStrokePoints = emptyList()
                                            },
                                            onDragCancel = {
                                                currentStrokePoints = emptyList()
                                            }
                                        )
                                    }
                            }
                        }

                        Canvas(modifier = pageCanvasModifier) {
                            val canvasWidth = size.width
                            val canvasHeight = size.height

                            // 1. Draw saved strokes for this page
                            val pageAnno = annotationsPerPage[currentPageIndex]
                            if (pageAnno != null) {
                                for (stroke in pageAnno.strokes) {
                                    if (stroke.points.size < 2) continue
                                    val path = Path()
                                    val first = stroke.points[0]
                                    path.moveTo(first.xRatio * canvasWidth, first.yRatio * canvasHeight)
                                    for (i in 1 until stroke.points.size) {
                                        val p = stroke.points[i]
                                        path.lineTo(p.xRatio * canvasWidth, p.yRatio * canvasHeight)
                                    }
                                    drawPath(
                                        path = path,
                                        color = Color(stroke.colorArgb).copy(
                                            alpha = if (stroke.isHighlighter) 0.42f else 1.0f
                                        ),
                                        style = Stroke(
                                            width = stroke.strokeWidthPt * 2.0f,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                }

                                // Draw text items
                                for (txt in pageAnno.textItems) {
                                    drawContext.canvas.nativeCanvas.drawText(
                                        txt.text,
                                        txt.xRatio * canvasWidth,
                                        txt.yRatio * canvasHeight,
                                        android.graphics.Paint().apply {
                                            color = txt.colorArgb
                                            textSize = txt.fontSizePt * 2.2f
                                            isAntiAlias = true
                                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                                        }
                                    )
                                }
                            }

                            // 2. Draw active in-progress stroke
                            if (currentStrokePoints.size > 1) {
                                val isHighlight = activeTool == EditTool.HIGHLIGHTER
                                val path = Path()
                                val first = currentStrokePoints[0]
                                path.moveTo(first.xRatio * canvasWidth, first.yRatio * canvasHeight)
                                for (i in 1 until currentStrokePoints.size) {
                                    val p = currentStrokePoints[i]
                                    path.lineTo(p.xRatio * canvasWidth, p.yRatio * canvasHeight)
                                }
                                drawPath(
                                    path = path,
                                    color = selectedColor.copy(
                                        alpha = if (isHighlight) 0.42f else 1.0f
                                    ),
                                    style = Stroke(
                                        width = strokeThickness * 2.0f,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }
                    }

                    // Floating Page Navigator Bar (Top Center)
                    if (totalPages > 1) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xDD1E293B),
                            tonalElevation = 6.dp,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { if (currentPageIndex > 0) currentPageIndex-- },
                                    enabled = currentPageIndex > 0,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Page", tint = Color.White)
                                }
                                Text(
                                    text = "Page ${currentPageIndex + 1} of $totalPages",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(
                                    onClick = { if (currentPageIndex < totalPages - 1) currentPageIndex++ },
                                    enabled = currentPageIndex < totalPages - 1,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Page", tint = Color.White)
                                }
                            }
                        }
                    }

                    // Floating Tool Dock (Bottom)
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(0.96f)
                            .padding(bottom = 12.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(22.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Row 1: Tools (Pan, Color, Pencil, Highlight, Text)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // ✋ Pan & Drag Mode
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (activeTool == EditTool.NONE_SCROLL) ToolEmerald else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .clickable { activeTool = EditTool.NONE_SCROLL }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.PanTool,
                                            contentDescription = "Pan View",
                                            tint = if (activeTool == EditTool.NONE_SCROLL) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Pan",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.5.sp,
                                                color = if (activeTool == EditTool.NONE_SCROLL) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }

                                // Colored Pencil
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (activeTool == EditTool.COLORED_PENCIL) ToolCoral else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            activeTool = if (activeTool == EditTool.COLORED_PENCIL) EditTool.NONE_SCROLL else EditTool.COLORED_PENCIL
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Create,
                                            contentDescription = "Color Pencil",
                                            tint = if (activeTool == EditTool.COLORED_PENCIL) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Color",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = if (activeTool == EditTool.COLORED_PENCIL) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }

                                // Graphite Pencil
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (activeTool == EditTool.GRAPHITE_PENCIL) Color(0xFF1E293B) else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            if (activeTool == EditTool.GRAPHITE_PENCIL) {
                                                activeTool = EditTool.NONE_SCROLL
                                            } else {
                                                activeTool = EditTool.GRAPHITE_PENCIL
                                                selectedColor = Color(0xFF1E293B)
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Brush,
                                            contentDescription = "Graphite Pencil",
                                            tint = if (activeTool == EditTool.GRAPHITE_PENCIL) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Pencil",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = if (activeTool == EditTool.GRAPHITE_PENCIL) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }

                                // Highlighter
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (activeTool == EditTool.HIGHLIGHTER) Color(0xFFEAB308) else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .clickable {
                                            if (activeTool == EditTool.HIGHLIGHTER) {
                                                activeTool = EditTool.NONE_SCROLL
                                            } else {
                                                activeTool = EditTool.HIGHLIGHTER
                                                selectedColor = Color(0xFFFACC15)
                                                strokeThickness = 14.0f
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Highlight,
                                            contentDescription = "Highlighter",
                                            tint = if (activeTool == EditTool.HIGHLIGHTER) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Highlight",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = if (activeTool == EditTool.HIGHLIGHTER) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }

                                // Add Text
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (activeTool == EditTool.ADD_TEXT) ToolIndigo else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .weight(0.9f)
                                        .clickable {
                                            activeTool = EditTool.ADD_TEXT
                                            showTextDialog = true
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.TextFields,
                                            contentDescription = "Text",
                                            tint = if (activeTool == EditTool.ADD_TEXT) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Text",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = if (activeTool == EditTool.ADD_TEXT) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Row 2: Zoom and View Controls (Zoom Out, Presets, Zoom In, Reset View, Undo)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Zoom Buttons
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { zoomScale = (zoomScale - 0.25f).coerceAtLeast(0.7f) },
                                        enabled = zoomScale > 0.7f,
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", modifier = Modifier.size(16.dp))
                                    }

                                    listOf(1.0f to "1x", 1.5f to "1.5x", 2.0f to "2x", 3.0f to "3x").forEach { (sc, lbl) ->
                                        val isSelected = abs(zoomScale - sc) < 0.12f
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSelected) PdfPrimary else Color.Transparent,
                                            modifier = Modifier
                                                .padding(horizontal = 2.dp)
                                                .clickable { zoomScale = sc }
                                        ) {
                                            Text(
                                                text = lbl,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 11.sp
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { zoomScale = (zoomScale + 0.25f).coerceAtMost(4.5f) },
                                        enabled = zoomScale < 4.5f,
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", modifier = Modifier.size(16.dp))
                                    }
                                }

                                // Reset Pan & Center Button
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.clickable {
                                        zoomScale = 1.0f
                                        panOffsetX = 0f
                                        panOffsetY = 0f
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.FitScreen, contentDescription = "Center View", modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Center",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }

                                // Undo Button
                                IconButton(
                                    onClick = {
                                        val currentAnno = annotationsPerPage[currentPageIndex] ?: return@IconButton
                                        if (currentAnno.strokes.isNotEmpty()) {
                                            annotationsPerPage[currentPageIndex] = currentAnno.copy(
                                                strokes = currentAnno.strokes.dropLast(1)
                                            )
                                        } else if (currentAnno.textItems.isNotEmpty()) {
                                            annotationsPerPage[currentPageIndex] = currentAnno.copy(
                                                textItems = currentAnno.textItems.dropLast(1)
                                            )
                                        }
                                    },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(Icons.Default.Undo, contentDescription = "Undo", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                                }
                            }

                            // Optional Row 3: Colors (only when Colored Pencil active)
                            if (activeTool == EditTool.COLORED_PENCIL) {
                                Spacer(modifier = Modifier.height(8.dp))
                                val paletteColors = listOf(
                                    Color(0xFFEF4444), // Crimson Red
                                    Color(0xFF3B82F6), // Royal Blue
                                    Color(0xFF10B981), // Emerald Green
                                    Color(0xFFF59E0B), // Warm Amber
                                    Color(0xFF8B5CF6), // Purple
                                    Color(0xFFF43F5E), // Coral
                                    Color(0xFF0F172A), // Midnight Dark
                                    Color(0xFFFFFFFF)  // White
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    paletteColors.forEach { color ->
                                        val isChosen = selectedColor == color
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .border(
                                                    width = if (isChosen) 3.dp else 1.dp,
                                                    color = if (isChosen) PdfPrimary else Color.LightGray,
                                                    shape = CircleShape
                                                )
                                                .clickable { selectedColor = color }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Results Card Dialog after Saving
            editedResultFile?.let { result ->
                AlertDialog(
                    onDismissRequest = { editedResultFile = null },
                    icon = {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenSuccess, modifier = Modifier.size(36.dp))
                    },
                    title = {
                        Text("PDF Saved Successfully!", fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column {
                            Text("${result.name} (${result.length() / 1024} KB)")
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Your drawings, pencil marks, and text annotations are baked into the PDF.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                editedResultFile = null
                                viewModel.setDirectActiveFile(result)
                                viewModel.navigateTo(ScreenDestination.VIEWER)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PdfPrimary)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View in App")
                        }
                    },
                    dismissButton = {
                        Row {
                            OutlinedButton(
                                onClick = {
                                    viewModel.openPdfInSystemViewer(context, result)
                                    editedResultFile = null
                                }
                            ) {
                                Text("Open External")
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            OutlinedButton(
                                onClick = {
                                    viewModel.shareFile(context, result)
                                    editedResultFile = null
                                }
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                )
            }
        }
    }

    // Add Text Dialog
    if (showTextDialog) {
        var inputDraft by remember { mutableStateOf(pendingText) }
        AlertDialog(
            onDismissRequest = { showTextDialog = false },
            title = {
                Text("Add Text Annotation", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = inputDraft,
                        onValueChange = { inputDraft = it },
                        label = { Text("Annotation Text") },
                        placeholder = { Text("e.g. Approved, Signed, Note...") },
                        singleLine = false,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Font Size", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(14f to "Small", 18f to "Medium", 26f to "Large").forEach { (sz, lbl) ->
                            val isSel = pendingTextSize == sz
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) ToolIndigo else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { pendingTextSize = sz }
                            ) {
                                Text(
                                    text = lbl,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
                                )
                            }
                        }
                    }

                    Text("Text Color", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(
                            Color(0xFF0F172A),
                            Color(0xFFDC2626),
                            Color(0xFF2563EB),
                            Color(0xFF059669),
                            Color(0xFFD97706)
                        ).forEach { col ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .border(
                                        width = if (pendingTextColor == col) 2.5.dp else 1.dp,
                                        color = if (pendingTextColor == col) ToolIndigo else Color.LightGray,
                                        shape = CircleShape
                                    )
                                    .clickable { pendingTextColor = col }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputDraft.isNotBlank()) {
                            pendingText = inputDraft
                            val current = annotationsPerPage[currentPageIndex] ?: PageAnnotations()
                            val newText = TextAnnotation(
                                text = inputDraft,
                                xRatio = 0.15f,
                                yRatio = 0.20f,
                                colorArgb = pendingTextColor.toArgb(),
                                fontSizePt = pendingTextSize
                            )
                            annotationsPerPage[currentPageIndex] = current.copy(
                                textItems = current.textItems + newText
                            )
                            pendingText = ""
                        }
                        showTextDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ToolIndigo),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Add to Page", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
