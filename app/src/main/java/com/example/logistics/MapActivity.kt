package com.example.logistics

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.net.URLEncoder

class MapActivity : AppCompatActivity() {

    private lateinit var mapWebView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        mapWebView = findViewById(R.id.mapWebView)
        mapWebView.settings.javaScriptEnabled = true
        mapWebView.webViewClient = WebViewClient()

        val sourceCity = intent.getStringExtra("sourceCity") ?: ""
        val destinationCity = intent.getStringExtra("destinationCity") ?: ""

        if (sourceCity.isNotEmpty() && destinationCity.isNotEmpty()) {
            showMap(sourceCity, destinationCity)
        } else {
            Toast.makeText(this, "Source or Destination missing", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showMap(source: String, destination: String) {
        try {
            // Encode cities for URL
            val encodedSource = URLEncoder.encode(source, "UTF-8")
            val encodedDestination = URLEncoder.encode(destination, "UTF-8")

            // OpenStreetMap directions URL using FOSSGIS engine (free)
            val mapUrl = "https://www.openstreetmap.org/directions?engine=fossgis_osrm_car&route=$encodedSource;$encodedDestination"

            mapWebView.loadUrl(mapUrl)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to load map", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        if (mapWebView.canGoBack()) {
            mapWebView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
