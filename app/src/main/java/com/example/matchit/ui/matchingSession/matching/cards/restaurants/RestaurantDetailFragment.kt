package com.example.matchit.ui.matchingSession.matching.cards.restaurants

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources

import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.example.matchit.R
import com.example.matchit.data.model.googlePlaces.PlacePhoto
import com.example.matchit.data.model.session.OpeningDayUi
import com.example.matchit.data.model.session.OpeningHoursUi
import com.example.matchit.data.model.session.RestaurantCardData
import com.example.matchit.data.model.session.toLatLng
import com.example.matchit.databinding.FragmentRestaurantDetailBinding
import com.example.matchit.ui.matchingSession.common.BottomSheetFragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale


@AndroidEntryPoint
class RestaurantDetailFragment : Fragment(), RestaurantImageCarouselAdapter.OnItemClickListener
{
    public companion object {
        public const val ARG_ITEM = "item"

        fun newInstance(item: RestaurantCardData) = RestaurantDetailFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_ITEM, item)
            }
        }
    }

    lateinit var mapView: MapView
    lateinit var googleMap: GoogleMap

    private var _binding: FragmentRestaurantDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var googleRating: TextView
    private lateinit var carouselAdapter: RestaurantImageCarouselAdapter
    private lateinit var carouseRecyclerView: RecyclerView
    private lateinit var titleView: TextView

    // openning hours
    private var openingExpanded = false
    private lateinit var openingHoursContainer: LinearLayout
    private lateinit var openingHoursList: LinearLayout
    private lateinit var openingHoursToday: TextView

    private lateinit var openingHoursHeader: LinearLayout
    private lateinit var openingHoursArrow: ImageView

    private lateinit var googleMapsBtn: TextView
    private val restaurantNavigator: RestaurantNavigator = RestaurantNavigator(this)

    private val restaurantDetailViewModel: RestaurantDetailViewModel by viewModels<RestaurantDetailViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRestaurantDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    lateinit var snapHelper: SnapHelper
    private lateinit var cardData: RestaurantCardData

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        cardData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(ARG_ITEM, RestaurantCardData::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable(ARG_ITEM)
        } ?: error("RestaurantCardData is missing in arguments")

        titleView = binding.cardTitle
        titleView.text = cardData?.title ?: "Missing restaurant name."

        carouseRecyclerView = binding.carouselRecyclerView

        carouselAdapter = RestaurantImageCarouselAdapter(requireContext(),this, emptyList())
        carouseRecyclerView.adapter = carouselAdapter

        snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(carouseRecyclerView)

        openingHoursContainer = binding.openingHoursContainer
        openingHoursToday =  binding.openingHoursToday
        openingHoursList = binding.openingHoursList
        openingHoursHeader = binding.openingHoursHeader
        openingHoursArrow = binding.openingHoursArrow

        bindOpeningHours(cardData.openingHours)

        cardData?.placePhotos?.let {
            carouselAdapter.setImages(ArrayList(it))
            carouselAdapter.notifyDataSetChanged()
        }

        val cardView = binding.restaurantCardView
        val behavior = BottomSheetBehavior.from(cardView)

        cardView.translationY = resources.displayMetrics.heightPixels.toFloat()

        // Ensure the card is closed on start!
        behavior.isDraggable = false
        behavior.state = BottomSheetBehavior.STATE_COLLAPSED

        cardView.post {
            // Slide animation
            cardView.animate()
                .translationY(0f)
                .setDuration(150)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    // once we are up, we allow drag
                    behavior.isDraggable = true
                    behavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
                .start()
        }

        val typeChiplist = binding.typeChipsGroup
        val featureChiplist = binding.featuresChipsGroup
        val addressText = binding.placeAddressText

        addressText.text = cardData?.locationAddress ?: "unknown"

        cardData?.let {
            val chipListManger = RestaurantChipListManger(requireContext(), typeChiplist, featureChiplist)
            chipListManger.clearChips()

            chipListManger.appendTypesChips(cardData.type)
            chipListManger.appendVegetarianChip(cardData.servesVegetarian)
            chipListManger.appendTakeoutChip(cardData.takeout)
            chipListManger.appendPriceLevel(cardData.priceLevel ?: 2)
        }

        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    parentFragmentManager.beginTransaction()
                        .remove(this@RestaurantDetailFragment)
                        .commit()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) { /*react to drag progress */ }
        })

        embedGoogleMap(savedInstanceState)

        restaurantDetailViewModel.userLocation.observe(viewLifecycleOwner) { location ->
            val userLatLng = location.toLatLng()
            val restaurantLatLng = cardData?.location?.toLatLng()

            if (location == null || restaurantLatLng == null) return@observe

            googleMap.addMarker(
                MarkerOptions()
                    .position(userLatLng)
                    .title(getString(R.string.you_on_the_map_text))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            )

            val distanceM = calculateDistanceMeters(userLatLng, restaurantLatLng)

            googleMap.addPolyline(
                com.google.android.gms.maps.model.PolylineOptions()
                    .add(userLatLng, restaurantLatLng)
                    .width(8f)
                    .color(resources.getColor(R.color.app_base_color))
                    .pattern(listOf(
                        com.google.android.gms.maps.model.Dash(30f),
                        com.google.android.gms.maps.model.Gap(20f)
                    ))
            )

            // Set camera correctly to include all points
            val builder = com.google.android.gms.maps.model.LatLngBounds.Builder()
            builder.include(userLatLng)
            builder.include(restaurantLatLng)

            val bounds = builder.build()
            googleMap.animateCamera(
                CameraUpdateFactory.newLatLngBounds(bounds, 150)
            )
        }

        googleMapsBtn = binding.openInMapsBtn

        googleMapsBtn.setOnClickListener {
            this.restaurantNavigator.openGoogleMaps(cardData)
        }

        googleRating = binding.googleRating
        googleRating.text = cardData.rating.toString()
    }

    private fun calculateDistanceMeters(a: LatLng, b: LatLng): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            a.latitude, a.longitude,
            b.latitude, b.longitude,
            results
        )
        return results[0] // meters
    }

    fun drawableToBitmap(context: Context, drawableRes: Int): Bitmap {
        val drawable = AppCompatResources.getDrawable(context, drawableRes)!!

        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 100
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 100

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    @SuppressLint("ClickableViewAccessibility") // Google MapView does not override implement performClick, this suppress the warning on setOnTouchListener
    private fun embedGoogleMap(savedInstanceState: Bundle?) {

        mapView = binding.restaurantMap
        mapView.onCreate(savedInstanceState)

        mapView.getMapAsync { map: GoogleMap ->
            googleMap = map

            val restaurantPos = cardData.location.toLatLng()

            googleMap.addMarker(
                MarkerOptions()
                .position(restaurantPos)
                .title("Restaurant")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            )

            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(restaurantPos, 15f))
        }
    }



    override fun onItemClicked(position: Int, item: PlacePhoto) {
        val credentialsArgs = Bundle().apply {
            putParcelable(PhotoAttributionsFragment.ATTRIBUTIONS, item)
        }
        val bottomSheet = BottomSheetFragment.newInstance(PhotoAttributionsFragment::class.java, credentialsArgs)
        bottomSheet.show(childFragmentManager, bottomSheet.tag)
    }

    fun OpeningDayUi.isClosed(): Boolean {
        return openTime == null || closeTime == null
    }

    fun OpeningDayUi.isOpenNow(): Boolean {
        if (isClosed()) return false

        val timeFormatter = DateTimeFormatter.ofPattern("H:mm")

        val now = LocalTime.now()
        val open = LocalTime.parse(openTime, timeFormatter)
        val close = LocalTime.parse(closeTime, timeFormatter)

        return if (close.isAfter(open)) {
            now.isAfter(open) && now.isBefore(close)
        } else {
            now.isAfter(open) || now.isBefore(close)
        }
    }

    fun OpeningDayUi.timeRangeOrDash(): String {
        return if (isClosed()) "—" else "$openTime – $closeTime"
    }

    fun OpeningHoursUi.today(): OpeningDayUi {
        val todayIndex = LocalDate.now().dayOfWeek.value % 7
        return week.first { it.day == todayIndex }
    }

    private fun todayGoogleIndex(): Int {
        return LocalDate.now().dayOfWeek.value % 7  // Mon=1..Sun=7 -> Sun=0
    }

    private fun OpeningHoursUi.nextOpeningLabelCs(): String? {
        val todayIdx = todayGoogleIndex()
        val now = LocalTime.now()

        // try today if not open so far
        val today = week.firstOrNull { it.day == todayIdx } ?: return null
        if (!today.isClosed()) {
            val open = LocalTime.parse(today.openTime)
            if (now.isBefore(open)) {
                return "today ${today.openTime}"
            }
        }

        // try other days
        for (i in 1..7) {
            val idx = (todayIdx + i) % 7
            val d = week.firstOrNull { it.day == idx } ?: continue
            if (!d.isClosed()) {
                val dayShort = LocalDate.now().plusDays(i.toLong()).dayOfWeek
                    .getDisplayName(TextStyle.SHORT, Locale("cs", "CZ"))
                return "$dayShort ${d.openTime}"
            }
        }

        return null
    }

    fun bindOpeningHours(data: OpeningHoursUi?) {
        if (data == null) {
            openingHoursContainer.visibility = View.GONE
            return
        }

        openingHoursContainer.visibility = View.VISIBLE

        val today = data.week.first { it.day == todayGoogleIndex() }
        val isOpenNow = today.isOpenNow()
        val nextLabel = data.nextOpeningLabelCs()

        openingHoursToday.text = when {
            isOpenNow -> getString(
                R.string.opening_open_today,
                today.openTime,
                today.closeTime
            )

            !today.isClosed() && nextLabel != null -> getString(
                R.string.opening_closed_opens,
                nextLabel
            )

            else -> getString(R.string.opening_today_closed)
        }

        val colorRes = if (isOpenNow) R.color.btn_admit_friend_request else R.color.black
        openingHoursToday.setTextColor(ContextCompat.getColor(requireContext(), colorRes))

        openingHoursList.removeAllViews()
        data.week.forEach { day ->
            val tv = TextView(requireContext()).apply {
                text = "${day.dayName}: ${day.timeRangeOrDash()}"
                textSize = 14f
                setPadding(4, 6, 4, 6)
            }
            openingHoursList.addView(tv)
        }

        openingHoursHeader.setOnClickListener {
            openingExpanded = !openingExpanded
            openingHoursList.visibility = if (openingExpanded) View.VISIBLE else View.GONE
            openingHoursArrow.animate().rotation(if (openingExpanded) 180f else 0f).setDuration(200).start()
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

}