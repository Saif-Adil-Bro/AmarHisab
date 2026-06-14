package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CategoryEntity
import com.example.data.RecurringExpenseEntity
import com.example.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringExpensesScreen(
    viewModel: ExpenseViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val recurringExpenses by viewModel.allRecurringExpenses.collectAsStateWithLifecycle()
    val customCategories by viewModel.allCategories.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<RecurringExpenseEntity?>(null) }

    val bnLocale = remember { Locale("bn", "BD") }
    val dateFormatter = remember { SimpleDateFormat("dd MMMM yyyy", bnLocale) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        text = "নিয়মিত খরচ",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "ফিরে যান"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_recurring_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "নতুন নিয়মিত খরচ")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (recurringExpenses.isEmpty()) {
                // Empty state view
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.size(100.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "কোন নিয়মিত খরচ খুঁজে পাওয়া যায়নি!",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "আপনার প্রতি সপ্তাহের রিচার্জ, মাসিক ভাড়া বা অন্যান্য নিয়মিত খরচ এখানে যোগ করে রাখুন। অ্যাপটি নির্দিষ্ট দিনে অটোমেটিক সেটি যোগ করে নিবে।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "নতুন নিয়মিত খরচ যুক্ত করুন")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recurringExpenses, key = { it.id }) { item ->
                        RecurringExpenseItemCard(
                            item = item,
                            dateFormatter = dateFormatter,
                            customCategories = customCategories,
                            onToggleActive = { viewModel.toggleRecurringExpenseActive(item) },
                            onDeleteClick = { itemToDelete = item }
                        )
                    }
                }
            }
        }
    }

    // Add Recurring Expense Dialog
    if (showAddDialog) {
        AddRecurringExpenseDialog(
            categories = customCategories,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, amt, cat, freq, dueDate ->
                viewModel.insertRecurringExpense(name, amt, cat, freq, dueDate)
                showAddDialog = false
            }
        )
    }

    // Delete confirmation dialog
    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = {
                Text(
                    text = "মুছে ফেলার নিশ্চিতকরণ",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(text = "আপনি কি নিশ্চিতভাবে এই নিয়মিত খরচটি মুছে ফেলতে চান?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        itemToDelete?.let { viewModel.deleteRecurringExpense(it) }
                        itemToDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text(text = "মুছুন", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text(text = "বাতিল")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun RecurringExpenseItemCard(
    item: RecurringExpenseEntity,
    dateFormatter: SimpleDateFormat,
    customCategories: List<CategoryEntity> = emptyList(),
    onToggleActive: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val frequencyLabel = when (item.frequency) {
        "DAILY" -> "দৈনিক"
        "WEEKLY" -> "সাপ্তাহিক"
        "MONTHLY" -> "মাসিক"
        "YEARLY" -> "বার্ষিক"
        else -> item.frequency
    }

    val formattedDueDate = remember(item.nextDueDate) {
        dateFormatter.format(Date(item.nextDueDate))
    }

    val emoji = remember(item.category, customCategories) {
        getCategoryEmoji(item.category, customCategories)
    }
    val color = remember(item.category, customCategories) {
        getCategoryColor(item.category, customCategories)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recurring_item_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.12f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = emoji, fontSize = 24.sp)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.itemName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(text = frequencyLabel) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        border = null,
                        modifier = Modifier.height(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "পরবর্তী: $formattedDueDate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "৳${String.format(Locale.getDefault(), "%.1f", item.amount)}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = item.isActive,
                        onCheckedChange = { onToggleActive() },
                        modifier = Modifier
                            .scale(0.8f)
                            .testTag("toggle_active_${item.id}")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_recurring_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "মুছে ফেলুন",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecurringExpenseDialog(
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, amount: Double, category: String, frequency: String, nextDueDate: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var itemName by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    
    val defaultCategory = if (categories.isNotEmpty()) categories.first().name else "অন্যান্য"
    var selectedCategory by remember { mutableStateOf(defaultCategory) }
    var isCategoryExpanded by remember { mutableStateOf(false) }

    val frequencies = listOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY")
    val frequencyLabels = listOf("দৈনিক", "সাপ্তাহিক", "মাসিক", "বার্ষিক")
    var selectedFrequencyIndex by remember { mutableIntStateOf(2) }

    var dateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val bnLocale = remember { Locale("bn", "BD") }
    val displayDateFormatter = remember { SimpleDateFormat("dd MMMM yyyy", bnLocale) }

    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "নতুন নিয়মিত খরচ যোগ করুন",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (localError != null) {
                    Text(
                        text = localError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                // Item Name Input
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { 
                        itemName = it
                        localError = null
                    },
                    label = { Text("খরচের নাম / বিবরণ (যেমন: ইন্টারনেট বিল)") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recurring_name_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors()
                )

                // Amount Input
                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { 
                        priceStr = it
                        localError = null
                    },
                    label = { Text("টাকার পরিমাণ") },
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recurring_amount_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors()
                )

                // Category Selection Dropdown
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "ক্যাটাগরি",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ExposedDropdownMenuBox(
                        expanded = isCategoryExpanded,
                        onExpandedChange = { isCategoryExpanded = !isCategoryExpanded }
                    ) {
                        val selectedCategoryObj = categories.find { it.name == selectedCategory }
                        val displayText = if (selectedCategoryObj != null) {
                            "${selectedCategoryObj.iconEmoji} ${selectedCategoryObj.name}"
                        } else {
                            selectedCategory
                        }

                        OutlinedTextField(
                            readOnly = true,
                            value = displayText,
                            onValueChange = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("recurring_category_dropdown"),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(),
                            shape = RoundedCornerShape(14.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = isCategoryExpanded,
                            onDismissRequest = { isCategoryExpanded = false }
                        ) {
                            categories.forEach { dbCat ->
                                DropdownMenuItem(
                                    text = {
                                        Text(text = "${dbCat.iconEmoji} ${dbCat.name}")
                                    },
                                    onClick = {
                                        selectedCategory = dbCat.name
                                        isCategoryExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }
                }

                // Frequency Selectable Chips List
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "পুনরাবৃত্তি (Frequency)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        frequencyLabels.forEachIndexed { index, label ->
                            val isSelected = selectedFrequencyIndex == index
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedFrequencyIndex = index },
                                label = { Text(text = label, maxLines = 1, fontSize = 11.sp) },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Start Date Selection Dialog trigger
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "পরবর্তী পরিশোধের তারিখ",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        readOnly = true,
                        value = displayDateFormatter.format(Date(dateMillis)),
                        onValueChange = {},
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                            .testTag("recurring_date_picker_trigger"),
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "তারিখ নির্বাচন করুন")
                        },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(imageVector = Icons.Default.EditCalendar, contentDescription = "তারিখ ওয়ান")
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = itemName.trim()
                    val finalPrice = priceStr.toDoubleOrNull()
                    if (finalName.isEmpty()) {
                        localError = "অনুগ্রহ করে খরচের নামটি প্রদান করুন"
                        return@Button
                    }
                    if (finalPrice == null || finalPrice <= 0.0) {
                        localError = "অনুগ্রহ করে একটি সঠিক টাকার সংখ্যা লিখুন"
                        return@Button
                    }
                    onConfirm(
                        finalName,
                        finalPrice,
                        selectedCategory,
                        frequencies[selectedFrequencyIndex],
                        dateMillis
                    )
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("recurring_save_button")
            ) {
                Text(text = "সেভ করুন", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "বাতিল")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            dateMillis = it
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
