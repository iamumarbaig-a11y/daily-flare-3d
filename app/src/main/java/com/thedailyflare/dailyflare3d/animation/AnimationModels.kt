package com.thedailyflare.dailyflare3d.animation

import kotlinx.serialization.Serializable

@Serializable
data class AnimationProject(
    val durationMs: Long = 10_000L,
    val fps: Int = 60,
    val camera: List<CameraKeyframe> = emptyList(),
    val routes: List<RouteAnimation> = emptyList(),
    val highlights: List<RegionHighlight> = emptyList(),
    val markers: List<MarkerAnimation> = emptyList()
)

@Serializable
data class CameraKeyframe(
    val timeMs: Long,
    val latitude: Double,
    val longitude: Double,
    val zoom: Double,
    val bearing: Double = 0.0,
    val pitch: Double = 0.0
)

@Serializable
data class RouteAnimation(
    val id: String,
    val points: List<GeoPoint>,
    val startMs: Long,
    val durationMs: Long,
    val width: Float = 4f
)

@Serializable
data class RegionHighlight(
    val id: String,
    val startMs: Long,
    val durationMs: Long,
    val opacity: Float = 0.45f
)

@Serializable
data class MarkerAnimation(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val startMs: Long,
    val durationMs: Long
)

@Serializable
data class GeoPoint(
    val latitude: Double,
    val longitude: Double
)
