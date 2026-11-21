package com.teti.iptv.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.work.WorkInfo
import com.teti.iptv.data.local.entity.PlaylistProfileEntity
import com.teti.iptv.data.local.entity.PlaylistType
import com.teti.iptv.domain.epg.EpgOffset
import com.teti.iptv.domain.epg.EpgUpdateInterval
import com.teti.iptv.player.BufferProfile
import com.teti.iptv.ui.components.GlassSurface
import com.teti.iptv.ui.components.PillChip
import com.teti.iptv.ui.components.PrimaryActionButton
import com.teti.iptv.ui.components.ScreenHeading
import com.teti.iptv.ui.components.ThinProgress
import com.teti.iptv.ui.components.tetiClickable
import com.teti.iptv.ui.theme.TeTiBlack
import com.teti.iptv.ui.theme.TeTiBorder
import com.teti.iptv.ui.theme.TeTiFaint
import com.teti.iptv.ui.theme.TeTiGlass
import com.teti.iptv.ui.theme.TeTiMuted
import com.teti.iptv.ui.theme.TeTiOnWhite
import com.teti.iptv.ui.theme.TeTiRed
import com.teti.iptv.ui.theme.TeTiText
import com.teti.iptv.ui.theme.TeTiWhite
import com.teti.iptv.ui.viewmodel.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    val epgAuto by viewModel.epgAutoXtream.collectAsState()
    val epgCustomUrl by viewModel.epgCustomUrl.collectAsState()
    val epgInterval by viewModel.epgInterval.collectAsState()
    val epgOffset by viewModel.epgOffsetHours.collectAsState()
    val epgLastUpdated by viewModel.epgLastUpdated.collectAsState()
    val epgStatus by viewModel.epgStatus.collectAsState()
    val isEpgSyncing by viewModel.isEpgSyncing.collectAsState()
    val tmdbProgress by viewModel.tmdbProgress.collectAsState()
    val tmdbRunning by viewModel.tmdbRunning.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        ScreenHeading(title = "Ayarlar", subtitle = "Profil, rehber, oynatıcı ve puanlar")

        // --- OYNATMA LİSTELERİ ---
        SettingsCard(icon = Icons.Default.VideoLibrary, title = "Oynatma listeleri") {
            if (profiles.isEmpty()) {
                Text("Henüz profil yok.", color = TeTiMuted, style = MaterialTheme.typography.bodyMedium)
            }
            profiles.forEach { profile ->
                val isActive = profile.id == activeProfile?.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .tetiClickable(shape = RoundedCornerShape(12.dp)) { viewModel.selectProfile(profile.id) }
                        .background(if (isActive) TeTiWhite.copy(alpha = 0.06f) else TeTiBlack.copy(alpha = 0f), RoundedCornerShape(12.dp))
                        .padding(start = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                        Text(text = profile.name, color = TeTiText, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "${profile.type.label()} • ${profile.serverUrl}",
                            color = TeTiFaint,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (isActive) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Aktif", tint = TeTiWhite, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { viewModel.deleteProfile(profile) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Sil", tint = TeTiFaint)
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryActionButton(
                    text = "Profil Ekle",
                    icon = Icons.Default.Add,
                    onClick = { showAddDialog = true },
                    height = 44.dp
                )
                if (activeProfile != null) {
                    SecondaryButton(
                        text = "Senkronize Et",
                        icon = Icons.Default.Refresh,
                        busy = isSyncing,
                        onClick = viewModel::syncCurrentProfile
                    )
                }
            }
            syncStatus?.let { StatusText(it) }
        }

        // --- EPG (TV REHBERİ) ---
        SettingsCard(icon = Icons.Default.LiveTv, title = "EPG (TV Rehberi)") {
            SwitchRow(
                title = "Xtream'den otomatik çek",
                subtitle = "Profil sunucusunun xmltv.php adresi kullanılır",
                checked = epgAuto,
                onCheckedChange = viewModel::setEpgAutoXtream
            )
            Spacer(modifier = Modifier.height(10.dp))
            var urlInput by remember(epgCustomUrl) { mutableStateOf(epgCustomUrl) }
            OutlinedTextField(
                value = urlInput,
                onValueChange = {
                    urlInput = it
                    viewModel.setEpgCustomUrl(it)
                },
                label = { Text("Özel XMLTV URL (isteğe bağlı)") },
                placeholder = { Text("https://…/epg.xml.gz") },
                supportingText = { Text("Doluysa otomatik kaynağın yerine kullanılır") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                colors = tetiFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            FieldLabel("Otomatik güncelleme sıklığı")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                EpgUpdateInterval.entries.forEach { option ->
                    PillChip(text = option.label, selected = option == epgInterval, onClick = { viewModel.setEpgInterval(option) })
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            FieldLabel("Zaman kaydırma (EPG Offset)")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                EpgOffset.OPTIONS.forEach { hours ->
                    PillChip(text = EpgOffset.label(hours), selected = hours == epgOffset, onClick = { viewModel.setEpgOffsetHours(hours) })
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PrimaryActionButton(
                    text = if (isEpgSyncing) "Güncelleniyor…" else "EPG'yi Şimdi Güncelle",
                    icon = Icons.Default.CloudSync,
                    onClick = viewModel::syncEpgNow,
                    enabled = !isEpgSyncing && activeProfile != null,
                    height = 44.dp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Son güncelleme: " + if (epgLastUpdated > 0L) formatDateTime(epgLastUpdated) else "henüz yok",
                color = TeTiMuted,
                style = MaterialTheme.typography.bodySmall
            )
            epgStatus?.let { StatusText(it) }
        }

        // --- OYNATICI ---
        SettingsCard(icon = Icons.Default.Tune, title = "Oynatıcı") {
            FieldLabel("Tampon (buffer) modu")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BufferProfile.entries.forEach { p ->
                    PillChip(text = p.title, selected = p == bufferProfile, onClick = { viewModel.setBufferProfile(p) })
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            SwitchRow(
                title = "Yazılımsal çözücü",
                subtitle = "Donanım desteği olmayan ses ve videolar için",
                checked = useSoftwareDecoder,
                onCheckedChange = viewModel::setSoftwareDecoder
            )
        }

        // --- PUAN SERVİSLERİ ---
        SettingsCard(icon = Icons.Default.Star, title = "Puan servisleri") {
            Text(
                "Puan, özet, afiş ve oyuncular TMDB'den (tr-TR) alınır. Sağlayıcının gönderdiği puanlar kullanılmaz.",
                color = TeTiMuted,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(10.dp))
            var tmdbInput by remember(tmdbApiKey) { mutableStateOf(tmdbApiKey) }
            OutlinedTextField(
                value = tmdbInput,
                onValueChange = {
                    tmdbInput = it
                    viewModel.setTmdbApiKey(it)
                },
                label = { Text("TMDB API anahtarı veya okuma jetonu") },
                placeholder = { Text("Boş: yerleşik anahtar kullanılır") },
                supportingText = { Text(if (tmdbInput.isBlank()) "Birincil kaynak • yerleşik anahtar etkin" else "Birincil kaynak • kendi anahtarınız") },
                singleLine = true,
                colors = tetiFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            var omdbInput by remember(omdbApiKey) { mutableStateOf(omdbApiKey) }
            OutlinedTextField(
                value = omdbInput,
                onValueChange = {
                    omdbInput = it
                    viewModel.setOmdbApiKey(it)
                },
                label = { Text("OMDb API anahtarı (isteğe bağlı yedek)") },
                supportingText = { Text("Yalnızca TMDB eşleşme bulamazsa denenir") },
                singleLine = true,
                colors = tetiFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            PrimaryActionButton(
                text = if (tmdbRunning) "TMDB verileri çekiliyor…" else "TMDB Verilerini Şimdi Uygula / Önbelleği Yenile",
                icon = Icons.Default.Refresh,
                onClick = viewModel::refreshTmdbCache,
                enabled = !tmdbRunning && activeProfile != null,
                height = 48.dp,
                modifier = Modifier.fillMaxWidth()
            )
            tmdbProgress?.let { progress ->
                Spacer(modifier = Modifier.height(10.dp))
                when {
                    progress.error != null -> StatusText(progress.error, error = true)
                    progress.state == WorkInfo.State.RUNNING && progress.total > 0 -> {
                        ThinProgress(fraction = progress.done.toFloat() / progress.total)
                        StatusText("${progress.done} / ${progress.total} içerik işlendi")
                    }
                    progress.state == WorkInfo.State.RUNNING || progress.state == WorkInfo.State.ENQUEUED ->
                        StatusText("Sırada, ağ bağlantısı bekleniyor…")
                    progress.state == WorkInfo.State.SUCCEEDED ->
                        StatusText("TMDB verileri güncel.")
                    progress.state == WorkInfo.State.FAILED -> StatusText("TMDB güncellemesi tamamlanamadı.", error = true)
                    else -> Unit
                }
            }
        }
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
private fun SettingsCard(icon: ImageVector, title: String, content: @Composable ColumnScope.() -> Unit) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .widthIn(max = 840.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = TeTiText, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(title, color = TeTiText, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, color = TeTiMuted, style = MaterialTheme.typography.labelLarge)
    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
private fun StatusText(text: String, error: Boolean = false) {
    Spacer(modifier = Modifier.height(6.dp))
    Text(text = text, color = if (error) TeTiRed else TeTiMuted, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tetiClickable(shape = RoundedCornerShape(12.dp)) { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TeTiText, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, color = TeTiFaint, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TeTiOnWhite,
                checkedTrackColor = TeTiWhite,
                uncheckedThumbColor = TeTiMuted,
                uncheckedTrackColor = TeTiGlass,
                uncheckedBorderColor = TeTiBorder
            )
        )
    }
}

@Composable
private fun SecondaryButton(text: String, icon: ImageVector, busy: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = !busy,
        modifier = Modifier.height(44.dp),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TeTiBorder),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TeTiText)
    ) {
        if (busy) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TeTiText, strokeWidth = 2.dp)
        } else {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(text)
    }
}

@Composable
private fun tetiFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = TeTiWhite,
    unfocusedBorderColor = TeTiBorder,
    focusedLabelColor = TeTiWhite,
    unfocusedLabelColor = TeTiMuted,
    cursorColor = TeTiWhite,
    focusedTextColor = TeTiText,
    unfocusedTextColor = TeTiText,
    focusedSupportingTextColor = TeTiFaint,
    unfocusedSupportingTextColor = TeTiFaint
)

private fun PlaylistType.label(): String = when (this) {
    PlaylistType.XTREAM -> "Xtream"
    PlaylistType.M3U_URL -> "M3U"
    PlaylistType.M3U_LOCAL -> "Yerel M3U"
}

private fun formatDateTime(timeMs: Long): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", Locale.forLanguageTag("tr-TR")).format(Date(timeMs))

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
        containerColor = TeTiGlass,
        titleContentColor = TeTiText,
        textContentColor = TeTiText,
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
                            containerColor = if (type == PlaylistType.XTREAM) TeTiWhite else TeTiGlass,
                            contentColor = if (type == PlaylistType.XTREAM) TeTiOnWhite else TeTiText
                        )
                    ) { Text("Xtream", fontSize = 12.sp) }

                    Button(
                        onClick = { type = PlaylistType.M3U_URL },
                        modifier = Modifier.height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == PlaylistType.M3U_URL) TeTiWhite else TeTiGlass,
                            contentColor = if (type == PlaylistType.M3U_URL) TeTiOnWhite else TeTiText
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
                OutlinedTextField(value = epgUrl, onValueChange = { epgUrl = it }, label = { Text("EPG URL (isteğe bağlı)") }, singleLine = true)
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
                colors = ButtonDefaults.buttonColors(containerColor = TeTiWhite, contentColor = TeTiOnWhite)
            ) { Text("Kaydet") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal", color = TeTiMuted) }
        }
    )
}
