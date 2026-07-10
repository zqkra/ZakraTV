package com.zakratv.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.zakratv.app.BuildConfig
import com.zakratv.app.ZakraApp
import com.zakratv.app.data.model.CatalogFilter
import com.zakratv.app.data.model.CatalogSection
import com.zakratv.app.data.model.Episode
import com.zakratv.app.data.model.MediaItem
import com.zakratv.app.data.model.MediaType
import com.zakratv.app.data.model.Season
import com.zakratv.app.data.model.StreamLink
import com.zakratv.app.data.ranking.StreamRanker
import com.zakratv.app.data.repository.CatalogRepository
import com.zakratv.app.data.update.AppUpdateChecker
import com.zakratv.app.data.update.AvailableUpdate
import com.zakratv.app.data.update.UpdateInstaller
import com.zakratv.app.player.PlayerActivity
import com.zakratv.app.ui.components.BigButton
import com.zakratv.app.ui.components.ErrorBox
import com.zakratv.app.ui.components.FilterChip
import com.zakratv.app.ui.components.HeroBackdrop
import com.zakratv.app.ui.components.LoadingBox
import com.zakratv.app.ui.components.MediaRow
import com.zakratv.app.ui.components.PosterCard
import com.zakratv.app.ui.components.SideNavItem
import com.zakratv.app.ui.components.SplashLoading
import com.zakratv.app.ui.components.StepLabel
import com.zakratv.app.ui.components.ZakraSpinner
import com.zakratv.app.ui.navigation.AppDestination
import com.zakratv.app.ui.theme.ZakraAccent
import com.zakratv.app.ui.theme.ZakraBg
import com.zakratv.app.ui.theme.ZakraMuted
import com.zakratv.app.ui.theme.ZakraSuccess
import com.zakratv.app.ui.theme.ZakraSurface
import com.zakratv.app.ui.theme.ZakraSurface2
import com.zakratv.app.ui.theme.ZakraText
import com.zakratv.app.ui.theme.ZakraType
import com.zakratv.app.ui.theme.ZakraWarning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ZakraRoot() {
    var booting by remember { mutableStateOf(true) }
    var dest by remember { mutableStateOf(AppDestination.Home) }
    var selected by remember { mutableStateOf<MediaItem?>(null) }
    var pendingUpdate by remember { mutableStateOf<AvailableUpdate?>(null) }
    var updateMsg by remember { mutableStateOf("") }
    var updating by remember { mutableStateOf(false) }
    val repo = ZakraApp.instance.repository
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            runCatching {
                val token = ZakraApp.instance.prefs.ensureDefaultRdToken()
                if (token.isNotBlank()) ZakraApp.instance.realDebrid.setToken(token)
            }
            // Public GitHub Releases — no API key
            pendingUpdate = runCatching { AppUpdateChecker.checkForUpdate() }.getOrNull()
        }
        delay(500)
        booting = false
    }

    if (booting) {
        SplashLoading("Cargando catálogo y buscando actualizaciones…")
        return
    }

    Column(Modifier.fillMaxSize().background(ZakraBg)) {
        // Compact update bar — fixed height, cannot cover content
        pendingUpdate?.let { upd ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(ZakraAccent.copy(alpha = 0.22f))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Actualización ${upd.versionName} lista",
                    style = ZakraType.caption,
                    color = ZakraText,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                if (updating) {
                    ZakraSpinner(size = 28)
                } else {
                    BigButton(
                        label = "Actualizar",
                        accent = true,
                        onClick = {
                            scope.launch {
                                updating = true
                                updateMsg = ""
                                if (!UpdateInstaller.canRequestPackageInstalls(context)) {
                                    UpdateInstaller.openUnknownSourcesSettings(context)
                                    updateMsg = "Activa instalar apps desconocidas y reintenta."
                                    updating = false
                                    return@launch
                                }
                                val err = withContext(Dispatchers.IO) {
                                    UpdateInstaller.downloadAndPromptInstall(context, upd.apkUrl)
                                }
                                updateMsg = err ?: "Confirma la instalación"
                                updating = false
                            }
                        },
                    )
                    BigButton(label = "Luego", onClick = { pendingUpdate = null })
                }
            }
            if (updateMsg.isNotBlank()) {
                Text(
                    updateMsg,
                    style = ZakraType.caption,
                    color = ZakraWarning,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }
        }

        Row(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            SideBar(
                current = dest,
                onSelect = {
                    selected = null
                    dest = it
                },
                modifier = Modifier
                    .width(210.dp)
                    .fillMaxHeight()
                    .background(ZakraSurface)
                    .padding(vertical = 20.dp, horizontal = 10.dp),
            )
            Box(Modifier.weight(1f).fillMaxHeight()) {
                when {
                    selected != null -> DetailScreen(
                        item = selected!!,
                        onBack = { selected = null },
                        repo = repo,
                    )
                    else -> when (dest) {
                        AppDestination.Home -> HomeScreen(repo) { selected = it }
                        AppDestination.Movies -> GridCatalogScreen(
                            title = "Películas",
                            load = { page, filter -> repo.movies(page, filter) },
                            onClick = { selected = it },
                            showFilters = true,
                        )
                        AppDestination.Series -> GridCatalogScreen(
                            title = "Series",
                            load = { page, filter -> repo.series(page, filter) },
                            onClick = { selected = it },
                            showFilters = true,
                        )
                        AppDestination.Trending -> GridCatalogScreen(
                            title = "Tendencias",
                            load = { page, _ -> repo.trending(page) },
                            onClick = { selected = it },
                            showFilters = false,
                        )
                        AppDestination.MyList -> MyListScreen(repo) { selected = it }
                        AppDestination.Search -> SearchScreen(repo) { selected = it }
                        AppDestination.Settings -> SettingsScreen()
                    }
                }
            }
        }
    }
}

@Composable
private fun SideBar(
    current: AppDestination,
    onSelect: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Zakra TV",
            style = ZakraType.title,
            color = ZakraAccent,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
        Spacer(Modifier.height(8.dp))
        AppDestination.entries.forEach { d ->
            SideNavItem(
                label = d.label,
                selected = current == d,
                onClick = { onSelect(d) },
                icon = d.iconKind,
            )
        }
    }
}

@Composable
private fun HomeScreen(
    repo: CatalogRepository,
    onClick: (MediaItem) -> Unit,
) {
    var sections by remember { mutableStateOf<List<CatalogSection>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        error = null
        runCatching { repo.homeSections() }
            .onSuccess {
                sections = it
                loading = false
            }
            .onFailure {
                error = it.message ?: "Error al cargar el catálogo"
                loading = false
            }
    }

    when {
        loading -> LoadingBox(message = "Cargando inicio…")
        error != null && sections.isEmpty() -> ErrorBox(error!!)
        else -> LazyColumn(Modifier.fillMaxSize()) {
            item {
                Text(
                    "Inicio",
                    style = ZakraType.title,
                    color = ZakraText,
                    modifier = Modifier.padding(24.dp),
                )
            }
            items(sections, key = { it.id }) { section ->
                MediaRow(
                    title = section.title,
                    items = section.items,
                    onItemClick = onClick,
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun GridCatalogScreen(
    title: String,
    load: suspend (page: Int, filter: CatalogFilter) -> List<MediaItem>,
    onClick: (MediaItem) -> Unit,
    showFilters: Boolean,
) {
    var items by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var year by remember { mutableStateOf<Int?>(null) }
    var genreId by remember { mutableStateOf<Int?>(null) }
    // Metadata always Spanish; filter only soft-preferences, never empties catalog
    var preferEs by remember { mutableStateOf(true) }

    LaunchedEffect(year, genreId, preferEs) {
        loading = true
        error = null
        val result = runCatching {
            load(
                1,
                CatalogFilter(
                    language = if (preferEs) "es" else "en",
                    genreId = genreId,
                    year = year,
                ),
            )
        }
        items = result.getOrDefault(emptyList())
        if (result.isFailure) {
            error = result.exceptionOrNull()?.message ?: "Error al cargar"
        }
        loading = false
    }

    // Header + chips are wrapContent; ONLY the grid uses weight — prevents covering
    Column(Modifier.fillMaxSize()) {
        Text(
            title,
            style = ZakraType.title,
            color = ZakraText,
            modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 8.dp),
        )
        if (showFilters) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    label = if (preferEs) "Idioma: Español" else "Idioma: Inglés",
                    selected = preferEs,
                    onClick = { preferEs = !preferEs },
                )
                FilterChip(
                    label = year?.let { "Año $it" } ?: "Año: todos",
                    selected = year != null,
                    onClick = {
                        year = when (year) {
                            null -> 2025
                            2025 -> 2024
                            2024 -> 2023
                            2023 -> 2022
                            else -> null
                        }
                    },
                )
                FilterChip(
                    label = when (genreId) {
                        null -> "Género: todos"
                        28 -> "Acción"
                        35 -> "Comedia"
                        18 -> "Drama"
                        27 -> "Terror"
                        10749 -> "Romance"
                        else -> "Género"
                    },
                    selected = genreId != null,
                    onClick = {
                        genreId = when (genreId) {
                            null -> 28
                            28 -> 35
                            35 -> 18
                            18 -> 27
                            27 -> 10749
                            else -> null
                        }
                    },
                )
            }
        }
        when {
            loading -> LoadingBox(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                message = "Cargando $title…",
            )
            error != null && items.isEmpty() -> ErrorBox(
                message = error!!,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
            items.isEmpty() -> ErrorBox(
                message = "Sin resultados con estos filtros. Prueba «Año: todos».",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                contentPadding = PaddingValues(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                items(items, key = { it.id }) { item ->
                    PosterCard(item = item, onClick = { onClick(item) }, width = 148)
                }
            }
        }
    }
}

@Composable
private fun MyListScreen(
    repo: CatalogRepository,
    onClick: (MediaItem) -> Unit,
) {
    var items by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        loading = true
        items = repo.myListItems()
        loading = false
    }
    Column(Modifier.fillMaxSize()) {
        Text(
            "Mi Lista",
            style = ZakraType.title,
            color = ZakraText,
            modifier = Modifier.padding(24.dp),
        )
        when {
            loading -> LoadingBox(Modifier.weight(1f), message = "Cargando tu lista…")
            items.isEmpty() -> ErrorBox("Tu lista está vacía. Añade títulos desde el detalle.")
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                contentPadding = PaddingValues(24.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(items, key = { it.id }) { item ->
                    PosterCard(item = item, onClick = { onClick(item) })
                }
            }
        }
    }
}

/**
 * Search with system TV IME (Fire Stick keyboard), not a custom letter grid.
 * Focus the field → OS keyboard opens.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SearchScreen(
    repo: CatalogRepository,
    onClick: (MediaItem) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var items by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Escribe el título con el teclado del televisor") }
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        delay(300)
        runCatching {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    fun runSearch() {
        val q = query.trim()
        if (q.length < 2) {
            status = "Escribe al menos 2 letras"
            return
        }
        scope.launch {
            loading = true
            status = "Buscando «$q»…"
            keyboard?.hide()
            items = repo.search(q)
            loading = false
            status = if (items.isEmpty()) {
                "Sin resultados para «$q»"
            } else {
                "${items.size} resultados (películas y series)"
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Text("Buscar", style = ZakraType.title, color = ZakraText)
        Spacer(Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .height(56.dp)
                    .background(ZakraSurface2, RoundedCornerShape(12.dp))
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (query.isEmpty()) {
                    Text(
                        "Título de película o serie…",
                        style = ZakraType.body,
                        color = ZakraMuted,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    textStyle = ZakraType.body.copy(color = ZakraText),
                    cursorBrush = SolidColor(ZakraAccent),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { runSearch() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
            }
            BigButton(
                label = "Buscar",
                accent = true,
                onClick = { runSearch() },
            )
            BigButton(
                label = "Teclado",
                onClick = {
                    focusRequester.requestFocus()
                    keyboard?.show()
                },
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(status, style = ZakraType.caption, color = ZakraMuted, maxLines = 1)
        Spacer(Modifier.height(12.dp))
        when {
            loading -> LoadingBox(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                message = "Buscando…",
            )
            items.isEmpty() -> Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Los resultados aparecen aquí",
                    style = ZakraType.bodyMuted,
                    color = ZakraMuted,
                )
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                items(items, key = { "${it.mediaType}-${it.id}" }) { item ->
                    PosterCard(item = item, onClick = { onClick(item) }, width = 140)
                }
            }
        }
    }
}

/**
 * Detalle didáctico: pasos claros, temporadas/episodios en filas horizontales,
 * enlaces (🇲🇽 Latino primero) cerca de la acción sin bajar tanto.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DetailScreen(
    item: MediaItem,
    onBack: () -> Unit,
    repo: CatalogRepository,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var detailed by remember { mutableStateOf(item) }
    var inList by remember { mutableStateOf(false) }
    var streams by remember { mutableStateOf<List<StreamLink>>(emptyList()) }
    var loadingStreams by remember { mutableStateOf(false) }
    var seasons by remember { mutableStateOf<List<Season>>(emptyList()) }
    var episodes by remember { mutableStateOf<List<Episode>>(emptyList()) }
    var seasonNum by remember { mutableIntStateOf(1) }
    var status by remember { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }

    LaunchedEffect(item.id, item.mediaType) {
        // Guarded: nothing here may crash the app while opening a title.
        runCatching {
            detailed = repo.detail(item)
            inList = repo.isInList(detailed)
            streams = emptyList()
            showPicker = false
            status = ""
            if (detailed.mediaType == MediaType.SERIES) {
                seasons = repo.seasons(detailed.id)
                if (seasons.isNotEmpty()) {
                    seasonNum = seasons.first().seasonNumber
                    episodes = repo.episodes(detailed.id, seasonNum)
                }
            }
        }
    }

    fun play(link: StreamLink) {
        runCatching {
            val intent = Intent(context, PlayerActivity::class.java).apply {
                putExtra(PlayerActivity.EXTRA_URL, link.playableUrl)
                putExtra(PlayerActivity.EXTRA_TITLE, detailed.title)
                putExtra(PlayerActivity.EXTRA_QUALITY, link.quality)
            }
            context.startActivity(intent)
        }.onFailure {
            status = "No se pudo abrir el reproductor. Prueba otro enlace."
        }
    }

    fun loadStreams(season: Int? = null, episode: Int? = null) {
        scope.launch {
            loadingStreams = true
            showPicker = true
            status = "Buscando enlaces (Latino y Real-Debrid primero)…"
            streams = repo.findStreams(detailed, season, episode)
            loadingStreams = false
            status = if (streams.isEmpty()) {
                "No hay enlaces válidos (hosts suspendidos ocultos)."
            } else {
                val lat = streams.count {
                    com.zakratv.app.data.ranking.LanguagePreference.streamLanguageLabel(it)
                        .contains("Latino")
                }
                val cached = streams.count { it.isCached || it.isPremium }
                "${streams.size} enlaces · $cached RD · $lat Latino arriba"
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        HeroBackdrop(
            url = detailed.backdropUrl("w780"),
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .align(Alignment.TopCenter),
        )
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BigButton(label = "← Volver", onClick = onBack)
                    BigButton(
                        label = if (inList) "★ En Mi Lista" else "+ Mi Lista",
                        onClick = { scope.launch { runCatching { inList = repo.toggleList(detailed) } } },
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(detailed.title, style = ZakraType.title, color = ZakraText)
                Spacer(Modifier.height(6.dp))
                val meta = buildString {
                    detailed.year?.let { append(it) }
                    if (detailed.voteAverage > 0) {
                        if (isNotEmpty()) append(" · ")
                        append("★ %.1f".format(detailed.voteAverage))
                    }
                    if (detailed.mediaType == MediaType.SERIES) {
                        if (isNotEmpty()) append(" · ")
                        append("Serie")
                    } else {
                        if (isNotEmpty()) append(" · ")
                        append("Película")
                    }
                }
                Text(meta, style = ZakraType.bodyMuted, color = ZakraMuted)
                Spacer(Modifier.height(8.dp))
                Text(
                    detailed.overview.ifBlank { "Sin descripción." },
                    style = ZakraType.body,
                    color = ZakraText,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 780.dp),
                )
            }

            // —— PASO 1: acción principal cerca del top ——
            item {
                Spacer(Modifier.height(8.dp))
                StepLabel(1, if (detailed.mediaType == MediaType.MOVIE) "Pulsa reproducir" else "Elige temporada y episodio")
                if (detailed.mediaType == MediaType.MOVIE) {
                    BigButton(
                        label = "▶ Reproducir ahora",
                        accent = true,
                        onClick = { loadStreams() },
                        modifier = Modifier.widthIn(min = 280.dp),
                    )
                }
            }

            // —— Series: filas horizontales (menos scroll) ——
            if (detailed.mediaType == MediaType.SERIES && seasons.isNotEmpty()) {
                item {
                    Text("Temporada", style = ZakraType.bodyMuted, color = ZakraMuted)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(seasons.size) { i ->
                            val s = seasons[i]
                            BigButton(
                                label = "T${s.seasonNumber}",
                                accent = s.seasonNumber == seasonNum,
                                onClick = {
                                    seasonNum = s.seasonNumber
                                    scope.launch {
                                        episodes = repo.episodes(detailed.id, seasonNum)
                                    }
                                },
                            )
                        }
                    }
                }
                item {
                    Text("Episodio (pulsa para buscar enlaces)", style = ZakraType.bodyMuted, color = ZakraMuted)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(episodes, key = { it.id }) { ep ->
                            BigButton(
                                label = "E${ep.episodeNumber}",
                                onClick = { loadStreams(seasonNum, ep.episodeNumber) },
                            )
                        }
                    }
                    if (episodes.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            episodes.firstOrNull { true }?.let { "" }.orEmpty(),
                            style = ZakraType.caption,
                            color = ZakraMuted,
                        )
                    }
                }
            }

            // —— PASO 2: enlaces (arriba, no al final del todo) ——
            if (showPicker || loadingStreams || streams.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(10.dp))
                    StepLabel(2, "Elige calidad e idioma (Latino primero)")
                    if (status.isNotBlank()) {
                        Text(status, style = ZakraType.caption, color = ZakraWarning)
                        Spacer(Modifier.height(8.dp))
                    }
                    if (loadingStreams) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ZakraSpinner(size = 36)
                            Spacer(Modifier.width(14.dp))
                            Text("Buscando en Real-Debrid…", style = ZakraType.body, color = ZakraMuted)
                        }
                    }
                }
            }

            if (streams.isNotEmpty()) {
                itemsIndexed(streams.take(18)) { i, s ->
                    BigButton(
                        label = StreamRanker.displayLabel(s, i),
                        accent = i == 0,
                        onClick = { play(s) },
                        modifier = Modifier.fillMaxWidth(0.92f),
                    )
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingsScreen() {
    val app = ZakraApp.instance
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var token by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Comprobando Real-Debrid…") }
    var rdUser by remember { mutableStateOf("") }
    var deviceCode by remember { mutableStateOf("") }
    var updateStatus by remember { mutableStateOf("Pulsa «Buscar actualización» para comprobar GitHub.") }
    var updateBusy by remember { mutableStateOf(false) }
    var foundUpdate by remember { mutableStateOf<AvailableUpdate?>(null) }

    LaunchedEffect(Unit) {
        val t = app.prefs.getRdToken()
        token = t
        if (t.isNotBlank()) {
            app.realDebrid.setToken(t)
            status = withContext(Dispatchers.IO) {
                runCatching {
                    val u = app.realDebrid.user()
                    rdUser = u.username
                    val premium = if (u.premium > 0) "Premium activo" else "Sin premium"
                    "Conectado como ${u.username} · $premium"
                }.getOrElse { "Token guardado, pero no se pudo verificar: ${it.message}" }
            }
        } else {
            status = "Sin token. Pega tu API token de Real-Debrid."
        }
    }

    fun checkUpdate() {
        scope.launch {
            updateBusy = true
            updateStatus = "Consultando GitHub…"
            foundUpdate = null
            val result = withContext(Dispatchers.IO) {
                runCatching { AppUpdateChecker.checkForUpdate() }
            }
            updateBusy = false
            result.onSuccess { upd ->
                if (upd == null) {
                    updateStatus =
                        "Ya tienes la última versión (${BuildConfig.VERSION_NAME} / ${BuildConfig.VERSION_CODE})."
                } else {
                    foundUpdate = upd
                    updateStatus =
                        "Nueva versión ${upd.versionName} (code ${upd.versionCode}). Pulsa Instalar."
                }
            }.onFailure {
                updateStatus = "No se pudo comprobar: ${it.message}"
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Ajustes", style = ZakraType.title, color = ZakraText)
        Text(
            "Versión ${BuildConfig.VERSION_NAME} (código ${BuildConfig.VERSION_CODE})",
            style = ZakraType.body,
            color = ZakraText,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigButton(
                label = if (updateBusy) "Comprobando…" else "Buscar actualización",
                accent = true,
                onClick = { if (!updateBusy) checkUpdate() },
            )
            foundUpdate?.let { upd ->
                BigButton(
                    label = "Instalar ${upd.versionName}",
                    onClick = {
                        scope.launch {
                            updateBusy = true
                            updateStatus = "Descargando APK…"
                            if (!UpdateInstaller.canRequestPackageInstalls(context)) {
                                UpdateInstaller.openUnknownSourcesSettings(context)
                                updateStatus = "Activa instalar apps desconocidas y reintenta."
                                updateBusy = false
                                return@launch
                            }
                            val err = withContext(Dispatchers.IO) {
                                UpdateInstaller.downloadAndPromptInstall(context, upd.apkUrl)
                            }
                            updateStatus = err ?: "Instalador abierto — confirma en el sistema."
                            updateBusy = false
                        }
                    },
                )
            }
        }
        Text(updateStatus, style = ZakraType.caption, color = ZakraWarning, maxLines = 2)

        Spacer(Modifier.height(8.dp))
        Text("Real-Debrid", style = ZakraType.section, color = ZakraText)
        Text(status, style = ZakraType.body, color = if (rdUser.isNotBlank()) ZakraSuccess else ZakraWarning)
        Text("Token API (real-debrid.com/apitoken)", style = ZakraType.caption, color = ZakraMuted)
        Box(
            Modifier
                .fillMaxWidth(0.7f)
                .height(56.dp)
                .background(ZakraSurface2, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = token,
                onValueChange = { token = it },
                textStyle = ZakraType.body.copy(color = ZakraText),
                cursorBrush = SolidColor(ZakraAccent),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigButton(
                label = "Guardar token",
                onClick = {
                    scope.launch {
                        app.prefs.setRdToken(token)
                        app.realDebrid.setToken(token)
                        status = withContext(Dispatchers.IO) {
                            runCatching {
                                val u = app.realDebrid.user()
                                rdUser = u.username
                                "Guardado · ${u.username}"
                            }.getOrElse { "Error: ${it.message}" }
                        }
                    }
                },
            )
            BigButton(
                label = "Código de dispositivo",
                onClick = {
                    scope.launch {
                        status = withContext(Dispatchers.IO) {
                            runCatching {
                                val dc = app.realDebrid.deviceCode()
                                deviceCode = dc.userCode
                                "Ve a ${dc.verificationUrl} e introduce: ${dc.userCode}"
                            }.getOrElse { "No se pudo iniciar OAuth: ${it.message}" }
                        }
                    }
                },
            )
        }
        if (deviceCode.isNotBlank()) {
            Text("Código: $deviceCode", style = ZakraType.section, color = ZakraAccent)
        }
    }
}
