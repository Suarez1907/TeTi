package com.teti.iptv.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.PasswordVisualTransformation
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

    val parentalEnabled by viewModel.parentalEnabled.collectAsState()
    val lockedCategories by viewModel.lockedCategories.collectAsState()
    val sessionUnlocked by viewModel.sessionUnlocked.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val subtitleScale by viewModel.subtitleScale.collectAsState()
    val subtitleBackground by viewModel.subtitleBackground.collectAsState()
    val backupStatus by viewModel.backupStatus.collectAsState()
    val cleanupStatus by viewModel.cleanupStatus.collectAsState()
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var pinDialog by remember { mutableStateOf<PinAction?>(null) }
    var showCategoryPicker by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(viewModel::exportBackup)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importBackup)
    }

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
            if (activeProfile != null) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton("Tekrar eden kanalları gizle", Icons.Default.CleaningServices, busy = false, onClick = viewModel::hideDuplicateChannels)
                    SecondaryButton("Gizlenenleri göster", Icons.Default.Visibility, busy = false, onClick = viewModel::unhideAllChannels)
                }
                cleanupStatus?.let { StatusText(it) }
            }
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
            Spacer(modifier = Modifier.height(12.dp))
            FieldLabel("Altyazı boyutu")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(1f to "Sistem", 1.25f to "Büyük", 1.5f to "Çok büyük").forEach { (scale, label) ->
                    PillChip(text = label, selected = subtitleScale == scale, onClick = { viewModel.setSubtitleScale(scale) })
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            SwitchRow(
                title = "Altyazı arka planı",
                subtitle = "Okunabilirlik için yazının arkasına koyu bant",
                checked = subtitleBackground,
                onCheckedChange = viewModel::setSubtitleBackground
            )
        }

        // --- EBEVEYN KİLİDİ ---
        SettingsCard(icon = Icons.Default.Lock, title = "Ebeveyn kilidi") {
            Text(
                when {
                    !parentalEnabled -> "PIN belirleyin ve kilitlenecek kategorileri seçin. Kilitli kategoriler tüm listelerden ve aramadan gizlenir."
                    sessionUnlocked -> "Kilit bu oturum için açık; kilitli kategoriler görünüyor."
                    else -> "${lockedCategories.size} kategori kilitli ve gizli."
                },
                color = TeTiMuted,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!parentalEnabled) {
                    PrimaryActionButton("PIN belirle", Icons.Default.Lock, onClick = { pinDialog = PinAction.SET }, height = 44.dp)
                } else {
                    PrimaryActionButton("Kategorileri seç", Icons.Default.Checklist, onClick = { pinDialog = PinAction.EDIT_CATEGORIES }, height = 44.dp)
                    if (sessionUnlocked) {
                        SecondaryButton("Yeniden kilitle", Icons.Default.Lock, busy = false, onClick = viewModel::lockSession)
                    } else {
                        SecondaryButton("Bu oturumda aç", Icons.Default.LockOpen, busy = false, onClick = { pinDialog = PinAction.UNLOCK })
                    }
                    SecondaryButton("PIN'i değiştir", Icons.Default.Password, busy = false, onClick = { pinDialog = PinAction.CHANGE })
                    SecondaryButton("PIN'i kaldır", Icons.Default.Delete, busy = false, onClick = { pinDialog = PinAction.REMOVE })
                }
            }
        }

        // --- YEDEKLEME ---
        SettingsCard(icon = Icons.Default.Backup, title = "Yedekle ve geri yükle") {
            Text(
                "Profiller, favoriler, izleme geçmişi ve ayarlar tek bir dosyaya yazılır. Google Drive gibi bir konuma kaydederek cihazlar arasında taşıyabilirsiniz.",
                color = TeTiMuted,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryActionButton("Yedekle", Icons.Default.Upload, onClick = {
                    exportLauncher.launch("teti-yedek-${SimpleDateFormat("yyyyMMdd", Locale.ROOT).format(Date())}.json")
                }, height = 44.dp)
                SecondaryButton("Geri yükle", Icons.Default.Download, busy = false, onClick = {
                    importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                })
            }
            backupStatus?.let { StatusText(it) }
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

        // --- HAKKINDA ---
        SettingsCard(icon = Icons.Default.Info, title = "Hakkında ve lisanslar") {
            val version = remember {
                runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "-"
            }
            Text("TeTi $version", color = TeTiText, style = MaterialTheme.typography.bodyLarge)
            Text(
                "TeTi içerik sağlamaz; yalnızca sizin eklediğiniz listeleri oynatır.",
                color = TeTiMuted,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "This product uses the TMDB API but is not endorsed or certified by TMDB.",
                color = TeTiText,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Video çözme: FFmpeg (LGPL-2.1-or-later), put.io media3-ffmpeg-decoder (Apache-2.0). " +
                    "Diğer bileşenler: AndroidX, Media3, Compose, Room, OkHttp, Retrofit, Coil, kotlinx.serialization (Apache-2.0).",
                color = TeTiMuted,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton("Gizlilik politikası", Icons.Default.Policy, busy = false, onClick = {
                    uriHandler.openUri("https://github.com/Suarez1907/TeTi/blob/main/docs/privacy-policy.md")
                })
                SecondaryButton("FFmpeg kaynak kodu", Icons.Default.Code, busy = false, onClick = {
                    uriHandler.openUri("https://github.com/putdotio/media3-ffmpeg-decoder")
                })
                SecondaryButton("TMDB", Icons.Default.Movie, busy = false, onClick = {
                    uriHandler.openUri("https://www.themoviedb.org")
                })
            }
        }
    }

    pinDialog?.let { action ->
        PinDialog(
            action = action,
            onDismiss = { pinDialog = null },
            onSubmit = { current, new ->
                when (action) {
                    PinAction.SET -> viewModel.setPin(new, null)
                    PinAction.CHANGE -> viewModel.setPin(new, current)
                    PinAction.UNLOCK -> if (viewModel.unlockSession(current)) null else "PIN yanlış"
                    PinAction.REMOVE -> if (viewModel.removePin(current)) null else "PIN yanlış"
                    PinAction.EDIT_CATEGORIES -> if (viewModel.unlockSession(current)) {
                        showCategoryPicker = true
                        null
                    } else "PIN yanlış"
                }
            }
        )
    }

    if (showCategoryPicker) {
        CategoryLockDialog(
            categories = allCategories,
            locked = lockedCategories,
            onDismiss = { showCategoryPicker = false },
            onSave = {
                viewModel.setLockedCategories(it)
                viewModel.lockSession()
                showCategoryPicker = false
            }
        )
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

private enum class PinAction(val title: String, val needsCurrent: Boolean, val needsNew: Boolean) {
    SET("PIN belirle", needsCurrent = false, needsNew = true),
    CHANGE("PIN'i değiştir", needsCurrent = true, needsNew = true),
    UNLOCK("Kilidi aç", needsCurrent = true, needsNew = false),
    REMOVE("PIN'i kaldır", needsCurrent = true, needsNew = false),
    EDIT_CATEGORIES("PIN'i girin", needsCurrent = true, needsNew = false)
}

@Composable
private fun PinDialog(action: PinAction, onDismiss: () -> Unit, onSubmit: (current: String, new: String) -> String?) {
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TeTiGlass,
        titleContentColor = TeTiText,
        textContentColor = TeTiText,
        title = { Text(action.title) },
        text = {
            Column {
                if (action.needsCurrent) PinField("Mevcut PIN", current) { current = it }
                if (action.needsNew) {
                    PinField("Yeni PIN (4-8 rakam)", new) { new = it }
                    PinField("Yeni PIN tekrar", repeat) { repeat = it }
                }
                error?.let { Text(it, color = TeTiRed, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = when {
                    action.needsNew && new != repeat -> "PIN'ler eşleşmiyor"
                    else -> onSubmit(current, new)
                }
                if (error == null) onDismiss()
            }) { Text("Tamam", color = TeTiWhite) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal", color = TeTiMuted) } }
    )
}

@Composable
private fun PinField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter(Char::isDigit).take(8)) },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        colors = tetiFieldColors(),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}

@Composable
private fun CategoryLockDialog(
    categories: List<String>,
    locked: Set<String>,
    onDismiss: () -> Unit,
    onSave: (Set<String>) -> Unit
) {
    var selection by remember { mutableStateOf(locked) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TeTiGlass,
        titleContentColor = TeTiText,
        textContentColor = TeTiText,
        title = { Text("Kilitlenecek kategoriler") },
        text = {
            if (categories.isEmpty()) {
                Text("Önce bir profil ekleyip senkronize edin.", color = TeTiMuted)
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(categories, key = { it }) { cat ->
                        val checked = cat in selection
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .tetiClickable(shape = RoundedCornerShape(8.dp)) {
                                    selection = if (checked) selection - cat else selection + cat
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { selection = if (it) selection + cat else selection - cat },
                                colors = CheckboxDefaults.colors(checkedColor = TeTiWhite, checkmarkColor = TeTiOnWhite)
                            )
                            Text(cat, color = TeTiText, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(selection) }) { Text("Kaydet", color = TeTiWhite) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal", color = TeTiMuted) } }
    )
}
