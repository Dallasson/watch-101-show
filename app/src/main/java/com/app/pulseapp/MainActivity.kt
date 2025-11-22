package com.app.pulseapp

import android.graphics.Paint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import org.osmdroid.config.Configuration
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class MainActivity : AppCompatActivity() {

    private lateinit var map: MapView

    data class LocationPoint(
        val lat: Double,
        val lon: Double,
        val networkType: String = "unknown",
        val timestamp: Long = 0
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(this, getSharedPreferences("osm_prefs", MODE_PRIVATE))

        setContentView(R.layout.activity_main)

        map = findViewById(R.id.map)
        map.setMultiTouchControls(true)

        fetchAllLocations()
    }

    private fun fetchAllLocations() {
        val database = FirebaseDatabase.getInstance()
            .getReference("pulse_data")

        database.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                map.overlays.clear()

                val collected = mutableListOf<LocationPoint>()

                for (deviceSnapshot in snapshot.children) {  // DEVICE ID level
                    for (pointSnapshot in deviceSnapshot.children) {  // pushed location nodes

                        val lat = pointSnapshot.child("latitude").getValue(Double::class.java)
                        val lon = pointSnapshot.child("longitude").getValue(Double::class.java)
                        val network = pointSnapshot.child("networkType").getValue(String::class.java) ?: "unknown"
                        val timestamp = pointSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L

                        Log.d("Coordinates","Longitude : " + lat)
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


                collected.sortBy { it.timestamp }

                collected.forEach {
                    addMarker(
                        GeoPoint(it.lat, it.lon),
                        "Network: ${it.networkType}",
                        "Time: ${it.timestamp}"
                    )
                }

                drawColoredRoutes(collected)

                if (collected.isNotEmpty()) {
                    val box = BoundingBox.fromGeoPoints(
                        collected.map { GeoPoint(it.lat, it.lon) }
                    )
                    map.zoomToBoundingBox(box, true)
                }

                map.invalidate()
            }

            override fun onCancelled(error: DatabaseError) {}
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
                outlinePaint.style = Paint.Style.STROKE
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

        map.overlays.add(marker)
    }
}
