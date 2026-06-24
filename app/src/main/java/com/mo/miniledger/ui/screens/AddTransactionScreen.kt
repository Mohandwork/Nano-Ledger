package com.mo.miniledger.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.mo.miniledger.ai.model.ExtractedTransaction
import com.mo.miniledger.ai.model.LineItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AddTransactionScreen(
    onBackClick: () -> Unit,
    onSaveClick: (Long, Map<String, String>, List<String>) -> Unit,
    isProcessing: Boolean = false,
    extractedTransaction: ExtractedTransaction? = null,
    receiptBitmap: Bitmap? = null,
    onReceiptPicked: (Bitmap) -> Unit,
    onClearExtracted: () -> Unit
) {
    var fields by remember { mutableStateOf(mapOf<String, String>()) }
    var flaggedItems by remember { mutableStateOf(listOf<String>()) }
    var items by remember { mutableStateOf(listOf<LineItem>()) }
    var timestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.READ_CALENDAR,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                val bitmap =
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, it))
                onReceiptPicked(bitmap)
            }
        }
    )

    LaunchedEffect(extractedTransaction) {
        extractedTransaction?.let {
            fields = it.fields
            flaggedItems = it.flaggedItems
            items = it.items
            it.suggestedTimestamp?.let { ts -> timestamp = ts }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Autonomous Entry", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .animateContentSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Receipt Preview
            if (receiptBitmap != null) {
                Image(
                    bitmap = receiptBitmap.asImageBitmap(),
                    contentDescription = "Receipt Preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
            }

            // Scan Button
            OutlinedButton(
                onClick = {
                    if (permissionsState.allPermissionsGranted) {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    } else {
                        permissionsState.launchMultiplePermissionRequest()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Analyzing...")
                } else {
                    Icon(Icons.Rounded.DocumentScanner, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan with AI")
                }
            }

            if (fields.isNotEmpty()) {
                TextButton(onClick = onClearExtracted, modifier = Modifier.align(Alignment.End)) {
                    Text("Reset AI Data")
                }
            }

            // Flagged Items Warning
            if (flaggedItems.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "AI Flagged Prohibited Items",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        flaggedItems.forEach { item ->
                            Text("• $item", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Compliance Analysis (Interactive Resolution)
            if (items.isNotEmpty()) {
                Text(
                    "Compliance Analysis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                items.forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = when (item.policyStatus) {
                                "APPROVED" -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                            }
                        ),
                        border = if (item.policyStatus != "APPROVED") {
                            CardDefaults.outlinedCardBorder()
                        } else null
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.description, fontWeight = FontWeight.SemiBold)
                                    Text(item.category, style = MaterialTheme.typography.labelSmall)
                                }
                                Text(
                                    formatCurrency(item.totalPrice),
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.policyStatus == "APPROVED") {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    }
                                )
                            }

                            if (item.policyStatus != "APPROVED") {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        item.violationReason ?: "Policy Violation",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    item.inlineActions.forEach { action ->
                                        AssistChip(
                                            onClick = {
                                                // 1. Update Total Amount in dynamic fields
                                                val currentAmountStr = fields["Amount"] ?: "0"
                                                val currentAmount = currentAmountStr.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
                                                val newAmount = currentAmount + action.resultingValueModifier
                                                fields = fields.toMutableMap().apply {
                                                    put("Amount", "%.2f".format(newAmount))
                                                }
                                                // 2. Remove the resolved item from UI list (Triggering animation)
                                                items = items.filter { it.itemId != item.itemId }
                                            },
                                            label = { Text(action.buttonText) },
                                            colors = when (action.uiStyleHint) {
                                                "PRIMARY" -> AssistChipDefaults.assistChipColors(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    labelColor = MaterialTheme.colorScheme.onPrimary
                                                )
                                                "SECONDARY" -> AssistChipDefaults.assistChipColors(
                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                                )
                                                else -> AssistChipDefaults.assistChipColors()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Dynamic Fields Generated from AI Schema
            if (fields.isNotEmpty()) {
                Text(
                    "AI-Extracted Data",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                val sortedFields = fields.entries.sortedByDescending {
                    when {
                        it.key.equals("Merchant", ignoreCase = true) -> 3
                        it.key.equals("Business Reason", ignoreCase = true) -> 2
                        it.key.equals("Amount", ignoreCase = true) -> 1
                        else -> 0
                    }
                }

                sortedFields.forEach { (key, value) ->
                    OutlinedTextField(
                        value = value,
                        onValueChange = { newValue ->
                            fields = fields.toMutableMap().apply {
                                put(key, newValue)
                            }
                        },
                        label = { Text(key) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        keyboardOptions = if (key.contains("Amount", ignoreCase = true)) {
                            KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        } else {
                            KeyboardOptions.Default
                        }
                    )
                }
            } else if (!isProcessing) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Scan a receipt to see dynamic fields",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    onSaveClick(timestamp, fields, flaggedItems)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large,
                enabled = fields.isNotEmpty()
            ) {
                Text("Save Dynamic Transaction", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
