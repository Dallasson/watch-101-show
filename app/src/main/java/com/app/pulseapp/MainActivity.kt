package com.app.pulseapp

import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import java.io.Serializable


class MainActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Load Configuration
        Configuration.getInstance().load(this, getSharedPreferences("osm_pref", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_main)

        map = findViewById(R.id.map)

        // 2. Setup Map Visuals
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        // --- FIX: Prevents the "Repeating World" visual glitch ---
        map.isVerticalMapRepetitionEnabled = false
        map.isHorizontalMapRepetitionEnabled = false

        // Set a default zoom so it's not empty while loading
        map.controller.setZoom(5.0)

        loadData()
    }

    // 3. Lifecycle Methods (Required for osmdroid)
    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    private fun loadData() {
        // Retrieve the ArrayList using the correct key
        val waypoints = intent.getSerializableExtra("selected_locations") as? ArrayList<LocationPoint>

        if (waypoints.isNullOrEmpty()) {
            Log.d(TAG, "No location data received!")
            return
        }

        // Debugging logs
        waypoints.forEachIndexed { index, point ->
            Log.d(TAG, "Point[$index] Type=${point.networkType}")
        }

        // Draw the route
        drawColoredRoutes(waypoints)

        // --- FIX: Zoom Logic ---
        // We use map.post {} to wait until the view is fully drawn.
        // If we don't do this, zoomToBoundingBox will fail or do nothing.
        map.post {
            val geoPoints = waypoints.map { GeoPoint(it.lat, it.lon) }

            if (geoPoints.isNotEmpty()) {
                // Center roughly on the first point
                map.controller.setCenter(geoPoints.first())
            }

            if (geoPoints.size > 1) {
                val box = BoundingBox.fromGeoPoints(geoPoints)
                // zoomToBoundingBox(box, animate, borderPaddingInPixels)
                map.zoomToBoundingBox(box, true, 100)
            }
        }
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
                // Use outlinePaint for newer osmdroid versions
                outlinePaint.color = getColorForNetwork(start.networkType)
                outlinePaint.strokeWidth = 10f
                outlinePaint.style = Paint.Style.STROKE
                outlinePaint.strokeCap = Paint.Cap.ROUND // Makes the line ends round/smoother
                isGeodesic = true
            }

            map.overlays.add(polyline)
        }

        map.invalidate() // Refresh map to show lines
    }

    private fun getColorForNetwork(type: String): Int {
        return when (type.lowercase()) {
            "5g" -> Color.GREEN
            "4g", "lte" -> Color.YELLOW
            "3g" -> Color.BLUE
            "2g", "edge" -> Color.RED
            "wifi" -> Color.MAGENTA
            else -> Color.GRAY
        }
    }
}