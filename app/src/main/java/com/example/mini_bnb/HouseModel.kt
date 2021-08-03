package com.example.mini_bnb

data class HouseModel(
    val id: Int,
    val title: String,
    val price: Int,
    val lat: Double,
    val lng: Double,
    val imgUrl: String
)
