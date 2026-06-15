package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.launch
import com.example.ui.NavigationDrawerContent
import com.example.ui.ThemeBottomSheet
import com.example.ui.BackupBottomSheet
import com.example.ui.ContactBottomSheet
import com.example.ui.PrivacyPolicyScreen
import com.example.viewmodel.BackupViewModel
import com.example.viewmodel.BackupUiState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Money
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
import com.example.ui.DebtListScreen
import com.example.ui.AddEditDebtScreen
import com.example.viewmodel.DebtViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ExpenseViewModel
import com.example.worker.RecurringExpenseWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val viewModel: ExpenseViewModel by viewModels {
        ExpenseViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val fadeOut = android.view.animation.AlphaAnimation(1f, 0f).apply {
                duration = 500L
                fillAfter = true
            }
            splashScreenView.view.startAnimation(fadeOut)
            splashScreenView.view.postDelayed({
                splashScreenView.remove()
            }, 500L)
        }
        
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val application = context.applicationContext as android.app.Application
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isBangla = appLanguage == "Bangla"

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    var showThemeSheet by remember { mutableStateOf(false) }
    var showBackupSheet by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showContactSheet by remember { mutableStateOf(false) }

    // Backup & Restore Setup at Root level
    val backupViewModel: BackupViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = BackupViewModel.Factory(application)
    )
    val exportState by backupViewModel.exportState.collectAsStateWithLifecycle()
    val importState by backupViewModel.importState.collectAsStateWithLifecycle()

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            backupViewModel.exportData(context, it)
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            backupViewModel.importData(context, it)
        }
    }

    LaunchedEffect(exportState) {
        when (val state = exportState) {
            is BackupUiState.Success -> {
                android.widget.Toast.makeText(context, state.message, android.widget.Toast.LENGTH_LONG).show()
                backupViewModel.resetStates()
            }
            is BackupUiState.Error -> {
                android.widget.Toast.makeText(context, state.errorMsg, android.widget.Toast.LENGTH_LONG).show()
                backupViewModel.resetStates()
            }
            else -> {}
        }
    }

    LaunchedEffect(importState) {
        when (val state = importState) {
            is BackupUiState.Success -> {
                android.widget.Toast.makeText(context, state.message, android.widget.Toast.LENGTH_LONG).show()
                backupViewModel.resetStates()
            }
            is BackupUiState.Error -> {
                android.widget.Toast.makeText(context, state.errorMsg, android.widget.Toast.LENGTH_LONG).show()
                backupViewModel.resetStates()
            }
            else -> {}
        }
    }

    val showBottomBar = currentRoute?.startsWith("add_edit") == false &&
                        currentRoute?.startsWith("add_edit_debt") == false &&
                        currentRoute?.startsWith("settings") == false &&
                        currentRoute != "categories" &&
                        currentRoute != "recurring_expenses" &&
                        currentRoute != "privacy_policy"

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationDrawerContent(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo("dashboard") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    viewModel = viewModel,
                    onCloseDrawer = {
                        coroutineScope.launch { drawerState.close() }
                    },
                    onThemeClick = {
                        showThemeSheet = true
                    },
                    onBackupClick = {
                        showBackupSheet = true
                    },
                    onAboutClick = {
                        showAboutDialog = true
                    },
                    onPrivacyClick = {
                        navController.navigate("privacy_policy") {
                            popUpTo("dashboard") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onContactClick = {
                        showContactSheet = true
                    }
                )
            }
        },
        gesturesEnabled = true
    ) {
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
                        selected = currentRoute == "debt_list",
                        onClick = {
                            if (currentRoute != "debt_list") {
                                navController.navigate("debt_list") {
                                    popUpTo("dashboard")
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == "debt_list") Icons.Filled.Money else Icons.Outlined.Money,
                                contentDescription = "Debts"
                            )
                        },
                        label = { Text(if (isBangla) "ঋণ" else "Debt") }
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
                    },
                    onNavigateToDebt = {
                        navController.navigate("debt_list")
                    },
                    onMenuClick = {
                        coroutineScope.launch { drawerState.open() }
                    }
                )
            }

            composable("shopping") {
                ShoppingListScreen(
                    viewModel = viewModel,
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    },
                    onMenuClick = {
                        coroutineScope.launch { drawerState.open() }
                    }
                )
            }

            composable("summary") {
                SummaryScreen(
                    viewModel = viewModel,
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    },
                    onMenuClick = {
                        coroutineScope.launch { drawerState.open() }
                    }
                )
            }

            composable(
                route = "settings?action={action}",
                arguments = listOf(
                    navArgument("action") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val action = backStackEntry.arguments?.getString("action")
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
                    },
                    initialAction = action
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

            composable("debt_list") {
                val debtViewModel: DebtViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = DebtViewModel.Factory(application)
                )
                DebtListScreen(
                    viewModel = debtViewModel,
                    onNavigateToAddEditDebt = { id ->
                        navController.navigate("add_edit_debt/$id")
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = "add_edit_debt/{debtId}",
                arguments = listOf(
                    navArgument("debtId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val debtId = backStackEntry.arguments?.getLong("debtId") ?: 0L
                val debtViewModel: DebtViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = DebtViewModel.Factory(application)
                )
                AddEditDebtScreen(
                    viewModel = debtViewModel,
                    debtId = debtId,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable("privacy_policy") {
                PrivacyPolicyScreen(
                    onBackClick = {
                        navController.popBackStack()
                    },
                    isBangla = isBangla
                )
            }
        }

        if (showThemeSheet) {
            val themePref by viewModel.themePreference.collectAsStateWithLifecycle()
            ThemeBottomSheet(
                onDismissRequest = { showThemeSheet = false },
                currentTheme = themePref,
                onThemeSelected = { selectedTheme ->
                    viewModel.saveThemePreference(selectedTheme)
                    showThemeSheet = false
                }
            )
        }

        if (showBackupSheet) {
            BackupBottomSheet(
                onDismissRequest = { showBackupSheet = false },
                onExportClick = {
                    val formatter = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                    val dateStr = formatter.format(java.util.Date())
                    createDocumentLauncher.launch("amar_hisab_backup_$dateStr.json")
                    showBackupSheet = false
                },
                onImportClick = {
                    openDocumentLauncher.launch(arrayOf("application/json", "*/*"))
                    showBackupSheet = false
                }
            )
        }

        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                title = {
                    Text(
                        text = if (isBangla) "আমার হিসাব" else "My Calculations",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isBangla) "ভার্সন ১.০.০" else "Version 1.0.0",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isBangla) 
                                "আমার হিসাব অ্যাপটি আপনার দৈনন্দিন ও সাপ্তাহিক বাজার খরচ সহজে পরিচালনা এবং নজরদারি করার জন্য ডিজাইন করা হয়েছে।" 
                                else "Amar Hisab app is designed to easily manage and track your daily and weekly market expenses.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAboutDialog = false }) {
                        Text(if (isBangla) "ঠিক আছে" else "OK")
                    }
                }
            )
        }

        if (showContactSheet) {
            ContactBottomSheet(
                onDismissRequest = { showContactSheet = false },
                isBangla = isBangla
            )
        }

        if (exportState is BackupUiState.Loading || importState is BackupUiState.Loading) {
            Dialog(
                onDismissRequest = {} // Non-cancelable
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .padding(16.dp)
                        .testTag("backup_loader_dialog")
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("backup_progress_indicator")
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isBangla) "ডাটা প্রক্রিয়াকরণ করা হচ্ছে..." else "Processing data...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
}
