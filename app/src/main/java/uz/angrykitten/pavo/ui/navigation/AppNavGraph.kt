package uz.angrykitten.pavo.ui.navigation

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.*
import uz.angrykitten.pavo.ui.localization.AppLanguage
import uz.angrykitten.pavo.ui.localization.tr
import uz.angrykitten.pavo.ui.screens.*
import uz.angrykitten.pavo.ui.theme.LocalDarkTheme
import uz.angrykitten.pavo.ui.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph(
    viewModel: AppViewModel = viewModel(),
    onToggleTheme: () -> Unit = {},
    currentLanguage: AppLanguage = AppLanguage.UZ,
    onChangeLanguage: (AppLanguage) -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavRoutes = setOf(
        Screen.Home.route,
        Screen.Search.route,
        Screen.PostListing.route,
        Screen.Saved.route,
        Screen.Profile.route,
        Screen.Chat.route,
        Screen.ChatDetail.route
    )
    val showBottomBar = currentDestination?.route in bottomNavRoutes

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(tween(300)) { it } + fadeIn(tween(300)),
                exit = slideOutVertically(tween(300)) { it } + fadeOut(tween(300))
            ) {
                MinimalistNavigationBar(
                    navController = navController,
                    currentDestination = currentDestination
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { slideInHorizontally(tween(350)) { it / 3 } + fadeIn(tween(350)) },
            exitTransition = { slideOutHorizontally(tween(350)) { -it / 3 } + fadeOut(tween(350)) },
            popEnterTransition = { slideInHorizontally(tween(350)) { -it / 3 } + fadeIn(tween(350)) },
            popExitTransition = { slideOutHorizontally(tween(350)) { it / 3 } + fadeOut(tween(350)) }
        ) {
            composable(Screen.Home.route) { HomeScreen(viewModel, navController) }
            composable(Screen.Search.route) { SearchScreen(viewModel, navController) }
            composable(Screen.PostListing.route) { PostListingScreen(viewModel, navController) }
            composable(Screen.Saved.route) { SavedScreen(viewModel, navController) }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    viewModel,
                    navController,
                    onToggleTheme,
                    currentLanguage = currentLanguage,
                    onChangeLanguage = onChangeLanguage
                )
            }
            composable(Screen.AnimalDetail.route) { backStackEntry ->
                val animalId = backStackEntry.arguments?.getString("animalId") ?: return@composable
                AnimalDetailScreen(animalId, viewModel, navController)
            }
            composable(Screen.Splash.route) { SplashScreen(viewModel, navController) }
            composable(Screen.Login.route) { LoginScreen(viewModel, navController) }
            composable(Screen.Register.route) { RegisterScreen(viewModel, navController) }
            composable(Screen.MyListings.route) { MyListingsScreen(viewModel, navController) }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel,
                    navController,
                    onToggleTheme,
                    currentLanguage = currentLanguage,
                    onChangeLanguage = onChangeLanguage
                )
            }
            composable(Screen.Chat.route) { ChatScreen(viewModel, navController) }
            composable(Screen.ChatDetail.route) { backStackEntry ->
                val chatId = backStackEntry.arguments?.getString("chatId")?.let(Uri::decode) ?: return@composable
                val otherUserId = backStackEntry.arguments?.getString("otherUserId")?.let(Uri::decode) ?: return@composable
                val otherUserName = backStackEntry.arguments?.getString("otherUserName")?.let(Uri::decode) ?: return@composable
                ChatDetailScreen(chatId, otherUserId, otherUserName, viewModel, navController)
            }
            composable(Screen.SellerProfile.route) { backStackEntry ->
                val sellerId = backStackEntry.arguments?.getString("sellerId") ?: return@composable
                SellerProfileScreen(sellerId, viewModel, navController)
            }
            composable(Screen.FAQ.route) { FAQScreen(navController) }
            composable(Screen.PrivacyPolicy.route) { PrivacyPolicyScreen(navController) }
        }
    }
}


@Composable
fun MinimalistNavigationBar(
    navController: androidx.navigation.NavController,
    currentDestination: androidx.navigation.NavDestination?
) {
    val navSelected = MaterialTheme.colorScheme.primary
    val navBackground = if (LocalDarkTheme.current) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, spotColor = Color.Black.copy(alpha = 0.4f))
            .background(navBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEach { item ->
                val selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
                val isPost = item.screen == Screen.PostListing

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            navController.navigate(item.screen.route) {
                                // Use Home route (not findStartDestination) because Splash
                                // removes itself from the back stack at launch, making
                                // findStartDestination point to an entry that no longer exists
                                // — this caused every tab press to push onto the stack.
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isPost) {
                        // FAB-style center button
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedContent(
                                targetState = selected,
                                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                                label = "postIcon"
                            ) { sel ->
                                Icon(
                                    imageVector = if (sel) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else {
                        AnimatedContent(
                            targetState = selected,
                            transitionSpec = { fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) togetherWith fadeOut(spring(stiffness = Spring.StiffnessMediumLow)) },
                            label = "navIcon_${item.label}"
                        ) { sel ->
                            Icon(
                                imageVector = if (sel) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                tint = if (sel) navSelected else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Text(
                        text = bottomNavLabel(item.screen),
                        fontSize = 9.5.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) navSelected else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun bottomNavLabel(screen: Screen): String {
    return when (screen) {
        Screen.Home -> tr("Asosiy", "Home", "Главная")
        Screen.Chat -> tr("Chat", "Chats", "Чаты")
        Screen.PostListing -> tr("E'lon", "Post", "Подать")
        Screen.Saved -> tr("Saqlangan", "Saved", "Избранное")
        Screen.Profile -> tr("Profil", "Profile", "Профиль")
        else -> ""
    }
}
