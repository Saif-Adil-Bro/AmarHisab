package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.ExpenseViewModel
import com.example.data.ExpenseEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditExpenseScreen(
    viewModel: ExpenseViewModel,
    expenseId: Long,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val isEditMode = expenseId > 0L

    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val defaultCurrency by viewModel.defaultCurrency.collectAsStateWithLifecycle()
    val isBangla = appLanguage == "Bangla"

    // Form states
    var itemName by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf("৳") }
    var category by remember { mutableStateOf("Grocery") }
    var dateMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    // Dropdown and error states
    var isCategoryExpanded by remember { mutableStateOf(false) }
    var isCurrencyExpanded by remember { mutableStateOf(false) }
    var isItemNameExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var inputError by remember { mutableStateOf<String?>(null) }

    // Fetch and populate if edit mode or set default currency
    LaunchedEffect(expenseId, defaultCurrency) {
        if (isEditMode) {
            val db = viewModel.allExpenses.value.find { it.id == expenseId }
            if (db != null) {
                itemName = db.itemName
                priceStr = db.price.toString()
                selectedCurrency = db.currency
                category = db.category
                dateMillis = db.date
            }
        } else {
            selectedCurrency = defaultCurrency
        }
    }

    // Auto-complete suggestion source from ViewModel
    val distinctNames by viewModel.distinctItemNames.collectAsStateWithLifecycle()
    val filteredSuggestions = remember(itemName, distinctNames) {
        if (itemName.isBlank()) {
            emptyList()
        } else {
            distinctNames.filter {
                it.contains(itemName, ignoreCase = true) && !it.equals(itemName, ignoreCase = true)
            }.take(5)
        }
    }

    val categoriesList = remember {
        listOf("Vegetables", "Meat", "Grocery", "Dairy/Eggs", "Fruits", "Beverages", "Snacks", "Others")
    }

    val currencyOptions = remember {
        listOf("৳", "$", "£", "₹")
    }

    val dateFormatter = remember { SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) {
                            if (isBangla) "লেনদেন সংশোধন" else "Edit Transaction"
                        } else {
                            if (isBangla) "নতুন খরচ যোগ" else "Add Expense"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error alert box
            if (inputError != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error Logo",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = inputError ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Item Name with Auto-Suggest (ExposedDropdownMenu style autocomplete)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isBangla) "জিনিসের নাম (Item Name)" else "Item Name",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                ExposedDropdownMenuBox(
                    expanded = isItemNameExpanded && filteredSuggestions.isNotEmpty(),
                    onExpandedChange = { isItemNameExpanded = !isItemNameExpanded }
                ) {
                    OutlinedTextField(
                        value = itemName,
                        onValueChange = {
                            itemName = it
                            isItemNameExpanded = true
                            inputError = null
                        },
                        placeholder = { Text(if (isBangla) "যেমন: খাসির মাংস বা চাল" else "e.g. Organic Bananas") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("item_name_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        trailingIcon = {
                            if (itemName.isNotEmpty()) {
                                IconButton(onClick = { itemName = "" }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        }
                    )

                    ExposedDropdownMenu(
                        expanded = isItemNameExpanded && filteredSuggestions.isNotEmpty(),
                        onDismissRequest = { isItemNameExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        filteredSuggestions.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(suggestion) },
                                onClick = {
                                    itemName = suggestion
                                    isItemNameExpanded = false
                                    focusManager.clearFocus()
                                }
                            )
                        }
                    }
                }
            }

            // Price & Currency Selection Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Price Input
                Column(modifier = Modifier.weight(1.5f)) {
                    Text(
                        text = if (isBangla) "মূল্য (Price)" else "Price",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = {
                            priceStr = it
                            inputError = null
                        },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("price_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        leadingIcon = {
                            Text(
                                text = selectedCurrency,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    )
                }

                // Currency Selector
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isBangla) "মুদ্রা (Currency)" else "Currency",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    ExposedDropdownMenuBox(
                        expanded = isCurrencyExpanded,
                        onExpandedChange = { isCurrencyExpanded = !isCurrencyExpanded }
                    ) {
                        OutlinedTextField(
                            readOnly = true,
                            value = selectedCurrency,
                            onValueChange = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("currency_selector_trigger"),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCurrencyExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = isCurrencyExpanded,
                            onDismissRequest = { isCurrencyExpanded = false }
                        ) {
                            currencyOptions.forEach { curr ->
                                DropdownMenuItem(
                                    text = { Text(curr, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                                    onClick = {
                                        selectedCurrency = curr
                                        isCurrencyExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Category Selection Dropdown
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isBangla) "বিভাগ (Category)" else "Category",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                ExposedDropdownMenuBox(
                    expanded = isCategoryExpanded,
                    onExpandedChange = { isCategoryExpanded = !isCategoryExpanded }
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = if (isBangla) {
                            when (category) {
                                "Vegetables" -> "সবজি"
                                "Meat" -> "মাংস"
                                "Grocery" -> "মুদিখানা"
                                "Dairy/Eggs" -> "দুধ/ডিম"
                                "Fruits" -> "ফলমূল"
                                "Beverages" -> "পানীয়"
                                "Snacks" -> "নাস্তা"
                                "Others" -> "অন্যান্য"
                                else -> category
                            }
                        } else category,
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("category_dropdown"),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = isCategoryExpanded,
                        onDismissRequest = { isCategoryExpanded = false }
                    ) {
                        categoriesList.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (isBangla) {
                                            when (cat) {
                                                "Vegetables" -> "সবজি (Vegetables)"
                                                "Meat" -> "মাংস (Meat)"
                                                "Grocery" -> "মুদিখানা (Grocery)"
                                                "Dairy/Eggs" -> "দুধ/ডিম (Dairy/Eggs)"
                                                "Fruits" -> "ফলমূল (Fruits)"
                                                "Beverages" -> "পানীয় (Beverages)"
                                                "Snacks" -> "নাস্তা (Snacks)"
                                                "Others" -> "অন্যান্য (Others)"
                                                else -> cat
                                            }
                                        } else cat
                                    )
                                },
                                onClick = {
                                    category = cat
                                    isCategoryExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
            }

            // Date Picker Field
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isBangla) "তারিখ (Date)" else "Date",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = dateFormatter.format(Date(dateMillis)),
                    onValueChange = {},
                    readOnly = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("date_picker_trigger")
                        .clickable { showDatePicker = true },
                    enabled = false, // disable key inputs, make only clickable
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onBackground,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Choose Date")
                    },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(imageVector = Icons.Default.EditCalendar, contentDescription = "Select Date Button")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = {
                    val finalName = itemName.trim()
                    val finalPrice = priceStr.toDoubleOrNull()

                    when {
                        finalName.isEmpty() -> {
                            inputError = if (isBangla) "জিনিসের নাম আবশ্যক।" else "Item name is required."
                        }
                        finalPrice == null -> {
                            inputError = if (isBangla) "দয়া করে সঠিক মূল্য লিখুন।" else "Please enter a valid price."
                        }
                        finalPrice <= 0.0 -> {
                            inputError = if (isBangla) "মূল্য অবশ্যই শূন্যের চেয়ে বেশি হতে হবে।" else "Price must be greater than zero."
                        }
                        else -> {
                            coroutineScope.launch {
                                if (isEditMode) {
                                    viewModel.updateExpense(
                                        id = expenseId,
                                        itemName = finalName,
                                        price = finalPrice,
                                        currency = selectedCurrency,
                                        category = category,
                                        date = dateMillis
                                    )
                                } else {
                                    viewModel.insertExpense(
                                        itemName = finalName,
                                        price = finalPrice,
                                        currency = selectedCurrency,
                                        category = category,
                                        date = dateMillis
                                    )
                                }
                                onNavigateBack()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("save_expense_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEditMode) {
                        if (isBangla) "সংশোধন সংরক্ষণ করুন" else "Update Transaction"
                    } else {
                        if (isBangla) "খরচ সংরক্ষণ করুন" else "Save Expense"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Official M3 DatePickerDialog
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
