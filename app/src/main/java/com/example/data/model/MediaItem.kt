package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ContentType {
    LIVE_TV,
    MOVIE,
    SERIES;

    companion object {
        fun fromString(value: String): ContentType {
            return when (value.uppercase()) {
                "MOVIE", "FILME" -> MOVIE
                "SERIES", "SERIE" -> SERIES
                else -> LIVE_TV
            }
        }
    }
}

@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey
    val id: String,
    val sourceId: String,
    val title: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val groupTitle: String = "Geral",
    val tvgId: String? = null,
    val contentType: String = "LIVE_TV",
    val season: Int? = null,
    val episode: Int? = null,
    val durationMs: Long? = null,
    val isFeatured: Boolean = false,
    val isAvailable: Boolean = true,
    val year: Int? = null,
    val streamFormat: String = "HLS", // "MP4", "HLS", "OUTRO"
    val audioLanguage: String? = null, // "pt-BR", "en", "ja", etc.
    val hasSubtitles: Boolean = false,
    val licenseStatus: String = "Domínio Público / Licença Aberta",
    val lastVerified: Long = System.currentTimeMillis()
) {
    val type: ContentType
        get() = ContentType.fromString(contentType)

    val isMp4: Boolean
        get() = streamFormat.equals("MP4", ignoreCase = true) ||
                streamUrl.split("?")[0].lowercase().endsWith(".mp4")
}

@Entity(tableName = "playlist_sources")
data class PlaylistSourceEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val url: String,
    val itemCount: Int = 0,
    val lastUpdated: Long = 0L,
    val status: String = "PENDING", // "OK", "ERROR", "PENDING"
    val errorMessage: String? = null,
    val isPreloaded: Boolean = false
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val mediaId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey
    val mediaId: String,
    val title: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val groupTitle: String = "Geral",
    val contentType: String = "LIVE_TV",
    val lastPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val lastWatchedAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false
) {
    val progressFraction: Float
        get() = if (durationMs > 0) (lastPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}

data class SyncStats(
    val existingChannelsBeforeSync: Int,
    val addedChannels: Int,
    val updatedChannels: Int,
    val duplicateChannels: Int,
    val totalChannelsAfterSync: Int
)
