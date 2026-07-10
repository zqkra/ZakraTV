package com.zakratv.app.ui.navigation

/**
 * Side-rail destinations. Order is intentional: Inicio and Buscar on top (most used),
 * then the content sections, and Ajustes last. Every item has an icon so the rail
 * reads like a real streaming app.
 */
enum class AppDestination(
    val label: String,
    val iconKind: NavIcon,
) {
    Home("Inicio", NavIcon.Home),
    Search("Buscar", NavIcon.Search),
    Movies("Películas", NavIcon.Movies),
    Series("Series", NavIcon.Series),
    Trending("Tendencias", NavIcon.Trending),
    MyList("Mi Lista", NavIcon.MyList),
    Settings("Ajustes", NavIcon.Settings),
}

enum class NavIcon {
    Home,
    Search,
    Movies,
    Series,
    Trending,
    MyList,
    Settings,
}
