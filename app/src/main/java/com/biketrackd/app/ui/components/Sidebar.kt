package com.biketrackd.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biketrackd.app.R

enum class Screen { GPS, SPEEDOMETER, SETTINGS }

@Composable
fun Sidebar(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(80.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 16.dp),
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
) {
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
}
