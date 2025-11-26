package com.example.logistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class HistoryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private val db = FirebaseFirestore.getInstance()
    private val mAuth = FirebaseAuth.getInstance()
    private val logisticList = mutableListOf<Logistic>()
    private lateinit var adapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)
        recyclerView = view.findViewById(R.id.historyRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = HistoryAdapter(logisticList)
        recyclerView.adapter = adapter
        loadHistory()
        return view
    }

    private fun loadHistory() {
        val uid = mAuth.currentUser?.uid ?: return
        db.collection("logistics")
            .whereEqualTo("uid", uid)
            .get()
            .addOnSuccessListener { documents ->
                logisticList.clear()
                for (doc in documents) {
                    val logistic = doc.toObject(Logistic::class.java).copy(id = doc.id)
                    logisticList.add(logistic)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error loading history", Toast.LENGTH_SHORT).show()
            }
    }

    inner class HistoryAdapter(private val items: List<Logistic>) :
        RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

        inner class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvDate: TextView = view.findViewById(R.id.tvDate)
            val tvLogisticName: TextView = view.findViewById(R.id.tvLogisticName)
            val tvStatus: TextView = view.findViewById(R.id.tvStatus)
            val tvTon: TextView = view.findViewById(R.id.tvTon)
            val tvRate: TextView = view.findViewById(R.id.tvRate)
            val tvSourceCity: TextView = view.findViewById(R.id.tvSourceCity)
            val tvDestinationCity: TextView = view.findViewById(R.id.tvDestinationCity)
            val tvTotal: TextView = view.findViewById(R.id.tvTotal)
            val arrowIcon: ImageView = view.findViewById(R.id.arrowIcon)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.history_item, parent, false)
            return HistoryViewHolder(view)
        }

        override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
            val logistic = items[position]

            // Format date as "20 Oct" from "20/10/2025"
            val formattedDate = try {
                val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
                val dateObj = inputFormat.parse(logistic.date)
                if (dateObj != null) outputFormat.format(dateObj) else "--"
            } catch (e: Exception) {
                "--"
            }

            holder.tvDate.text = formattedDate
            holder.tvLogisticName.text = logistic.logisticType

            // Set status text and color
            val status = logistic.status ?: "Pending"
            holder.tvStatus.text = status
            holder.tvStatus.setTextColor(
                when (status.lowercase(Locale.getDefault())) {
                    "pending" -> 0xFFFFC107.toInt() // Yellow
                    "1", "2", "3" -> 0xFF4CAF50.toInt() // Green
                    else -> 0xFFF44336.toInt() // Red
                }
            )

            holder.tvTon.text = "Ton: ${logistic.tons}"
            holder.tvRate.text = "₹${logistic.pricePerTon} / ton"
            holder.tvTotal.text = "₹${logistic.totalPrice}"
            holder.tvSourceCity.text = "${logistic.sourceCity}, ${logistic.sourceState}"
            holder.tvDestinationCity.text = "${logistic.destinationCity}, ${logistic.destinationState}"

            holder.arrowIcon.setOnClickListener {
                db.collection("logistics").document(logistic.id)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Deleted successfully", Toast.LENGTH_SHORT).show()
                        (logisticList as MutableList).removeAt(position)
                        adapter.notifyItemRemoved(position)
                        adapter.notifyItemRangeChanged(position, logisticList.size)
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to delete", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
