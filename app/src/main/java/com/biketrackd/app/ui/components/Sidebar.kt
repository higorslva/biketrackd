package com.biketrackd.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biketrackd.app.R

enum class Screen { GPS, SPEEDOMETER, BIKES, MAINTENANCE, STATISTICS, ABOUT, SETTINGS }

@Composable
fun Sidebar(
    currentScreen: Screen,
    wornCount: Int = 0,
    onScreenSelected: (Screen) -> Unit,
    burnInDimmed: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val dimAlpha by animateFloatAsState(
        targetValue = if (burnInDimmed) 0.35f else 1f,
        animationSpec = tween(800),
        label = "sidebarDim",
    )
    Column(
        modifier = modifier
            .width(80.dp)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 16.dp)
            .alpha(dimAlpha),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
    ) {
        SidebarButton(
            icon = { Icon(Icons.Default.Map, contentDescription = stringResource(R.string.desc_sidebar_gps), modifier = Modifier.size(24.dp)) },
            label = stringResource(R.string.desc_sidebar_gps),
            selected = currentScreen == Screen.GPS,
            onClick = { onScreenSelected(Screen.GPS) },
        )
        SidebarButton(
            icon = { Icon(Icons.Default.Speed, contentDescription = stringResource(R.string.desc_sidebar_panel), modifier = Modifier.size(24.dp)) },
            label = stringResource(R.string.desc_sidebar_panel),
            selected = currentScreen == Screen.SPEEDOMETER,
            onClick = { onScreenSelected(Screen.SPEEDOMETER) },
        )
        SidebarButton(
            icon = { Icon(Icons.AutoMirrored.Filled.DirectionsBike, contentDescription = stringResource(R.string.desc_sidebar_bikes), modifier = Modifier.size(24.dp)) },
            label = stringResource(R.string.desc_sidebar_bikes),
            selected = currentScreen == Screen.BIKES,
            onClick = { onScreenSelected(Screen.BIKES) },
        )
        SidebarButton(
            icon = { Icon(Icons.Default.Build, contentDescription = stringResource(R.string.desc_sidebar_maintenance), modifier = Modifier.size(24.dp)) },
            label = stringResource(R.string.desc_sidebar_maintenance),
            selected = currentScreen == Screen.MAINTENANCE,
            onClick = { onScreenSelected(Screen.MAINTENANCE) },
            badgeCount = wornCount,
        )
        SidebarButton(
            icon = { Icon(Icons.Default.Assessment, contentDescription = stringResource(R.string.desc_sidebar_stats), modifier = Modifier.size(24.dp)) },
            label = stringResource(R.string.desc_sidebar_stats),
            selected = currentScreen == Screen.STATISTICS,
            onClick = { onScreenSelected(Screen.STATISTICS) },
        )
        SidebarButton(
            icon = { Icon(Icons.Default.Info, contentDescription = stringResource(R.string.desc_sidebar_about), modifier = Modifier.size(24.dp)) },
            label = stringResource(R.string.desc_sidebar_about),
            selected = currentScreen == Screen.ABOUT,
            onClick = { onScreenSelected(Screen.ABOUT) },
        )
        SidebarButton(
            icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.desc_sidebar_options), modifier = Modifier.size(24.dp)) },
            label = stringResource(R.string.desc_sidebar_options),
            selected = currentScreen == Screen.SETTINGS,
            onClick = { onScreenSelected(Screen.SETTINGS) },
        )
    }
}

@Composable
private fun SidebarButton(
    icon: @Composable () -> Unit,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    badgeCount: Int = 0,
) {
    Box {
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surface,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier.size(64.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                icon()
                Text(
                    text = label,
                    fontSize = 9.sp,
                    maxLines = 1,
                )
            }
        }
        if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-4).dp, y = 4.dp)
                    .size(18.dp)
                    .background(MaterialTheme.colorScheme.error, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                    color = MaterialTheme.colorScheme.onError,
                    fontSize = 10.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                )
            }
        }
    }
}
