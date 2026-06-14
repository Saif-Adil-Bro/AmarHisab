package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ShoppingListItemEntity
import com.example.viewmodel.ExpenseViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val items by viewModel.shoppingList.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()

    val defaultCurrency by viewModel.defaultCurrency.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isBangla = appLanguage == "Bangla"

    val profileThemeColor = remember(activeProfile) {
        parseHexColor(activeProfile?.colorHex ?: "#6750A4")
    }

    // High visibility text field styles
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        errorContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        disabledTextColor = MaterialTheme.colorScheme.onSurface,
        errorTextColor = MaterialTheme.colorScheme.onSurface,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline
    )

    // Input Form States
    var itemName by remember { mutableStateOf("") }
    var quantityStr by remember { mutableStateOf("") }
    var estimatedPriceStr by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf("pcs") }

    var isUnitDropdownExpanded by remember { mutableStateOf(false) }
    var inputError by remember { mutableStateOf<String?>(null) }

    // Active item selected for checkout dialog
    var itemToCheckout by remember { mutableStateOf<ShoppingListItemEntity?>(null) }

    val unitOptions = listOf("pcs", "kg", "gm", "liter", "packet", "box")

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ProfileSwitcherAppBar(
                viewModel = viewModel,
                titleText = "বাজারের তালিকা"
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
            // Elegant Addition Form Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isBangla) "নতুন বাজার যোগ করুন" else "Add Grocery Item",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Error Alert
                    if (inputError != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = inputError ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    // Item Name
                    OutlinedTextField(
                        value = itemName,
                        onValueChange = {
                            itemName = it
                            inputError = null
                        },
                        label = { Text(if (isBangla) "জিনিসের নাম" else "Item Name") },
                        placeholder = { Text(if (isBangla) "যেমন: আলু, পেঁয়াজ, ডিম" else "e.g. Potato, Eggs") },
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shopping_item_name_input"),
                        singleLine = true
                    )

                    // Row: Quantity & Unit
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quantity
                        OutlinedTextField(
                            value = quantityStr,
                            onValueChange = {
                                quantityStr = it
                                inputError = null
                            },
                            label = { Text(if (isBangla) "পরিমাণ" else "Qty") },
                            placeholder = { Text("1.0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("shopping_quantity_input"),
                            singleLine = true
                        )

                        // Unit dropdown selector
                        Box(modifier = Modifier.weight(1f)) {
                            ExposedDropdownMenuBox(
                                expanded = isUnitDropdownExpanded,
                                onExpandedChange = { isUnitDropdownExpanded = !isUnitDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedUnit,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(if (isBangla) "একক" else "Unit") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isUnitDropdownExpanded) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = textFieldColors,
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                        .testTag("shopping_unit_dropdown")
                                )
                                ExposedDropdownMenu(
                                    expanded = isUnitDropdownExpanded,
                                    onDismissRequest = { isUnitDropdownExpanded = false }
                                ) {
                                    unitOptions.forEach { unit ->
                                        DropdownMenuItem(
                                            text = { Text(unit) },
                                            onClick = {
                                                selectedUnit = unit
                                                isUnitDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Est Price
                    OutlinedTextField(
                        value = estimatedPriceStr,
                        onValueChange = {
                            estimatedPriceStr = it
                            inputError = null
                        },
                        label = { Text(if (isBangla) "আনুমানিক দাম" else "Est Price") },
                        placeholder = { Text("0.00") },
                        leadingIcon = { Text(defaultCurrency, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shopping_est_price_input"),
                        singleLine = true
                    )

                    // Save Button
                    Button(
                        onClick = {
                            val finalName = itemName.trim()
                            val qty = quantityStr.toDoubleOrNull() ?: 1.0
                            val estPrice = estimatedPriceStr.toDoubleOrNull() ?: 0.0

                            if (finalName.isEmpty()) {
                                inputError = if (isBangla) "বাজারের নাম পূরণ করা আবশ্যক।" else "Shopping item name is required."
                            } else {
                                viewModel.insertShoppingItem(
                                    itemName = finalName,
                                    quantity = qty,
                                    unit = selectedUnit,
                                    estimatedPrice = estPrice
                                )
                                // Clear Form
                                itemName = ""
                                quantityStr = ""
                                estimatedPriceStr = ""
                                selectedUnit = "pcs"
                                inputError = null
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("add_shopping_item_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = profileThemeColor)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isBangla) "তালিকায় যোগ করুন" else "Add to List", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Checklist section
            Text(
                text = if (isBangla) "বাজারের ফর্দ" else "Shopping Checklist",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 22.dp, top = 8.dp, end = 22.dp, bottom = 6.dp)
            )

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            color = profileThemeColor.copy(alpha = 0.1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ListAlt,
                                    contentDescription = null,
                                    tint = profileThemeColor,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isBangla) "ফর্দ এখন সম্পূর্ণ খালি!" else "Checklist is empty!",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isBangla) "উপরের ফর্মে বাজার সওদা লিখে তালিকায় এন্ট্রি করুন।" else "Type a grocery item above to add to your list.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        ShoppingListItemRow(
                            item = item,
                            accentColor = profileThemeColor,
                            defaultCurrency = defaultCurrency,
                            onTogglePurchased = {
                                if (!item.isPurchased) {
                                    // Open price checkout dialog
                                    itemToCheckout = item
                                } else {
                                    // Reactivate item back to checklist
                                    viewModel.toggleShoppingItemPurchased(item, null)
                                }
                            },
                            onDelete = { viewModel.deleteShoppingItem(item) }
                        )
                    }
                }
            }
        }
    }

    // Checkout Actual Price Confirmation Dialog
    itemToCheckout?.let { item ->
        var actualPriceStr by remember { mutableStateOf(if (item.estimatedPrice > 0.0) item.estimatedPrice.toString() else "") }
        var isConversionError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { itemToCheckout = null },
            title = {
                Text(
                    text = if (isBangla) "বাজার সম্পন্ন করুন" else "Complete Purchase",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isBangla) {
                            "কত টাকায় '${item.itemName}' কিনলেন? এটি সরাসরি আপনার খরচ তালিকায় যুক্ত হবে।"
                        } else {
                            "How much did you spend on '${item.itemName}'? This will be added directly to your expense tracker."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = actualPriceStr,
                        onValueChange = {
                            actualPriceStr = it
                            isConversionError = false
                        },
                        label = { Text(if (isBangla) "প্রকৃত মূল্য" else "Actual Price") },
                        leadingIcon = { Text(defaultCurrency, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checkout_price_input"),
                        singleLine = true,
                        isError = isConversionError
                    )

                    if (isConversionError) {
                        Text(
                            text = if (isBangla) "অনুগ্রহ করে একটি সঠিক মূল্য প্রবেশ করান।" else "Please enter a valid price details.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val actualPrice = actualPriceStr.toDoubleOrNull()
                        if (actualPrice != null && actualPrice > 0.0) {
                            viewModel.toggleShoppingItemPurchased(item, actualPrice)
                            itemToCheckout = null
                        } else {
                            isConversionError = true
                        }
                    },
                    modifier = Modifier.testTag("complete_with_expense_btn")
                ) {
                    Text(if (isBangla) "কিনলাম" else "Log Expense")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            // Mark checked without converting to expense
                            viewModel.toggleShoppingItemPurchased(item, null)
                            itemToCheckout = null
                        },
                        modifier = Modifier.testTag("ignore_expense_btn")
                    ) {
                        Text(if (isBangla) "শুধু সম্পন্ন করুন" else "Just Complete")
                    }
                    TextButton(onClick = { itemToCheckout = null }) {
                        Text(if (isBangla) "বাতিল" else "Cancel")
                    }
                }
            }
        )
    }
}

@Composable
fun ShoppingListItemRow(
    item: ShoppingListItemEntity,
    accentColor: Color,
    defaultCurrency: String,
    onTogglePurchased: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .testTag("shopping_item_${item.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isPurchased) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, if (item.isPurchased) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isPurchased) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Interactive Checkbox
                Checkbox(
                    checked = item.isPurchased,
                    onCheckedChange = { _ -> onTogglePurchased() },
                    colors = CheckboxDefaults.colors(checkedColor = accentColor),
                    modifier = Modifier.testTag("shopping_checkbox_${item.id}")
                )

                Spacer(modifier = Modifier.width(6.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.itemName,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (item.isPurchased) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (item.isPurchased) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Qty + Unit
                        Text(
                            text = "${item.quantity} ${item.unit}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = accentColor.copy(alpha = if (item.isPurchased) 1.0f else 0.8f)
                        )

                        if (item.estimatedPrice > 0.0) {
                            Text(text = "•", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text(
                                text = "Est: $defaultCurrency${item.estimatedPrice}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Delete item action
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("delete_shopping_item_button_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete item",
                    tint = Color(0xFFB3261E).copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
