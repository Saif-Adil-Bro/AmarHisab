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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.AddEditExpenseScreen
import com.example.ui.DashboardScreen
import com.example.ui.ShoppingListScreen
import com.example.ui.SummaryScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ExpenseViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: ExpenseViewModel by viewModels {
        ExpenseViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themePref by viewModel.themePreference.collectAsState()
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

    val appLanguage by viewModel.appLanguage.collectAsState()
    val isBangla = appLanguage == "Bangla"

    val showBottomBar = currentRoute?.startsWith("add_edit") == false

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
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("dashboard") {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToAdd = {
                        navController.navigate("add_edit/0")
                    },
                    onNavigateToEdit = { id ->
                        navController.navigate("add_edit/$id")
                    }
                )
            }

            composable("shopping") {
                ShoppingListScreen(
                    viewModel = viewModel
                )
            }

            composable("summary") {
                SummaryScreen(
                    viewModel = viewModel
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
