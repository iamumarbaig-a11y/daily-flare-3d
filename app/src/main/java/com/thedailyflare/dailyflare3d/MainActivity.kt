package com.thedailyflare.dailyflare3d

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.fillColor
import org.maplibre.android.style.layers.PropertyFactory.fillOpacity
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.iconOpacity
import org.maplibre.android.style.layers.PropertyFactory.iconSize
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textHaloColor
import org.maplibre.android.style.layers.PropertyFactory.textHaloWidth
import org.maplibre.android.style.layers.PropertyFactory.textOpacity
import org.maplibre.android.style.layers.PropertyFactory.textSize
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource

private const val MAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"
private const val DURATION_MS = 10_000L
private const val ROUTE_SOURCE = "animated-route-source"
private const val ROUTE_LAYER = "animated-route-layer"
private const val MARKER_SOURCE = "animated-marker-source"
private const val MARKER_LAYER = "animated-marker-layer"
private const val HIGHLIGHT_SOURCE = "country-highlight-source"
private const val HIGHLIGHT_LAYER = "country-highlight-layer"
private const val TEXT_SOURCE = "editor-text-source"
private const val TEXT_LAYER = "editor-text-layer"
private const val PNG_SOURCE = "editor-png-source"
private const val PNG_LAYER = "editor-png-layer"
private const val PNG_IMAGE = "editor-png-image"

private enum class CameraMode(val label: String) {
    TOP_DOWN("Top-down"), BOUNCE("Bounce"), FLY_TO("Fly-to"), ORBIT("Orbit"), CINEMATIC("Cinematic")
}
private enum class EditorTab(val label: String) {
    MAP("Map"), LAYERS("Layers"), ROUTE("Route"), PNG("PNG"), TEXT("Text"), EXPORT("Export")
}
private enum class PngMotion(val label: String) {
    STATIC("Static"), PATH("Move on path"), POP_OUT("Pop out")
}
private data class Country(val name: String, val center: LatLng, val polygon: List<LatLng>)

private val countries = listOf(
    Country("Netherlands", LatLng(52.13, 5.29), listOf(
        LatLng(53.55, 3.35), LatLng(53.55, 7.25), LatLng(51.30, 7.25),
        LatLng(50.75, 5.85), LatLng(51.45, 3.35), LatLng(53.55, 3.35)
    )),
    Country("Germany", LatLng(51.16, 10.45), listOf(
        LatLng(55.05, 5.87), LatLng(55.05, 15.05), LatLng(47.27, 15.05),
        LatLng(47.27, 5.87), LatLng(55.05, 5.87)
    )),
    Country("Poland", LatLng(52.10, 19.40), listOf(
        LatLng(54.84, 14.12), LatLng(54.84, 24.15), LatLng(49.00, 24.15),
        LatLng(49.00, 14.12), LatLng(54.84, 14.12)
    )),
    Country("Ukraine", LatLng(48.38, 31.17), listOf(
        LatLng(52.38, 22.14), LatLng(52.38, 40.23), LatLng(44.38, 40.23),
        LatLng(44.38, 22.14), LatLng(52.38, 22.14)
    ))
)
private val demoRoute = listOf(
    LatLng(52.3676, 4.9041), LatLng(52.5200, 13.4050),
    LatLng(52.2298, 21.0118), LatLng(50.4501, 30.5234)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        setContent { MaterialTheme { Surface(Modifier.fillMaxSize()) { MapStudioScreen() } } }
    }
}

@Composable
private fun MapStudioScreen() {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }

    var progress by remember { mutableFloatStateOf(0f) }
    var cameraMode by remember { mutableStateOf(CameraMode.TOP_DOWN) }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var selectedCountry by remember { mutableStateOf(countries.first()) }
    var countryQuery by remember { mutableStateOf("") }
    var placeQuery by remember { mutableStateOf("") }
    var highlightEnabled by remember { mutableStateOf(false) }
    var routeVisible by remember { mutableStateOf(true) }
    var markerVisible by remember { mutableStateOf(true) }
    var textVisible by remember { mutableStateOf(true) }
    var routeWidth by remember { mutableFloatStateOf(5f) }
    var editorText by remember { mutableStateOf("Daily Flare") }
    var textSizeValue by remember { mutableFloatStateOf(24f) }
    var selectedTab by remember { mutableStateOf(EditorTab.MAP) }
    var pngUri by remember { mutableStateOf<Uri?>(null) }
    var pngName by remember { mutableStateOf("No PNG selected") }
    var pngVisible by remember { mutableStateOf(true) }
    var pngMotion by remember { mutableStateOf(PngMotion.PATH) }
    var pngSize by remember { mutableFloatStateOf(1f) }
    var pngStart by remember { mutableFloatStateOf(0f) }
    var pngDuration by remember { mutableFloatStateOf(1f) }

    fun refresh(style: Style) {
        updateVisuals(
            style, progress, selectedCountry, highlightEnabled, routeVisible, markerVisible,
            textVisible, routeWidth, editorText, textSizeValue, pngMotion, pngSize,
            pngVisible, pngStart, pngDuration
        )
    }

    val pngPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            pngUri = uri
            pngName = uri.lastPathSegment?.substringAfterLast('/') ?: "PNG image"
            map?.style?.let { style ->
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }?.let { bitmap ->
                    if (style.getImage(PNG_IMAGE) != null) style.removeImage(PNG_IMAGE)
                    style.addImage(PNG_IMAGE, bitmap)
                    installPngLayer(style)
                    refresh(style)
                }
            }
        }
    }

    fun loadStyle(loaded: MapLibreMap) {
        loaded.setStyle(Style.Builder().fromUri(MAP_STYLE)) { style ->
            installLayers(style, selectedCountry, editorText)
            installPngLayer(style)
            pngUri?.let { uri ->
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }?.let {
                    if (style.getImage(PNG_IMAGE) != null) style.removeImage(PNG_IMAGE)
                    style.addImage(PNG_IMAGE, it)
                }
            }
            refresh(style)
        }
    }

    DisposableEffect(mapView) {
        mapView.onStart()
        mapView.onResume()
        mapView.getMapAsync { loaded ->
            map = loaded
            loadStyle(loaded)
            loaded.moveCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder().target(demoRoute.first()).zoom(4.0).build()
                )
            )
        }
        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    val filteredCountries = countries.filter {
        countryQuery.isBlank() || it.name.contains(countryQuery.trim(), ignoreCase = true)
    }
    val placeResults = if (placeQuery.isBlank()) emptyList() else searchMapPlaces(placeQuery)
    val currentMs = (progress * DURATION_MS).toLong()

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).background(Color(0xFF172A3A)).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Daily Flare 3D", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text(formatTime(currentMs) + " / 0:10", color = Color.White.copy(alpha = .8f))
        }

        Box(Modifier.weight(1f)) {
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        }

        Column(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                EditorTab.entries.forEach { tab ->
                    Button(
                        onClick = { selectedTab = tab },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) { Text(tab.label, maxLines = 1) }
                }
            }

            when (selectedTab) {
                EditorTab.MAP -> {
                    Text("Map", style = MaterialTheme.typography.titleMedium)
                    Text("Place / City Search", Modifier.padding(top = 5.dp), style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = placeQuery,
                        onValueChange = { placeQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(top = 3.dp),
                        singleLine = true,
                        placeholder = { Text("Search city or landmark…") }
                    )
                    if (placeResults.isNotEmpty()) {
                        Column(Modifier.fillMaxWidth().padding(top = 3.dp)) {
                            placeResults.take(3).forEach { place ->
                                Button(
                                    onClick = {
                                        placeQuery = place.name
                                        map?.easeCamera(
                                            CameraUpdateFactory.newCameraPosition(
                                                CameraPosition.Builder().target(place.location).zoom(9.0).build()
                                            ), 650
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
                                ) { Text("${place.name} • ${place.type}") }
                            }
                        }
                    }

                    Text("Camera • ${cameraMode.label}", Modifier.padding(top = 5.dp), style = MaterialTheme.typography.titleSmall)
                    Row(Modifier.fillMaxWidth().padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        CameraMode.entries.forEach { mode ->
                            Button(
                                onClick = {
                                    cameraMode = mode
                                    map?.let { applyCameraMode(it, mode, progress) }
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 1.dp)
                            ) { Text(mode.label, maxLines = 1) }
                        }
                    }

                    Text("Country / Region", Modifier.padding(top = 5.dp), style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = countryQuery,
                        onValueChange = { countryQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(top = 3.dp),
                        singleLine = true,
                        placeholder = { Text("Search country…") }
                    )
                    if (countryQuery.isNotBlank() && filteredCountries.isNotEmpty()) {
                        Row(Modifier.fillMaxWidth().padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            filteredCountries.take(4).forEach { country ->
                                Button(
                                    onClick = {
                                        selectedCountry = country
                                        countryQuery = country.name
                                        map?.style?.let {
                                            installLayers(it, country, editorText)
                                            refresh(it)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 1.dp)
                                ) { Text(country.name, maxLines = 1) }
                            }
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(selectedCountry.name)
                            Text(
                                if (highlightEnabled) "Highlight active" else "Highlight off",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(onClick = {
                            highlightEnabled = !highlightEnabled
                            map?.style?.let { updateHighlight(it, selectedCountry, highlightEnabled, progress) }
                        }) { Text(if (highlightEnabled) "Remove" else "Highlight") }
                    }
                }

                EditorTab.LAYERS -> {
                    Text("Layers", style = MaterialTheme.typography.titleMedium)
                    LayerButton("Route", routeVisible) {
                        routeVisible = !routeVisible
                        map?.style?.let { updateLayerVisibility(it, routeVisible, markerVisible, textVisible, pngVisible) }
                    }
                    LayerButton("Marker", markerVisible) {
                        markerVisible = !markerVisible
                        map?.style?.let { updateLayerVisibility(it, routeVisible, markerVisible, textVisible, pngVisible) }
                    }
                    LayerButton("Country highlight", highlightEnabled) {
                        highlightEnabled = !highlightEnabled
                        map?.style?.let { updateHighlight(it, selectedCountry, highlightEnabled, progress) }
                    }
                    LayerButton("Text", textVisible) {
                        textVisible = !textVisible
                        map?.style?.let { updateLayerVisibility(it, routeVisible, markerVisible, textVisible, pngVisible) }
                    }
                    LayerButton("PNG image", pngVisible) {
                        pngVisible = !pngVisible
                        map?.style?.let { updatePng(it, progress, selectedCountry, pngMotion, pngSize, pngVisible, pngStart, pngDuration) }
                    }
                }

                EditorTab.ROUTE -> {
                    Text("Route", style = MaterialTheme.typography.titleMedium)
                    Text("Animated route • ${demoRoute.size} key points", Modifier.padding(top = 3.dp))
                    Text("Width ${routeWidth.toInt()} px", Modifier.padding(top = 5.dp))
                    Slider(
                        value = routeWidth,
                        onValueChange = {
                            routeWidth = it
                            map?.style?.let { style ->
                                (style.getLayer(ROUTE_LAYER) as? LineLayer)?.setProperties(lineWidth(it.toDouble()))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        valueRange = 2f..12f
                    )
                    Text("Timeline controls the route reveal.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                EditorTab.PNG -> {
                    Text("PNG Image", style = MaterialTheme.typography.titleMedium)
                    Button(
                        onClick = { pngPicker.launch("image/png") },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) { Text("Add PNG") }
                    Text(pngName, Modifier.padding(top = 3.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Text("Motion", Modifier.padding(top = 6.dp), style = MaterialTheme.typography.titleSmall)
                    Row(Modifier.fillMaxWidth().padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        PngMotion.entries.forEach { motion ->
                            Button(
                                onClick = {
                                    pngMotion = motion
                                    map?.style?.let { updatePng(it, progress, selectedCountry, pngMotion, pngSize, pngVisible, pngStart, pngDuration) }
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 1.dp)
                            ) { Text(motion.label, maxLines = 1) }
                        }
                    }

                    Text("Size ${"%.1f".format(pngSize)}x", Modifier.padding(top = 5.dp))
                    Slider(
                        value = pngSize,
                        onValueChange = {
                            pngSize = it
                            map?.style?.let { updatePng(it, progress, selectedCountry, pngMotion, pngSize, pngVisible, pngStart, pngDuration) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        valueRange = .25f..3f
                    )

                    Text("Start ${formatTime((pngStart * DURATION_MS).toLong())}", Modifier.padding(top = 2.dp))
                    Slider(
                        value = pngStart,
                        onValueChange = {
                            pngStart = it.coerceAtMost(pngDuration)
                            map?.style?.let { updatePng(it, progress, selectedCountry, pngMotion, pngSize, pngVisible, pngStart, pngDuration) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        valueRange = 0f..1f
                    )

                    Text("Duration ${"%.1f".format(pngDuration * 10)}s", Modifier.padding(top = 2.dp))
                    Slider(
                        value = pngDuration,
                        onValueChange = {
                            pngDuration = it.coerceAtLeast(pngStart)
                            map?.style?.let { updatePng(it, progress, selectedCountry, pngMotion, pngSize, pngVisible, pngStart, pngDuration) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        valueRange = .1f..1f
                    )

                    Text(
                        when (pngMotion) {
                            PngMotion.STATIC -> "PNG stays at the selected country."
                            PngMotion.PATH -> "PNG follows the animated route."
                            PngMotion.POP_OUT -> "PNG starts on the country and pops outward."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                EditorTab.TEXT -> {
                    Text("Text", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = editorText,
                        onValueChange = {
                            editorText = it
                            map?.style?.let { style ->
                                (style.getLayer(TEXT_LAYER) as? SymbolLayer)?.setProperties(textField(editorText))
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        singleLine = true
                    )
                    Text("Size ${textSizeValue.toInt()} px", Modifier.padding(top = 4.dp))
                    Slider(
                        value = textSizeValue,
                        onValueChange = {
                            textSizeValue = it
                            map?.style?.let { style ->
                                (style.getLayer(TEXT_LAYER) as? SymbolLayer)?.setProperties(textSize(it.toDouble()))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        valueRange = 12f..60f
                    )
                }

                EditorTab.EXPORT -> {
                    Text("Export", style = MaterialTheme.typography.titleMedium)
                    Text("MP4 export — coming next.", Modifier.padding(top = 5.dp))
                    Text("Target: deterministic 30/60 FPS rendering.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(4.dp))
            Text("Timeline", style = MaterialTheme.typography.titleSmall)
            Slider(
                value = progress,
                onValueChange = {
                    progress = it
                    map?.style?.let { style -> refresh(style) }
                },
                modifier = Modifier.fillMaxWidth(),
                valueRange = 0f..1f
            )
            Text(formatTime(currentMs), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LayerButton(label: String, active: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(top = 3.dp)) {
        Text("$label • ${if (active) "ON" else "OFF"}")
    }
}

private fun installLayers(style: Style, country: Country, text: String) {
    if (style.getSource(ROUTE_SOURCE) == null) style.addSource(GeoJsonSource(ROUTE_SOURCE, routeGeoJson(demoRoute)))
    if (style.getLayer(ROUTE_LAYER) == null) {
        style.addLayer(
            LineLayer(ROUTE_LAYER, ROUTE_SOURCE).withProperties(
                lineColor("#FF5A36"), lineWidth(5.0), lineOpacity(0.95), lineCap("round")
            )
        )
    }

    if (style.getSource(MARKER_SOURCE) == null) style.addSource(GeoJsonSource(MARKER_SOURCE, pointGeoJson(demoRoute.first())))
    if (style.getLayer(MARKER_LAYER) == null) {
        style.addLayer(
            CircleLayer(MARKER_LAYER, MARKER_SOURCE).withProperties(
                circleColor("#FFFFFF"), circleRadius(7.0), circleOpacity(1.0)
            )
        )
    }

    if (style.getSource(HIGHLIGHT_SOURCE) == null) {
        style.addSource(GeoJsonSource(HIGHLIGHT_SOURCE, polygonGeoJson(country.polygon)))
    } else {
        (style.getSource(HIGHLIGHT_SOURCE) as? GeoJsonSource)?.setGeoJson(polygonGeoJson(country.polygon))
    }
    if (style.getLayer(HIGHLIGHT_LAYER) == null) {
        style.addLayer(
            FillLayer(HIGHLIGHT_LAYER, HIGHLIGHT_SOURCE).withProperties(
                fillColor("#FFB703"), fillOpacity(0.45)
            )
        )
    }

    if (style.getSource(TEXT_SOURCE) == null) style.addSource(GeoJsonSource(TEXT_SOURCE, pointGeoJson(country.center)))
    if (style.getLayer(TEXT_LAYER) == null) {
        style.addLayer(
            SymbolLayer(TEXT_LAYER, TEXT_SOURCE).withProperties(
                textField(text), textColor("#FFFFFF"), textHaloColor("#172A3A"),
                textHaloWidth(2.0), textSize(24.0), textOpacity(1.0),
                iconAllowOverlap(true), iconIgnorePlacement(true)
            )
        )
    }
}

private fun installPngLayer(style: Style) {
    if (style.getSource(PNG_SOURCE) == null) style.addSource(GeoJsonSource(PNG_SOURCE, pointGeoJson(demoRoute.first())))
    if (style.getLayer(PNG_LAYER) == null) {
        style.addLayer(
            SymbolLayer(PNG_LAYER, PNG_SOURCE).withProperties(
                iconImage(PNG_IMAGE), iconAllowOverlap(true), iconIgnorePlacement(true),
                iconOpacity(0.0), iconSize(1.0)
            )
        )
    }
}

private fun updateVisuals(
    style: Style, progress: Float, country: Country, highlight: Boolean,
    routeVisible: Boolean, markerVisible: Boolean, textVisible: Boolean, routeWidth: Float,
    text: String, textSizeValue: Float, pngMotion: PngMotion, pngSize: Float,
    pngVisible: Boolean, pngStart: Float, pngDuration: Float
) {
    installLayers(style, country, text)
    installPngLayer(style)
    (style.getLayer(ROUTE_LAYER) as? LineLayer)?.setProperties(
        lineWidth(routeWidth.toDouble()), lineOpacity(if (routeVisible) 0.95 else 0.0)
    )
    (style.getLayer(MARKER_LAYER) as? CircleLayer)?.setProperties(
        circleOpacity(if (markerVisible) 1.0 else 0.0)
    )
    (style.getLayer(TEXT_LAYER) as? SymbolLayer)?.setProperties(
        textField(text), textSize(textSizeValue.toDouble()),
        textOpacity(if (textVisible) 1.0 else 0.0)
    )
    updateHighlight(style, country, highlight, progress)
    updateRouteAndMarker(style, progress)
    updatePng(style, progress, country, pngMotion, pngSize, pngVisible, pngStart, pngDuration)
}

private fun updateLayerVisibility(style: Style, route: Boolean, marker: Boolean, text: Boolean, png: Boolean) {
    (style.getLayer(ROUTE_LAYER) as? LineLayer)?.setProperties(lineOpacity(if (route) 0.95 else 0.0))
    (style.getLayer(MARKER_LAYER) as? CircleLayer)?.setProperties(circleOpacity(if (marker) 1.0 else 0.0))
    (style.getLayer(TEXT_LAYER) as? SymbolLayer)?.setProperties(textOpacity(if (text) 1.0 else 0.0))
    (style.getLayer(PNG_LAYER) as? SymbolLayer)?.setProperties(iconOpacity(if (png) 1.0 else 0.0))
}

private fun updateHighlight(style: Style, country: Country, enabled: Boolean, progress: Float) {
    (style.getSource(HIGHLIGHT_SOURCE) as? GeoJsonSource)?.setGeoJson(polygonGeoJson(country.polygon))
    val opacity = if (enabled) 0.15 + 0.45 * easeInOut(progress.toDouble()) else 0.0
    (style.getLayer(HIGHLIGHT_LAYER) as? FillLayer)?.setProperties(fillOpacity(opacity))
}

private fun updateRouteAndMarker(style: Style, progress: Float) {
    val route = revealRoute(demoRoute, progress.toDouble())
    (style.getSource(ROUTE_SOURCE) as? GeoJsonSource)?.setGeoJson(routeGeoJson(route))
    val marker = interpolateRoute(demoRoute, progress.toDouble())
    (style.getSource(MARKER_SOURCE) as? GeoJsonSource)?.setGeoJson(pointGeoJson(marker))
}

private fun updatePng(
    style: Style, progress: Float, country: Country, motion: PngMotion,
    size: Float, visible: Boolean, start: Float, duration: Float
) {
    val layer = style.getLayer(PNG_LAYER) as? SymbolLayer ?: return
    val active = visible && progress >= start && progress <= duration
    if (!active || style.getImage(PNG_IMAGE) == null) {
        layer.setProperties(iconOpacity(0.0))
        return
    }

    val local = if (duration <= start) 1.0 else ((progress - start) / (duration - start)).coerceIn(0f, 1f)
    val point = when (motion) {
        PngMotion.STATIC -> country.center
        PngMotion.PATH -> interpolateRoute(demoRoute, local.toDouble())
        PngMotion.POP_OUT -> {
            val eased = easeOutBack(local.toDouble())
            LatLng(country.center.latitude + 3.5 * eased, country.center.longitude + 3.0 * eased)
        }
    }
    (style.getSource(PNG_SOURCE) as? GeoJsonSource)?.setGeoJson(pointGeoJson(point))
    layer.setProperties(
        iconOpacity(1.0),
        iconSize(
            when (motion) {
                PngMotion.POP_OUT -> (0.25 + (size - 0.25) * local).toDouble()
                else -> size.toDouble()
            }
        )
    )
}

private fun applyCameraMode(map: MapLibreMap, mode: CameraMode, progress: Float) {
    val p = interpolateRoute(demoRoute, progress.toDouble())
    val builder = CameraPosition.Builder().target(p)
    when (mode) {
        CameraMode.TOP_DOWN -> builder.zoom(4.0).tilt(0.0).bearing(0.0)
        CameraMode.BOUNCE -> builder.zoom(5.2 + 0.8 * kotlin.math.sin(progress * Math.PI).toDouble()).tilt(20.0)
        CameraMode.FLY_TO -> builder.zoom(7.0 + 2.0 * progress.toDouble()).tilt(20.0)
        CameraMode.ORBIT -> builder.zoom(5.5).tilt(35.0).bearing(progress * 360.0)
        CameraMode.CINEMATIC -> builder.zoom(5.0 + 2.5 * progress.toDouble()).tilt(30.0 + 20.0 * progress).bearing(-35.0 + 70.0 * progress)
    }
    map.easeCamera(CameraUpdateFactory.newCameraPosition(builder.build()), 350)
}

private fun revealRoute(points: List<LatLng>, progress: Double): List<LatLng> {
    if (points.size < 2 || progress <= 0.0) return if (progress <= 0.0) listOf(points.first()) else points
    if (progress >= 1.0) return points
    val segments = points.size - 1
    val scaled = progress * segments
    val index = kotlin.math.floor(scaled).toInt().coerceAtMost(segments - 1)
    val local = scaled - index
    val result = points.take(index + 1).toMutableList()
    result.add(interpolate(points[index], points[index + 1], local))
    return result
}

private fun interpolateRoute(points: List<LatLng>, progress: Double): LatLng {
    if (points.size < 2) return points.first()
    val scaled = progress.coerceIn(0.0, 1.0) * (points.size - 1)
    val index = kotlin.math.floor(scaled).toInt().coerceAtMost(points.size - 2)
    return interpolate(points[index], points[index + 1], scaled - index)
}

private fun interpolate(a: LatLng, b: LatLng, t: Double): LatLng =
    LatLng(a.latitude + (b.latitude - a.latitude) * t, a.longitude + (b.longitude - a.longitude) * t)

private fun easeInOut(t: Double): Double =
    if (t < 0.5) 2.0 * t * t else 1.0 - ((-2.0 * t + 2.0) * (-2.0 * t + 2.0)) / 2.0

private fun easeOutBack(t: Double): Double {
    val c1 = 1.70158
    val c3 = c1 + 1.0
    val x = t - 1.0
    return 1.0 + c3 * x * x * x + c1 * x * x
}

private fun pointGeoJson(point: LatLng): String =
    """{"type":"Feature","geometry":{"type":"Point","coordinates":[${point.longitude},${point.latitude}]}}"""

private fun routeGeoJson(points: List<LatLng>): String =
    """{"type":"Feature","geometry":{"type":"LineString","coordinates":[${points.joinToString(",") { "[${it.longitude},${it.latitude}]" }}]}}"""

private fun polygonGeoJson(points: List<LatLng>): String =
    """{"type":"Feature","geometry":{"type":"Polygon","coordinates":[[${points.joinToString(",") { "[${it.longitude},${it.latitude}]" }}]]}}"""

private fun formatTime(ms: Long): String {
    val seconds = ms / 1000
    return "0:${seconds.toString().padStart(2, '0')}"
}
