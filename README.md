# Mini_AirBnb

### Naver Map API
초기설정 시 API문서 외 아래 추가 설정 필요
https://developer.android.com/jetpack/androidx?hl=ko
~~~kotlin
// in gradle.poroerties
android.enableJetifier=true
~~~
MapView 와 LocationButtonView(현재 위치)
~~~xml
// in activity_main.xml
<com.naver.maps.map.MapView ... />
<com.naver.maps.map.widget.LocationButtonView ... />
~~~
<초기화 및 설정>
Activity의 각 생명주기에 맞춰 mapView를 초기화해야한다.
(그래서 보통 Fragment에 올린다고 한다.)
~~~kotlin
// in MainActivity.kt
// class에 OnMapReadyCallback 종속성 추가 -> 지도
class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    mapView.getMapAsync(this)

    override fun onMapReady(p0: NaverMap) {
        naverMap = p0
        // 최대, 최소 배율
        naverMap.maxZoom = 18.0
        naverMap.minZoom = 10.0
        // 카메라 시점 변환
        val cameraUpdate = CameraUpdate.scrollTo(LatLng(37.498095, 127.027610))
        naverMap.moveCamera(cameraUpdate)
        // 기본 현재 위치 버튼 비활성화
        val uiSetting = naverMap.uiSettings
        uiSetting.isLocationButtonEnabled = false
        // 위치정보 수집권한
        locationSource = FusedLocationSource(this@MainActivity, LOCATION_PERMISSION_REQUEST_CODE)
        naverMap.locationSource = locationSource
    }

    // 위치정보 수집권한 결과
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

	//onStart, onResume, onPause, onSaveInstanceState, onStop, onDestroy, onLowMemory 모두에 적용
	override fun onStart() {
        super.onStart()
        mapView.onStart()
    }
}
~~~

### Naver Map API Marker
~~~kotlin
private fun setNaverMapMarker(houseModel: HouseModel) {
    val marker = Marker()
    marker.position = LatLng(houseModel.lat, houseModel.lng)
    marker.tag = houseModel.id
    marker.icon = OverlayImage.fromResource(R.drawable.black_marker)
    marker.iconTintColor = Color.BLACK
    marker.width = 150
    marker.height = 150
    marker.alpha = 0.7f
    // class에 Overlay.OnClickListener 종속성 추가
    marker.onClickListener = this
    marker.map = naverMap
}
    // marker.onClickListener
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
~~~

### Retrofit2 : Restful API
interface 생성
~~~ kotlin
// HouseService.kt 생성
interface HouseService {
    @GET("/v3/9ec922b9-9e19-4cdc-8a73-2a27759aed7a") // 공통 url 뒷단
    fun getHouseList(): Call<HouseDto>
}
~~~

data 구조 정의 : data class 생성
data class의 value-자료형은 json 의 key-value값과 Sync
~~~kotlin
// HouseDto.kt 생성
data class HouseDto(
    val items: List<HouseModel>
)
// HouseModel.kt 생성
data class HouseModel(
    val id: Int,
    val title: String,
    val price: Int,
    val lat: Double,
    val lng: Double,
    val imgUrl: String
)
~~~

~~~kotlin
// retrofit 빌드
val retrofit = Retrofit.Builder()
    .baseUrl("https://run.mocky.io") // 공통 url
    .addConverterFactory(GsonConverterFactory.create())
    .build()
// retrifit 생성
retrofit.create(HouseService::class.java).also {
    it.getHouseList()
        .enqueue(object: Callback<HouseDto> {
            override fun onResponse(call: Call<HouseDto>, response: Response<HouseDto>) {
                // API 호출 성공
                if (response.isSuccessful.not()) {
                    //실패처리
                    return
                }
                response.body()?.let { houseDto ->
                    // houseDto를 호출한다. ( = List<HouseModel>)
                }
            }

        override fun onFailure(call: Call<HouseDto>, t: Throwable) {
            //실패처리
        }
    })
}
~~~

### ViewPager2, RecyclerView
~~~kotlin
// RecycleView 초기 설정
recyclerView.layoutManager = LinearLayoutManager(this)
recyclerViewAdapter = HouseListAdapter()
recyclerView.adapter = recyclerViewAdapter
// ViewPager 초기 설정
// ViewPager 는 layoutManager = LinearLayoutManager(this) 필요없다
// ViewPager ViewHolder의 layout은 반드시 matched_parent
viewPagerAdapter = HouseViewPagerAdapter ()
viewPager.adapter = viewPagerAdapter
// adapter는 모두 RecyclerView 기반

viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
    // ViewPager의 currentItem이 변경 되었을때
    override fun onPageSelected(position: Int) {
        super.onPageSelected(position)
        // 변경된 Item의 좌표를 통해 Map의 카메라 시점 변경
        val currentHouseModel = viewPagerAdapter.currentList[position]
        val cameraUpdate = CameraUpdate.scrollTo(LatLng(currentHouseModel.lat, currentHouseModel.lng))
            .animate(CameraAnimation.Easing)

        naverMap.moveCamera(cameraUpdate)
    }
})
~~~

### CoordinatorLayout - BottomSheet
~~~xml
<!--CoordinatorLayout 내부에 @layout/bottom_sheet-->
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:layout_marginTop="24dp"
    tools:context=".MainActivity">

    <include layout="@layout/bottom_sheet" />

</androidx.coordinatorlayout.widget.CoordinatorLayout>

<!--@layout/bottom_sheet-->
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@drawable/top_radius_white_background"
    app:layout_behavior="com.google.android.material.bottomsheet.BottomSheetBehavior"
    app:behavior_peekHeight="60dp"
    xmlns:app="http://schemas.android.com/apk/res-auto">
<!--app:layout_behavior과 app:behavior_peekHeight로 설정-->
~~~

### 외부로 공유 (ACTION_SEND, createChooser)
https://developer.android.com/training/sharing/send
~~~kotlin
// intent의 action, Extra, type 을 지정
val intent = Intent().apply {
    action = Intent.ACTION_SEND
    putExtra(Intent.EXTRA_TEXT, "[숙소 정보]\n${houseModel.title}\n${price}원\nURL : ${houseModel.imgUrl}")
    type = "text/plain"
}
// createChooser Activity 실행 (공유)
startActivity(Intent.createChooser(intent, null))
~~~