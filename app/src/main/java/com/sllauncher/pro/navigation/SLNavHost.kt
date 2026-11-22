package com.sllauncher.pro.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.sllauncher.pro.common.UiCommand
import com.sllauncher.pro.home.HomeRoute
import com.sllauncher.pro.onboarding.OnboardingRoute
import com.sllauncher.pro.setup.LauncherSetupRoute
import com.sllauncher.pro.widgets.WidgetHostManager
import com.sllauncher.pro.widgets.WidgetPickerRoute
import kotlinx.coroutines.flow.Flow

/** Feature screens registered by later phases (news, music, settings...). */
typealias FeatureGraph = NavGraphBuilder.(navController: NavHostController) -> Unit

@Composable
fun SLNavHost(
    navController: NavHostController,
    startOnboarding: Boolean,
    widgetHost: WidgetHostManager,
    commands: Flow<UiCommand>,
    homeExtras: HomeExtras,
    featureGraph: FeatureGraph,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = if (startOnboarding) OnboardingDestination else HomeDestination,
        modifier = modifier,
    ) {
        composable<HomeDestination> {
            HomeRoute(
                onOpenLauncherSetup = { navController.navigate(LauncherSetupDestination) { launchSingleTop = true } },
                onNavigate = { route -> navController.navigate(route) { launchSingleTop = true } },
                widgetHost = widgetHost,
                island = homeExtras.island,
                overview = homeExtras.overview,
                badgeFor = homeExtras.badgeFor,
                commands = commands,
            )
        }
        composable<OnboardingDestination> {
            OnboardingRoute(onDone = {
                navController.navigate(HomeDestination) { popUpTo(OnboardingDestination) { inclusive = true } }
            })
        }
        composable<LauncherSetupDestination> {
            LauncherSetupRoute(onDone = { navController.popBackStack(HomeDestination, inclusive = false) })
        }
        composable<WidgetPickerDestination> {
            WidgetPickerRoute(onBack = { navController.popBackStack() })
        }
        featureGraph(navController)
    }
}

/** Home-screen pieces provided by feature phases. */
class HomeExtras(
    val island: @Composable () -> Unit = {},
    val overview: @Composable () -> Unit = {},
    val badgeFor: (String) -> Int = { 0 },
)

/** Returns to the home screen, closing anything above it. Used when Home is pressed. */
fun NavHostController.returnToHome() {
    popBackStack(HomeDestination, inclusive = false)
}
