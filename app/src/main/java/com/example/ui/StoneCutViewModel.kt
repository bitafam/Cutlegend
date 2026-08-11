package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.db.AppDatabase
import com.example.db.ProjectEntity
import com.example.model.*
import com.example.solver.StoneCutSolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StoneCutViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java, "stone_cut_database"
    ).fallbackToDestructiveMigration(true).build()
    
    private val projectDao = db.projectDao()

    private val _savedProjects = MutableStateFlow<List<ProjectEntity>>(emptyList())
    val savedProjects: StateFlow<List<ProjectEntity>> = _savedProjects.asStateFlow()

    private val _currentProjectId = MutableStateFlow<Int?>(null)
    val currentProjectId: StateFlow<Int?> = _currentProjectId.asStateFlow()

    private val _currentProjectName = MutableStateFlow<String?>(null)
    val currentProjectName: StateFlow<String?> = _currentProjectName.asStateFlow()

    private val _standardSlab = MutableStateFlow(StandardSlab())
    val standardSlab: StateFlow<StandardSlab> = _standardSlab.asStateFlow()

    private val _scrapInventory = MutableStateFlow<List<ScrapPiece>>(
        listOf(
            ScrapPiece(id = 1, length = 1200f, width = 600f, isEnabled = true),
            ScrapPiece(id = 2, length = 800f, width = 450f, isEnabled = true)
        )
    )
    val scrapInventory: StateFlow<List<ScrapPiece>> = _scrapInventory.asStateFlow()

    private val _useScrap = MutableStateFlow(true)
    val useScrap: StateFlow<Boolean> = _useScrap.asStateFlow()

    private val _parts = MutableStateFlow<List<Part>>(
        listOf(
            Part(id = "A", name = "صفحه کانتر اصلی", length = 600f, width = 1800f, allowRotation = false),
            Part(id = "B", name = "دیوارپوش هماهنگ پشت‌کار", length = 300f, width = 1800f, allowRotation = false, matchAdjacentTo = "A"),
            Part(id = "C", name = "پیشانی کانتر کناری", length = 150f, width = 600f, allowRotation = true),
            Part(id = "D", name = "پوشش انتهایی کانتر جزیره", length = 600f, width = 1200f, allowRotation = false),
            Part(id = "E", name = "تخته کار سنگی توکار", length = 300f, width = 400f, allowRotation = true)
        )
    )
    val parts: StateFlow<List<Part>> = _parts.asStateFlow()

    private val _diskThickness = MutableStateFlow(5f)
    val diskThickness: StateFlow<Float> = _diskThickness.asStateFlow()

    private val _trimMargin = MutableStateFlow(20f)
    val trimMargin: StateFlow<Float> = _trimMargin.asStateFlow()

    private val _optimizationResult = MutableStateFlow<OptimizationResult?>(null)
    val optimizationResult: StateFlow<OptimizationResult?> = _optimizationResult.asStateFlow()

    init {
        // Run initial optimization on start
        triggerOptimization()
        loadProjects()
    }

    fun triggerOptimization() {
        _optimizationResult.value = StoneCutSolver.optimize(
            standardSlab = _standardSlab.value,
            scrapInventory = _scrapInventory.value,
            useScrap = _useScrap.value,
            parts = _parts.value,
            diskThickness = _diskThickness.value,
            trimMargin = _trimMargin.value
        )
    }

    fun updateStandardSlab(length: Float, width: Float, thickness: Float) {
        _standardSlab.value = StandardSlab(length, width, thickness)
        triggerOptimization()
    }

    fun setUseScrap(enabled: Boolean) {
        _useScrap.value = enabled
        triggerOptimization()
    }

    fun addScrap(length: Float, width: Float) {
        val nextId = (_scrapInventory.value.maxOfOrNull { it.id } ?: 0) + 1
        _scrapInventory.value = _scrapInventory.value + ScrapPiece(nextId, length, width, true)
        triggerOptimization()
    }

    fun toggleScrapEnabled(id: Int) {
        _scrapInventory.value = _scrapInventory.value.map {
            if (it.id == id) it.copy(isEnabled = !it.isEnabled) else it
        }
        triggerOptimization()
    }

    fun removeScrap(id: Int) {
        _scrapInventory.value = _scrapInventory.value.filter { it.id != id }
        triggerOptimization()
    }

    fun addPart(
        name: String,
        length: Float,
        width: Float,
        allowRotation: Boolean,
        matchAdjacentTo: String,
        miterLeft: Boolean = false,
        miterTop: Boolean = false,
        miterRight: Boolean = false,
        miterBottom: Boolean = false,
        isBookmatch: Boolean = false
    ) {
        val nextId = getNextLatinId()
        val newPart = Part(
            id = nextId,
            name = name,
            length = length,
            width = width,
            allowRotation = allowRotation,
            matchAdjacentTo = matchAdjacentTo,
            miterLeft = miterLeft,
            miterTop = miterTop,
            miterRight = miterRight,
            miterBottom = miterBottom,
            isBookmatch = isBookmatch
        )
        _parts.value = _parts.value + newPart
        triggerOptimization()
    }

    fun updatePart(updated: Part) {
        _parts.value = _parts.value.map {
            if (it.id == updated.id) updated else it
        }
        triggerOptimization()
    }

    fun removePart(id: String) {
        // Also remove any adjacent linkages pointing to this part to avoid invalid links
        _parts.value = _parts.value.filter { it.id != id }.map {
            if (it.matchAdjacentTo == id) it.copy(matchAdjacentTo = "") else it
        }
        triggerOptimization()
    }

    fun updateMachineParameters(kerf: Float, trim: Float) {
        _diskThickness.value = kerf
        _trimMargin.value = trim
        triggerOptimization()
    }

    fun loadLShapedCountertopTemplate() {
        _parts.value = listOf(
            Part(id = "A", name = "اسلب اصلی الف", length = 600f, width = 1800f, allowRotation = false, miterLeft = true, miterTop = true),
            Part(id = "B", name = "اسلب پیشانی ال‌شکل ب", length = 600f, width = 1200f, allowRotation = false, matchAdjacentTo = "A", miterBottom = true),
            Part(id = "C", name = "قرنیز دیواری بلند ج", length = 300f, width = 1800f, allowRotation = false, matchAdjacentTo = "B", miterRight = true),
            Part(id = "D", name = "نوار برش تزیینی د", length = 150f, width = 900f, allowRotation = true),
            Part(id = "E", name = "نگین سنگی تزیینی ه", length = 200f, width = 200f, allowRotation = true)
        )
        triggerOptimization()
    }

    fun loadWallCladdingTemplate() {
        _parts.value = listOf(
            Part(id = "A", name = "پنل دیواری چپ", length = 1200f, width = 1400f, allowRotation = false, isBookmatch = true),
            Part(id = "B", name = "پنل دیواری راست بوک‌مچ", length = 1200f, width = 1400f, allowRotation = false, matchAdjacentTo = "A", isBookmatch = true),
            Part(id = "C", name = "باند تزیینی بالای دیوار", length = 400f, width = 2800f, allowRotation = false, miterTop = true),
            Part(id = "D", name = "نوار حاشیه چپ کوچک", length = 150f, width = 600f, allowRotation = true),
            Part(id = "E", name = "نوار حاشیه راست کوچک", length = 150f, width = 600f, allowRotation = true)
        )
        triggerOptimization()
    }

    fun loadClearTemplate() {
        _parts.value = emptyList()
        _optimizationResult.value = null
    }

    private fun getNextLatinId(): String {
        val currentIds = _parts.value.map { it.id }.toSet()
        var index = 0
        while (true) {
            val id = getLatinCode(index)
            if (!currentIds.contains(id)) return id
            index++
        }
    }

    private fun getLatinCode(index: Int): String {
        val sb = java.lang.StringBuilder()
        var temp = index
        while (temp >= 0) {
            sb.insert(0, ('A' + (temp % 26)))
            temp = (temp / 26) - 1
        }
        return sb.toString()
    }

    // --- Room Database Operations ---

    fun loadProjects() {
        viewModelScope.launch {
            _savedProjects.value = projectDao.getAllProjects()
        }
    }

    fun saveProject(name: String) {
        viewModelScope.launch {
            val project = ProjectEntity(
                id = _currentProjectId.value ?: 0,
                name = name,
                timestamp = System.currentTimeMillis(),
                slabLength = _standardSlab.value.length,
                slabWidth = _standardSlab.value.width,
                slabThickness = _standardSlab.value.thickness,
                diskThickness = _diskThickness.value,
                trimMargin = _trimMargin.value,
                useScrap = _useScrap.value,
                parts = _parts.value,
                scrap = _scrapInventory.value
            )
            if (project.id == 0) {
                val newId = projectDao.insertProject(project).toInt()
                _currentProjectId.value = newId
            } else {
                projectDao.updateProject(project)
            }
            _currentProjectName.value = name
            loadProjects()
        }
    }

    fun saveAsNewProject(name: String) {
        viewModelScope.launch {
            val project = ProjectEntity(
                id = 0,
                name = name,
                timestamp = System.currentTimeMillis(),
                slabLength = _standardSlab.value.length,
                slabWidth = _standardSlab.value.width,
                slabThickness = _standardSlab.value.thickness,
                diskThickness = _diskThickness.value,
                trimMargin = _trimMargin.value,
                useScrap = _useScrap.value,
                parts = _parts.value,
                scrap = _scrapInventory.value
            )
            val newId = projectDao.insertProject(project).toInt()
            _currentProjectId.value = newId
            _currentProjectName.value = name
            loadProjects()
        }
    }

    fun loadProject(project: ProjectEntity) {
        _currentProjectId.value = project.id
        _currentProjectName.value = project.name
        _standardSlab.value = StandardSlab(project.slabLength, project.slabWidth, project.slabThickness)
        _diskThickness.value = project.diskThickness
        _trimMargin.value = project.trimMargin
        _useScrap.value = project.useScrap
        _parts.value = project.parts
        _scrapInventory.value = project.scrap
        triggerOptimization()
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            projectDao.deleteProject(project)
            if (_currentProjectId.value == project.id) {
                resetToNewProject()
            }
            loadProjects()
        }
    }

    fun resetToNewProject() {
        _currentProjectId.value = null
        _currentProjectName.value = null
        _standardSlab.value = StandardSlab()
        _diskThickness.value = 5f
        _trimMargin.value = 20f
        _useScrap.value = true
        _parts.value = emptyList()
        _scrapInventory.value = emptyList()
        _optimizationResult.value = null
    }

    // --- PDF Report Generation ---

    fun generatePdfReport(context: Context, onComplete: (File) -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val pdfDocument = PdfDocument()
                val pageWidth = 595
                val pageHeight = 842
                var pageNumber = 1

                // Page 1: Technical Specifications & Summary
                var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create()
                var page = pdfDocument.startPage(pageInfo)
                var canvas = page.canvas

                val paint = Paint()
                val textPaint = TextPaint().apply {
                    color = Color.BLACK
                    textSize = 12f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                }

                // Header Banner
                paint.color = Color.parseColor("#1B5E20") // Slate stone / Emerald green theme
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), 95f, paint)

                textPaint.color = Color.WHITE
                textPaint.textSize = 20f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                drawRtlText(canvas, "گزارش چیدمان و الگوی برش سنگ (کات لند)", pageWidth - 30f, 30f, textPaint)

                textPaint.textSize = 11f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US)
                val dateStr = sdf.format(Date())
                drawRtlText(canvas, "صادر شده توسط اپلیکیشن کات لند // تاریخ: $dateStr", pageWidth - 30f, 65f, textPaint)

                // Header Line Accent
                paint.color = Color.parseColor("#81C784")
                canvas.drawRect(0f, 95f, pageWidth.toFloat(), 100f, paint)

                // Project Details
                textPaint.color = Color.BLACK
                var currentY = 135f

                textPaint.textSize = 14f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val nameToDisplay = _currentProjectName.value ?: "پروژه جدید بدون نام"
                drawRtlText(canvas, "نام پروژه: $nameToDisplay", pageWidth - 30f, currentY, textPaint)

                currentY += 35f

                // Technical specs background box
                paint.color = Color.parseColor("#F1F8E9")
                canvas.drawRect(25f, currentY - 15f, pageWidth - 25f, currentY + 115f, paint)

                paint.color = Color.parseColor("#C8E6C9")
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f
                canvas.drawRect(25f, currentY - 15f, pageWidth - 25f, currentY + 115f, paint)
                paint.style = Paint.Style.FILL // Reset

                // Specs details
                textPaint.textSize = 11f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textPaint.color = Color.parseColor("#1B5E20")
                drawRtlText(canvas, "مشخصات فنی سنگ خام و ابزار برش:", pageWidth - 40f, currentY, textPaint)

                currentY += 25f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textPaint.color = Color.BLACK
                
                drawRtlText(canvas, "ابعاد اسلب خام اصلی: ${_standardSlab.value.length.toInt()} × ${_standardSlab.value.width.toInt()} میلی‌متر (ضخامت: ${_standardSlab.value.thickness.toInt()} میلی‌متر)", pageWidth - 40f, currentY, textPaint)

                currentY += 20f
                drawRtlText(canvas, "ضخامت دیسک اره (Kerf): ${_diskThickness.value} میلی‌متر // مقدار حاشیه هرس اسلب: ${_trimMargin.value} میلی‌متر", pageWidth - 40f, currentY, textPaint)

                val result = _optimizationResult.value
                val efficiencyStr = if (result != null) String.format(Locale.US, "%.1f%%", result.totalYieldPercentage) else "---"
                val slabsUsedCount = result?.slabLayouts?.filter { !it.isScrap }?.size ?: 0
                val scrapsUsedCount = result?.slabLayouts?.filter { it.isScrap }?.size ?: 0

                currentY += 20f
                drawRtlText(canvas, "بازدهی کل طرح: $efficiencyStr // تعداد کل قطعات برش: ${_parts.value.size} مورد", pageWidth - 40f, currentY, textPaint)

                currentY += 20f
                drawRtlText(canvas, "تعداد اسلب خام مصرفی: $slabsUsedCount عدد // تعداد ضایعات بازیافتی مصرفی: $scrapsUsedCount عدد", pageWidth - 40f, currentY, textPaint)

                currentY += 55f

                // Table of Parts Header
                textPaint.textSize = 12f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textPaint.color = Color.parseColor("#1B5E20")
                drawRtlText(canvas, "لیست جزئیات قطعات برش پروژه:", pageWidth - 30f, currentY, textPaint)

                currentY += 20f

                // Table header box
                paint.color = Color.parseColor("#EEEEEE")
                canvas.drawRect(30f, currentY, pageWidth - 30f, currentY + 25f, paint)

                textPaint.textSize = 9f
                textPaint.color = Color.BLACK

                val colIdX = 45f
                val colNameX = 110f
                val colSizeX = 330f
                val colRotX = 430f
                val colMatchX = 510f

                canvas.drawText("شناسه", colIdX, currentY + 16f, textPaint)
                canvas.drawText("نام قطعه / محل استفاده", colNameX, currentY + 16f, textPaint)
                canvas.drawText("ابعاد (عرض × طول، میلی‌متر)", colSizeX, currentY + 16f, textPaint)
                canvas.drawText("چرخش", colRotX, currentY + 16f, textPaint)
                canvas.drawText("پیوستگی رگه سنگ", colMatchX, currentY + 16f, textPaint)

                currentY += 25f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                _parts.value.forEachIndexed { index, part ->
                    if (currentY > pageHeight - 80f) {
                        pdfDocument.finishPage(page)
                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas

                        // Simple page top banner
                        paint.color = Color.parseColor("#1B5E20")
                        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 35f, paint)
                        textPaint.color = Color.WHITE
                        textPaint.textSize = 10f
                        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        drawRtlText(canvas, "لیست قطعات پروژه (ادامه)", pageWidth - 30f, 12f, textPaint)

                        currentY = 55f
                        paint.color = Color.parseColor("#EEEEEE")
                        canvas.drawRect(30f, currentY, pageWidth - 30f, currentY + 25f, paint)
                        textPaint.color = Color.BLACK
                        canvas.drawText("شناسه", colIdX, currentY + 16f, textPaint)
                        canvas.drawText("نام قطعه / محل استفاده", colNameX, currentY + 16f, textPaint)
                        canvas.drawText("ابعاد (میلی‌متر)", colSizeX, currentY + 16f, textPaint)
                        canvas.drawText("چرخش", colRotX, currentY + 16f, textPaint)
                        canvas.drawText("پیوستگی رگه سنگ", colMatchX, currentY + 16f, textPaint)
                        currentY += 25f
                    }

                    // Alternating background for rows
                    if (index % 2 == 1) {
                        paint.color = Color.parseColor("#FAFAFA")
                        canvas.drawRect(30f, currentY, pageWidth - 30f, currentY + 22f, paint)
                    }

                    paint.color = Color.parseColor("#E0E0E0")
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 0.5f
                    canvas.drawLine(30f, currentY + 22f, pageWidth - 30f, currentY + 22f, paint)
                    paint.style = Paint.Style.FILL // Reset

                    textPaint.color = Color.BLACK
                    textPaint.textSize = 9f
                    canvas.drawText(part.id, colIdX, currentY + 15f, textPaint)
                    
                    drawPersianTextColumn(canvas, part.name, colNameX, colSizeX - 10f, currentY + 15f, textPaint)
                    
                    val dims = "${part.width.toInt()} × ${part.length.toInt()}"
                    canvas.drawText(dims, colSizeX, currentY + 15f, textPaint)
                    
                    val rotStr = if (part.allowRotation) "مجاز" else "ثابت"
                    drawPersianTextColumn(canvas, rotStr, colRotX, colMatchX - 10f, currentY + 15f, textPaint)
                    
                    val matchStr = if (part.matchAdjacentTo.isNotEmpty()) "هم‌راستا با قطعه ${part.matchAdjacentTo}" else "مستقل"
                    drawPersianTextColumn(canvas, matchStr, colMatchX, pageWidth - 35f, currentY + 15f, textPaint)

                    currentY += 22f
                }

                pdfDocument.finishPage(page)

                // Page 2+: Layout maps for each raw plate / slab
                result?.slabLayouts?.forEachIndexed { layoutIdx, layout ->
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas

                    // Technical Page Header
                    paint.color = Color.parseColor("#37474F") // Technical dark gray
                    canvas.drawRect(0f, 0f, pageWidth.toFloat(), 55f, paint)

                    val plateLabel = layout.containerId
                        .replace("Slab", "اسلب")
                        .replace("Scrap", "ضایعات")

                    textPaint.color = Color.WHITE
                    textPaint.textSize = 13f
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    drawRtlText(canvas, "نقشه چیدمان و موقعیت‌های برش سنگ: $plateLabel", pageWidth - 30f, 15f, textPaint)

                    textPaint.textSize = 9f
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    val rawSlabType = if (layout.isScrap) "قطعه ضایعاتی بازیافتی" else "اسلب خام استاندارد"
                    drawRtlText(canvas, "نوع ورق: $rawSlabType // ابعاد ورق خام: ${layout.originalLength.toInt()} × ${layout.originalWidth.toInt()} میلی‌متر // راندمان مصرف: ${String.format(Locale.US, "%.1f%%", layout.efficiency)}", pageWidth - 30f, 38f, textPaint)

                    // Draw Slab Visual Map inside scaled coordinate system
                    val visualTop = 80f
                    val visualBottom = 330f
                    val visualHeight = visualBottom - visualTop
                    val visualLeft = 40f
                    val visualRight = pageWidth - 40f
                    val visualWidth = visualRight - visualLeft

                    val scaleX = visualWidth / layout.originalLength
                    val scaleY = visualHeight / layout.originalWidth
                    val scale = Math.min(scaleX, scaleY)

                    val actualSlabW = layout.originalLength * scale
                    val actualSlabH = layout.originalWidth * scale

                    val drawStartX = visualLeft + (visualWidth - actualSlabW) / 2f
                    val drawStartY = visualTop + (visualHeight - actualSlabH) / 2f

                    // Draw plate background representation
                    paint.color = Color.parseColor("#ECEFF1") // Marble light gray color
                    canvas.drawRect(drawStartX, drawStartY, drawStartX + actualSlabW, drawStartY + actualSlabH, paint)

                    // Draw plate border
                    paint.color = Color.parseColor("#78909C")
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 2f
                    canvas.drawRect(drawStartX, drawStartY, drawStartX + actualSlabW, drawStartY + actualSlabH, paint)
                    paint.style = Paint.Style.FILL // Reset

                    // Draw Stone Veins decoration
                    paint.color = Color.parseColor("#CFD8DC")
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 1f
                    val veinPath = android.graphics.Path()
                    veinPath.moveTo(drawStartX + actualSlabW * 0.15f, drawStartY)
                    veinPath.cubicTo(
                        drawStartX + actualSlabW * 0.35f, drawStartY + actualSlabH * 0.35f,
                        drawStartX + actualSlabW * 0.2f, drawStartY + actualSlabH * 0.65f,
                        drawStartX + actualSlabW * 0.6f, drawStartY + actualSlabH
                    )
                    canvas.drawPath(veinPath, paint)
                    
                    val veinPath2 = android.graphics.Path()
                    veinPath2.moveTo(drawStartX + actualSlabW * 0.5f, drawStartY)
                    veinPath2.cubicTo(
                        drawStartX + actualSlabW * 0.75f, drawStartY + actualSlabH * 0.25f,
                        drawStartX + actualSlabW * 0.6f, drawStartY + actualSlabH * 0.75f,
                        drawStartX + actualSlabW * 0.9f, drawStartY + actualSlabH
                    )
                    canvas.drawPath(veinPath2, paint)
                    paint.style = Paint.Style.FILL // Reset

                    // Draw parts inside the slab
                    layout.placedParts.forEach { part ->
                        val px = drawStartX + part.x * scale
                        val py = drawStartY + part.y * scale
                        val pw = part.width * scale
                        val ph = part.height * scale

                        // Draw Part Fill (mint green style highlight for useful parts)
                        paint.color = Color.parseColor("#E8F5E9")
                        canvas.drawRect(px, py, px + pw, py + ph, paint)

                        // Draw Part Green Outlines
                        paint.color = Color.parseColor("#4CAF50")
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = 1.2f
                        canvas.drawRect(px, py, px + pw, py + ph, paint)
                        paint.style = Paint.Style.FILL // Reset

                        // Text labels centered
                        textPaint.color = Color.parseColor("#1B5E20")
                        textPaint.textSize = 10f
                        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        
                        val labelId = "قطعه ${part.part.id}"
                        val textW = textPaint.measureText(labelId)
                        canvas.drawText(labelId, px + pw / 2f - textW / 2f, py + ph / 2f + 3f, textPaint)

                        if (pw > 45f && ph > 25f) {
                            textPaint.textSize = 6.5f
                            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                            val dimsText = "${part.part.width.toInt()}x${part.part.length.toInt()}"
                            val dimsW = textPaint.measureText(dimsText)
                            canvas.drawText(dimsText, px + pw / 2f - dimsW / 2f, py + ph / 2f + 12f, textPaint)
                        }
                    }

                    // Draw useful offcuts inside the slab
                    layout.offcuts.forEach { offcut ->
                        val ox_off = drawStartX + offcut.x * scale
                        val oy_off = drawStartY + offcut.y * scale
                        val ow_off = offcut.width * scale
                        val oh_off = offcut.height * scale

                        // Draw offcut fill (light orange/amber)
                        paint.color = Color.parseColor("#FFF3E0")
                        canvas.drawRect(ox_off, oy_off, ox_off + ow_off, oy_off + oh_off, paint)

                        // Draw dashed border
                        paint.color = Color.parseColor("#FFB74D")
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = 1f
                        val dashPath = android.graphics.Path()
                        dashPath.addRect(ox_off, oy_off, ox_off + ow_off, oy_off + oh_off, android.graphics.Path.Direction.CW)
                        canvas.drawPath(dashPath, paint)
                        paint.style = Paint.Style.FILL // Reset

                        // Draw text "پرت مفید"
                        textPaint.color = Color.parseColor("#E65100")
                        textPaint.textSize = 8f
                        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        val lbl = "پرت"
                        val lblW = textPaint.measureText(lbl)
                        canvas.drawText(lbl, ox_off + ow_off / 2f - lblW / 2f, oy_off + oh_off / 2f + 3f, textPaint)
                    }

                    // Operator Instructions section below
                    currentY = 350f
                    textPaint.color = Color.parseColor("#37474F")
                    textPaint.textSize = 12f
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    drawRtlText(canvas, "ترتیب گام‌های برش سنگ مخصوص اپراتور دستگاه اره دیسکی:", pageWidth - 30f, currentY, textPaint)

                    currentY += 18f
                    textPaint.textSize = 10f
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    textPaint.color = Color.BLACK

                    val stepsList = layout.instructions

                    stepsList.forEach { step ->
                        if (currentY > pageHeight - 50f) {
                            pdfDocument.finishPage(page)
                            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create()
                            page = pdfDocument.startPage(pageInfo)
                            canvas = page.canvas

                            // technical Header again
                            paint.color = Color.parseColor("#37474F")
                            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 35f, paint)
                            textPaint.color = Color.WHITE
                            textPaint.textSize = 11f
                            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            drawRtlText(canvas, "ترتیب مراحل برش اسلب (ادامه)", pageWidth - 30f, 12f, textPaint)

                            currentY = 55f
                            textPaint.color = Color.BLACK
                            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        }

                        // Step index badge
                        paint.color = Color.parseColor("#ECEFF1")
                        canvas.drawRoundRect(30f, currentY - 11f, 52f, currentY + 7f, 4f, 4f, paint)

                        paint.color = Color.parseColor("#546E7A")
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = 0.8f
                        canvas.drawRoundRect(30f, currentY - 11f, 52f, currentY + 7f, 4f, 4f, paint)
                        paint.style = Paint.Style.FILL // Reset

                        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textPaint.color = Color.parseColor("#37474F")
                        canvas.drawText("${step.stepNo}", 37f, currentY + 2f, textPaint)

                        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        textPaint.color = Color.BLACK
                        
                        drawPersianTextColumn(canvas, step.description, 60f, pageWidth - 30f, currentY + 2f, textPaint)

                        currentY += 28f
                    }

                    pdfDocument.finishPage(page)
                }

                // Write document to cache directory
                val reportFile = File(context.cacheDir, "stone_cut_report_${System.currentTimeMillis()}.pdf")
                val fos = FileOutputStream(reportFile)
                pdfDocument.writeTo(fos)
                pdfDocument.close()
                fos.close()

                withContext(Dispatchers.Main) {
                    onComplete(reportFile)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onError(e)
                }
            }
        }
    }

    private fun drawRtlText(canvas: Canvas, text: String, x: Float, y: Float, paint: TextPaint) {
        val width = paint.measureText(text).toInt() + 10
        val staticLayout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_OPPOSITE)
            .build()
        canvas.save()
        canvas.translate(x - width, y)
        staticLayout.draw(canvas)
        canvas.restore()
    }

    private fun drawPersianTextColumn(canvas: Canvas, text: String, startX: Float, endX: Float, y: Float, paint: TextPaint) {
        val width = (endX - startX).toInt()
        if (width <= 0) return
        val staticLayout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()
        canvas.save()
        canvas.translate(startX, y - 10f)
        staticLayout.draw(canvas)
        canvas.restore()
    }
}

