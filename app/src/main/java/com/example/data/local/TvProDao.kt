package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.FavoriteEntity
import com.example.data.model.MediaItemEntity
import com.example.data.model.PlaylistSourceEntity
import com.example.data.model.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TvProDao {

    // --- Media Items ---
    @Query("SELECT * FROM media_items ORDER BY isAvailable DESC, title ASC")
    fun getAllMedia(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items")
    suspend fun getAllMediaOnce(): List<MediaItemEntity>

    @Query("SELECT * FROM media_items WHERE sourceId = :sourceId ORDER BY title ASC")
    suspend fun getMediaBySourceOnce(sourceId: String): List<MediaItemEntity>

    @Query("SELECT * FROM media_items WHERE streamUrl = :streamUrl LIMIT 1")
    suspend fun getMediaByStreamUrl(streamUrl: String): MediaItemEntity?

    @Query("SELECT * FROM media_items WHERE contentType = :type ORDER BY isAvailable DESC, title ASC")
    fun getMediaByType(type: String): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE contentType = :type AND groupTitle = :group ORDER BY isAvailable DESC, title ASC")
    fun getMediaByGroup(type: String, group: String): Flow<List<MediaItemEntity>>

    @Query("SELECT DISTINCT groupTitle FROM media_items WHERE contentType = :type ORDER BY groupTitle ASC")
    fun getGroupsByType(type: String): Flow<List<String>>

    @Query("SELECT * FROM media_items WHERE title LIKE '%' || :query || '%' OR groupTitle LIKE '%' || :query || '%' ORDER BY isAvailable DESC, title ASC")
    fun searchMedia(query: String): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE id = :id LIMIT 1")
    fun getMediaById(id: String): Flow<MediaItemEntity?>

    @Query("SELECT * FROM media_items WHERE id = :id LIMIT 1")
    suspend fun getMediaByIdOnce(id: String): MediaItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaItems(items: List<MediaItemEntity>)

    @Query("DELETE FROM media_items WHERE sourceId = :sourceId")
    suspend fun deleteMediaBySource(sourceId: String)

    @Query("DELETE FROM media_items")
    suspend fun clearAllMedia()

    @Query("SELECT * FROM media_items WHERE isFeatured = 1 LIMIT 20")
    fun getFeaturedMedia(): Flow<List<MediaItemEntity>>

    // --- Playlists ---
    @Query("SELECT * FROM playlist_sources ORDER BY isPreloaded DESC, name ASC")
    fun getAllSources(): Flow<List<PlaylistSourceEntity>>

    @Query("SELECT * FROM playlist_sources")
    suspend fun getAllSourcesOnce(): List<PlaylistSourceEntity>

    @Query("SELECT * FROM playlist_sources WHERE id = :id LIMIT 1")
    suspend fun getSourceById(id: String): PlaylistSourceEntity?

    @Query("SELECT * FROM playlist_sources WHERE url = :url LIMIT 1")
    suspend fun getSourceByUrl(url: String): PlaylistSourceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: PlaylistSourceEntity)

    @Query("UPDATE playlist_sources SET status = :status, itemCount = :count, lastUpdated = :timestamp, errorMessage = :error WHERE id = :id")
    suspend fun updateSourceStatus(id: String, status: String, count: Int, timestamp: Long, error: String?)

    @Query("DELETE FROM playlist_sources WHERE id = :id")
    suspend fun deleteSource(id: String)

    // --- Favorites ---
    @Query("""
        SELECT m.* FROM media_items m
        INNER JOIN favorites f ON m.id = f.mediaId
        ORDER BY f.addedAt DESC
    """)
    fun getAllFavorites(): Flow<List<MediaItemEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE mediaId = :mediaId)")
    fun isFavorite(mediaId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE mediaId = :mediaId")
    suspend fun removeFavorite(mediaId: String)

    // --- Watch History ---
    @Query("SELECT * FROM watch_history ORDER BY lastWatchedAt DESC")
    fun getWatchHistory(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE isCompleted = 0 AND lastPositionMs > 10000 ORDER BY lastWatchedAt DESC LIMIT 15")
    fun getContinueWatching(): Flow<List<WatchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(history: WatchHistoryEntity)

    @Query("SELECT * FROM watch_history WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getHistoryItem(mediaId: String): WatchHistoryEntity?

    @Query("DELETE FROM watch_history WHERE mediaId = :mediaId")
    suspend fun deleteHistoryItem(mediaId: String)

    @Query("DELETE FROM watch_history")
    suspend fun clearHistory()
}
