package com.example.mini_bnb

import retrofit2.Call
import retrofit2.http.GET

interface HouseService {
    @GET("/v3/9ec922b9-9e19-4cdc-8a73-2a27759aed7a")
    fun getHouseList(): Call<HouseDto>
}