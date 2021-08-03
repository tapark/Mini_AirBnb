package com.example.mini_bnb

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.*
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.Overlay
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.util.FusedLocationSource
import com.naver.maps.map.widget.LocationButtonView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.DecimalFormat

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity(), OnMapReadyCallback, Overlay.OnClickListener {

    private val mapView: MapView by lazy {
        findViewById<MapView>(R.id.mapView)
    }

    private val viewPager: ViewPager2 by lazy {
        findViewById<ViewPager2>(R.id.houseViewPager)
    }

    private val recyclerView: RecyclerView by lazy {
        findViewById<RecyclerView>(R.id.houseListRecyclerView)
    }

    private val currentLocationButton: LocationButtonView by lazy {
        findViewById<LocationButtonView>(R.id.currentLocationButton)
    }

    private val bottomSheetTitleTextView: TextView by lazy {
        findViewById<TextView>(R.id.bottomSheetTitleTextView)
    }

    private lateinit var viewPagerAdapter: HouseViewPagerAdapter
    private lateinit var recyclerViewAdapter: HouseListAdapter

    private lateinit var naverMap: NaverMap
    private lateinit var locationSource: FusedLocationSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mapView.onCreate(savedInstanceState)

        initSystemFullScreenLightTheme()

        mapView.getMapAsync(this)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerViewAdapter = HouseListAdapter {
            itemClickToShare(it)
        }
        recyclerView.adapter = recyclerViewAdapter

        viewPagerAdapter = HouseViewPagerAdapter {
            itemClickToShare(it)
        }
        viewPager.adapter = viewPagerAdapter
        //ViewPager 는 layoutManager = LinearLayoutManager(this) 필요없다

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                val currentHouseModel = viewPagerAdapter.currentList[position]
                val cameraUpdate = CameraUpdate.scrollTo(LatLng(currentHouseModel.lat, currentHouseModel.lng))
                    .animate(CameraAnimation.Easing)

                naverMap.moveCamera(cameraUpdate)
            }
        })
    }

    private fun itemClickToShare(houseModel: HouseModel) {
        val decimalFormat = DecimalFormat("#,###")
        val price = decimalFormat.format(houseModel.price)

        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "[숙소 정보]\n${houseModel.title}\n${price}원\nURL : ${houseModel.imgUrl}")
            type = "text/plain"
        }
        startActivity(Intent.createChooser(intent, null))
    }

    private fun initSystemFullScreenLightTheme() {
        this.window?.apply {
            this.statusBarColor = getColor(R.color.alpha_black)
            decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        }
    }

    override fun onMapReady(p0: NaverMap) {
        naverMap = p0

        naverMap.maxZoom = 18.0
        naverMap.minZoom = 10.0

        val cameraUpdate = CameraUpdate.scrollTo(LatLng(37.498095, 127.027610))
        naverMap.moveCamera(cameraUpdate)

        val uiSetting = naverMap.uiSettings
        uiSetting.isLocationButtonEnabled = false

        currentLocationButton.map = naverMap

        locationSource = FusedLocationSource(this@MainActivity, LOCATION_PERMISSION_REQUEST_CODE)
        naverMap.locationSource = locationSource

        getHouseListFromAPI()
    }

    private fun getHouseListFromAPI() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://run.mocky.io")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(HouseService::class.java).also {
            it.getHouseList()
                .enqueue(object: Callback<HouseDto> {
                    override fun onResponse(call: Call<HouseDto>, response: Response<HouseDto>) {
                        if (response.isSuccessful.not()) {
                            //실패처리
                            return
                        }
                        response.body()?.let { houseDto ->
                            // 마커 표시
                            houseDto.items.forEach { houseModel ->
                                setNaverMapMarker(houseModel)
                            }
                            // ViewPager, RecyclerView Adapter에 데이터(리스트) 입력
                            viewPagerAdapter.submitList(houseDto.items)
                            recyclerViewAdapter.submitList(houseDto.items)

                            bottomSheetTitleTextView.text = "모든 숙소 보기(${houseDto.items.size})"
                        }
                    }

                override fun onFailure(call: Call<HouseDto>, t: Throwable) {
                    //실패처리
                }

            })
        }
    }

    private fun setNaverMapMarker(houseModel: HouseModel) {
        val marker = Marker()
        marker.position = LatLng(houseModel.lat, houseModel.lng)
        marker.tag = houseModel.id
        marker.icon = OverlayImage.fromResource(R.drawable.black_marker)
        marker.iconTintColor = Color.BLACK
        marker.width = 150
        marker.height = 150
        marker.alpha = 0.7f
        marker.onClickListener = this
        marker.map = naverMap
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return
        }

        if (locationSource.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            if (!locationSource.isActivated) {
                naverMap.locationTrackingMode = LocationTrackingMode.None
            }
        }

    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onClick(overlay: Overlay): Boolean {

        val selectedModel = viewPagerAdapter.currentList.firstOrNull {
            it.id == overlay.tag
        }
        selectedModel?.let {
            val position = viewPagerAdapter.currentList.indexOf(it)
            viewPager.currentItem = position
        }
        return true
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }

}