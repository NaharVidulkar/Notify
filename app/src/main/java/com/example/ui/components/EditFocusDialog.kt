package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NotifyTheme

@Composable
fun EditFocusDialog(
    currentFocus: String,
    currentSubtext: String,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var focus by remember { mutableStateOf(currentFocus) }
    var subtext by remember { mutableStateOf(currentSubtext) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Today's Focus",
                fontSize = 18.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = focus,
                    onValueChange = { focus = it },
                    label = { Text("Primary Focus") },
                    placeholder = { Text("e.g. DSA – Graphs & DP") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = subtext,
                    onValueChange = { subtext = it },
                    label = { Text("Subtitle / Track") },
                    placeholder = { Text("e.g. 5 reminders today · Java track") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(focus, subtext) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = NotifyTheme.colors.accent,
                    contentColor = NotifyTheme.colors.onAccent
                ),
                shape = RoundedCornerShape(50)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = NotifyTheme.colors.mutedText)
            }
        },
        containerColor = NotifyTheme.colors.cardBg,
        shape = RoundedCornerShape(20.dp)
    )
}
