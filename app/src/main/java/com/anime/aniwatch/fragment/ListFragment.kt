package com.anime.aniwatch.fragment

import android.os.Bundle
import android.view.*
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.anime.aniwatch.R
import com.anime.aniwatch.adapter.FragmentAdapter
import com.anime.aniwatch.fragment.ScheduleFragment
import com.google.android.material.tabs.TabLayout

class ListFragment : Fragment() {

    private var viewPager: ViewPager? = null
    private var tabLayout: TabLayout? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = activity as? AppCompatActivity
        activity?.supportActionBar?.apply {
            show()
            title = "My List"
        }


        viewPager = view.findViewById(R.id.viewPager)
        tabLayout = view.findViewById(R.id.tabLayout)


        val fragmentAdapter = FragmentAdapter(childFragmentManager)
        fragmentAdapter.addFragment(WatchlistFragment(), "Watchlist")
        fragmentAdapter.addFragment(HistoryFragment(), "History")
        fragmentAdapter.addFragment(ScheduleFragment(), "Schedule")

        viewPager?.adapter = fragmentAdapter
        tabLayout?.setupWithViewPager(viewPager)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Clean up resources to prevent memory leaks
        viewPager?.adapter = null
        viewPager = null
        tabLayout = null
        val activity = activity as? AppCompatActivity

    }
}
