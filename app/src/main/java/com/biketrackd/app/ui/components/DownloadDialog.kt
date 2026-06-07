package com.biketrackd.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.biketrackd.app.data.DownloadProgress

@Composable
fun DownloadDialog(
    progress: DownloadProgress,
    onDismiss: () -> Unit,
) {
    val isCompleted = progress.status == DownloadProgress.Status.Completed
    val isError = progress.status == DownloadProgress.Status.Error

    if (progress.status == DownloadProgress.Status.Idle) return

    AlertDialog(
        onDismissRequest = { if (isCompleted || isError) onDismiss() },
        title = {
            Text(
                text = when (progress.status) {
                    DownloadProgress.Status.Downloading -> "Baixando mapa..."
                    DownloadProgress.Status.Completed -> "Download concluído!"
                    DownloadProgress.Status.Error -> "Erro no download"
                    DownloadProgress.Status.Idle -> ""
                },
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                when (progress.status) {
                    DownloadProgress.Status.Downloading -> {
                        Text(
                            text = "Zoom ${progress.zoom} — ${progress.current} de ${progress.total} tiles",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { progress.fraction },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${(progress.fraction * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DownloadProgress.Status.Completed -> {
                        Text("${progress.total} tiles baixados com sucesso.")
                    }
                    DownloadProgress.Status.Error -> {
                        Text(
                            "Falha ao baixar o mapa. Verifique sua conexão e tente novamente.",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    DownloadProgress.Status.Idle -> {}
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(if (isCompleted || isError) "OK" else "Cancelar")
            }
        },
    )
}
