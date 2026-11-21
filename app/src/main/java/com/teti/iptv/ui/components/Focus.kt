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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.teti.iptv.ui.theme.TeTiWhite

/** Dokunma ve D-pad için tıklanabilir alan: odaklanınca beyaz çerçeve çizer. */
fun Modifier.tetiClickable(
    shape: Shape = RoundedCornerShape(12.dp),
    minSize: Dp = 48.dp,
    onClick: () -> Unit
): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    this
        .defaultMinSize(minWidth = minSize, minHeight = minSize)
        .onFocusChanged { focused = it.isFocused }
        .border(
            width = if (focused) 2.dp else 0.dp,
            color = if (focused) TeTiWhite else Color.Transparent,
            shape = shape
        )
        .clip(shape)
        .clickable(onClick = onClick)
}
