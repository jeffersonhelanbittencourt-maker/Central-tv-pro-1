package com.example.data.repository

import android.util.Log
import com.example.data.local.TvProDao
import com.example.data.model.ContentType
import com.example.data.model.FavoriteEntity
import com.example.data.model.MediaItemEntity
import com.example.data.model.PlaylistSourceEntity
import com.example.data.model.SyncStats
import com.example.data.model.WatchHistoryEntity
import com.example.data.parser.M3uParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID
import java.util.concurrent.TimeUnit

class TvRepository(private val dao: TvProDao) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    val allSources: Flow<List<PlaylistSourceEntity>> = dao.getAllSources()
    val featuredMedia: Flow<List<MediaItemEntity>> = dao.getFeaturedMedia()
    val allLiveTv: Flow<List<MediaItemEntity>> = dao.getMediaByType(ContentType.LIVE_TV.name)
    val allMovies: Flow<List<MediaItemEntity>> = dao.getMediaByType(ContentType.MOVIE.name)
    val allSeries: Flow<List<MediaItemEntity>> = dao.getMediaByType(ContentType.SERIES.name)
    val allFavorites: Flow<List<MediaItemEntity>> = dao.getAllFavorites()
    val watchHistory: Flow<List<WatchHistoryEntity>> = dao.getWatchHistory()
    val continueWatching: Flow<List<WatchHistoryEntity>> = dao.getContinueWatching()

    fun searchMedia(query: String): Flow<List<MediaItemEntity>> = dao.searchMedia(query)
    fun isFavorite(mediaId: String): Flow<Boolean> = dao.isFavorite(mediaId)
    fun getGroupsForType(type: ContentType): Flow<List<String>> = dao.getGroupsByType(type.name)
    fun getMediaByGroup(type: ContentType, group: String): Flow<List<MediaItemEntity>> =
        dao.getMediaByGroup(type.name, group)

    suspend fun getMediaById(id: String): MediaItemEntity? = dao.getMediaByIdOnce(id)
    suspend fun getAllMediaList(): List<MediaItemEntity> = dao.getAllMediaOnce()

    /**
     * Initializes and registers default legal public domain and Brazilian IPTV playlists.
     * Preserves existing sources and user data without overwriting or deleting records.
     */
    suspend fun initializeDefaultSourcesIfNeeded(): SyncStats? = withContext(Dispatchers.IO) {
        val existingSources = dao.getAllSourcesOnce()
        val defaultSources = listOf(
            PlaylistSourceEntity(
                id = "iptv-brasil-comunitaria",
                name = "IPTV Brasil — Lista comunitária",
                url = "https://iptv-org.github.io/iptv/countries/br.m3u",
                itemCount = 0,
                status = "PENDING",
                isPreloaded = true
            ),
            PlaylistSourceEntity(
                id = "source_curated_samples",
                name = "Central TV Pro — Amostras Selecionadas",
                url = "local://curated",
                itemCount = 0,
                status = "OK",
                isPreloaded = true
            ),
            PlaylistSourceEntity(
                id = "source_public_movies",
                name = "Filmes de Domínio Público",
                url = "https://raw.githubusercontent.com/OnlineM3U/publicdomainm3u/main/movies.m3u",
                itemCount = 0,
                status = "PENDING",
                isPreloaded = true
            ),
            PlaylistSourceEntity(
                id = "source_public_series",
                name = "Séries de Domínio Público",
                url = "https://raw.githubusercontent.com/OnlineM3U/publicdomainm3u/main/series.m3u",
                itemCount = 0,
                status = "PENDING",
                isPreloaded = true
            ),
            PlaylistSourceEntity(
                id = "source_pluto_br",
                name = "Pluto TV Brasil (Comunidade)",
                url = "https://raw.githubusercontent.com/BuddyChewChew/app-m3u-generator/main/playlists/plutotv_br.m3u",
                itemCount = 0,
                status = "PENDING",
                isPreloaded = true
            ),
            PlaylistSourceEntity(
                id = "source_orphaned_films",
                name = "Orphaned Films (Clássicos MP4)",
                url = "https://www.orphanedfilms.com/api/tv/playlist.m3u",
                itemCount = 0,
                status = "PENDING",
                isPreloaded = true
            )
        )

        val existingUrls = existingSources.map { it.url.trim().lowercase() }.toSet()
        val existingIds = existingSources.map { it.id }.toSet()

        // Insert missing sources while strictly preserving all existing sources
        for (src in defaultSources) {
            val urlNorm = src.url.trim().lowercase()
            if (!existingIds.contains(src.id)) {
                // If it's iptv-brasil-comunitaria, insert it even if another source had the URL
                if (src.id == "iptv-brasil-comunitaria" || !existingUrls.contains(urlNorm)) {
                    dao.insertSource(src)
                }
            }
        }

        // Seed initial curated channels only if catalog is completely empty
        val currentMedia = dao.getAllMediaOnce()
        if (currentMedia.isEmpty()) {
            seedCuratedSampleChannels()
        }

        // Query and synchronize the Brazilian IPTV community source
        val brSource = dao.getAllSourcesOnce().find {
            it.id == "iptv-brasil-comunitaria" || it.id == "source_iptv_br" ||
                    it.url.equals("https://iptv-org.github.io/iptv/countries/br.m3u", ignoreCase = true)
        }

        if (brSource != null) {
            val syncResult = syncSource(brSource)
            syncResult.getOrNull()
        } else {
            null
        }
    }

    private suspend fun seedCuratedSampleChannels() {
        val curated = listOf(
            MediaItemEntity(
                id = "curated_tv_brasil",
                sourceId = "source_curated_samples",
                title = "TV Brasil HD",
                streamUrl = "https://stream.ebc.com.br/hls/tvbrasil/tvbrasil.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/6f/TV_Brasil_logo_2023.svg/512px-TV_Brasil_logo_2023.svg.png",
                groupTitle = "Notícias",
                contentType = ContentType.LIVE_TV.name,
                isFeatured = true,
                isAvailable = true,
                streamFormat = "HLS",
                audioLanguage = "pt-BR",
                hasSubtitles = false,
                licenseStatus = "Transmissão Aberta Oficial (EBC)",
                lastVerified = System.currentTimeMillis()
            ),
            MediaItemEntity(
                id = "curated_camara",
                sourceId = "source_curated_samples",
                title = "TV Câmara",
                streamUrl = "https://videostreaming.camara.leg.br/live/tvcamara/playlist.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d4/Logo_TV_C%C3%A2mara.svg/512px-Logo_TV_C%C3%A2mara.svg.png",
                groupTitle = "Notícias",
                contentType = ContentType.LIVE_TV.name,
                isFeatured = false,
                isAvailable = true,
                streamFormat = "HLS",
                audioLanguage = "pt-BR",
                hasSubtitles = false,
                licenseStatus = "Transmissão Aberta Oficial (Câmara)",
                lastVerified = System.currentTimeMillis()
            ),
            MediaItemEntity(
                id = "curated_senado",
                sourceId = "source_curated_samples",
                title = "TV Senado",
                streamUrl = "https://stm2.senado.leg.br/tv/tvsenado/playlist.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/30/TV_Senado_logo.png/512px-TV_Senado_logo.png",
                groupTitle = "Notícias",
                contentType = ContentType.LIVE_TV.name,
                isFeatured = false,
                isAvailable = true,
                streamFormat = "HLS",
                audioLanguage = "pt-BR",
                hasSubtitles = false,
                licenseStatus = "Transmissão Aberta Oficial (Senado)",
                lastVerified = System.currentTimeMillis()
            ),
            MediaItemEntity(
                id = "curated_redbull",
                sourceId = "source_curated_samples",
                title = "Red Bull TV",
                streamUrl = "https://rbmn-live.akamaized.net/hls/live/590964/BoRB-AT/master.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ae/Red_Bull_TV_logo.svg/512px-Red_Bull_TV_logo.svg.png",
                groupTitle = "Esportes",
                contentType = ContentType.LIVE_TV.name,
                isFeatured = true,
                isAvailable = true,
                streamFormat = "HLS",
                audioLanguage = "en",
                hasSubtitles = false,
                licenseStatus = "Distribuição Web Oficial Gratuita",
                lastVerified = System.currentTimeMillis()
            ),
            MediaItemEntity(
                id = "curated_nasa",
                sourceId = "source_curated_samples",
                title = "NASA TV Public HD",
                streamUrl = "https://ntv1.akamaized.net/hls/live/2014075/NASA-NTV1-HLS/master.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e5/NASA_logo.svg/512px-NASA_logo.svg.png",
                groupTitle = "Documentários",
                contentType = ContentType.LIVE_TV.name,
                isFeatured = true,
                isAvailable = true,
                streamFormat = "HLS",
                audioLanguage = "en",
                hasSubtitles = false,
                licenseStatus = "Domínio Público (Governo dos EUA)",
                lastVerified = System.currentTimeMillis()
            ),
            MediaItemEntity(
                id = "curated_movie_bbb",
                sourceId = "source_curated_samples",
                title = "Big Buck Bunny (4K Open Movie)",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c5/Big_buck_bunny_poster_big.jpg/450px-Big_buck_bunny_poster_big.jpg",
                groupTitle = "Animação",
                contentType = ContentType.MOVIE.name,
                durationMs = 596000L,
                isFeatured = true,
                isAvailable = true,
                year = 2008,
                streamFormat = "MP4",
                audioLanguage = "en",
                hasSubtitles = false,
                licenseStatus = "Creative Commons CC-BY 3.0 (Blender Foundation)",
                lastVerified = System.currentTimeMillis()
            ),
            MediaItemEntity(
                id = "curated_movie_tos",
                sourceId = "source_curated_samples",
                title = "Tears of Steel (Sci-Fi Aberto)",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/eb/Tears_of_Steel_poster.jpg/450px-Tears_of_Steel_poster.jpg",
                groupTitle = "Ficção Científica",
                contentType = ContentType.MOVIE.name,
                durationMs = 734000L,
                isFeatured = true,
                isAvailable = true,
                year = 2012,
                streamFormat = "MP4",
                audioLanguage = "en",
                hasSubtitles = false,
                licenseStatus = "Creative Commons CC-BY 3.0 (Blender Foundation)",
                lastVerified = System.currentTimeMillis()
            ),
            MediaItemEntity(
                id = "curated_movie_sintel",
                sourceId = "source_curated_samples",
                title = "Sintel (Fantasia Aberta)",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8f/Sintel_poster.jpg/450px-Sintel_poster.jpg",
                groupTitle = "Fantasia",
                contentType = ContentType.MOVIE.name,
                durationMs = 888000L,
                isFeatured = true,
                isAvailable = true,
                year = 2010,
                streamFormat = "MP4",
                audioLanguage = "en",
                hasSubtitles = false,
                licenseStatus = "Creative Commons CC-BY 3.0 (Blender Foundation)",
                lastVerified = System.currentTimeMillis()
            ),
            MediaItemEntity(
                id = "curated_series_cosmos_1",
                sourceId = "source_curated_samples",
                title = "Cosmos: As Fronteiras do Universo - T01E01",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                logoUrl = "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=600&auto=format&fit=crop&q=80",
                groupTitle = "Séries & Documentários",
                contentType = ContentType.SERIES.name,
                season = 1,
                episode = 1,
                durationMs = 653000L,
                isFeatured = true,
                isAvailable = true,
                year = 2006,
                streamFormat = "MP4",
                audioLanguage = "en",
                hasSubtitles = false,
                licenseStatus = "Creative Commons CC-BY 2.5 (Orange Open Movie)",
                lastVerified = System.currentTimeMillis()
            ),
            MediaItemEntity(
                id = "curated_series_cosmos_2",
                sourceId = "source_curated_samples",
                title = "Cosmos: Segredos das Galáxias - T01E02",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                logoUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600&auto=format&fit=crop&q=80",
                groupTitle = "Séries & Documentários",
                contentType = ContentType.SERIES.name,
                season = 1,
                episode = 2,
                durationMs = 540000L,
                isFeatured = false,
                isAvailable = true,
                year = 2015,
                streamFormat = "MP4",
                audioLanguage = "en",
                hasSubtitles = false,
                licenseStatus = "Domínio Público / Licença Aberta",
                lastVerified = System.currentTimeMillis()
            )
        )

        dao.insertMediaItems(curated)
        dao.updateSourceStatus("source_curated_samples", "OK", curated.size, System.currentTimeMillis(), null)
    }

    suspend fun addCustomSource(name: String, url: String): Result<SyncStats> = withContext(Dispatchers.IO) {
        val id = "source_${UUID.randomUUID()}"
        val newSource = PlaylistSourceEntity(
            id = id,
            name = name.trim().ifEmpty { "Lista M3U Personalizada" },
            url = url.trim(),
            itemCount = 0,
            status = "PENDING",
            isPreloaded = false
        )
        dao.insertSource(newSource)
        syncSource(newSource)
    }

    suspend fun importRawM3uContent(name: String, content: String): SyncStats = withContext(Dispatchers.IO) {
        val id = "source_${UUID.randomUUID()}"
        val newSource = PlaylistSourceEntity(
            id = id,
            name = name.trim().ifEmpty { "Lista Importada" },
            url = "local://imported",
            itemCount = 0,
            status = "PENDING",
            isPreloaded = false
        )
        dao.insertSource(newSource)

        val allBefore = dao.getAllMediaOnce()
        val items = M3uParser.parse(content, id, "")

        val existingByUrl = allBefore.associateBy { it.streamUrl.trim().lowercase() }.toMutableMap()
        var added = 0
        var updated = 0
        var duplicates = 0
        val toInsert = mutableListOf<MediaItemEntity>()

        for (item in items) {
            val key = item.streamUrl.trim().lowercase()
            val existing = existingByUrl[key]
            if (existing != null) {
                if (existing.title != item.title || existing.logoUrl != item.logoUrl || existing.groupTitle != item.groupTitle) {
                    val upd = existing.copy(
                        title = item.title,
                        logoUrl = item.logoUrl ?: existing.logoUrl,
                        groupTitle = item.groupTitle
                    )
                    toInsert.add(upd)
                    existingByUrl[key] = upd
                    updated++
                } else {
                    duplicates++
                }
            } else {
                toInsert.add(item)
                existingByUrl[key] = item
                added++
            }
        }

        if (toInsert.isNotEmpty()) {
            dao.insertMediaItems(toInsert)
            val totalForSource = dao.getMediaBySourceOnce(id).size
            dao.updateSourceStatus(id, "OK", totalForSource, System.currentTimeMillis(), null)
        } else {
            dao.updateSourceStatus(id, "OK", 0, System.currentTimeMillis(), null)
        }

        val totalAfter = dao.getAllMediaOnce().size
        SyncStats(
            existingChannelsBeforeSync = allBefore.size,
            addedChannels = added,
            updatedChannels = updated,
            duplicateChannels = duplicates,
            totalChannelsAfterSync = totalAfter
        )
    }

    /**
     * Synchronizes an M3U playlist source:
     * - Preserves all existing channels, favorites, and watch history.
     * - Identifies duplicates by comparing stream addresses and identifiers (Rule 4).
     * - Retains valid alternative streams with distinct URLs instead of discarding them.
     * - Updates modified metadata without altering existing channel IDs.
     * - Keeps existing channels even if temporarily unavailable (Rule 7).
     * - In case of network/source failure, retains existing catalog intact (Rule 8).
     */
    suspend fun syncSource(source: PlaylistSourceEntity): Result<SyncStats> = withContext(Dispatchers.IO) {
        if (source.url.startsWith("local://")) {
            val count = dao.getMediaBySourceOnce(source.id).size
            val allCount = dao.getAllMediaOnce().size
            return@withContext Result.success(SyncStats(allCount, 0, 0, 0, allCount))
        }

        val allMediaBefore = dao.getAllMediaOnce()
        val existingTotalCount = allMediaBefore.size

        // Build lookup maps for deduplication and update tracking
        val existingByUrl = HashMap<String, MediaItemEntity>(allMediaBefore.size)
        val existingById = HashMap<String, MediaItemEntity>(allMediaBefore.size)

        for (m in allMediaBefore) {
            existingByUrl[m.streamUrl.trim().lowercase()] = m
            existingById[m.id] = m
        }

        try {
            val request = Request.Builder()
                .url(source.url)
                .header("User-Agent", "CentralTVPro/1.0 (Linux; Android TV; ExoPlayer)")
                .header("Accept", "*/*")
                .build()

            val response = httpClient.newCall(request).execute()
            val isBrSource = source.id == "iptv-brasil-comunitaria" || source.id == "source_iptv_br" || source.url.contains("countries/br.m3u")
            if (!response.isSuccessful) {
                val errorMsg = if (isBrSource) {
                    "Falha na conexão ao atualizar a lista brasileira. Os canais salvos continuam disponíveis."
                } else {
                    "Falha na conexão: o servidor retornou código HTTP ${response.code}"
                }
                val currentCount = dao.getMediaBySourceOnce(source.id).size
                dao.updateSourceStatus(source.id, "ERROR", currentCount, System.currentTimeMillis(), errorMsg)
                // Rule 8: Preserve existing data, do NOT clear catalog
                return@withContext Result.failure(Exception(errorMsg))
            }

            val body = response.body?.string() ?: ""
            if (body.isBlank()) {
                val errorMsg = if (isBrSource) {
                    "Falha na conexão ao atualizar a lista brasileira. Os canais salvos continuam disponíveis."
                } else {
                    "Lista de reprodução retornou resposta vazia."
                }
                val currentCount = dao.getMediaBySourceOnce(source.id).size
                dao.updateSourceStatus(source.id, "ERROR", currentCount, System.currentTimeMillis(), errorMsg)
                // Rule 8: Preserve existing data, do NOT clear catalog
                return@withContext Result.failure(Exception(errorMsg))
            }

            val parsedItems = M3uParser.parse(body, source.id, source.url)
            if (parsedItems.isEmpty()) {
                val errorMsg = if (isBrSource) {
                    "Falha na conexão ao atualizar a lista brasileira. Os canais salvos continuam disponíveis."
                } else {
                    "Nenhum canal válido encontrado na lista de reprodução."
                }
                val currentCount = dao.getMediaBySourceOnce(source.id).size
                dao.updateSourceStatus(source.id, "ERROR", currentCount, System.currentTimeMillis(), errorMsg)
                return@withContext Result.failure(Exception(errorMsg))
            }

            var addedCount = 0
            var updatedCount = 0
            var duplicateCount = 0

            val itemsToAdd = mutableListOf<MediaItemEntity>()
            val itemsToUpdate = mutableListOf<MediaItemEntity>()
            val seenUrlsInBatch = mutableSetOf<String>()

            for (newItem in parsedItems) {
                val normUrl = newItem.streamUrl.trim().lowercase()

                if (seenUrlsInBatch.contains(normUrl)) {
                    // Exact duplicate stream entry inside the same playlist
                    duplicateCount++
                    continue
                }
                seenUrlsInBatch.add(normUrl)

                val existingMatch = existingByUrl[normUrl] ?: existingById[newItem.id]

                if (existingMatch != null) {
                    // Exact same stream address already present in the catalog.
                    // Compare metadata to see if it was updated or is identical (duplicate)
                    val titleChanged = newItem.title.isNotBlank() && newItem.title != existingMatch.title
                    val logoChanged = !newItem.logoUrl.isNullOrBlank() && newItem.logoUrl != existingMatch.logoUrl
                    val groupChanged = newItem.groupTitle != "Geral" && newItem.groupTitle != existingMatch.groupTitle
                    val tvgIdChanged = !newItem.tvgId.isNullOrBlank() && newItem.tvgId != existingMatch.tvgId
                    val availChanged = newItem.isAvailable != existingMatch.isAvailable
                    val formatChanged = newItem.streamFormat != existingMatch.streamFormat
                    val yearChanged = newItem.year != null && newItem.year != existingMatch.year
                    val langChanged = newItem.audioLanguage != null && newItem.audioLanguage != existingMatch.audioLanguage

                    if (titleChanged || logoChanged || groupChanged || tvgIdChanged || availChanged || formatChanged || yearChanged || langChanged) {
                        // Channel updated: preserve existing ID so favorites, history and settings are preserved
                        val updated = existingMatch.copy(
                            title = if (titleChanged) newItem.title else existingMatch.title,
                            logoUrl = if (logoChanged) newItem.logoUrl else existingMatch.logoUrl,
                            groupTitle = if (groupChanged) newItem.groupTitle else existingMatch.groupTitle,
                            tvgId = if (tvgIdChanged) newItem.tvgId else existingMatch.tvgId,
                            contentType = newItem.contentType,
                            isAvailable = newItem.isAvailable,
                            year = newItem.year ?: existingMatch.year,
                            streamFormat = newItem.streamFormat,
                            audioLanguage = newItem.audioLanguage ?: existingMatch.audioLanguage,
                            hasSubtitles = newItem.hasSubtitles || existingMatch.hasSubtitles,
                            licenseStatus = newItem.licenseStatus,
                            lastVerified = System.currentTimeMillis()
                        )
                        itemsToUpdate.add(updated)
                        existingByUrl[normUrl] = updated
                        updatedCount++
                    } else {
                        // Already exists in catalog with identical stream and metadata
                        duplicateCount++
                    }
                } else {
                    // The stream address is distinct from existing items.
                    // Rule 4: Do not discard valid alternative transmissions for channels with the same or similar names.
                    itemsToAdd.add(newItem)
                    existingByUrl[normUrl] = newItem
                    existingById[newItem.id] = newItem
                    addedCount++
                }
            }

            // Persist new additions and updates WITHOUT deleting existing channels (Rule 1 & 7)
            if (itemsToAdd.isNotEmpty()) {
                dao.insertMediaItems(itemsToAdd)
            }
            if (itemsToUpdate.isNotEmpty()) {
                dao.insertMediaItems(itemsToUpdate)
            }

            val sourceCount = dao.getMediaBySourceOnce(source.id).size
            dao.updateSourceStatus(source.id, "OK", sourceCount, System.currentTimeMillis(), null)

            val totalAfter = dao.getAllMediaOnce().size
            val stats = SyncStats(
                existingChannelsBeforeSync = existingTotalCount,
                addedChannels = addedCount,
                updatedChannels = updatedCount,
                duplicateChannels = duplicateCount,
                totalChannelsAfterSync = totalAfter
            )
            Result.success(stats)
        } catch (e: Exception) {
            val isBrSource = source.id == "iptv-brasil-comunitaria" || source.id == "source_iptv_br" || source.url.contains("countries/br.m3u")
            val errorMsg = if (isBrSource) {
                "Falha na conexão ao atualizar a lista brasileira. Os canais salvos continuam disponíveis."
            } else {
                e.localizedMessage ?: "Falha na conexão ao atualizar a lista de reprodução."
            }
            val currentCount = dao.getMediaBySourceOnce(source.id).size
            dao.updateSourceStatus(source.id, "ERROR", currentCount, System.currentTimeMillis(), errorMsg)
            Result.failure(Exception(errorMsg, e))
        }
    }

    suspend fun syncAllSources(): List<SyncStats> = withContext(Dispatchers.IO) {
        val sources = dao.getAllSourcesOnce()
        val statsList = mutableListOf<SyncStats>()
        for (src in sources) {
            if (!src.url.startsWith("local://")) {
                syncSource(src).getOrNull()?.let { statsList.add(it) }
            }
        }
        statsList
    }

    suspend fun deleteSource(sourceId: String) = withContext(Dispatchers.IO) {
        dao.deleteMediaBySource(sourceId)
        dao.deleteSource(sourceId)
    }

    suspend fun toggleFavorite(mediaId: String, isFav: Boolean) = withContext(Dispatchers.IO) {
        if (isFav) {
            dao.removeFavorite(mediaId)
        } else {
            dao.addFavorite(FavoriteEntity(mediaId = mediaId))
        }
    }

    suspend fun savePlaybackProgress(
        item: MediaItemEntity,
        positionMs: Long,
        durationMs: Long
    ) = withContext(Dispatchers.IO) {
        if (item.type == ContentType.LIVE_TV) {
            dao.upsertHistory(
                WatchHistoryEntity(
                    mediaId = item.id,
                    title = item.title,
                    streamUrl = item.streamUrl,
                    logoUrl = item.logoUrl,
                    groupTitle = item.groupTitle,
                    contentType = item.contentType,
                    lastPositionMs = 0L,
                    durationMs = 0L,
                    lastWatchedAt = System.currentTimeMillis(),
                    isCompleted = false
                )
            )
        } else {
            val completed = durationMs > 0 && positionMs >= (durationMs * 0.95)
            dao.upsertHistory(
                WatchHistoryEntity(
                    mediaId = item.id,
                    title = item.title,
                    streamUrl = item.streamUrl,
                    logoUrl = item.logoUrl,
                    groupTitle = item.groupTitle,
                    contentType = item.contentType,
                    lastPositionMs = positionMs,
                    durationMs = durationMs,
                    lastWatchedAt = System.currentTimeMillis(),
                    isCompleted = completed
                )
            )
        }
    }

    suspend fun getWatchHistoryItem(mediaId: String): WatchHistoryEntity? = withContext(Dispatchers.IO) {
        dao.getHistoryItem(mediaId)
    }

    suspend fun deleteHistoryItem(mediaId: String) = withContext(Dispatchers.IO) {
        dao.deleteHistoryItem(mediaId)
    }

    suspend fun clearAllHistory() = withContext(Dispatchers.IO) {
        dao.clearHistory()
    }
}
