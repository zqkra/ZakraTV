@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.zakratv.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
 * Episode tile: still image, "E#" chip and the episode TITLE (up to 2 lines).
 * Makes seasons readable like a real streaming app instead of bare E1/E2 buttons.
 */
@Composable
fun EpisodeCard(
    number: Int,
    title: String,
    stillUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.width(216.dp),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = ZakraSurface,
            focusedContainerColor = ZakraSurface2,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, ZakraFocus),
                shape = RoundedCornerShape(12.dp),
            )
        ),
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(ZakraSurface2),
            ) {
                if (stillUrl != null) {
                    AsyncImage(
                        model = stillUrl,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text("E$number", style = ZakraType.caption, color = ZakraText)
                }
            }
            Text(
                text = title.ifBlank { "Episodio $number" },
                style = ZakraType.caption,
                color = ZakraText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .heightIn(min = 40.dp),
            )
        }
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

/** Clean circular loader: a faint full ring plus a rotating accent arc. */
@Composable
fun ZakraSpinner(modifier: Modifier = Modifier, size: Int = 48) {
    val transition = rememberInfiniteTransition(label = "spin")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "angle",
    )
    val strokeDp = (size / 9f).coerceIn(3f, 7f)
    Canvas(
        modifier = modifier
            .size(size.dp)
            .rotate(angle),
    ) {
        val stroke = strokeDp.dp.toPx()
        val d = this.size.minDimension - stroke
        val topLeft = Offset(
            (this.size.width - d) / 2f,
            (this.size.height - d) / 2f,
        )
        val arcSize = Size(d, d)
        drawArc(
            color = ZakraAccent.copy(alpha = 0.18f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawArc(
            color = ZakraAccent,
            startAngle = 0f,
            sweepAngle = 250f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
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

/**
 * The app logo mark — same design as the launcher icon (red gradient plate + bold white Z),
 * so the splash and the side menu match the real brand instead of a flat YT-like square.
 */
@Composable
fun ZakraLogoMark(size: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size * 0.22f).dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFF11722), Color(0xFFE50914), Color(0xFF7A0510)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Z",
            style = ZakraType.title.copy(
                fontSize = (size * 0.52f).sp,
                fontWeight = FontWeight.Black,
            ),
            color = Color.White,
        )
    }
}

/** Splash: the real app logo with a spinning ring around it + wordmark. */
@Composable
fun SplashLoading(message: String = "Preparando Zakra TV…") {
    Box(
        Modifier
            .fillMaxSize()
            .background(ZakraBg),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                ZakraSpinner(size = 136)
                ZakraLogoMark(size = 92)
            }
            Spacer(Modifier.height(28.dp))
            Text(
                "ZAKRA TV",
                style = ZakraType.title.copy(letterSpacing = 6.sp),
                color = ZakraText,
            )
            Spacer(Modifier.height(10.dp))
            Text(message, style = ZakraType.bodyMuted, color = ZakraMuted, textAlign = TextAlign.Center)
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
    icon: NavIcon,
    modifier: Modifier = Modifier,
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
            Box(
                Modifier.size(width = 4.dp, height = 22.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Box(
                        Modifier
                            .size(width = 4.dp, height = 22.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(ZakraAccent),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Image(
                painter = painterResource(navIconRes(icon)),
                contentDescription = label,
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(if (selected) ZakraText else ZakraMuted),
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = label,
                style = ZakraType.body,
                color = if (selected) ZakraText else ZakraMuted,
                maxLines = 1,
            )
        }
    }
}

private fun navIconRes(icon: NavIcon): Int = when (icon) {
    NavIcon.Home -> R.drawable.ic_home
    NavIcon.Search -> R.drawable.ic_search
    NavIcon.Movies -> R.drawable.ic_movie
    NavIcon.Series -> R.drawable.ic_series
    NavIcon.Trending -> R.drawable.ic_trending
    NavIcon.MyList -> R.drawable.ic_list
    NavIcon.Settings -> R.drawable.ic_settings
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
