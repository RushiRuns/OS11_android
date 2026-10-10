package com.os11.app.data

import android.content.Context
import com.os11.app.data.db.OS11Database
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Test

class RoomInitTest {

    @Test
    fun testRoomInitialization() {
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        val db = OS11Database.buildInMemory(context)
        assertNotNull(db)
        db.close()
    }
}
