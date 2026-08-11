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
import java.text.SimpleDateFormat
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

fun String.toEnglishDigits(): String {
    var result = this
    val farsiDigits = arrayOf("۰", "۱", "۲", "۳", "۴", "۵", "۶", "۷", "۸", "۹")
    val arabicDigits = arrayOf("٠", "١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩")
    for (i in 0..9) {
        result = result.replace(farsiDigits[i], i.toString())
        result = result.replace(arabicDigits[i], i.toString())
    }
    result = result.replace("٫", ".").replace(",", ".")
    return result
}

fun String.toFloatOrNullWithPersian(): Float? {
    return this.toEnglishDigits().trim().toFloatOrNull()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoneCutApp(viewModel: StoneCutViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("تنظیمات و انبار", "قطعات و رگه‌ها", "نقشه چیدمان بهینه")

    val standardSlab by viewModel.standardSlab.collectAsState()
    val scrapInventory by viewModel.scrapInventory.collectAsState()
    val useScrap by viewModel.useScrap.collectAsState()
    val parts by viewModel.parts.collectAsState()
    val diskThickness by viewModel.diskThickness.collectAsState()
    val trimMargin by viewModel.trimMargin.collectAsState()
    val result by viewModel.optimizationResult.collectAsState()

    val savedProjects by viewModel.savedProjects.collectAsState()
    val currentProjectId by viewModel.currentProjectId.collectAsState()
    val currentProjectName by viewModel.currentProjectName.collectAsState()

    var showProjectsDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showSaveAsDialog by remember { mutableStateOf(false) }
    var projectNameInput by remember { mutableStateOf("") }

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
                                text = "موتور بهینه‌ساز v4.2 // آماده به کار",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.5.sp, fontWeight = FontWeight.Bold),
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
                            Text("دمو کابینت L", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                            Text("دمو دیوارپوش", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                            0 -> "انبار"
                            1 -> "پروژه"
                            else -> "چیدمان"
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

            // Active Project Info & Toolbar Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Active Project name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Done,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = currentProjectName?.let { "پروژه فعال: $it" } ?: "پروژه جدید (ذخیره نشده)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                // Right side: Project Actions (Projects, Save, Export PDF)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Projects Manager Button
                    FilledTonalButton(
                        onClick = { showProjectsDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("پروژه‌ها", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Save Button
                    if (currentProjectId != null) {
                        Button(
                            onClick = { viewModel.saveProject(currentProjectName ?: "") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ذخیره سریع", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                projectNameInput = ""
                                showSaveDialog = true
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ذخیره", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // PDF Button
                    var pdfLoading by remember { mutableStateOf(false) }
                    val context = LocalContext.current
                    Button(
                        onClick = {
                            pdfLoading = true
                            viewModel.generatePdfReport(
                                context = context,
                                onComplete = { file ->
                                    pdfLoading = false
                                    try {
                                        val authority = "${context.packageName}.fileprovider"
                                        val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "application/pdf")
                                            flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(android.content.Intent.createChooser(intent, "مشاهده گزارش PDF"))
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        try {
                                            val authority = "${context.packageName}.fileprovider"
                                            val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
                                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                type = "application/pdf"
                                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                            }
                                            context.startActivity(android.content.Intent.createChooser(shareIntent, "اشتراک‌گذاری گزارش PDF"))
                                        } catch (ex: Exception) {
                                            ex.printStackTrace()
                                            android.widget.Toast.makeText(context, "خطا در ساخت گزارش", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onError = { ex ->
                                    pdfLoading = false
                                    android.widget.Toast.makeText(context, "خطا: ${ex.message}", android.widget.Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp),
                        enabled = !pdfLoading
                    ) {
                        if (pdfLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), color = MaterialTheme.colorScheme.onTertiary, strokeWidth = 1.5.dp)
                        } else {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("خروجی PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // DXF Button
                    var dxfLoading by remember { mutableStateOf(false) }
                    Button(
                        onClick = {
                            dxfLoading = true
                            viewModel.generateDxfExport(
                                context = context,
                                onComplete = { file ->
                                    dxfLoading = false
                                    try {
                                        val authority = "${context.packageName}.fileprovider"
                                        val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
                                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "application/dxf"
                                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                            flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        }
                                        context.startActivity(android.content.Intent.createChooser(shareIntent, "اشتراک‌گذاری نقشه اتوکد DXF"))
                                    } catch (ex: Exception) {
                                        ex.printStackTrace()
                                        android.widget.Toast.makeText(context, "خطا در خروجی اتوکد", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onError = { ex ->
                                    dxfLoading = false
                                    android.widget.Toast.makeText(context, "خطا: ${ex.message}", android.widget.Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp),
                        enabled = !dxfLoading
                    ) {
                        if (dxfLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), color = MaterialTheme.colorScheme.onSecondary, strokeWidth = 1.5.dp)
                        } else {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("خروجی DXF (اتوکد)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

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

    if (showProjectsDialog) {
        AlertDialog(
            onDismissRequest = { showProjectsDialog = false },
            title = {
                Text(
                    text = "مدیریت پروژه‌های ذخیره شده",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    // New project action button
                    Button(
                        onClick = {
                            viewModel.resetToNewProject()
                            showProjectsDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ایجاد پروژه جدید (خالی)", fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "لیست پروژه‌ها (${savedProjects.size} مورد):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (savedProjects.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("هنوز پروژه‌ای ذخیره نشده است.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(savedProjects) { project ->
                                val date = SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.US).format(java.util.Date(project.timestamp))
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.loadProject(project)
                                            showProjectsDialog = false
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (currentProjectId == project.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.deleteProject(project) }
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف پروژه", tint = MaterialTheme.colorScheme.error)
                                        }
                                        
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            Text(
                                                text = project.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                textAlign = TextAlign.Right
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "$date // ابعاد اسلب: ${project.slabLength.toInt()}x${project.slabWidth.toInt()} میلی‌متر",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 10.sp,
                                                textAlign = TextAlign.Right
                                            )
                                            Text(
                                                text = "تعداد قطعات: ${project.parts.size} عدد",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Right
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProjectsDialog = false }) {
                    Text("بستن")
                }
            }
        )
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = {
                Text(
                    text = "ذخیره پروژه جدید",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "لطفاً نامی برای پروژه انتخاب کنید:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = projectNameInput,
                        onValueChange = { projectNameInput = it },
                        label = { Text("نام پروژه") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (projectNameInput.isNotEmpty()) {
                            viewModel.saveProject(projectNameInput)
                            showSaveDialog = false
                        }
                    }
                ) {
                    Text("ذخیره")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    if (showSaveAsDialog) {
        AlertDialog(
            onDismissRequest = { showSaveAsDialog = false },
            title = {
                Text(
                    text = "ذخیره به عنوان پروژه جدید",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "لطفاً نام جدیدی برای این پروژه وارد کنید:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = projectNameInput,
                        onValueChange = { projectNameInput = it },
                        label = { Text("نام پروژه جدید") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (projectNameInput.isNotEmpty()) {
                            viewModel.saveAsNewProject(projectNameInput)
                            showSaveAsDialog = false
                        }
                    }
                ) {
                    Text("ذخیره")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveAsDialog = false }) {
                    Text("انصراف")
                }
            }
        )
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
                    text = "بازدهی چیدمان",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
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
                    text = "اسلب مصرفی",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
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
                    text = "ضایعات مصرفی",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
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
                        text = "ابعاد اسلب استاندارد",
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
                                it.toFloatOrNullWithPersian()?.let { l ->
                                    viewModel.updateStandardSlab(l, editSlabW.toFloatOrNullWithPersian() ?: 1800f, editSlabT.toFloatOrNullWithPersian() ?: 20f)
                                }
                            },
                            label = { Text("طول اسلب L (میلی‌متر)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("slab_length_input")
                        )
                        OutlinedTextField(
                            value = editSlabW,
                            onValueChange = {
                                editSlabW = it
                                it.toFloatOrNullWithPersian()?.let { w ->
                                    viewModel.updateStandardSlab(editSlabL.toFloatOrNullWithPersian() ?: 3000f, w, editSlabT.toFloatOrNullWithPersian() ?: 20f)
                                }
                            },
                            label = { Text("عرض اسلب W (میلی‌متر)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("slab_width_input")
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editSlabT,
                        onValueChange = {
                            editSlabT = it
                            it.toFloatOrNullWithPersian()?.let { t ->
                                viewModel.updateStandardSlab(editSlabL.toFloatOrNullWithPersian() ?: 3000f, editSlabW.toFloatOrNullWithPersian() ?: 1800f, t)
                            }
                        },
                        label = { Text("ضخامت اسلب T (میلی‌متر)") },
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
                        text = "تنظیمات دستگاه برش و اره",
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
                                it.toFloatOrNullWithPersian()?.let { k ->
                                    viewModel.updateMachineParameters(k, trimMargin)
                                }
                            },
                            label = { Text("ضخامت تیغه / کرف (میلی‌متر)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).testTag("blade_kerf_input")
                        )
                        OutlinedTextField(
                            value = editTrim,
                            onValueChange = {
                                editTrim = it
                                it.toFloatOrNullWithPersian()?.let { t ->
                                    viewModel.updateMachineParameters(diskThickness, t)
                                }
                            },
                            label = { Text("حاشیه دور سنگ / هرس (میلی‌متر)") },
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
                            text = "انبار ضایعات و تکه سنگ‌های باقیمانده",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("استفاده از ضایعات", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Checkbox(
                                checked = useScrap,
                                onCheckedChange = { viewModel.setUseScrap(it) },
                                modifier = Modifier.testTag("use_scrap_checkbox")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "پیش از برش اسلب‌های جدید، استفاده از این قطعات باقیمانده کارگاه را در اولویت قرار دهید تا دورریز سنگ حداقل شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Add scrap form
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newScrapL,
                            onValueChange = { newScrapL = it },
                            label = { Text("طول L (میلی‌متر)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newScrapW,
                            onValueChange = { newScrapW = it },
                            label = { Text("عرض W (میلی‌متر)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                val l = newScrapL.toFloatOrNullWithPersian()
                                val w = newScrapW.toFloatOrNullWithPersian()
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
                            Icon(Icons.Default.Add, contentDescription = "افزودن ضایعات")
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
                            Text("هیچ قطعه ضایعاتی ثبت نشده است", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
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
                                            text = "ضایعات #${scrap.id}: ${scrap.length.toInt()} × ${scrap.width.toInt()} میلی‌متر",
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
                                            contentDescription = "حذف",
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

    var miterLeft by remember { mutableStateOf(false) }
    var miterTop by remember { mutableStateOf(false) }
    var miterRight by remember { mutableStateOf(false) }
    var miterBottom by remember { mutableStateOf(false) }

    var isBookmatch by remember { mutableStateOf(false) }
    var quantity by remember { mutableStateOf(1) }

    var editingPart by remember { mutableStateOf<Part?>(null) }
    var expandedDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(editingPart) {
        if (editingPart != null) {
            name = editingPart!!.name
            lStr = editingPart!!.length.toInt().toString()
            wStr = editingPart!!.width.toInt().toString()
            allowRotation = editingPart!!.allowRotation
            matchAdjacentTo = editingPart!!.matchAdjacentTo
            miterLeft = editingPart!!.miterLeft
            miterTop = editingPart!!.miterTop
            miterRight = editingPart!!.miterRight
            miterBottom = editingPart!!.miterBottom
            isBookmatch = editingPart!!.isBookmatch
            quantity = editingPart!!.quantity
        } else {
            name = ""
            lStr = ""
            wStr = ""
            allowRotation = false
            matchAdjacentTo = ""
            miterLeft = false
            miterTop = false
            miterRight = false
            miterBottom = false
            isBookmatch = false
            quantity = 1
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Add/Edit Part Input Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (editingPart == null) "افزودن قطعه برای برش" else "ویرایش قطعه ${editingPart!!.id}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("نام یا محل استفاده قطعه") },
                        modifier = Modifier.fillMaxWidth().testTag("part_name_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = lStr,
                            onValueChange = { lStr = it },
                            label = { Text("طول قطعه (Y، میلی‌متر)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("part_length_input")
                        )
                        OutlinedTextField(
                            value = wStr,
                            onValueChange = { wStr = it },
                            label = { Text("عرض قطعه (X، میلی‌متر)") },
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
                            Text("اجازه چرخش ۹۰ درجه", fontSize = 14.sp)
                        }

                        // Match Adjacent dropdown
                        Box {
                            Button(
                                onClick = { expandedDropdown = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                            ) {
                                Text(if (matchAdjacentTo.isEmpty()) "اتصال رگه سنگ" else "انطباق رگه با: $matchAdjacentTo")
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "رگه‌ها")
                            }
                            DropdownMenu(
                                expanded = expandedDropdown,
                                onDismissRequest = { expandedDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("بدون پیوستگی رگه (مستقل)") },
                                    onClick = {
                                        matchAdjacentTo = ""
                                        expandedDropdown = false
                                    }
                                )
                                parts.forEach { p ->
                                    if (editingPart == null || p.id != editingPart!!.id) {
                                        DropdownMenuItem(
                                            text = { Text("پیوستگی رگه با قطعه ${p.id} (${p.name})") },
                                            onClick = {
                                                matchAdjacentTo = p.id
                                                expandedDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Miter Edges Selection using modern FilterChips
                    Text("فارسی‌بر لبه‌ها:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = miterLeft,
                            onClick = { miterLeft = !miterLeft },
                            label = { Text("چپ", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = miterTop,
                            onClick = { miterTop = !miterTop },
                            label = { Text("بالا", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = miterRight,
                            onClick = { miterRight = !miterRight },
                            label = { Text("راست", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = miterBottom,
                            onClick = { miterBottom = !miterBottom },
                            label = { Text("پایین", fontSize = 11.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("تعداد قطعه مشابه:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { if (quantity > 1) quantity-- },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                                    .size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Remove,
                                    contentDescription = "کاهش",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "$quantity",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.widthIn(min = 24.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            IconButton(
                                onClick = { quantity++ },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                                    .size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "افزایش",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (editingPart != null) {
                            OutlinedButton(
                                onClick = { editingPart = null },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("انصراف")
                            }
                        }
                        Button(
                            onClick = {
                                val l = lStr.toFloatOrNullWithPersian()
                                val w = wStr.toFloatOrNullWithPersian()
                                if (name.isNotEmpty() && l != null && w != null) {
                                    if (editingPart == null) {
                                        viewModel.addPart(
                                            name = name,
                                            length = l,
                                            width = w,
                                            allowRotation = allowRotation,
                                            matchAdjacentTo = matchAdjacentTo,
                                            miterLeft = miterLeft,
                                            miterTop = miterTop,
                                            miterRight = miterRight,
                                            miterBottom = miterBottom,
                                            isBookmatch = isBookmatch,
                                            bookmatchLeft = false,
                                            bookmatchTop = false,
                                            bookmatchRight = false,
                                            bookmatchBottom = false,
                                            quantity = quantity
                                        )
                                    } else {
                                        viewModel.updatePart(
                                            editingPart!!.copy(
                                                name = name,
                                                length = l,
                                                width = w,
                                                allowRotation = allowRotation,
                                                matchAdjacentTo = matchAdjacentTo,
                                                miterLeft = miterLeft,
                                                miterTop = miterTop,
                                                miterRight = miterRight,
                                                miterBottom = miterBottom,
                                                isBookmatch = isBookmatch,
                                                bookmatchLeft = false,
                                                bookmatchTop = false,
                                                bookmatchRight = false,
                                                bookmatchBottom = false,
                                                quantity = quantity
                                            )
                                        )
                                        editingPart = null
                                    }
                                    if (editingPart == null) {
                                        name = ""
                                        lStr = ""
                                        wStr = ""
                                        allowRotation = false
                                        matchAdjacentTo = ""
                                        miterLeft = false
                                        miterTop = false
                                        miterRight = false
                                        miterBottom = false
                                        isBookmatch = false
                                        quantity = 1
                                    }
                                }
                            },
                            modifier = Modifier.weight(1.5f).testTag("add_part_submit_button")
                        ) {
                            Icon(if (editingPart == null) Icons.Default.Add else Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (editingPart == null) "افزودن به لیست برش" else "ثبت تغییرات")
                        }
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
                    text = "لیست قطعات مورد نیاز",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${parts.size} مورد",
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
                    Text("هنوز قطعه‌ای برای برش ثبت نشده است.\nبرای شروع روی دموهای بالا کلیک کنید تا لیست پر شود!", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(parts) { part ->
                PartRowItem(
                    part = part,
                    onDelete = { viewModel.removePart(part.id) },
                    onEdit = { editingPart = part }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun PartRowItem(part: Part, onDelete: () -> Unit, onEdit: () -> Unit) {
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = part.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(" ${part.quantity} عدد ", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        text = "${part.width.toInt()} × ${part.length.toInt()} میلی‌متر (مساحت: ${(part.width * part.length / 1000000).format(2)} مترمربع)",
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
                                label = { Text("قابل چرخش", fontSize = 10.sp) },
                                modifier = Modifier.scale(0.85f)
                            )
                        } else {
                            SuggestionChip(
                                onClick = {},
                                label = { Text("جهت رگه ثابت", fontSize = 10.sp) },
                                modifier = Modifier.scale(0.85f),
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                )
                            )
                        }

                        if (part.matchAdjacentTo.isNotEmpty()) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text("اتصال رگه ➔ قطعه ${part.matchAdjacentTo}", fontSize = 10.sp) },
                                modifier = Modifier.scale(0.85f),
                                icon = {
                                    Icon(
                                        Icons.Default.Link,
                                        contentDescription = "جفت‌شده",
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                        }



                        val miters = mutableListOf<String>()
                        if (part.miterLeft) miters.add("چپ")
                        if (part.miterTop) miters.add("بالا")
                        if (part.miterRight) miters.add("راست")
                        if (part.miterBottom) miters.add("پایین")
                        if (miters.isNotEmpty()) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text("📐 فارسی‌بر: ${miters.joinToString("، ")}", fontSize = 10.sp) },
                                modifier = Modifier.scale(0.85f),
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "ویرایش قطعه",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "حذف قطعه",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun VisualMapTab(viewModel: StoneCutViewModel, result: OptimizationResult?, diskThickness: Float, trimMargin: Float) {
    val customSlabLayouts by viewModel.customSlabLayouts.collectAsState()
    val layoutsToRender = customSlabLayouts ?: result?.slabLayouts ?: emptyList()

    if (layoutsToRender.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text("نقشه‌ای وجود ندارد. لطفاً ابتدا قطعاتی را به لیست برش اضافه کنید.", textAlign = TextAlign.Center)
            }
        }
        return
    }

    val allLayoutIds = layoutsToRender.map { it.containerId }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "نقشه‌های تعاملی چیدمان و برش سنگ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "هر اسلب در زیر با قطعات قرارگرفته، خطوط دقیق برش و جهت رگه‌های طبیعی سنگ ترسیم شده است. شما می‌توانید جهت و موقعیت هر قطعه را به صورت دستی تغییر دهید.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (customSlabLayouts != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text("✍️", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "تغییرات دستی فعال است. محاسبات بازدهی مجدداً انجام شد.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(
                            onClick = { viewModel.resetToAuto() }
                        ) {
                            Text("حذف تغییرات دستی", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        items(layoutsToRender) { layout ->
            SlabLayoutCard(
                layout = layout,
                diskThickness = diskThickness,
                trimMargin = trimMargin,
                viewModel = viewModel,
                allLayoutIds = allLayoutIds
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun SlabLayoutCard(
    layout: SlabLayout,
    diskThickness: Float,
    trimMargin: Float,
    viewModel: StoneCutViewModel,
    allLayoutIds: List<String>
) {
    val checkedSteps = remember { mutableStateMapOf<Int, Boolean>() }

    val containerLabel = layout.containerId
        .replace("Slab", "اسلب")
        .replace("Scrap", "ضایعات")

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
                        text = containerLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "${layout.originalLength.toInt()} × ${layout.originalWidth.toInt()} میلی‌متر " +
                                if (layout.isScrap) "(تکه ضایعاتی بازیافتی)" else "(اسلب استاندارد)",
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
                        text = String.format("بازدهی %.1f%%", layout.efficiency),
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
            Text("آمار مصرف مواد و مصالح", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("مساحت قطعات مفید", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${(layout.placedParts.sumOf { (it.width * it.height).toDouble() } / 1000000.0).format(2)} مترمربع", fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("هدررفت پودری تیغه", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${(layout.wasteDiskKerfArea / 1000000.0).format(3)} مترمربع", fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("ضایعات و دورریز جامد", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${(layout.wasteSlabScrapArea / 1000000.0).format(2)} مترمربع", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))

            // Operator sequence checkbox checklist
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ترتیب گام‌های برش مخصوص اپراتور دستگاه اره",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                val context = LocalContext.current
                IconButton(
                    onClick = {
                        val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clipData = android.content.ClipData.newPlainText(
                            "Cutting Instructions",
                            layout.instructions.joinToString("\n") { "${it.stepNo}. ${it.description}" }
                        )
                        clipboardManager.setPrimaryClip(clipData)
                        android.widget.Toast.makeText(context, "دستورالعمل‌های برش کپی شدند", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "کپی دستورالعمل‌ها",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
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

            // COLLAPSIBLE MANUAL ADJUSTMENT PANEL
            var showManualEditor by remember { mutableStateOf(false) }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showManualEditor = !showManualEditor }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ویرایش دستی موقعیت و جهت قطعات",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Icon(
                    imageVector = if (showManualEditor) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }

            if (showManualEditor) {
                Spacer(modifier = Modifier.height(8.dp))
                if (layout.placedParts.isEmpty()) {
                    Text(
                        text = "هیچ قطعه‌ای در این اسلب وجود ندارد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                layout.placedParts.forEach { placed ->
                    val cleanId = placed.part.id.substringBefore("_")
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "قطعه $cleanId (${placed.part.name})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "ابعاد: ${placed.width.toInt()} × ${placed.height.toInt()} م‌م | موقعیت: X=${placed.x.toInt()}، Y=${placed.y.toInt()}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Rotate Button
                                IconButton(
                                    onClick = { viewModel.rotatePlacedPart(layout.containerId, placed.part.id) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "چرخش",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Nudge Coordinates buttons
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("جابجایی:", fontSize = 11.sp, fontWeight = FontWeight.Bold)

                                    // Move Left
                                    IconButton(
                                        onClick = { viewModel.movePlacedPart(layout.containerId, placed.part.id, -50f, 0f) },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                    ) {
                                        Text("◀", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    // Move Right
                                    IconButton(
                                        onClick = { viewModel.movePlacedPart(layout.containerId, placed.part.id, 50f, 0f) },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                    ) {
                                        Text("▶", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    // Move Up
                                    IconButton(
                                        onClick = { viewModel.movePlacedPart(layout.containerId, placed.part.id, 0f, -50f) },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                    ) {
                                        Text("▲", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    // Move Down
                                    IconButton(
                                        onClick = { viewModel.movePlacedPart(layout.containerId, placed.part.id, 0f, 50f) },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                    ) {
                                        Text("▼", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Slab Transfer Dropdown
                                if (allLayoutIds.size > 1) {
                                    var showSlabDropdown by remember { mutableStateOf(false) }
                                    Box {
                                        Button(
                                            onClick = { showSlabDropdown = true },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                            ),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text("انتقال اسلب", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        DropdownMenu(
                                            expanded = showSlabDropdown,
                                            onDismissRequest = { showSlabDropdown = false }
                                        ) {
                                            allLayoutIds.forEach { destId ->
                                                if (destId != layout.containerId) {
                                                    val cleanDestId = destId
                                                        .replace("Slab", "اسلب")
                                                        .replace("Scrap", "ضایعات")
                                                    DropdownMenuItem(
                                                        text = { Text("به $cleanDestId", fontSize = 11.sp) },
                                                        onClick = {
                                                            viewModel.changePartSlab(placed.part.id, layout.containerId, destId)
                                                            showSlabDropdown = false
                                                        }
                                                    )
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
        }
    }
}

@Composable
fun StoneLayoutCanvas(layout: SlabLayout, diskThickness: Float, trimMargin: Float) {
    val totalL = layout.originalLength
    val totalW = layout.originalWidth

    val isDark = isSystemInDarkTheme()
    val canvasBg = if (isDark) Color(0xFF161719) else Color(0xFFF7F5F0)
    val slabColor = if (isDark) Color(0xFF222429) else Color(0xFFEBE6DC)
    val slabOutlineColor = if (isDark) Color(0xFF383C48) else Color(0xFFCCC4B4)

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val outlineColor = MaterialTheme.colorScheme.outline

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(canvasBg)
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
            color = slabColor,
            topLeft = Offset(ox, oy),
            size = Size(totalL * scale, totalW * scale)
        )
        // Outline of the slab
        drawRect(
            color = slabOutlineColor,
            topLeft = Offset(ox, oy),
            size = Size(totalL * scale, totalW * scale),
            style = Stroke(width = 1.dp.toPx())
        )

        // Draw natural stone veins running diagonally (representing continuous grain)
        val veinColor1 = primaryColor.copy(alpha = 0.15f)
        val veinColor2 = secondaryColor.copy(alpha = 0.15f)
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
                color = Color(0xFFEF4444).copy(alpha = 0.4f),
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

            // Draw filled block - Translucent matching theme
            drawRect(
                color = primaryColor.copy(alpha = 0.15f),
                topLeft = Offset(px, py),
                size = Size(pw, ph)
            )
            // Draw matching border
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
                color = secondaryColor.copy(alpha = 0.6f),
                style = Stroke(
                    width = 1.5f.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )
            )

            // Draw miter hatching if enabled
            val drawMiterLeft = if (part.isRotated) part.part.miterBottom else part.part.miterLeft
            val drawMiterTop = if (part.isRotated) part.part.miterLeft else part.part.miterTop
            val drawMiterRight = if (part.isRotated) part.part.miterTop else part.part.miterRight
            val drawMiterBottom = if (part.isRotated) part.part.miterRight else part.part.miterBottom

            val miterHatchColor = Color.Black // Black hatching for miter as requested

            if (drawMiterLeft) {
                val hatchSize = 3.dp.toPx()
                val step = 6.dp.toPx()
                var currY = py
                while (currY < py + ph) {
                    val nextY = minOf(currY + hatchSize, py + ph)
                    val nextX = px + (nextY - currY)
                    drawLine(
                        color = miterHatchColor,
                        start = Offset(px, currY),
                        end = Offset(nextX, nextY),
                        strokeWidth = 0.8.dp.toPx()
                    )
                    currY += step
                }
                drawLine(
                    color = miterHatchColor,
                    start = Offset(px, py),
                    end = Offset(px, py + ph),
                    strokeWidth = 1.2.dp.toPx()
                )
            }

            if (drawMiterRight) {
                val hatchSize = 3.dp.toPx()
                val step = 6.dp.toPx()
                var currY = py
                while (currY < py + ph) {
                    val nextY = minOf(currY + hatchSize, py + ph)
                    val nextX = px + pw - (nextY - currY)
                    drawLine(
                        color = miterHatchColor,
                        start = Offset(px + pw, currY),
                        end = Offset(nextX, nextY),
                        strokeWidth = 0.8.dp.toPx()
                    )
                    currY += step
                }
                drawLine(
                    color = miterHatchColor,
                    start = Offset(px + pw, py),
                    end = Offset(px + pw, py + ph),
                    strokeWidth = 1.2.dp.toPx()
                )
            }

            if (drawMiterTop) {
                val hatchSize = 3.dp.toPx()
                val step = 6.dp.toPx()
                var currX = px
                while (currX < px + pw) {
                    val nextX = minOf(currX + hatchSize, px + pw)
                    val nextY = py + (nextX - currX)
                    drawLine(
                        color = miterHatchColor,
                        start = Offset(currX, py),
                        end = Offset(nextX, nextY),
                        strokeWidth = 0.8.dp.toPx()
                    )
                    currX += step
                }
                drawLine(
                    color = miterHatchColor,
                    start = Offset(px, py),
                    end = Offset(px + pw, py),
                    strokeWidth = 1.2.dp.toPx()
                )
            }

            if (drawMiterBottom) {
                val hatchSize = 3.dp.toPx()
                val step = 6.dp.toPx()
                var currX = px
                while (currX < px + pw) {
                    val nextX = minOf(currX + hatchSize, px + pw)
                    val nextY = py + ph - (nextX - currX)
                    drawLine(
                        color = miterHatchColor,
                        start = Offset(currX, py + ph),
                        end = Offset(nextX, nextY),
                        strokeWidth = 0.8.dp.toPx()
                    )
                    currX += step
                }
                drawLine(
                    color = miterHatchColor,
                    start = Offset(px, py + ph),
                    end = Offset(px + pw, py + ph),
                    strokeWidth = 1.2.dp.toPx()
                )
            }



            // Draw text label with high contrast
            val textColorHex = if (isDark) "#80CBC4" else "#0F5A60"
            val detailColorHex = if (isDark) "#E2E8F0" else "#1C1B19"

            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor(textColorHex)
                textSize = 12.dp.toPx()
                isFakeBoldText = true
                textAlign = android.graphics.Paint.Align.CENTER
            }
            
            val detailsPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor(detailColorHex)
                textSize = 9.dp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
            }

            drawIntoCanvas { canvas ->
                val partLabel = if (part.part.id.contains("_")) {
                    "قطعه ${part.part.id.substringBefore("_")} (${part.part.id.substringAfter("_")})"
                } else {
                    "قطعه ${part.part.id}"
                }
                canvas.nativeCanvas.drawText(
                    partLabel,
                    px + pw / 2f,
                    py + ph / 2f,
                    textPaint
                )
                canvas.nativeCanvas.drawText(
                    "${part.part.width.toInt()} × ${part.part.length.toInt()} م‌م",
                    px + pw / 2f,
                    py + ph / 2f + 11.dp.toPx(),
                    detailsPaint
                )
            }

            // Draw badges inside part
            val miterLabel = if (part.part.miterLeft || part.part.miterTop || part.part.miterRight || part.part.miterBottom) "📐" else ""
            val bmLabel = if (part.part.matchAdjacentTo.isNotEmpty()) "🔗" else ""
            val badgeStr = listOfNotNull(miterLabel.takeIf { it.isNotEmpty() }, bmLabel.takeIf { it.isNotEmpty() }).joinToString(" ")

            if (badgeStr.isNotEmpty()) {
                val badgePaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor(if (isDark) "#E6C280" else "#85581A")
                    textSize = 10.dp.toPx()
                    isFakeBoldText = true
                }
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        badgeStr,
                        px + 6.dp.toPx() + 8.dp.toPx(),
                        py + 15.dp.toPx(),
                        badgePaint
                    )
                }
            }
        }

        // 3. Draw Cut lines (dash lines along actual kerfs)
        layout.cutLines.forEach { cut ->
            val cxStart = ox + cut.startX * scale
            val cyStart = oy + cut.startY * scale
            val cxEnd = ox + cut.endX * scale
            val cyEnd = oy + cut.endY * scale

            drawLine(
                color = if (cut.isPrimary) secondaryColor else primaryColor,
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

        // 5. Draw Offcuts (useful remaining pieces labeled as "پرت")
        layout.offcuts.forEach { offcut ->
            val ox_off = ox + offcut.x * scale
            val oy_off = oy + offcut.y * scale
            val ow_off = offcut.width * scale
            val oh_off = offcut.height * scale

            // Draw translucent dashed amber/gold background
            drawRect(
                color = secondaryColor.copy(alpha = 0.05f),
                topLeft = Offset(ox_off, oy_off),
                size = Size(ow_off, oh_off)
            )
            // Draw dashed amber outline
            drawRect(
                color = secondaryColor.copy(alpha = 0.5f),
                topLeft = Offset(ox_off, oy_off),
                size = Size(ow_off, oh_off),
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )
            )

            // Draw label "پرت مفید" with dimensions inside
            val offcutLabelPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor(if (isDark) "#E6C280" else "#85581A")
                textSize = 9.dp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
            }
            val offcutDimPaint = android.graphics.Paint().apply {
                val detailColorHex = if (isDark) "#E2E8F0" else "#1C1B19"
                color = android.graphics.Color.parseColor(detailColorHex)
                textSize = 7.dp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
            }

            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    "پرت مفید",
                    ox_off + ow_off / 2f,
                    oy_off + oh_off / 2f,
                    offcutLabelPaint
                )
                canvas.nativeCanvas.drawText(
                    "${offcut.width.toInt()} × ${offcut.height.toInt()} م‌م",
                    ox_off + ow_off / 2f,
                    oy_off + oh_off / 2f + 9.dp.toPx(),
                    offcutDimPaint
                )
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
