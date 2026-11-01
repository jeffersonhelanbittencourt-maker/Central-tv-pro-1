package com.example

import com.example.data.model.ContentType
import com.example.data.model.MediaItemEntity
import com.example.data.parser.M3uParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ExampleUnitTest {

    @Test
    fun testM3uParserLiveChannel() {
        val sampleM3u = """
            #EXTM3U
            #EXTINF:-1 tvg-id="tvbrasil.br" tvg-name="TV Brasil" tvg-logo="https://exemplo.com/logo.png" group-title="Notícias",TV Brasil HD
            https://stream.ebc.com.br/hls/tvbrasil/tvbrasil.m3u8
        """.trimIndent()

        val items = M3uParser.parse(sampleM3u, "source_test", "https://exemplo.com/playlist.m3u")
        assertEquals(1, items.size)

        val channel = items[0]
        assertEquals("TV Brasil HD", channel.title)
        assertEquals("https://stream.ebc.com.br/hls/tvbrasil/tvbrasil.m3u8", channel.streamUrl)
        assertEquals("https://exemplo.com/logo.png", channel.logoUrl)
        assertEquals("Notícias", channel.groupTitle)
        assertEquals(ContentType.LIVE_TV, channel.type)
        assertTrue(channel.isAvailable)
    }

    @Test
    fun testM3uParserMovieAndSeriesDetection() {
        val sampleM3u = """
            #EXTM3U
            #EXTINF:-1 tvg-logo="https://exemplo.com/bbb.jpg" group-title="Cine Filmes",Big Buck Bunny
            https://commondatastorage.googleapis.com/sample/BigBuckBunny.mp4
            #EXTINF:-1 group-title="Séries Clássicas",Cosmos S01E02 - Segredos
            https://commondatastorage.googleapis.com/sample/Cosmos_S01E02.mp4
        """.trimIndent()

        val items = M3uParser.parse(sampleM3u, "source_test", "")
        assertEquals(2, items.size)

        val movie = items[0]
        assertEquals(ContentType.MOVIE, movie.type)
        assertEquals("Big Buck Bunny", movie.title)

        val series = items[1]
        assertEquals(ContentType.SERIES, series.type)
        assertEquals(1, series.season)
        assertEquals(2, series.episode)
    }

    @Test
    fun testDeduplicationAndAlternativeStreamsLogic() {
        // 1 existing channel in database
        val existingChannel = MediaItemEntity(
            id = "c1",
            sourceId = "source_curated",
            title = "TV Brasil HD",
            streamUrl = "https://stream.ebc.com.br/hls/tvbrasil/tvbrasil.m3u8",
            logoUrl = "https://exemplo.com/old_logo.png",
            groupTitle = "Notícias",
            contentType = ContentType.LIVE_TV.name
        )

        val existingList = listOf(existingChannel)
        val existingByUrl = existingList.associateBy { it.streamUrl.trim().lowercase() }.toMutableMap()

        // Incoming M3U with:
        // 1. Same streamUrl but updated logo & group (should be updated)
        // 2. Same channel name "TV Brasil HD" but DIFFERENT streamUrl (should be added as valid alternative stream)
        // 3. New channel "TV Cultura" (should be added)
        // 4. Repeated streamUrl in batch (should be identified as duplicate)
        val sampleM3u = """
            #EXTM3U
            #EXTINF:-1 tvg-logo="https://exemplo.com/new_logo.png" group-title="Geral",TV Brasil HD
            https://stream.ebc.com.br/hls/tvbrasil/tvbrasil.m3u8
            #EXTINF:-1 tvg-logo="https://exemplo.com/alt_logo.png" group-title="Notícias",TV Brasil HD (Mirror 2)
            https://stream.ebc.com.br/hls/tvbrasil/backup.m3u8
            #EXTINF:-1 tvg-logo="https://exemplo.com/cultura.png" group-title="Cultura",TV Cultura
            https://cultura.stream/hls/live.m3u8
            #EXTINF:-1 tvg-logo="https://exemplo.com/cultura.png" group-title="Cultura",TV Cultura
            https://cultura.stream/hls/live.m3u8
        """.trimIndent()

        val parsed = M3uParser.parse(sampleM3u, "source_iptv_br", "")
        assertEquals(4, parsed.size)

        var added = 0
        var updated = 0
        var duplicates = 0
        val seenBatch = mutableSetOf<String>()

        for (item in parsed) {
            val key = item.streamUrl.trim().lowercase()
            if (seenBatch.contains(key)) {
                duplicates++
                continue
            }
            seenBatch.add(key)

            val match = existingByUrl[key]
            if (match != null) {
                val hasChanged = match.title != item.title ||
                        (item.logoUrl != null && match.logoUrl != item.logoUrl) ||
                        (item.groupTitle != "Geral" && match.groupTitle != item.groupTitle)
                if (hasChanged) {
                    updated++
                } else {
                    duplicates++
                }
            } else {
                added++
            }
        }

        assertEquals(1, updated) // TV Brasil HD stream updated with new logo
        assertEquals(2, added) // TV Brasil HD (Mirror 2 alternative stream) + TV Cultura
        assertEquals(1, duplicates) // 1 in-batch duplicate of TV Cultura
    }

    @Test
    fun testMp4AndMetadataExtraction() {
        val sampleM3u = """
            #EXTM3U
            #EXTINF:-1 tvg-id="night.living.dead" group-title="Terror",Night of the Living Dead (1968) [DUBLADO]
            https://archive.org/download/night_of_the_living_dead/night_of_the_living_dead_512kb.mp4?token=abc
            #EXTINF:-1 tvg-id="nosferatu" tvg-language="de" group-title="Clássicos",Nosferatu (1922) [LEG]
            https://archive.org/download/nosferatu_1922/nosferatu.mp4
            #EXTINF:-1 group-title="Anime Clássico",Astro Boy (1963)
            https://commondatastorage.googleapis.com/sample/anime.mp4
        """.trimIndent()

        val items = M3uParser.parse(sampleM3u, "source_orphaned_films", "")
        assertEquals(3, items.size)

        val film1 = items[0]
        assertEquals("Night of the Living Dead (1968) [DUBLADO]", film1.title)
        assertEquals(1968, film1.year)
        assertEquals("MP4", film1.streamFormat)
        assertTrue(film1.isMp4)
        assertEquals("pt-BR", film1.audioLanguage)
        assertEquals(ContentType.MOVIE, film1.type)
        assertEquals("Domínio Público / Licença Aberta", film1.licenseStatus)

        val film2 = items[1]
        assertEquals(1922, film2.year)
        assertEquals("MP4", film2.streamFormat)
        assertTrue(film2.isMp4)
        assertTrue(film2.hasSubtitles)
        assertEquals("de", film2.audioLanguage)

        val film3 = items[2]
        assertEquals(1963, film3.year)
        assertEquals("ja", film3.audioLanguage)
    }
}
