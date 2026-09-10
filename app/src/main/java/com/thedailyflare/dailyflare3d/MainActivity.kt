package com.thedailyflare.dailyflare3d

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.foundation.layout.width
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
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textHaloColor
import org.maplibre.android.style.layers.PropertyFactory.textHaloWidth
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

private enum class CameraMode(val label: String) { TOP_DOWN("Top-down"), BOUNCE("Bounce"), FLY_TO("Fly-to"), ORBIT("Orbit"), CINEMATIC("Cinematic") }
private enum class EditorTab(val label: String) { MAP("Map"), LAYERS("Layers"), ROUTE("Route"), TEXT("Text"), EXPORT("Export") }
private data class Country(val name: String, val center: LatLng, val polygon: List<LatLng>)

private val countries = listOf(
    Country("Netherlands", LatLng(52.13, 5.29), listOf(LatLng(53.55,3.35),LatLng(53.55,7.25),LatLng(51.30,7.25),LatLng(50.75,5.85),LatLng(51.45,3.35),LatLng(53.55,3.35))),
    Country("Germany", LatLng(51.16, 10.45), listOf(LatLng(55.05,5.87),LatLng(55.05,15.05),LatLng(47.27,15.05),LatLng(47.27,5.87),LatLng(55.05,5.87))),
    Country("Poland", LatLng(52.10, 19.40), listOf(LatLng(54.84,14.12),LatLng(54.84,24.15),LatLng(49.00,24.15),LatLng(49.00,14.12),LatLng(54.84,14.12))),
    Country("Ukraine", LatLng(48.38, 31.17), listOf(LatLng(52.38,22.14),LatLng(52.38,40.23),LatLng(44.38,40.23),LatLng(44.38,22.14),LatLng(52.38,22.14)))
)
private val demoRoute = listOf(LatLng(52.3676,4.9041),LatLng(52.5200,13.4050),LatLng(52.2298,21.0118),LatLng(50.4501,30.5234))

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
    var ready by remember { mutableStateOf(false) }
    var selectedCountry by remember { mutableStateOf(countries.first()) }
    var countryQuery by remember { mutableStateOf("") }
    var placeQuery by remember { mutableStateOf("") }
    var selectedPlace by remember { mutableStateOf<MapPlace?>(null) }
    var highlightEnabled by remember { mutableStateOf(false) }
    var routeVisible by remember { mutableStateOf(true) }
    var markerVisible by remember { mutableStateOf(true) }
    var textVisible by remember { mutableStateOf(true) }
    var routeWidth by remember { mutableFloatStateOf(5f) }
    var editorText by remember { mutableStateOf("Daily Flare") }
    var textSizeValue by remember { mutableFloatStateOf(24f) }
    var selectedTab by remember { mutableStateOf(EditorTab.MAP) }

    fun loadMapStyle(loaded: MapLibreMap) {
        loaded.setStyle(Style.Builder().fromUri(MAP_STYLE)) { style ->
            installLayers(style, selectedCountry, editorText)
            ready = true
            updateVisuals(style, progress, selectedCountry, highlightEnabled, routeVisible, markerVisible, textVisible, routeWidth, editorText, textSizeValue)
        }
    }

    DisposableEffect(mapView) {
        mapView.onStart(); mapView.onResume()
        mapView.getMapAsync { loaded ->
            map = loaded
            loadMapStyle(loaded)
            loaded.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder().target(demoRoute.first()).zoom(4.0).build()))
        }
        onDispose { mapView.onPause(); mapView.onStop(); mapView.onDestroy() }
    }

    val filteredCountries = countries.filter { countryQuery.isBlank() || it.name.contains(countryQuery.trim(), ignoreCase = true) }
    val placeResults = if (placeQuery.isBlank()) emptyList() else searchMapPlaces(placeQuery)
    val currentMs = (progress * DURATION_MS).toLong()

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(56.dp).background(Color(0xFF172A3A)).padding(horizontal=16.dp), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.SpaceBetween) {
            Text("Daily Flare 3D", color=Color.White, style=MaterialTheme.typography.titleLarge)
            Text(formatTime(currentMs)+" / 0:10", color=Color.White.copy(alpha=.8f))
        }
        Box(Modifier.weight(1f)) { AndroidView(factory={mapView}, modifier=Modifier.fillMaxSize()) }

        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal=12.dp, vertical=8.dp)) {
            when (selectedTab) {
                EditorTab.MAP -> {
                    Text("Map", style=MaterialTheme.typography.titleMedium)
                    Text("Place / City Search", Modifier.padding(top=6.dp), style=MaterialTheme.typography.titleSmall)
                    OutlinedTextField(placeQuery,{placeQuery=it},Modifier.fillMaxWidth().padding(top=3.dp),singleLine=true,placeholder={Text("Search city or landmark…")})
                    if (placeResults.isNotEmpty()) {
                        Column(Modifier.fillMaxWidth().padding(top=3.dp)) {
                            placeResults.take(3).forEach { place ->
                                Button({ selectedPlace=place; placeQuery=place.name; map?.let { loaded -> loaded.easeCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder().target(place.location).zoom(9.0).build()),650) } },Modifier.fillMaxWidth().padding(vertical=1.dp)) { Text("${place.name} • ${place.type}") }
                            }
                        }
                    }
                    Text("Camera • ${cameraMode.label}", Modifier.padding(top=6.dp), style=MaterialTheme.typography.titleSmall)
                    Row(Modifier.fillMaxWidth().padding(top=3.dp), horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        CameraMode.entries.forEach { mode -> Button({ cameraMode=mode; map?.let { applyCameraMode(it,mode,progress) } }, Modifier.weight(1f), contentPadding=PaddingValues(horizontal=2.dp)) { Text(mode.label,maxLines=1) } }
                    }
                    Text("Country / Region", Modifier.padding(top=7.dp), style=MaterialTheme.typography.titleSmall)
                    OutlinedTextField(countryQuery,{countryQuery=it},Modifier.fillMaxWidth().padding(top=3.dp),singleLine=true,placeholder={Text("Search country…")})
                    if (countryQuery.isNotBlank() && filteredCountries.isNotEmpty()) {
                        Row(Modifier.fillMaxWidth().padding(top=3.dp), horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                            filteredCountries.take(4).forEach { country -> Button({ selectedCountry=country; countryQuery=country.name; map?.style?.let { installLayers(it,country,editorText); updateVisuals(it,progress,country,highlightEnabled,routeVisible,markerVisible,textVisible,routeWidth,editorText,textSizeValue) } },Modifier.weight(1f),contentPadding=PaddingValues(horizontal=2.dp)) { Text(country.name,maxLines=1) } }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(top=4.dp), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) { Text(selectedCountry.name); Text(if(highlightEnabled) "Highlight active" else "Highlight off",color=MaterialTheme.colorScheme.onSurfaceVariant) }
                        Button({ highlightEnabled=!highlightEnabled; map?.style?.let { updateHighlight(it,selectedCountry,highlightEnabled,progress) } }) { Text(if(highlightEnabled) "Remove" else "Highlight") }
                    }
                }
                EditorTab.LAYERS -> {
                    Text("Layers", style=MaterialTheme.typography.titleMedium)
                    LayerButton("Route", routeVisible) { routeVisible=!routeVisible; map?.style?.let { updateLayerVisibility(it,routeVisible,markerVisible,textVisible) } }
                    LayerButton("Marker", markerVisible) { markerVisible=!markerVisible; map?.style?.let { updateLayerVisibility(it,routeVisible,markerVisible,textVisible) } }
                    LayerButton("Country highlight", highlightEnabled) { highlightEnabled=!highlightEnabled; map?.style?.let { updateHighlight(it,selectedCountry,highlightEnabled,progress) } }
                    LayerButton("Text", textVisible) { textVisible=!textVisible; map?.style?.let { updateLayerVisibility(it,routeVisible,markerVisible,textVisible) } }
                }
                EditorTab.ROUTE -> {
                    Text("Route", style=MaterialTheme.typography.titleMedium)
                    Text("Animated route • ${demoRoute.size} key points", Modifier.padding(top=3.dp))
                    Text("Width ${routeWidth.toInt()} px", Modifier.padding(top=5.dp))
                    Slider(routeWidth,{ routeWidth=it; map?.style?.let { style -> (style.getLayer(ROUTE_LAYER) as? LineLayer)?.setProperties(lineWidth(it)) }},Modifier.fillMaxWidth(),valueRange=2f..12f)
                    Text("Timeline controls the route reveal.",color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                EditorTab.TEXT -> {
                    Text("Text Layer", style=MaterialTheme.typography.titleMedium)
                    OutlinedTextField(editorText,{editorText=it; map?.style?.let { style -> updateTextLayer(style,editorText,textSizeValue,textVisible) }},Modifier.fillMaxWidth().padding(top=4.dp),singleLine=true,placeholder={Text("Enter map text…")})
                    Text("Size ${textSizeValue.toInt()}", Modifier.padding(top=4.dp))
                    Slider(textSizeValue,{ textSizeValue=it; map?.style?.let { style -> updateTextLayer(style,editorText,textSizeValue,textVisible) }},Modifier.fillMaxWidth(),valueRange=12f..64f)
                    Text("Text is anchored to the selected place or country.",color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                EditorTab.EXPORT -> {
                    Text("Export", style=MaterialTheme.typography.titleMedium)
                    Text("Render pipeline", Modifier.padding(top=5.dp), style=MaterialTheme.typography.titleSmall)
                    Text("60 FPS timeline is ready. MP4 rendering is the next engine milestone.", Modifier.padding(top=2.dp))
                    Spacer(Modifier.height(4.dp))
                    Button({}, enabled=false, modifier=Modifier.fillMaxWidth()) { Text("Export MP4 — coming next") }
                }
            }

            Text("Timeline • Route • Highlight • Marker", Modifier.padding(top=7.dp), style=MaterialTheme.typography.titleSmall)
            Slider(progress,{ progress=it; map?.let { loaded -> loaded.style?.let { style -> if(ready) updateVisuals(style,it,selectedCountry,highlightEnabled,routeVisible,markerVisible,textVisible,routeWidth,editorText,textSizeValue) }; applyCameraMode(loaded,cameraMode,it) }},Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) { Text("0:00",style=MaterialTheme.typography.labelSmall); Text("60 FPS",style=MaterialTheme.typography.labelSmall); Text("0:10",style=MaterialTheme.typography.labelSmall) }
            Row(Modifier.fillMaxWidth().padding(top=5.dp), horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                EditorTab.entries.forEach { tab -> Button({selectedTab=tab},Modifier.weight(1f),contentPadding=PaddingValues(horizontal=2.dp)){Text(tab.label,maxLines=1)} }
            }
        }
    }
}

@Composable
private fun LayerButton(name:String, enabled:Boolean, onClick:()->Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical=2.dp),verticalAlignment=Alignment.CenterVertically) {
        Button(onClick,Modifier.weight(1f)){Text(name)}
        Spacer(Modifier.width(8.dp))
        Text(if(enabled) "ON" else "OFF",color=if(enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun installLayers(style: Style, country: Country, text: String) {
    if(style.getSource(ROUTE_SOURCE)==null) style.addSource(GeoJsonSource(ROUTE_SOURCE,lineGeoJson(listOf(demoRoute.first(),demoRoute.first()))))
    if(style.getSource(MARKER_SOURCE)==null) style.addSource(GeoJsonSource(MARKER_SOURCE,pointGeoJson(demoRoute.first())))
    if(style.getSource(HIGHLIGHT_SOURCE)==null) style.addSource(GeoJsonSource(HIGHLIGHT_SOURCE,polygonGeoJson(country.polygon))) else (style.getSource(HIGHLIGHT_SOURCE) as? GeoJsonSource)?.setGeoJson(polygonGeoJson(country.polygon))
    if(style.getSource(TEXT_SOURCE)==null) style.addSource(GeoJsonSource(TEXT_SOURCE,pointGeoJson(country.center)))
    if(style.getLayer(ROUTE_LAYER)==null) style.addLayer(LineLayer(ROUTE_LAYER,ROUTE_SOURCE).withProperties(lineColor(android.graphics.Color.parseColor("#E05A47")),lineWidth(5f),lineCap("round"),lineOpacity(1f)))
    if(style.getLayer(MARKER_LAYER)==null) style.addLayer(CircleLayer(MARKER_LAYER,MARKER_SOURCE).withProperties(circleRadius(7f),circleColor(android.graphics.Color.parseColor("#172A3A")),circleOpacity(1f)))
    if(style.getLayer(HIGHLIGHT_LAYER)==null) style.addLayer(FillLayer(HIGHLIGHT_LAYER,HIGHLIGHT_SOURCE).withProperties(fillColor(android.graphics.Color.parseColor("#E05A47")),fillOpacity(0f)))
    if(style.getLayer(TEXT_LAYER)==null) style.addLayer(SymbolLayer(TEXT_LAYER,TEXT_SOURCE).withProperties(textField(text),textSize(24f),textColor(android.graphics.Color.parseColor("#172A3A")),textHaloColor(android.graphics.Color.WHITE),textHaloWidth(2f)))
    updateTextLayer(style,text,24f,true)
}

private fun updateVisuals(style:Style,p:Float,country:Country,highlight:Boolean,routeVisible:Boolean,markerVisible:Boolean,textVisible:Boolean,routeWidth:Float,text:String,textSize:Float){
    updateRoute(style,p,routeWidth)
    updateHighlight(style,country,highlight,p)
    updateLayerVisibility(style,routeVisible,markerVisible,textVisible)
    updateTextLayer(style,text,textSize,textVisible)
}

private fun updateRoute(style:Style,p:Float,width:Float){
    val x=p.coerceIn(0f,1f).toDouble()*(demoRoute.size-1)
    val seg=x.toInt().coerceAtMost(demoRoute.size-2)
    val current=interpolate(demoRoute[seg],demoRoute[seg+1],x-seg)
    val visible=demoRoute.take(seg+1).toMutableList()
    if(visible.last()!=current) visible.add(current)
    (style.getSource(ROUTE_SOURCE) as? GeoJsonSource)?.setGeoJson(lineGeoJson(if(p==0f) listOf(demoRoute.first()) else visible))
    (style.getSource(MARKER_SOURCE) as? GeoJsonSource)?.setGeoJson(pointGeoJson(current))
    (style.getLayer(ROUTE_LAYER) as? LineLayer)?.setProperties(lineWidth(width))
}

private fun updateHighlight(style:Style,country:Country,enabled:Boolean,p:Float){
    (style.getSource(HIGHLIGHT_SOURCE) as? GeoJsonSource)?.setGeoJson(polygonGeoJson(country.polygon))
    (style.getLayer(HIGHLIGHT_LAYER) as? FillLayer)?.setProperties(fillOpacity(if(enabled)(p*2f).coerceIn(0f,.42f) else 0f))
}

private fun updateLayerVisibility(style:Style,route:Boolean,marker:Boolean,text:Boolean){
    (style.getLayer(ROUTE_LAYER) as? LineLayer)?.setProperties(lineOpacity(if(route)1f else 0f))
    (style.getLayer(MARKER_LAYER) as? CircleLayer)?.setProperties(circleOpacity(if(marker)1f else 0f))
    (style.getLayer(TEXT_LAYER) as? SymbolLayer)?.setProperties(org.maplibre.android.style.layers.PropertyFactory.textOpacity(if(text)1f else 0f))
}

private fun updateTextLayer(style:Style,text:String,size:Float,visible:Boolean){
    val source=style.getSource(TEXT_SOURCE) as? GeoJsonSource ?: return
    source.setGeoJson(pointGeoJson(demoRoute.last()))
    (style.getLayer(TEXT_LAYER) as? SymbolLayer)?.setProperties(
        textField(text.ifBlank { "Daily Flare" }),
        textSize(size),
        textColor(android.graphics.Color.parseColor("#172A3A")),
        textHaloColor(android.graphics.Color.WHITE),
        textHaloWidth(2f),
        org.maplibre.android.style.layers.PropertyFactory.textOpacity(if(visible)1f else 0f)
    )
}

private fun interpolate(a:LatLng,b:LatLng,t:Double)=LatLng(a.latitude+(b.latitude-a.latitude)*t.coerceIn(0.0,1.0),a.longitude+(b.longitude-a.longitude)*t.coerceIn(0.0,1.0))
private fun lineGeoJson(points:List<LatLng>)="{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"LineString\",\"coordinates\":["+points.joinToString(","){"[${it.longitude},${it.latitude}]"}+"]}}"
private fun polygonGeoJson(points:List<LatLng>)="{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[["+points.joinToString(","){"[${it.longitude},${it.latitude}]"}+"]]}}"
private fun pointGeoJson(p:LatLng)="{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Point\",\"coordinates\":[${p.longitude},${p.latitude}]}}"
private fun applyCameraMode(map:MapLibreMap,mode:CameraMode,p:Float){val c=map.cameraPosition;val x=p.coerceIn(0f,1f);val bearing=when(mode){CameraMode.ORBIT->x*360.0;CameraMode.CINEMATIC->-18.0+x*36.0;else->c.bearing};val tilt=when(mode){CameraMode.TOP_DOWN->0.0;CameraMode.BOUNCE->8.0+kotlin.math.sin(x*Math.PI*4)*28;CameraMode.FLY_TO->20.0+x*35;CameraMode.ORBIT->35.0;CameraMode.CINEMATIC->25.0+kotlin.math.sin(x*Math.PI)*25};val zoom=when(mode){CameraMode.TOP_DOWN->c.zoom.coerceAtLeast(2.5);CameraMode.BOUNCE->4.0+kotlin.math.sin(x*Math.PI*4)*.35;CameraMode.FLY_TO->2.5+x*4;CameraMode.ORBIT->4.5;CameraMode.CINEMATIC->3.5+x*2};map.easeCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder(c).zoom(zoom).bearing(bearing).tilt(tilt).build()),180)}
private fun formatTime(ms:Long):String { val s=ms/1000; return "%d:%02d".format(s/60,s%60) }
