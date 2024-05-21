package com.example.find_my_notes.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.find_my_notes.R

class AboutUsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_about_us, container, false)
    }

    override fun onResume() {
        super.onResume()
        requireActivity().title = "About Us" // Set the toolbar title

        // Hide the bottom navigation
        val bottomNavigationView = requireActivity().findViewById<View>(R.id.bottomNavigation)
        bottomNavigationView.visibility = View.GONE
    }
}