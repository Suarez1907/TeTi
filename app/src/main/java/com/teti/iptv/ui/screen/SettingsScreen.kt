package com.teti.iptv.ui.screen

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.teti.iptv.data.local.entity.PlaylistProfileEntity
import com.teti.iptv.data.local.entity.PlaylistType
import com.teti.iptv.player.BufferProfile
import com.teti.iptv.ui.components.ScreenHeading
import com.teti.iptv.ui.components.tetiClickable
import com.teti.iptv.ui.theme.DarkRed
import com.teti.iptv.ui.theme.DarkSurface
import com.teti.iptv.ui.theme.TeTiAmberDeep
import com.teti.iptv.ui.theme.TeTiMuted
import com.teti.iptv.ui.theme.TeTiText
import com.teti.iptv.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val profiles by viewModel.profiles.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    val bufferProfile by viewModel.bufferProfile.collectAsState()
    val useSoftwareDecoder by viewModel.useSoftwareDecoder.collectAsState()
    val omdbApiKey by viewModel.omdbApiKey.collectAsState()
    val tmdbApiKey by viewModel.tmdbApiKey.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        ScreenHeading(title = "Ayarlar", subtitle = "Profil, oynatıcı ve anahtarlar")
        Text(
            "Oynatma listeleri",
            color = TeTiText,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))

        profiles.forEach { profile ->
            val isActive = profile.id == activeProfile?.id
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .tetiClickable(shape = RoundedCornerShape(14.dp)) { viewModel.selectProfile(profile.id) },
                color = DarkSurface,
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile.name,
                            color = if (isActive) MaterialTheme.colorScheme.primary else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "${profile.type} • ${profile.serverUrl}",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }

                    if (isActive) {
                        Icon(Icons.Default.Check, contentDescription = "Aktif", tint = DarkRed, modifier = Modifier.padding(end = 8.dp))
                    }

                    IconButton(onClick = { viewModel.deleteProfile(profile) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Color.Gray)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Profil Ekle")
            }

            if (activeProfile != null) {
                OutlinedButton(
                    onClick = { viewModel.syncCurrentProfile() },
                    enabled = !isSyncing,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.height(16.dp).width(16.dp), color = Color.White)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Senkronize Et")
                }
            }
        }

        syncStatus?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Oynatıcı",
            color = TeTiText,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            "Tampon (buffer) modu",
            color = TeTiMuted,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            BufferProfile.entries.forEach { p ->
                val isSelected = p == bufferProfile
                Button(
                    onClick = { viewModel.setBufferProfile(p) },
                    modifier = Modifier.height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) DarkRed else DarkSurface,
                        contentColor = if (isSelected) TeTiAmberDeep else TeTiText
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(p.title, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Yazılımsal çözücü", color = TeTiText, style = MaterialTheme.typography.bodyLarge)
                Text("Donanım desteği olmayan ses ve videolar için", color = TeTiMuted, style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = useSoftwareDecoder,
                onCheckedChange = { viewModel.setSoftwareDecoder(it) },
                colors = SwitchDefaults.colors(checkedThumbColor = DarkRed)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Puan servisleri",
            color = TeTiText,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))

        var omdbInput by remember(omdbApiKey) { mutableStateOf(omdbApiKey) }
        OutlinedTextField(
            value = omdbInput,
            onValueChange = {
                omdbInput = it
                viewModel.setOmdbApiKey(it)
            },
            label = { Text("OMDb API Key") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        var tmdbInput by remember(tmdbApiKey) { mutableStateOf(tmdbApiKey) }
        OutlinedTextField(
            value = tmdbInput,
            onValueChange = {
                tmdbInput = it
                viewModel.setTmdbApiKey(it)
            },
            label = { Text("TMDB API Key (v3 auth)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showAddDialog) {
        AddProfileDialog(
            onDismiss = { showAddDialog = false },
            onSave = { profile ->
                viewModel.addOrUpdateProfile(profile) {
                    showAddDialog = false
                    viewModel.syncCurrentProfile()
                }
            }
        )
    }
}

@Composable
fun AddProfileDialog(
    onDismiss: () -> Unit,
    onSave: (PlaylistProfileEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(PlaylistType.XTREAM) }
    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var epgUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni Profil Ekle") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Profil Adı") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { type = PlaylistType.XTREAM },
                        modifier = Modifier.height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == PlaylistType.XTREAM) DarkRed else DarkSurface,
                            contentColor = if (type == PlaylistType.XTREAM) TeTiAmberDeep else Color.White
                        )
                    ) { Text("Xtream", fontSize = 12.sp) }

                    Button(
                        onClick = { type = PlaylistType.M3U_URL },
                        modifier = Modifier.height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == PlaylistType.M3U_URL) DarkRed else DarkSurface,
                            contentColor = if (type == PlaylistType.M3U_URL) TeTiAmberDeep else Color.White
                        )
                    ) { Text("M3U URL", fontSize = 12.sp) }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text(if (type == PlaylistType.XTREAM) "Sunucu URL (http://...:port)" else "M3U Linki") },
                    singleLine = true
                )

                if (type == PlaylistType.XTREAM) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Kullanıcı Adı") }, singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Şifre") }, singleLine = true)
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = epgUrl, onValueChange = { epgUrl = it }, label = { Text("EPG URL (İsteğe Bağlı)") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && serverUrl.isNotBlank()) {
                        onSave(
                            PlaylistProfileEntity(
                                name = name.trim(),
                                type = type,
                                serverUrl = serverUrl.trim(),
                                username = username.trim().ifBlank { null },
                                password = password.trim().ifBlank { null },
                                epgUrl = epgUrl.trim().ifBlank { null }
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkRed, contentColor = TeTiAmberDeep)
            ) { Text("Kaydet") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        }
    )
}
