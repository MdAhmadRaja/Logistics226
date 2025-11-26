package com.example.logistics

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment

class HomeFragmentUser : Fragment() {

    private lateinit var btnAddLoad: LinearLayout
    private lateinit var btnUpload: LinearLayout
    private lateinit var txtFindLoad: TextView
    private lateinit var txtSearch: TextView

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home_user, container, false)

        // Initialize views
        btnAddLoad = view.findViewById(R.id.btnAddLoaduser)
        btnUpload = view.findViewById(R.id.btnUploaduser)
        txtFindLoad = view.findViewById(R.id.txtFindLoad)
        txtSearch = view.findViewById(R.id.txtSearch)

        // Load default fragment
        loadFragment(DoubleFragmentUser())
        setTabSelected(true)

        // On click for Find Load
        btnAddLoad.setOnClickListener {
            loadFragment(DoubleFragmentUser())
            setTabSelected(true)
        }

        // On click for Search
        btnUpload.setOnClickListener {
            loadFragment(SingleFragmentUser())
            setTabSelected(false)
        }

        return view
    }

    private fun loadFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction()
            .replace(R.id.childFragmentContainer, fragment)
            .commit()
    }

    // Set colors + backgrounds depending on selected tab
    private fun setTabSelected(isAddLoadSelected: Boolean) {

        if (isAddLoadSelected) {
            // Active = Add Load
            btnAddLoad.setBackgroundResource(R.drawable.btn_login_background)
            btnUpload.setBackgroundResource(R.drawable.edittext_background)

            txtFindLoad.setTextColor(Color.WHITE)
            txtSearch.setTextColor(Color.BLACK)

        } else {
            // Active = Search
            btnAddLoad.setBackgroundResource(R.drawable.edittext_background)
            btnUpload.setBackgroundResource(R.drawable.btn_login_background)

            txtFindLoad.setTextColor(Color.BLACK)
            txtSearch.setTextColor(Color.WHITE)
        }
    }
}
