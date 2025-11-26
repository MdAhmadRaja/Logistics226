package com.example.logistics

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import java.util.*

data class CityInfo(
    val id: String,
    val name: String,
    val state: String
)

class HomeFragment : Fragment() {

    private lateinit var logisticType: EditText
    private lateinit var tonAmount: EditText
    private lateinit var pricePerTon: EditText
    private lateinit var totalPriceText: TextView
    private lateinit var vehicleTypeSpinner: Spinner
    private lateinit var contactNumber: EditText
    private lateinit var sourceCity: AutoCompleteTextView
    private lateinit var sourceState: EditText
    private lateinit var destinationCity: AutoCompleteTextView
    private lateinit var destinationState: EditText
    private lateinit var dateField: EditText
    private lateinit var submitButton: Button

    private val db = FirebaseFirestore.getInstance()
    private val mAuth = FirebaseAuth.getInstance()
    private lateinit var cityList: List<CityInfo>

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        logisticType = view.findViewById(R.id.logisticType)
        tonAmount = view.findViewById(R.id.tonAmount)
        pricePerTon = view.findViewById(R.id.pricePerTon)
        totalPriceText = view.findViewById(R.id.totalPrice)
        vehicleTypeSpinner = view.findViewById(R.id.vehicleTypeSpinner)
        contactNumber = view.findViewById(R.id.contactNumber)
        sourceCity = view.findViewById(R.id.sourceCity)
        sourceState = view.findViewById(R.id.sourceState)
        destinationCity = view.findViewById(R.id.destinationCity)
        destinationState = view.findViewById(R.id.destinationState)
        dateField = view.findViewById(R.id.dateField)
        submitButton = view.findViewById(R.id.submitButton)

        cityList = loadCitiesFromJson()
        setupAutoCompleteCity(sourceCity, sourceState)
        setupAutoCompleteCity(destinationCity, destinationState)

        setupSpinner()

        dateField.setOnClickListener { openDatePicker() }

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateTotalPrice()
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        tonAmount.addTextChangedListener(watcher)
        pricePerTon.addTextChangedListener(watcher)

        submitButton.setOnClickListener { submitLogistic() }

        return view
    }

    private fun loadCitiesFromJson(): List<CityInfo> {
        val cityList = mutableListOf<CityInfo>()
        try {
            val inputStream = requireContext().assets.open("cities.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            val jsonStr = String(buffer, Charsets.UTF_8)
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                cityList.add(
                    CityInfo(
                        obj.getString("id"),
                        obj.getString("name"),
                        obj.getString("state")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return cityList
    }

    private fun setupAutoCompleteCity(cityField: AutoCompleteTextView, stateField: EditText) {
        val cityNames = cityList.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, cityNames)
        cityField.setAdapter(adapter)
        cityField.threshold = 1

        cityField.setOnItemClickListener { parent, _, position, _ ->
            val selectedCity = parent.getItemAtPosition(position).toString()
            val cityInfo = cityList.find { it.name.equals(selectedCity, ignoreCase = true) }
            cityInfo?.let { stateField.setText(it.state) }
        }
    }

    private fun setupSpinner() {
        val vehicleTypes = listOf("Choose vehicle type", "Truck", "Trailer", "All")
        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            R.layout.spinner_item_selected,
            vehicleTypes
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

    private fun openDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(requireContext(), { _, y, m, d ->
            dateField.setText("$d/${m + 1}/$y")
        }, year, month, day)

        datePicker.datePicker.minDate = calendar.timeInMillis
        datePicker.show()
    }

    private fun updateTotalPrice() {
        val tons = tonAmount.text.toString().toDoubleOrNull() ?: 0.0
        val price = pricePerTon.text.toString().toDoubleOrNull() ?: 0.0
        val total = tons * price
        totalPriceText.text = "Total Price: ₹$total"
    }

    private fun submitLogistic() {
        val user = mAuth.currentUser ?: return
        val tons = tonAmount.text.toString().toDoubleOrNull() ?: 0.0
        val price = pricePerTon.text.toString().toDoubleOrNull() ?: 0.0
        val total = tons * price

        val contact = contactNumber.text.toString().trim()
        val dateSelected = dateField.text.toString().trim()

        if (logisticType.text.isEmpty() || tons <= 0 || price <= 0 ||
            contact.length != 10 || sourceCity.text.isEmpty() || sourceState.text.isEmpty() ||
            destinationCity.text.isEmpty() || destinationState.text.isEmpty() || dateSelected.isEmpty()
        ) {
            if (contact.length != 10) {
                Toast.makeText(requireContext(), "Contact number must be exactly 10 digits", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
            return
        }

        var selectedVehicleType = vehicleTypeSpinner.selectedItem.toString().trim().lowercase()
        
        if (selectedVehicleType == "choose vehicle type") {
             Toast.makeText(requireContext(), "Please select a vehicle type", Toast.LENGTH_SHORT).show()
             return
        }

        val finalVehicleType = if (selectedVehicleType == "all") {
            "all,truck,tailor,trailer"
        } else {
            selectedVehicleType
        }

        val data = hashMapOf(
            "uid" to user.uid,
            "logisticType" to logisticType.text.toString(),
            "vehicleType" to finalVehicleType,
            "tons" to tons,
            "pricePerTon" to price,
            "totalPrice" to total,
            "contact" to contact,
            "sourceCity" to sourceCity.text.toString(),
            "sourceState" to sourceState.text.toString(),
            "destinationCity" to destinationCity.text.toString(),
            "destinationState" to destinationState.text.toString(),
            "date" to dateSelected,
            "status" to "Pending"
        )

        db.collection("logistics").add(data)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Logistic added successfully!", Toast.LENGTH_SHORT).show()
                clearFields()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearFields() {
        logisticType.text.clear()
        tonAmount.text.clear()
        pricePerTon.text.clear()
        contactNumber.text.clear()
        sourceCity.text.clear()
        sourceState.text.clear()
        destinationCity.text.clear()
        destinationState.text.clear()
        dateField.text.clear()
        totalPriceText.text = "Total Price: ₹0.0"
        vehicleTypeSpinner.setSelection(0)
    }
}
