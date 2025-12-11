package com.app.pulseapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class LocationListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val allLocations = mutableListOf<LocationPoint>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_list)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        fetchAllLocations()
    }

    private fun fetchAllLocations() {
        val database = FirebaseDatabase.getInstance().getReference("pulse_data")

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allLocations.clear()

                for (deviceSnapshot in snapshot.children) {
                    for (pointSnapshot in deviceSnapshot.children) {

                        val lat = pointSnapshot.child("latitude").getValue(Double::class.java)
                        val lon = pointSnapshot.child("longitude").getValue(Double::class.java)
                        val network = pointSnapshot.child("networkType").getValue(String::class.java) ?: "unknown"
                        val timestamp = pointSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L

                        if (lat != null && lon != null) {
                            allLocations.add(
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

                recyclerView.adapter = LocationAdapter(allLocations)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    inner class LocationAdapter(private val items: List<LocationPoint>) :
        RecyclerView.Adapter<LocationAdapter.LocationViewHolder>() {

        inner class LocationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textView: TextView = view.findViewById(R.id.textView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_location, parent, false)
            return LocationViewHolder(view)
        }

        override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
            val loc = items[position]

            holder.textView.text = "Lat: ${loc.lat}, Lon: ${loc.lon}\nNet: ${loc.networkType}\nTime: ${loc.timestamp}"
        }

        override fun getItemCount(): Int = items.size
    }

    fun onNextClicked(view: View) {
        Log.d("LocationListActivity", "Sending ALL ${allLocations.size} locations")

        val intent = Intent(this, MainActivity::class.java)
        intent.putParcelableArrayListExtra("selected_locations", ArrayList(allLocations))
        startActivity(intent)
    }
}
