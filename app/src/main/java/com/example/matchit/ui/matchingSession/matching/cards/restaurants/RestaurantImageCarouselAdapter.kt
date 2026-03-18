package com.example.matchit.ui.matchingSession.matching.cards.restaurants

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.Button
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.example.matchit.R
import com.example.matchit.data.model.googlePlaces.PlacePhoto
import com.stfalcon.imageviewer.StfalconImageViewer
import com.bumptech.glide.request.target.Target



class RestaurantImageCarouselAdapter(private val context: Context, private val clickListener: RestaurantImageCarouselAdapter.OnItemClickListener, private var placesPhotos: List<PlacePhoto>)  :
RecyclerView.Adapter<RestaurantImageCarouselAdapter.ViewHolder>()
{

    class ViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.restaurant_carousel_image)
        val attributionsBtn: Button = view.findViewById(R.id.attributions_btn)

    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.restaurant_carousel_image, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: RestaurantImageCarouselAdapter.ViewHolder, position: Int) {
        val placePhoto: PlacePhoto = placesPhotos[position]

        Glide.with(holder.image)
            .load(placePhoto.url)
            .listener(object : RequestListener<Drawable> {
                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: com.bumptech.glide.request.target.Target<Drawable>?,
                    dataSource: com.bumptech.glide.load.DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    removePhoto(placePhoto)
                    return true
                }

            })
            .error(R.drawable.no_image_placeholder)
            .into(holder.image)

        holder.attributionsBtn.setOnClickListener {
            clickListener.onItemClicked(position, placePhoto)
        }

        holder.image.setOnClickListener{
            openImageViewer(holder, position)
        }

    }

    fun removePhoto(photo: PlacePhoto) {
        val idx = placesPhotos.indexOf(photo)
        if (idx == -1) return

        val mutable = placesPhotos.toMutableList()
        mutable.removeAt(idx)
        placesPhotos = mutable

        if (placesPhotos.isEmpty()) {
            notifyDataSetChanged()   // carousel zmizí bez pádu
        } else {
            notifyItemRemoved(idx)
        }
    }

    private fun openImageViewer(holder: RestaurantImageCarouselAdapter.ViewHolder, position: Int) {

        StfalconImageViewer.Builder<String>(context, placesPhotos.map { it.url }) { imageView, image ->
            Glide.with(holder.image)
                .load(image)
                .into(imageView)
        }
            .withStartPosition(position) // Start at the clicked image
            .withTransitionFrom(holder.image) // Smooth transition from the clicked ImageView
            .allowZooming(true) // Enable pinch-to-zoom
            .allowSwipeToDismiss(true) // Enable swipe-to-dismiss
            .show()
    }


    override fun getItemCount(): Int = placesPhotos.size

    fun setImages(cards: ArrayList<PlacePhoto>) {
        this.placesPhotos = cards
    }

    public interface OnItemClickListener {
        fun onItemClicked(position: Int, item: PlacePhoto)
    }

}