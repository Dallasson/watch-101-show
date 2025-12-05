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

class MainActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load OSM Configuration
        Configuration.getInstance().load(this, getSharedPreferences("osm_pref", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_main)

        map = findViewById(R.id.map)

        // Setup Map
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.isVerticalMapRepetitionEnabled = false
        map.isHorizontalMapRepetitionEnabled = false
        map.controller.setZoom(5.0)

        // Load selected points from Intent
        loadData()
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    private fun loadData() {
        val waypoints = intent.getParcelableArrayListExtra<LocationPoint>("selected_locations")

        if (waypoints.isNullOrEmpty()) {
            Log.d(TAG, "No location data received in Intent!")
            return
        }

        // PRINT ALL VALUES
        waypoints.forEachIndexed { index, p ->
            Log.d(TAG, "Point[$index] lat=${p.lat}, lon=${p.lon}, net=${p.networkType}, time=${p.timestamp}")
        }

        // Draw routes
        drawColoredRoutes(waypoints)

        // Auto zoom + center
        map.post {
            val geoPoints = waypoints.map { GeoPoint(it.lat, it.lon) }
            if (geoPoints.isNotEmpty()) map.controller.setCenter(geoPoints.first())
            if (geoPoints.size > 1) {
                val box = BoundingBox.fromGeoPoints(geoPoints)
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
                setPoints(
                    listOf(
                        GeoPoint(start.lat, start.lon),
                        GeoPoint(end.lat, end.lon)
                    )
                )

                // Color based on END point network
                outlinePaint.color = getColorForNetwork(end.networkType)
                outlinePaint.strokeWidth = 10f
                outlinePaint.style = Paint.Style.STROKE
                outlinePaint.strokeCap = Paint.Cap.ROUND
                isGeodesic = true
            }

            map.overlays.add(polyline)
        }

        map.invalidate()
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
