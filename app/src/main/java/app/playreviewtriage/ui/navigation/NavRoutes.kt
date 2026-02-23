package app.playreviewtriage.ui.navigation

sealed class NavRoutes(val route: String) {
    data object SignIn : NavRoutes("sign_in")
    data object Setup : NavRoutes("setup")
    data object Today : NavRoutes("today")
    data object Detail : NavRoutes("detail/{reviewId}") {
        fun createRoute(reviewId: String) = "detail/$reviewId"
    }
    data object Settings : NavRoutes("settings")
}
