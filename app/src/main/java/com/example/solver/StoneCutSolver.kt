package com.example.solver

import com.example.model.*
import kotlin.math.max

object StoneCutSolver {

    fun optimize(
        standardSlab: StandardSlab,
        scrapInventory: List<ScrapPiece>,
        useScrap: Boolean,
        parts: List<Part>,
        diskThickness: Float,
        trimMargin: Float
    ): OptimizationResult {
        if (parts.isEmpty()) {
            return OptimizationResult(
                standardSlabsUsedCount = 0,
                scrapPiecesUsedCount = 0,
                totalYieldPercentage = 0f,
                totalSlabScrapArea = 0f,
                totalDiskKerfArea = 0f,
                totalPartArea = 0f,
                slabLayouts = emptyList()
            )
        }

        // 1. Graph Construction: Group parts into chains and independent parts
        val chains = buildChains(parts)
        val independentParts = parts.filter { part ->
            // Independent if not in any multi-part chain
            val chain = chains.find { it.contains(part) }
            chain == null || chain.size == 1
        }
        val multiPartChains = chains.filter { it.size > 1 }

        val usedScraps = mutableListOf<ScrapPiece>()
        val slabLayouts = mutableListOf<SlabLayout>()

        // 2. Scrap First Evaluation (for independent parts)
        val remainingIndependent = independentParts.toMutableList()
        if (useScrap) {
            val enabledScraps = scrapInventory.filter { it.isEnabled }.sortedBy { it.length * it.width }
            for (scrap in enabledScraps) {
                // Try to pack as many independent parts as possible into this scrap piece
                val usableL = max(0f, scrap.length - 2 * trimMargin)
                val usableW = max(0f, scrap.width - 2 * trimMargin)

                if (usableL <= 0 || usableW <= 0) continue

                val packedInThisScrap = mutableListOf<PlacedPart>()
                val remainingTemp = mutableListOf<Part>()

                // Simple shelf packing on this scrap piece
                var currentX = 0f
                var currentY = 0f
                var currentShelfHeight = 0f
                var scrapUsed = false

                for (part in remainingIndependent) {
                    var placed = false
                    val dimensions = getOrientations(part)

                    for (dim in dimensions) {
                        val pw = dim.first
                        val ph = dim.second

                        // Check if fits on current shelf
                        if (currentX + pw <= usableL && currentY + ph <= usableW) {
                            packedInThisScrap.add(
                                PlacedPart(
                                    part = part,
                                    x = trimMargin + currentX,
                                    y = trimMargin + currentY,
                                    width = pw,
                                    height = ph,
                                    isRotated = pw != part.width,
                                    isScrapPiece = true,
                                    containerId = "Scrap #${scrap.id}"
                                )
                            )
                            currentX += pw + diskThickness
                            currentShelfHeight = max(currentShelfHeight, ph)
                            placed = true
                            scrapUsed = true
                            break
                        }
                    }

                    if (!placed) {
                        // Try new shelf
                        val nextY = currentY + currentShelfHeight + diskThickness
                        for (dim in dimensions) {
                            val pw = dim.first
                            val ph = dim.second

                            if (pw <= usableL && nextY + ph <= usableW) {
                                currentX = 0f
                                currentY = nextY
                                currentShelfHeight = ph
                                packedInThisScrap.add(
                                    PlacedPart(
                                        part = part,
                                        x = trimMargin + currentX,
                                        y = trimMargin + currentY,
                                        width = pw,
                                        height = ph,
                                        isRotated = pw != part.width,
                                        isScrapPiece = true,
                                        containerId = "Scrap #${scrap.id}"
                                    )
                                )
                                currentX += pw + diskThickness
                                placed = true
                                scrapUsed = true
                                break
                            }
                        }
                    }

                    if (!placed) {
                        remainingTemp.add(part)
                    }
                }

                if (scrapUsed) {
                    usedScraps.add(scrap.copy(isUsed = true))
                    remainingIndependent.clear()
                    remainingIndependent.addAll(remainingTemp)

                    // Compute Scrap layout statistics
                    val scrapArea = scrap.length * scrap.width
                    val partArea = packedInThisScrap.sumOf { (it.width * it.height).toDouble() }.toFloat()
                    
                    // Kerf area inside scrap: each part after the first on shelf adds a kerf cut, plus shelf splits
                    // Let's compute actual kerf used
                    val kerfArea = computeKerfArea(packedInThisScrap, diskThickness)
                    val wasteSlabScrap = max(0f, scrapArea - partArea - kerfArea)
                    val efficiency = (partArea / scrapArea) * 100f

                    val cutLines = generateCutLines(packedInThisScrap, scrap.length, scrap.width, trimMargin, diskThickness)
                    val instructions = generateInstructions(packedInThisScrap, scrap.length, scrap.width, trimMargin, diskThickness, "Scrap #${scrap.id}")

                    slabLayouts.add(
                        SlabLayout(
                            containerId = "Scrap #${scrap.id}",
                            isScrap = true,
                            originalLength = scrap.length,
                            originalWidth = scrap.width,
                            trimMargin = trimMargin,
                            placedParts = packedInThisScrap,
                            efficiency = efficiency,
                            wasteSlabScrapArea = wasteSlabScrap,
                            wasteDiskKerfArea = kerfArea,
                            cutLines = cutLines,
                            instructions = instructions
                        )
                    )
                }
            }
        }

        // 3. Chain & Remaining Independent Nesting on Standard Slabs
        // Prepare packing items. Each item is either a compound chain or a single part.
        val packItems = mutableListOf<PackableItem>()

        // Convert multi-part chains to PackableItems
        for (chain in multiPartChains) {
            packItems.add(PackableItem.fromChain(chain, diskThickness))
        }

        // Convert remaining independent parts to PackableItems
        for (part in remainingIndependent) {
            packItems.add(PackableItem.fromPart(part))
        }

        // Sort items by height descending to optimize shelf packing
        packItems.sortByDescending { it.height }

        val usableL = max(0f, standardSlab.length - 2 * trimMargin)
        val usableW = max(0f, standardSlab.width - 2 * trimMargin)

        var slabIndex = 1
        val itemsToPack = packItems.toMutableList()

        while (itemsToPack.isNotEmpty()) {
            val containerId = "Slab $slabIndex"
            val packedInSlab = mutableListOf<PlacedPart>()
            val unplacedForNextSlab = mutableListOf<PackableItem>()

            var currentX = 0f
            var currentY = 0f
            var currentShelfHeight = 0f

            for (item in itemsToPack) {
                var placed = false

                // Try placing in current shelf
                if (currentX + item.width <= usableL && currentY + item.height <= usableW) {
                    packedInSlab.addAll(item.toPlacedParts(trimMargin + currentX, trimMargin + currentY, containerId, false))
                    currentX += item.width + diskThickness
                    currentShelfHeight = max(currentShelfHeight, item.height)
                    placed = true
                }

                // If independent part, try with rotation
                if (!placed && item.isSinglePart && item.parts.first().allowRotation) {
                    if (currentX + item.height <= usableL && currentY + item.width <= usableW) {
                        packedInSlab.addAll(item.toPlacedParts(trimMargin + currentX, trimMargin + currentY, containerId, true))
                        currentX += item.height + diskThickness
                        currentShelfHeight = max(currentShelfHeight, item.width)
                        placed = true
                    }
                }

                // Try new shelf
                if (!placed) {
                    val nextY = currentY + currentShelfHeight + diskThickness
                    if (currentX > 0f && nextY + item.height <= usableW && item.width <= usableL) {
                        currentX = 0f
                        currentY = nextY
                        currentShelfHeight = item.height
                        packedInSlab.addAll(item.toPlacedParts(trimMargin + currentX, trimMargin + currentY, containerId, false))
                        currentX += item.width + diskThickness
                        placed = true
                    } else if (currentX > 0f && item.isSinglePart && item.parts.first().allowRotation && nextY + item.width <= usableW && item.height <= usableL) {
                        currentX = 0f
                        currentY = nextY
                        currentShelfHeight = item.width
                        packedInSlab.addAll(item.toPlacedParts(trimMargin + currentX, trimMargin + currentY, containerId, true))
                        currentX += item.height + diskThickness
                        placed = true
                    }
                }

                if (!placed) {
                    unplacedForNextSlab.add(item)
                }
            }

            if (packedInSlab.isNotEmpty()) {
                val slabArea = standardSlab.length * standardSlab.width
                val partArea = packedInSlab.sumOf { (it.width * it.height).toDouble() }.toFloat()
                val kerfArea = computeKerfArea(packedInSlab, diskThickness)
                val wasteSlabScrap = max(0f, slabArea - partArea - kerfArea)
                val efficiency = (partArea / slabArea) * 100f

                val cutLines = generateCutLines(packedInSlab, standardSlab.length, standardSlab.width, trimMargin, diskThickness)
                val instructions = generateInstructions(packedInSlab, standardSlab.length, standardSlab.width, trimMargin, diskThickness, containerId)

                slabLayouts.add(
                    SlabLayout(
                        containerId = containerId,
                        isScrap = false,
                        originalLength = standardSlab.length,
                        originalWidth = standardSlab.width,
                        trimMargin = trimMargin,
                        placedParts = packedInSlab,
                        efficiency = efficiency,
                        wasteSlabScrapArea = wasteSlabScrap,
                        wasteDiskKerfArea = kerfArea,
                        cutLines = cutLines,
                        instructions = instructions
                    )
                )
                slabIndex++
            } else {
                // Critical: Item is too large to fit even a single empty slab.
                // We force-place or skip to prevent infinite loop.
                if (itemsToPack.size == unplacedForNextSlab.size) {
                    // All remaining items are too big, we skip them to avoid freeze
                    break
                }
            }

            itemsToPack.clear()
            itemsToPack.addAll(unplacedForNextSlab)
        }

        // Compute overall statistics
        val totalPartArea = parts.sumOf { (it.length * it.width).toDouble() }.toFloat()
        val standardSlabsUsed = slabLayouts.count { !it.isScrap }
        val scrapsUsedCount = usedScraps.size

        val totalContainerArea = (standardSlabsUsed * standardSlab.length * standardSlab.width) +
                slabLayouts.filter { it.isScrap }.sumOf { (it.originalLength * it.originalWidth).toDouble() }.toFloat()

        val totalYield = if (totalContainerArea > 0) (totalPartArea / totalContainerArea) * 100f else 0f
        val totalKerf = slabLayouts.sumOf { it.wasteDiskKerfArea.toDouble() }.toFloat()
        val totalScrapArea = slabLayouts.sumOf { it.wasteSlabScrapArea.toDouble() }.toFloat()

        return OptimizationResult(
            standardSlabsUsedCount = standardSlabsUsed,
            scrapPiecesUsedCount = scrapsUsedCount,
            totalYieldPercentage = totalYield,
            totalSlabScrapArea = totalScrapArea,
            totalDiskKerfArea = totalKerf,
            totalPartArea = totalPartArea,
            slabLayouts = slabLayouts
        )
    }

    private fun buildChains(parts: List<Part>): List<List<Part>> {
        val partMap = parts.associateBy { it.id }
        val visited = mutableSetOf<String>()
        val chains = mutableListOf<List<Part>>()

        // Build parent relationships
        val childrenMap = mutableMapOf<String, MutableList<Part>>()
        for (part in parts) {
            if (part.matchAdjacentTo.isNotEmpty()) {
                childrenMap.getOrPut(part.matchAdjacentTo) { mutableListOf() }.add(part)
            }
        }

        // Trace chains starting from roots (parts that are NOT matching anyone else)
        val roots = parts.filter { it.matchAdjacentTo.isEmpty() }
        for (root in roots) {
            val currentChain = mutableListOf<Part>()
            var current: Part? = root
            while (current != null && !visited.contains(current.id)) {
                visited.add(current.id)
                currentChain.add(current)
                // Next item in chain is the child that matches this current item
                // If there are multiple, trace the first one
                val children = childrenMap[current.id] ?: emptyList()
                current = children.firstOrNull()
            }
            if (currentChain.isNotEmpty()) {
                chains.add(currentChain)
            }
        }

        // Fallback for circular or orphaned chains
        for (part in parts) {
            if (!visited.contains(part.id)) {
                val currentChain = mutableListOf<Part>()
                var current: Part? = part
                while (current != null && !visited.contains(current.id)) {
                    visited.add(current.id)
                    currentChain.add(current)
                    val children = childrenMap[current.id] ?: emptyList()
                    current = children.firstOrNull()
                }
                if (currentChain.isNotEmpty()) {
                    chains.add(currentChain)
                }
            }
        }

        return chains
    }

    private fun getOrientations(part: Part): List<Pair<Float, Float>> {
        val list = mutableListOf(Pair(part.width, part.length)) // Standard: Width (X) x Length (Y)
        if (part.allowRotation) {
            list.add(Pair(part.length, part.width)) // Rotated 90 deg
        }
        return list
    }

    private fun computeKerfArea(placedParts: List<PlacedPart>, diskThickness: Float): Float {
        // Kerf loss area estimation.
        // For N parts, there are cutting operations. Let's approximate kerf loss.
        var kerfArea = 0f
        // Trim cut area
        if (placedParts.isNotEmpty()) {
            val first = placedParts.first()
            val isScrap = first.isScrapPiece
            // We assume a trim around 4 edges of the slab
            // Wait, we only add kerf area for cuts made
            // Inside the layout, each placed part is separated by a kerf cut.
            // Let's estimate kerf loss area as the perimeter of the cut lines * diskThickness
            val containerId = first.containerId
            // Let's find unique X boundaries and Y boundaries of placed parts to compute cuts
            val xCuts = placedParts.map { it.x + it.width }.filter { it > 0f }.distinct()
            val yCuts = placedParts.map { it.y + it.height }.filter { it > 0f }.distinct()
            
            // Sum of kerf area: X cuts and Y cuts inside the container
            for (x in xCuts) {
                kerfArea += diskThickness * placedParts.filter { it.x + it.width == x }.sumOf { it.height.toDouble() }.toFloat()
            }
            for (y in yCuts) {
                kerfArea += diskThickness * placedParts.filter { it.y + it.height == y }.sumOf { it.width.toDouble() }.toFloat()
            }
        }
        return kerfArea
    }

    private fun generateCutLines(
        placedParts: List<PlacedPart>,
        slabL: Float,
        slabW: Float,
        trimMargin: Float,
        diskThickness: Float
    ): List<CutLine> {
        val cutLines = mutableListOf<CutLine>()

        if (placedParts.isEmpty()) return cutLines

        // 1. Trim cut lines
        if (trimMargin > 0f) {
            cutLines.add(CutLine(trimMargin, 0f, trimMargin, slabW, true, "Trim Left Margin"))
            cutLines.add(CutLine(slabL - trimMargin, 0f, slabL - trimMargin, slabW, true, "Trim Right Margin"))
            cutLines.add(CutLine(0f, trimMargin, slabL, trimMargin, true, "Trim Top Margin"))
            cutLines.add(CutLine(0f, slabW - trimMargin, slabL, slabW - trimMargin, true, "Trim Bottom Margin"))
        }

        // 2. Shelf / Longitudinal cuts (Primary)
        // Group parts by their top Y coordinate to identify shelves
        val shelves = placedParts.groupBy { it.y }.toSortedMap()
        var lastShelfMaxY = trimMargin
        
        for ((shelfY, partsInShelf) in shelves) {
            val maxHeight = partsInShelf.maxOf { it.height }
            val shelfEndY = shelfY + maxHeight
            
            // Primary longitudinal cut at the end of the shelf (except last shelf boundary if it touches trim margin)
            if (shelfEndY < slabW - trimMargin) {
                cutLines.add(
                    CutLine(
                        startX = trimMargin,
                        startY = shelfEndY + diskThickness / 2f,
                        endX = slabL - trimMargin,
                        endY = shelfEndY + diskThickness / 2f,
                        isPrimary = true,
                        description = "Longitudinal cut at Y = ${shelfEndY + diskThickness / 2f} mm"
                    )
                )
            }

            // 3. Transverse cuts inside the shelf (Secondary)
            val sortedParts = partsInShelf.sortedBy { it.x }
            for (i in 0 until sortedParts.size - 1) {
                val part1 = sortedParts[i]
                val part2 = sortedParts[i + 1]
                val cutX = part1.x + part1.width + diskThickness / 2f
                cutLines.add(
                    CutLine(
                        startX = cutX,
                        startY = shelfY,
                        endX = cutX,
                        endY = shelfEndY,
                        isPrimary = false,
                        description = "Transverse cut at X = $cutX mm (between ${part1.part.id} and ${part2.part.id})"
                    )
                )
            }

            // Slice the end of the last part if there is empty space to the right
            val lastPart = sortedParts.last()
            val endX = lastPart.x + lastPart.width
            if (endX < slabL - trimMargin) {
                cutLines.add(
                    CutLine(
                        startX = endX + diskThickness / 2f,
                        startY = shelfY,
                        endX = endX + diskThickness / 2f,
                        endY = shelfEndY,
                        isPrimary = false,
                        description = "Clean cut at X = ${endX + diskThickness / 2f} mm"
                    )
                )
            }
        }

        return cutLines
    }

    private fun generateInstructions(
        placedParts: List<PlacedPart>,
        slabL: Float,
        slabW: Float,
        trimMargin: Float,
        diskThickness: Float,
        containerId: String
    ): List<StepInstruction> {
        val instructions = mutableListOf<StepInstruction>()
        var step = 1

        val containerLabel = containerId
            .replace("Slab", "اسلب")
            .replace("Scrap", "ضایعات")

        // 1. Trim margins
        if (trimMargin > 0f) {
            instructions.add(
                StepInstruction(
                    stepNo = step++,
                    description = "[$containerLabel] هرس کردن حاشیه: برش $trimMargin میلی‌متر از هر ۴ لبه بیرونی اسلب برای جدا کردن بخش‌های ناهموار لبه سنگ."
                )
            )
        }

        // 2. Primary & Secondary cuts
        val shelves = placedParts.groupBy { it.y }.toSortedMap()
        var shelfNo = 1
        for ((shelfY, partsInShelf) in shelves) {
            val maxHeight = partsInShelf.maxOf { it.height }
            val shelfEndY = shelfY + maxHeight

            instructions.add(
                StepInstruction(
                    stepNo = step++,
                    description = "[$containerLabel] ایجاد برش طولی اصلی در Y = ${shelfEndY + diskThickness / 2f} میلی‌متر برای جدا کردن ردیف #$shelfNo (ارتفاع ردیف: $maxHeight میلی‌متر)."
                )
            )

            val sortedParts = partsInShelf.sortedBy { it.x }
            for (i in sortedParts.indices) {
                val part = sortedParts[i]
                val isLast = i == sortedParts.size - 1

                val desc = StringBuilder()
                desc.append("[$containerLabel] برش عرضی ردیف #$shelfNo در X = ${part.x + part.width + diskThickness / 2f} میلی‌متر ")
                desc.append("برای جدا کردن قطعه ${part.part.id} (${part.part.name}، ابعاد: ${part.part.width.toInt()} × ${part.part.length.toInt()} میلی‌متر).")
                
                if (part.isRotated) {
                    desc.append(" (قطعه جهت کاهش ضایعات ۹۰ درجه چرخانده شده است).")
                }

                // Mention vein matching chain details
                if (part.part.matchAdjacentTo.isNotEmpty()) {
                    desc.append(" *هشدار تطابق رگه: این قطعه بلافاصله در کنار قطعه ${part.part.matchAdjacentTo} چیده شده است تا هماهنگی رگه‌های طبیعی سنگ حفظ شود.*")
                }

                instructions.add(
                    StepInstruction(
                        stepNo = step++,
                        description = desc.toString()
                    )
                )
            }
            shelfNo++
        }

        return instructions
    }

    // A helper wrapper representing either a chain of parts or a single part packed as one block
    private class PackableItem(
        val parts: List<Part>,
        val width: Float,
        val height: Float,
        val isSinglePart: Boolean
    ) {
        companion object {
            fun fromPart(part: Part): PackableItem {
                return PackableItem(
                    parts = listOf(part),
                    width = part.width,
                    height = part.length,
                    isSinglePart = true
                )
            }

            fun fromChain(chain: List<Part>, diskThickness: Float): PackableItem {
                // Layout chain parts horizontally side-by-side
                val totalWidth = chain.sumOf { it.width.toDouble() }.toFloat() + (chain.size - 1) * diskThickness
                val maxHeight = chain.maxOf { it.length }
                return PackableItem(
                    parts = chain,
                    width = totalWidth,
                    height = maxHeight,
                    isSinglePart = false
                )
            }
        }

        fun toPlacedParts(startX: Float, startY: Float, containerId: String, rotateSingle: Boolean): List<PlacedPart> {
            val list = mutableListOf<PlacedPart>()
            if (isSinglePart) {
                val part = parts.first()
                if (rotateSingle) {
                    list.add(
                        PlacedPart(
                            part = part,
                            x = startX,
                            y = startY,
                            width = part.length,
                            height = part.width,
                            isRotated = true,
                            containerId = containerId
                        )
                    )
                } else {
                    list.add(
                        PlacedPart(
                            part = part,
                            x = startX,
                            y = startY,
                            width = part.width,
                            height = part.length,
                            isRotated = false,
                            containerId = containerId
                        )
                    )
                }
            } else {
                // Multi-part chain: nested horizontally in original orientation to match vein
                var currentX = startX
                val diskThickness = if (parts.size > 1) {
                    val gap = (width - parts.sumOf { it.width.toDouble() }.toFloat()) / (parts.size - 1)
                    gap
                } else 0f

                for (part in parts) {
                    list.add(
                        PlacedPart(
                            part = part,
                            x = currentX,
                            y = startY, // align tops
                            width = part.width,
                            height = part.length,
                            isRotated = false,
                            containerId = containerId
                        )
                    )
                    currentX += part.width + diskThickness
                }
            }
            return list
        }
    }
}
