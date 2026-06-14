package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ExpenseEntity
import com.example.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier,
    onNavigateToSettings: (() -> Unit)? = null
) {
    val expenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()

    val defaultCurrency by viewModel.defaultCurrency.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isBangla = appLanguage == "Bangla"

    val profileThemeColor = remember(activeProfile) {
        parseHexColor(activeProfile?.colorHex ?: "#6750A4")
    }

    // Calculate current month calendar constraints
    val calendar = remember { Calendar.getInstance() }
    val currentMonthName = remember {
        calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: "Current Month"
    }
    val currentYear = remember { calendar.get(Calendar.YEAR) }

    val localizedMonthName = remember(currentMonthName, isBangla) {
        if (isBangla) {
            when (currentMonthName.lowercase()) {
                "january" -> "জানুয়ারি"
                "february" -> "ফেব্রুয়ারি"
                "march" -> "মার্চ"
                "april" -> "এপ্রিল"
                "may" -> "মে"
                "june" -> "জুন"
                "july" -> "জুলাই"
                "august" -> "আগস্ট"
                "september" -> "সেপ্টেম্বর"
                "october" -> "অক্টোবর"
                "november" -> "নভেম্বর"
                "december" -> "ডিসেম্বর"
                else -> currentMonthName
            }
        } else {
            currentMonthName
        }
    }

    // Filter expenses directly for reactivity
    val currentMonthExpenses = remember(expenses) {
        val currentMonthIdx = calendar.get(Calendar.MONTH)
        val currentYearIdx = calendar.get(Calendar.YEAR)
        expenses.filter { exp ->
            val expenseCal = Calendar.getInstance().apply { timeInMillis = exp.date }
            expenseCal.get(Calendar.MONTH) == currentMonthIdx && expenseCal.get(Calendar.YEAR) == currentYearIdx
        }
    }

    val convertToDefaultCurrency = remember(defaultCurrency) {
        { price: Double, fromCurrency: String ->
            // Convert to BDT baseline first
            val bdtBaseline = when (fromCurrency) {
                "৳" -> price
                "$" -> price * 120.0
                "£" -> price * 150.0
                "₹" -> price * 1.4
                else -> price
            }
            // Now convert from BDT baseline to target currency pref
            when (defaultCurrency) {
                "৳" -> bdtBaseline
                "$" -> bdtBaseline / 120.0
                "£" -> bdtBaseline / 150.0
                "₹" -> bdtBaseline / 1.4
                else -> bdtBaseline
            }
        }
    }

    // Process breakdown by Category
    val categoryBreakdown = remember(currentMonthExpenses, convertToDefaultCurrency) {
        currentMonthExpenses.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { convertToDefaultCurrency(it.price, it.currency) } }
            .toList()
            .sortedByDescending { it.second }
    }

    val totalExpense = remember(categoryBreakdown) {
        categoryBreakdown.sumOf { it.second }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ProfileSwitcherAppBar(
                viewModel = viewModel,
                titleText = "মাসিক বিশ্লেষণ",
                onNavigateToSettings = onNavigateToSettings
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                        )
                    )
                )
        ) {
            // Title Header with active profile
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 20.dp, end = 24.dp, bottom = 12.dp)
            ) {
                Text(
                    text = if (isBangla) "$localizedMonthName $currentYear ওভারভিউ" else "$localizedMonthName $currentYear Overview",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        fontSize = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (isBangla) "সংসার খরচের অনুপাত বিশ্লেষণ" else "Proportional analysis of pantry expenses",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (currentMonthExpenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape,
                            color = profileThemeColor.copy(alpha = 0.1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PieChart,
                                    contentDescription = null,
                                    tint = profileThemeColor,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isBangla) "এই মাসে কোনো খরচ নেই" else "No expenses recorded this month",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isBangla) {
                                "বিশ্লেষণ দেখার জন্য প্রথমে ড্যাশবোর্ডে গিয়ে নতুন বাজার বা খরচ রেকর্ড করুন।"
                            } else {
                                "To view monthly analytics, log some expenses or add grocery shopping list entries first."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Modern styled Total box
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, Color(0xFFF3EDF7)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(22.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (isBangla) "এই মাসের মোট খরচ" else "This Month's Total Expenses",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = String.format(Locale.getDefault(), "%s%.2f", defaultCurrency, totalExpense),
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-1).sp,
                                        fontSize = 32.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isBangla) {
                                        "মোট ${currentMonthExpenses.size} টি বাজার বা এন্ট্রি যোগ করা হয়েছে"
                                    } else {
                                        "Total ${currentMonthExpenses.size} entries have been recorded"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Card with informational prompt
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = profileThemeColor.copy(alpha = 0.05f)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, profileThemeColor.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = profileThemeColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isBangla) {
                                        "সব মুদ্রা স্বয়ংক্রিয়ভাবে $defaultCurrency -এ রূপান্তর করা হয়েছে।"
                                    } else {
                                        "All transactions have been automatically converted to $defaultCurrency."
                                    },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = profileThemeColor
                                )
                            }
                        }
                    }

                    // Section header
                    item {
                        Text(
                            text = if (isBangla) "শ্রেণীভিত্তিক বিশ্লেষণ" else "Spending Breakdown",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 6.dp, top = 8.dp)
                        )
                    }

                    // Category progress bars
                    items(categoryBreakdown) { (cat, cost) ->
                        val pct = if (totalExpense > 0.0) cost / totalExpense else 0.0
                        val percentageText = String.format(Locale.getDefault(), "%.1f%%", pct * 100)
                        val formattedCost = String.format(Locale.getDefault(), "%s%.2f", defaultCurrency, cost)

                        val localizedCat = if (isBangla) {
                            when (cat) {
                                "Vegetables" -> "সবজি"
                                "Meat" -> "মাংস"
                                "Grocery" -> "মুদিখানা"
                                "Dairy/Eggs" -> "দুধ/ডিম"
                                "Fruits" -> "ফলমূল"
                                "Beverages" -> "পানীয়"
                                "Snacks" -> "নাস্তা"
                                "Others" -> "অন্যান্য"
                                else -> cat
                            }
                        } else cat

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("summary_category_$cat"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = getCategoryColor(cat).copy(alpha = 0.12f),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = getCategoryEmoji(cat),
                                                    fontSize = 18.sp
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = localizedCat,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = formattedCost,
                                            fontWeight = FontWeight.Black,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = percentageText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Custom styled visual progress indicator bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(fraction = pct.toFloat())
                                            .clip(RoundedCornerShape(100.dp))
                                            .background(getCategoryColor(cat))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
