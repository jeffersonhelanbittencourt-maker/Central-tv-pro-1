package com.example

import android.content.res.Configuration
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.VirtualRemoteDialog
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LiveTvScreen
import com.example.ui.screens.MoviesScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SeriesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CinemaBlack
import com.example.ui.theme.CinemaNavy
import com.example.ui.theme.CinemaSlate
import com.example.ui.theme.LiveBadgeRed
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ScreenDestination
import com.example.ui.viewmodel.TvViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: TvViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Check if device is television
        val isUiModeTv = (resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION
        if (isUiModeTv) {
            viewModel.setTvLayoutMode(true)
        }

        setContent {
            MyApplicationTheme {
                MainContent(viewModel = viewModel)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val current = viewModel.currentScreen.value
        if (current == ScreenDestination.PLAYER) {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    viewModel.playerManager.togglePlayPause()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PLAY -> {
                    viewModel.playerManager.getPlayer().play()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                    viewModel.playerManager.getPlayer().pause()
                    return true
                }
                KeyEvent.KEYCODE_CHANNEL_UP -> {
                    viewModel.nextChannel()
                    return true
                }
                KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                    viewModel.previousChannel()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (!viewModel.playerManager.isLive.value) {
                        viewModel.playerManager.seekBackward(10000L)
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (!viewModel.playerManager.isLive.value) {
                        viewModel.playerManager.seekForward(10000L)
                        return true
                    }
                }
                KeyEvent.KEYCODE_BACK -> {
                    viewModel.saveCurrentProgress()
                    if (viewModel.navigateBack()) return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}

data class NavItem(
    val destination: ScreenDestination,
    val title: String,
    val icon: ImageVector,
    val isLiveBadge: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(viewModel: TvViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val isTvLayout by viewModel.isTvLayoutMode.collectAsStateWithLifecycle()
    val showRemote by viewModel.showVirtualRemote.collectAsStateWithLifecycle()
    val isPlaying by viewModel.playerManager.isPlaying.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600 || isTvLayout

    val navItems = listOf(
        NavItem(ScreenDestination.HOME, "Início", Icons.Default.Home),
        NavItem(ScreenDestination.LIVE_TV, "TV ao vivo", Icons.Default.LiveTv, isLiveBadge = true),
        NavItem(ScreenDestination.MOVIES, "Filmes", Icons.Default.Movie),
        NavItem(ScreenDestination.SERIES, "Séries", Icons.Default.Tv),
        NavItem(ScreenDestination.FAVORITES, "Favoritos", Icons.Default.Favorite),
        NavItem(ScreenDestination.PLAYLISTS, "Lista de reprodução", Icons.Default.PlaylistPlay),
        NavItem(ScreenDestination.SETTINGS, "Configurações", Icons.Default.Settings)
    )

    BackHandler(enabled = currentScreen != ScreenDestination.HOME) {
        viewModel.navigateBack()
    }

    // If active player is shown, display Player full bleed
    if (currentScreen == ScreenDestination.PLAYER) {
        PlayerScreen(viewModel = viewModel)
    } else {
        if (isWideScreen) {
            // TV / Tablet / Landscape Layout with NavigationRail
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CinemaBlack)
            ) {
                NavigationRail(
                    containerColor = CinemaNavy,
                    contentColor = TextPrimary,
                    header = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NeonCyan),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tv,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "TV PRO",
                                color = NeonCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    },
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    navItems.forEach { item ->
                        val isSelected = currentScreen == item.destination
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = { viewModel.navigateTo(item.destination) },
                            icon = {
                                if (item.isLiveBadge) {
                                    BadgedBox(
                                        badge = {
                                            Badge(
                                                containerColor = LiveBadgeRed,
                                                modifier = Modifier.size(6.dp)
                                            )
                                        }
                                    ) {
                                        Icon(item.icon, contentDescription = item.title)
                                    }
                                } else {
                                    Icon(item.icon, contentDescription = item.title)
                                }
                            },
                            label = { Text(item.title, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = NeonCyan,
                                indicatorColor = NeonCyan,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("nav_rail_${item.destination.name.lowercase()}")
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(
                        onClick = { viewModel.toggleVirtualRemote() },
                        modifier = Modifier.padding(bottom = 12.dp).testTag("rail_remote_btn")
                    ) {
                        Icon(Icons.Default.SettingsRemote, contentDescription = "Controle Remoto", tint = NeonAmber)
                    }
                }

                // Main Content Pane
                Box(modifier = Modifier.fillMaxSize()) {
                    ScreenContent(currentScreen = currentScreen, viewModel = viewModel)
                }
            }
        } else {
            // Mobile Vertical Layout with Scaffold & NavigationBar
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(NeonCyan),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Tv, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Central TV Pro",
                                    color = TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { viewModel.navigateTo(ScreenDestination.SEARCH) },
                                modifier = Modifier.testTag("top_search_btn")
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Pesquisar", tint = TextPrimary)
                            }
                            IconButton(
                                onClick = { viewModel.navigateTo(ScreenDestination.PLAYLISTS) },
                                modifier = Modifier.testTag("top_playlists_btn")
                            ) {
                                Icon(Icons.Default.PlaylistPlay, contentDescription = "Lista de reprodução", tint = TextPrimary)
                            }
                            IconButton(
                                onClick = { viewModel.navigateTo(ScreenDestination.SETTINGS) },
                                modifier = Modifier.testTag("top_settings_btn")
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "Configurações", tint = TextPrimary)
                            }
                            IconButton(
                                onClick = { viewModel.toggleVirtualRemote() },
                                modifier = Modifier.testTag("top_remote_btn")
                            ) {
                                Icon(Icons.Default.SettingsRemote, contentDescription = "Controle Virtual", tint = NeonAmber)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = CinemaNavy
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = CinemaNavy,
                        contentColor = TextPrimary,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        navItems.take(5).forEach { item ->
                            val isSelected = currentScreen == item.destination
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { viewModel.navigateTo(item.destination) },
                                icon = {
                                    if (item.isLiveBadge) {
                                        BadgedBox(
                                            badge = {
                                                Badge(
                                                    containerColor = LiveBadgeRed,
                                                    modifier = Modifier.size(6.dp)
                                                )
                                            }
                                        ) {
                                            Icon(item.icon, contentDescription = item.title)
                                        }
                                    } else {
                                        Icon(item.icon, contentDescription = item.title)
                                    }
                                },
                                label = { Text(item.title, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = NeonCyan,
                                    indicatorColor = NeonCyan,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_bar_${item.destination.name.lowercase()}")
                            )
                        }
                    }
                },
                containerColor = CinemaBlack,
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    ScreenContent(currentScreen = currentScreen, viewModel = viewModel)
                }
            }
        }
    }

    // Virtual Remote Control Dialog
    if (showRemote) {
        VirtualRemoteDialog(
            onDismiss = { viewModel.dismissVirtualRemote() },
            onUp = {},
            onDown = {},
            onLeft = {
                if (currentScreen == ScreenDestination.PLAYER) {
                    viewModel.playerManager.seekBackward(10000L)
                }
            },
            onRight = {
                if (currentScreen == ScreenDestination.PLAYER) {
                    viewModel.playerManager.seekForward(10000L)
                }
            },
            onOk = {
                if (currentScreen == ScreenDestination.PLAYER) {
                    viewModel.playerManager.togglePlayPause()
                }
            },
            onBack = {
                if (currentScreen == ScreenDestination.PLAYER) {
                    viewModel.saveCurrentProgress()
                    viewModel.navigateBack()
                } else {
                    viewModel.dismissVirtualRemote()
                }
            },
            onPlayPause = {
                viewModel.playerManager.togglePlayPause()
            },
            onNextChannel = {
                viewModel.nextChannel()
            },
            onPrevChannel = {
                viewModel.previousChannel()
            },
            onAspectRatio = {
                viewModel.playerManager.cycleAspectRatio()
            },
            isPlaying = isPlaying
        )
    }
}

@Composable
fun ScreenContent(
    currentScreen: ScreenDestination,
    viewModel: TvViewModel
) {
    Crossfade(targetState = currentScreen, label = "screen_crossfade") { screen ->
        when (screen) {
            ScreenDestination.HOME -> HomeScreen(viewModel = viewModel)
            ScreenDestination.LIVE_TV -> LiveTvScreen(viewModel = viewModel)
            ScreenDestination.MOVIES -> MoviesScreen(viewModel = viewModel)
            ScreenDestination.SERIES -> SeriesScreen(viewModel = viewModel)
            ScreenDestination.FAVORITES -> FavoritesScreen(viewModel = viewModel)
            ScreenDestination.HISTORY -> HistoryScreen(viewModel = viewModel)
            ScreenDestination.SEARCH -> SearchScreen(viewModel = viewModel)
            ScreenDestination.PLAYLISTS -> PlaylistsScreen(viewModel = viewModel)
            ScreenDestination.SETTINGS -> SettingsScreen(viewModel = viewModel)
            ScreenDestination.PLAYER -> PlayerScreen(viewModel = viewModel)
        }
    }
}
