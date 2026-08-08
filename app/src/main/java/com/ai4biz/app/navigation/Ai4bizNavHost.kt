package com.ai4biz.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ai4biz.app.ui.auth.AuthScreen
import com.ai4biz.app.ui.businesssetup.BusinessSetupScreen
import com.ai4biz.app.ui.generator.GeneratorScreen
import com.ai4biz.app.ui.home.HomeScreen
import com.ai4biz.app.ui.onboarding.OnboardingScreen
import com.ai4biz.app.ui.profile.ProfileScreen
import com.ai4biz.app.ui.result.ResultScreen
import com.ai4biz.app.ui.subscription.SubscriptionScreen
import com.ai4biz.app.ui.workspace.WorkspaceScreen

@Composable
fun Ai4bizNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.ONBOARDING) {
        composable(Routes.ONBOARDING) { OnboardingScreen(navController) }
        composable(Routes.AUTH) { AuthScreen(navController) }
        composable(Routes.BUSINESS_SETUP) { BusinessSetupScreen(navController) }
        composable(Routes.HOME) { HomeScreen(navController) }
        composable(
            Routes.GENERATOR,
            arguments = listOf(navArgument("toolId") { type = NavType.StringType })
        ) { backStackEntry ->
            val toolId = backStackEntry.arguments?.getString("toolId").orEmpty()
            GeneratorScreen(navController = navController, toolId = toolId)
        }
        composable(
            Routes.RESULT,
            arguments = listOf(navArgument("documentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getString("documentId").orEmpty()
            ResultScreen(navController = navController, documentId = documentId)
        }
        composable(Routes.WORKSPACE) { WorkspaceScreen(navController) }
        composable(Routes.SUBSCRIPTION) { SubscriptionScreen(navController) }
        composable(Routes.PROFILE) { ProfileScreen(navController) }
    }
}
