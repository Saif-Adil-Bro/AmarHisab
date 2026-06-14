package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.*
import com.example.ui.AddEditExpenseScreen
import com.example.ui.DashboardScreen
import com.example.ui.ShoppingListScreen
import com.example.ui.SummaryScreen
import com.example.ui.SettingsScreen
import com.example.ui.ManageCategoriesScreen
import com.example.ui.RecurringExpensesScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ExpenseViewModel
import com.example.worker.RecurringExpenseWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val viewModel: ExpenseViewModel by viewModels {
        ExpenseViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            val workRequest = PeriodicWorkRequestBuilder<RecurringExpenseWorker>(1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .build()

            WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                "RecurringExpenseWorker",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        enableEdgeToEdge()
        setContent {
            val themePref by viewModel.themePreference.collectAsStateWithLifecycle()
            val darkTheme = when (themePref) {
                "Light" -> false
                "Dark" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            MyApplicationTheme(darkTheme = darkTheme) {
                MainAppLayout(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppLayout(viewModel: ExpenseViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isBangla = appLanguage == "Bangla"

    val showBottomBar = currentRoute?.startsWith("add_edit") == false &&
                        currentRoute != "settings" &&
                        currentRoute != "categories" &&
                        currentRoute != "recurring_expenses"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    tonalElevation = 8.dp,
                    modifier = Modifier.drawWithContent {
                        drawContent()
                        drawLine(
                            color = Color.Black.copy(alpha = 0.08f),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                ) {
                    NavigationBarItem(
                        selected = currentRoute == "dashboard",
                        onClick = {
                            if (currentRoute != "dashboard") {
                                navController.navigate("dashboard") {
                                    popUpTo("dashboard") { inclusive = true }
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == "dashboard") Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                                contentDescription = "Dashboard"
                            )
                        },
                        label = { Text(if (isBangla) "ড্যাশবোর্ড" else "Dashboard") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "shopping",
                        onClick = {
                            if (currentRoute != "shopping") {
                                navController.navigate("shopping") {
                                    popUpTo("dashboard")
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == "shopping") Icons.Filled.ListAlt else Icons.Outlined.ListAlt,
                                contentDescription = "Shopping List"
                            )
                        },
                        label = { Text(if (isBangla) "বাজারের ফর্দ" else "Shopping List") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "summary",
                        onClick = {
                            if (currentRoute != "summary") {
                                navController.navigate("summary") {
                                    popUpTo("dashboard")
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == "summary") Icons.Filled.PieChart else Icons.Outlined.PieChart,
                                contentDescription = "Summary"
                            )
                        },
                        label = { Text(if (isBangla) "বিশ্লেষণ" else "Summary") }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(220)) },
            exitTransition = { fadeOut(animationSpec = tween(220)) },
            popEnterTransition = { fadeIn(animationSpec = tween(220)) },
            popExitTransition = { fadeOut(animationSpec = tween(220)) }
        ) {
            composable("dashboard") {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToAdd = {
                        navController.navigate("add_edit/0")
                    },
                    onNavigateToEdit = { id ->
                        navController.navigate("add_edit/$id")
                    },
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    }
                )
            }

            composable("shopping") {
                ShoppingListScreen(
                    viewModel = viewModel,
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    }
                )
            }

            composable("summary") {
                SummaryScreen(
                    viewModel = viewModel,
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    }
                )
            }

            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToCategories = {
                        navController.navigate("categories")
                    },
                    onNavigateToRecurring = {
                        navController.navigate("recurring_expenses")
                    }
                )
            }

            composable("categories") {
                ManageCategoriesScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable("recurring_expenses") {
                RecurringExpensesScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = "add_edit/{expenseId}",
                arguments = listOf(
                    navArgument("expenseId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val expenseId = backStackEntry.arguments?.getLong("expenseId") ?: 0L
                AddEditExpenseScreen(
                    viewModel = viewModel,
                    expenseId = expenseId,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
