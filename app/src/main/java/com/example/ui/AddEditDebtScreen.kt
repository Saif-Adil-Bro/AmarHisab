package com.example.ui

import android.app.Activity
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDebtScreen(
    viewModel: DebtViewModel,
    debtId: Long = 0L,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isEditMode = debtId > 0L

    val currency by viewModel.defaultCurrency.collectAsStateWithLifecycle()
    val distinctNames by viewModel.distinctPersonNames.collectAsStateWithLifecycle()

    var personName by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("borrowed") } // "borrowed" or "lent"
    var dueDateMillis by remember { mutableStateOf<Long?>(null) }
    var notes by remember { mutableStateOf("") }
    var contactNumber by remember { mutableStateOf("") }

    var showDatePicker by remember { mutableStateOf(false) }
    var nameInputError by remember { mutableStateOf<String?>(null) }
    var amountInputError by remember { mutableStateOf<String?>(null) }

    // Fetch existing debt details if editing
    LaunchedEffect(debtId) {
        if (isEditMode) {
            val debt = viewModel.getDebtById(debtId)
            if (debt != null) {
                personName = debt.personName
                amountStr = debt.amount.toString()
                selectedType = debt.type
                dueDateMillis = debt.dueDate
                notes = debt.notes ?: ""
                contactNumber = debt.contactNumber ?: ""
            }
        }
    }

    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // List of filtered suggestion names
    val filteredNames = remember(personName, distinctNames) {
        if (personName.isBlank()) {
            emptyList()
        } else {
            distinctNames.filter {
                it.contains(personName, ignoreCase = true) && it != personName
            }.take(3)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        text = if (isEditMode) "ঋণের বিবরণ পরিবর্তন" else "নতুন ঋণ যোগ করুন",
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Amount Input Card (Highlighted)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "টাকার পরিমাণ",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = currency,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextField(
                            value = amountStr,
                            onValueChange = {
                                if (it.isEmpty() || it.toDoubleOrNull() != null || it.all { char -> char.isDigit() || char == '.' }) {
                                    amountStr = it
                                    amountInputError = null
                                }
                            },
                            placeholder = {
                                Text(
                                    text = "0.00",
                                    style = MaterialTheme.typography.headlineMedium.copy(color = Color.LightGray)
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.width(180.dp)
                        )
                    }
                    if (amountInputError != null) {
                        Text(
                            text = amountInputError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Segmented Selector for Borrowed vs Lent
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val borrowedSelected = selectedType == "borrowed"
                    val lentSelected = selectedType == "lent"

                    Button(
                        onClick = { selectedType = "borrowed" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (borrowedSelected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (borrowedSelected) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = "নেওয়া")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ঋণ নেওয়া (Borrowed)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = { selectedType = "lent" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (lentSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (lentSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = "দেওয়া")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ঋণ দেওয়া (Lent)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            // Person Name Field
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "ব্যক্তির নাম",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = personName,
                    onValueChange = {
                        personName = it
                        nameInputError = null
                    },
                    placeholder = { Text("যেমন: আবুল হাসান") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = "ব্যক্তি") },
                    isError = nameInputError != null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (nameInputError != null) {
                    Text(
                        text = nameInputError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }

                // Autocomplete suggests
                if (filteredNames.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filteredNames.forEach { name ->
                            SuggestionChip(
                                onClick = { personName = name },
                                label = { Text(name) },
                                modifier = Modifier.padding(2.dp)
                            )
                        }
                    }
                }
            }

            // Contact Number Field
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "যোগাযোগের নম্বর (ঐচ্ছিক)",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = contactNumber,
                    onValueChange = { contactNumber = it },
                    placeholder = { Text("যেমন: +৮৮০১৭১২৩৪৫৬৭৮") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "ফোন") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Due Date Field
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "ফেরতের তারিখ (ঐচ্ছিক)",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .clickable { showDatePicker = true }
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "ক্যালেন্ডার",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (dueDateMillis != null) dateFormatter.format(Date(dueDateMillis!!)) else "তারিখ নির্বাচন করুন",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (dueDateMillis != null) MaterialTheme.colorScheme.onSurface else Color.Gray
                            )
                        }
                        if (dueDateMillis != null) {
                            IconButton(
                                onClick = { dueDateMillis = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "তারিখ মুছুন",
                                    tint = Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            // Notes / Remarks Field
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "মন্তব্য বা অতিরিক্ত তথ্য (ঐচ্ছিক)",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text("ঋণের বিবরণ লিখুন...") },
                    leadingIcon = { Icon(Icons.Default.Notes, contentDescription = "নোট") },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Action Button
            Button(
                onClick = {
                    var hasError = false
                    if (personName.isBlank()) {
                        nameInputError = "ব্যক্তির নামটি আবশ্যক।"
                        hasError = true
                    }
                    val amountVal = amountStr.toDoubleOrNull()
                    if (amountVal == null || amountVal <= 0.0) {
                        amountInputError = "সঠিক টাকা প্রদান করুন।"
                        hasError = true
                    }

                    if (!hasError) {
                        if (isEditMode) {
                            // Fetch existing to preserve paidAmount & status
                            coroutineScope.launch {
                                val original = viewModel.getDebtById(debtId)
                                if (original != null) {
                                    val status = when {
                                        original.paidAmount >= amountVal!! -> "paid"
                                        original.paidAmount > 0.0 -> "partial"
                                        else -> "unpaid"
                                    }
                                    viewModel.updateDebt(
                                        original.copy(
                                            personName = personName,
                                            amount = amountVal,
                                            type = selectedType,
                                            dueDate = dueDateMillis,
                                            status = status,
                                            notes = notes.ifBlank { null },
                                            contactNumber = contactNumber.ifBlank { null }
                                        )
                                    )
                                }
                                (context as? Activity)?.runOnUiThread {
                                    onNavigateBack()
                                }
                            }
                        } else {
                            viewModel.insertDebt(
                                personName = personName,
                                amount = amountVal!!,
                                type = selectedType,
                                dueDate = dueDateMillis,
                                notes = notes.ifBlank { null },
                                contactNumber = contactNumber.ifBlank { null }
                            )
                            onNavigateBack()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = "সংরক্ষণ")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEditMode) "পরিবর্তন সংরক্ষণ করুন" else "ঋণ যুক্ত করুন",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Material 3 Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDateMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dueDateMillis = datePickerState.selectedDateMillis
                        showDatePicker = false
                    }
                ) {
                    Text("ঠিক আছে", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("বাতিল")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
