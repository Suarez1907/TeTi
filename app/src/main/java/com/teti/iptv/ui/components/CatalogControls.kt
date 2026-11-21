package com.teti.iptv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.teti.iptv.domain.catalog.CatalogSort
import com.teti.iptv.ui.theme.TeTiAmber
import com.teti.iptv.ui.theme.TeTiAmberDeep
import com.teti.iptv.ui.theme.TeTiMuted
import com.teti.iptv.ui.theme.TeTiSurfaceHigh
import com.teti.iptv.ui.theme.TeTiText

@Composable
fun CatalogControlBar(
    sort: CatalogSort,
    onSortChange: (CatalogSort) -> Unit,
    years: List<Int>,
    selectedYear: Int?,
    onYearChange: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxWidth(), color = TeTiSurfaceHigh.copy(alpha = 0.98f)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CatalogMenu(
                label = sort.label(),
                icon = Icons.AutoMirrored.Filled.Sort,
                modifier = Modifier.weight(1f)
            ) { close ->
                CatalogSort.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label()) },
                        onClick = {
                            onSortChange(option)
                            close()
                        }
                    )
                }
            }
            CatalogMenu(
                label = selectedYear?.toString() ?: "Tüm yıllar",
                icon = Icons.Default.DateRange,
                modifier = Modifier.weight(1f)
            ) { close ->
                DropdownMenuItem(
                    text = { Text("Tüm yıllar") },
                    onClick = {
                        onYearChange(null)
                        close()
                    }
                )
                years.forEach { year ->
                    DropdownMenuItem(
                        text = { Text(year.toString()) },
                        onClick = {
                            onYearChange(year)
                            close()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogMenu(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    menu: @Composable (close: () -> Unit) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = open,
        onExpandedChange = { open = it },
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            color = TeTiAmber,
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null, tint = TeTiAmberDeep)
                Text(
                    text = label,
                    color = TeTiAmberDeep,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        ExposedDropdownMenu(
            expanded = open,
            onDismissRequest = { open = false }
        ) {
            menu { open = false }
        }
    }
}

@Composable
fun ScreenHeading(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(text = "TeTi", color = TeTiAmber, style = MaterialTheme.typography.labelLarge)
        Text(text = title, color = TeTiText, style = MaterialTheme.typography.headlineSmall)
        Text(text = subtitle, color = TeTiMuted, style = MaterialTheme.typography.bodyMedium)
    }
}
