@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.zakratv.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.Image
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.zakratv.app.R
import com.zakratv.app.data.model.MediaItem
import com.zakratv.app.ui.navigation.NavIcon
import com.zakratv.app.ui.theme.ZakraAccent
import com.zakratv.app.ui.theme.ZakraBg
import com.zakratv.app.ui.theme.ZakraFocus
import com.zakratv.app.ui.theme.ZakraMuted
import com.zakratv.app.ui.theme.ZakraSurface
import com.zakratv.app.ui.theme.ZakraSurface2
import com.zakratv.app.ui.theme.ZakraText
import com.zakratv.app.ui.theme.ZakraType

@Composable
fun PosterCard(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Int = 140,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.width(width.dp),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = ZakraSurface,
            focusedContainerColor = ZakraSurface2,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, ZakraFocus),
                shape = RoundedCornerShape(10.dp),
            )
        ),
    ) {
        Column(Modifier.wrapContentHeight()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .background(ZakraSurface2),
            ) {
                AsyncImage(
                    model = item.posterUrl("w342"),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Text(
                text = item.title,
                style = ZakraType.caption,
                color = ZakraText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
fun MediaRow(
    title: String,
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = ZakraType.section,
            color = ZakraText,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(items, key = { "${it.mediaType}-${it.id}" }) { item ->
                PosterCard(item = item, onClick = { onItemClick(item) })
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

/**
 * TV-safe button: FIXED height only. Never fillMaxSize() — that expands and covers the screen on TV.
 */
@Composable
fun BigButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(52.dp)
            .defaultMinSize(minWidth = 120.dp),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (accent) ZakraAccent else ZakraSurface2,
            focusedContainerColor = if (accent) ZakraAccent.copy(alpha = 0.92f) else Color(0xFF2A2A3A),
            contentColor = ZakraText,
            focusedContentColor = ZakraText,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, ZakraFocus),
                shape = RoundedCornerShape(12.dp),
            )
        ),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 20.dp),
        ) {
            Text(
                text = label,
                style = ZakraType.button,
                color = ZakraText,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Compact filter chip for catalog bars — never grows. */
@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(22.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) ZakraAccent else ZakraSurface2,
            focusedContainerColor = if (selected) ZakraAccent.copy(alpha = 0.9f) else Color(0xFF2A2A3A),
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, ZakraFocus),
                shape = RoundedCornerShape(22.dp),
            )
        ),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .height(44.dp)
                .padding(horizontal = 18.dp),
        ) {
            Text(
                text = label,
                style = ZakraType.caption,
                color = ZakraText,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun ZakraSpinner(modifier: Modifier = Modifier, size: Int = 48) {
    val transition = rememberInfiniteTransition(label = "spin")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "angle",
    )
    Box(
        modifier = modifier
            .size(size.dp)
            .rotate(angle)
            .clip(CircleShape)
            .background(
                Brush.sweepGradient(
                    listOf(ZakraAccent, ZakraAccent.copy(alpha = 0.15f), ZakraAccent),
                ),
            )
            .padding(5.dp)
            .clip(CircleShape)
            .background(ZakraBg),
    )
}

@Composable
fun LoadingBox(
    modifier: Modifier = Modifier,
    message: String = "Cargando…",
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ZakraBg),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ZakraSpinner(size = 52)
            Spacer(Modifier.height(16.dp))
            Text(message, style = ZakraType.body, color = ZakraMuted, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun SplashLoading(message: String = "Preparando Zakra TV…") {
    Box(
        Modifier
            .fillMaxSize()
            .background(ZakraBg),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(ZakraAccent),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Z",
                    style = ZakraType.title.copy(fontSize = 48.sp),
                    color = Color.White,
                )
            }
            Spacer(Modifier.height(24.dp))
            ZakraSpinner(size = 48)
            Spacer(Modifier.height(16.dp))
            Text(message, style = ZakraType.body, color = ZakraMuted, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun ErrorBox(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ZakraBg)
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(message, style = ZakraType.body, color = ZakraMuted, textAlign = TextAlign.Center)
    }
}

@Composable
fun HeroBackdrop(
    url: String?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(ZakraBg, ZakraBg.copy(alpha = 0.88f), Color.Transparent),
                    ),
                ),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, ZakraBg)),
                ),
        )
    }
}

@Composable
fun SideNavItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: NavIcon = NavIcon.None,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) ZakraAccent.copy(alpha = 0.28f) else Color.Transparent,
            focusedContainerColor = ZakraSurface2,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, ZakraFocus),
                shape = RoundedCornerShape(10.dp),
            )
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 14.dp),
        ) {
            if (selected) {
                Box(
                    Modifier
                        .size(width = 4.dp, height = 20.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(ZakraAccent),
                )
                Spacer(Modifier.width(10.dp))
            }
            when (icon) {
                NavIcon.Search -> {
                    Image(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = "Buscar",
                        modifier = Modifier.size(26.dp),
                        colorFilter = ColorFilter.tint(
                            if (selected) ZakraText else ZakraMuted,
                        ),
                    )
                    if (label.isNotBlank()) {
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = label,
                            style = ZakraType.body,
                            color = if (selected) ZakraText else ZakraMuted,
                            maxLines = 1,
                        )
                    }
                }
                NavIcon.None -> {
                    Text(
                        text = label,
                        style = ZakraType.body,
                        color = if (selected) ZakraText else ZakraMuted,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
fun StepLabel(step: Int, text: String) {
    Text(
        text = "$step. $text",
        style = ZakraType.section,
        color = ZakraText,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}
