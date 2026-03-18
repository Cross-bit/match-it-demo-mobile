package com.example.matchit.ui.matchingSession.matching.cards.restaurants


import android.annotation.SuppressLint
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import com.example.matchit.databinding.FragmentMapViewBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.GoogleMap
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException
import java.util.Locale

@AndroidEntryPoint
class MapViewFragment : Fragment(),OnMapReadyCallback
{

    private var _binding: FragmentMapViewBinding? = null
    private val binding get() = _binding!!

    var map: FrameLayout? = null
    var gMap: GoogleMap? = null
    var currentLocation: Location? = null
    var marker: Marker? = null
    var fusedClient: FusedLocationProviderClient? = null
    private val REQUEST_CODE = 101
    lateinit var searchView: SearchView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMapViewBinding.inflate(inflater, container, false)
        return binding.root;
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
      map = binding.map
        searchView = binding.search

        searchView.clearFocus()

        fusedClient = LocationServices.getFusedLocationProviderClient(this.requireContext())

        getLocation()

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {

                val loc = searchView.query.toString()

                if (loc == null) {
                    Toast.makeText(this@MapViewFragment.context, "Location Not Found", Toast.LENGTH_SHORT)
                        .show()
                } else {

                    val geocoder = Geocoder(this@MapViewFragment.requireContext(), Locale.getDefault())

                    try {

                        val addressList: List<Address>? = geocoder.getFromLocationName(loc, 1)

                        if (addressList!!.size > 0) {
                            val latLng = LatLng(
                                addressList!![0].getLatitude(), addressList!![0].getLongitude()
                            )
                            if (marker != null) {
                                marker!!.remove()
                            }
                            val markerOptions = MarkerOptions().position(latLng).title(loc)
                            markerOptions.icon(
                                BitmapDescriptorFactory.defaultMarker(
                                    BitmapDescriptorFactory.HUE_AZURE
                                )
                            )

                            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15f)
                            gMap!!.animateCamera(cameraUpdate)
                            ;        marker = gMap!!.addMarker(markerOptions)
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                return false
            }
        })


    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {

        val ctx = this.requireContext()

        // first rather check the permissions...

        /*if (ActivityCompat.checkSelfPermission(
                ctx,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                ctx,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        )
        {
            ActivityCompat.requestPermissions(
                this.requireActivity(),
                arrayOf<String>(Manifest.permission.),
                101
            )-
        }*/

        val task: Task<Location> = fusedClient!!.lastLocation

        task.addOnSuccessListener { location ->
            if (location != null) {

                val supportMapFragment: SupportMapFragment =  childFragmentManager.findFragmentById(binding.map.id) as SupportMapFragment

                supportMapFragment.getMapAsync(this);

                currentLocation = location
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        gMap = googleMap
        val latLng = LatLng(
            currentLocation!!.latitude, currentLocation!!.longitude
        )

        val markerOptions = MarkerOptions().position(latLng).title("Location")
        googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng))
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 5f))
        googleMap.addMarker(markerOptions)

    }

}