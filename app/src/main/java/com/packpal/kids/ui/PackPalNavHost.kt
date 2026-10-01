package com.packpal.kids.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.packpal.kids.ui.backpack.BackpackRoute
import com.packpal.kids.ui.compartment.CompartmentRoute
import com.packpal.kids.ui.history.HistoryDetailRoute
import com.packpal.kids.ui.history.HistoryRoute
import com.packpal.kids.ui.parent.ParentHistoryRoute
import com.packpal.kids.ui.parent.ParentHomeRoute
import com.packpal.kids.ui.parent.PrivacyRoute
import com.packpal.kids.ui.parent.TemplateEditorRoute
import com.packpal.kids.ui.templates.TemplateSelectorRoute
import com.packpal.kids.ui.verification.VerificationRoute

object Routes {
    const val BACKPACK = "backpack"
    const val TEMPLATES = "templates"
    const val COMPARTMENT = "compartment/{templateId}/{category}"
    const val VERIFY = "verify/{templateId}"
    const val HISTORY = "history"
    const val HISTORY_DETAIL = "history/{historyId}"
    const val PARENT = "parent"
    const val PARENT_HISTORY = "parent/history"
    const val PRIVACY = "parent/privacy"
    const val EDITOR = "parent/editor/{templateId}"

    fun compartment(templateId: Long, category: String) = "compartment/$templateId/$category"
    fun verify(templateId: Long) = "verify/$templateId"
    fun historyDetail(id: Long) = "history/$id"
    fun editor(templateId: Long?) = "parent/editor/${templateId ?: -1L}"
}

private fun NavHostController.backToBackpack() {
    if (!popBackStack(Routes.BACKPACK, inclusive = false)) navigate(Routes.BACKPACK)
}

@Composable
fun PackPalNavHost(nav: NavHostController = rememberNavController()) {
    NavHost(navController = nav, startDestination = Routes.BACKPACK) {
        composable(Routes.BACKPACK) {
            BackpackRoute(
                onOpenCompartment = { id, c -> nav.navigate(Routes.compartment(id, c.name)) },
                onOpenTemplates = { nav.navigate(Routes.TEMPLATES) },
                onStartCheck = { id -> nav.navigate(Routes.verify(id)) },
                onOpenHistory = { nav.navigate(Routes.HISTORY) },
                onOpenParents = { nav.navigate(Routes.PARENT) { launchSingleTop = true } },
            )
        }
        composable(Routes.TEMPLATES) { TemplateSelectorRoute(onBack = { nav.popBackStack() }) }
        composable(
            Routes.COMPARTMENT,
            arguments = listOf(navArgument("templateId") { type = NavType.LongType }, navArgument("category") { type = NavType.StringType }),
        ) { CompartmentRoute(onBack = { nav.popBackStack() }) }
        composable(Routes.VERIFY, arguments = listOf(navArgument("templateId") { type = NavType.LongType })) {
            VerificationRoute(onBackToBackpack = { nav.backToBackpack() })
        }
        composable(Routes.HISTORY) { HistoryRoute(onBack = { nav.popBackStack() }, onOpen = { nav.navigate(Routes.historyDetail(it)) }) }
        composable(Routes.HISTORY_DETAIL, arguments = listOf(navArgument("historyId") { type = NavType.LongType })) {
            HistoryDetailRoute(onBack = { nav.popBackStack() })
        }
        composable(Routes.PARENT) {
            ParentHomeRoute(
                onBack = { nav.popBackStack() },
                onEditTemplate = { nav.navigate(Routes.editor(it)) },
                onManageHistory = { nav.navigate(Routes.PARENT_HISTORY) },
                onPrivacy = { nav.navigate(Routes.PRIVACY) },
                onDataCleared = { nav.backToBackpack() },
            )
        }
        composable(Routes.PARENT_HISTORY) { ParentHistoryRoute(onBack = { nav.popBackStack() }) }
        composable(Routes.PRIVACY) { PrivacyRoute(onBack = { nav.popBackStack() }) }
        composable(Routes.EDITOR, arguments = listOf(navArgument("templateId") { type = NavType.LongType })) {
            TemplateEditorRoute(onClose = { nav.popBackStack() })
        }
    }
}
