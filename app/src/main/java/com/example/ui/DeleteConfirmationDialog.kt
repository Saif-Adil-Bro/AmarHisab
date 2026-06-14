package com.example.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.DialogProperties

/**
 * A beautiful, reusable Material 3 Delete Confirmation Dialog in Bangla & English.
 */
@Composable
fun DeleteConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    isBangla: Boolean = true,
    title: String? = null,
    message: String? = null
) {
    val dialogTitle = title ?: if (isBangla) "সতর্কবার্তা" else "Warning"
    val dialogMessage = message ?: if (isBangla) {
        "আপনি কি নিশ্চিত যে আপনি এটি মুছে ফেলতে চান? এই কাজটি পূর্বাবস্থায় ফেরানো যাবে না।"
    } else {
        "Are you sure you want to delete this item? This action cannot be undone."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = dialogTitle,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Text(
                text = dialogMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.testTag("confirm_delete_dialog_btn")
            ) {
                Text(if (isBangla) "মুছে ফেলুন" else "Delete")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_delete_dialog_btn")
            ) {
                Text(
                    text = if (isBangla) "বাতিল" else "Cancel",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = true
        ),
        modifier = modifier.testTag("delete_confirmation_dialog")
    )
}
