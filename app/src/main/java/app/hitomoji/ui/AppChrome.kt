package app.hitomoji.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.hitomoji.R

enum class AppTab {
    Home,
    Catalog,
    About,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HitomojiTopBar(
    title: String,
    onUp: (() -> Unit)? = null,
) {
    CenterAlignedTopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (onUp != null) {
                IconButton(onClick = onUp) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                    )
                }
            }
        },
    )
}

@Composable
fun HitomojiNavigationBar(
    selected: AppTab,
    onSelect: (AppTab) -> Unit,
) {
    NavigationBar {
        BarItem(
            selected = selected == AppTab.Home,
            label = stringResource(R.string.tab_home),
            filled = Icons.Filled.Home,
            outlined = Icons.Outlined.Home,
            onClick = { onSelect(AppTab.Home) },
        )
        BarItem(
            selected = selected == AppTab.Catalog,
            label = stringResource(R.string.catalog),
            filled = Icons.AutoMirrored.Filled.List,
            outlined = Icons.AutoMirrored.Outlined.List,
            onClick = { onSelect(AppTab.Catalog) },
        )
        BarItem(
            selected = selected == AppTab.About,
            label = stringResource(R.string.about),
            filled = Icons.Filled.Info,
            outlined = Icons.Outlined.Info,
            onClick = { onSelect(AppTab.About) },
        )
    }
}

@Composable
private fun RowScope.BarItem(
    selected: Boolean,
    label: String,
    filled: androidx.compose.ui.graphics.vector.ImageVector,
    outlined: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = if (selected) filled else outlined,
                contentDescription = null,
            )
        },
        label = { Text(label) },
    )
}
