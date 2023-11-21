package com.example.polestarshare

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.OnPointAnnotationClickListener
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.location


class MapFragment : Fragment()  {

    private lateinit var mapView: MapView
    private lateinit var floatingActionButton: FloatingActionButton
    private lateinit var floatingActionButtonMapFilter: FloatingActionButton
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    var LAT: Double = 51.450340
    var LON: Double = 5.452850
    val db = Firebase.firestore






    val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Only approximate location access granted.
                getCurrentLocation()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Precise location access granted.
                getCurrentLocation()
            }
            else -> {
                mapView.camera.flyTo(cameraOptions { center(Point.fromLngLat(LON,LAT)) })
            // No location access granted.
        }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        locationPermissionRequest.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION))


        fusedLocationProviderClient = LocationServices
            .getFusedLocationProviderClient(requireActivity());

        val view = inflater.inflate(R.layout.fragment_map, container, false)
        mapView = view.findViewById(R.id.mapView)
        mapView?.getMapboxMap()?.loadStyleUri(
            Style.MAPBOX_STREETS,
            // After the style is loaded, initialize the Location component.
            object : Style.OnStyleLoaded {
                override fun onStyleLoaded(style: Style) {
                    mapView.location.updateSettings {
                        enabled = true
                        pulsingEnabled = true

                    }
                    db.collection("Markers")
                        .get()
                        .addOnSuccessListener { result ->
                            val context = context ?: return@addOnSuccessListener
                            if(context != null){


                            for (document in result) {
                                addAnnotationToMap(
                                    document.data.get("lat").toString().toDouble(),
                                    document.data.get("lon").toString().toDouble(),
                                    document.data.get("carModel").toString(),
                                    document.data.get("battery").toString(),
                                    )
                            }
                            }
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(requireContext(), "something went wrong fetching the annotaions", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        )

        floatingActionButton = view.findViewById(R.id.floatingActionButton)
        floatingActionButton.setOnClickListener(View.OnClickListener { getCurrentLocation() })

        floatingActionButtonMapFilter = view.findViewById(R.id.floatingActionButtonMapFilter)
        floatingActionButtonMapFilter.setOnClickListener(View.OnClickListener {
            val dialog = BottomSheetDialog(requireContext())
            val view = layoutInflater.inflate(R.layout.map_filter, null)
            val satalietkaart = view.findViewById<CardView>(R.id.satelietkaart)
            satalietkaart.setOnClickListener {
                mapView?.getMapboxMap()?.loadStyleUri(Style.SATELLITE_STREETS)
            }
            val standaardkaart = view.findViewById<CardView>(R.id.standaardkaart)
            standaardkaart.setOnClickListener {
                mapView?.getMapboxMap()?.loadStyleUri(Style.MAPBOX_STREETS)
            }
            val tereinkaart = view.findViewById<CardView>(R.id.tereinkaart)
            tereinkaart.setOnClickListener {
                mapView?.getMapboxMap()?.loadStyleUri(Style.OUTDOORS)
            }
            dialog.setCancelable(true)
            dialog.setContentView(view)
            dialog.show()
        })

        mapView.camera.addCameraCenterChangeListener { cameraOptions ->
                val center = cameraOptions.coordinates()[0].toString().take(7).equals(LON.toString().take(7))
            if (center){
                floatingActionButton.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.location))

            }
            else{
                floatingActionButton.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.location_searching))
            }


    }





        // Inflate the layout for this fragment
        return view
    }


    private fun addAnnotationToMap(lat: Double, lon: Double, carModelNumber : String, battery: String) {
        bitmapFromDrawableRes(
            requireContext(),
            R.drawable.marker
        )?.let {
            val annotationApi = mapView?.annotations
            val pointAnnotationManager = annotationApi?.createPointAnnotationManager(mapView!!)
            val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
                .withPoint(Point.fromLngLat(lon, lat))
                .withIconImage(it)
            pointAnnotationManager?.apply { addClickListener(
                OnPointAnnotationClickListener {
                    val dialog = BottomSheetDialog(requireContext())
                    val view = layoutInflater.inflate(R.layout.reserve_car_bottom_sheet, null)
                    val carModelText = view.findViewById<TextView>(R.id.carModelText)
                    carModelText.text = "Polestar ${carModelNumber}"
                    val carModelImg = view.findViewById<ImageView>(R.id.carModelImg)
                    if(carModelNumber.equals("1")){
                        carModelImg.setImageResource(R.drawable.polestar1)
                    }
                    else if (carModelNumber.equals("2")){
                        carModelImg.setImageResource(R.drawable.polestar2)
                    }
                    else{
                        carModelImg.setImageResource(R.drawable.polestar3)
                    }
                    val batteryText = view.findViewById<TextView>(R.id.battery)
                    batteryText.text = "${battery}%"
                    val batteryRange = view.findViewById<TextView>(R.id.batteryRange)
                    val range : Double = 635 * (battery.toDouble() / 100)
                    batteryRange.text = "${range.toInt().toString()} km"
                    openBottomsheet(dialog,view)
                    mapView.camera.flyTo(cameraOptions {
                        center(Point.fromLngLat(lon, lat))
                        zoom(17.0)
                    })

                    val reserveer = view.findViewById<MaterialButton>(R.id.reserveer)
                    reserveer.setOnClickListener{
                        val intent = Intent(requireContext(), Navigation::class.java)
                        val b = Bundle()
                        b.putDouble("LAT", lat)
                        b.putDouble("LON", lon)
                        intent.putExtras(b);
                        startActivity(intent)
                    }

                    val priceScreen = layoutInflater.inflate(R.layout.reserve_car_price_bottom_sheet, null)
                    val priceButton = view.findViewById<MaterialButton>(R.id.priceButton)
                    priceButton.setOnClickListener{
                        changeBottomSheet(dialog,priceScreen)
                    }
                    val priceBackButton = priceScreen.findViewById<ImageView>(R.id.priceBack)
                    priceBackButton.setOnClickListener{
                        changeBottomSheet(dialog,view)
                        dialog.setCancelable(true)
                    }
                    val routeScreen = layoutInflater.inflate(R.layout.reserve_car_plan_route_bottom_sheet, null)
                    val routeButton = view.findViewById<MaterialButton>(R.id.routeButton)
                    routeButton.setOnClickListener{
                        changeBottomSheet(dialog,routeScreen)
                    }

                    val routeBackButton = routeScreen.findViewById<ImageView>(R.id.routeBack)
                    routeBackButton.setOnClickListener{
                        changeBottomSheet(dialog,view)
                        dialog.setCancelable(true)
                    }


                    false

                }
            ) }
            pointAnnotationManager?.create(pointAnnotationOptions)

        }
    }


    private fun openBottomsheet(dialog:Dialog, view: View){
        dialog.setCancelable(true)
        dialog.setContentView(view)
        dialog.show()
    }

    private fun changeBottomSheet(dialog:Dialog, view:View){
        dialog.setCancelable(false)
        dialog.setContentView(view)
    }



    private fun bitmapFromDrawableRes(context: Context, @DrawableRes resourceId: Int) =
        convertDrawableToBitmap(AppCompatResources.getDrawable(context, resourceId))

    private fun convertDrawableToBitmap(sourceDrawable: Drawable?): Bitmap? {
        if (sourceDrawable == null) {
            return null
        }
        return if (sourceDrawable is BitmapDrawable) {
            sourceDrawable.bitmap
        } else {
// copying drawable object to not manipulate on the same reference
            val constantState = sourceDrawable.constantState ?: return null
            val drawable = constantState.newDrawable().mutate()
            val bitmap: Bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth, drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    }

    
    @SuppressLint("MissingPermission")
     fun getCurrentLocation() {
        //Als locatie verkrijgen gelukt is dit uitvoeren
        fusedLocationProviderClient.lastLocation.addOnCompleteListener(requireActivity()){ task ->
            val location: Location? = task.result
            if(location != null){
                LAT = location.latitude
                LON = location.longitude

                    //Instellingen voor map aangeven en daarna bouwen
                    mapView.camera.flyTo(cameraOptions {
                        center(Point.fromLngLat(LON,LAT))
                        zoom(15.0)
                    })

            }
        }
    }





    override fun onStart() {
        super.onStart()
        mapView?.onStart()
        //Checken of locatie permissie al is toegestaan
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }

}