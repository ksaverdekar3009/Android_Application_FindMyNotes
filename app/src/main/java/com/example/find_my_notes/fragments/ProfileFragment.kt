package com.example.find_my_notes.fragments

import ProfilePagerAdapter
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.example.find_my_notes.R
import com.example.find_my_notes.adapters.DatabaseAdapter
import com.example.find_my_notes.databinding.FragmentProfileBinding
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso

class ProfileFragment : Fragment() {

    private var b: FragmentProfileBinding? = null

    private lateinit var currentUser: FirebaseUser
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        b = FragmentProfileBinding.inflate(inflater, container, false)
        b?.btnEditProfile?.setOnClickListener {
            val transaction = parentFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, EditProfileFragment())
//            transaction.addToBackStack(null)
            transaction.commit()
        }

        currentUser = DatabaseAdapter.returnUser()!!
        databaseReference = DatabaseAdapter.users

        // Retrieve the details from the database
        databaseReference.child(currentUser.uid).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val imageUrl = dataSnapshot.child("imageUrl").value.toString()
                b!!.txtProFullName.text = dataSnapshot.child("fullname").value.toString()
                b!!.txtProEmail.text =  dataSnapshot.child("email").value.toString()

                // Load image from the URL into navProfilePic ImageView
                Picasso.get().load(imageUrl).placeholder(R.drawable.profile_pic).into(b!!.imgProfile)

                // Set click listener for profile image
                        b!!.imgProfile.setOnClickListener {
                            // Inflate dialog layout
                            val dialogView = LayoutInflater.from(requireContext())
                                .inflate(R.layout.dialog_image_viewer, null)
                            val dialogImageView = dialogView.findViewById<ImageView>(R.id.imageView)

                            // Load image using Picasso into the dialog's ImageView
                            Picasso.get().load(imageUrl)
                                .placeholder(R.drawable.profile_pic)
                                .into(dialogImageView)

                            // Create and show the dialog
                            val builder = AlertDialog.Builder(requireContext())
                            builder.setView(dialogView)
                            builder.setPositiveButton("Close") { dialog, _ ->
                                dialog.dismiss()
                            }
                            val dialog = builder.create()
                            dialog.show()
                        }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                b!!.txtProFullName.text = ""
                b!!.txtProEmail.text = ""
                b!!.imgProfile.setImageResource(R.drawable.profile_pic)
            }
        })

        return b?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tabLayout = b?.tbTablayout
        val viewPager = b?.vpViewpager2

        viewPager?.adapter = ProfilePagerAdapter(this)

        TabLayoutMediator(tabLayout!!, viewPager!!) { tab, position ->
            when(position){
            0-> tab.text="My Notes"
            1-> tab.text="My Info"
        }
        }.attach()
    }

    override fun onResume() {
        super.onResume()
        requireActivity().title = "Profile" // Set the toolbar title
    }
}