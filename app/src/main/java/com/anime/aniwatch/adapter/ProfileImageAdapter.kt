package com.anime.aniwatch.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import de.hdodenhof.circleimageview.CircleImageView

class ProfileImageAdapter(private val context: Context, private val imageIds: Array<Int>) : BaseAdapter() {

    override fun getCount(): Int {
        return imageIds.size  // Ensure this returns the total number of items
    }

    override fun getItem(position: Int): Any {
        return imageIds[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val imageView: CircleImageView

        if (convertView == null) {
            imageView = CircleImageView(context)
            imageView.layoutParams = ViewGroup.LayoutParams(200, 200) // Size for grid items
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        } else {
            imageView = convertView as CircleImageView
        }

        imageView.setImageResource(imageIds[position])

        return imageView
    }
}
