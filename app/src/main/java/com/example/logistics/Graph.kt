package com.example.logistics

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class Graph : AppCompatActivity() {

    private lateinit var totalLoadsText: TextView
    private lateinit var completedLoadsText: TextView
    private lateinit var totalVisitsText: TextView
    private lateinit var lineChart: LineChart
    private lateinit var spinnerMode: Spinner
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("MM/yyyy", Locale.getDefault())
    private val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graph)

        totalLoadsText = findViewById(R.id.totalLoadsText)
        completedLoadsText = findViewById(R.id.completedLoadsText)
        totalVisitsText = findViewById(R.id.totalVisitsText)
        lineChart = findViewById(R.id.lineChart)
        spinnerMode = findViewById(R.id.spinnerMode)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Custom Adapter for Spinner
        val chartModes = resources.getStringArray(R.array.chart_modes)
        val adapter = ArrayAdapter(this, R.layout.spinner_item_selected, chartModes)
        adapter.setDropDownViewResource(R.layout.spinner_item_dropdown)
        spinnerMode.adapter = adapter

        loadDashboardData()

        spinnerMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long
            ) {
                loadDashboardData()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadDashboardData() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("logistics")
            .whereEqualTo("uid", userId)
            .get()
            .addOnSuccessListener { documents ->
                var totalLoads = 0
                var completedLoads = 0
                var totalVisits = 0

                val dateCountMap = mutableMapOf<String, Int>()
                val selectedMode = spinnerMode.selectedItem.toString().lowercase(Locale.getDefault())

                for (doc in documents) {
                    val logistic = doc.toObject(Logistic::class.java)
                    totalLoads++

                    val status = logistic.status?.trim()?.lowercase() ?: "pending"
                    if (status != "pending") {
                        completedLoads++
                        totalVisits += status.toIntOrNull() ?: 0
                    }

                    val dateStr = logistic.date
                    if (!dateStr.isNullOrEmpty()) {
                        val date = dateFormat.parse(dateStr)
                        val key = when (selectedMode) {
                            "day" -> dateFormat.format(date!!)
                            "month" -> monthFormat.format(date!!)
                            "year" -> yearFormat.format(date!!)
                            else -> dateFormat.format(date!!)
                        }
                        dateCountMap[key] = dateCountMap.getOrDefault(key, 0) + 1
                    }
                }

                // Total Loads
                totalLoadsText.text = "$totalLoads"
                val drawable1 = resources.getDrawable(R.drawable.img_5, null)
                val textSizePx1 = totalLoadsText.textSize.toInt()
                drawable1.setBounds(0, 0, textSizePx1, textSizePx1)
                totalLoadsText.setCompoundDrawables(drawable1, null, null, null)
                totalLoadsText.compoundDrawablePadding = 8

// Completed Loads
                completedLoadsText.text = "$completedLoads"
                val drawable2 = resources.getDrawable(R.drawable.img_6, null)
                val textSizePx2 = completedLoadsText.textSize.toInt()
                drawable2.setBounds(0, 0, textSizePx2, textSizePx2)
                completedLoadsText.setCompoundDrawables(drawable2, null, null, null)
                completedLoadsText.compoundDrawablePadding = 8

// Total Visits
                totalVisitsText.text = "$totalVisits"
                val drawable3 = resources.getDrawable(R.drawable.img_6, null)
                val textSizePx3 = totalVisitsText.textSize.toInt()
                drawable3.setBounds(0, 0, textSizePx3, textSizePx3)
                totalVisitsText.setCompoundDrawables(drawable3, null, null, null)
                totalVisitsText.compoundDrawablePadding = 8


                updateLineChart(dateCountMap)
            }
            .addOnFailureListener { e ->
                Log.e("GraphDebug", "Error fetching documents", e)
            }
    }

    private fun updateLineChart(dateCountMap: Map<String, Int>) {
        if (dateCountMap.isEmpty()) {
            lineChart.clear()
            lineChart.setNoDataText("No data available")
            lineChart.invalidate()
            return
        }

        val entries = mutableListOf<Entry>()
        val labels = mutableListOf<String>()

        var index = 0f
        for ((date, count) in dateCountMap.toSortedMap()) {
            entries.add(Entry(index, count.toFloat()))
            labels.add(date)
            index += 1f
        }

        val dataSet = LineDataSet(entries, "Loads per ${spinnerMode.selectedItem}")
        dataSet.color = Color.parseColor("#0EC7DE")
        dataSet.valueTextColor = Color.BLACK
        dataSet.lineWidth = 2f
        dataSet.setDrawFilled(true)
        dataSet.fillColor = Color.parseColor("#FFF5E1") // cream color
        dataSet.setDrawCircles(true)
        dataSet.circleRadius = 4f
        dataSet.setCircleColor(Color.parseColor("#0EC7DE"))

        val lineData = LineData(dataSet)
        lineChart.data = lineData

        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels)

        lineChart.axisRight.isEnabled = false
        lineChart.description.isEnabled = false
        lineChart.animateY(800)
        lineChart.invalidate()
    }
}
