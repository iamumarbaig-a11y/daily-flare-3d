package com.thedailyflare.dailyflare3d

import org.maplibre.android.geometry.LatLng

data class MapPlace(val name: String, val type: String, val location: LatLng)

val commonMapPlaces = listOf(
    MapPlace("Amsterdam", "City", LatLng(52.3676, 4.9041)),
    MapPlace("Berlin", "City", LatLng(52.5200, 13.4050)),
    MapPlace("Warsaw", "City", LatLng(52.2297, 21.0122)),
    MapPlace("Kyiv", "City", LatLng(50.4501, 30.5234)),
    MapPlace("Paris", "City", LatLng(48.8566, 2.3522)),
    MapPlace("London", "City", LatLng(51.5074, -0.1278)),
    MapPlace("Rome", "City", LatLng(41.9028, 12.4964)),
    MapPlace("Madrid", "City", LatLng(40.4168, -3.7038)),
    MapPlace("New York", "City", LatLng(40.7128, -74.0060)),
    MapPlace("Washington", "City", LatLng(38.9072, -77.0369)),
    MapPlace("Tokyo", "City", LatLng(35.6762, 139.6503)),
    MapPlace("Dubai", "City", LatLng(25.2048, 55.2708)),
    MapPlace("Istanbul", "City", LatLng(41.0082, 28.9784)),
    MapPlace("Cairo", "City", LatLng(30.0444, 31.2357)),
    MapPlace("Islamabad", "City", LatLng(33.6844, 73.0479)),
    MapPlace("Lahore", "City", LatLng(31.5204, 74.3587)),
    MapPlace("Gujranwala", "City", LatLng(32.1877, 74.1945)),
    MapPlace("Eiffel Tower", "Landmark", LatLng(48.8584, 2.2945)),
    MapPlace("Big Ben", "Landmark", LatLng(51.5007, -0.1246)),
    MapPlace("Burj Khalifa", "Landmark", LatLng(25.1972, 55.2744))
)

fun searchMapPlaces(query: String): List<MapPlace> =
    commonMapPlaces.filter { it.name.contains(query.trim(), ignoreCase = true) }.take(6)
