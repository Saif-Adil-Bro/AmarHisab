package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CategoryEntity
import com.example.viewmodel.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesScreen(
    viewModel: ExpenseViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        text = "ক্যাটাগরি ম্যানেজমেন্ট",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("categories_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "ফিরে যান"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(16.dp)
                    .testTag("add_category_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "নতুন ক্যাটাগরি তৈরি করুন")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (categories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
            ) {
                items(categories, key = { it.id }) { category ->
                    CategoryItemRow(
                        category = category,
                        onDeleteClick = {
                            categoryToDelete = category
                            showDeleteConfirmDialog = true
                        }
                    )
                }
            }
        }
    }

    // Add Category Dialog
    if (showAddDialog) {
        val emojis = listOf(
            "🛒", "🥬", "🍖", "🚗", "🏠", "💡", "🍔", "🍎", "💊", "👗",
            "📚", "🎮", "🎁", "✈️", "💸", "💈", "🧼", "🐶", "🍿", "☕",
            "🎬", "🛠️", "🩹", "🍼", "🎒", "💍", "🎨", "🎤", "🏋️", "🚲"
        )
        val colors = listOf(
            "#1565C0", // Blue
            "#2E7D32", // Green
            "#C62828", // Red
            "#ED6C02", // Orange
            "#6A1B9A", // Purple
            "#AD1457", // Pink
            "#00838F", // Teal
            "#4E342E"  // Brown
        )

        var nameInput by remember { mutableStateOf("") }
        var selectedEmoji by remember { mutableStateOf("🛒") }
        var selectedColor by remember { mutableStateOf(colors.first()) }
        var nameError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = "নতুন ক্যাটাগরি যোগ করুন",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = {
                            nameInput = it
                            nameError = false
                        },
                        label = { Text("ক্যাটাগরির নাম") },
                        placeholder = { Text("যেমন: চিকিৎসা") },
                        singleLine = true,
                        isError = nameError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_category_name_input")
                    )

                    if (nameError) {
                        Text(
                            text = "দয়া করে একটি সঠিক ক্যাটাগরির নাম লিখুন!",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // Emoji Grid Header
                    Text(
                        text = "একটি ইমোজি নির্বাচন করুন:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Emoji Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(items = emojis, key = { it }) { emoji ->
                            val isSelected = selectedEmoji == emoji
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedEmoji = emoji }
                                    .testTag("emoji_opt_$emoji"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 20.sp)
                            }
                        }
                    }

                    // Color Line Header
                    Text(
                        text = "একটি থিম কালার নির্বাচন করুন:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Color presets row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        colors.forEach { hex ->
                            val isSelected = selectedColor == hex
                            val color = try {
                                Color(android.graphics.Color.parseColor(hex))
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.primary
                            }

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = hex }
                                    .testTag("color_opt_$hex")
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameInput.trim().isEmpty()) {
                            nameError = true
                        } else {
                            viewModel.insertCategory(
                                name = nameInput.trim(),
                                iconEmoji = selectedEmoji,
                                colorHex = selectedColor
                            )
                            showAddDialog = false
                            android.widget.Toast.makeText(
                                context,
                                "ক্যাটাগরি সফলভাবে তৈরি করা হয়েছে!",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier.testTag("save_category_dialog_btn")
                ) {
                    Text("সেভ করুন")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddDialog = false }
                ) {
                    Text("বাতিল", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            modifier = Modifier.testTag("add_category_dialog")
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog && categoryToDelete != null) {
        val category = categoryToDelete!!
        DeleteConfirmationDialog(
            isBangla = true,
            title = "ক্যাটাগরি ডিলিট",
            message = "\"${category.name}\" ক্যাটাগরি ডিলিট করতে চান? এই ক্যাটাগরিতে থাকা খরচের তথ্যসমূহ ডিলিট হবে না, তবে ক্যাটাগরির নাম হিসেবে থাকবে।",
            onDismiss = {
                showDeleteConfirmDialog = false
                categoryToDelete = null
            },
            onConfirm = {
                viewModel.deleteCategory(category)
                showDeleteConfirmDialog = false
                categoryToDelete = null
                android.widget.Toast.makeText(
                    context,
                    "ক্যাটাগরি মুছে ফেলা হয়েছে!",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        )
    }
}

@Composable
fun CategoryItemRow(
    category: CategoryEntity,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = remember(category.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(category.colorHex))
        } catch (e: Exception) {
            Color(0xFF6750A4)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("category_row_${category.name}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Colored container with emoji
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = category.iconEmoji, fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (category.isDefault) {
                        Text(
                            text = "ডিফল্ট ক্যাটাগরি",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    } else {
                        Text(
                            text = "আপনার তৈরি করা",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Delete item button (Only let user delete non-default categories to prevent database inconsistency, or allow with warning)
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.testTag("delete_cat_btn_${category.name}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "মুছে ফেলুন",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }
    }
}
