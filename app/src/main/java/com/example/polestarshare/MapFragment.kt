package com.example.polestarshare

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
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
import com.mapbox.search.ResponseInfo
import com.mapbox.search.ReverseGeoOptions
import com.mapbox.search.SearchCallback
import com.mapbox.search.SearchEngine
import com.mapbox.search.SearchEngineSettings
import com.mapbox.search.autocomplete.PlaceAutocomplete
import com.mapbox.search.autocomplete.PlaceAutocompleteSuggestion
import com.mapbox.search.common.AsyncOperationTask
import com.mapbox.search.result.SearchResult
import com.mapbox.search.ui.adapter.autocomplete.PlaceAutocompleteUiAdapter
import com.mapbox.search.ui.view.CommonSearchViewConfiguration
import com.mapbox.search.ui.view.SearchResultsView
import java.math.BigDecimal
import java.math.RoundingMode


class MapFragment : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var floatingActionButton: FloatingActionButton
    private lateinit var floatingActionButtonMapFilter: FloatingActionButton
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    var LAT: Double = 51.450340
    var LON: Double = 5.452850
    val db = Firebase.firestore


    private lateinit var searchEngine: SearchEngine
    private lateinit var searchRequestTask: AsyncOperationTask
    private var reverseLocation: String = "Deelauto locatie"
    var SearchLAT: Double = 51.0
    var SearchLON: Double = 5.0




    private lateinit var placeAutocompleteMap: PlaceAutocomplete
    private lateinit var searchResultsViewMap: SearchResultsView
    private lateinit var placeAutocompleteMapUiAdapter: PlaceAutocompleteUiAdapter
    private lateinit var queryEditTextMap: EditText
    private var ignoreNextQueryUpdateMap = false

    private lateinit var placeAutocompleteRoute: PlaceAutocomplete
    private lateinit var searchResultsViewRoute: SearchResultsView
    private lateinit var placeAutocompleteRouteUiAdapter: PlaceAutocompleteUiAdapter
    private lateinit var queryEditTextRoute: EditText
    private var ignoreNextQueryUpdateRoute = false



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
                mapView.camera.flyTo(cameraOptions { center(Point.fromLngLat(LON, LAT)) })
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
        val view = inflater.inflate(R.layout.fragment_map, container, false)

        placeAutocompleteMap = PlaceAutocomplete.create(getString(R.string.mapbox_access_token))

        queryEditTextMap = view.findViewById(R.id.query_text)


        searchResultsViewMap = view.findViewById(R.id.search_results_view)

        searchResultsViewMap.initialize(
            SearchResultsView.Configuration(
                commonConfiguration = CommonSearchViewConfiguration()
            )
        )

        placeAutocompleteMapUiAdapter = PlaceAutocompleteUiAdapter(
            view = searchResultsViewMap,
            placeAutocomplete = placeAutocompleteMap
        )

        placeAutocompleteMapUiAdapter.addSearchListener(object : PlaceAutocompleteUiAdapter.SearchListener {

            override fun onSuggestionsShown(suggestions: List<PlaceAutocompleteSuggestion>) {
                // Nothing to do
            }

            override fun onSuggestionSelected(suggestion: PlaceAutocompleteSuggestion) {
                mapView.camera.flyTo(cameraOptions {
                    center(Point.fromLngLat(suggestion.coordinate.coordinates()[0].toDouble(), suggestion.coordinate.coordinates()[1].toDouble()))
                    zoom(12.0)
                })
                searchResultsViewMap.isVisible = false
                hideKeyBoard(view)
            }

            override fun onPopulateQueryClick(suggestion: PlaceAutocompleteSuggestion) {
                queryEditTextMap.setText(suggestion.name)
            }

            override fun onError(e: Exception) {
                // Nothing to do
            }
        })
        val closeSearch = view.findViewById<ImageButton>(R.id.closeSearch)
        closeSearch.setOnClickListener{
            searchResultsViewMap.isVisible = false
            queryEditTextMap.setText("")
            hideKeyBoard(view)
        }

        queryEditTextMap.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(text: CharSequence, start: Int, before: Int, count: Int) {
                if (ignoreNextQueryUpdateMap) {
                    ignoreNextQueryUpdateMap = false
                } else {
                    Log.d("tag", "search place update")
                }

                lifecycleScope.launchWhenStarted {
                    placeAutocompleteMapUiAdapter.search(text.toString())
                    searchResultsViewMap.isVisible = text.isNotEmpty()
                }
            }
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // Nothing to do
            }

            override fun afterTextChanged(s: Editable) {
                // Nothing to do
            }
        })


        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )


        fusedLocationProviderClient = LocationServices
            .getFusedLocationProviderClient(requireActivity());


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
                        .whereEqualTo("reserved", false)
                        .get()
                        .addOnSuccessListener { result ->
                            val context = context ?: return@addOnSuccessListener
                            if (context != null) {


                                for (document in result) {
                                    addAnnotationToMap(
                                        document.data.get("lat").toString().toDouble(),
                                        document.data.get("lon").toString().toDouble(),
                                        document.data.get("carModel").toString(),
                                        document.data.get("battery").toString(),
                                        document.id,
                                    )
                                }
                            }
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(
                                requireContext(),
                                "something went wrong fetching the annotaions",
                                Toast.LENGTH_SHORT
                            ).show()
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
            val center =
                cameraOptions.coordinates()[0].toString().take(7).equals(LON.toString().take(7))
            if (center) {
                floatingActionButton.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.location
                    )
                )

            } else {
                floatingActionButton.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.location_searching
                    )
                )
            }


        }


        // Inflate the layout for this fragment
        return view
    }


    private fun addAnnotationToMap(
        lat: Double,
        lon: Double,
        carModelNumber: String,
        battery: String,
        userID: String
    ) {
        bitmapFromDrawableRes(
            requireContext(),
            R.drawable.marker
        )?.let {
            val annotationApi = mapView?.annotations
            val pointAnnotationManager = annotationApi?.createPointAnnotationManager(mapView!!)
            val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
                .withPoint(Point.fromLngLat(lon, lat))
                .withIconImage(it)
            pointAnnotationManager?.apply {
                addClickListener(
                    OnPointAnnotationClickListener {
                        val dialog = BottomSheetDialog(requireContext())
                        val view = layoutInflater.inflate(R.layout.reserve_car_bottom_sheet, null)



                        val carModelText = view.findViewById<TextView>(R.id.carModelText)
                        val batteryText = view.findViewById<TextView>(R.id.battery)

                        carModelText.text = "Polestar ${carModelNumber}"
                        val carModelImg = view.findViewById<ImageView>(R.id.carModelImg)
                        if (carModelNumber.equals("1")) {
                            carModelImg.setImageResource(R.drawable.polestar1)
                        } else if (carModelNumber.equals("2")) {
                            carModelImg.setImageResource(R.drawable.polestar2)
                        } else {
                            carModelImg.setImageResource(R.drawable.polestar3)
                        }

                        batteryText.text = "${battery}%"
                        val batteryRange = view.findViewById<TextView>(R.id.batteryRange)
                        val range: Double = 635 * (battery.toDouble() / 100)
                        batteryRange.text = "${range.toInt().toString()} km"

                        val walking = view.findViewById<TextView>(R.id.walkingRange)
                        walking.text = "${BigDecimal(distance(LAT, LON, lat, lon)).setScale(2, RoundingMode.HALF_UP)} km";
                        val walkingTime = view.findViewById<TextView>(R.id.walkingTime)
                        walkingTime.text = formattedTime(BigDecimal(distance(LAT, LON, lat, lon) / 5).setScale(2, RoundingMode.HALF_UP).toDouble())
                        openBottomsheet(dialog, view)
                        mapView.camera.flyTo(cameraOptions {
                            center(Point.fromLngLat(lon, lat))
                            zoom(17.0)
                        })
                        val confirmScreen = layoutInflater.inflate(
                            R.layout.reserve_car_bottom_sheet_confirmation,
                            null
                        )
                        val reserveer = view.findViewById<MaterialButton>(R.id.reserveer)
                        reserveer.setOnClickListener {
                            changeBottomSheet(dialog, confirmScreen)
                        }

                        val carModelTextConfirmation =
                            confirmScreen.findViewById<TextView>(R.id.carModelText)
                        val batteryTextConfirmation =
                            confirmScreen.findViewById<TextView>(R.id.battery)

                        carModelTextConfirmation.text = "Polestar ${carModelNumber}"
                        val carModelImgConfirmation =
                            confirmScreen.findViewById<ImageView>(R.id.carModelImg)
                        if (carModelNumber.equals("1")) {
                            carModelImgConfirmation.setImageResource(R.drawable.polestar1)
                        } else if (carModelNumber.equals("2")) {
                            carModelImgConfirmation.setImageResource(R.drawable.polestar2)
                        } else {
                            carModelImgConfirmation.setImageResource(R.drawable.polestar3)
                        }

                        batteryTextConfirmation.text = "${battery}%"
                        val batteryRangeConfirmation =
                            confirmScreen.findViewById<TextView>(R.id.batteryRange)
                        batteryRangeConfirmation.text = "${range.toInt().toString()} km"

                        val walkingConfirmation = confirmScreen.findViewById<TextView>(R.id.walkingRange)
                        walkingConfirmation.text = "${BigDecimal(distance(LAT, LON, lat, lon)).setScale(2, RoundingMode.HALF_UP)} km";
                        val walkingTimeConfirmation = confirmScreen.findViewById<TextView>(R.id.walkingTime)
                        walkingTimeConfirmation.text = formattedTime(BigDecimal(distance(LAT, LON, lat, lon) / 5).setScale(2, RoundingMode.HALF_UP).toDouble())
                        val cancelButton =
                            confirmScreen.findViewById<MaterialButton>(R.id.cancel_button)
                        cancelButton.setOnClickListener {
                            changeBottomSheet(dialog, view)
                            dialog.setCancelable(true)
                        }

                        searchEngine = SearchEngine.createSearchEngineWithBuiltInDataProviders(
                            SearchEngineSettings(getString(R.string.mapbox_access_token))
                        )

                        val options = ReverseGeoOptions(
                            center = Point.fromLngLat(lon, lat),
                            limit = 1
                        )
                        searchRequestTask = searchEngine.search(options, searchCallback)

                        val searchScreen = layoutInflater.inflate(R.layout.search, null)
                        val autoLocatie = searchScreen.findViewById<EditText>(R.id.autoLocatie)
                        val startRoute = searchScreen.findViewById<MaterialButton>(R.id.startRoute)
                        val closeButton = searchScreen.findViewById<ImageView>(R.id.closeButton)
                        startRoute.setOnClickListener {
                            var RouteIntent = Intent(requireContext(), Navigation::class.java)
                            val bundle = Bundle()

                            bundle.putDouble("CarLAT", lat)
                            bundle.putDouble("CarLON", lon)
                            bundle.putDouble("LAT", SearchLAT)
                            bundle.putDouble("LON", SearchLON)
                            RouteIntent.putExtras(bundle);

                            startActivity(RouteIntent)

                        }

                        closeButton.setOnClickListener {
                            dialog.dismiss()
                        }

                        placeAutocompleteRoute = PlaceAutocomplete.create(getString(R.string.mapbox_access_token))

                        queryEditTextRoute = searchScreen.findViewById(R.id.query_text)


                        searchResultsViewRoute = searchScreen.findViewById(R.id.search_results_view)

                        searchResultsViewRoute.initialize(
                            SearchResultsView.Configuration(
                                commonConfiguration = CommonSearchViewConfiguration()
                            )
                        )

                        placeAutocompleteRouteUiAdapter = PlaceAutocompleteUiAdapter(
                            view = searchResultsViewRoute,
                            placeAutocomplete = placeAutocompleteRoute
                        )

                        placeAutocompleteRouteUiAdapter.addSearchListener(object : PlaceAutocompleteUiAdapter.SearchListener {

                            override fun onSuggestionsShown(suggestions: List<PlaceAutocompleteSuggestion>) {
                                // Nothing to do
                            }

                            @SuppressLint("ResourceType")
                            override fun onSuggestionSelected(suggestion: PlaceAutocompleteSuggestion) {

                                mapView.camera.flyTo(cameraOptions {
                                    center(Point.fromLngLat(suggestion.coordinate.coordinates()[0].toDouble(), suggestion.coordinate.coordinates()[1].toDouble()))
                                    zoom(12.0)
                                })
                                SearchLAT = suggestion.coordinate.coordinates()[1].toDouble()
                                SearchLON = suggestion.coordinate.coordinates()[0].toDouble()
                                startRoute.isEnabled = true
                                startRoute.setBackgroundColor(Color.parseColor(getString(R.color.orange)));
                                searchResultsViewRoute.isVisible = false
                                hideKeyBoard(searchScreen)

                            }
                            override fun onPopulateQueryClick(suggestion: PlaceAutocompleteSuggestion) {
                                queryEditTextRoute.setText(suggestion.name)
                            }


                            override fun onError(e: Exception) {
                                // Nothing to do
                            }
                        })

                        queryEditTextRoute.addTextChangedListener(object : TextWatcher {

                            override fun onTextChanged(text: CharSequence, start: Int, before: Int, count: Int) {
                                if (ignoreNextQueryUpdateRoute) {
                                    ignoreNextQueryUpdateRoute = false
                                } else {
                                    Log.d("tag", "search place update")
                                }

                                lifecycleScope.launchWhenStarted {
                                    placeAutocompleteRouteUiAdapter.search(text.toString())
                                    searchResultsViewRoute.isVisible = text.isNotEmpty()
                                }
                            }
                            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                                // Nothing to do
                            }

                            override fun afterTextChanged(s: Editable) {
                                // Nothing to do
                            }
                        })


                        val confirmButton =
                            confirmScreen.findViewById<MaterialButton>(R.id.confirm_button)
                        confirmButton.setOnClickListener {
                            autoLocatie.setText(reverseLocation);
                            changeBottomSheet(dialog, searchScreen)

                            db.collection("Markers").document(userID).update("reserved", true)

                        }

                        val priceScreen =
                            layoutInflater.inflate(R.layout.reserve_car_price_bottom_sheet, null)
                        val priceButton = view.findViewById<MaterialButton>(R.id.priceButton)
                        priceButton.setOnClickListener {
                            changeBottomSheet(dialog, priceScreen)
                        }
                        val priceBackButton = priceScreen.findViewById<ImageView>(R.id.priceBack)
                        priceBackButton.setOnClickListener {
                            changeBottomSheet(dialog, view)
                            dialog.setCancelable(true)
                        }

                        false

                    }
                )
            }
            pointAnnotationManager?.create(pointAnnotationOptions)

        }
    }

    private fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val theta = lon1 - lon2
        var dist = (Math.sin(deg2rad(lat1))
                * Math.sin(deg2rad(lat2))
                + (Math.cos(deg2rad(lat1))
                * Math.cos(deg2rad(lat2))
                * Math.cos(deg2rad(theta))))
        dist = Math.acos(dist)
        dist = rad2deg(dist)
        dist = dist * 60 * 1.1515 * 1.609344
        return dist
    }

    private fun deg2rad(deg: Double): Double {
        return deg * Math.PI / 180.0
    }

    private fun rad2deg(rad: Double): Double {
        return rad * 180.0 / Math.PI
    }

    private fun formattedTime(totalhours: Double) : String {
        val wholeHours = totalhours.toInt()
        val remainingMinutes = ((totalhours - wholeHours) * 60).toInt()

        val formattedTime = if (wholeHours > 0) {
            "$wholeHours uur ${if (remainingMinutes > 0) "$remainingMinutes min" else ""}"
        } else {
            "$remainingMinutes min"
        }
        return formattedTime
    }


    private fun openBottomsheet(dialog: Dialog, view: View) {
        dialog.setCancelable(true)
        dialog.setContentView(view)
        dialog.show()
    }

    private fun changeBottomSheet(dialog: Dialog, view: View) {
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
        fusedLocationProviderClient.lastLocation.addOnCompleteListener(requireActivity()) { task ->
            val location: Location? = task.result
            if (location != null) {
                LAT = location.latitude
                LON = location.longitude

                //Instellingen voor map aangeven en daarna bouwen
                mapView.camera.flyTo(cameraOptions {
                    center(Point.fromLngLat(LON, LAT))
                    zoom(15.0)
                })

            }
        }
    }


    private val searchCallback = object : SearchCallback {

        override fun onResults(results: List<SearchResult>, responseInfo: ResponseInfo) {
            if (results.isEmpty()) {
                Log.i("SearchApiExample", "No reverse geocoding results")
            } else {
                Log.i("BEEEN", results[0].address?.street.toString())

                var a = ""
                var b = ""
                var c = ""
                var d = ""

                if (results[0].address?.street !== null) {
                    a = results[0].address?.street + ", "
                }
                if (results[0].address?.houseNumber !== null) {
                    b = results[0].address?.houseNumber + ", "
                }
                if (results[0].address?.postcode !== null) {
                    c = results[0].address?.postcode + ", "
                } else {
                }
                if (results[0].address?.place !== null) {
                    d = results[0].address?.place + ", "
                }


                var adressCombined = a + b + c + d
                reverseLocation = adressCombined.dropLast(2)
            }
        }

        override fun onError(e: Exception) {
            Log.i("SearchApiExample", "Reverse geocoding error", e)
        }
    }

    private fun hideKeyBoard(it: View){
        try{
            val imm = requireActivity().getSystemService(
                INPUT_METHOD_SERVICE
            ) as InputMethodManager

            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }catch (e:Exception){
            e.printStackTrace()
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
        mapView?.onDestroy()
        super.onDestroy()

    }

}