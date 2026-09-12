package com.itantra.wifi

import android.Manifest
import android.content.pm.PackageManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var tts: TextToSpeech
    private lateinit var nsdHelper: NsdHelper
    private var discoveredServices = mutableStateOf<List<NsdServiceInfo>>(emptyList())

    private val permissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val basePermissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val allPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            basePermissions + Manifest.permission.NEARBY_WIFI_DEVICES
        } else {
            basePermissions
        }

        permissions.launch(allPermissions)

        nsdHelper = NsdHelper(this) { services ->
            discoveredServices.value = services
        }
        nsdHelper.registerService()

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.ENGLISH
            }
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ItontraScreen(
                        discoveredServices = discoveredServices.value,
                        onDiscoveryToggle = { start ->
                            if (start) nsdHelper.discoverServices()
                            else nsdHelper.stopDiscovery()
                        },
                        onSpeak = { text, locale ->
                            tts.language = locale
                            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "itantra")
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        nsdHelper.tearDown()
        super.onDestroy()
    }
}

data class Message(val text: String, val fromMe: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItontraScreen(
    discoveredServices: List<NsdServiceInfo>,
    onDiscoveryToggle: (Boolean) -> Unit,
    onSpeak: (String, Locale) -> Unit
) {
    var language by remember { mutableStateOf("English") }
    var connected by remember { mutableStateOf(false) }
    var isDiscovering by remember { mutableStateOf(false) }
    var pairedDevice by remember { mutableStateOf<NsdServiceInfo?>(null) }
    var listening by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<Message>()) }

    val languages = listOf(
        "English" to Locale.ENGLISH,
        "Hindi" to Locale("hi"),
        "Bengali" to Locale("bn"),
        "Gujarati" to Locale("gu"),
        "Marathi" to Locale("mr"),
        "Kannada" to Locale("kn"),
        "Malayalam" to Locale("ml"),
        "Tamil" to Locale("ta"),
        "Telugu" to Locale("te"),
        "Odia" to Locale("or")
    )
    val selectedLocale = languages.first { it.first == language }.second

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Column {
                    Text("iTantra", style = MaterialTheme.typography.titleLarge)
                    Text("Offline Wi-Fi Voice Transceiver",
                        style = MaterialTheme.typography.labelMedium)
                }
            })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Connection", style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f))
                            Text(
                                if (connected) "● PAIRED: ${pairedDevice?.serviceName}" else "● NOT CONNECTED",
                                color = if (connected)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                isDiscovering = !isDiscovering
                                onDiscoveryToggle(isDiscovering)
                            }) {
                                Text(if (isDiscovering) "Stop Discovery" else "Scan for Devices")
                            }
                            if (connected) {
                                Button(onClick = {
                                    connected = false
                                    pairedDevice = null
                                }) {
                                    Text("Disconnect")
                                }
                            }
                        }
                        
                        if (isDiscovering && !connected) {
                            Spacer(Modifier.height(10.dp))
                            Text("Nearby Devices:", style = MaterialTheme.typography.labelMedium)
                            if (discoveredServices.isEmpty()) {
                                Text("No devices found yet...", style = MaterialTheme.typography.bodySmall)
                            }
                            discoveredServices.forEach { service ->
                                TextButton(onClick = {
                                    pairedDevice = service
                                    connected = true
                                    isDiscovering = false
                                    onDiscoveryToggle(false)
                                }) {
                                    Text("Connect to ${service.serviceName} (${service.host?.hostAddress})")
                                }
                            }
                        }

                        Text(
                            "Offline Wi-Fi pairing enabled via NSD. "
                                    + "Select a device to start low-latency voice exchange.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded,
                    onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = language,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Output language") },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded,
                        onDismissRequest = { expanded = false }) {
                        languages.forEach { (name, _) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = { language = name; expanded = false }
                            )
                        }
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(24.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            if (listening) "Listening…" else "Push-to-Talk",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            enabled = connected,
                            onClick = {
                                listening = !listening
                                if (!listening && draft.isNotBlank()) {
                                    messages = messages + Message(draft, true)
                                    onSpeak(draft, selectedLocale)
                                    draft = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(58.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Text(if (listening) "RELEASE TO SEND" else "HOLD / TAP TO TALK")
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            label = { Text("Text fallback / test packet") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "This starter uses Android TTS locally. "
                                    + "Add an open-source offline STT model for full SIH operation.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item {
                Text("Performance", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()) {
                    MetricCard("STT", "—")
                    MetricCard("TX", "—")
                    MetricCard("E2E", "—")
                }
            }

            item { Text("Conversation", style = MaterialTheme.typography.titleMedium) }

            items(messages) { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(if (msg.fromMe) "YOU" else "REMOTE",
                            style = MaterialTheme.typography.labelSmall)
                        Text(msg.text)
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.MetricCard(title: String, value: String) {
    Card(modifier = Modifier.weight(1f)) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}