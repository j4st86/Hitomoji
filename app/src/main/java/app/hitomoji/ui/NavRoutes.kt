package app.hitomoji.ui

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data class StudyRoute(val rank: Int, val gradingAllowed: Boolean)

@Serializable
data object CatalogRoute

@Serializable
data class KanjiViewRoute(val rank: Int)

@Serializable
data object AboutRoute
