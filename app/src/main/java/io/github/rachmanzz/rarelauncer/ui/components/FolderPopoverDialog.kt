package io.github.rachmanzz.rarelauncer.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.rachmanzz.rarelauncer.data.model.AppDirectory
import io.github.rachmanzz.rarelauncer.data.model.AppItem
import io.github.rachmanzz.rarelauncer.ui.theme.AetherGlassBorder
import io.github.rachmanzz.rarelauncer.ui.theme.AetherGlassBorderSubtle
import io.github.rachmanzz.rarelauncer.ui.theme.AetherOnSurface
import io.github.rachmanzz.rarelauncer.ui.theme.AetherOnSurfaceVariant
import io.github.rachmanzz.rarelauncer.ui.theme.AetherPrimary
import io.github.rachmanzz.rarelauncer.ui.theme.AetherSurfaceBright
import io.github.rachmanzz.rarelauncer.ui.theme.AetherSurfaceContainer

/**
 * 1:1 Floating Interactive Pop-Over for Directory/Folder Expansion.
 * - Strictly max 4 apps can be selected.
 * - Hidden apps (biometric vault apps) are excluded and cannot be selected.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderPopoverDialog(
    directory: AppDirectory,
    allApps: List<AppItem>,
    modifier: Modifier = Modifier,
    hiddenApps: Set<String> = emptySet(),
    onDismissRequest: () -> Unit,
    onAppClicked: (AppItem) -> Unit,
    onRemoveApp: (packageName: String) -> Unit,
    onAddApp: (packageName: String) -> Unit,
    onToggleApp: (packageName: String) -> Unit,
    onClearDirectory: () -> Unit
) {
    // Strictly filter out any hidden/vault apps
    val visibleApps = remember(allApps, hiddenApps) {
        allApps.filter { it.packageName !in hiddenApps }
    }
    val appMap = remember(visibleApps) { visibleApps.associateBy { it.packageName } }
    // Take at most 4 apps that are NOT hidden
    val dirApps = remember(directory.packageNames, visibleApps) {
        directory.packageNames.filter { it !in hiddenApps }.take(4).mapNotNull { appMap[it] }
    }
    var showAppPicker by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xF21F232D))
                    .border(1.dp, AetherGlassBorder, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                // Header: Cyan dot + Folder Title + Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(AetherPrimary)
                        )
                        Text(
                            text = directory.name,
                            color = AetherOnSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9999.dp))
                                .background(AetherPrimary.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${dirApps.size}/4 Apps",
                                color = AetherPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (dirApps.isNotEmpty()) {
                            IconButton(
                                onClick = onClearDirectory,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(AetherSurfaceBright)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Clear all",
                                    tint = AetherOnSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismissRequest,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(AetherSurfaceBright)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = AetherOnSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2x2 Items Grid (Matching the 4-slot directory layout)
                if (dirApps.isNotEmpty()) {
                    val rows = dirApps.chunked(2)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rows.forEach { rowApps ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowApps.forEach { app ->
                                    Box(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        PopoverAppItem(
                                            app = app,
                                            onAppClicked = {
                                                onDismissRequest()
                                                onAppClicked(app)
                                            },
                                            onRemoveApp = {
                                                onRemoveApp(app.packageName)
                                            }
                                        )
                                    }
                                }
                                repeat(2 - rowApps.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AetherSurfaceBright.copy(alpha = 0.2f))
                            .clickable { showAppPicker = true }
                            .padding(vertical = 28.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = AetherPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "No apps selected yet",
                                color = AetherOnSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Choose up to 4 apps for this directory",
                                color = AetherOnSurfaceVariant,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Bar: "X/4 Selected" + "+ CHOOSE APPS" Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${dirApps.size}/4 Selected",
                        color = if (dirApps.size == 4) AetherPrimary else AetherOnSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = if (dirApps.size == 4) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(AetherPrimary.copy(alpha = 0.15f))
                            .border(1.dp, AetherPrimary.copy(alpha = 0.4f), RoundedCornerShape(9999.dp))
                            .clickable { showAppPicker = true }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = AetherPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (dirApps.size >= 4) "MANAGE APPS (4/4)" else "CHOOSE APPS (MAX 4)",
                                color = AetherPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Multi-Select App Picker Dialog (Excludes hidden apps; strictly enforces max 4 apps)
    if (showAppPicker) {
        var pickerSearchQuery by remember { mutableStateOf("") }
        val filteredPickerApps = remember(visibleApps, pickerSearchQuery) {
            if (pickerSearchQuery.isBlank()) {
                visibleApps
            } else {
                val q = pickerSearchQuery.trim().lowercase()
                visibleApps.filter { it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
            }
        }

        BasicAlertDialog(
            onDismissRequest = { showAppPicker = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF171A23))
                    .border(1.dp, AetherGlassBorder, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Select Directory Apps (Max 4)",
                            color = AetherOnSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${dirApps.size} / 4 apps selected",
                            color = if (dirApps.size >= 4) AetherPrimary else AetherOnSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = if (dirApps.size >= 4) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    IconButton(
                        onClick = { showAppPicker = false },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = AetherOnSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar inside picker
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(9999.dp))
                        .background(Color(0xFF0B0E14))
                        .border(1.dp, AetherGlassBorderSubtle, RoundedCornerShape(9999.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = AetherPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (pickerSearchQuery.isEmpty()) {
                            Text(
                                text = "Search apps...",
                                color = AetherOnSurfaceVariant.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                        }
                        BasicTextField(
                            value = pickerSearchQuery,
                            onValueChange = { pickerSearchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = AetherOnSurface,
                                fontSize = 12.sp
                            ),
                            cursorBrush = SolidColor(AetherPrimary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (pickerSearchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { pickerSearchQuery = "" },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = AetherOnSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // App list with checkboxes (Max 4 limit strictly enforced)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredPickerApps, key = { it.packageName }) { app ->
                        val isSelected = app.packageName in directory.packageNames
                        val isMaxReached = !isSelected && dirApps.size >= 4

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when {
                                        isSelected -> AetherPrimary.copy(alpha = 0.12f)
                                        isMaxReached -> AetherSurfaceBright.copy(alpha = 0.1f)
                                        else -> AetherSurfaceBright.copy(alpha = 0.25f)
                                    }
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) AetherPrimary.copy(alpha = 0.4f) else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    if (isSelected || !isMaxReached) {
                                        onToggleApp(app.packageName)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (app.icon != null) {
                                Image(
                                    bitmap = app.icon,
                                    contentDescription = app.label,
                                    modifier = Modifier.size(30.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(AetherSurfaceBright),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = app.label.take(1).uppercase(),
                                        color = AetherPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = app.label,
                                color = when {
                                    isSelected -> AetherPrimary
                                    isMaxReached -> AetherOnSurfaceVariant.copy(alpha = 0.4f)
                                    else -> AetherOnSurface
                                },
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            if (isMaxReached) {
                                Text(
                                    text = "MAX 4",
                                    color = AetherOnSurfaceVariant.copy(alpha = 0.5f),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }

                            // Checkbox indicator
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isSelected) AetherPrimary else Color.Transparent
                                    )
                                    .border(
                                        1.dp,
                                        when {
                                            isSelected -> AetherPrimary
                                            isMaxReached -> AetherOnSurfaceVariant.copy(alpha = 0.2f)
                                            else -> AetherOnSurfaceVariant.copy(alpha = 0.5f)
                                        },
                                        RoundedCornerShape(6.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.Black,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Done button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AetherPrimary)
                        .clickable { showAppPicker = false }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DONE",
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun PopoverAppItem(
    app: AppItem,
    onAppClicked: () -> Unit,
    onRemoveApp: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AetherSurfaceBright.copy(alpha = 0.35f))
                .clickable { onAppClicked() }
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AetherSurfaceContainer),
                contentAlignment = Alignment.Center
            ) {
                if (app.icon != null) {
                    Image(
                        bitmap = app.icon,
                        contentDescription = app.label,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Text(
                        text = app.label.take(1).uppercase(),
                        color = AetherPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = app.label,
                color = AetherOnSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Quick remove button (top right of item)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color(0xD90B0E14))
                .border(1.dp, AetherGlassBorderSubtle, CircleShape)
                .clickable { onRemoveApp() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                tint = AetherOnSurfaceVariant,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 600)
@Composable
fun FolderPopoverDialogPreview() {
    FolderPopoverDialog(
        directory = AppDirectory(
            id = "prod_1",
            name = "Productivity",
            packageNames = listOf("com.android.calculator2", "com.google.android.calendar")
        ),
        allApps = listOf(
            AppItem("Calculator", "com.android.calculator2", ""),
            AppItem("Calendar", "com.google.android.calendar", ""),
            AppItem("Camera", "com.android.camera", "")
        ),
        hiddenApps = emptySet(),
        onDismissRequest = {},
        onAppClicked = {},
        onRemoveApp = {},
        onAddApp = {},
        onToggleApp = {},
        onClearDirectory = {}
    )
}
