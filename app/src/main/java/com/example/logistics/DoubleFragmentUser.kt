package com.example.logistics

import android.app.DatePickerDialog
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
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
import java.text.SimpleDateFormat
import java.util.*

class DoubleFragmentUser : Fragment() {

    private lateinit var sourceState: AutoCompleteTextView
    private lateinit var destinationState: AutoCompleteTextView
    private lateinit var vehicleTypeSpinner: Spinner
    private lateinit var datePickerText: TextView
    private lateinit var searchButton: Button
    private lateinit var recyclerView: RecyclerView

    private val db = FirebaseFirestore.getInstance()
    private val user = FirebaseAuth.getInstance().currentUser
    private val logisticList = mutableListOf<Logistic>()
    private lateinit var adapter: UserHomeAdapter
    private var selectedDate: String? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_double_user, container, false)

        sourceState = view.findViewById(R.id.sourceu2user)
        destinationState = view.findViewById(R.id.destinationu2user)
        vehicleTypeSpinner = view.findViewById(R.id.vehicleTypeSpinner)
        datePickerText = view.findViewById(R.id.datePickerText2)
        searchButton = view.findViewById(R.id.searchu2)
        recyclerView = view.findViewById(R.id.recycleru2)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = UserHomeAdapter(logisticList)
        recyclerView.adapter = adapter

        setupSpinner()

        val indianStates = listOf(
            "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh",
            "Goa", "Gujarat", "Haryana", "Himachal Pradesh", "Jharkhand", "Karnataka",
            "Kerala", "Madhya Pradesh", "Maharashtra", "Manipur", "Meghalaya", "Mizoram",
            "Nagaland", "Odisha", "Punjab", "Rajasthan", "Sikkim", "Tamil Nadu",
            "Telangana", "Tripura", "Uttar Pradesh", "Uttarakhand", "West Bengal",
            "Andaman and Nicobar Islands", "Chandigarh", "Dadra and Nagar Haveli and Daman and Diu",
            "Delhi", "Jammu and Kashmir", "Ladakh", "Lakshadweep", "Puducherry"
        )
        val stateAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, indianStates)
        sourceState.setAdapter(stateAdapter)
        destinationState.setAdapter(stateAdapter)

        sourceState.setOnClickListener { sourceState.showDropDown() }
        destinationState.setOnClickListener { destinationState.showDropDown() }

        datePickerText.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(requireContext(), { _, y, m, d ->
                val calendarPicked = Calendar.getInstance()
                calendarPicked.set(y, m, d)

                // ✅ Internal format (for Firestore)
                val internalFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                selectedDate = internalFormat.format(calendarPicked.time)

                // ✅ Display format (for user)
                val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                datePickerText.text = "\uD83D\uDDD3\uFE0F ${displayFormat.format(calendarPicked.time)}"

            }, year, month, day)

            // ✅ Restrict past dates
            datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
            datePickerDialog.show()
        }

        searchButton.setOnClickListener { searchLogistics() }

        return view
    }

    private fun setupSpinner() {
        val vehicleOptions = listOf("Choose vehicle type", "All", "Truck", "Trailer")
        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            R.layout.spinner_item_selected,
            vehicleOptions
        ) {
            override fun isEnabled(position: Int): Boolean {
                return position != 0
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                val tv = view as TextView
                if (position == 0) {
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

    private fun normalizeStateName(state: String): String {
        return state.replace("\\s+".toRegex(), "").lowercase(Locale.getDefault())
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
        val sState = normalizeStateName(sourceState.text.toString().trim())
        val dState = normalizeStateName(destinationState.text.toString().trim())
        var vehicleType = vehicleTypeSpinner.selectedItem.toString().trim().lowercase()
        val pickedDate = selectedDate

        if (vehicleType == "choose vehicle type") {
             vehicleType = "all"
        }

        if (sState.isEmpty() || dState.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter both Source and Destination States", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("logistics")
            .get(Source.SERVER)
            .addOnSuccessListener { documents ->
                logisticList.clear()

                for (doc in documents) {
                    try {
                        val logistic = doc.toObject(Logistic::class.java)?.copy(id = doc.id) ?: continue

                        val srcState = normalizeStateName(logistic.sourceState ?: "")
                        val destState = normalizeStateName(logistic.destinationState ?: "")
                        val vehicle = logistic.vehicleType?.trim()?.lowercase() ?: ""
                        val logisticDate = logistic.date ?: ""

                        val matchesSourceDest = srcState == sState && destState == dState
                        val matchesVehicle = (vehicleType == "all" || vehicle == vehicleType)
                        val validDate = isFutureOrToday(logisticDate)

                        var matchesDate = true
                        if (!pickedDate.isNullOrEmpty()) {
                            matchesDate = logisticDate == pickedDate
                        } else {
                            matchesDate = validDate
                        }

                        if (matchesSourceDest && matchesVehicle && matchesDate) {
                            logisticList.add(logistic)
                        }
                    } catch (e: Exception) {
                        Log.e("LOG_SEARCH_ERROR", "Error parsing logistic: ${doc.id}", e)
                    }
                }

                adapter.notifyDataSetChanged()

                if (logisticList.isEmpty()) {
                    Toast.makeText(requireContext(), "No logistics found for given criteria", Toast.LENGTH_SHORT).show()
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
            val view = LayoutInflater.from(parent.context).inflate(R.layout.user_home_item, parent, false)
            return UserHomeViewHolder(view)
        }

        override fun onBindViewHolder(holder: UserHomeViewHolder, position: Int) {
            val logistic = items[position]

            // ✅ Format date display
            val dateDisplay = try {
                val input = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val output = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                output.format(input.parse(logistic.date ?: "")!!)
            } catch (e: Exception) {
                logistic.date ?: "N/A"
            }

            holder.logisticType.text = " ${logistic.logisticType ?: "N/A"}"
            holder.vehicleType.text = " ${logistic.vehicleType ?: "N/A"}"
            holder.tonsPrice.text =
                " ${logistic.tons ?: "N/A"} tons | ₹${logistic.pricePerTon ?: "0"}/Ton\n Total: ₹${logistic.totalPrice ?: "0"}"
            holder.cityInfo.text =
                "📍 From: ${logistic.sourceCity ?: "N/A"} (${logistic.sourceState ?: "N/A"})\n" +
                        "🎯 To: ${logistic.destinationCity ?: "N/A"} (${logistic.destinationState ?: "N/A"})"
            holder.date.text = "🗓️ $dateDisplay"

            // ✅ Green call icon
            val contactText = "☎\uFE0F ${logistic.contact ?: "N/A"}"
            val spannable = SpannableString(contactText)
            spannable.setSpan(
                ForegroundColorSpan(Color.parseColor("#008000")),
                0, 2, // 📞 emoji range
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            holder.contact.text = spannable

            holder.bookButton.setOnClickListener {
                val docId = logistic.id
                val userId = user?.uid ?: ""

                db.collection("user_history")
                    .whereEqualTo("uid", userId)
                    .whereEqualTo("id", docId)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (documents.isEmpty) {
                            val currentCount = (logistic.status?.toIntOrNull() ?: 0) + 1

                            db.collection("logistics").document(docId)
                                .get()
                                .addOnSuccessListener { docSnapshot ->
                                    if (docSnapshot.exists()) {
                                        db.collection("logistics").document(docId)
                                            .update("status", currentCount.toString())
                                            .addOnSuccessListener {
                                                val bookedLogistic = logistic.copy(uid = userId)
                                                db.collection("user_history").add(bookedLogistic)

                                                Toast.makeText(requireContext(), "Load booked successfully", Toast.LENGTH_SHORT).show()
                                                logistic.status = currentCount.toString()
                                                notifyItemChanged(position)
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(requireContext(), "Failed to update load", Toast.LENGTH_SHORT).show()
                                            }
                                    } else {
                                        Toast.makeText(requireContext(), "This load is no longer available", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        } else {
                            Toast.makeText(requireContext(), "You have already booked this load", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
