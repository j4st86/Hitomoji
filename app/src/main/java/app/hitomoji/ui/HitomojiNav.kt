package app.hitomoji.ui

import android.app.Activity
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.hitomoji.MainActivity
import app.hitomoji.ui.about.AboutScreen
import app.hitomoji.ui.catalog.CatalogScreen
import app.hitomoji.ui.catalog.KanjiDetailScreen
import app.hitomoji.ui.home.HomeScreen
import app.hitomoji.ui.study.StudyScreen
import kotlinx.coroutines.flow.StateFlow

@Composable
fun HitomojiNav(navSignals: StateFlow<Int>) {
    val navController = rememberNavController()
    val activity = LocalContext.current as Activity
    val start = remember {
        studyRouteFrom(activity.intent) ?: HomeRoute
    }
    val signal by navSignals.collectAsStateWithLifecycle()
    LaunchedEffect(signal) {
        if (signal == 0) return@LaunchedEffect
        val route = studyRouteFrom(activity.intent)
        if (route != null) {
            navController.navigate(route) { launchSingleTop = true }
        } else {
            navController.popBackStack(HomeRoute, inclusive = false)
        }
    }
    NavHost(navController = navController, startDestination = start) {
        composable<HomeRoute> {
            HomeScreen(
                onStart = { rank ->
                    navController.navigate(StudyRoute(rank, gradingAllowed = true))
                },
                onCatalog = { navController.navigate(CatalogRoute) },
                onAbout = { navController.navigate(AboutRoute) },
            )
        }
        composable<StudyRoute> { entry ->
            val route = entry.toRoute<StudyRoute>()
            StudyScreen(
                startRank = route.rank,
                gradingAllowed = route.gradingAllowed,
                onFinishedHome = {
                    navController.popBackStack(HomeRoute, inclusive = false)
                },
            )
        }
        composable<CatalogRoute> {
            CatalogScreen(
                onBack = { navController.popBackStack() },
                onOpen = { rank -> navController.navigate(KanjiViewRoute(rank)) },
            )
        }
        composable<KanjiViewRoute> { entry ->
            val route = entry.toRoute<KanjiViewRoute>()
            KanjiDetailScreen(rank = route.rank, onBack = { navController.popBackStack() })
        }
        composable<AboutRoute> {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}

fun studyRouteFrom(intent: Intent?): StudyRoute? {
    if (intent == null) return null
    val rank = intent.getIntExtra(MainActivity.EXTRA_RANK, -1)
    val grading = intent.getBooleanExtra(MainActivity.EXTRA_GRADING, false)
    return if (rank > 0 && grading) StudyRoute(rank, true) else null
}
