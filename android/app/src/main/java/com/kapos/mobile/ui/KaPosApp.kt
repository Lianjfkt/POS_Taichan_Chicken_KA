package com.kapos.mobile.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.kapos.mobile.feature.splash.MotionSplashScreen
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
import com.kapos.mobile.feature.dashboard.DashboardRoute
import com.kapos.mobile.feature.dashboard.DashboardViewModel
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
        PosViewModel(app.posRepository, app.savedOrderRepository, app.inventoryRepository)
    }
    val checkoutViewModel: CheckoutViewModel = viewModel {
        CheckoutViewModel(
            app.posRepository,
            app.settingsRepository,
            app.bluetoothPrinterService,
            app.customerRepository,
            app.promotionRepository
        )
    }
    val customerViewModel: CustomerViewModel = viewModel {
        CustomerViewModel(app.customerRepository, app.settingsRepository)
    }
    val menuManagementViewModel: MenuManagementViewModel = viewModel {
        MenuManagementViewModel(app.posRepository, app.inventoryRepository)
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
        StockViewModel(app.stockRepository, app.posRepository, app.inventoryRepository)
    }
    val analyticsViewModel: AnalyticsViewModel = viewModel {
        AnalyticsViewModel(app.posRepository, app.settingsRepository)
    }
    val dashboardViewModel: DashboardViewModel = viewModel {
        DashboardViewModel(
            posRepository = app.posRepository,
            settingsRepository = app.settingsRepository,
            inventoryRepository = app.inventoryRepository
        )
    }
    val reportViewModel: ReportViewModel = viewModel {
        ReportViewModel(
            repository = app.posRepository,
            settingsRepository = app.settingsRepository,
            shiftRepository = app.shiftRepository,
            bluetoothPrinterService = app.bluetoothPrinterService
        )
    }
    val settingsViewModel: SettingsViewModel = viewModel {
        SettingsViewModel(
            settingsRepository = app.settingsRepository,
            supabaseClient = app.supabaseClient,
            posRepository = app.posRepository,
            bluetoothPrinterService = app.bluetoothPrinterService,
            database = app.database,
            stockRepository = app.stockRepository,
            savedOrderRepository = app.savedOrderRepository
        )
    }

    val destinations = getBottomNavDestinations(currentSession.role)
    val isPrimaryTab = destinations.any { it.route == currentRoute }

    var showSplash by rememberSaveable { mutableStateOf(true) }
    var lastInteractionTime by androidx.compose.runtime.remember { mutableStateOf(System.currentTimeMillis()) }

    androidx.compose.runtime.LaunchedEffect(settings.autoLockMinutes, currentSession.isLoggedIn) {
        if (currentSession.isLoggedIn && settings.autoLockMinutes > 0) {
            val timeoutMillis = settings.autoLockMinutes * 60 * 1000L
            while (true) {
                kotlinx.coroutines.delay(5000L)
                val elapsed = System.currentTimeMillis() - lastInteractionTime
                if (elapsed >= timeoutMillis) {
                    app.settingsRepository.updateSession(
                        currentSession.copy(isLoggedIn = false)
                    )
                    break
                }
            }
        }
    }

    Crossfade(
        targetState = showSplash,
        animationSpec = tween(durationMillis = 500),
        label = "splash_crossfade"
    ) { isSplash ->
        if (isSplash) {
            MotionSplashScreen(
                onSplashFinished = { showSplash = false },
                modifier = modifier.fillMaxSize()
            )
        } else {
            if (!currentSession.isLoggedIn) {
                LoginRoute(
                    viewModel = loginViewModel,
                    onLoginSuccess = {
                        lastInteractionTime = System.currentTimeMillis()
                        navController.navigate("pos") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = true
                            }
                        }
                    },
                    modifier = modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                lastInteractionTime = System.currentTimeMillis()
                            }
                        }
                ) {
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

                        composable("dashboard") {
                            DashboardRoute(viewModel = dashboardViewModel)
                        }

                        composable("report") {
                            ReportRoute(
                                viewModel = reportViewModel,
                                onBack = { navController.popBackStack() }
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
                                },
                                onNavigateToReport = {
                                    navController.navigate("report")
                                },
                                onNavigateToAnalytics = {
                                    navController.navigate("analytics")
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
    }
}
