package com.motorola.launcher.ui.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Top-level immutable state holding workspace and drawer UI tree data.
 */
@Immutable
data class LauncherUiState(
    val dockApps: ImmutableList<AppItem> = persistentListOf(),
    val drawerApps: ImmutableList<AppItem> = persistentListOf(),
    val searchResults: ImmutableList<AppItem> = persistentListOf(),
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val isLoading: Boolean = true
)
