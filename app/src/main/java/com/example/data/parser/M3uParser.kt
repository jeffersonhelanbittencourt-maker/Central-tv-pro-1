package com.example.data.parser

import com.example.data.model.ContentType
import com.example.data.model.MediaItemEntity
import java.io.BufferedReader
import java.io.StringReader
import java.net.URI
import java.util.UUID
import java.util.regex.Pattern

object M3uParser {

    private val TVG_ID_REGEX = Pattern.compile("""tvg-id="([^"]*)"""", Pattern.CASE_INSENSITIVE)
    private val TVG_NAME_REGEX = Pattern.compile("""tvg-name="([^"]*)"""", Pattern.CASE_INSENSITIVE)
    private val TVG_LOGO_REGEX = Pattern.compile("""tvg-logo="([^"]*)"""", Pattern.CASE_INSENSITIVE)
    private val GROUP_TITLE_REGEX = Pattern.compile("""group-title="([^"]*)"""", Pattern.CASE_INSENSITIVE)
    private val TVG_LANG_REGEX = Pattern.compile("""tvg-language="([^"]*)"""", Pattern.CASE_INSENSITIVE)
    private val SEASON_EPISODE_REGEX = Pattern.compile("""[sS](\d+)[eE](\d+)""", Pattern.CASE_INSENSITIVE)
    private val SEASON_EPISODE_PT_REGEX = Pattern.compile("""[tT](\d+)[eE](\d+)""", Pattern.CASE_INSENSITIVE)
    private val YEAR_REGEX = Pattern.compile("""\((\d{4})\)""")

    fun parse(
        content: String,
        sourceId: String,
        sourceUrl: String = ""
    ): List<MediaItemEntity> {
        val reader = BufferedReader(StringReader(content))
        val items = mutableListOf<MediaItemEntity>()

        var currentExtInf: String? = null
        var line: String? = reader.readLine()

        while (line != null) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                line = reader.readLine()
                continue
            }

            if (trimmed.startsWith("#EXTINF", ignoreCase = true)) {
                currentExtInf = trimmed
            } else if (!trimmed.startsWith("#") && currentExtInf != null) {
                // This is the stream URL line
                val resolvedUrl = resolveUrl(trimmed, sourceUrl)
                if (resolvedUrl.isNotBlank()) {
                    val mediaItem = parseItem(currentExtInf, resolvedUrl, sourceId)
                    items.add(mediaItem)
                }
                currentExtInf = null
            }

            line = reader.readLine()
        }

        return items
    }

    private fun parseItem(extInf: String, streamUrl: String, sourceId: String): MediaItemEntity {
        val tvgId = extractAttribute(TVG_ID_REGEX, extInf)
        val tvgName = extractAttribute(TVG_NAME_REGEX, extInf)
        val tvgLogo = extractAttribute(TVG_LOGO_REGEX, extInf)
        val tvgLang = extractAttribute(TVG_LANG_REGEX, extInf)
        val groupTitle = extractAttribute(GROUP_TITLE_REGEX, extInf) ?: "Geral"

        // Extract title after comma
        val commaIndex = extInf.lastIndexOf(',')
        val extractedTitle = if (commaIndex != -1 && commaIndex < extInf.length - 1) {
            extInf.substring(commaIndex + 1).trim()
        } else {
            tvgName ?: "Canal Sem Nome"
        }

        val displayTitle = if (extractedTitle.isNotBlank()) extractedTitle else tvgName ?: "Canal"

        // Detect Series season / episode
        var season: Int? = null
        var episode: Int? = null

        val seMatcher = SEASON_EPISODE_REGEX.matcher(displayTitle)
        if (seMatcher.find()) {
            season = seMatcher.group(1)?.toIntOrNull()
            episode = seMatcher.group(2)?.toIntOrNull()
        } else {
            val ptMatcher = SEASON_EPISODE_PT_REGEX.matcher(displayTitle)
            if (ptMatcher.find()) {
                season = ptMatcher.group(1)?.toIntOrNull()
                episode = ptMatcher.group(2)?.toIntOrNull()
            }
        }

        // Extract Year (e.g. "Night of the Living Dead (1968)")
        var year: Int? = null
        val yearMatcher = YEAR_REGEX.matcher(displayTitle)
        if (yearMatcher.find()) {
            year = yearMatcher.group(1)?.toIntOrNull()
        }

        // Detect stream format: MP4 vs HLS vs OUTRO
        val cleanUrl = streamUrl.split("?")[0].lowercase()
        val streamFormat = when {
            cleanUrl.endsWith(".mp4") || cleanUrl.endsWith(".m4v") -> "MP4"
            cleanUrl.endsWith(".m3u8") || cleanUrl.contains("/hls/") -> "HLS"
            cleanUrl.endsWith(".mpd") -> "DASH"
            else -> "OUTRO"
        }

        // Detect audio language & subtitles
        val titleLower = displayTitle.lowercase()
        val extInfLower = extInf.lowercase()
        val hasSubtitles = titleLower.contains("[leg") || titleLower.contains("(leg") ||
                titleLower.contains("sub") || extInfLower.contains("subtitles")

        val groupLower = groupTitle.lowercase()
        val audioLanguage = when {
            tvgLang?.isNotBlank() == true -> tvgLang
            titleLower.contains("[dub") || titleLower.contains("(dub") ||
                    titleLower.contains("dublado") || sourceId.contains("br") -> "pt-BR"
            titleLower.contains("anime") || titleLower.contains("jap") ||
                    groupLower.contains("anime") || groupLower.contains("jap") -> "ja"
            sourceId.contains("public") || sourceId.contains("orphaned") -> "en"
            else -> null
        }

        // Determine content type
        val contentType = detectContentType(groupTitle, displayTitle, streamUrl, season != null)

        val uniqueId = UUID.nameUUIDFromBytes("$sourceId:$streamUrl".toByteArray()).toString()
        val isOffline = displayTitle.contains("[offline]", ignoreCase = true) || extInf.contains("[offline]", ignoreCase = true)

        val licenseStatus = when {
            streamUrl.contains("archive.org") || streamUrl.contains("publicdomain") ||
                    streamUrl.contains("orphanedfilms") || streamUrl.contains("blender.org") ->
                "Domínio Público / Licença Aberta"
            sourceId.contains("iptv_br") || sourceId.contains("brasil") || streamUrl.contains("ebc.com.br") ||
                    streamUrl.contains("camara.leg.br") || streamUrl.contains("senado.leg.br") ->
                "Transmissão Aberta Oficial (Brasil)"
            else -> "Distribuição Web Aberta"
        }

        return MediaItemEntity(
            id = uniqueId,
            sourceId = sourceId,
            title = displayTitle,
            streamUrl = streamUrl,
            logoUrl = tvgLogo?.takeIf { it.isNotBlank() },
            groupTitle = sanitizeGroup(groupTitle),
            tvgId = tvgId?.takeIf { it.isNotBlank() },
            contentType = contentType.name,
            season = season,
            episode = episode,
            durationMs = null,
            isFeatured = false,
            isAvailable = !isOffline,
            year = year,
            streamFormat = streamFormat,
            audioLanguage = audioLanguage,
            hasSubtitles = hasSubtitles,
            licenseStatus = licenseStatus,
            lastVerified = System.currentTimeMillis()
        )
    }

    private fun detectContentType(
        group: String,
        title: String,
        url: String,
        hasSeasonEp: Boolean
    ): ContentType {
        if (hasSeasonEp) return ContentType.SERIES

        val groupLower = group.lowercase()
        val titleLower = title.lowercase()
        val urlLower = url.lowercase()

        // Check Series keywords
        if (groupLower.contains("série") || groupLower.contains("series") ||
            groupLower.contains("temporada") || groupLower.contains("novela")
        ) {
            return ContentType.SERIES
        }

        val cleanUrl = urlLower.split("?")[0]

        // Check Movie keywords
        if (groupLower.contains("filme") || groupLower.contains("movie") ||
            groupLower.contains("cine") || groupLower.contains("vod") ||
            groupLower.contains("longa") ||
            cleanUrl.endsWith(".mp4") || cleanUrl.endsWith(".mkv") ||
            cleanUrl.endsWith(".avi") || cleanUrl.endsWith(".m4v") || cleanUrl.endsWith(".webm")
        ) {
            return ContentType.MOVIE
        }

        // Default for IPTV is Live TV
        return ContentType.LIVE_TV
    }

    private fun sanitizeGroup(rawGroup: String): String {
        val trimmed = rawGroup.trim()
        if (trimmed.isEmpty() || trimmed.equals("undefined", ignoreCase = true)) {
            return "Geral"
        }
        // Clean up common IPTV group formatting e.g. [BR] Notícias or BR | ESPORTES
        val cleaned = trimmed.replace(Regex("""^\[?[A-Z]{2}\]?\s*[-|:]?\s*"""), "").trim()
            .ifEmpty { "Geral" }

        return when (cleaned.lowercase()) {
            "news" -> "Notícias"
            "sports", "sport" -> "Esportes"
            "kids", "children", "animation" -> "Infantil"
            "religious", "religion" -> "Religiosos"
            "movies", "movie", "cinema" -> "Filmes"
            "series", "tv series" -> "Séries"
            "documentary", "documentaries" -> "Documentários"
            "music" -> "Música"
            "general" -> "Geral"
            "entertainment" -> "Entretenimento"
            "education", "educational" -> "Educação"
            "comedy" -> "Comédia"
            "science" -> "Ciência"
            "culture" -> "Cultura"
            "legislative" -> "Notícias"
            else -> cleaned
        }
    }

    private fun extractAttribute(pattern: Pattern, text: String): String? {
        val matcher = pattern.matcher(text)
        return if (matcher.find()) {
            matcher.group(1)?.trim()
        } else null
    }

    private fun resolveUrl(rawUrl: String, baseUrl: String): String {
        return try {
            if (rawUrl.startsWith("http://", ignoreCase = true) ||
                rawUrl.startsWith("https://", ignoreCase = true)
            ) {
                rawUrl
            } else if (baseUrl.isNotBlank()) {
                URI.create(baseUrl).resolve(rawUrl).toString()
            } else {
                rawUrl
            }
        } catch (e: Exception) {
            rawUrl
        }
    }
}
