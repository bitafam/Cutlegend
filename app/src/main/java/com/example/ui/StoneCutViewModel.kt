package com.example.ui

import androidx.lifecycle.ViewModel
import com.example.model.*
import com.example.solver.StoneCutSolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StoneCutViewModel : ViewModel() {

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

    private val _diskThickness = MutableStateFlow(3.5f)
    val diskThickness: StateFlow<Float> = _diskThickness.asStateFlow()

    private val _trimMargin = MutableStateFlow(10f)
    val trimMargin: StateFlow<Float> = _trimMargin.asStateFlow()

    private val _optimizationResult = MutableStateFlow<OptimizationResult?>(null)
    val optimizationResult: StateFlow<OptimizationResult?> = _optimizationResult.asStateFlow()

    init {
        // Run initial optimization on start
        triggerOptimization()
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

    fun addPart(name: String, length: Float, width: Float, allowRotation: Boolean, matchAdjacentTo: String) {
        val nextId = getNextLatinId()
        val newPart = Part(nextId, name, length, width, allowRotation, matchAdjacentTo)
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
            Part(id = "A", name = "اسلب اصلی الف", length = 600f, width = 1800f, allowRotation = false),
            Part(id = "B", name = "اسلب پیشانی ال‌شکل ب", length = 600f, width = 1200f, allowRotation = false, matchAdjacentTo = "A"),
            Part(id = "C", name = "قرنیز دیواری بلند ج", length = 300f, width = 1800f, allowRotation = false, matchAdjacentTo = "B"),
            Part(id = "D", name = "نوار برش تزیینی د", length = 150f, width = 900f, allowRotation = true),
            Part(id = "E", name = "نگین سنگی تزیینی ه", length = 200f, width = 200f, allowRotation = true)
        )
        triggerOptimization()
    }

    fun loadWallCladdingTemplate() {
        _parts.value = listOf(
            Part(id = "A", name = "پنل دیواری چپ", length = 1200f, width = 1400f, allowRotation = false),
            Part(id = "B", name = "پنل دیواری راست بوک‌مچ", length = 1200f, width = 1400f, allowRotation = false, matchAdjacentTo = "A"),
            Part(id = "C", name = "باند تزیینی بالای دیوار", length = 400f, width = 2800f, allowRotation = false),
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
        val sb = StringBuilder()
        var temp = index
        while (temp >= 0) {
            sb.insert(0, ('A' + (temp % 26)))
            temp = (temp / 26) - 1
        }
        return sb.toString()
    }
}
