package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CloudDownload
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import com.example.ui.theme.HindSiliguri
import com.example.ui.theme.NotoSansBengali
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ProfileEntity
import com.example.viewmodel.ExpenseViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.viewmodel.BackupViewModel
import com.example.viewmodel.BackupUiState
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSwitcherAppBar(
    viewModel: ExpenseViewModel,
    titleText: String,
    modifier: Modifier = Modifier
) {
    val profiles by viewModel.allProfiles.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isBangla = appLanguage == "Bangla"

    var isMenuExpanded by remember { mutableStateOf(false) }
    var showCreateProfileDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else if (isMenuExpanded) 0.98f else 1.0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "profile_scale"
    )

    val activeProfileColor = remember(activeProfile) {
        activeProfile?.let { parseHexColor(it.colorHex) } ?: Color(0xFF6750A4)
    }

    val bgColor by animateColorAsState(
        targetValue = if (isMenuExpanded) {
            activeProfileColor.copy(alpha = 0.15f)
        } else if (isPressed) {
            activeProfileColor.copy(alpha = 0.1f)
        } else {
            activeProfileColor.copy(alpha = 0.05f)
        },
        label = "profile_bg_color"
    )

    TopAppBar(
        modifier = modifier.testTag("profile_switcher_topappbar"),
        title = {
            Text(
                text = if (isBangla) titleText else "Weekly Pantry",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        actions = {
            activeProfile?.let { prof ->
                Box(modifier = Modifier.wrapContentSize()) {
                    Row(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clip(RoundedCornerShape(100.dp))
                            .background(bgColor)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = LocalIndication.current,
                                onClick = { isMenuExpanded = true }
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("clickable_profile_section"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profile Icon/Avatar
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(activeProfileColor)
                                .testTag("avatar_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = prof.name.substringBefore(" ").take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Profile Name
                        Text(
                            text = if (prof.name.contains("আমার পকেট")) {
                                if (isBangla) "আমার পকেট" else "Personal"
                            } else if (prof.name.contains("সংসার বাজার")) {
                                if (isBangla) "সংসার বাজার" else "Family"
                            } else if (prof.name.contains("অফিস")) {
                                if (isBangla) "অফিস প্যান্ট্রি" else "Office Pantry"
                            } else {
                                if (isBangla) prof.name.substringBefore(" (") else prof.name
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("active_profile_name_text")
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        // Small dropdown arrow
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Show profile options",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false },
                        modifier = Modifier
                            .width(260.dp)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            text = if (isBangla) "প্রোফাইল পরিবর্তন করুন" else "Switch Profile",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )

                        profiles.forEach { profile ->
                            val isSelected = profile.id == prof.id
                            val pName = if (profile.name.contains("আমার পকেট")) {
                                if (isBangla) "আমার পকেট" else "Personal Pocket"
                            } else if (profile.name.contains("সংসার বাজার")) {
                                if (isBangla) "সংসার বাজার" else "Family Grocery"
                            } else if (profile.name.contains("অফিস")) {
                                if (isBangla) "অফিস প্যান্ট্রি" else "Office Pantry"
                            } else {
                                if (isBangla) profile.name.substringBefore(" (") else profile.name
                            }

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(parseHexColor(profile.colorHex))
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = pName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.weight(1f))
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    viewModel.selectProfile(profile.id)
                                    isMenuExpanded = false
                                },
                                modifier = Modifier.testTag("profile_item_${profile.id}")
                            )
                        }

                        Divider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = if (isBangla) "নতুন প্রোফাইল যোগ করুন" else "Add New Profile",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            onClick = {
                                showCreateProfileDialog = true
                                isMenuExpanded = false
                            },
                            modifier = Modifier.testTag("add_profile_dropdown_btn")
                        )

                        Divider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = if (isBangla) "অ্যাপ সেটিংস" else "App Settings",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            onClick = {
                                showSettingsDialog = true
                                isMenuExpanded = false
                            },
                            modifier = Modifier.testTag("settings_dropdown_btn")
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        )
    )

    if (showCreateProfileDialog) {
        CreateProfileDialog(
            isBangla = isBangla,
            onDismiss = { showCreateProfileDialog = false },
            onConfirm = { name, colorHex ->
                viewModel.insertProfile(name, 0, colorHex)
                showCreateProfileDialog = false
            }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            viewModel = viewModel,
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    viewModel: ExpenseViewModel,
    onDismiss: () -> Unit
) {
    val profiles by viewModel.allProfiles.collectAsStateWithLifecycle()
    val defaultProfileId by viewModel.defaultProfileId.collectAsStateWithLifecycle()
    val defaultCurrency by viewModel.defaultCurrency.collectAsStateWithLifecycle()
    val themePreference by viewModel.themePreference.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

    val isBangla = appLanguage == "Bangla"

    val context = LocalContext.current
    val backupViewModel: BackupViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isBangla) "অ্যাপ্লিকেশন সেটিংস" else "App Settings",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Language Preference
                Column {
                    Text(
                        text = if (isBangla) "ভাষার পছন্দ" else "App Language",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Bangla", "English").forEach { lang ->
                            val isSelected = appLanguage == lang
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.saveAppLanguage(lang) },
                                label = { Text(if (lang == "Bangla") "বাংলা" else "English") }
                            )
                        }
                    }
                }

                // 2. Theme Preference
                ThemePreferenceSection(
                    selectedTheme = themePreference,
                    onThemeSelected = { viewModel.saveThemePreference(it) },
                    isBangla = isBangla
                )

                // 3. Default Currency Selection
                Column {
                    Text(
                        text = if (isBangla) "ডিফল্ট মুদ্রা" else "Default Currency",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("৳", "$", "£", "₹").forEach { cur ->
                            val isSelected = defaultCurrency == cur
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.saveDefaultCurrency(cur) },
                                label = { Text(cur) }
                            )
                        }
                    }
                }

                // 4. Default Profile ID Preference
                Column {
                    Text(
                        text = if (isBangla) "ডিফল্ট প্রোফাইল নির্বাচন করুন" else "Default Launch Profile",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    profiles.forEach { profile ->
                        val isDefaultLaunch = defaultProfileId == profile.id
                        val pName = if (profile.name.contains("আমার পকেট")) {
                            if (isBangla) "আমার পকেট" else "Personal Pocket"
                        } else if (profile.name.contains("সংসার বাজার")) {
                            if (isBangla) "সংসার বাজার" else "Family Grocery"
                        } else if (profile.name.contains("অফিস")) {
                            if (isBangla) "অফিস প্যান্ট্রি" else "Office Pantry"
                        } else {
                            if (isBangla) profile.name.substringBefore(" (") else profile.name
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isDefaultLaunch) {
                                        viewModel.saveDefaultProfileId(null)
                                    } else {
                                        viewModel.saveDefaultProfileId(profile.id)
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isDefaultLaunch,
                                onClick = {
                                    if (isDefaultLaunch) {
                                        viewModel.saveDefaultProfileId(null)
                                    } else {
                                        viewModel.saveDefaultProfileId(profile.id)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(profile.colorHex))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = pName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // 5. Data Backup & Restore Selection
                Column {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    Text(
                        text = if (isBangla) "ডাটা ব্যাকআপ ও পুনরুদ্ধার" else "Data Backup & Restore",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                createDocumentLauncher.launch("amar_hisab_backup_$dateStr.json")
                            },
                            modifier = Modifier.weight(1f).testTag("backup_export_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Export Backup",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBangla) "ব্যাকআপ ফাইল তৈরি" else "Create Backup",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                openDocumentLauncher.launch(arrayOf("application/json", "*/*"))
                            },
                            modifier = Modifier.weight(1f).testTag("backup_import_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "Import Backup",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBangla) "ব্যাকআপ পুনরুদ্ধার" else "Restore Backup",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isBangla) "আপনার হিসাবের ব্যাকআপ ফাইলটি নিরাপদ জায়গায় সংরক্ষণ করুন।" else "Securely store your json backup document in a secure location.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(if (isBangla) "সম্পন্ন" else "Done")
            }
        }
    )
}

@Composable
fun ThemePreferenceSection(
    selectedTheme: String,
    onThemeSelected: (String) -> Unit,
    isBangla: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = if (isBangla) "থিম পছন্দ" else "App Theme",
            fontFamily = HindSiliguri,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val options = listOf(
            Triple("Light", if (isBangla) "লাইট মোড" else "Light Mode", Icons.Default.WbSunny),
            Triple("Dark", if (isBangla) "ডার্ক মোড" else "Dark Mode", Icons.Default.Nightlight),
            Triple("System", if (isBangla) "সিস্টেম ডিফল্ট" else "System Default", Icons.Default.SettingsSuggest)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, (themeKey, label, icon) ->
                    val isSelected = selectedTheme == themeKey
                    val rowBgColor = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    } else {
                        Color.Transparent
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .background(rowBgColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(),
                                onClick = { onThemeSelected(themeKey) }
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = label,
                                fontFamily = NotoSansBengali,
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick = { onThemeSelected(themeKey) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    if (index < options.lastIndex) {
                        Divider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreateProfileDialog(
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var profileName by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf("#9C27B0") }

    val colorsList = listOf(
        "#9C27B0", // Purple
        "#3F51B5", // Indigo
        "#E91E63", // Pink
        "#009688", // Teal
        "#4CAF50", // Green
        "#FF9800", // Orange
        "#FF5722"  // Deep Orange
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "নতুন প্রোফাইল তৈরি করুন", // Create New Profile (Bangla)
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text("প্রোফাইলের নাম (Name)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_profile_name_input")
                )

                Column {
                    Text(
                        text = "থিম কালার নির্বাচন করুন", // Choose theme color (Bangla)
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        colorsList.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(hex))
                                    .clickable { selectedColorHex = hex }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedColorHex == hex) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.5f))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (profileName.isNotBlank()) {
                        onConfirm(profileName.trim(), selectedColorHex)
                    }
                },
                enabled = profileName.isNotBlank(),
                modifier = Modifier.testTag("confirm_create_profile_btn")
            ) {
                Text("তৈরি করুন (Create)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল (Cancel)")
            }
        }
    )
}

fun parseHexColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        Color(0xFF6750A4)
    }
}
