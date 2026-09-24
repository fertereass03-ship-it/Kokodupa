package com.example.navigation

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.components.GlassBottomBar
import com.example.ui.components.NavTab
import com.example.ui.screens.catalog.CatalogScreen
import com.example.ui.screens.catalog.CatalogViewModel
import com.example.ui.screens.chat.ChatScreen
import com.example.ui.screens.chat.ChatViewModel
import com.example.ui.screens.detail.AnimeDetailScreen
import com.example.ui.screens.detail.AnimeDetailViewModel
import com.example.ui.screens.favorites.FavoritesScreen
import com.example.ui.screens.favorites.FavoritesViewModel
import com.example.ui.screens.friends.FriendsScreen
import com.example.ui.screens.friends.FriendsViewModel
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.screens.player.PlayerScreen
import com.example.ui.screens.player.PlayerViewModel
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.profile.ProfileViewModel
import com.example.ui.screens.profile.SetupProfileScreen
import com.example.ui.screens.splash.AnimeSplashScreen
import com.example.ui.screens.voice.VoiceAndEpisodeScreen
import com.example.ui.screens.voice.VoiceAndEpisodeViewModel
import com.example.ui.theme.BackgroundDark
import java.net.URLDecoder
import java.net.URLEncoder

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Auth : Screen("auth")
    object Home : Screen("home")
    object Catalog : Screen("catalog?preset={preset}") {
        fun createRoute(preset: String? = null): String {
            return if (!preset.isNullOrBlank()) "catalog?preset=$preset" else "catalog"
        }
    }
    object Favorites : Screen("favorites")
    object Friends : Screen("friends")
    object Profile : Screen("profile")
    object Detail : Screen("anime_detail/{animeId}") {
        fun createRoute(animeId: Long) = "anime_detail/$animeId"
    }
    object Voice : Screen("anime_voice/{animeId}") {
        fun createRoute(animeId: Long) = "anime_voice/$animeId"
    }
    object Player : Screen("anime_player/{animeId}/{episodeNum}/{voiceName}") {
        fun createRoute(animeId: Long, episodeNum: Int, voiceName: String) =
            "anime_player/$animeId/$episodeNum/${URLEncoder.encode(voiceName, "UTF-8")}"
    }
    object Chat : Screen("chat/{friendId}/{friendName}") {
        fun createRoute(friendId: String, friendName: String) =
            "chat/$friendId/${URLEncoder.encode(friendName, "UTF-8")}"
    }
    object SetupProfile : Screen("setup_profile/{email}/{displayName}/{avatarUrl}") {
        fun createRoute(email: String, displayName: String, avatarUrl: String): String {
            val encEmail = URLEncoder.encode(email.ifEmpty { "user@gmail.com" }, "UTF-8")
            val encName = URLEncoder.encode(displayName.ifEmpty { "User" }, "UTF-8")
            val encAvatar = URLEncoder.encode(avatarUrl.ifEmpty { "default" }, "UTF-8")
            return "setup_profile/$encEmail/$encName/$encAvatar"
        }
    }
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val application = context.applicationContext as? Application ?: (context as? Application) ?: (context.applicationContext as Application)
    val startDestination = Screen.Splash.route
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val currentTab = when {
        currentRoute == Screen.Home.route -> NavTab.HOME
        currentRoute == Screen.Catalog.route || currentRoute?.startsWith("catalog") == true -> NavTab.CATALOG
        currentRoute == Screen.Favorites.route -> NavTab.FAVORITES
        currentRoute == Screen.Friends.route -> NavTab.FRIENDS
        currentRoute == Screen.Profile.route -> NavTab.PROFILE
        else -> NavTab.HOME
    }

    val isTopLevelDestination = currentRoute in listOf(
        Screen.Home.route,
        Screen.Catalog.route,
        Screen.Favorites.route,
        Screen.Friends.route,
        Screen.Profile.route
    ) || currentRoute?.startsWith("catalog") == true

    Scaffold(
        modifier = modifier.fillMaxSize().background(BackgroundDark),
        containerColor = BackgroundDark,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
    ) { _ ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.fillMaxSize()
            ) {
                // 0. Anime Splash Screen (Anime intro with magical animation)
                composable(Screen.Splash.route) {
                    AnimeSplashScreen(
                        onFinish = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        }
                    )
                }

                // Auth Screen (Available when user chooses to login or switch accounts)
                composable(Screen.Auth.route) {
                    com.example.ui.screens.auth.AuthScreen(
                        onAuthSuccess = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Auth.route) { inclusive = true }
                            }
                        },
                        onContinueAsGuest = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Auth.route) { inclusive = true }
                            }
                        }
                    )
                }

                // 1. Главное (Home)
                composable(Screen.Home.route) {
                    val viewModel: HomeViewModel = viewModel()
                    HomeScreen(
                        viewModel = viewModel,
                        onAnimeClick = { animeId ->
                            navController.navigate(Screen.Detail.createRoute(animeId))
                        },
                        onWatchClick = { animeId, _ ->
                            navController.navigate(Screen.Voice.createRoute(animeId))
                        },
                        onNavigateToCatalog = {
                            navController.navigate(Screen.Catalog.createRoute()) {
                                popUpTo(Screen.Home.route)
                                launchSingleTop = true
                            }
                        },
                        onNavigateToCatalogWithPreset = { preset ->
                            navController.navigate(Screen.Catalog.createRoute(preset)) {
                                popUpTo(Screen.Home.route)
                                launchSingleTop = true
                            }
                        },
                        onNavigateToProfile = {
                            navController.navigate(Screen.Profile.route) {
                                popUpTo(Screen.Home.route)
                                launchSingleTop = true
                            }
                        }
                    )
                }

                // 2. Каталог (Catalog)
                composable(
                    route = Screen.Catalog.route,
                    arguments = listOf(
                        navArgument("preset") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { backStackEntry ->
                    val preset = backStackEntry.arguments?.getString("preset")
                    val viewModel: CatalogViewModel = viewModel()
                    androidx.compose.runtime.LaunchedEffect(preset) {
                        if (!preset.isNullOrBlank()) {
                            viewModel.applyPreset(preset)
                        }
                    }
                    CatalogScreen(
                        viewModel = viewModel,
                        onAnimeClick = { animeId ->
                            navController.navigate(Screen.Detail.createRoute(animeId))
                        },
                        onPlayEpisodeClick = { animeId, episodeNum ->
                            navController.navigate(Screen.Player.createRoute(animeId, episodeNum, "AniLibria"))
                        }
                    )
                }

                // 3. Избранное (Favorites)
                composable(Screen.Favorites.route) {
                    val viewModel: FavoritesViewModel = viewModel()
                    FavoritesScreen(
                        viewModel = viewModel,
                        onAnimeClick = { animeId ->
                            navController.navigate(Screen.Detail.createRoute(animeId))
                        },
                        onWatchHistoryItemClick = { animeId, _ ->
                            navController.navigate(Screen.Voice.createRoute(animeId))
                        },
                        onNavigateToCatalog = {
                            navController.navigate(Screen.Catalog.createRoute())
                        },
                        onNavigateToProfile = {
                            navController.navigate(Screen.Profile.route) {
                                popUpTo(Screen.Home.route)
                                launchSingleTop = true
                            }
                        }
                    )
                }

                // 4. Друзья (Friends)
                composable(Screen.Friends.route) {
                    val viewModel: FriendsViewModel = viewModel()
                    FriendsScreen(
                        viewModel = viewModel,
                        onOpenChat = { friendId, friendName ->
                            navController.navigate(Screen.Chat.createRoute(friendId, friendName))
                        },
                        onNavigateToProfile = {
                            navController.navigate(Screen.Profile.route) {
                                popUpTo(Screen.Home.route)
                                launchSingleTop = true
                            }
                        }
                    )
                }

                // 5. Профиль (Profile)
                composable(Screen.Profile.route) {
                    val viewModel: ProfileViewModel = viewModel()
                    ProfileScreen(
                        viewModel = viewModel,
                        onAnimeClick = { animeId ->
                            navController.navigate(Screen.Detail.createRoute(animeId))
                        },
                        onWatchClick = { animeId, _ ->
                            navController.navigate(Screen.Voice.createRoute(animeId))
                        },
                        onNavigateToFavorites = {
                            navController.navigate(Screen.Favorites.route) {
                                popUpTo(Screen.Home.route)
                                launchSingleTop = true
                            }
                        },
                        onNavigateToHome = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        },
                        onNavigateToSetupProfile = { email, displayName, avatarUrl ->
                            navController.navigate(Screen.SetupProfile.createRoute(email, displayName, avatarUrl))
                        },
                        onNavigateToAuth = {
                            navController.navigate(Screen.Auth.route)
                        },
                        onLogoutSuccess = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                // --- Details Screen ---
                composable(
                    route = Screen.Detail.route,
                    arguments = listOf(navArgument("animeId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val animeId = backStackEntry.arguments?.getLong("animeId") ?: 0L
                    val viewModel: AnimeDetailViewModel = viewModel(
                        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                @Suppress("UNCHECKED_CAST")
                                return AnimeDetailViewModel(application, animeId) as T
                            }
                        }
                    )
                    AnimeDetailScreen(
                        viewModel = viewModel,
                        onBackClick = { navController.popBackStack() },
                        onWatchClick = { id, _ ->
                            navController.navigate(Screen.Voice.createRoute(id))
                        },
                        onAnimeClick = { targetId ->
                            navController.navigate(Screen.Detail.createRoute(targetId))
                        },
                        onNavigateToProfile = {
                            navController.navigate(Screen.Profile.route) {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToAuth = {
                            navController.navigate(Screen.Auth.route)
                        }
                    )
                }

                // --- Voice & Episode Selection ---
                composable(
                    route = Screen.Voice.route,
                    arguments = listOf(navArgument("animeId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val animeId = backStackEntry.arguments?.getLong("animeId") ?: 0L
                    val viewModel: VoiceAndEpisodeViewModel = viewModel(
                        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                @Suppress("UNCHECKED_CAST")
                                return VoiceAndEpisodeViewModel(application, animeId) as T
                            }
                        }
                    )
                    VoiceAndEpisodeScreen(
                        viewModel = viewModel,
                        onBackClick = { navController.popBackStack() },
                        onEpisodeSelected = { episodeNum, voiceName ->
                            navController.navigate(Screen.Player.createRoute(animeId, episodeNum, voiceName))
                        }
                    )
                }

                // --- Video Player Screen ---
                composable(
                    route = Screen.Player.route,
                    arguments = listOf(
                        navArgument("animeId") { type = NavType.LongType },
                        navArgument("episodeNum") { type = NavType.IntType },
                        navArgument("voiceName") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val animeId = backStackEntry.arguments?.getLong("animeId") ?: 0L
                    val episodeNum = backStackEntry.arguments?.getInt("episodeNum") ?: 1
                    val rawVoice = backStackEntry.arguments?.getString("voiceName").orEmpty()
                    val voiceName = try { URLDecoder.decode(rawVoice, "UTF-8") } catch (e: Exception) { rawVoice }

                    val viewModel: PlayerViewModel = viewModel(
                        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                @Suppress("UNCHECKED_CAST")
                                return PlayerViewModel(application, animeId, episodeNum, voiceName) as T
                            }
                        }
                    )
                    PlayerScreen(
                        viewModel = viewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // --- Chat Screen ---
                composable(
                    route = Screen.Chat.route,
                    arguments = listOf(
                        navArgument("friendId") { type = NavType.StringType },
                        navArgument("friendName") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val friendId = backStackEntry.arguments?.getString("friendId").orEmpty()
                    val rawName = backStackEntry.arguments?.getString("friendName").orEmpty()
                    val friendName = try { URLDecoder.decode(rawName, "UTF-8") } catch (e: Exception) { rawName }

                    val viewModel: ChatViewModel = viewModel(
                        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                @Suppress("UNCHECKED_CAST")
                                return ChatViewModel(application, friendId, friendName) as T
                            }
                        }
                    )
                    ChatScreen(
                        viewModel = viewModel,
                        onBackClick = { navController.popBackStack() },
                        onAnimeClick = { animeId ->
                            navController.navigate(Screen.Detail.createRoute(animeId))
                        }
                    )
                }

                // 6. Встановлення нікнейму (Setup Profile - Step 2 Onboarding)
                composable(
                    route = Screen.SetupProfile.route,
                    arguments = listOf(
                        navArgument("email") { type = NavType.StringType },
                        navArgument("displayName") { type = NavType.StringType },
                        navArgument("avatarUrl") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val email = backStackEntry.arguments?.getString("email")?.let {
                        try { URLDecoder.decode(it, "UTF-8") } catch (e: Exception) { it }
                    }.orEmpty()
                    val displayName = backStackEntry.arguments?.getString("displayName")?.let {
                        try { URLDecoder.decode(it, "UTF-8") } catch (e: Exception) { it }
                    }.orEmpty()
                    val avatarUrlRaw = backStackEntry.arguments?.getString("avatarUrl")?.let {
                        try { URLDecoder.decode(it, "UTF-8") } catch (e: Exception) { it }
                    }.orEmpty()
                    val avatarUrl = if (avatarUrlRaw == "default") "" else avatarUrlRaw

                    val profileViewModel: ProfileViewModel = viewModel()

                    SetupProfileScreen(
                        email = email,
                        initialDisplayName = displayName,
                        avatarUrl = avatarUrl,
                        viewModel = profileViewModel,
                        onProfileSaved = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }

            // Glass Bottom Bar (Overlayed on top-level tabs)
            AnimatedVisibility(
                visible = isTopLevelDestination,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                GlassBottomBar(
                    currentTab = currentTab,
                    onTabSelected = { tab ->
                        val targetRoute = when (tab) {
                            NavTab.HOME -> Screen.Home.route
                            NavTab.CATALOG -> Screen.Catalog.createRoute()
                            NavTab.FAVORITES -> Screen.Favorites.route
                            NavTab.FRIENDS -> Screen.Friends.route
                            NavTab.PROFILE -> Screen.Profile.route
                        }
                        if (currentRoute != targetRoute) {
                            navController.navigate(targetRoute) {
                                popUpTo(Screen.Home.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    }
}
