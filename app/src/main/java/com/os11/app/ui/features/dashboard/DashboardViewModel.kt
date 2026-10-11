package com.os11.app.ui.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.os11.app.service.dashboard.DailyTrendPoint
import com.os11.app.service.dashboard.DashboardOverviewStats
import com.os11.app.service.dashboard.DashboardService
import com.os11.app.service.dashboard.DateRangeOption
import com.os11.app.service.dashboard.HeatmapDay
import com.os11.app.service.dashboard.PriorityDistributionItem
import com.os11.app.service.settings.SettingsService
import com.os11.domain.result.AppResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = false,
    val isDashboardEnabled: Boolean = true,
    val selectedRange: DateRangeOption = DateRangeOption.ThirtyDays,
    val stats: DashboardOverviewStats = DashboardOverviewStats(0, 0, 0, 0, 0f),
    val priorities: List<PriorityDistributionItem> = emptyList(),
    val dailyTrend: List<DailyTrendPoint> = emptyList(),
    val heatmapDays: List<HeatmapDay> = emptyList(),
    val exportedCsvContent: String? = null,
)

private const val SUBSCRIPTION_TIMEOUT_MS = 5000L

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardService: DashboardService,
    private val settingsService: SettingsService,
) : ViewModel() {

    private val selectedRange = MutableStateFlow(DateRangeOption.ThirtyDays)
    private val exportedCsvContent = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DashboardUiState> = combine(
        selectedRange.flatMapLatest { range -> dashboardService.observeDashboardData(range) },
        settingsService.observeModuleEnabled("dashboard"),
        exportedCsvContent,
    ) { data, isEnabled, csv ->
        DashboardUiState(
            isLoading = false,
            isDashboardEnabled = isEnabled,
            selectedRange = data.dateRange,
            stats = data.stats,
            priorities = data.priorities,
            dailyTrend = data.dailyTrend,
            heatmapDays = data.heatmapDays,
            exportedCsvContent = csv,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
        initialValue = DashboardUiState(isLoading = true),
    )

    fun selectRange(range: DateRangeOption) {
        selectedRange.value = range
    }

    fun exportCsv(onComplete: (String) -> Unit) {
        viewModelScope.launch {
            val result = dashboardService.exportDashboardToCsv(selectedRange.value)
            if (result is AppResult.Ok) {
                exportedCsvContent.value = result.value
                onComplete(result.value)
            }
        }
    }

    fun clearExportedCsv() {
        exportedCsvContent.value = null
    }
}
