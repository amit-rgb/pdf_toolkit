package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.engine.PageSizePreset
import com.example.ui.components.PdfTopBar
import com.example.ui.components.PrivacyBadge
import com.example.ui.theme.CardBorderGradient
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.PdfPrimary
import com.example.ui.theme.ToolCoral
import com.example.ui.viewmodel.PdfViewModel
import com.example.ui.viewmodel.ScreenDestination
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageToPdfScreen(viewModel: PdfViewModel) {
    val context = LocalContext.current
    BackHandler { viewModel.navigateBack() }

    val imageUris = remember { mutableStateListOf<Uri>() }
    val rotations = remember { mutableStateListOf<Int>() }
    var resultPdfFile by remember { mutableStateOf<File?>(null) }

    var selectedPreset by remember { mutableStateOf(PageSizePreset.A4) }
    var isLandscape by remember { mutableStateOf(false) }
    var marginPt by remember { mutableIntStateOf(18) }
    var isExpandedDropdown by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        for (u in uris) {
            imageUris.add(u)
            rotations.add(0)
        }
    }

    Scaffold(
        topBar = {
            PdfTopBar(
                title = "Images to PDF",
                subtitle = "Convert multiple photos to document",
                onBack = { viewModel.navigateBack() }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                PrivacyBadge(modifier = Modifier.fillMaxWidth())
            }

            // Image Selection Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Selected Photos (${imageUris.size})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                letterSpacing = (-0.2).sp
                            )
                        )
                        Text(
                            text = "Add, reorder, or rotate before converting",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ToolCoral),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Photos", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (imageUris.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CardBorderGradient, RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(
                                        Brush.linearGradient(listOf(ToolCoral.copy(alpha = 0.2f), Color(0xFF8B5CF6).copy(alpha = 0.2f))),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = ToolCoral, modifier = Modifier.size(30.dp))
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text("No Photos Selected", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Pick JPG, JPEG, or PNG images to merge into a single PDF document",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ToolCoral),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Choose Photos from Gallery", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // Images List with Reorder and Rotate
                itemsIndexed(imageUris) { index, uri ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = "Image $index",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Page ${index + 1}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                )
                                Text(
                                    text = "Rotation: ${rotations[index]}°",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }

                            // Rotate
                            IconButton(
                                onClick = {
                                    rotations[index] = (rotations[index] + 90) % 360
                                }
                            ) {
                                Icon(Icons.Default.RotateRight, contentDescription = "Rotate", modifier = Modifier.size(20.dp))
                            }

                            // Move Up
                            IconButton(
                                onClick = {
                                    if (index > 0) {
                                        val tempUri = imageUris[index]
                                        imageUris[index] = imageUris[index - 1]
                                        imageUris[index - 1] = tempUri

                                        val tempRot = rotations[index]
                                        rotations[index] = rotations[index - 1]
                                        rotations[index - 1] = tempRot
                                    }
                                },
                                enabled = index > 0
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(18.dp))
                            }

                            // Move Down
                            IconButton(
                                onClick = {
                                    if (index < imageUris.size - 1) {
                                        val tempUri = imageUris[index]
                                        imageUris[index] = imageUris[index + 1]
                                        imageUris[index + 1] = tempUri

                                        val tempRot = rotations[index]
                                        rotations[index] = rotations[index + 1]
                                        rotations[index + 1] = tempRot
                                    }
                                },
                                enabled = index < imageUris.size - 1
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(18.dp))
                            }

                            // Delete
                            IconButton(
                                onClick = {
                                    imageUris.removeAt(index)
                                    rotations.removeAt(index)
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // Page Layout Options
                item {
                    Text(
                        text = "Document Page Settings",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Preset Dropdown
                    ExposedDropdownMenuBox(
                        expanded = isExpandedDropdown,
                        onExpandedChange = { isExpandedDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = selectedPreset.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Page Size") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpandedDropdown) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = isExpandedDropdown,
                            onDismissRequest = { isExpandedDropdown = false }
                        ) {
                            PageSizePreset.values().forEach { preset ->
                                DropdownMenuItem(
                                    text = { Text(preset.label) },
                                    onClick = {
                                        selectedPreset = preset
                                        isExpandedDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Orientation Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = !isLandscape,
                            onClick = { isLandscape = false },
                            label = { Text("Portrait") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = isLandscape,
                            onClick = { isLandscape = true },
                            label = { Text("Landscape") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Margins Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = marginPt == 0,
                            onClick = { marginPt = 0 },
                            label = { Text("No Margin") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = marginPt == 18,
                            onClick = { marginPt = 18 },
                            label = { Text("Normal (18pt)") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = marginPt == 36,
                            onClick = { marginPt = 36 },
                            label = { Text("Wide (36pt)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Generate PDF Button
                item {
                    Button(
                        onClick = {
                            viewModel.createImagesPdf(
                                uris = imageUris.toList(),
                                pageSize = selectedPreset,
                                isLandscape = isLandscape,
                                margin = marginPt,
                                rotations = rotations.toList()
                            ) { result ->
                                resultPdfFile = result
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ToolCoral),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create PDF (${imageUris.size} Pages)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            // Result
            resultPdfFile?.let { result ->
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, GreenSuccess.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenSuccess, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "PDF Created Successfully!",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = GreenSuccess)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${result.name} • ${result.length() / 1024} KB",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.setDirectActiveFile(result)
                                        viewModel.navigateTo(ScreenDestination.VIEWER)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PdfPrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("View in App (Vertical Paging)", fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.openPdfInSystemViewer(context, result) },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Open External")
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.shareFile(context, result) },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Share")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
