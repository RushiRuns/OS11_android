package com.os11.app.service

import com.os11.app.data.db.entities.TaskEntity
import com.os11.app.data.repository.SearchRepository
import com.os11.app.service.search.SearchService
import com.os11.app.service.settings.SettingsService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SearchServiceTest {

    private val searchRepository: SearchRepository = mockk(relaxed = true)
    private val settingsService: SettingsService = mockk(relaxed = true)
    private lateinit var searchService: SearchService

    @Before
    fun setup() {
        searchService = SearchService(
            searchRepository = searchRepository,
            settingsService = settingsService,
            ioDispatcher = Dispatchers.Unconfined,
        )
    }

    @Test
    fun `searchTaskItems with empty query returns empty list immediately`() = runBlocking {
        val result = searchService.searchTaskItems("   ")
        assertTrue(result.isOk)
        assertEquals(emptyList<Any>(), result.getOrNull())
        coVerify(exactly = 0) { searchRepository.searchTasks(any(), any()) }
    }

    @Test
    fun `searchTaskItems delegates to repository and maps to TaskItem`() = runBlocking {
        val entity = TaskEntity(
            id = "task-1",
            title = "Doctor appointment",
            notes = "Bring insurance card",
            workspaceId = "ws-1",
            createdAt = "2026-10-10T10:00:00Z",
            updatedAt = "2026-10-10T10:00:00Z",
        )
        coEvery { searchRepository.searchTasks("doctor", 50) } returns listOf(entity)

        val result = searchService.searchTaskItems("doctor")
        assertTrue(result.isOk)
        val items = result.getOrNull()
        assertNotNull(items)
        assertEquals(1, items?.size)
        assertEquals("task-1", items?.first()?.id)
        assertEquals("Doctor appointment", items?.first()?.title)
    }

    @Test
    fun `addRecentSearch deduplicates and prepends query`() = runBlocking {
        coEvery { settingsService.getSetting("recent_searches_json") } returns "[\"groceries\"]"
        coEvery { settingsService.setSetting("recent_searches_json", any()) } returns
            com.os11.domain.result.AppResult.ok(Unit)

        searchService.addRecentSearch("dentist")

        coVerify {
            settingsService.setSetting("recent_searches_json", "[\"dentist\",\"groceries\"]")
        }
    }

    @Test
    fun `removeRecentSearch removes specified query`() = runBlocking {
        coEvery { settingsService.getSetting("recent_searches_json") } returns "[\"dentist\",\"groceries\"]"
        coEvery { settingsService.setSetting("recent_searches_json", any()) } returns
            com.os11.domain.result.AppResult.ok(Unit)

        searchService.removeRecentSearch("dentist")

        coVerify {
            settingsService.setSetting("recent_searches_json", "[\"groceries\"]")
        }
    }

    @Test
    fun `clearRecentSearches sets empty json array`() = runBlocking {
        coEvery { settingsService.setSetting("recent_searches_json", "[]") } returns
            com.os11.domain.result.AppResult.ok(Unit)

        searchService.clearRecentSearches()

        coVerify {
            settingsService.setSetting("recent_searches_json", "[]")
        }
    }
}
