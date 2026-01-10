package com.example.signalwearos.presentation.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.example.signalwearos.presentation.model.Contact

@Composable
fun ContactListScreen(
    contacts: List<Contact>,
    onContactClick: (Contact) -> Unit
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        anchorType = ScalingLazyListAnchorType.ItemStart
    ) {
        item {
            Text(
                text = "Signal",
                style = MaterialTheme.typography.title1,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(contacts) { contact ->
            ContactChip(contact = contact, onClick = { onContactClick(contact) })
        }
    }
}

@Composable
fun ContactChip(
    contact: Contact,
    onClick: () -> Unit
) {
    Chip(
        onClick = onClick,
        label = { Text(text = contact.name) },
        secondaryLabel = { Text(text = contact.lastMessage) },
        colors = ChipDefaults.secondaryChipColors(),
        modifier = Modifier.fillMaxWidth()
    )
}
