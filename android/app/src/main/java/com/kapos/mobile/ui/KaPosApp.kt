package com.kapos.mobile.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kapos.mobile.KaPosApplication
import com.kapos.mobile.core.components.AppBottomNav
import com.kapos.mobile.core.components.bottomNavDestinations
import com.kapos.mobile.feature.checkout.CheckoutViewModel
import com.kapos.mobile.feature.checkout.ConfirmSaleRoute
import com.kapos.mobile.feature.pos.PosRoute
import com.kapos.mobile.feature.pos.PosViewModel
import com.kapos.mobile.feature.report.ReportRoute
import com.kapos.mobile.feature.report.ReportViewModel
import com.kapos.mobile.feature.settings.SettingsRoute
import com.kapos.mobile.feature.settings.SettingsViewModel
import com.kapos.mobile.feature.shift.ShiftRoute
import com.kapos.mobile.feature.shift.ShiftViewModel

@Composable
fun KaPosApp(
    app: KaPosApplication,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "pos"

    // Shared ViewModels
    val posViewModel: PosViewModel = viewModel {
        PosViewModel(app.posRepository, app.savedOrderRepository)
    }
    val checkoutViewModel: CheckoutViewModel = viewModel {
        CheckoutViewModel(app.posRepository, app.settingsRepository, app.bluetoothPrinterService)
    }
    val transactionsViewModel: com.kapos.mobile.feature.transactions.TransactionsViewModel = viewModel {
        com.kapos.mobile.feature.transactions.TransactionsViewModel(
            app.posRepository,
            app.settingsRepository,
            app.bluetoothPrinterService
        )
    }
    val shiftViewModel: ShiftViewModel = viewModel {
        ShiftViewModel(app.shiftRepository, app.settingsRepository)
    }
    val reportViewModel: ReportViewModel = viewModel {
        ReportViewModel(app.posRepository, app.settingsRepository)
    }
    val settingsViewModel: SettingsViewModel = viewModel {
        SettingsViewModel(
            settingsRepository = app.settingsRepository,
            supabaseClient = app.supabaseClient,
            posRepository = app.posRepository,
            bluetoothPrinterService = app.bluetoothPrinterService,
            database = app.database
        )
    }

    val isPrimaryTab = bottomNavDestinations.any { it.route == currentRoute }

    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = "pos",
            modifier = Modifier.fillMaxSize()
        ) {
            composable("pos") {
                PosRoute(
                    viewModel = posViewModel,
                    onNavigateToCheckout = {
                        val cartItems = posViewModel.uiState.value.cartItems
                        checkoutViewModel.initCheckout(cartItems)
                        navController.navigate("checkout")
                    }
                )
            }

            composable("checkout") {
                ConfirmSaleRoute(
                    viewModel = checkoutViewModel,
                    onBack = { navController.popBackStack() },
                    onSuccessFinish = {
                        posViewModel.clearCart()
                        navController.navigate("transactions") {
                            popUpTo("pos")
                        }
                    }
                )
            }

            composable("transactions") {
                com.kapos.mobile.feature.transactions.TransactionsRoute(
                    viewModel = transactionsViewModel
                )
            }

            composable("shift") {
                ShiftRoute(viewModel = shiftViewModel)
            }

            composable("report") {
                ReportRoute(
                    viewModel = reportViewModel
                )
            }

            composable("settings") {
                SettingsRoute(
                    viewModel = settingsViewModel
                )
            }
        }

        // Floating Bottom Navigation (only visible on the 4 primary tabs)
        if (isPrimaryTab) {
            AppBottomNav(
                currentRoute = currentRoute,
                onNavigate = { destinationRoute ->
                    if (currentRoute != destinationRoute) {
                        navController.navigate(destinationRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}


