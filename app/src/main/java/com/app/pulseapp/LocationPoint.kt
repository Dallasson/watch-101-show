package com.app.pulseapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LocationPoint(
    val lat: Double,
    val lon: Double,
    val networkType: String = "unknown",
    val timestamp: Long = 0
) : Parcelable
