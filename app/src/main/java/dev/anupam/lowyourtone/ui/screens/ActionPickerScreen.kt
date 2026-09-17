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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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

private data class ActionCategory(
    val title: String,
    val emoji: String,
    val actions: List<ActionType>
)

private val ACTION_CATEGORIES = listOf(
    ActionCategory(
        title = "Emergency",
        emoji = "🆘",
        actions = listOf(ActionType.CALL_EMERGENCY, ActionType.CALL_CONTACT, ActionType.SEND_LOCATION_SMS)
    ),
    ActionCategory(
        title = "Message",
        emoji = "💬",
        actions = listOf(ActionType.SEND_SMS, ActionType.SEND_WHATSAPP)
    ),
    ActionCategory(
        title = "Alert",
        emoji = "🔊",
        actions = listOf(ActionType.PLAY_ALARM_SOUND, ActionType.TOGGLE_FLASHLIGHT, ActionType.DISCO_FLASH)
    ),
    ActionCategory(
        title = "Record",
        emoji = "🎙️",
        actions = listOf(ActionType.START_AUDIO_RECORD, ActionType.START_VIDEO_RECORD)
    ),
    ActionCategory(
        title = "Phone",
        emoji = "📱",
        actions = listOf(
            ActionType.LOCK_SCREEN,
            ActionType.TOGGLE_SILENT,
            ActionType.TOGGLE_DND,
            ActionType.MAX_VOLUME,
            ActionType.STOP_MEDIA
        )
    ),
    ActionCategory(
        title = "Apps & Custom",
        emoji = "⚡",
        actions = listOf(ActionType.OPEN_APP, ActionType.CUSTOM_INTENT)
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionPickerScreen(navController: NavController) {
    var selectedAction by remember { mutableStateOf<ActionType?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Choose Action",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "What should happen when you say the word?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            ACTION_CATEGORIES.forEach { category ->
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    ) {
                        Text(
                            text = category.emoji,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = category.title.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.5.sp
                        )
                    }
                }

                val chunked = category.actions.chunked(2)
                chunked.forEach { rowActions ->
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowActions.forEach { action ->
                                ActionRow(
                                    action = action,
                                    modifier = Modifier.weight(1f),
                                    onClick = { selectedAction = action }
                                )
                            }
                            if (rowActions.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(4.dp)) }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
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
fun ActionRow(action: ActionType, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier
            .height(72.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getActionIcon(action),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = action.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )
        }
    }
}

@Composable
fun ActionCard(action: ActionType, onClick: () -> Unit) {
    ActionRow(action = action, onClick = onClick)
}

@Composable
fun ActionConfigForm(actionType: ActionType, onDone: (Map<String, String>) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getActionIcon(actionType),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = actionType.label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(20.dp))

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

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun NoConfigForm(onDone: (Map<String, String>) -> Unit) {
    Text(
        text = "No extra setup needed. Just say your word.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(bottom = 20.dp)
    )
    DoneButton { onDone(emptyMap()) }
}

@Composable
fun CallContactForm(onDone: (Map<String, String>) -> Unit) {
    var phoneNumber by remember { mutableStateOf("") }
    Text(
        "Who should be called?",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(bottom = 10.dp)
    )
    ContactPickerField("Phone Number", phoneNumber) { phoneNumber = it }
    Spacer(modifier = Modifier.height(20.dp))
    DoneButton(enabled = phoneNumber.isNotBlank()) {
        onDone(mapOf("phoneNumber" to phoneNumber))
    }
}

@Composable
fun CallEmergencyForm(onDone: (Map<String, String>) -> Unit) {
    var number by remember { mutableStateOf("112") }
    var emergencyContact by remember { mutableStateOf("") }

    Text(
        text = "Indian emergency numbers",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(bottom = 8.dp)
    )

    val presets = listOf(
        "112" to "112 (All SOS)",
        "100" to "100 Police",
        "101" to "101 Fire",
        "108" to "108 Ambulance",
        "1091" to "1091 Women"
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        presets.forEach { (num, label) ->
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { number = num },
                shape = RoundedCornerShape(8.dp),
                color = if (number == num)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = num,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (number == num)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = label.substringAfter(" "),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (number == num)
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))
    OutlinedTextField(
        value = number,
        onValueChange = { number = it },
        label = { Text("Emergency Number") },
        modifier = Modifier.fillMaxWidth(),
        colors = textFieldColors(),
        singleLine = true
    )
    Spacer(modifier = Modifier.height(12.dp))
    ContactPickerField("Also SMS location to (optional)", emergencyContact) { emergencyContact = it }
    Spacer(modifier = Modifier.height(20.dp))
    DoneButton(enabled = true) {
        val finalNumber = number.trim().ifBlank { "112" }
        val params = mutableMapOf("number" to finalNumber)
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

    ContactPickerField("Send to", phoneNumber) { phoneNumber = it }
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(
        value = messageTemplate,
        onValueChange = { messageTemplate = it },
        label = { Text("Message") },
        modifier = Modifier.fillMaxWidth(),
        colors = textFieldColors(),
        minLines = 3
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("{time}", "{date}", "{location}").forEach { tag ->
            Chip(tag) { messageTemplate += tag }
        }
    }
    Spacer(modifier = Modifier.height(20.dp))
    DoneButton(enabled = phoneNumber.isNotBlank() && messageTemplate.isNotBlank()) {
        onDone(mapOf("phoneNumber" to phoneNumber, "messageTemplate" to messageTemplate))
    }
}

@Composable
fun AudioRecordForm(onDone: (Map<String, String>) -> Unit) {
    var durationSeconds by remember { mutableStateOf("30") }
    Text(
        "How long should it record?",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(bottom = 10.dp)
    )
    OutlinedTextField(
        value = durationSeconds,
        onValueChange = { durationSeconds = it },
        label = { Text("Duration (seconds)") },
        modifier = Modifier.fillMaxWidth(),
        colors = textFieldColors(),
        singleLine = true
    )
    Spacer(modifier = Modifier.height(20.dp))
    DoneButton {
        onDone(mapOf("durationSeconds" to durationSeconds))
    }
}

@Composable
fun SendLocationForm(onDone: (Map<String, String>) -> Unit) {
    var phoneNumber by remember { mutableStateOf("") }
    Text(
        "Sends your GPS location as a text message.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(bottom = 10.dp)
    )
    ContactPickerField("Send to", phoneNumber) { phoneNumber = it }
    Spacer(modifier = Modifier.height(20.dp))
    DoneButton(enabled = phoneNumber.isNotBlank()) {
        onDone(mapOf("phoneNumber" to phoneNumber))
    }
}

@Composable
fun OpenAppForm(onDone: (Map<String, String>) -> Unit) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedPackage by remember { mutableStateOf("") }
    var selectedLabel by remember { mutableStateOf("") }

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
        label = { Text("Search apps") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        modifier = Modifier.fillMaxWidth(),
        colors = textFieldColors(),
        singleLine = true
    )
    Spacer(modifier = Modifier.height(10.dp))

    if (selectedLabel.isNotBlank()) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Selected: $selectedLabel",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(12.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
    }

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
                    .clickable {
                        selectedPackage = app.second
                        selectedLabel = app.first
                    }
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else Color.Transparent
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = app.first,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(20.dp))
    DoneButton(enabled = selectedPackage.isNotBlank()) {
        onDone(mapOf("packageName" to selectedPackage))
    }
}

@Composable
fun AlarmSoundForm(onDone: (Map<String, String>) -> Unit) {
    var durationSeconds by remember { mutableStateOf("10") }
    Text(
        "Plays a loud alarm sound.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(bottom = 10.dp)
    )
    OutlinedTextField(
        value = durationSeconds,
        onValueChange = { durationSeconds = it },
        label = { Text("Duration (seconds)") },
        modifier = Modifier.fillMaxWidth(),
        colors = textFieldColors(),
        singleLine = true
    )
    Spacer(modifier = Modifier.height(20.dp))
    DoneButton {
        onDone(mapOf("durationSeconds" to durationSeconds))
    }
}

@Composable
fun DiscoFlashForm(onDone: (Map<String, String>) -> Unit) {
    var durationSeconds by remember { mutableStateOf("10") }
    Text(
        "Flashes the torch rapidly as a distress signal.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(bottom = 10.dp)
    )
    OutlinedTextField(
        value = durationSeconds,
        onValueChange = { durationSeconds = it },
        label = { Text("Duration (seconds)") },
        modifier = Modifier.fillMaxWidth(),
        colors = textFieldColors(),
        singleLine = true
    )
    Spacer(modifier = Modifier.height(20.dp))
    DoneButton {
        onDone(mapOf("durationSeconds" to durationSeconds))
    }
}

@Composable
fun CustomIntentForm(onDone: (Map<String, String>) -> Unit) {
    var action by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("") }

    Text(
        "Advanced: send a raw Android intent.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(bottom = 10.dp)
    )
    OutlinedTextField(
        value = action,
        onValueChange = { action = it },
        label = { Text("Intent action (e.g. android.intent.action.VIEW)") },
        modifier = Modifier.fillMaxWidth(),
        colors = textFieldColors(),
        singleLine = true
    )
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(
        value = packageName,
        onValueChange = { packageName = it },
        label = { Text("Package name (optional)") },
        modifier = Modifier.fillMaxWidth(),
        colors = textFieldColors(),
        singleLine = true
    )
    Spacer(modifier = Modifier.height(20.dp))
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
            colors = textFieldColors(),
            singleLine = true
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
            modifier = Modifier
                .size(56.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
        ) {
            Icon(
                Icons.Default.Contacts,
                contentDescription = "Pick contact",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
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
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            "Done",
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
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
