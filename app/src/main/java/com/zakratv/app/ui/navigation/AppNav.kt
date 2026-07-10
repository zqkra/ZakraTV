package com.zakratv.app.ui.navigation

/**
 * @param label visible text shown in the side rail.
 * @param iconKind optional icon for the side rail (e.g. the search magnifier).
 */
enum class AppDestination(
    val label: String,
    val iconKind: NavIcon = NavIcon.None,
) {
    Home("Inicio"),
    Movies("Películas"),
    Series("Series"),
    Trending("Tendencias"),
    Search("Buscar", NavIcon.Search),
    MyList("Mi Lista"),
    Settings("Ajustes"),
}

enum class NavIcon {
    None,
    Search,
}
