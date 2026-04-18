package com.stepserve.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.stepserve.app.data.auth.TokenManager
import com.stepserve.app.ui.screens.*
import com.stepserve.app.ui.screens.admin.*
import com.stepserve.app.ui.screens.provider.*

// ── Route constants ──────────────────────────────────────────────────────────

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val SEARCH = "search"
    const val SERVICE_DETAIL = "service/{serviceId}"
    const val BOOKING = "booking/{serviceId}/{serviceTitle}/{price}"
    const val CUSTOMER_BOOKINGS = "bookings"
    const val PROFILE = "profile"
    // Provider
    const val PROVIDER_DASHBOARD = "provider/dashboard"
    const val CREATE_LISTING = "provider/create"
    const val PROVIDER_LISTINGS = "provider/listings"
    const val PROVIDER_DOCUMENTS = "provider/documents"
    // Admin
    const val ADMIN_OVERVIEW = "admin/overview"
    const val ADMIN_USERS = "admin/users"
    const val ADMIN_BOOKINGS = "admin/bookings"
    const val ADMIN_CATEGORIES = "admin/categories"

    fun serviceDetail(serviceId: Int) = "service/$serviceId"
    fun booking(serviceId: Int, serviceTitle: String, price: Double) =
        "booking/$serviceId/${serviceTitle.encodeForRoute()}/$price"
}

private fun String.encodeForRoute() = java.net.URLEncoder.encode(this, "UTF-8")

// ── Bottom nav items (role-aware) ────────────────────────────────────────────

sealed class BottomNavItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home      : BottomNavItem(Routes.HOME, "Home", Icons.Filled.Home)
    object Search    : BottomNavItem(Routes.SEARCH, "Search", Icons.Filled.Search)
    object Bookings  : BottomNavItem(Routes.CUSTOMER_BOOKINGS, "Bookings", Icons.Filled.DateRange)
    object Dashboard : BottomNavItem(Routes.PROVIDER_DASHBOARD, "Dashboard", Icons.Filled.Dashboard)
    object Admin     : BottomNavItem(Routes.ADMIN_OVERVIEW, "Admin", Icons.Filled.AdminPanelSettings)
    object Profile   : BottomNavItem(Routes.PROFILE, "Profile", Icons.Filled.Person)
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    val role = TokenManager.getRole()
    val isLoggedIn = TokenManager.isLoggedIn()

    val bottomItems: List<BottomNavItem> = remember(role, isLoggedIn) {
        when {
            !isLoggedIn -> listOf(BottomNavItem.Home, BottomNavItem.Search)
            role == "customer" -> listOf(BottomNavItem.Home, BottomNavItem.Search, BottomNavItem.Bookings, BottomNavItem.Profile)
            role == "provider" -> listOf(BottomNavItem.Home, BottomNavItem.Search, BottomNavItem.Dashboard, BottomNavItem.Profile)
            role == "admin" -> listOf(BottomNavItem.Home, BottomNavItem.Search, BottomNavItem.Admin, BottomNavItem.Profile)
            else -> listOf(BottomNavItem.Home, BottomNavItem.Search)
        }
    }

    val showBottomBar = currentRoute in bottomItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentBackStack?.destination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(navController)
            }
            composable(Routes.LOGIN) {
                LoginScreen(navController)
            }
            composable(Routes.REGISTER) {
                RegisterScreen(navController)
            }
            composable(Routes.HOME) {
                HomeScreen(navController)
            }
            composable(Routes.SEARCH) {
                SearchScreen(navController)
            }
            composable(
                Routes.SERVICE_DETAIL,
                arguments = listOf(navArgument("serviceId") { type = NavType.IntType }),
            ) { backStackEntry ->
                val serviceId = backStackEntry.arguments?.getInt("serviceId") ?: return@composable
                ServiceDetailScreen(navController, serviceId)
            }
            composable(
                Routes.BOOKING,
                arguments = listOf(
                    navArgument("serviceId") { type = NavType.IntType },
                    navArgument("serviceTitle") { type = NavType.StringType },
                    navArgument("price") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val serviceId = backStackEntry.arguments?.getInt("serviceId") ?: return@composable
                val serviceTitle = backStackEntry.arguments?.getString("serviceTitle")
                    ?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: ""
                val price = backStackEntry.arguments?.getString("price")?.toDoubleOrNull() ?: 0.0
                BookingScreen(navController, serviceId, serviceTitle, price)
            }
            composable(Routes.CUSTOMER_BOOKINGS) {
                CustomerBookingsScreen(navController)
            }
            composable(Routes.PROFILE) {
                ProfileScreen(navController)
            }
            // Provider
            composable(Routes.PROVIDER_DASHBOARD) {
                ProviderDashboardScreen(navController)
            }
            composable(Routes.CREATE_LISTING) {
                CreateListingScreen(navController)
            }
            composable(Routes.PROVIDER_LISTINGS) {
                ProviderListingsScreen(navController)
            }
            composable(Routes.PROVIDER_DOCUMENTS) {
                ProviderDocumentsScreen(navController)
            }
            // Admin
            composable(Routes.ADMIN_OVERVIEW) {
                AdminOverviewScreen(navController)
            }
            composable(Routes.ADMIN_USERS) {
                AdminUsersScreen(navController)
            }
            composable(Routes.ADMIN_BOOKINGS) {
                AdminBookingsScreen(navController)
            }
            composable(Routes.ADMIN_CATEGORIES) {
                AdminCategoriesScreen(navController)
            }
        }
    }
}
