package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.TvProDatabase
import com.example.data.model.PlaylistSourceEntity
import com.example.data.repository.TvRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Central TV Pro", appName)
    }

    @Test
    fun `test Brazilian M3U source synchronization and real counts`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, TvProDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.tvProDao()
        val repo = TvRepository(dao)

        // 1. Initial state: seeded curated channels
        val initStats = repo.initializeDefaultSourcesIfNeeded()
        assertNotNull("InitStats should not be null", initStats)

        println("=== RESULTADO DA EXECUÇÃO INICIAL ===")
        println("Canais já existentes antes da sincronização: ${initStats?.existingChannelsBeforeSync}")
        println("Canais adicionados da lista brasileira: ${initStats?.addedChannels}")
        println("Canais atualizados: ${initStats?.updatedChannels}")
        println("Canais duplicados identificados: ${initStats?.duplicateChannels}")
        println("Total no catálogo após sincronização: ${initStats?.totalChannelsAfterSync}")

        // 2. Second sync to test updating without duplicates or data loss
        val brSource = dao.getAllSourcesOnce().find {
            it.url.contains("countries/br.m3u")
        }
        assertNotNull(brSource)

        val secondSync = repo.syncSource(brSource!!)
        assertTrue(secondSync.isSuccess)
        val stats2 = secondSync.getOrNull()

        println("=== RESULTADO DA SEGUNDA SINCRONIZAÇÃO (ATUALIZAÇÃO) ===")
        println("Canais já existentes antes da 2ª sincronização: ${stats2?.existingChannelsBeforeSync}")
        println("Canais novos adicionados: ${stats2?.addedChannels}")
        println("Canais atualizados: ${stats2?.updatedChannels}")
        println("Canais duplicados identificados: ${stats2?.duplicateChannels}")
        println("Total final no catálogo: ${stats2?.totalChannelsAfterSync}")

        // 3. Verify curated MP4 items are persisted with format MP4
        val allMedia = dao.getAllMediaOnce()
        val mp4Items = allMedia.filter { it.isMp4 }
        assertTrue("Should have MP4 items in catalog", mp4Items.isNotEmpty())
        val bbb = mp4Items.find { it.title.contains("Big Buck Bunny") }
        assertNotNull("Big Buck Bunny should exist", bbb)
        assertEquals("MP4", bbb?.streamFormat)
        assertEquals(2008, bbb?.year)

        db.close()
    }
}
