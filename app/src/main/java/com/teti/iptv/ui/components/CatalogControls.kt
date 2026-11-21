package com.teti.iptv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teti.iptv.domain.catalog.CatalogSort
import com.teti.iptv.ui.theme.TeTiBorder
import com.teti.iptv.ui.theme.TeTiGlass
import com.teti.iptv.ui.theme.TeTiMuted
import com.teti.iptv.ui.theme.TeTiText
import com.teti.iptv.ui.theme.TeTiWhite
import kotlinx.coroutines.launch

/** Ekran başlığı: solda başlık/alt başlık, sağda kompakt ikon aksiyonları. */
@Composable
fun ScreenHeading(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TeTiText, style = MaterialTheme.typography.headlineMedium)
            Text(text = subtitle, color = TeTiMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = actions)
    }
}

/**
 * Başlığın sağındaki sıralama ve yıl ikonları. Her biri ModalBottomSheet açar;
 * aktif bir filtre varsa ikonun köşesinde kırmızı nokta görünür.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogHeaderActions(
    sort: CatalogSort,
    onSortChange: (CatalogSort) -> Unit,
    years: List<Int>,
    selectedYear: Int?,
    onYearChange: (Int?) -> Unit
) {
    var sheet by remember { mutableStateOf<CatalogSheet?>(null) }

    BadgedIcon(active = sort != CatalogSort.RECENTLY_ADDED) {
        GlassIconButton(
            icon = Icons.AutoMirrored.Filled.Sort,
            contentDescription = "Sırala: ${sort.label()}",
            onClick = { sheet = CatalogSheet.SORT },
            size = 44.dp
        )
    }
    BadgedIcon(active = selectedYear != null) {
        GlassIconButton(
            icon = Icons.Default.CalendarMonth,
            contentDescription = "Yıl: ${selectedYear ?: "Tüm yıllar"}",
            onClick = { sheet = CatalogSheet.YEAR },
            size = 44.dp
        )
    }

    when (sheet) {
        CatalogSheet.SORT -> OptionSheet(
            title = "Sırala",
            options = CatalogSort.entries.map { it.label() to it },
            selected = sort,
            onSelect = onSortChange,
            onDismiss = { sheet = null }
        )
        CatalogSheet.YEAR -> OptionSheet(
            title = "Yıl",
            options = listOf<Pair<String, Int?>>("Tüm yıllar" to null) + years.map { it.toString() to it },
            selected = selectedYear,
            onSelect = onYearChange,
            onDismiss = { sheet = null }
        )
        null -> Unit
    }
}

private enum class CatalogSheet { SORT, YEAR }

@Composable
private fun BadgedIcon(active: Boolean, content: @Composable () -> Unit) {
    Box {
        content()
        if (active) {
            LiveDot(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
                size = 7.dp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> OptionSheet(
    title: String,
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = options.size <= 8)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = TeTiGlass,
        contentColor = TeTiText,
        scrimColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = TeTiBorder) }
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = TeTiText,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.navigationBarsPadding()) {
            items(options, key = { it.first }) { (label, value) ->
                val isSelected = value == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .tetiClickable(shape = RoundedCornerShape(12.dp)) {
                            onSelect(value)
                            scope.launch { state.hide() }.invokeOnCompletion { onDismiss() }
                        }
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) TeTiWhite else TeTiMuted,
                        style = if (isSelected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = "Seçili", tint = TeTiWhite, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}
