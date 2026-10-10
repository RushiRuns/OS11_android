package com.os11.app.service.task

/**
 * Service DTO for Browse and navigation badge counts.
 */
data class BrowseCountsItem(
    val inboxCount: Int = 0,
    val myDayCount: Int = 0,
    val plannedCount: Int = 0,
    val importantCount: Int = 0,
    val anytimeCount: Int = 0,
    val somedayCount: Int = 0,
    val waitingForCount: Int = 0,
    val completedCount: Int = 0,
    val trashCount: Int = 0,
)
