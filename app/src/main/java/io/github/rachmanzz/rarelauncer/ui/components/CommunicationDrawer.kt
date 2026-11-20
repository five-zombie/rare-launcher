package io.github.rachmanzz.rarelauncer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.rachmanzz.rarelauncer.data.model.ContactItem
import io.github.rachmanzz.rarelauncer.ui.theme.AetherGlassBorder
import io.github.rachmanzz.rarelauncer.ui.theme.AetherGlassBorderSubtle
import io.github.rachmanzz.rarelauncer.ui.theme.AetherOnPrimary
import io.github.rachmanzz.rarelauncer.ui.theme.AetherOnSurface
import io.github.rachmanzz.rarelauncer.ui.theme.AetherOnSurfaceVariant
import io.github.rachmanzz.rarelauncer.ui.theme.AetherPrimary
import io.github.rachmanzz.rarelauncer.ui.theme.AetherSecondary
import io.github.rachmanzz.rarelauncer.ui.theme.AetherSurfaceBright
import io.github.rachmanzz.rarelauncer.ui.theme.AetherSurfaceContainerHigh
import io.github.rachmanzz.rarelauncer.ui.theme.RareLauncerTheme

@Composable
fun CommunicationDrawer(
    isOpen: Boolean,
    contacts: List<ContactItem>,
    onClose: () -> Unit,
    onCallClicked: (String?) -> Unit,
    onMessageClicked: (String?) -> Unit,
    onContactsClicked: () -> Unit,
    modifier: Modifier = Modifier,
    hasContactsPermission: Boolean = true,
    onRequestContactsPermission: () -> Unit = {}
) {
    var contactFilter by remember { mutableStateOf("") }
    var pendingAction by remember { mutableStateOf<Pair<ContactItem, CommunicationActionType>?>(null) }
    val filteredContacts = remember(contacts, contactFilter) {
        if (contactFilter.isBlank()) {
            contacts.take(15)
        } else {
            val q = contactFilter.trim().lowercase()
            contacts.filter {
                it.name.lowercase().contains(q) || it.phoneNumber.contains(q)
            }
        }
    }

    if (isOpen) {
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            // Scrim to tap and close
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000))
                    .clickable { onClose() }
            )

            // Right Slide-in Drawer Container
            AnimatedVisibility(
                visible = isOpen,
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it }),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(320.dp)
                        .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
                        .background(Color(0xF50E121A))
                        .border(1.dp, AetherGlassBorder, RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                    ) {
                        // Header (Solid black background covering status bar all the way to the top)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF000000))
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = AetherPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "COMMUNICATION",
                                    color = AetherOnSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(AetherSurfaceBright, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Close",
                                    tint = AetherPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Scrollable Content
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Section: Quick Access Big Cards
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "QUICK ACTIONS",
                                        color = AetherOnSurfaceVariant,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Quick Phone Call
                                        QuickActionButton(
                                            title = "Call",
                                            icon = Icons.Default.Call,
                                            color = AetherPrimary,
                                            onClick = { onCallClicked(null) },
                                            modifier = Modifier.weight(1f)
                                        )
                                        // Quick Message
                                        QuickActionButton(
                                            title = "Message",
                                            icon = Icons.AutoMirrored.Filled.Message,
                                            color = AetherSecondary,
                                            onClick = { onMessageClicked(null) },
                                            modifier = Modifier.weight(1f)
                                        )
                                        // Quick Contacts
                                        QuickActionButton(
                                            title = "Contacts",
                                            icon = Icons.Default.Person,
                                            color = Color(0xFFFFB2BA),
                                            onClick = onContactsClicked,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            // Section: Contact Search
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "LOOKUP CONTACT",
                                        color = AetherOnSurfaceVariant,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(AetherSurfaceContainerHigh)
                                            .border(1.dp, AetherGlassBorderSubtle, RoundedCornerShape(12.dp))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PersonSearch,
                                            contentDescription = null,
                                            tint = AetherOnSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        BasicTextField(
                                            value = contactFilter,
                                            onValueChange = { contactFilter = it },
                                            singleLine = true,
                                            textStyle = TextStyle(
                                                color = AetherOnSurface,
                                                fontSize = 12.sp
                                            ),
                                            cursorBrush = SolidColor(AetherPrimary),
                                            modifier = Modifier.fillMaxWidth(),
                                            decorationBox = { innerTextField ->
                                                if (contactFilter.isEmpty()) {
                                                    Text(
                                                        text = "Search by name or number...",
                                                        color = AetherOnSurfaceVariant.copy(alpha = 0.5f),
                                                        fontSize = 12.sp
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        )
                                    }
                                }
                            }

                            // Section: Speed Dial & Recents List
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "SPEED DIAL & DIRECT",
                                        color = AetherOnSurfaceVariant,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = null,
                                        tint = AetherPrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }

                            if (!hasContactsPermission) {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(AetherSurfaceContainerHigh.copy(alpha = 0.7f))
                                            .border(1.dp, AetherGlassBorderSubtle, RoundedCornerShape(14.dp))
                                            .padding(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Izin akses kontak belum diberikan",
                                            color = AetherOnSurfaceVariant,
                                            fontSize = 12.sp
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(AetherPrimary)
                                                .clickable { onRequestContactsPermission() }
                                                .padding(horizontal = 14.dp, vertical = 7.dp)
                                        ) {
                                            Text(
                                                text = "Izinkan Kontak",
                                                color = AetherOnPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            } else if (filteredContacts.isEmpty()) {
                                item {
                                    Text(
                                        text = if (contactFilter.isBlank()) {
                                            "Tidak ada kontak di perangkat"
                                        } else {
                                            "Kontak tidak ditemukan"
                                        },
                                        color = AetherOnSurfaceVariant,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            } else {
                                items(filteredContacts, key = { it.id }) { contact ->
                                    ContactRowItem(
                                        contact = contact,
                                        onCall = { pendingAction = contact to CommunicationActionType.CALL },
                                        onMessage = { pendingAction = contact to CommunicationActionType.MESSAGE }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Slide-to-Confirm Overlay for Call / Message action
            pendingAction?.let { (contact, actionType) ->
                SlideToCallOverlay(
                    contact = contact,
                    actionType = actionType,
                    onConfirm = {
                        val number = contact.phoneNumber
                        pendingAction = null
                        if (actionType == CommunicationActionType.CALL) {
                            onCallClicked(number)
                        } else {
                            onMessageClicked(number)
                        }
                    },
                    onDismiss = { pendingAction = null }
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = color,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            color = AetherOnSurface,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ContactRowItem(
    contact: ContactItem,
    onCall: () -> Unit,
    onMessage: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AetherSurfaceContainerHigh.copy(alpha = 0.7f))
            .clickable { onCall() }
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AetherPrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.initials,
                    color = AetherPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    color = AetherOnSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (contact.phoneNumber.isNotEmpty()) contact.phoneNumber else contact.subtitle,
                    color = AetherOnSurfaceVariant,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Message button
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(AetherSurfaceBright)
                    .clickable { onMessage() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Message,
                    contentDescription = "Message",
                    tint = AetherOnSurface,
                    modifier = Modifier.size(14.dp)
                )
            }
            // Call button
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(AetherPrimary)
                    .clickable { onCall() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call",
                    tint = AetherOnPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun CommunicationDrawerPreview() {
    RareLauncerTheme {
        CommunicationDrawer(
            isOpen = true,
            contacts = listOf(
                ContactItem("1", "Alexandre Laurent", "+123456789", "AL", "Mobile • 12m ago"),
                ContactItem("2", "Sarah Miller", "+987654321", "SM", "Missed • 1h ago")
            ),
            onClose = {},
            onCallClicked = {},
            onMessageClicked = {},
            onContactsClicked = {}
        )
    }
}
