package dev.anupam.lowyourtone.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dev.anupam.lowyourtone.data.model.ActionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionPickerScreen(navController: NavController) {
    var selectedAction by remember { mutableStateOf<ActionType?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choose Action", color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(ActionType.entries) { action ->
                ActionCard(action = action) {
                    selectedAction = action
                }
            }
        }
    }

    if (selectedAction != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedAction = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            ActionConfigForm(
                actionType = selectedAction!!,
                onDone = { params ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("selected_action_type", selectedAction!!.name)
                    navController.previousBackStackEntry?.savedStateHandle?.set("selected_action_params", params)
                    navController.navigateUp()
                }
            )
        }
    }
}

@Composable
fun ActionCard(action: ActionType, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = getActionIcon(action),
                contentDescription = action.label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = action.label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ActionConfigForm(actionType: ActionType, onDone: (Map<String, String>) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Text(
            text = actionType.label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        when (actionType) {
            ActionType.CALL_CONTACT -> CallContactForm(onDone)
            ActionType.CALL_EMERGENCY -> CallEmergencyForm(onDone)
            ActionType.SEND_SMS -> SendSmsForm(onDone)
            ActionType.SEND_WHATSAPP -> SendSmsForm(onDone)
            ActionType.START_AUDIO_RECORD -> AudioRecordForm(onDone)
            ActionType.SEND_LOCATION_SMS -> SendLocationForm(onDone)
            ActionType.OPEN_APP -> OpenAppForm(onDone)
            ActionType.PLAY_ALARM_SOUND -> AlarmSoundForm(onDone)
            ActionType.DISCO_FLASH -> DiscoFlashForm(onDone)
            ActionType.CUSTOM_INTENT -> CustomIntentForm(onDone)
            else -> NoConfigForm(onDone)
        }
    }
}

@Composable
fun NoConfigForm(onDone: (Map<String, String>) -> Unit) {
    Text(
        text = "No setup needed",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 16.sp,
        modifier = Modifier.padding(bottom = 24.dp)
    )
    DoneButton { onDone(emptyMap()) }
}

@Composable
fun CallContactForm(onDone: (Map<String, String>) -> Unit) {
    var phoneNumber by remember { mutableStateOf("") }
    ContactPickerField("Phone Number", phoneNumber) { phoneNumber = it }
    Spacer(modifier = Modifier.height(24.dp))
    DoneButton(enabled = phoneNumber.isNotBlank()) {
        onDone(mapOf("phoneNumber" to phoneNumber))
    }
}

@Composable
fun CallEmergencyForm(onDone: (Map<String, String>) -> Unit) {
    var number by remember { mutableStateOf("") }
    var emergencyContact by remember { mutableStateOf("") }

    OutlinedTextField(
        value = number,
        onValueChange = { number = it },
        label = { Text("Emergency Number (e.g. 911)") },
        modifier = Modifier.fillMaxWidth(),
        colors = textFieldColors()
    )
    Spacer(modifier = Modifier.height(16.dp))
    ContactPickerField("Also notify this number", emergencyContact) { emergencyContact = it }
    Spacer(modifier = Modifier.height(24.dp))
    DoneButton(enabled = number.isNotBlank()) {
        val params = mutableMapOf("number" to number)
        if (emergencyContact.isNotBlank()) {
            params["emergencyContact"] = emergencyContact
            params["sendLocationSms"] = "true"
        }
        onDone(params)
    }
}

@Composable
fun SendSmsForm(onDone: (Map<String, String>) -> Unit) {
    var phoneNumber by remember { mutableStateOf("") }
    var messageTemplate by remember { mutableStateOf("") }

    ContactPickerField("Phone Number", phoneNumber) { phoneNumber = it }
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(
        value = messageTemplate,
        onValueChange = { messageTemplate = it },
        label = { Text("Message") },
        modifier = Modifier.fillMaxWidth(),
        colors = textFieldColors(),
        minLines = 3
    )
    Spacer(modifier = Modifier.height(8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(listOf("{time}", "{date}", "{location}")) { tag ->
            Chip(tag) { messageTemplate += tag }
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
    DoneButton(enabled = phoneNumber.isNotBlank() && messageTemplate.isNotBlank()) {
        onDone(mapOf("phoneNumber" to phoneNumber, "messageTemplate" to messageTemplate))
    }
}

@Composable
fun AudioRecordForm(onDone: (Map<String, String>) -> Unit) {
    var durationSeconds by remember { mutableStateOf("30") }
    OutlinedTextField(
        value = durationSeconds,
        onValueChange = { durationSeconds = it },
        label = { Text("How long? (seconds)") },
        modifier = Modifier.fillMaxWidth(),
        colors = textFieldColors()
    )
    Spacer(modifier = Modifier.height(24.dp))
    DoneButton {
        onDone(mapOf("durationSeconds" to durationSeconds))
    }
}

@Composable
fun SendLocationForm(onDone: (Map<String, String>) -> Unit) {
    var phoneNumber by remember { mutableStateOf("") }
    ContactPickerField("Send location to", phoneNumber) { phoneNumber = it }
    Spacer(modifier = Modifier.height(24.dp))
    DoneButton(enabled = phoneNumber.isNotBlank()) {
        onDone(mapOf("phoneNumber" to phoneNumber))
    }
}

@Composable
fun OpenAppForm(onDone: (Map<String, String>) -> Unit) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedPackage by remember { mutableStateOf("") }
    
    val apps = remember {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PackageManager.MATCH_ALL else 0
        pm.queryIntentActivities(intent, flags).map { resolveInfo ->
            Pair(
                resolveInfo.loadLabel(pm).toString(),
                resolveInfo.activityInfo.packageName
            )
        }.distinctBy { it.second }.sortedBy { it.first.lowercase() }
    }

    val filteredApps = apps.filter { it.first.contains(searchQuery, ignoreCase = true) }

    OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        label = { Text("Search Apps") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        modifier = Modifier.fillMaxWidth(),
        colors = textFieldColors()
    )
    Spacer(modifier = Modifier.height(16.dp))
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        items(filteredApps) { app ->
            val isSelected = selectedPackage == app.second
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedPackage = app.second }
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .padding(12.dp)
            ) {
                Column {
                    Text(text = app.first, color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 16.sp)
                    Text(text = app.second, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
    DoneButton(enabled = selectedPackage.isNotBlank()) {
        onDone(mapOf("packageName" to selectedPackage))
    }
}

@Composable
fun AlarmSoundForm(onDone: (Map<String, String>) -> Unit) {
    var durationSeconds by remember { mutableStateOf("10") }
    OutlinedTextField(
        value = durationSeconds,
        onValueChange = { durationSeconds = it },
        label = { Text("How long? (seconds)") },
        modifier = Modifier.fillMaxWidth(),
        colors = textFieldColors()
    )
    Spacer(modifier = Modifier.height(24.dp))
    DoneButton {
        onDone(mapOf("durationSeconds" to durationSeconds))
    }
}

@Composable
fun DiscoFlashForm(onDone: (Map<String, String>) -> Unit) {
    var durationSeconds by remember { mutableStateOf("10") }
    OutlinedTextField(
        value = durationSeconds,
        onValueChange = { durationSeconds = it },
        label = { Text("How long? (seconds)") },
        modifier = Modifier.fillMaxWidth(),
        colors = textFieldColors()
    )
    Spacer(modifier = Modifier.height(24.dp))
    DoneButton {
        onDone(mapOf("durationSeconds" to durationSeconds))
    }
}

@Composable
fun CustomIntentForm(onDone: (Map<String, String>) -> Unit) {
    var action by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("") }

    OutlinedTextField(
        value = action,
        onValueChange = { action = it },
        label = { Text("Intent Action") },
        modifier = Modifier.fillMaxWidth(),
        colors = textFieldColors()
    )
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(
        value = packageName,
        onValueChange = { packageName = it },
        label = { Text("Package (optional)") },
        modifier = Modifier.fillMaxWidth(),
        colors = textFieldColors()
    )
    Spacer(modifier = Modifier.height(24.dp))
    DoneButton(enabled = action.isNotBlank()) {
        onDone(mapOf("action" to action, "package" to packageName))
    }
}

@Composable
fun ContactPickerField(label: String, value: String, onValueChange: (String) -> Unit) {
    val context = LocalContext.current
    val contactPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri ->
        uri?.let {
            val cursor = context.contentResolver.query(it, null, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val hasPhoneIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                
                if (hasPhoneIndex != -1 && idIndex != -1) {
                    val hasPhone = cursor.getString(hasPhoneIndex)
                    val id = cursor.getString(idIndex)
                    
                    if (hasPhone == "1") {
                        val phones = context.contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id,
                            null,
                            null
                        )
                        if (phones != null && phones.moveToFirst()) {
                            val numberIndex = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            if (numberIndex != -1) {
                                val number = phones.getString(numberIndex)
                                onValueChange(number.replace(Regex("[^0-9+]"), ""))
                            }
                            phones.close()
                        }
                    }
                }
                cursor.close()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            contactPicker.launch(null)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.weight(1f),
            colors = textFieldColors()
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
            modifier = Modifier
                .size(56.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
        ) {
            Icon(Icons.Default.Contacts, contentDescription = "Pick Contact", tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun DoneButton(enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text("Done", color = MaterialTheme.colorScheme.onPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Chip(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = text, color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 12.sp)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun textFieldColors(): TextFieldColors {
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        cursorColor = MaterialTheme.colorScheme.primary
    )
}
