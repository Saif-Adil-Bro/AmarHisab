package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Debt
import com.example.viewmodel.DebtViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtListScreen(
    viewModel: DebtViewModel,
    onNavigateToAddEditDebt: (Long) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    val debts by viewModel.allDebts.collectAsStateWithLifecycle()
    val currency by viewModel.defaultCurrency.collectAsStateWithLifecycle()

    val totalBorrowed by viewModel.totalBorrowed.collectAsStateWithLifecycle()
    val totalLent by viewModel.totalLent.collectAsStateWithLifecycle()
    val netBalance by viewModel.netBalance.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableStateOf(0) } // 0 = Borrowed (নেওয়া), 1 = Lent (দেওয়া)
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("All") } // "All", "unpaid", "partial", "paid"

    // Dialog state
    var showPaymentDialogForDebt by remember { mutableStateOf<Debt?>(null) }
    var showDeleteConfirmForDebt by remember { mutableStateOf<Debt?>(null) }

    // Tab titles
    val tabTitles = listOf("ঋণ নেওয়া (Borrowed)", "ঋণ দেওয়া (Lent)")

    // Categorized and filtered list
    val currentType = if (selectedTabIndex == 0) "borrowed" else "lent"
    val filteredDebts = remember(debts, currentType, searchQuery, statusFilter) {
        debts.filter { debt ->
            debt.type == currentType &&
            (searchQuery.isBlank() || debt.personName.contains(searchQuery, ignoreCase = true) || (debt.notes?.contains(searchQuery, ignoreCase = true) ?: false)) &&
            (statusFilter == "All" || debt.status == statusFilter)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        text = "কর্জ ও ঋণ ট্র্যাকার",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "ফিরে যান"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToAddEditDebt(0L) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "ঋণ যোগ করুন")
            }
        }
    ) { innerPadding ->
        var isLoading by remember { mutableStateOf(true) }
        LaunchedEffect(debts) {
            if (debts.isNotEmpty() || true) {
                kotlinx.coroutines.delay(400)
                isLoading = false
            }
        }

        if (isLoading) {
            LoadingAnimation(
                modifier = Modifier.padding(innerPadding),
                message = "ঋণ ও কর্জের তালিকা লোড হচ্ছে..."
            )
        } else {
            Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Net Debt and Summary Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Net balance indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "বর্তমান নেট ব্যালেন্স",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val balanceColor = if (netBalance >= 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                        Text(
                            text = "$currency ${String.format(Locale.US, "%.1f", netBalance)}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = balanceColor
                        )
                    }

                    Divider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Borrowed Box
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = "নেওয়া",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "মোট ঋণ ( borrowed )",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "$currency ${String.format(Locale.US, "%.1f", totalBorrowed)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        // Lent Box
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "দেওয়া",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "মোট পাওনা ( lent )",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "$currency ${String.format(Locale.US, "%.1f", totalLent)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Tab Rows
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    )
                }
            }

            // Search Bar & Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ব্যক্তির নামে খুঁজুন...", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "খুঁজুন", modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "ক্লিয়ার", modifier = Modifier.size(20.dp))
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.weight(1.3f)
                )

                // Quick Status Select Filter
                var expandedFilter by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(0.7f)) {
                    Button(
                        onClick = { expandedFilter = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        val filterText = when (statusFilter) {
                            "unpaid" -> "বাকি"
                            "partial" -> "আংশিক"
                            "paid" -> "পরিশোধিত"
                            else -> "ফিল্টার"
                        }
                        Text(filterText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Icon(imageVector = Icons.Default.FilterList, contentDescription = "ফিল্টার", modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(
                        expanded = expandedFilter,
                        onDismissRequest = { expandedFilter = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("সব (All)") },
                            onClick = {
                                statusFilter = "All"
                                expandedFilter = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("অপরিশোধিত") },
                            onClick = {
                                statusFilter = "unpaid"
                                expandedFilter = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("আংশিক") },
                            onClick = {
                                statusFilter = "partial"
                                expandedFilter = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("পরিশোধিত") },
                            onClick = {
                                statusFilter = "paid"
                                expandedFilter = false
                            }
                        )
                    }
                }
            }

            // List of debts
            if (filteredDebts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoneyOff,
                            contentDescription = "খালি",
                            tint = Color.LightGray,
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "কোনো ঋণের রেকর্ড খুঁজে পাওয়া যায়নি।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredDebts, key = { it.id }) { debt ->
                        DebtItemCard(
                            debt = debt,
                            currency = currency,
                            onEdit = { onNavigateToAddEditDebt(debt.id) },
                            onDelete = { showDeleteConfirmForDebt = debt },
                            onPay = { showPaymentDialogForDebt = debt },
                            onShare = { shareDebtDetails(debt, currency, context) }
                        )
                    }
                }
            }
        }
    }

    // Payment Dialog Component
    if (showPaymentDialogForDebt != null) {
        val activeDebt = showPaymentDialogForDebt!!
        var payAmountStr by remember { mutableStateOf("") }
        var payInputError by remember { mutableStateOf<String?>(null) }
        val remainingToPay = activeDebt.amount - activeDebt.paidAmount

        // Auto pre-population helper
        LaunchedEffect(activeDebt) {
            payAmountStr = String.format(Locale.US, "%.1f", remainingToPay)
        }

        AlertDialog(
            onDismissRequest = { showPaymentDialogForDebt = null },
            title = {
                Text(
                    text = "পরিশোধ সংরক্ষণ (${activeDebt.personName})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "মোট কর্জ: $currency ${activeDebt.amount}")
                    Text(text = "ইতিপূর্বে পরিশোধিত: $currency ${activeDebt.paidAmount}")
                    Text(
                        text = "বাকি আছে: $currency ${String.format(Locale.US, "%.1f", remainingToPay)}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = payAmountStr,
                        onValueChange = {
                            if (it.isEmpty() || it.toDoubleOrNull() != null) {
                                payAmountStr = it
                                payInputError = null
                            }
                        },
                        label = { Text("নতুন পেমেন্ট পরিমাণ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = payInputError != null,
                        modifier = Modifier.fillMaxWidth()
                      )
                    if (payInputError != null) {
                        Text(
                            text = payInputError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amountToPay = payAmountStr.toDoubleOrNull()
                        if (amountToPay == null || amountToPay <= 0.0) {
                            payInputError = "সঠিক পরিমাণ টাকা লিখুন!"
                        } else {
                            viewModel.recordPayment(activeDebt, amountToPay)
                            showPaymentDialogForDebt = null
                        }
                    }
                ) {
                    Text("সংরক্ষণ", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentDialogForDebt = null }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirmForDebt != null) {
        val target = showDeleteConfirmForDebt!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmForDebt = null },
            title = { Text("মুছে ফেলার নিশ্চায়ন", fontWeight = FontWeight.Bold) },
            text = { Text("আপনি কি নিশ্চিতভাবে \"${target.personName}\"-এর এই লেনা-দেনা রেকর্ডটি চিরতরে মুছে ফেলতে চান?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDebt(target)
                        showDeleteConfirmForDebt = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("মুছে ফেলুন", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmForDebt = null }) {
                    Text("বাতিল")
                }
            }
        )
    }
    }
}

@Composable
fun DebtItemCard(
    debt: Debt,
    currency: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPay: () -> Unit,
    onShare: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val isUnpaid = debt.status == "unpaid"
    val isPartial = debt.status == "partial"
    val isPaid = debt.status == "paid"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Main Top Row name & badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (debt.type == "borrowed") Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = if (debt.type == "borrowed") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = debt.personName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (!debt.contactNumber.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = debt.contactNumber,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }

                // Styled State Badge
                val badgeColor = when (debt.status) {
                    "paid" -> Color(0xFF2E7D32) // Soft Green
                    "partial" -> Color(0xFFE65100) // Dark Orange
                    else -> Color(0xFFC62828) // Deep Red
                }
                val badgeText = when (debt.status) {
                    "paid" -> "পরিশোধিত"
                    "partial" -> "আংশিক"
                    else -> "বাকি"
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            // Amount, progress tracker
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "টাকার পরিমাণ: $currency ${debt.amount}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (debt.paidAmount > 0.0) {
                        Text(
                            text = "পরিশোধিত: $currency ${debt.paidAmount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }

                if (debt.dueDate != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "সীমা তারিখ",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = dateFormatter.format(Date(debt.dueDate)),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Progress bar
            if (isPartial || (debt.paidAmount > 0.0 && !isPaid)) {
                Spacer(modifier = Modifier.height(10.dp))
                val progress = (debt.paidAmount / debt.amount).toFloat().coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFFE65100),
                    trackColor = Color(0xFFE0E0E0)
                )
            }

            // Notes Section
            if (!debt.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(8.dp)
                ) {
                    Row {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = debt.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )
                    }
                }
            }

            Divider(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

            // Action triggers row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Secondary triggers (Edit/Delete)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "সম্পাদনা",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "মুছুন",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onShare) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "শেয়ার",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Payment Action Button
                if (!isPaid) {
                    Button(
                        onClick = onPay,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (debt.type == "borrowed") Color(0xFFC62828) else Color(0xFF2E7D32)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        val paybackText = if (debt.type == "borrowed") "টাকা ফেরত দিন" else "টাকা সংগ্রহ করুন"
                        Text(paybackText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "সম্পূর্ণ পরিশোধিত",
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "পরিশোধিত",
                            color = Color(0xFF2E7D32),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

// Share helpers using sms or standard sharing chooser
fun shareDebtDetails(debt: Debt, currency: String, context: Context) {
    val typeText = if (debt.type == "borrowed") "ঋণ গ্রহণ বাকি" else "ঋণ প্রদান বাকী"
    val balance = debt.amount - debt.paidAmount
    val dueDateText = if (debt.dueDate != null) {
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        "সীমা তারিখ: ${df.format(Date(debt.dueDate))}"
    } else ""

    val shareContent = """
        আমার হিসাব অ্যাপ কর্জ হিসেব বিবরণী:
        ব্যক্তি: ${debt.personName}
        হিসাবের ধরন: $typeText
        মূল সংখ্যা: $currency ${debt.amount}
        ইতিমধ্যে পরিশোধিত: $currency ${debt.paidAmount}
        বকেয়া অবশিষ্টাংশ: $currency $balance
        $dueDateText
        মন্তব্য: ${debt.notes ?: "নেই"}
    """.trimIndent()

    if (!debt.contactNumber.isNullOrBlank()) {
        try {
            // Offer SMS directly
            val uri = Uri.parse("smsto:${debt.contactNumber}")
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                putExtra("sms_body", shareContent)
            }
            context.startActivity(intent)
            return
        } catch (e: Exception) {
            // Fallback to chooser
        }
    }

    // Default intent chooser fallback
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareContent)
    }
    context.startActivity(Intent.createChooser(shareIntent, "বিবরণ শেয়ার করুন"))
}
