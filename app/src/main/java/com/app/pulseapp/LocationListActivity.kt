package com.app.pulseapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class LocationListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val allLocations = mutableListOf<LocationPoint>()
    private val selectedLocations = mutableListOf<LocationPoint>()

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
                        val network =
                            pointSnapshot.child("networkType").getValue(String::class.java) ?: "unknown"
                        val timestamp =
                            pointSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L

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
            val checkBox: CheckBox = view.findViewById(R.id.checkBox)
            val textView: TextView = view.findViewById(R.id.textView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_location, parent, false)
            return LocationViewHolder(view)
        }

        override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
            val loc = items[position]

            holder.textView.text =
                "Lat: ${loc.lat}, Lon: ${loc.lon}\nNet: ${loc.networkType}\nTime: ${loc.timestamp}"

            // Fix recycling
            holder.checkBox.setOnCheckedChangeListener(null)
            holder.checkBox.isChecked = selectedLocations.contains(loc)

            holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedLocations.add(loc)
                else selectedLocations.remove(loc)
            }

            holder.itemView.setOnClickListener {
                holder.checkBox.isChecked = !holder.checkBox.isChecked
            }
        }

        override fun getItemCount(): Int = items.size
    }

    fun onNextClicked(view: View) {
        if (selectedLocations.size < 2) {
            Toast.makeText(this, "Please select at least 2 locations to proceed", Toast.LENGTH_SHORT).show()
            return
        }

        // Log values before sending
        selectedLocations.forEachIndexed { index, loc ->
            Log.d(
                "LocationListActivity",
                "Sending[$index] lat=${loc.lat}, lon=${loc.lon}, net=${loc.networkType}, time=${loc.timestamp}"
            )
        }

        // Convert to ArrayList because intent requires Serializable collection type
        val listToSend = ArrayList(selectedLocations)

        val intent = Intent(this, MainActivity::class.java)
        intent.putParcelableArrayListExtra("selected_locations", listToSend)
        startActivity(intent)
    }

}
