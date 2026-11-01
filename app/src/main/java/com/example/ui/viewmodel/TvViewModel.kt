package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.TvProApplication
import com.example.data.model.ContentType
import com.example.data.model.MediaItemEntity
import com.example.data.model.PlaylistSourceEntity
import com.example.data.model.SyncStats
import com.example.data.model.WatchHistoryEntity
import com.example.data.repository.TvRepository
import com.example.player.TvPlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScreenDestination {
    HOME,
    LIVE_TV,
    MOVIES,
    SERIES,
    FAVORITES,
    HISTORY,
    SEARCH,
    PLAYLISTS,
    SETTINGS,
    PLAYER
}

class TvViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TvRepository = (application as TvProApplication).repository
    val playerManager = TvPlayerManager(application)

    // Navigation Stack
    private val _backStack = MutableStateFlow<List<ScreenDestination>>(listOf(ScreenDestination.HOME))
    val currentScreen: StateFlow<ScreenDestination> = _backStack
        .combine(MutableStateFlow(Unit)) { stack, _ -> stack.lastOrNull() ?: ScreenDestination.HOME }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ScreenDestination.HOME)

    // Active playing item
    private val _activeMediaItem = MutableStateFlow<MediaItemEntity?>(null)
    val activeMediaItem: StateFlow<MediaItemEntity?> = _activeMediaItem.asStateFlow()

    // TV mode preference (auto or force TV layout)
    private val _isTvLayoutMode = MutableStateFlow(false)
    val isTvLayoutMode: StateFlow<Boolean> = _isTvLayoutMode.asStateFlow()

    // Virtual remote overlay visible
    private val _showVirtualRemote = MutableStateFlow(false)
    val showVirtualRemote: StateFlow<Boolean> = _showVirtualRemote.asStateFlow()

    // Syncing state
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncStatusMessage = MutableStateFlow<String?>(null)
    val syncStatusMessage: StateFlow<String?> = _syncStatusMessage.asStateFlow()

    private val _lastSyncStats = MutableStateFlow<SyncStats?>(null)
    val lastSyncStats: StateFlow<SyncStats?> = _lastSyncStats.asStateFlow()

    // Search query
    val searchQuery = MutableStateFlow("")
    val searchResults: StateFlow<List<MediaItemEntity>> = searchQuery
        .debounce(250)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.featuredMedia
            } else {
                repository.searchMedia(query.trim())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Category filters
    val selectedLiveCategory = MutableStateFlow("Todos")
    val selectedMovieCategory = MutableStateFlow("Todos")
    val selectedSeriesCategory = MutableStateFlow("Todos")

    // Database flows
    val allSources: StateFlow<List<PlaylistSourceEntity>> = repository.allSources
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val featuredMedia: StateFlow<List<MediaItemEntity>> = repository.featuredMedia
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLiveTv: StateFlow<List<MediaItemEntity>> = repository.allLiveTv
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMovies: StateFlow<List<MediaItemEntity>> = repository.allMovies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSeries: StateFlow<List<MediaItemEntity>> = repository.allSeries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFavorites: StateFlow<List<MediaItemEntity>> = repository.allFavorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchHistory: StateFlow<List<WatchHistoryEntity>> = repository.watchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val continueWatching: StateFlow<List<WatchHistoryEntity>> = repository.continueWatching
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val liveCategories: StateFlow<List<String>> = repository.getGroupsForType(ContentType.LIVE_TV)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val movieCategories: StateFlow<List<String>> = repository.getGroupsForType(ContentType.MOVIE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val seriesCategories: StateFlow<List<String>> = repository.getGroupsForType(ContentType.SERIES)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatusMessage.value = "Consultando fontes e TV brasileira..."
            val stats = repository.initializeDefaultSourcesIfNeeded()
            if (stats != null) {
                _lastSyncStats.value = stats
                _syncStatusMessage.value = "TV Brasil (IPTV-org): +${stats.addedChannels} adicionados, ${stats.updatedChannels} atualizados, ${stats.duplicateChannels} duplicados (Total: ${stats.totalChannelsAfterSync})"
            } else {
                _syncStatusMessage.value = "Catálogo inicial pronto."
            }
            _isSyncing.value = false
        }
    }

    // Navigation
    fun navigateTo(destination: ScreenDestination) {
        val current = _backStack.value
        if (current.lastOrNull() == destination) return
        _backStack.value = current + destination
    }

    fun navigateBack(): Boolean {
        val current = _backStack.value
        return if (current.size > 1) {
            val destinationPopped = current.last()
            if (destinationPopped == ScreenDestination.PLAYER) {
                saveCurrentProgress()
            }
            _backStack.value = current.dropLast(1)
            true
        } else {
            false
        }
    }

    fun playMedia(item: MediaItemEntity, startPosMs: Long = 0L) {
        _activeMediaItem.value = item
        navigateTo(ScreenDestination.PLAYER)
        playerManager.playMedia(item, startPosMs)
    }

    fun playFromHistory(history: WatchHistoryEntity) {
        viewModelScope.launch {
            val existingItem = repository.getMediaById(history.mediaId) ?: MediaItemEntity(
                id = history.mediaId,
                sourceId = "history",
                title = history.title,
                streamUrl = history.streamUrl,
                logoUrl = history.logoUrl,
                groupTitle = history.groupTitle,
                contentType = history.contentType
            )
            playMedia(existingItem, history.lastPositionMs)
        }
    }

    fun nextChannel() {
        val current = _activeMediaItem.value ?: return
        if (current.type == ContentType.LIVE_TV) {
            val list = allLiveTv.value
            val idx = list.indexOfFirst { it.id == current.id }
            if (idx != -1 && list.isNotEmpty()) {
                val nextIdx = (idx + 1) % list.size
                playMedia(list[nextIdx])
            }
        }
    }

    fun previousChannel() {
        val current = _activeMediaItem.value ?: return
        if (current.type == ContentType.LIVE_TV) {
            val list = allLiveTv.value
            val idx = list.indexOfFirst { it.id == current.id }
            if (idx != -1 && list.isNotEmpty()) {
                val prevIdx = if (idx - 1 < 0) list.size - 1 else idx - 1
                playMedia(list[prevIdx])
            }
        }
    }

    fun toggleFavorite(item: MediaItemEntity) {
        viewModelScope.launch {
            val isFav = allFavorites.value.any { it.id == item.id }
            repository.toggleFavorite(item.id, isFav)
        }
    }

    fun saveCurrentProgress() {
        val item = _activeMediaItem.value ?: return
        val pos = playerManager.currentPosition.value
        val dur = playerManager.duration.value
        viewModelScope.launch {
            repository.savePlaybackProgress(item, pos, dur)
        }
    }

    fun deleteHistoryItem(mediaId: String) {
        viewModelScope.launch {
            repository.deleteHistoryItem(mediaId)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }

    fun syncAllSources() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatusMessage.value = "Atualizando canais..."
            try {
                val statsList = repository.syncAllSources()
                val totalAdded = statsList.sumOf { it.addedChannels }
                val totalUpdated = statsList.sumOf { it.updatedChannels }
                val totalCatalog = statsList.lastOrNull()?.totalChannelsAfterSync ?: 0
                if (totalAdded == 0 && totalUpdated == 0) {
                    _syncStatusMessage.value = "Nenhuma alteração encontrada na lista."
                } else {
                    _syncStatusMessage.value = "Canais atualizados com sucesso: $totalCatalog canais encontrados."
                }
            } catch (e: Exception) {
                _syncStatusMessage.value = "Falha na conexão: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun syncSingleSource(source: PlaylistSourceEntity) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatusMessage.value = "Atualizando canais..."
            val result = repository.syncSource(source)
            result.onSuccess { stats ->
                _lastSyncStats.value = stats
                if (stats.addedChannels == 0 && stats.updatedChannels == 0) {
                    _syncStatusMessage.value = "Nenhuma alteração encontrada na lista."
                } else {
                    _syncStatusMessage.value = "Canais atualizados com sucesso: ${stats.totalChannelsAfterSync} canais encontrados."
                }
            }.onFailure { err ->
                _syncStatusMessage.value = err.message ?: "Falha na conexão ao atualizar a lista."
            }
            _isSyncing.value = false
        }
    }

    fun addCustomSource(name: String, url: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatusMessage.value = "Importando lista de reprodução..."
            val result = repository.addCustomSource(name, url)
            result.onSuccess { stats ->
                _lastSyncStats.value = stats
                _syncStatusMessage.value = "Canais atualizados com sucesso: ${stats.totalChannelsAfterSync} canais encontrados."
            }.onFailure { err ->
                _syncStatusMessage.value = "Erro ao adicionar $name: ${err.message}"
            }
            _isSyncing.value = false
        }
    }

    fun importRawM3u(name: String, content: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatusMessage.value = "Importando lista de reprodução..."
            val stats = repository.importRawM3uContent(name, content)
            _lastSyncStats.value = stats
            _syncStatusMessage.value = "Canais atualizados com sucesso: ${stats.totalChannelsAfterSync} canais encontrados."
            _isSyncing.value = false
        }
    }

    fun deleteSource(sourceId: String) {
        viewModelScope.launch {
            repository.deleteSource(sourceId)
        }
    }

    fun toggleTvLayoutMode() {
        _isTvLayoutMode.value = !_isTvLayoutMode.value
    }

    fun setTvLayoutMode(enabled: Boolean) {
        _isTvLayoutMode.value = enabled
    }

    fun toggleVirtualRemote() {
        _showVirtualRemote.value = !_showVirtualRemote.value
    }

    fun dismissVirtualRemote() {
        _showVirtualRemote.value = false
    }

    override fun onCleared() {
        super.onCleared()
        saveCurrentProgress()
        playerManager.release()
    }
}
