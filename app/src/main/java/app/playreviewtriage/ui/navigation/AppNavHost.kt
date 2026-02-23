package app.playreviewtriage.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.playreviewtriage.presentation.viewmodel.MainViewModel
import app.playreviewtriage.ui.component.LoadingView
import app.playreviewtriage.ui.screen.detail.ReviewDetailScreen
import app.playreviewtriage.ui.screen.settings.SettingsScreen
import app.playreviewtriage.ui.screen.setup.SetupScreen
import app.playreviewtriage.ui.screen.signin.SignInScreen
import app.playreviewtriage.ui.screen.today.TodayScreen

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    val startDestination by mainViewModel.startDestination.collectAsState()

    val resolvedStart = startDestination ?: run {
        LoadingView()
        return
    }

    NavHost(
        navController = navController,
        startDestination = resolvedStart,
    ) {
        composable(NavRoutes.SignIn.route) {
            SignInScreen(
                onSuccess = {
                    navController.navigate(NavRoutes.Setup.route) {
                        popUpTo(NavRoutes.SignIn.route) { inclusive = true }
                    }
                },
            )
        }
        composable(NavRoutes.Setup.route) {
            SetupScreen(
                onSuccess = {
                    navController.navigate(NavRoutes.Today.route) {
                        popUpTo(NavRoutes.Setup.route) { inclusive = true }
                    }
                },
                onLogout = {
                    navController.navigate(NavRoutes.SignIn.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
        composable(NavRoutes.Today.route) {
            TodayScreen(
                onNavigateToDetail = { reviewId ->
                    navController.navigate(NavRoutes.Detail.createRoute(reviewId))
                },
                onNavigateToSettings = {
                    navController.navigate(NavRoutes.Settings.route)
                },
            )
        }
        composable(
            route = NavRoutes.Detail.route,
            arguments = listOf(navArgument("reviewId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val reviewId = backStackEntry.arguments?.getString("reviewId") ?: ""
            ReviewDetailScreen(
                reviewId = reviewId,
                onBack = { navController.popBackStack() },
            )
        }
        composable(NavRoutes.Settings.route) {
            SettingsScreen(
                onLogout = {
                    navController.navigate(NavRoutes.SignIn.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
