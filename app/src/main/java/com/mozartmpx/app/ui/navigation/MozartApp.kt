package com.mozartmpx.app.ui.navigation

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.mozartmpx.app.data.model.Track
import com.mozartmpx.app.ui.components.AddToPlaylistDialog
import com.mozartmpx.app.ui.components.ConfirmTrackActionDialog
import com.mozartmpx.app.ui.components.CreatePlaylistDialog
import com.mozartmpx.app.ui.components.MiniPlayerBar
import com.mozartmpx.app.ui.screens.AlbumDetailScreen
import com.mozartmpx.app.ui.screens.ArtistDetailScreen
import com.mozartmpx.app.ui.screens.ArtistsAndAlbumsScreen
import com.mozartmpx.app.ui.screens.EqualizerHiFiSheet
import com.mozartmpx.app.ui.screens.FolderDetailScreen
import com.mozartmpx.app.ui.screens.FoldersScreen
import com.mozartmpx.app.ui.screens.HomeScreen
import com.mozartmpx.app.ui.screens.NowPlayingScreen
import com.mozartmpx.app.ui.screens.PlaylistDetailScreen
import com.mozartmpx.app.ui.screens.PlaylistsScreen
import com.mozartmpx.app.ui.screens.QueueBottomSheet
import com.mozartmpx.app.ui.screens.SearchScreen
import com.mozartmpx.app.ui.screens.VIRTUAL_PLAYLIST_FAVORITES
import com.mozartmpx.app.ui.screens.VIRTUAL_PLAYLIST_MOST_PLAYED
import com.mozartmpx.app.ui.viewmodel.MozartViewModel
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object ArtistsRoute
@Serializable data object FoldersRoute
@Serializable data object PlaylistsRoute
@Serializable data object SearchRoute
@Serializable data class ArtistDetailRoute(val artistId: String)
@Serializable data class AlbumDetailRoute(val albumId: Long)
@Serializable data class FolderDetailRoute(val folderPath: String)
@Serializable data class PlaylistDetailRoute(val playlistId: Long)

private data class TopNavItem(
    val route: Any,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun MozartApp(viewModel: MozartViewModel) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val libraryState by viewModel.libraryState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val audioFxState by viewModel.audioFxState.collectAsStateWithLifecycle()
    val hiddenTracks by viewModel.hiddenTracks.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.filteredSearchTracks.collectAsStateWithLifecycle()

    var nowPlayingExpanded by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showEqSheet by remember { mutableStateOf(false) }
    var inspectedHiFiTrack by remember { mutableStateOf<Track?>(null) }
    var trackForPlaylistDialog by remember { mutableStateOf<Track?>(null) }
    var pendingNewPlaylistTrack by remember { mutableStateOf<Track?>(null) }
    var trackPendingHideConfirm by remember { mutableStateOf<Track?>(null) }
    var trackPendingDeleteConfirm by remember { mutableStateOf<Track?>(null) }
    var pendingSystemDeleteTrack by remember { mutableStateOf<Track?>(null) }

    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[audioPermission] == true
        viewModel.setAudioPermissionGranted(granted, triggerScan = granted)
    }

    val systemDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val target = pendingSystemDeleteTrack
        pendingSystemDeleteTrack = null
        if (result.resultCode == Activity.RESULT_OK && target != null) {
            viewModel.onDeviceDeleteConfirmed(target)
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.addLocalFolder(treeUri)
        }
    }

    val audioFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
            viewModel.importAudioFiles(uris)
        }
    }

    val requestAudioPermissions: () -> Unit = {
        val toRequest = buildList {
            add(audioPermission)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
        permissionLauncher.launch(toRequest)
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
        viewModel.setAudioPermissionGranted(granted, triggerScan = granted)
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.deleteConfirmationEvents.collect { pending ->
            pendingSystemDeleteTrack = pending.track
            systemDeleteLauncher.launch(
                IntentSenderRequest.Builder(pending.intentSender).build()
            )
        }
    }

    val topNavItems = remember {
        listOf(
            TopNavItem(HomeRoute, "Inicio", Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
            TopNavItem(ArtistsRoute, "Artistas", Icons.Rounded.Person, Icons.Outlined.Person, "nav_artists"),
            TopNavItem(FoldersRoute, "Carpetas", Icons.Rounded.Folder, Icons.Outlined.Folder, "nav_folders"),
            TopNavItem(PlaylistsRoute, "Listas", Icons.AutoMirrored.Rounded.QueueMusic, Icons.AutoMirrored.Outlined.QueueMusic, "nav_playlists")
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val navigateTopLevel: (Any) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val openHiFiForTrack: (Track?) -> Unit = { target ->
        val resolved = target ?: playbackState.currentTrack
        inspectedHiFiTrack = resolved
        showEqSheet = true
        if (resolved != null) {
            viewModel.enrichTrackForInspection(resolved) { enriched ->
                inspectedHiFiTrack = enriched
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Box(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        topNavItems.forEach { item ->
                            val selected = when (item.route) {
                                is HomeRoute -> currentDestination?.hasRoute<HomeRoute>() == true
                                is ArtistsRoute -> currentDestination?.hasRoute<ArtistsRoute>() == true ||
                                    currentDestination?.hasRoute<ArtistDetailRoute>() == true ||
                                    currentDestination?.hasRoute<AlbumDetailRoute>() == true
                                is FoldersRoute -> currentDestination?.hasRoute<FoldersRoute>() == true ||
                                    currentDestination?.hasRoute<FolderDetailRoute>() == true
                                is PlaylistsRoute -> currentDestination?.hasRoute<PlaylistsRoute>() == true ||
                                    currentDestination?.hasRoute<PlaylistDetailRoute>() == true
                                else -> false
                            }
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        Column {
                            AnimatedVisibility(
                                visible = playbackState.currentTrack != null && !nowPlayingExpanded,
                                enter = slideInVertically(tween(180)) { it } + fadeIn(tween(150)),
                                exit = slideOutVertically(tween(150)) { it } + fadeOut(tween(120))
                            ) {
                                MiniPlayerBar(
                                    playbackState = playbackState,
                                    positionMsFlow = viewModel.playbackPositionMs,
                                    onExpand = { nowPlayingExpanded = true },
                                    onTogglePlayPause = viewModel::togglePlayPause,
                                    onSkipNext = viewModel::skipNext
                                )
                            }

                            if (!useNavigationRail) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                    tonalElevation = 2.dp
                                ) {
                                    topNavItems.forEach { item ->
                                        val selected = when (item.route) {
                                            is HomeRoute -> currentDestination?.hasRoute<HomeRoute>() == true
                                            is ArtistsRoute -> currentDestination?.hasRoute<ArtistsRoute>() == true ||
                                                currentDestination?.hasRoute<ArtistDetailRoute>() == true ||
                                                currentDestination?.hasRoute<AlbumDetailRoute>() == true
                                            is FoldersRoute -> currentDestination?.hasRoute<FoldersRoute>() == true ||
                                                currentDestination?.hasRoute<FolderDetailRoute>() == true
                                            is PlaylistsRoute -> currentDestination?.hasRoute<PlaylistsRoute>() == true ||
                                                currentDestination?.hasRoute<PlaylistDetailRoute>() == true
                                            else -> false
                                        }
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { navigateTopLevel(item.route) },
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                    contentDescription = item.label
                                                )
                                            },
                                            label = { Text(item.label) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            modifier = Modifier.testTag(item.testTag)
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    val listBottomPadding = PaddingValues(bottom = 16.dp)
                    NavHost(
                        navController = navController,
                        startDestination = HomeRoute,
                        enterTransition = { fadeIn(tween(140)) },
                        exitTransition = { fadeOut(tween(120)) },
                        popEnterTransition = { fadeIn(tween(140)) },
                        popExitTransition = { fadeOut(tween(120)) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        composable<HomeRoute> {
                            HomeScreen(
                                libraryState = libraryState,
                                playbackState = playbackState,
                                onOpenSearch = { navController.navigate(SearchRoute) },
                                onOpenEqualizer = { openHiFiForTrack(playbackState.currentTrack) },
                                onRequestPermission = requestAudioPermissions,
                                onPickFolder = { folderPickerLauncher.launch(null) },
                                onImportFiles = { audioFilePickerLauncher.launch(arrayOf("audio/*")) },
                                onRescan = { viewModel.rescanLibrary(silent = false) },
                                onOpenArtist = { artistId -> navController.navigate(ArtistDetailRoute(artistId)) },
                                onOpenAlbum = { albumId -> navController.navigate(AlbumDetailRoute(albumId)) },
                                onPlaySession = { session ->
                                    viewModel.playTracks(session.tracks, 0, session.title, true)
                                },
                                onPlayTracks = viewModel::playTracks,
                                onPlayNext = viewModel::playNextInQueue,
                                onAddToQueue = viewModel::addToQueue,
                                onAddToPlaylist = { trackForPlaylistDialog = it },
                                onToggleFavorite = viewModel::toggleFavorite,
                                onInspectHiFi = { openHiFiForTrack(it) },
                                onHideTrack = { trackPendingHideConfirm = it },
                                onDeleteTrack = { trackPendingDeleteConfirm = it },
                                contentBottomPadding = listBottomPadding
                            )
                        }

                        composable<ArtistsRoute> {
                            ArtistsAndAlbumsScreen(
                                artists = libraryState.artists,
                                albums = libraryState.albums,
                                onOpenArtist = { artistId -> navController.navigate(ArtistDetailRoute(artistId)) },
                                onOpenAlbum = { albumId -> navController.navigate(AlbumDetailRoute(albumId)) },
                                onOpenSearch = { navController.navigate(SearchRoute) },
                                contentBottomPadding = listBottomPadding
                            )
                        }

                        composable<ArtistDetailRoute> { entry ->
                            val args = entry.toRoute<ArtistDetailRoute>()
                            val artist = libraryState.artists.firstOrNull { it.id == args.artistId }
                            ArtistDetailScreen(
                                artist = artist,
                                playbackState = playbackState,
                                onBack = { navController.popBackStack() },
                                onOpenAlbum = { albumId -> navController.navigate(AlbumDetailRoute(albumId)) },
                                onPlayTracks = viewModel::playTracks,
                                onPlayNext = viewModel::playNextInQueue,
                                onAddToQueue = viewModel::addToQueue,
                                onAddToPlaylist = { trackForPlaylistDialog = it },
                                onToggleFavorite = viewModel::toggleFavorite,
                                onInspectHiFi = { openHiFiForTrack(it) },
                                onHideTrack = { trackPendingHideConfirm = it },
                                onDeleteTrack = { trackPendingDeleteConfirm = it },
                                contentBottomPadding = listBottomPadding
                            )
                        }

                        composable<AlbumDetailRoute> { entry ->
                            val args = entry.toRoute<AlbumDetailRoute>()
                            val album = libraryState.albums.firstOrNull { it.albumId == args.albumId }
                            AlbumDetailScreen(
                                album = album,
                                playbackState = playbackState,
                                onBack = { navController.popBackStack() },
                                onPlayTracks = viewModel::playTracks,
                                onPlayNext = viewModel::playNextInQueue,
                                onAddToQueue = viewModel::addToQueue,
                                onAddToPlaylist = { trackForPlaylistDialog = it },
                                onToggleFavorite = viewModel::toggleFavorite,
                                onInspectHiFi = { openHiFiForTrack(it) },
                                onHideTrack = { trackPendingHideConfirm = it },
                                onDeleteTrack = { trackPendingDeleteConfirm = it },
                                contentBottomPadding = listBottomPadding
                            )
                        }

                        composable<FoldersRoute> {
                            FoldersScreen(
                                folders = libraryState.folders,
                                hiddenTracks = hiddenTracks,
                                onOpenFolder = { path -> navController.navigate(FolderDetailRoute(path)) },
                                onPlayFolder = { folder ->
                                    viewModel.playTracks(folder.tracks, 0, folder.folderName, false)
                                },
                                onPickFolder = { folderPickerLauncher.launch(null) },
                                onImportFiles = { audioFilePickerLauncher.launch(arrayOf("audio/*")) },
                                onUnhideTrack = viewModel::unhideTrack,
                                contentBottomPadding = listBottomPadding
                            )
                        }

                        composable<FolderDetailRoute> { entry ->
                            val args = entry.toRoute<FolderDetailRoute>()
                            val folder = libraryState.folders.firstOrNull { it.folderPath == args.folderPath }
                            FolderDetailScreen(
                                folder = folder,
                                playbackState = playbackState,
                                onBack = { navController.popBackStack() },
                                onPlayTracks = viewModel::playTracks,
                                onPlayNext = viewModel::playNextInQueue,
                                onAddToQueue = viewModel::addToQueue,
                                onAddToPlaylist = { trackForPlaylistDialog = it },
                                onToggleFavorite = viewModel::toggleFavorite,
                                onInspectHiFi = { openHiFiForTrack(it) },
                                onHideTrack = { trackPendingHideConfirm = it },
                                onDeleteTrack = { trackPendingDeleteConfirm = it },
                                contentBottomPadding = listBottomPadding
                            )
                        }

                        composable<PlaylistsRoute> {
                            PlaylistsScreen(
                                playlists = libraryState.playlists,
                                favoritesCount = libraryState.favorites.size,
                                mostPlayedCount = libraryState.mostPlayed.size,
                                onCreatePlaylist = { name, desc -> viewModel.createPlaylist(name, desc) },
                                onRenamePlaylist = viewModel::renamePlaylist,
                                onDeletePlaylist = viewModel::deletePlaylist,
                                onOpenPlaylist = { id -> navController.navigate(PlaylistDetailRoute(id)) },
                                contentBottomPadding = listBottomPadding
                            )
                        }

                        composable<PlaylistDetailRoute> { entry ->
                            val args = entry.toRoute<PlaylistDetailRoute>()
                            val playlistId = args.playlistId
                            val customTracks by viewModel.observePlaylistTracks(playlistId)
                                .collectAsStateWithLifecycle(initialValue = emptyList())

                            val (title, tracks, removable) = when (playlistId) {
                                VIRTUAL_PLAYLIST_FAVORITES -> Triple("Favoritas", libraryState.favorites, false)
                                VIRTUAL_PLAYLIST_MOST_PLAYED -> Triple("Top", libraryState.mostPlayed, false)
                                else -> {
                                    val summary = libraryState.playlists.firstOrNull { it.id == playlistId }
                                    Triple(summary?.name ?: "Lista", customTracks, true)
                                }
                            }

                            PlaylistDetailScreen(
                                playlistId = playlistId,
                                title = title,
                                tracks = tracks,
                                playbackState = playbackState,
                                onBack = { navController.popBackStack() },
                                onPlayTracks = viewModel::playTracks,
                                onPlayNext = viewModel::playNextInQueue,
                                onAddToQueue = viewModel::addToQueue,
                                onAddToPlaylist = { trackForPlaylistDialog = it },
                                onToggleFavorite = viewModel::toggleFavorite,
                                onInspectHiFi = { openHiFiForTrack(it) },
                                onHideTrack = { trackPendingHideConfirm = it },
                                onDeleteTrack = { trackPendingDeleteConfirm = it },
                                onRemoveFromPlaylist = if (removable) {
                                    { trackId -> viewModel.removeTrackFromPlaylist(playlistId, trackId) }
                                } else null,
                                contentBottomPadding = listBottomPadding
                            )
                        }

                        composable<SearchRoute> {
                            SearchScreen(
                                query = searchQuery,
                                results = searchResults,
                                playbackState = playbackState,
                                onQueryChange = viewModel::updateSearchQuery,
                                onBack = { navController.popBackStack() },
                                onPlayTracks = viewModel::playTracks,
                                onPlayNext = viewModel::playNextInQueue,
                                onAddToQueue = viewModel::addToQueue,
                                onAddToPlaylist = { trackForPlaylistDialog = it },
                                onToggleFavorite = viewModel::toggleFavorite,
                                onInspectHiFi = { openHiFiForTrack(it) },
                                onHideTrack = { trackPendingHideConfirm = it },
                                onDeleteTrack = { trackPendingDeleteConfirm = it },
                                contentBottomPadding = listBottomPadding
                            )
                        }
                    }
                }
            }

            // Full-Screen 100% Opaque Now Playing Overlay (snappy 200ms transition)
            AnimatedVisibility(
                visible = nowPlayingExpanded && playbackState.currentTrack != null,
                enter = slideInVertically(tween(200)) { it },
                exit = slideOutVertically(tween(180)) { it }
            ) {
                NowPlayingScreen(
                    playbackState = playbackState,
                    positionMsFlow = viewModel.playbackPositionMs,
                    eqActive = audioFxState.enabled,
                    onDismiss = { nowPlayingExpanded = false },
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onSkipNext = viewModel::skipNext,
                    onSkipPrevious = viewModel::skipPrevious,
                    onSeekTo = viewModel::seekTo,
                    onToggleShuffle = viewModel::toggleShuffle,
                    onCycleRepeat = viewModel::cycleRepeatMode,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onOpenQueue = { showQueueSheet = true },
                    onOpenEqualizer = { openHiFiForTrack(playbackState.currentTrack) },
                    onAddToPlaylist = { trackForPlaylistDialog = it },
                    onHideTrack = { trackPendingHideConfirm = it },
                    onDeleteTrack = { trackPendingDeleteConfirm = it }
                )
            }
        }
    }

    if (showQueueSheet) {
        QueueBottomSheet(
            playbackState = playbackState,
            onDismiss = { showQueueSheet = false },
            onPlayTrack = { track ->
                viewModel.playTrack(track)
                showQueueSheet = false
            },
            onRemoveManual = viewModel::removeFromManualQueue,
            onRemoveSession = viewModel::removeFromSessionQueue,
            onClearManual = viewModel::clearManualQueue,
            onToggleSmartAutoplay = viewModel::setSmartAutoplayEnabled
        )
    }

    if (showEqSheet) {
        EqualizerHiFiSheet(
            audioFxState = audioFxState,
            crossfadeSeconds = playbackState.crossfadeSeconds,
            inspectedTrack = inspectedHiFiTrack ?: playbackState.currentTrack,
            onDismiss = { showEqSheet = false },
            onSetEqEnabled = viewModel::setEqEnabled,
            onSelectPreset = viewModel::selectEqPreset,
            onSetBandLevel = viewModel::setEqBandLevel,
            onSetBassBoost = viewModel::setBassBoost,
            onSetVirtualizer = viewModel::setVirtualizer,
            onResetEq = viewModel::resetEqualizer,
            onSetCrossfadeSeconds = viewModel::setCrossfadeSeconds
        )
    }

    trackForPlaylistDialog?.let { targetTrack ->
        AddToPlaylistDialog(
            playlists = libraryState.playlists,
            track = targetTrack,
            onDismiss = { trackForPlaylistDialog = null },
            onSelectPlaylist = { pl ->
                viewModel.addTrackToPlaylist(pl.id, pl.name, targetTrack)
                trackForPlaylistDialog = null
            },
            onCreateNewPlaylist = {
                pendingNewPlaylistTrack = targetTrack
                trackForPlaylistDialog = null
            }
        )
    }

    pendingNewPlaylistTrack?.let { initialTrack ->
        CreatePlaylistDialog(
            onDismiss = { pendingNewPlaylistTrack = null },
            onConfirm = { name, desc ->
                viewModel.createPlaylist(name, desc, initialTrack)
                pendingNewPlaylistTrack = null
            }
        )
    }

    trackPendingHideConfirm?.let { target ->
        ConfirmTrackActionDialog(
            title = "Ocultar pista",
            body = "«${target.title}» se ocultará permanentemente en Mozart mpx sin borrar el archivo del teléfono.",
            confirmLabel = "Ocultar",
            isDestructive = false,
            onDismiss = { trackPendingHideConfirm = null },
            onConfirm = {
                viewModel.hideTrackPermanently(target)
                trackPendingHideConfirm = null
            }
        )
    }

    trackPendingDeleteConfirm?.let { target ->
        ConfirmTrackActionDialog(
            title = "Eliminar del dispositivo",
            body = "Se borrará permanentemente el archivo «${target.title}» de la memoria de tu teléfono.",
            confirmLabel = "Eliminar",
            isDestructive = true,
            onDismiss = { trackPendingDeleteConfirm = null },
            onConfirm = {
                viewModel.deleteTrackFromDevice(target)
                trackPendingDeleteConfirm = null
            }
        )
    }
}
