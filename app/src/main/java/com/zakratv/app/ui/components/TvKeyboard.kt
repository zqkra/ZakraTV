@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.zakratv.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.zakratv.app.ui.theme.ZakraAccent
import com.zakratv.app.ui.theme.ZakraFocus
import com.zakratv.app.ui.theme.ZakraSurface2
import com.zakratv.app.ui.theme.ZakraText
import com.zakratv.app.ui.theme.ZakraType

/**
 * On-screen keyboard for Android TV / Fire Stick (DPAD). No system keyboard required.
 */
@Composable
fun TvOnScreenKeyboard(
    onChar: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onSpace: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = listOf(
        "1234567890",
        "QWERTYUIOP",
        "ASDFGHJKL",
        "ZXCVBNM",
    )
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                row.forEach { ch ->
                    KeyButton(
                        label = ch.toString(),
                        onClick = { onChar(ch) },
                        width = 48,
                    )
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KeyButton(label = "ESPACIO", onClick = onSpace, width = 160)
            KeyButton(label = "BORRAR", onClick = onBackspace, width = 110)
            KeyButton(label = "LIMPIAR", onClick = onClear, width = 110)
            KeyButton(label = "BUSCAR", onClick = onSearch, width = 130, accent = true)
        }
    }
}

@Composable
private fun KeyButton(
    label: String,
    onClick: () -> Unit,
    width: Int,
    accent: Boolean = false,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(width.dp)
            .height(44.dp),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (accent) ZakraAccent else ZakraSurface2,
            focusedContainerColor = if (accent) ZakraAccent.copy(alpha = 0.9f) else ZakraFocus.copy(alpha = 0.18f),
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, ZakraFocus),
                shape = RoundedCornerShape(8.dp),
            )
        ),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(width.dp)
                .height(44.dp)
                .padding(horizontal = 4.dp),
        ) {
            Text(
                text = label,
                style = ZakraType.caption,
                color = ZakraText,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}
