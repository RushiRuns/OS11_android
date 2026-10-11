package com.os11.app.ui.features.dayplan

import androidx.compose.runtime.Immutable
import com.os11.app.service.task.TaskItem

private const val MINUTES_PER_HOUR = 60

@Immutable
data class DayPlanBlock(
    val task: TaskItem,
    val startMin: Int,
    val durationMin: Int,
    val laneIndex: Int = 0,
    val totalLanes: Int = 1,
) {
    val endMin: Int get() = startMin + durationMin

    val formattedTimeRange: String
        get() {
            val startH = startMin / MINUTES_PER_HOUR
            val startM = startMin % MINUTES_PER_HOUR
            val endH = endMin / MINUTES_PER_HOUR
            val endM = endMin % MINUTES_PER_HOUR
            val startStr = "%02d:%02d".format(startH, startM)
            val endStr = "%02d:%02d".format(endH, endM)
            return "$startStr - $endStr (${durationMin}m)"
        }
}

@Immutable
data class DayPlanUiState(
    val date: String = "",
    val dayStartMin: Int = DEFAULT_DAY_START_MIN,
    val scheduledBlocks: List<DayPlanBlock> = emptyList(),
    val unscheduledTasks: List<TaskItem> = emptyList(),
    val selectedUnscheduledIds: Set<String> = emptySet(),
    val isAgendaEnabled: Boolean = true,
    val isSheetExpanded: Boolean = false,
    val selectedBlockForAction: DayPlanBlock? = null,
    val isLoading: Boolean = false,
) {
    companion object {
        const val DEFAULT_DAY_START_MIN = 360 // 06:00 AM
    }
}

/**
 * Computes side-by-side lanes for overlapping time blocks.
 * Uses interval graph coloring to assign each block a lane and calculates
 * the total concurrent lanes per connected overlapping component.
 */
object DayPlanLaneCalculator {
    fun computeLanes(rawBlocks: List<DayPlanBlock>): List<DayPlanBlock> {
        if (rawBlocks.isEmpty()) return emptyList()

        // 1. Sort by startMin ASC, then durationMin DESC
        val sorted = rawBlocks.sortedWith(
            compareBy<DayPlanBlock> { it.startMin }.thenByDescending { it.durationMin },
        )

        // 2. Identify connected overlapping clusters
        val clusters = mutableListOf<MutableList<DayPlanBlock>>()
        var currentCluster = mutableListOf<DayPlanBlock>()
        var clusterMaxEnd = -1

        for (block in sorted) {
            if (currentCluster.isEmpty()) {
                currentCluster.add(block)
                clusterMaxEnd = block.endMin
            } else if (block.startMin < clusterMaxEnd) {
                currentCluster.add(block)
                clusterMaxEnd = maxOf(clusterMaxEnd, block.endMin)
            } else {
                clusters.add(currentCluster)
                currentCluster = mutableListOf(block)
                clusterMaxEnd = block.endMin
            }
        }
        if (currentCluster.isNotEmpty()) {
            clusters.add(currentCluster)
        }

        // 3. In each cluster, assign lanes using greedy graph coloring
        return clusters.flatMap { cluster -> assignLanesToCluster(cluster) }
    }

    private fun assignLanesToCluster(cluster: List<DayPlanBlock>): List<DayPlanBlock> {
        val laneAssignments = mutableListOf<Int>()
        val laneEndTimes = mutableListOf<Int>()

        for (block in cluster) {
            var assignedLane = -1
            for (laneIdx in laneEndTimes.indices) {
                if (laneEndTimes[laneIdx] <= block.startMin) {
                    assignedLane = laneIdx
                    laneEndTimes[laneIdx] = block.endMin
                    break
                }
            }
            if (assignedLane == -1) {
                assignedLane = laneEndTimes.size
                laneEndTimes.add(block.endMin)
            }
            laneAssignments.add(assignedLane)
        }

        val totalLanes = maxOf(1, laneEndTimes.size)
        return cluster.mapIndexed { i, block ->
            block.copy(
                laneIndex = laneAssignments[i],
                totalLanes = totalLanes,
            )
        }
    }
}
