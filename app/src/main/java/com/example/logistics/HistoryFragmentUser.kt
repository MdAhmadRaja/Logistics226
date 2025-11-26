package com.example.logistics

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
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

class HistoryFragmentUser : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val user = FirebaseAuth.getInstance().currentUser
    private val historyList = mutableListOf<Logistic>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HistoryAdapter
    private val uniqueKeys = mutableSetOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history_user, container, false)
        recyclerView = view.findViewById(R.id.recyclerViewHistory)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = HistoryAdapter(historyList)
        recyclerView.adapter = adapter
        loadHistory()
        return view
    }

    private fun loadHistory() {
        db.collection("user_history")
            .whereEqualTo("uid", user?.uid)
            .get()
            .addOnSuccessListener { documents ->
                historyList.clear()
                uniqueKeys.clear()

                for (doc in documents) {
                    val logistic = doc.toObject(Logistic::class.java)
                    val key =
                        "${logistic.logisticType}_${logistic.vehicleType}_${logistic.tons}_${logistic.pricePerTon}"

                    if (!uniqueKeys.contains(key)) {
                        uniqueKeys.add(key)
                        historyList.add(logistic)
                    } else {
                        db.collection("user_history").document(doc.id).delete()
                    }
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                Toast.makeText(requireContext(), "Failed to load history", Toast.LENGTH_SHORT).show()
            }
    }

    inner class HistoryAdapter(private val items: List<Logistic>) :
        RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

        inner class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val date: TextView = view.findViewById(R.id.tvDate)
            val logisticName: TextView = view.findViewById(R.id.tvLogisticName)
            val sourceCity: TextView = view.findViewById(R.id.tvSourceCity)
            val destinationCity: TextView = view.findViewById(R.id.tvDestinationCity)
            val ton: TextView = view.findViewById(R.id.tvTon)
            val rate: TextView = view.findViewById(R.id.tvRate)
            val total: TextView = view.findViewById(R.id.tvTotal)
            val imgIcon: ImageView = view.findViewById(R.id.imgIcon)
            val callIcon: ImageView? = view.findViewById(R.id.ivCall)
            val deleteButton: ImageButton? = view.findViewById(R.id.deleteButton)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.history_user_item, parent, false)
            return HistoryViewHolder(view)
        }

        override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
            val logistic = items[position]

            // Display date in "20 Oct" format
            holder.date.text = formatDate(logistic.date)

            holder.logisticName.text = "   ${logistic.logisticType}"
            holder.sourceCity.text = logistic.sourceCity
            holder.destinationCity.text = logistic.destinationCity

            val tonsVal = logistic.tons
            val priceVal = logistic.pricePerTon
            val total = tonsVal * priceVal

            holder.ton.text = "Ton: $tonsVal"
            holder.rate.text = "₹/T: ₹$priceVal"
            holder.total.text = "₹$total"

            // Delete button
            holder.deleteButton?.setOnClickListener {
                db.collection("user_history").document(logistic.id)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to delete", Toast.LENGTH_SHORT).show()
                    }
            }

            // Open MapActivity
            fun openMapActivity() {
                if (logistic.sourceCity.isNotEmpty() && logistic.destinationCity.isNotEmpty()) {
                    val intent = Intent(requireContext(), MapActivity::class.java)
                    intent.putExtra("sourceCity", logistic.sourceCity)
                    intent.putExtra("destinationCity", logistic.destinationCity)
                    startActivity(intent)
                } else {
                    Toast.makeText(requireContext(), "Source or Destination missing", Toast.LENGTH_SHORT).show()
                }
            }

            holder.itemView.setOnClickListener { openMapActivity() }
            holder.imgIcon.setOnClickListener { openMapActivity() }

            // Click → Call icon
            holder.callIcon?.setOnClickListener {
                if (logistic.contact.isNotEmpty()) {
                    val callIntent = Intent(Intent.ACTION_DIAL)
                    callIntent.data = Uri.parse("tel:${logistic.contact}")
                    startActivity(callIntent)
                } else {
                    Toast.makeText(requireContext(), "Contact number missing", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun getItemCount(): Int = items.size

        private fun formatDate(dateStr: String?): String {
            if (dateStr.isNullOrEmpty()) return "--"
            return try {
                val parser = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val date = parser.parse(dateStr)
                val formatter = SimpleDateFormat("dd MMM", Locale.getDefault())
                formatter.format(date!!)
            } catch (e: Exception) {
                "--"
            }
        }
    }
}
