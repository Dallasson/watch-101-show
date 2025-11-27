package com.app.pulseapp

import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class SecondActivity : AppCompatActivity() {

    private lateinit var map: MapView

    data class LocationPoint(
        val lat: Double,
        val lon: Double,
        val networkType: String = "unknown",
        val timestamp: Long = 0
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. IMPORTANT: Set User Agent to prevent tile download blocks
        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().load(this, getSharedPreferences("osm_prefs", MODE_PRIVATE))

        setContentView(R.layout.activity_main)

        map = findViewById(R.id.map)

        // 2. Initialize the Tile Source (Standard Map)
        // This fixes the "repeating blank world" issue
        map.setTileSource(TileSourceFactory.MAPNIK)

        // 3. Enable touch controls and prevent vertical repetition
        map.setMultiTouchControls(true)
        map.isVerticalMapRepetitionEnabled = false

        // 4. Set a default start view (Zoom 5 is decent for country-level view)
        // This ensures the map doesn't look broken while waiting for Firebase
        val mapController = map.controller
        mapController.setZoom(5.0)
        mapController.setCenter(GeoPoint(0.0, 0.0)) // Starts at 0,0 until data loads

        fetchAllLocations()
    }

    // 5. LIFECYCLE METHODS: Essential for osmdroid to function and save memory
    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    private fun fetchAllLocations() {
        val database = FirebaseDatabase.getInstance()
            .getReference("pulse_data")

        database.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                // Clear old markers/lines to prevent duplication on updates
                map.overlays.clear()

                val collected = mutableListOf<LocationPoint>()

                for (deviceSnapshot in snapshot.children) {
                    for (pointSnapshot in deviceSnapshot.children) {

                        val lat = pointSnapshot.child("latitude").getValue(Double::class.java)
                        val lon = pointSnapshot.child("longitude").getValue(Double::class.java)
                        val network = pointSnapshot.child("networkType").getValue(String::class.java) ?: "unknown"
                        val timestamp = pointSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L

                        if (lat != null && lon != null) {
                            collected.add(
                                LocationPoint(
                                    lat = lat,
                                    lon = lon,
                                    networkType = network,
                                    timestamp = timestamp
                                )
                            )
                        }
                    }
                }

                // Sort by time to draw the path in correct order
                collected.sortBy { it.timestamp }

                // Add Markers
                collected.forEach {
                    addMarker(
                        GeoPoint(it.lat, it.lon),
                        "Network: ${it.networkType}",
                        "Time: ${it.timestamp}"
                    )
                }

                // Draw Lines
                drawColoredRoutes(collected)

                // 6. ZOOM FIX: Use 'map.post' to ensure the map view size is ready before zooming
                if (collected.isNotEmpty()) {
                    map.post {
                        val box = BoundingBox.fromGeoPoints(
                            collected.map { GeoPoint(it.lat, it.lon) }
                        )
                        // The last parameter is the "padding" in pixels (e.g., 100px)
                        map.zoomToBoundingBox(box, true, 100)
                    }
                }

                map.invalidate()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error fetching data", error.toException())
            }
        })
    }

    private fun drawColoredRoutes(points: List<LocationPoint>) {
        if (points.size < 2) return

        for (i in 0 until points.size - 1) {
            val start = points[i]
            val end = points[i + 1]

            val polyline = Polyline().apply {
                setPoints(listOf(
                    GeoPoint(start.lat, start.lon),
                    GeoPoint(end.lat, end.lon)
                ))
                outlinePaint.color = getColorForNetwork(start.networkType)
                outlinePaint.strokeWidth = 8f
                // Style.STROKE is default for Polyline, but good to be explicit if using custom Paint
                outlinePaint.style = Paint.Style.STROKE
                // Optimize drawing
                isGeodesic = true
            }

            map.overlays.add(polyline)
        }
    }

    private fun getColorForNetwork(type: String): Int {
        return when (type.lowercase()) {
            "5g" -> Color.GREEN
            "lte", "4g" -> Color.YELLOW
            "3g" -> Color.BLUE
            "2g", "edge" -> Color.RED
            "wifi" -> Color.MAGENTA
            else -> Color.GRAY
        }
    }

    private fun addMarker(position: GeoPoint, title: String, desc: String?) {
        val marker = Marker(map)
        marker.position = position
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = title
        marker.subDescription = desc

        // Optional: Make sure markers close their info windows when tapping elsewhere
        marker.isInfoWindowShown // just to access property if needed in future logic

        map.overlays.add(marker)
    }
}