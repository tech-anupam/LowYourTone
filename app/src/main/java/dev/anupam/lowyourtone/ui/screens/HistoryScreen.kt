package dev.anupam.lowyourtone.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.anupam.lowyourtone.data.model.HistoryEntry
import dev.anupam.lowyourtone.ui.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(navController: NavController, viewModel: HistoryViewModel) {
    val historyGrouped by viewModel.historyGrouped.collectAsState(initial = emptyMap())
    val allWakeWords by viewModel.allWakeWords.collectAsState(initial = emptyList())
    val selectedFilter by viewModel.selectedWakeWordId.collectAsState()
    
    var selectedEntry by remember { mutableStateOf<HistoryEntry?>(null) }
    var filterExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HISTORY", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Export not hooked up to FileProvider in UI here */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Export")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                ExposedDropdownMenuBox(
                    expanded = filterExpanded,
                    onExpandedChange = { filterExpanded = it }
                ) {
                    androidx.compose.material3.OutlinedTextField(
                        value = allWakeWords.find { it.id == selectedFilter }?.phrase ?: "ALL WAKE WORDS",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = filterExpanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = filterExpanded,
                        onDismissRequest = { filterExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("ALL WAKE WORDS") },
                            onClick = {
                                viewModel.setFilter(null)
                                filterExpanded = false
                            }
                        )
                        allWakeWords.forEach { word ->
                            DropdownMenuItem(
                                text = { Text(word.phrase) },
                                onClick = {
                                    viewModel.setFilter(word.id)
                                    filterExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (historyGrouped.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("NO HISTORY FOUND", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    historyGrouped.forEach { (dateStr, entries) ->
                        stickyHeader {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = dateStr,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                        items(entries) { entry ->
                            HistoryEntryRow(entry, onClick = { selectedEntry = entry })
                        }
                    }
                }
            }
        }
    }

    selectedEntry?.let { entry ->
        ModalBottomSheet(
            onDismissRequest = { selectedEntry = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp).fillMaxWidth()) {
                Text("ENTRY DETAILS", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.padding(8.dp))
                Text("Phrase: ${entry.matchedPhrase}", style = MaterialTheme.typography.bodyLarge)
                Text("Time: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(entry.triggeredAt))}", style = MaterialTheme.typography.bodyLarge)
                Text("Confidence: ${(entry.confidence * 100).toInt()}%", style = MaterialTheme.typography.bodyLarge)
                Text("Action: ${entry.actionType.label}", style = MaterialTheme.typography.bodyLarge)
                Text("Success: ${entry.success}", style = MaterialTheme.typography.bodyLarge, color = if (entry.success) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary)
                entry.failureReason?.let {
                    Text("Error: $it", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
fun HistoryEntryRow(entry: HistoryEntry, onClick: () -> Unit) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeStr = timeFormat.format(Date(entry.triggeredAt))
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = timeStr, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(50.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = entry.matchedPhrase, style = MaterialTheme.typography.titleLarge)
            Text(text = entry.actionType.label, style = MaterialTheme.typography.bodySmall)
        }
        
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (entry.success) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
        ) {
            Text(
                text = "${(entry.confidence * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = if (entry.success) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
