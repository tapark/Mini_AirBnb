package com.example.mini_bnb

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import java.text.DecimalFormat

class HouseListAdapter(val itemClicked: (HouseModel) -> Unit): ListAdapter<HouseModel, HouseListAdapter.ViewHolder>(diffUtil) {

    inner class ViewHolder(private val view: View): RecyclerView.ViewHolder(view) {
        fun bind(houseModel: HouseModel) {
            val imageView = view.findViewById<ImageView>(R.id.thumbnailImageView)
            val titleTextView = view.findViewById<TextView>(R.id.titleTextView)
            val priceTextView = view.findViewById<TextView>(R.id.priceTextView)

            titleTextView.text = houseModel.title
            val decimalFormat = DecimalFormat("#,###")
            priceTextView.text = "${decimalFormat.format(houseModel.price)}원"
            Glide.with(imageView.context).load(houseModel.imgUrl)
                .transform(CenterCrop(), RoundedCorners(dpToPx(imageView.context, 12f).toInt()))
                .into(imageView)

            view.setOnClickListener {
                itemClicked(houseModel)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.item_house_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: HouseListAdapter.ViewHolder, position: Int) {
        holder.bind(currentList[position])
    }

    private fun dpToPx(context: Context, dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics)
    }

    companion object {
        val diffUtil = object: DiffUtil.ItemCallback<HouseModel>() {
            override fun areItemsTheSame(oldItem: HouseModel, newItem: HouseModel): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: HouseModel, newItem: HouseModel): Boolean {
                return oldItem == newItem
            }

        }
    }
}