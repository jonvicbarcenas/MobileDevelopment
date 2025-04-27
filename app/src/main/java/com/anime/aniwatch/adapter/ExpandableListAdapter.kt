package com.anime.aniwatch.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.anime.aniwatch.R

class ExpandableListAdapter(
    private val context: Context,
    private val data: HashMap<String, List<List<String>>>
) : BaseExpandableListAdapter() {

    private val headers = data.keys.toList()

    override fun getGroupCount(): Int = headers.size

    override fun getChildrenCount(groupPosition: Int): Int =
        data[headers[groupPosition]]?.size ?: 0

    override fun getGroup(groupPosition: Int): Any = headers[groupPosition]

    override fun getChild(groupPosition: Int, childPosition: Int): Any =
        data[headers[groupPosition]]!![childPosition]

    override fun getChildType(groupPosition: Int, childPosition: Int): Int {
        return when (headers[groupPosition]) {
            "Privacy Policy" -> 0
            "Terms of Service" -> 1
            "Third-party notices" -> 2

            else -> 3 // Developers
        }
    }


    override fun getChildTypeCount(): Int = 4

    override fun getGroupId(groupPosition: Int): Long = groupPosition.toLong()

    override fun getChildId(groupPosition: Int, childPosition: Int): Long =
        childPosition.toLong()

    override fun hasStableIds(): Boolean = false

    override fun getGroupView(
        groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup?
    ): View {
        val view = convertView ?: LayoutInflater.from(parent?.context)
            .inflate(android.R.layout.simple_expandable_list_item_1, parent, false)
        val textView = view.findViewById<TextView>(android.R.id.text1)
        textView.text = getGroup(groupPosition).toString()
        return view
    }

    override fun getChildView(
        groupPosition: Int, childPosition: Int, isLastChild: Boolean, convertView: View?, parent: ViewGroup?
    ): View {
        val groupTitle = getGroup(groupPosition).toString()
        val child = getChild(groupPosition, childPosition) as List<String>

        // Log the group and child data for debugging
        Log.d("ExpandableListAdapter", "Group: $groupTitle, Child: $child")

        return if (groupTitle == "Privacy Policy") {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.privacy_policy, parent, false)

            // Ensure child has at least 2 elements
            if (child.size >= 2) {
                val titleTextView: TextView = view.findViewById(R.id.policy_title)
                val descriptionTextView: TextView = view.findViewById(R.id.policyDescription)

                titleTextView.text = child[0]
                descriptionTextView.text = child[1]
            } else {
                // Handle the case where child data is not as expected
                Log.e("ExpandableListAdapter", "Child data for Privacy Policy is not valid: $child")
            }

            view
        }
       else if (groupTitle == "Terms of Service") {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.privacy_policy, parent, false)
            // Ensure child has at least 2 elements
            if (child.size >= 2) {
                val titleTextView: TextView = view.findViewById(R.id.policy_title)
                val descriptionTextView: TextView = view.findViewById(R.id.policyDescription)

                titleTextView.text = child[0]
                descriptionTextView.text = child[1]
            } else {
                // Handle the case where child data is not as expected
                Log.e("ExpandableListAdapter", "Child data for Privacy Policy is not valid: $child")
            }

            view
        }

        else if (groupTitle == "Third-party notices") {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.privacy_policy, parent, false)

            // Ensure child has at least 2 elements
            if (child.size >= 2) {
                val titleTextView: TextView = view.findViewById(R.id.policy_title)
                val descriptionTextView: TextView = view.findViewById(R.id.policyDescription)

                titleTextView.text = child[0]
                descriptionTextView.text = child[1]
            } else {
                // Handle the case where child data is not as expected
                Log.e("ExpandableListAdapter", "Child data for Privacy Policy is not valid: $child")
            }
            view
        }


        else {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.activity_developers, parent, false)

            // Ensure child has at least 2 elements
            if (child.size >= 2) {
                val personName: TextView = view.findViewById(R.id.personName)
                val personBio: TextView = view.findViewById(R.id.personBio)
                val personImage: ImageView = view.findViewById(R.id.imageView)
                val vision: TextView = view.findViewById(R.id.vision)

                val visionLabel: LinearLayout = view.findViewById(R.id.visionLabel)

                val aboutDevelopersLayout: LinearLayout = view.findViewById(R.id.aboutDevelopersLayout)


                // Ensure child has at least 2 elements
                if (child.size >= 2) {
                    personName.text = child[0]
                    personBio.text = child[1]
                    // Set visibility and image based on childPosition
                } else {
                    Log.e("ExpandableListAdapter", "Child data for Developers is not valid: $child")
                }

                if (childPosition == 0) {
                    aboutDevelopersLayout.visibility = View.VISIBLE
                    visionLabel.visibility = View.VISIBLE
                    vision.visibility = View.VISIBLE
                } else {
                    aboutDevelopersLayout.visibility = View.GONE
                    visionLabel.visibility = View.GONE
                    vision.visibility = View.GONE
                }

                // Set the image based on childPosition
                personImage.setImageResource(if (childPosition == 0) R.drawable.profile1 else R.drawable.profile2)
            } else {
                // Handle the case where child data is not as expected
                Log.e("ExpandableListAdapter", "Child data for Developers is not valid: $child")
            }

            view
        }
    }


    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }
}
