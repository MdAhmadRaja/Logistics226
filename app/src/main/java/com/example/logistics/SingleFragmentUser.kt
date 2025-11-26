package com.example.logistics

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

class SingleFragmentUser : Fragment() {

    private lateinit var searchCity: AutoCompleteTextView
    private lateinit var vehicleTypeSpinner: Spinner
    private lateinit var searchButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UserHomeAdapter
    private val db = FirebaseFirestore.getInstance()
    private val user = FirebaseAuth.getInstance().currentUser
    private val logisticList = mutableListOf<Logistic>()
    private val citiesList = mutableListOf<String>()

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_single_user, container, false)

        searchCity = view.findViewById(R.id.searchCity)
        vehicleTypeSpinner = view.findViewById(R.id.vehicleTypeSpinner)
        searchButton = view.findViewById(R.id.searchButton)
        recyclerView = view.findViewById(R.id.recyclerView)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = UserHomeAdapter(logisticList)
        recyclerView.adapter = adapter

        loadCitiesFromJson()
        setupSpinner()

        searchCity.setOnClickListener { searchCity.showDropDown() }

        searchButton.setOnClickListener { searchLogistics() }

        return view
    }

    private fun loadCitiesFromJson() {
        try {
            val inputStream = requireContext().assets.open("cities.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.readText()
            reader.close()

            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val cityObj = jsonArray.getJSONObject(i)
                val city = cityObj.getString("name")
                val state = cityObj.getString("state")
                citiesList.add("$city, $state")
            }

            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, citiesList)
            searchCity.setAdapter(adapter)

        } catch (e: Exception) {
            Log.e("JSON_ERROR", "Error loading cities.json", e)
        }
    }

    private fun setupSpinner() {
        // Added "Choose vehicle type" as the first item (hint)
        val vehicleOptions = listOf("Choose vehicle type", "All", "Truck", "Trailer")
        
        // Use a custom adapter to handle the hint's appearance (optional, but good practice)
        // For now, using the layout resources as requested.
        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            R.layout.spinner_item_selected,
            vehicleOptions
        ) {
            override fun isEnabled(position: Int): Boolean {
                // Disable the first item (hint) so it can't be selected from the dropdown
                return position != 0
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                val tv = view as TextView
                if (position == 0) {
                    // Set the hint text color to gray
                    tv.setTextColor(Color.GRAY)
                } else {
                    tv.setTextColor(Color.WHITE)
                }
                return view
            }
        }
        adapter.setDropDownViewResource(R.layout.spinner_item_dropdown)
        vehicleTypeSpinner.adapter = adapter
    }

    private fun normalizeName(name: String): String {
        return name.replace("\\s+".toRegex(), "").lowercase(Locale.getDefault())
    }

    private fun isFutureOrToday(dateStr: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val logisticDate = sdf.parse(dateStr)
            val currentDate = sdf.parse(sdf.format(Date()))
            logisticDate != null && !logisticDate.before(currentDate)
        } catch (e: Exception) {
            false
        }
    }

    private fun searchLogistics() {
        val selectedCityState = searchCity.text.toString().trim()
        var vehicleType = vehicleTypeSpinner.selectedItem.toString().trim().lowercase()

        // Handle the hint selection
        if (vehicleType == "choose vehicle type") {
            // Treat as "all" or prompt user. treating as "all" for now or handle as default.
            vehicleType = "all" 
        }

        if (selectedCityState.isEmpty()) {
            Toast.makeText(requireContext(), "Please select a city from list", Toast.LENGTH_SHORT).show()
            return
        }

        val parts = selectedCityState.split(",")
        val city = parts[0].trim()
        val state = if (parts.size > 1) parts[1].trim() else ""

        db.collection("logistics")
            .get(Source.SERVER)
            .addOnSuccessListener { documents ->
                logisticList.clear()

                for (doc in documents) {
                    try {
                        val logistic = doc.toObject(Logistic::class.java)?.copy(id = doc.id) ?: continue

                        val srcCity = normalizeName(logistic.sourceCity)
                        val destCity = normalizeName(logistic.destinationCity)
                        val srcState = normalizeName(logistic.sourceState)
                        val destState = normalizeName(logistic.destinationState)
                        val vehicle = logistic.vehicleType.trim().lowercase()
                        val logisticDate = logistic.date

                        val searchMatch = normalizeName(city) in listOf(srcCity, destCity) ||
                                normalizeName(state) in listOf(srcState, destState)

                        val vehicleNormalized = vehicle.lowercase().trim()
                        val vehicleTypeNormalized = vehicleType.lowercase().trim()

                        val matchesVehicle =
                            vehicleTypeNormalized == "all" ||
                                    vehicleNormalized.contains(vehicleTypeNormalized) ||
                                    (vehicleTypeNormalized == "truck" && (vehicleNormalized.contains("tailor") || vehicleNormalized.contains("trailer"))) ||
                                    (vehicleTypeNormalized == "trailer" && (vehicleNormalized.contains("tailor") || vehicleNormalized.contains("truck"))) ||
                                    (vehicleTypeNormalized == "tailor" && (vehicleNormalized.contains("truck") || vehicleNormalized.contains("trailer")))

                        val validDate = isFutureOrToday(logisticDate)

                        if (searchMatch && matchesVehicle && validDate) {
                            logisticList.add(logistic)
                        }

                    } catch (e: Exception) {
                        Log.e("SEARCH_ERROR", "Error parsing logistic", e)
                    }
                }

                adapter.notifyDataSetChanged()

                if (logisticList.isEmpty()) {
                    Toast.makeText(requireContext(), "No loads found for this location", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to fetch logistics", Toast.LENGTH_SHORT).show()
            }
    }

    inner class UserHomeAdapter(private val items: MutableList<Logistic>) :
        RecyclerView.Adapter<UserHomeAdapter.UserHomeViewHolder>() {

        inner class UserHomeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val logisticType: TextView = view.findViewById(R.id.logisticType)
            val vehicleType: TextView = view.findViewById(R.id.vehicleType)
            val tonsPrice: TextView = view.findViewById(R.id.tonsPrice)
            val contact: TextView = view.findViewById(R.id.contact)
            val cityInfo: TextView = view.findViewById(R.id.cityInfo)
            val date: TextView = view.findViewById(R.id.date)
            val bookButton: ImageView = view.findViewById(R.id.buyButton)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserHomeViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.user_home_item, parent, false)
            return UserHomeViewHolder(view)
        }

        override fun onBindViewHolder(holder: UserHomeViewHolder, position: Int) {
            val logistic = items[position]

            holder.logisticType.text = "Type: ${logistic.logisticType}"
            holder.vehicleType.text = "Vehicle: ${logistic.vehicleType}"
            holder.tonsPrice.text =
                "Tons: ${logistic.tons}, ₹${logistic.pricePerTon}/Ton\nTotal: ₹${logistic.totalPrice}"
            holder.cityInfo.text =
                "From: \uD83D\uDD48 ${logistic.sourceCity} (${logistic.sourceState})\nTo: \uD83D\uDD48 ${logistic.destinationCity} (${logistic.destinationState})"
            holder.date.text = "Date: ${logistic.date}"
            holder.contact.text = "Contact: ${logistic.contact}"

            holder.bookButton.setOnClickListener {
                val docId = logistic.id
                val userId = user?.uid ?: ""

                db.collection("user_history")
                    .whereEqualTo("uid", userId)
                    .whereEqualTo("id", docId)
                    .get()
                    .addOnSuccessListener { docs ->
                        if (docs.isEmpty) {
                            val count = (logistic.status?.toIntOrNull() ?: 0) + 1
                            db.collection("logistics").document(docId)
                                .update("status", count.toString())
                                .addOnSuccessListener {
                                    val bookedLogistic = logistic.copy(uid = userId)
                                    db.collection("user_history").add(bookedLogistic)
                                    Toast.makeText(
                                        requireContext(),
                                        "Load booked successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    logistic.status = count.toString()
                                    notifyItemChanged(position)
                                }
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "You already booked this load",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
