package com.packpal.kids.ui.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.packpal.kids.ui.components.PackPalScaffold

private val sections = listOf(
    "Everything stays on this device" to
        "Packing lists, packed items, unfinished checks and check history are stored only in this app's private storage on this device. They are never transmitted.",
    "No internet" to
        "PackPal Kids does not use the internet. It has no accounts, ads, analytics, payments, cloud sync or remote content. All pictures are built into the app.",
    "No permissions" to
        "The app does not ask for the camera, microphone, location, contacts, files or notifications.",
    "No backup or transfer" to
        "Android cloud backup and device-to-device transfer are turned off for this app, so its data is not copied off the device.",
    "Deleting data" to
        "Use \"Clear all local data\" in the parent area to delete everything, or uninstall the app. Single history entries can be deleted in \"Manage history\".",
    "Parent area" to
        "The press-and-hold Parents button only prevents accidental taps by children. It is not a password or a security lock.",
)

@Composable
fun PrivacyRoute(onBack: () -> Unit) {
    PackPalScaffold(title = "Privacy", onBack = onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 640.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                sections.forEach { (title, body) ->
                    SectionTitle(title)
                    Text(body, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
