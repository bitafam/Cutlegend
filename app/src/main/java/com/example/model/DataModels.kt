package com.example.model

data class StandardSlab(
    val length: Float = 3000f, // in mm
    val width: Float = 1800f,  // in mm
    val thickness: Float = 20f // in mm
)

data class ScrapPiece(
    val id: Int,
    val length: Float,
    val width: Float,
    val isEnabled: Boolean = true,
    val isUsed: Boolean = false
)

data class Part(
    val id: String, // Sequential Latin Code (A, B, C...)
    val name: String,
    val length: Float,
    val width: Float,
    val allowRotation: Boolean = false,
    val matchAdjacentTo: String = "" // Latin ID of parent part
)

data class PlacedPart(
    val part: Part,
    val x: Float, // Top-left X coordinate on the slab (after edge margin)
    val y: Float, // Top-left Y coordinate on the slab (after edge margin)
    val width: Float, // width in actual placed orientation
    val height: Float, // height in actual placed orientation
    val isRotated: Boolean,
    val isScrapPiece: Boolean = false,
    val containerId: String // e.g. "Slab 1" or "Scrap 2"
)

data class CutLine(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val isPrimary: Boolean,
    val description: String
)

data class StepInstruction(
    val stepNo: Int,
    val description: String
)

data class SlabLayout(
    val containerId: String, // e.g., "Slab 1" or "Scrap A"
    val isScrap: Boolean,
    val originalLength: Float, // L (X axis)
    val originalWidth: Float,  // W (Y axis)
    val trimMargin: Float,
    val placedParts: List<PlacedPart>,
    val efficiency: Float, // %
    val wasteSlabScrapArea: Float, // mm²
    val wasteDiskKerfArea: Float,  // mm²
    val cutLines: List<CutLine>,
    val instructions: List<StepInstruction>
)

data class OptimizationResult(
    val standardSlabsUsedCount: Int,
    val scrapPiecesUsedCount: Int,
    val totalYieldPercentage: Float,
    val totalSlabScrapArea: Float,
    val totalDiskKerfArea: Float,
    val totalPartArea: Float,
    val slabLayouts: List<SlabLayout>
)
