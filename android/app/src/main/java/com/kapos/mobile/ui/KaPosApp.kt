package com.kapos.mobile.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kapos.mobile.KaPosApplication
import com.kapos.mobile.core.components.AppBottomNav
import com.kapos.mobile.core.components.getBottomNavDestinations
import com.kapos.mobile.domain.model.AuthSession
import com.kapos.mobile.domain.model.UserRole
import com.kapos.mobile.feature.analytics.AnalyticsRoute
import com.kapos.mobile.feature.analytics.AnalyticsViewModel
import com.kapos.mobile.feature.auth.LoginRoute
import com.kapos.mobile.feature.auth.LoginViewModel
import com.kapos.mobile.feature.checkout.CheckoutViewModel
import com.kapos.mobile.feature.checkout.ConfirmSaleRoute
import com.kapos.mobile.feature.pos.PosRoute
import com.kapos.mobile.feature.pos.PosViewModel
import com.kapos.mobile.feature.report.ReportRoute
import com.kapos.mobile.feature.report.ReportViewModel
import com.kapos.mobile.feature.settings.SettingsRoute
import com.kapos.mobile.feature.settings.SettingsViewModel
import com.kapos.mobile.feature.customer.CustomerRoute
import com.kapos.mobile.feature.customer.CustomerViewModel
import com.kapos.mobile.feature.menu.MenuManagementRoute
import com.kapos.mobile.feature.menu.MenuManagementViewModel
import com.kapos.mobile.feature.shift.ShiftRoute
import com.kapos.mobile.feature.shift.ShiftViewModel
import com.kapos.mobile.feature.stock.StockRoute
import com.kapos.mobile.feature.stock.StockViewModel

@Composable
fun KaPosApp(
    app: KaPosApplication,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "pos"

    val settings by app.settingsRepository.settingsFlow.collectAsState()
    val currentSession = settings.currentSession

    // Shared ViewModels
    val loginViewModel: LoginViewModel = viewModel {
        LoginViewModel(app.settingsRepository)
    }
    val posViewModel: PosViewModel = viewModel {
        PosViewModel(app.posRepository, app.savedOrderRepository)
    }
    val checkoutViewModel: CheckoutViewModel = viewModel {
        CheckoutViewModel(
            app.posRepository,
            app.settingsRepository,
            app.bluetoothPrinterService,
            app.customerRepository
        )
    }
    val customerViewModel: CustomerViewModel = viewModel {
        CustomerViewModel(app.customerRepository)
    }
    val menuManagementViewModel: MenuManagementViewModel = viewModel {
        MenuManagementViewModel(app.posRepository)
    }
    val transactionsViewModel: com.kapos.mobile.feature.transactions.TransactionsViewModel = viewModel {
        com.kapos.mobile.feature.transactions.TransactionsViewModel(
            app.posRepository,
            app.settingsRepository,
            app.bluetoothPrinterService
        )
    }
    val shiftViewModel: ShiftViewModel = viewModel {
        ShiftViewModel(
            shiftRepository = app.shiftRepository,
            settingsRepository = app.settingsRepository,
            posRepository = app.posRepository,
            bluetoothPrinterService = app.bluetoothPrinterService
        )
    }
    val stockViewModel: StockViewModel = viewModel {
        StockViewModel(app.stockRepository, app.posRepository)
    }
    val analyticsViewModel: AnalyticsViewModel = viewModel {
        AnalyticsViewModel(app.posRepository, app.settingsRepository)
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

    val destinations = getBottomNavDestinations(currentSession.role)
    val isPrimaryTab = destinations.any { it.route == currentRoute }

    if (!currentSession.isLoggedIn) {
        LoginRoute(
            viewModel = loginViewModel,
            onLoginSuccess = {
                navController.navigate("pos") {
                    popUpTo(navController.graph.findStartDestination().id) {
                        inclusive = true
                    }
                }
            },
            modifier = modifier.fillMaxSize()
        )
    } else {
        Box(modifier = modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = "pos",
                modifier = Modifier.fillMaxSize()
            ) {
                composable("pos") {
                    PosRoute(
                        viewModel = posViewModel,
                        userRole = currentSession.role,
                        onLockClick = {
                            app.settingsRepository.updateSession(
                                currentSession.copy(isLoggedIn = false)
                            )
                        },
                        onLogoutClick = {
                            app.settingsRepository.updateSession(
                                AuthSession(isLoggedIn = false, role = UserRole.CASHIER, cashierName = "")
                            )
                        },
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

                composable("stock") {
                    StockRoute(viewModel = stockViewModel)
                }

                composable("analytics") {
                    AnalyticsRoute(viewModel = analyticsViewModel)
                }

                composable("report") {
                    ReportRoute(
                        viewModel = reportViewModel
                    )
                }

                composable("settings") {
                    SettingsRoute(
                        viewModel = settingsViewModel,
                        onLockClick = {
                            app.settingsRepository.updateSession(
                                currentSession.copy(isLoggedIn = false)
                            )
                        },
                        onLogoutClick = {
                            app.settingsRepository.updateSession(
                                AuthSession(isLoggedIn = false, role = UserRole.CASHIER, cashierName = "")
                            )
                        },
                        onNavigateToMenuManagement = {
                            navController.navigate("menu_management")
                        },
                        onNavigateToCustomers = {
                            navController.navigate("customers")
                        }
                    )
                }

                composable("menu_management") {
                    MenuManagementRoute(
                        viewModel = menuManagementViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable("customers") {
                    CustomerRoute(
                        viewModel = customerViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            // Floating Bottom Navigation (only visible on the role's primary tabs)
            if (isPrimaryTab) {
                AppBottomNav(
                    currentRoute = currentRoute,
                    role = currentSession.role,
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
}
