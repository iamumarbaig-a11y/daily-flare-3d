package com.thedailyflare.dailyflare3d

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource

private const val MAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"
private const val DURATION_MS = 10_000L
private const val ROUTE_SOURCE = "animated-route-source"
private const val ROUTE_LAYER = "animated-route-layer"
private const val MARKER_SOURCE = "animated-marker-source"
private const val MARKER_LAYER = "animated-marker-layer"

private enum class CameraMode(val label: String) {
    TOP_DOWN("Top-down"), BOUNCE("Bounce"), FLY_TO("Fly-to"), ORBIT("Orbit"), CINEMATIC("Cinematic")
}

private val demoRoute = listOf(
    LatLng(52.3676, 4.9041),
    LatLng(52.2297, 5.1667),
    LatLng(52.5200, 13.4050),
    LatLng(52.2298, 21.0118),
    LatLng(50.4501, 30.5234)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        setContent {
            MaterialTheme { Surface(modifier = Modifier.fillMaxSize()) { MapStudioScreen() } }
        }
    }
}

@Composable
private fun MapStudioScreen() {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    var timelineProgress by remember { mutableFloatStateOf(0f) }
    var cameraMode by remember { mutableStateOf(CameraMode.TOP_DOWN) }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var routeReady by remember { mutableStateOf(false) }

    DisposableEffect(mapView) {
        mapView.onStart()
        mapView.onResume()
        mapView.getMapAsync { loadedMap ->
            map = loadedMap
            loadedMap.setStyle(Style.Builder().fromUri(MAP_STYLE)) { style ->
                installRouteLayers(style)
                routeReady = true
                updateRouteVisuals(style, 0f)
                loadedMap.moveCamera(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder()
                            .target(demoRoute.first())
                            .zoom(4.0)
                            .build()
                    )
                )
            }
        }
        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    val currentMs = (timelineProgress * DURATION_MS).toLong()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).background(Color(0xFF172A3A)).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Daily Flare 3D", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text(formatTime(currentMs) + " / 0:10", color = Color.White.copy(alpha = 0.8f))
        }

        Box(modifier = Modifier.weight(1f)) {
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        }

        Column(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Camera", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.width(8.dp))
                Text(cameraMode.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CameraMode.entries.forEach { mode ->
                    Button(
                        onClick = {
                            cameraMode = mode
                            map?.let { applyCameraMode(it, mode, timelineProgress) }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                    ) { Text(mode.label, maxLines = 1) }
                }
            }

            Text("Route animation • 10 seconds • 60 FPS", modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleSmall)
            Slider(
                value = timelineProgress,
                onValueChange = {
                    timelineProgress = it
                    map?.let { loadedMap ->
                        loadedMap.style?.let { style ->
                            if (routeReady) updateRouteVisuals(style, it)
                        }
                        applyCameraMode(loadedMap, cameraMode, it)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("0:00", style = MaterialTheme.typography.labelSmall)
                Text("Route • Moving marker", style = MaterialTheme.typography.labelSmall)
                Text("0:10", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun installRouteLayers(style: Style) {
    if (style.getSource(ROUTE_SOURCE) == null) {
        style.addSource(GeoJsonSource(ROUTE_SOURCE, emptyLineGeoJson()))
    }
    if (style.getSource(MARKER_SOURCE) == null) {
        style.addSource(GeoJsonSource(MARKER_SOURCE, pointGeoJson(demoRoute.first())))
    }
    if (style.getLayer(ROUTE_LAYER) == null) {
        style.addLayer(
            LineLayer(ROUTE_LAYER, ROUTE_SOURCE).withProperties(
                lineColor(android.graphics.Color.parseColor("#E05A47")),
                lineWidth(5f),
                lineCap("round")
            )
        )
    }
    if (style.getLayer(MARKER_LAYER) == null) {
        style.addLayer(
            CircleLayer(MARKER_LAYER, MARKER_SOURCE).withProperties(
                circleRadius(7f),
                circleColor(android.graphics.Color.parseColor("#172A3A"))
            )
        )
    }
}

private fun updateRouteVisuals(style: Style, progress: Float) {
    val p = progress.coerceIn(0f, 1f)
    val scaled = p.toDouble() * (demoRoute.size - 1)
    val segment = scaled.toInt().coerceAtMost(demoRoute.size - 2)
    val local = scaled - segment
    val current = interpolate(demoRoute[segment], demoRoute[segment + 1], local)
    val visible = demoRoute.take(segment + 1).toMutableList()
    if (visible.last() != current) visible.add(current)

    val routeSource = style.getSource(ROUTE_SOURCE) as? GeoJsonSource
    val markerSource = style.getSource(MARKER_SOURCE) as? GeoJsonSource
    routeSource?.setGeoJson(lineGeoJson(if (p == 0f) listOf(demoRoute.first()) else visible))
    markerSource?.setGeoJson(pointGeoJson(current))
}

private fun interpolate(a: LatLng, b: LatLng, t: Double): LatLng {
    val clamped = t.coerceIn(0.0, 1.0)
    return LatLng(
        a.latitude + (b.latitude - a.latitude) * clamped,
        a.longitude + (b.longitude - a.longitude) * clamped
    )
}

private fun lineGeoJson(points: List<LatLng>): String =
    "{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"LineString\",\"coordinates\":[" +
        points.joinToString(",") { "[${it.longitude},${it.latitude}]" } + "]}}"

private fun emptyLineGeoJson(): String = lineGeoJson(listOf(demoRoute.first(), demoRoute.first()))

private fun pointGeoJson(point: LatLng): String =
    "{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Point\",\"coordinates\":[${point.longitude},${point.latitude}]}}"

private fun applyCameraMode(map: MapLibreMap, mode: CameraMode, progress: Float) {
    val current = map.cameraPosition
    val p = progress.coerceIn(0f, 1f)
    val bearing = when (mode) {
        CameraMode.ORBIT -> p * 360.0
        CameraMode.CINEMATIC -> -18.0 + p * 36.0
        else -> current.bearing
    }
    val pitch = when (mode) {
        CameraMode.TOP_DOWN -> 0.0
        CameraMode.BOUNCE -> 8.0 + kotlin.math.sin(p * Math.PI * 4.0) * 28.0
        CameraMode.FLY_TO -> 20.0 + p * 35.0
        CameraMode.ORBIT -> 35.0
        CameraMode.CINEMATIC -> 25.0 + kotlin.math.sin(p * Math.PI) * 25.0
    }
    val zoom = when (mode) {
        CameraMode.TOP_DOWN -> current.zoom.coerceAtLeast(2.5)
        CameraMode.BOUNCE -> 4.0 + kotlin.math.sin(p * Math.PI * 4.0) * 0.35
        CameraMode.FLY_TO -> 2.5 + p * 4.0
        CameraMode.ORBIT -> 4.5
        CameraMode.CINEMATIC -> 3.5 + p * 2.0
    }
    val target = CameraPosition.Builder(current)
        .zoom(zoom)
        .bearing(bearing)
        .tilt(pitch)
        .build()
    map.easeCamera(CameraUpdateFactory.newCameraPosition(target), 180)
}

private fun formatTime(ms: Long): String {
    val seconds = ms / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
