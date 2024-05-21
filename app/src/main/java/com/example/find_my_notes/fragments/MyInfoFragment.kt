package com.example.find_my_notes.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.find_my_notes.adapters.DatabaseAdapter
import com.example.find_my_notes.databinding.FragmentMyInfoBinding
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener

class MyInfoFragment : Fragment() {

    private var b: FragmentMyInfoBinding? = null
    private lateinit var currentUser: FirebaseUser
    private lateinit var databaseReference: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        b = FragmentMyInfoBinding.inflate(inflater, container, false)

        currentUser = DatabaseAdapter.returnUser()!!
        databaseReference = DatabaseAdapter.users

//        (requireActivity() as BaseActivity).showProgressBar()
        // Retrieve the details from the database
        databaseReference.child(currentUser.uid).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                b!!.txtInfoName.text = dataSnapshot.child("fullname").value.toString()
                b!!.txtInfoEmail.text = dataSnapshot.child("email").value.toString()
                b!!.txtInfoGender.text = dataSnapshot.child("gender").value.toString()
                b!!.txtInfoPhone.text = dataSnapshot.child("mobile").value.toString()
                b!!.txtInfoProgram.text = dataSnapshot.child("program").value.toString()
//                (requireActivity() as BaseActivity).hideProgressBar()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                b!!.txtInfoName.text = ""
                b!!.txtInfoEmail.text = ""
                b!!.txtInfoGender.text = ""
                b!!.txtInfoPhone.text = ""
                b!!.txtInfoProgram.text = ""
//                (requireActivity() as BaseActivity).hideProgressBar()
            }
        })
        return b?.root
    }
}
