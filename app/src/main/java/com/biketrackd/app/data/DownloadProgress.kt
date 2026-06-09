package com.biketrackd.app.data

data class DownloadProgress(
    val status: Status = Status.Idle,
    val zoom: Int = 0,
    val current: Int = 0,
    val total: Int = 0,
    val fileName: String = "",
) {
    val fraction: Float get() = if (total > 0) current.toFloat() / total else 0f
    val isActive: Boolean get() = status == Status.Downloading

    enum class Status { Idle, Downloading, Completed, Error }
}
