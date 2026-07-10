@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.zakratv.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * Our own on-screen keyboard for Android TV / Fire Stick — the D-PAD-optimized
 * alphabetical grid used by real TV apps (Netflix/Prime style). No system IME at all,
 * so it can never fail to open. Keys fill the column width evenly (weight-based).
 */
private val KEY_ROWS = listOf(
    "ABCDEFG",
    "HIJKLMN",
    "ÑOPQRST",
    "UVWXYZ0",
    "1234567",
    "89.:-'&",
)

@Composable
fun TvKeyboard(
    onChar: (Char) -> Unit,
    onSpace: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    firstKeyFocus: FocusRequester? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        KEY_ROWS.forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                row.forEachIndexed { colIndex, ch ->
                    val base = Modifier.weight(1f)
                    KeyButton(
                        label = ch.toString(),
                        onClick = { onChar(ch) },
                        modifier = if (rowIndex == 0 && colIndex == 0 && firstKeyFocus != null) {
                            base.focusRequester(firstKeyFocus)
                        } else {
                            base
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            KeyButton(label = "Espacio", onClick = onSpace, modifier = Modifier.weight(1.5f))
            KeyButton(label = "Borrar", onClick = onBackspace, modifier = Modifier.weight(1.2f))
            KeyButton(label = "Vaciar", onClick = onClear, modifier = Modifier.weight(1.2f))
            KeyButton(
                label = "Buscar",
                onClick = onSearch,
                modifier = Modifier.weight(1.3f),
                accent = true,
            )
        }
    }
}

@Composable
private fun KeyButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(42.dp),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (accent) ZakraAccent else ZakraSurface2,
            focusedContainerColor = if (accent) ZakraAccent.copy(alpha = 0.88f) else ZakraFocus.copy(alpha = 0.22f),
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
                .fillMaxWidth()
                .height(42.dp)
                .padding(horizontal = 2.dp),
        ) {
            Text(
                text = label,
                style = ZakraType.caption.copy(fontSize = 17.sp),
                color = ZakraText,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}
