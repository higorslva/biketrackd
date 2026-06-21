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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.biketrackd.app.R
import com.biketrackd.app.data.DownloadProgress
import com.biketrackd.app.data.DownloadProgress.Status

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
                    DownloadProgress.Status.Downloading -> stringResource(R.string.dialog_downloading)
                    DownloadProgress.Status.Completed -> stringResource(R.string.dialog_download_complete)
                    DownloadProgress.Status.Error -> stringResource(R.string.dialog_download_error)
                    DownloadProgress.Status.Idle -> ""
                },
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                when (progress.status) {
                    DownloadProgress.Status.Downloading -> {
                        val totalText = if (progress.total > 0)
                            stringResource(R.string.label_progress_count, progress.current, progress.total)
                        else
                            stringResource(R.string.label_progress_unknown, progress.current)
                        Text(
                            text = totalText,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { if (progress.total > 0) progress.fraction else 0f },
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
                        Text(stringResource(R.string.label_download_success, progress.total))
                    }
                    DownloadProgress.Status.Error -> {
                        Text(
                            stringResource(R.string.label_download_fail),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    DownloadProgress.Status.Idle -> {}
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(if (isCompleted || isError) stringResource(R.string.btn_ok) else stringResource(R.string.btn_cancel))
            }
        },
    )
}
