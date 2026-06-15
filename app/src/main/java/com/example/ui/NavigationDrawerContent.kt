package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.ExpenseViewModel

@Composable
fun NavigationDrawerContent(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    viewModel: ExpenseViewModel,
    onCloseDrawer: () -> Unit,
    onThemeClick: () -> Unit,
    onBackupClick: () -> Unit,
    onAboutClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onContactClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isBangla = appLanguage == "Bangla"

    // Parse profile theme color or default
    val activeProfileColor = remember(activeProfile) {
        activeProfile?.let {
            try {
                Color(android.graphics.Color.parseColor(it.colorHex))
            } catch (e: Exception) {
                Color(0xFF6750A4)
            }
        } ?: Color(0xFF6750A4)
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(320.dp)
            .background(MaterialTheme.colorScheme.surface)
            .testTag("modal_drawer_content")
    ) {
        // App / Profile Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = activeProfileColor.copy(alpha = 0.12f)
                )
                .padding(top = 40.dp, bottom = 24.dp, start = 20.dp, end = 20.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Profile Icon/Avatar
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(activeProfileColor)
                            .border(width = 2.dp, color = Color.White, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (activeProfile?.name ?: "আ").take(1).uppercase(),
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                        )
                    }

                    Column {
                        Text(
                            text = activeProfile?.name ?: (if (isBangla) "আমার হিসাব" else "My Pocket"),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        val budget = activeProfile?.monthlyBudget ?: 0.0
                        if (budget > 0.0) {
                            Text(
                                text = if (isBangla) "বাজেট: ৳${String.format("%,.0f", budget)}" else "Budget: ৳${String.format("%,.0f", budget)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = if (isBangla) "কোনো বাজেট সেট নেই" else "No budget set",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Drawer items wrapper with layout weights so footer remains at bottom
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 12.dp, bottom = 8.dp)
        ) {
            // Main Section Header
            Text(
                text = if (isBangla) "প্রধান মেনু" else "Main Menu",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )

            // Main Section Items
            DrawerItem(
                label = if (isBangla) "ড্যাশবোর্ড" else "Dashboard",
                icon = Icons.Default.Dashboard,
                selected = currentRoute == "dashboard",
                onClick = {
                    onNavigate("dashboard")
                    onCloseDrawer()
                },
                tag = "drawer_dashboard"
            )

            DrawerItem(
                label = if (isBangla) "বাজারের ফর্দ" else "Shopping List",
                icon = Icons.Default.ShoppingCart,
                selected = currentRoute == "shopping",
                onClick = {
                    onNavigate("shopping")
                    onCloseDrawer()
                },
                tag = "drawer_shopping"
            )

            DrawerItem(
                label = if (isBangla) "ঋণ/পাওনা" else "Debts & Loans",
                icon = Icons.Default.Money,
                selected = currentRoute == "debt_list",
                onClick = {
                    onNavigate("debt_list")
                    onCloseDrawer()
                },
                tag = "drawer_debt"
            )

            DrawerItem(
                label = if (isBangla) "বিশ্লেষণ/রিপোর্ট" else "Analytics",
                icon = Icons.Default.Analytics,
                selected = currentRoute == "summary",
                onClick = {
                    onNavigate("summary")
                    onCloseDrawer()
                },
                tag = "drawer_summary"
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Settings Section Header
            Text(
                text = if (isBangla) "অন্যান্য" else "Settings & Options",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )

            // Settings Section Items
            DrawerItem(
                label = if (isBangla) "সেটিংস" else "Settings",
                icon = Icons.Default.Settings,
                selected = currentRoute?.startsWith("settings") == true && !currentRoute.contains("action="),
                onClick = {
                    onNavigate("settings")
                    onCloseDrawer()
                },
                tag = "drawer_settings"
            )

            DrawerItem(
                label = if (isBangla) "ডাটা ব্যাকআপ/রিস্টোর" else "Backup & Restore",
                icon = Icons.Default.Backup,
                selected = false,
                onClick = {
                    onBackupClick()
                    onCloseDrawer()
                },
                tag = "drawer_backup"
            )

            DrawerItem(
                label = if (isBangla) "থিম পরিবর্তন" else "Change Theme",
                icon = Icons.Default.Palette,
                selected = false,
                onClick = {
                    onThemeClick()
                    onCloseDrawer()
                },
                tag = "drawer_theme"
            )

            DrawerItem(
                label = if (isBangla) "অ্যাপ সম্পর্কে" else "About App",
                icon = Icons.Default.Info,
                selected = false,
                onClick = {
                    onAboutClick()
                    onCloseDrawer()
                },
                tag = "drawer_about"
            )

            DrawerItem(
                label = if (isBangla) "গোপনীয়তা নীতি" else "Privacy Policy",
                icon = Icons.Default.Policy,
                selected = currentRoute == "privacy_policy",
                onClick = {
                    onPrivacyClick()
                    onCloseDrawer()
                },
                tag = "drawer_privacy"
            )

            DrawerItem(
                label = if (isBangla) "যোগাযোগ করুন" else "Contact Us",
                icon = Icons.Default.ContactMail,
                selected = false,
                onClick = {
                    onContactClick()
                    onCloseDrawer()
                },
                tag = "drawer_contact"
            )
        }

        // Drawer Footer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(vertical = 16.dp, horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBangla) "আমার হিসাব" else "Amar Hisab",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text = if (isBangla) "ভার্সন ১.০.০" else "Version 1.0.0",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}
