package com.example.logistics
data class Logistic(
    val id: String = "",
    val uid: String = "", // Add this
    val logisticType: String = "",
    val vehicleType: String = "",
    val tons: Double = 0.0,
    val pricePerTon: Double = 0.0,
    val totalPrice: Double = 0.0,
    val contact: String = "",
    val sourceCity: String = "",
    val sourceState: String = "",
    val destinationCity: String = "",
    val destinationState: String = "",
    val date: String = "",
    var status: String? = "0"
)

