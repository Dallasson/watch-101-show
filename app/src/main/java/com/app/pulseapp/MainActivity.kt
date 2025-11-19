package com.app.pulseapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import org.osmdroid.config.Configuration
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MainActivity : AppCompatActivity() {

    private lateinit var map: MapView

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

                val geoPoints = mutableListOf<GeoPoint>()

                for (deviceSnapshot in snapshot.children) {

                    val lat = deviceSnapshot.child("latitude").getValue(Double::class.java)
                    val lon = deviceSnapshot.child("longitude").getValue(Double::class.java)
                    val network = deviceSnapshot.child("networkType").getValue(String::class.java)
                    val id = deviceSnapshot.key ?: "Unknown Device"

                    if (lat != null && lon != null) {
                        val point = GeoPoint(lat, lon)
                        geoPoints.add(point)
                        addMarker(point, "ID: $id", "Network: $network")
                    }
                }

                // Autofit the map to all points
                if (geoPoints.isNotEmpty()) {
                    val box = BoundingBox.fromGeoPoints(geoPoints)
                    map.zoomToBoundingBox(box, true)
                }

                map.invalidate()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun addMarker(position: GeoPoint, title: String, desc: String?) {
        val marker = Marker(map)
        marker.position = position
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = title
        marker.subDescription = desc
        marker.showInfoWindow()

        map.overlays.add(marker)
    }
}
