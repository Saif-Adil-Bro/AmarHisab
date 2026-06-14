package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.viewmodel.BackupViewModel
import com.example.viewmodel.ExpenseViewModel
import com.example.viewmodel.BackupUiState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ExpenseViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val themePreference by viewModel.themePreference.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()

    // Backup & Restore Setup
    val backupViewModel: BackupViewModel = viewModel(
        factory = BackupViewModel.Factory(context.applicationContext as android.app.Application)
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

    // Modal Dialog States
    var showThemeDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "সেটিংস",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_btn")
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
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Section 1: APP THEME
            item {
                Spacer(modifier = Modifier.height(12.dp))
                SectionHeader(title = "অ্যাপের থিম")
            }
            item {
                val currentThemeText = when (themePreference) {
                    "Light" -> "লাইট মোড"
                    "Dark" -> "ডার্ক মোড"
                    else -> "সিস্টেম ডিফল্ট"
                }
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = "থিম পরিবর্তন করুন",
                    subtitle = currentThemeText,
                    onClick = { showThemeDialog = true },
                    testTag = "settings_theme_item"
                )
            }

            // Section 2: DATA & BACKUP
            item {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(title = "ডাটা ও ব্যাকআপ")
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Upload,
                    title = "ডাটা এক্সপোর্ট করুন",
                    subtitle = "ভবিষ্যতের জন্য ডাটা ব্যাকআপ ফাইল তৈরি করুন",
                    onClick = {
                        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                        createDocumentLauncher.launch("amar_hisab_backup_$dateStr.json")
                    },
                    testTag = "settings_export_item"
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Download,
                    title = "ডাটা ইমপোর্ট করুন",
                    subtitle = "পূর্বে তৈরি ব্যাকআপ ফাইল পুনরুদ্ধার করুন",
                    onClick = {
                        openDocumentLauncher.launch(arrayOf("application/json", "*/*"))
                    },
                    testTag = "settings_import_item"
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Delete,
                    title = "সব ডাটা মুছে ফেলুন",
                    subtitle = "আপনার সব প্রোফাইল ও ডাটা সম্পূর্ণ ডিলিট করুন",
                    onClick = { showDeleteConfirmDialog = true },
                    tint = MaterialTheme.colorScheme.error,
                    testTag = "settings_clear_all_item"
                )
            }

            // Section 3: BUDGET
            item {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(title = "বাজেট সেটিংস")
            }
            item {
                val currentBudget = activeProfile?.monthlyBudget ?: 0.0
                val budgetValueText = if (currentBudget > 0.0) {
                    "৳${String.format(Locale.US, "%,.0f", currentBudget)}"
                } else {
                    "কোনো বাজেট সেট করা নেই"
                }
                val activeProfileName = activeProfile?.let {
                    if (it.name.contains("আমার পকেট")) "আমার পকেট"
                    else if (it.name.contains("সংসার বাজার")) "সংসার বাজার"
                    else if (it.name.contains("অফিস")) "অফিস প্যান্ট্রি"
                    else it.name.substringBefore(" (")
                } ?: ""
                
                SettingsItem(
                    icon = Icons.Default.AccountBalanceWallet,
                    title = "মাসিক বাজেট সেট করুন",
                    subtitle = if (activeProfileName.isNotEmpty()) {
                        "বর্তমান প্রোফাইল ($activeProfileName): $budgetValueText"
                    } else {
                        budgetValueText
                    },
                    onClick = { showBudgetDialog = true },
                    testTag = "settings_budget_item"
                )
            }

            // Section 4: ABOUT
            item {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(title = "অ্যাপ সম্পর্কে")
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "অ্যাপের ভার্সন",
                    subtitle = "ভার্সন ১.০.০",
                    onClick = {},
                    showChevron = false,
                    testTag = "settings_version_item"
                )
            }
        }
    }

    // Dialog: Theme Selection
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = {
                Text(
                    text = "থিম পরিবর্তন করুন",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val themes = listOf(
                        Triple("Light", "লাইট মোড", Icons.Default.WbSunny),
                        Triple("Dark", "ডার্ক মোড", Icons.Default.Nightlight),
                        Triple("System", "সিস্টেম ডিফল্ট", Icons.Default.SettingsSuggest)
                    )
                    themes.forEach { (themeKey, themeLabel, themeIcon) ->
                        val isSelected = themePreference == themeKey
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.saveThemePreference(themeKey)
                                    showThemeDialog = false
                                }
                                .testTag("theme_opt_$themeKey"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = themeIcon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = themeLabel,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.saveThemePreference(themeKey)
                                        showThemeDialog = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("বাতিল", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            modifier = Modifier.testTag("theme_selection_dialog")
        )
    }

    // Dialog: Set Monthly Budget
    if (showBudgetDialog) {
        var budgetValStr by remember {
            val curBudget = activeProfile?.monthlyBudget ?: 0.0
            mutableStateOf(if (curBudget > 0.0) curBudget.toInt().toString() else "")
        }
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = {
                Text(
                    text = "মাসিক বাজেট নির্ধারণ",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val activeProfileName = activeProfile?.let {
                        if (it.name.contains("আমার পকেট")) "আমার পকেট"
                        else if (it.name.contains("সংসার বাজার")) "সংসার বাজার"
                        else if (it.name.contains("অফিস")) "অফিস প্যান্ট্রি"
                        else it.name.substringBefore(" (")
                    } ?: ""
                    Text(
                        text = "প্রোফাইল: $activeProfileName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = budgetValStr,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() }) {
                                budgetValStr = newValue
                            }
                        },
                        placeholder = {
                            Text(text = "বাজেটের পরিমাণ")
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_budget_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limit = budgetValStr.toDoubleOrNull() ?: 0.0
                        viewModel.updateMonthlyBudget(limit)
                        showBudgetDialog = false
                        android.widget.Toast.makeText(
                            context,
                            "মাসিক বাজেট সফলভাবে সেভ করা হয়েছে!",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("dialog_save_budget_btn")
                ) {
                    Text("সেভ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) {
                    Text("বাতিল", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            modifier = Modifier.testTag("budget_dialog")
        )
    }

    // Dialog: Reset/Delete All Data Warning
    if (showDeleteConfirmDialog) {
        DeleteConfirmationDialog(
            isBangla = true,
            title = "সতর্কবার্তা",
            message = "আপনি কি নিশ্চিত যে আপনি সকল ডাটা মুছে ফেলতে চান? আপনার সকল প্রোফাইল, খরচ এবং কেনাকাটার তালিকাও চিরতরে ডিলিট হয়ে যাবে। এই কাজটি আর ফিরিয়ে আনা সম্ভব নয়।",
            onDismiss = { showDeleteConfirmDialog = false },
            onConfirm = {
                viewModel.clearAllData()
                showDeleteConfirmDialog = false
                android.widget.Toast.makeText(
                    context,
                    "সকল ডাটা সফলভাবে মুছে ফেলা হয়েছে!",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.SemiBold
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("section_header_$title")
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showChevron: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.primary,
    testTag: String = ""
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (showChevron) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
