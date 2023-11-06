package com.example.polestarshare

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.observable.eventdata.CameraChangedEventData
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.OnPointAnnotationClickListener
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.delegates.listeners.OnCameraChangeListener
import com.mapbox.maps.plugin.locationcomponent.location
import kotlinx.coroutines.delay


class MapFragment : Fragment()  {

    private lateinit var mapView: MapView
    private lateinit var floatingActionButton: FloatingActionButton
    private lateinit var floatingActionButtonMapFilter: FloatingActionButton
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    var LAT: Double = 51.450340
    var LON: Double = 5.452850

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

                    addAnnotationToMap()
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


    private fun addAnnotationToMap() {
        bitmapFromDrawableRes(
            requireContext(),
            R.drawable.marker
        )?.let {
            val annotationApi = mapView?.annotations
            val pointAnnotationManager = annotationApi?.createPointAnnotationManager(mapView!!)
            val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
                .withPoint(Point.fromLngLat(5.452850, 51.450340))
                .withIconImage(it)
            pointAnnotationManager?.apply { addClickListener(
                OnPointAnnotationClickListener {
                    val dialog = BottomSheetDialog(requireContext())
                    val view = layoutInflater.inflate(R.layout.reserve_car_bottom_sheet, null)
                    dialog.setCancelable(true)
                    dialog.setContentView(view)
                    dialog.show()
                    false

                }
            ) }
            pointAnnotationManager?.create(pointAnnotationOptions)

        }
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