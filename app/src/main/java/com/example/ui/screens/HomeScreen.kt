package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RecentFileEntity
import com.example.ui.components.PrivacyBadge
import com.example.ui.components.WorldClassFeatureCard
import com.example.ui.theme.CardBorderGradient
import com.example.ui.theme.PdfPrimary
import com.example.ui.theme.ToolCoral
import com.example.ui.theme.ToolEmerald
import com.example.ui.theme.ToolIndigo
import com.example.ui.viewmodel.PdfViewModel
import com.example.ui.viewmodel.ScreenDestination
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(viewModel: PdfViewModel) {
    val context = LocalContext.current
    val recentFiles by viewModel.recentFiles.collectAsState()

    val openPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.openPdfFromUri(it, "Opened in Viewer") }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 36.dp)
    ) {
        // Hero Header Section
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "PDF Toolkit",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 28.sp,
                                        letterSpacing = (-0.5).sp
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = PdfPrimary
                                ) {
                                    Text(
                                        text = "PRO",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Lightning-fast • 100% Offline • Private",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            )
                        }
                        PrivacyBadge()
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Hero Quick-Launch Glassmorphic Card
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CardBorderGradient, RoundedCornerShape(22.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(
                                        Brush.linearGradient(listOf(PdfPrimary, Color(0xFF8B5CF6))),
                                        RoundedCornerShape(14.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = "Open Document",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Open & Read PDF",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                )
                                Text(
                                    text = "Pinch-zoom, vertical scroll & instant viewer",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                            Button(
                                onClick = { openPdfLauncher.launch("application/pdf") },
                                colors = ButtonDefaults.buttonColors(containerColor = PdfPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("open_pdf_main_button")
                            ) {
                                Text("Open", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Recent Documents Carousel
        if (recentFiles.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Files",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                letterSpacing = (-0.2).sp
                            )
                        )
                        IconButton(onClick = { viewModel.clearRecentFiles() }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Clear Recents",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(recentFiles.take(8)) { recent ->
                            RecentFileCard(
                                item = recent,
                                onClick = {
                                    val f = File(recent.filePath)
                                    if (f.exists()) {
                                        viewModel.setDirectActiveFile(f)
                                        viewModel.navigateTo(ScreenDestination.VIEWER)
                                    }
                                },
                                onShare = {
                                    val f = File(recent.filePath)
                                    if (f.exists()) {
                                        viewModel.shareFile(context, f)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Section Title: Powerful Core Tools
        item {
            Row(
                modifier = Modifier.padding(start = 22.dp, top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CORE TOOLKIT",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.4.sp,
                        fontSize = 11.5.sp
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        // The 3 Core Tools
        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Edit & Annotate PDF
                WorldClassFeatureCard(
                    title = "Edit & Annotate PDF",
                    description = "Draw with colored pencil or graphite, highlight text, and add custom notes.",
                    icon = Icons.Default.Create,
                    accentColor = ToolEmerald,
                    gradient = Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF059669))),
                    badgeText = "Pencil & Text",
                    onClick = { viewModel.navigateTo(ScreenDestination.EDIT) }
                )

                // 2. Images to PDF
                WorldClassFeatureCard(
                    title = "Images to PDF",
                    description = "Combine multiple gallery photos into a clean PDF with custom page sizes.",
                    icon = Icons.Default.Image,
                    accentColor = ToolCoral,
                    gradient = Brush.linearGradient(listOf(Color(0xFFF43F5E), Color(0xFFE11D48))),
                    badgeText = "Fast Convert",
                    onClick = { viewModel.navigateTo(ScreenDestination.IMAGE_TO_PDF) }
                )

                // 3. Merge PDFs
                WorldClassFeatureCard(
                    title = "Merge PDFs",
                    description = "Combine multiple PDF documents together into a single file in any order.",
                    icon = Icons.Default.MergeType,
                    accentColor = ToolIndigo,
                    gradient = Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF4F46E5))),
                    badgeText = "Multi-Doc Combine",
                    onClick = { viewModel.navigateTo(ScreenDestination.MERGE) }
                )
            }
        }
    }
}

@Composable
fun RecentFileCard(
    item: RecentFileEntity,
    onClick: () -> Unit,
    onShare: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(item.timestamp))
    val sizeKb = (item.fileSizeBytes / 1024).coerceAtLeast(1)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .width(185.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PdfPrimary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = item.operationType,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = PdfPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                    )
                }
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${item.pageCount} page(s) • ${sizeKb} KB",
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                maxLines = 1
            )
            Text(
                text = dateStr,
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline),
                fontSize = 10.sp
            )
        }
    }
}

