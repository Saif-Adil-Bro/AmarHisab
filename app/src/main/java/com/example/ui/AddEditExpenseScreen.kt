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
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

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
    val categoriesFromDb by viewModel.allCategories.collectAsStateWithLifecycle()

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

    val context = LocalContext.current
    var isListeningState by remember { mutableStateOf(false) }

    // Pulsing animation for the Mic Icon Button when active or highlighted
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val micScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListeningState = false
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.firstOrNull() ?: ""
            if (spokenText.isNotEmpty()) {
                val (parsedPrice, parsedItem) = com.example.util.VoiceInputParser.parseVoiceInput(spokenText)
                var matchesAny = false
                if (parsedPrice != null) {
                    priceStr = parsedPrice.toString()
                    matchesAny = true
                }
                if (parsedItem != null) {
                    itemName = parsedItem
                    matchesAny = true
                }
                
                if (matchesAny) {
                    inputError = null
                    val feedbackMsg = if (isBangla) {
                        "ভয়েস থেকে সফলভাবে সনাক্ত করা হয়েছে!"
                    } else {
                        "Successfully parsed from voice!"
                    }
                    Toast.makeText(context, feedbackMsg, Toast.LENGTH_SHORT).show()
                } else {
                    val alertMsg = if (isBangla) {
                        "দুঃখিত, কোনো মূল্য বা নাম খুঁজে পাওয়া যায়নি। আপনি বলেছেন: \"$spokenText\""
                    } else {
                        "Sorry, couldn't find a price or item name. You said: \"$spokenText\""
                    }
                    Toast.makeText(context, alertMsg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isListeningState = true
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "bn-BD")
                    putExtra(RecognizerIntent.EXTRA_PROMPT, if (isBangla) "বলুন: পঞ্চাশ টাকার আলু" else "Say e.g.: Fifty Taka Alu")
                }
                speechRecognizerLauncher.launch(intent)
            } catch (e: Exception) {
                isListeningState = false
                Toast.makeText(
                    context,
                    if (isBangla) "ভয়েস রিকগনিশন উপলব্ধ নয়" else "Voice recognition is not available",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(
                context,
                if (isBangla) "রেকর্ড অডিও পারমিশন দরকার।" else "Record audio permission is required.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val voiceOnClick = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            isListeningState = true
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "bn-BD")
                    putExtra(RecognizerIntent.EXTRA_PROMPT, if (isBangla) "বলুন: পঞ্চাশ টাকার আলু" else "Say e.g.: Fifty Taka Alu")
                }
                speechRecognizerLauncher.launch(intent)
            } catch (e: Exception) {
                isListeningState = false
                Toast.makeText(
                    context,
                    if (isBangla) "ভয়েস রিকগনিশন উপলব্ধ নয়" else "Voice recognition is not available",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Dropdown and error states migrated above

    // Fetch and populate if edit mode or set default currency
    LaunchedEffect(expenseId, defaultCurrency, categoriesFromDb) {
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
            if (category == "Grocery" && categoriesFromDb.isNotEmpty()) {
                val hasBajar = categoriesFromDb.any { it.name == "বাজার" }
                category = if (hasBajar) "বাজার" else categoriesFromDb.first().name
            }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBangla) "জিনিসের নাম (Item Name)" else "Item Name",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (isListeningState) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .scale(micScale)
                                    .background(MaterialTheme.colorScheme.error, shape = RoundedCornerShape(50))
                            )
                            Text(
                                text = if (isBangla) "শোনা হচ্ছে..." else "Listening...",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            )
                        }
                    }
                }
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                if (itemName.isNotEmpty()) {
                                    IconButton(
                                        onClick = { itemName = "" },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                                IconButton(
                                    onClick = voiceOnClick,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .scale(if (isListeningState) micScale else 1.0f)
                                        .testTag("voice_input_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Voice Input",
                                        tint = if (isListeningState) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
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
                    text = if (isBangla) "ক্যাটাগরি" else "Category",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                ExposedDropdownMenuBox(
                    expanded = isCategoryExpanded,
                    onExpandedChange = { isCategoryExpanded = !isCategoryExpanded }
                ) {
                    val selectedCategoryObj = categoriesFromDb.find { it.name == category }
                    val displayText = if (selectedCategoryObj != null) {
                        "${selectedCategoryObj.iconEmoji} ${selectedCategoryObj.name}"
                    } else {
                        category
                    }

                    OutlinedTextField(
                        readOnly = true,
                        value = displayText,
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
                        categoriesFromDb.forEach { dbCat ->
                            DropdownMenuItem(
                                text = {
                                    Text(text = "${dbCat.iconEmoji} ${dbCat.name}")
                                },
                                onClick = {
                                    category = dbCat.name
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
