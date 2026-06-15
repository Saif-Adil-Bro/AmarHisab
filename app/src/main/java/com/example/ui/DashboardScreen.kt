package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.viewmodel.ReportViewModel
import com.example.data.ExpenseEntity
import com.example.data.CategoryEntity
import com.example.viewmodel.ExpenseViewModel
import com.example.viewmodel.DebtViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ExpenseViewModel,
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToSettings: (() -> Unit)? = null,
    onNavigateToDebt: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null
) {
    val expenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val dailyTotal by viewModel.dailyTotal.collectAsStateWithLifecycle()
    val weeklyTotal by viewModel.weeklyTotal.collectAsStateWithLifecycle()
    val monthlyTotal by viewModel.monthlyTotal.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()

    val defaultCurrency by viewModel.defaultCurrency.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isBangla = appLanguage == "Bangla"
    val customCategories by viewModel.allCategories.collectAsStateWithLifecycle()

    var expenseToDelete by remember { mutableStateOf<ExpenseEntity?>(null) }

    val profileThemeColor = remember(activeProfile) {
        parseHexColor(activeProfile?.colorHex ?: "#6750A4")
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    val reportViewModel: ReportViewModel = viewModel(
        factory = ReportViewModel.Factory(context.applicationContext as android.app.Application)
    )

    val debtViewModel: DebtViewModel = viewModel(
        factory = DebtViewModel.Factory(context.applicationContext as android.app.Application)
    )
    val totalBorrowed by debtViewModel.totalBorrowed.collectAsStateWithLifecycle()
    val totalLent by debtViewModel.totalLent.collectAsStateWithLifecycle()

    var showExportPdfDialog by remember { mutableStateOf(false) }

    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let {
            try {
                val outputStream = context.contentResolver.openOutputStream(it)
                if (outputStream != null) {
                    val profileName = activeProfile?.name ?: "আমার হিসাব"
                    val profileId = activeProfile?.id ?: 1L
                    
                    reportViewModel.generatePdfStream(
                        context = context,
                        profileId = profileId,
                        profileName = profileName,
                        outputStream = outputStream,
                        onSuccess = {
                            android.widget.Toast.makeText(context, "PDF রিপোর্ট সফলভাবে ডাউনলোড করা হয়েছে!", android.widget.Toast.LENGTH_LONG).show()
                        },
                        onError = { error ->
                            android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_LONG).show()
                        }
                    )
                } else {
                    android.widget.Toast.makeText(context, "ফাইল তৈরি করা সম্ভব হয়নি", android.widget.Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(context, "ত্রুটি: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ProfileSwitcherAppBar(
                viewModel = viewModel,
                titleText = "সাপ্তাহিক বাজার",
                onNavigateToSettings = onNavigateToSettings,
                onExportPdf = { showExportPdfDialog = true },
                onMenuClick = onMenuClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = profileThemeColor,
                contentColor = Color.White,
                modifier = Modifier
                    .testTag("add_expense_fab")
                    .padding(bottom = 16.dp, end = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Grocery Expense"
                )
            }
        }
    ) { innerPadding ->
        var isLoading by remember { mutableStateOf(true) }
        LaunchedEffect(customCategories, activeProfile) {
            if (customCategories.isNotEmpty() && activeProfile != null) {
                kotlinx.coroutines.delay(400)
                isLoading = false
            }
        }

        if (isLoading) {
            LoadingAnimation(
                modifier = Modifier.padding(innerPadding),
                message = if (isBangla) "ড্যাশবোর্ড লোড হচ্ছে..." else "Loading dashboard..."
            )
        } else {
            LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                    )
                ),
            contentPadding = PaddingValues(
                // Reduce top padding relative to TopAppBar to make it compact
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 80.dp,
                start = 0.dp,
                end = 0.dp
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Header Calendar Details (no-wrapping)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = remember { SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault()).format(Date()) },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    activeProfile?.let { prof ->
                        val pName = if (prof.name.contains("আমার পকেট")) {
                            if (isBangla) "আমার পকেট" else "Personal"
                        } else if (prof.name.contains("সংসার বাজার")) {
                            if (isBangla) "সংসার বাজার" else "Family"
                        } else if (prof.name.contains("অফিস")) {
                            if (isBangla) "অফিস প্যান্ট্রি" else "Office"
                        } else {
                            prof.name
                        }

                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = parseHexColor(prof.colorHex).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = pName,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = parseHexColor(prof.colorHex),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Big Today's Spending Card (Vibrant Profile-themed Card)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = profileThemeColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            // Top-right background decorative circle
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .offset(x = 60.dp, y = (-60).dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .align(Alignment.TopEnd)
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (isBangla) "আজকের খরচ" else "Today's Expenses",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = Color.White.copy(alpha = 0.85f)
                                )

                                // Formatted price splits dollar/cents like the template
                                val totalSplit = remember(dailyTotal) {
                                    val formatted = String.format(Locale.getDefault(), "%.2f", dailyTotal)
                                    val parts = formatted.split(".")
                                    Pair(parts.getOrNull(0) ?: "0", parts.getOrNull(1) ?: "00")
                                }

                                Row(
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Text(
                                        text = defaultCurrency,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 26.sp
                                        ),
                                        color = Color.White,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    Text(
                                        text = totalSplit.first,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            fontSize = 42.sp
                                        ),
                                        color = Color.White
                                    )
                                    Text(
                                        text = ".${totalSplit.second}",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 22.sp
                                        ),
                                        color = Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier.padding(bottom = 5.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (dailyTotal > 0.0) {
                                            if (isBangla) "আজ বাজার করা হয়েছে" else "Expenses recorded today"
                                        } else {
                                            if (isBangla) "আজ কোনো খরচ রেকর্ড নেই" else "No expenses recorded today"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Monthly Budget Progress Card at the top of the dashboard (below the daily expense card)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
                ) {
                    val budget = activeProfile?.monthlyBudget ?: 0.0
                    val percentage = if (budget > 0.0) (monthlyTotal / budget) * 100.0 else 0.0
                    val progressFraction = if (budget > 0.0) (monthlyTotal / budget).coerceIn(0.0, 1.0).toFloat() else 0f

                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("budget_progress_card"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isBangla) "মাসিক বাজেট অগ্রগতি" else "Monthly Budget Progress",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                if (budget > 0.0) {
                                    val percentString = String.format(Locale.US, "%.0f%%", percentage)
                                    Text(
                                        text = if (isBangla) formatBengaliDigits(percentString) else percentString,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Black
                                        ),
                                        color = when {
                                            percentage < 80.0 -> MaterialTheme.colorScheme.primary
                                            percentage < 100.0 -> Color(0xFFED6C02)
                                            else -> MaterialTheme.colorScheme.error
                                        }
                                    )
                                }
                            }

                            if (budget <= 0.0) {
                                Text(
                                    text = if (isBangla) "আপনি এখনো মাসিক বাজেট সেট করেননি।" else "You haven't set a monthly budget yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                val budgetStr = String.format(Locale.US, "%,.0f", budget)
                                val spentStr = String.format(Locale.US, "%,.0f", monthlyTotal)
                                val progressStr = String.format(Locale.US, "%.0f", percentage)

                                val bStr = if (isBangla) formatBengaliDigits(budgetStr) else budgetStr
                                val sStr = if (isBangla) formatBengaliDigits(spentStr) else spentStr
                                val pStr = if (isBangla) formatBengaliDigits(progressStr) else progressStr

                                Text(
                                    text = if (isBangla) {
                                        "এই মাসের বাজেট: $defaultCurrency$sStr / $defaultCurrency$bStr ($pStr% ব্যবহৃত)"
                                    } else {
                                        "This month: $defaultCurrency$spentStr / $defaultCurrency$budgetStr ($progressStr% used)"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                val indicatorColor = when {
                                    percentage < 80.0 -> MaterialTheme.colorScheme.primary
                                    percentage < 100.0 -> Color(0xFFED6C02)
                                    else -> MaterialTheme.colorScheme.error
                                }

                                LinearProgressIndicator(
                                    progress = { progressFraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(100.dp))
                                        .testTag("budget_progress_indicator"),
                                    color = indicatorColor,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )

                                if (percentage >= 100.0) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (isBangla) "সতর্কতা: আপনার মাসিক বাজেট শেষ!" else "Warning: Your monthly budget has been exceeded!",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Week & Month grid (2 smaller cards side-by-side)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Week Card (Light themed background)
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = profileThemeColor.copy(alpha = 0.08f)),
                        border = BorderStroke(1.dp, profileThemeColor.copy(alpha = 0.15f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = if (isBangla) "এই সপ্তাহ" else "This Week",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                ),
                                color = profileThemeColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(Locale.getDefault(), "%s%.2f", defaultCurrency, weeklyTotal),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Month Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = if (isBangla) "এই মাস" else "This Month",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(Locale.getDefault(), "%s%.2f", defaultCurrency, monthlyTotal),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Debt Summary Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
                        .clickable { onNavigateToDebt?.invoke() }
                        .testTag("dashboard_debt_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Money,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isBangla) "কর্জ ও ঋণ হিসাব" else "Debts & Borrowing",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Total Borrowed
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = if (isBangla) "আমার ঋণ ( ধার নেওয়া )" else "Total Borrowed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$defaultCurrency ${String.format(Locale.getDefault(), "%.1f", totalBorrowed)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFC62828)
                                )
                            }

                            // Total Lent
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        Color(0xFFE8F5E9), // Light green background
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = if (isBangla) "আমার পাওনা ( ধার দেওয়া )" else "Total Lent",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF1B5E20),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$defaultCurrency ${String.format(Locale.getDefault(), "%.1f", totalLent)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }
            }

            // Recent Transactions List Heading (uppercase tracking-widest style)
            item {
                Text(
                    text = if (isBangla) "সাম্প্রতিক খরচ" else "Recent Expenses",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.0.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 22.dp, top = 12.dp, end = 22.dp, bottom = 4.dp)
                )
            }

            if (expenses.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp, horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                modifier = Modifier.size(72.dp),
                                shape = CircleShape,
                                color = profileThemeColor.copy(alpha = 0.1f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        tint = profileThemeColor,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (isBangla) "কোনো খরচ পাওয়া যায়নি" else "No expenses found",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isBangla) "নিচের (+) বাটনে চাপ দিয়ে প্রথম এন্ট্রি করুন।" else "Tap the (+) button below to add your first expense.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            } else {
                items(items = expenses, key = { it.id }) { expense ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        ExpenseItemCard(
                            expense = expense,
                            isBangla = isBangla,
                            onEdit = { onNavigateToEdit(expense.id) },
                            onDelete = { expenseToDelete = expense },
                            customCategories = customCategories
                        )
                    }
                }
            }
        }
    }

    expenseToDelete?.let { expense ->
        DeleteConfirmationDialog(
            isBangla = isBangla,
            onDismiss = { expenseToDelete = null },
            onConfirm = {
                viewModel.deleteExpense(expense)
                expenseToDelete = null
            }
        )
    }

    val reportStartDate by reportViewModel.startDate.collectAsStateWithLifecycle()
    val reportEndDate by reportViewModel.endDate.collectAsStateWithLifecycle()
    val reportPreset by reportViewModel.selectedPreset.collectAsStateWithLifecycle()
    val reportIsGenerating by reportViewModel.isGenerating.collectAsStateWithLifecycle()

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    if (showExportPdfDialog) {
        val bnLocale = remember { Locale("bn", "BD") }
        val rangeFormatter = remember { SimpleDateFormat("dd MMMM yyyy", bnLocale) }
        
        AlertDialog(
            onDismissRequest = { showExportPdfDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "পিডিএফ রিপোর্ট এক্সপোর্ট",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "রিপোর্টের জন্য সময়কাল নির্বাচন করুন:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Chips Row
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "THIS_MONTH" to "এই মাস",
                                "LAST_7_DAYS" to "গত ৭ দিন"
                            ).forEach { (presetVal, label) ->
                                FilterChip(
                                    selected = reportPreset == presetVal,
                                    onClick = { reportViewModel.setPreset(presetVal) },
                                    label = { Text(label, fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "LAST_30_DAYS" to "গত ৩০ দিন",
                                "ALL_TIME" to "সব সময়"
                            ).forEach { (presetVal, label) ->
                                FilterChip(
                                    selected = reportPreset == presetVal,
                                    onClick = { reportViewModel.setPreset(presetVal) },
                                    label = { Text(label, fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    // Start Date & End Date Selector trigger
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "কাস্টম সময়সীমা:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Start Date Card
                            OutlinedCard(
                                onClick = { showStartDatePicker = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("শুরুর তারিখ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = rangeFormatter.format(Date(reportStartDate)),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                            
                            // End Date Card
                            OutlinedCard(
                                onClick = { showEndDatePicker = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("শেষের তারিখ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = rangeFormatter.format(Date(reportEndDate)),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                    
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "নির্বাচিত: ${rangeFormatter.format(Date(reportStartDate))} হতে ${rangeFormatter.format(Date(reportEndDate))}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExportPdfDialog = false
                        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                        createPdfLauncher.launch("amar_hisab_report_$dateStr.pdf")
                    },
                    shape = RoundedCornerShape(12.dp),
                    enabled = !reportIsGenerating,
                    modifier = Modifier.testTag("pdf_download_confirm")
                ) {
                    if (reportIsGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("রিপোর্ট ডাউনলোড")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportPdfDialog = false }) {
                    Text("বাতিল")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = reportStartDate
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            reportViewModel.setCustomStartDate(it)
                        }
                        showStartDatePicker = false
                    }
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = reportEndDate
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            reportViewModel.setCustomEndDate(it)
                        }
                        showEndDatePicker = false
                    }
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    }
}

@Composable
fun ExpenseItemCard(
    expense: ExpenseEntity,
    isBangla: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    customCategories: List<CategoryEntity> = emptyList(),
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
    val formattedDate = remember(expense.date) { dateFormatter.format(Date(expense.date)) }

    val localizedCategory = remember(expense.category, isBangla, customCategories) {
        val customMatch = customCategories.find { it.name == expense.category }
        if (customMatch != null) {
            customMatch.name
        } else if (isBangla) {
            when (expense.category) {
                "Vegetables" -> "সবজি"
                "Meat" -> "মাংস"
                "Grocery" -> "মুদিখানা"
                "Dairy/Eggs" -> "দুধ/ডিম"
                "Fruits" -> "ফলমূল"
                "Beverages" -> "পানীয়"
                "Snacks" -> "নাস্তা"
                "Others" -> "অন্যান্য"
                else -> expense.category
            }
        } else expense.category
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .testTag("expense_item_${expense.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Category Emoji Badge & details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = getCategoryColor(expense.category, customCategories).copy(alpha = 0.12f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = getCategoryEmoji(expense.category, customCategories),
                            fontSize = 24.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expense.itemName,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = localizedCategory,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = getCategoryColor(expense.category, customCategories),
                            maxLines = 1
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }

            // Price & Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = String.format(Locale.getDefault(), "-%s%.1f", expense.currency, expense.price),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    ),
                    color = Color(0xFFB3261E),
                    modifier = Modifier.padding(end = 4.dp)
                )

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("edit_expense_button_${expense.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Expense",
                        tint = Color(0xFF6750A4),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("delete_expense_button_${expense.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Expense",
                        tint = Color(0xFFB3261E),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

fun formatBengaliDigits(input: String): String {
    val englishDigits = listOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
    val bengaliDigits = listOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    return input.map { char ->
        val index = englishDigits.indexOf(char)
        if (index != -1) bengaliDigits[index] else char
    }.joinToString("")
}
