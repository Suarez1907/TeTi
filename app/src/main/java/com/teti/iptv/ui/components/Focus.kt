package com.teti.iptv.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.teti.iptv.ui.theme.TeTiSignal

fun Modifier.tetiClickable(
    shape: Shape = RoundedCornerShape(12.dp),
    onClick: () -> Unit
): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    this
        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
        .onFocusChanged { focused = it.isFocused }
        .border(
            width = if (focused) 2.dp else 0.dp,
            color = if (focused) TeTiSignal else Color.Transparent,
            shape = shape
        )
        .clip(shape)
        .clickable(onClick = onClick)
}
