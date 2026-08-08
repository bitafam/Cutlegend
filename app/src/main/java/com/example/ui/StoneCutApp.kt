package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoneCutApp(viewModel: StoneCutViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Settings & Inventory", "Parts & Veins", "Optimized Layout Map")

    val standardSlab by viewModel.standardSlab.collectAsState()
    val scrapInventory by viewModel.scrapInventory.collectAsState()
    val useScrap by viewModel.useScrap.collectAsState()
    val parts by viewModel.parts.collectAsState()
    val diskThickness by viewModel.diskThickness.collectAsState()
    val trimMargin by viewModel.trimMargin.collectAsState()
    val result by viewModel.optimizationResult.collectAsState()

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Cyber circular glowing emblem
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    .background(Color.Transparent)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "LITHOS OPTIMA",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "ENGINE v4.2 // READY",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    // Demo load buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(
                            onClick = { viewModel.loadLShapedCountertopTemplate() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("L-Kitchen", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { viewModel.loadWallCladdingTemplate() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Cladding", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, label ->
                        val selected = selectedTab == index
                        val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        val icon = when (index) {
                            0 -> "🧱"
                            1 -> "📦"
                            else -> "📐"
                        }
                        val shortLabel = when (index) {
                            0 -> "STOCK"
                            1 -> "PROJECT"
                            else -> "NESTING"
                        }
                        
                        Column(
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { selectedTab = index }
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = icon,
                                fontSize = 22.sp,
                                modifier = if (selected) Modifier.scale(1.1f) else Modifier
                            )
                            Text(
                                text = shortLabel,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = color
                            )
                            if (selected) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp, 2.dp)
                                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(1.dp))
                                )
                            } else {
                                Spacer(modifier = Modifier.size(16.dp, 2.dp))
                            }
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Summary stats banner
            SummaryQuickBanner(result = result)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = MaterialTheme.colorScheme.background
            ) {
                when (selectedTab) {
                    0 -> InventorySettingsTab(
                        viewModel = viewModel,
                        standardSlab = standardSlab,
                        scrapInventory = scrapInventory,
                        useScrap = useScrap,
                        diskThickness = diskThickness,
                        trimMargin = trimMargin
                    )
                    1 -> PartsListTab(
                        viewModel = viewModel,
                        parts = parts
                    )
                    2 -> VisualMapTab(
                        viewModel = viewModel,
                        result = result,
                        diskThickness = diskThickness,
                        trimMargin = trimMargin
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryQuickBanner(result: OptimizationResult?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "YIELD EFFICIENCY",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format("%.1f", result?.totalYieldPercentage ?: 0f),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }
            
            VerticalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), 
                modifier = Modifier.height(40.dp)
            )
            
            Column {
                Text(
                    text = "SLABS USED",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${result?.standardSlabsUsedCount ?: 0}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            VerticalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), 
                modifier = Modifier.height(40.dp)
            )
            
            Column {
                Text(
                    text = "SCRAP REUSED",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${result?.scrapPiecesUsedCount ?: 0}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun InventorySettingsTab(
    viewModel: StoneCutViewModel,
    standardSlab: StandardSlab,
    scrapInventory: List<ScrapPiece>,
    useScrap: Boolean,
    diskThickness: Float,
    trimMargin: Float
) {
    var editSlabL by remember(standardSlab) { mutableStateOf(standardSlab.length.toInt().toString()) }
    var editSlabW by remember(standardSlab) { mutableStateOf(standardSlab.width.toInt().toString()) }
    var editSlabT by remember(standardSlab) { mutableStateOf(standardSlab.thickness.toInt().toString()) }

    var editKerf by remember(diskThickness) { mutableStateOf(diskThickness.toString()) }
    var editTrim by remember(trimMargin) { mutableStateOf(trimMargin.toString()) }

    var newScrapL by remember { mutableStateOf("") }
    var newScrapW by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Standard Slab Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("standard_slab_card"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Standard Slab Dimensions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = editSlabL,
                            onValueChange = {
                                editSlabL = it
                                it.toFloatOrNull()?.let { l ->
                                    viewModel.updateStandardSlab(l, editSlabW.toFloatOrNull() ?: 1800f, editSlabT.toFloatOrNull() ?: 20f)
                                }
                            },
                            label = { Text("Length L (mm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("slab_length_input")
                        )
                        OutlinedTextField(
                            value = editSlabW,
                            onValueChange = {
                                editSlabW = it
                                it.toFloatOrNull()?.let { w ->
                                    viewModel.updateStandardSlab(editSlabL.toFloatOrNull() ?: 3000f, w, editSlabT.toFloatOrNull() ?: 20f)
                                }
                            },
                            label = { Text("Width W (mm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("slab_width_input")
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editSlabT,
                        onValueChange = {
                            editSlabT = it
                            it.toFloatOrNull()?.let { t ->
                                viewModel.updateStandardSlab(editSlabL.toFloatOrNull() ?: 3000f, editSlabW.toFloatOrNull() ?: 1800f, t)
                            }
                        },
                        label = { Text("Thickness T (mm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Machine Parameters Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Machine & Saw Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = editKerf,
                            onValueChange = {
                                editKerf = it
                                it.toFloatOrNull()?.let { k ->
                                    viewModel.updateMachineParameters(k, trimMargin)
                                }
                            },
                            label = { Text("Blade Kerf (mm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).testTag("blade_kerf_input")
                        )
                        OutlinedTextField(
                            value = editTrim,
                            onValueChange = {
                                editTrim = it
                                it.toFloatOrNull()?.let { t ->
                                    viewModel.updateMachineParameters(diskThickness, t)
                                }
                            },
                            label = { Text("Trim Margin (mm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).testTag("trim_margin_input")
                        )
                    }
                }
            }
        }

        // Scrap Pieces Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Scrap Inventory / Offcuts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Use Scrap", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Checkbox(
                                checked = useScrap,
                                onCheckedChange = { viewModel.setUseScrap(it) },
                                modifier = Modifier.testTag("use_scrap_checkbox")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Prioritize using these pieces before cutting new slabs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))

                    // Add scrap form
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newScrapL,
                            onValueChange = { newScrapL = it },
                            label = { Text("L (mm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newScrapW,
                            onValueChange = { newScrapW = it },
                            label = { Text("W (mm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                val l = newScrapL.toFloatOrNull()
                                val w = newScrapW.toFloatOrNull()
                                if (l != null && w != null) {
                                    viewModel.addScrap(l, w)
                                    newScrapL = ""
                                    newScrapW = ""
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            modifier = Modifier.testTag("add_scrap_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Scrap")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (scrapInventory.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No scrap pieces registered", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            scrapInventory.forEach { scrap ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (scrap.isEnabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Switch(
                                            checked = scrap.isEnabled,
                                            onCheckedChange = { viewModel.toggleScrapEnabled(scrap.id) },
                                            modifier = Modifier.scale(0.8f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Scrap #${scrap.id}: ${scrap.length.toInt()} x ${scrap.width.toInt()} mm",
                                            fontWeight = if (scrap.isEnabled) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (scrap.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.removeScrap(scrap.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun PartsListTab(viewModel: StoneCutViewModel, parts: List<Part>) {
    var name by remember { mutableStateOf("") }
    var lStr by remember { mutableStateOf("") }
    var wStr by remember { mutableStateOf("") }
    var allowRotation by remember { mutableStateOf(false) }
    var matchAdjacentTo by remember { mutableStateOf("") }

    var expandedDropdown by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Add Part Input Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Add Piece to Cut",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Piece Name / Location") },
                        modifier = Modifier.fillMaxWidth().testTag("part_name_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = lStr,
                            onValueChange = { lStr = it },
                            label = { Text("Length W (Y, mm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("part_length_input")
                        )
                        OutlinedTextField(
                            value = wStr,
                            onValueChange = { wStr = it },
                            label = { Text("Width L (X, mm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("part_width_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = allowRotation,
                                onCheckedChange = { allowRotation = it },
                                modifier = Modifier.testTag("part_allow_rotation_checkbox")
                            )
                            Text("Allow 90° Rotation", fontSize = 14.sp)
                        }

                        // Match Adjacent dropdown
                        Box {
                            Button(
                                onClick = { expandedDropdown = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                            ) {
                                Text(if (matchAdjacentTo.isEmpty()) "Vein Connection" else "Match Adjacent to: $matchAdjacentTo")
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Veins")
                            }
                            DropdownMenu(
                                expanded = expandedDropdown,
                                onDismissRequest = { expandedDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("No Vein Continuity (Independent)") },
                                    onClick = {
                                        matchAdjacentTo = ""
                                        expandedDropdown = false
                                    }
                                )
                                parts.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text("Continuous with Part ${p.id} (${p.name})") },
                                        onClick = {
                                            matchAdjacentTo = p.id
                                            expandedDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val l = lStr.toFloatOrNull()
                            val w = wStr.toFloatOrNull()
                            if (name.isNotEmpty() && l != null && w != null) {
                                viewModel.addPart(name, l, w, allowRotation, matchAdjacentTo)
                                name = ""
                                lStr = ""
                                wStr = ""
                                allowRotation = false
                                matchAdjacentTo = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("add_part_submit_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add to Cutting List")
                    }
                }
            }
        }

        // Table Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Required Cuts List",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${parts.size} items",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (parts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No parts added yet. Click a demo template above to populate!", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(parts) { part ->
                PartRowItem(part = part, onDelete = { viewModel.removePart(part.id) })
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun PartRowItem(part: Part, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("part_row_${part.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = part.id,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = part.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        text = "${part.width.toInt()} x ${part.length.toInt()} mm (Area: ${(part.width * part.length / 1000000).format(2)} m²)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (part.allowRotation) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text("Rotatable", fontSize = 10.sp) },
                                modifier = Modifier.scale(0.85f)
                            )
                        } else {
                            SuggestionChip(
                                onClick = {},
                                label = { Text("Fixed Vein direction", fontSize = 10.sp) },
                                modifier = Modifier.scale(0.85f),
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                )
                            )
                        }

                        if (part.matchAdjacentTo.isNotEmpty()) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text("Vein alignment ➔ Part ${part.matchAdjacentTo}", fontSize = 10.sp) },
                                modifier = Modifier.scale(0.85f),
                                icon = {
                                    Icon(
                                        Icons.Default.Link,
                                        contentDescription = "Matched",
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                        }
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete part", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun VisualMapTab(viewModel: StoneCutViewModel, result: OptimizationResult?, diskThickness: Float, trimMargin: Float) {
    if (result == null || result.slabLayouts.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterAlignment) {
                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text("No layouts generated. Please add parts to cutting list first.", textAlign = TextAlign.Center)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Interactive Cutting Layout Maps",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Each slab is rendered below with its exact parts, cut lines, and grain flow direction.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(result.slabLayouts) { layout ->
            SlabLayoutCard(layout = layout, diskThickness = diskThickness, trimMargin = trimMargin)
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun SlabLayoutCard(layout: SlabLayout, diskThickness: Float, trimMargin: Float) {
    val checkedSteps = remember { mutableStateMapOf<Int, Boolean>() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("layout_card_${layout.containerId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = layout.containerId,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "${layout.originalLength.toInt()} x ${layout.originalWidth.toInt()} mm " +
                                if (layout.isScrap) "(Reused Scrap Piece)" else "(Standard Slab)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = String.format("%.1f%% Yield", layout.efficiency),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Graphical Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
            ) {
                StoneLayoutCanvas(layout = layout, diskThickness = diskThickness, trimMargin = trimMargin)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Statistics expansion
            Text("Material Statistics", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Parts Area", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${(layout.placedParts.sumOf { (it.width * it.height).toDouble() } / 1000000.0).format(2)} m²", fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Kerf Dust Loss", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${(layout.wasteDiskKerfArea / 1000000.0).format(3)} m²", fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Solid Waste Scrap", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${(layout.wasteSlabScrapArea / 1000000.0).format(2)} m²", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            // Operator sequence checkbox checklist
            Text(
                text = "Saw Operator Cutting Sequence",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            layout.instructions.forEach { step ->
                val checked = checkedSteps[step.stepNo] ?: false
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { checkedSteps[step.stepNo] = !checked }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { checkedSteps[step.stepNo] = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${step.stepNo}. ${step.description}",
                        fontSize = 13.sp,
                        color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun StoneLayoutCanvas(layout: SlabLayout, diskThickness: Float, trimMargin: Float) {
    val totalL = layout.originalLength
    val totalW = layout.originalWidth

    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(Color(0xFF080A0F))
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Fit whole slab dimensions to Canvas
        val scaleX = canvasWidth / totalL
        val scaleY = canvasHeight / totalW
        val scale = minOf(scaleX, scaleY)

        val ox = (canvasWidth - totalL * scale) / 2f
        val oy = (canvasHeight - totalW * scale) / 2f

        // 1. Draw Slab Base (High-tech Slate/Dark Marble look)
        drawRect(
            color = Color(0xFF121620),
            topLeft = Offset(ox, oy),
            size = Size(totalL * scale, totalW * scale)
        )
        // Outline of the slab
        drawRect(
            color = Color(0xFF1E293B),
            topLeft = Offset(ox, oy),
            size = Size(totalL * scale, totalW * scale),
            style = Stroke(width = 1.dp.toPx())
        )

        // Draw natural stone veins running diagonally (representing continuous grain)
        val veinColor1 = Color(0x1F22D3EE) // Glowing matrix cyan vein
        val veinColor2 = Color(0x18F59E0B) // Glowing golden vein
        val strokeVein = Stroke(width = 2.dp.toPx(), miter = 4f, cap = StrokeCap.Round)

        for (i in -2..6) {
            val offset = i * (totalL * scale / 4)
            val path = Path().apply {
                moveTo(ox + offset, oy)
                cubicTo(
                    ox + offset + 50f, oy + totalW * scale * 0.3f,
                    ox + offset - 50f, oy + totalW * scale * 0.7f,
                    ox + offset + totalW * scale * 0.5f, oy + totalW * scale
                )
            }
            drawPath(path, color = if (i % 2 == 0) veinColor1 else veinColor2, style = strokeVein)
        }

        // Draw Trim Margin (Dashed rectangle)
        if (trimMargin > 0f) {
            val tmScaled = trimMargin * scale
            drawRect(
                color = Color(0xFFEF4444).copy(alpha = 0.5f),
                topLeft = Offset(ox + tmScaled, oy + tmScaled),
                size = Size((totalL - 2 * trimMargin) * scale, (totalW - 2 * trimMargin) * scale),
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )
            )
        }

        // 2. Draw Placed Parts
        layout.placedParts.forEach { part ->
            val px = ox + part.x * scale
            val py = oy + part.y * scale
            val pw = part.width * scale
            val ph = part.height * scale

            // Draw filled block - Translucent Glowing glass
            drawRect(
                color = primaryColor.copy(alpha = 0.12f),
                topLeft = Offset(px, py),
                size = Size(pw, ph)
            )
            // Draw glowing cyan border
            drawRect(
                color = primaryColor,
                topLeft = Offset(px, py),
                size = Size(pw, ph),
                style = Stroke(width = 1.5f.dp.toPx())
            )

            // Draw sequence arrow indicators inside the parts to show pattern flow direction (Vein direction)
            val partVeinPath = Path().apply {
                moveTo(px + pw * 0.2f, py + ph * 0.8f)
                lineTo(px + pw * 0.8f, py + ph * 0.2f)
            }
            drawPath(
                partVeinPath,
                color = Color(0xFFF59E0B).copy(alpha = 0.7f),
                style = Stroke(
                    width = 1.5f.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )
            )

            // Draw text label with parsed high-contrast colors
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#22D3EE") // High-tech cyan
                textSize = 13.dp.toPx()
                isFakeBoldText = true
                textAlign = android.graphics.Paint.Align.CENTER
            }
            
            val detailsPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#E2E8F0") // Off-white/light slate
                textSize = 9.dp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
            }

            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    "Part ${part.part.id}",
                    px + pw / 2f,
                    py + ph / 2f,
                    textPaint
                )
                canvas.nativeCanvas.drawText(
                    "${part.part.width.toInt()}x${part.part.length.toInt()} mm",
                    px + pw / 2f,
                    py + ph / 2f + 11.dp.toPx(),
                    detailsPaint
                )
            }
        }

        // 3. Draw Cut lines (dash lines along actual kerfs)
        layout.cutLines.forEach { cut ->
            val cxStart = ox + cut.startX * scale
            val cyStart = oy + cut.startY * scale
            val cxEnd = ox + cut.endX * scale
            val cyEnd = oy + cut.endY * scale

            drawLine(
                color = if (cut.isPrimary) Color(0xFFF59E0B) else Color(0xFF22D3EE),
                start = Offset(cxStart, cyStart),
                end = Offset(cxEnd, cyEnd),
                strokeWidth = if (cut.isPrimary) 2.dp.toPx() else 1.2f.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
            )
        }

        // 4. Draw Vein Match Arrows linking connected parts (A ➔ B)
        layout.placedParts.forEach { part ->
            if (part.part.matchAdjacentTo.isNotEmpty()) {
                val parent = layout.placedParts.find { it.part.id == part.part.matchAdjacentTo }
                if (parent != null) {
                    val fromX = ox + parent.x * scale + (parent.width * scale) / 2f
                    val fromY = oy + parent.y * scale + (parent.height * scale) / 2f
                    val toX = ox + part.x * scale + (part.width * scale) / 2f
                    val toY = oy + part.y * scale + (part.height * scale) / 2f

                    // Draw a continuous vein arrow
                    drawVeinIndicatorArrow(fromX, fromY, toX, toY)
                }
            }
        }
    }
}

// Draw a custom golden indicator arrow to represent continuous grain flow from parent to child
fun androidx.compose.ui.graphics.drawscope.DrawScope.drawVeinIndicatorArrow(
    startX: Float, startY: Float, endX: Float, endY: Float
) {
    val arrowColor = Color(0xFFD97706)
    val strokeWidth = 3.dp.toPx()

    // Draw connecting line
    drawLine(
        color = arrowColor,
        start = Offset(startX, startY),
        end = Offset(endX, endY),
        strokeWidth = strokeWidth
    )

    // Draw arrowhead at end
    val angle = atan2(endY - startY, endX - startX)
    val arrowLength = 12.dp.toPx()
    val arrowAngle = Math.PI / 6 // 30 degrees

    val path = Path().apply {
        moveTo(endX, endY)
        lineTo(
            (endX - arrowLength * cos(angle - arrowAngle)).toFloat(),
            (endY - arrowLength * sin(angle - arrowAngle)).toFloat()
        )
        lineTo(
            (endX - arrowLength * cos(angle + arrowAngle)).toFloat(),
            (endY - arrowLength * sin(angle + arrowAngle)).toFloat()
        )
        close()
    }
    drawPath(path, color = arrowColor)
}

// Helpers
fun Float.format(digits: Int) = String.format("%.${digits}f", this)
fun Double.format(digits: Int) = String.format("%.${digits}f", this)

val Alignment.Companion.CenterAlignment: Alignment.Horizontal
    get() = Alignment.CenterHorizontally
