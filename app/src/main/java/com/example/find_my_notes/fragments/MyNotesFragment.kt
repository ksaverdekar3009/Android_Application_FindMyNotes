package com.example.find_my_notes.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.find_my_notes.activities.BaseActivity
import com.example.find_my_notes.R
import com.example.find_my_notes.adapters.DatabaseAdapter
import com.example.find_my_notes.adapters.MyNotesInfoRecycleAdapter
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener


class MyNotesFragment : Fragment() {

    private lateinit var currentUser: FirebaseUser
    private lateinit var databaseReference: DatabaseReference
    private lateinit var fragmentManager: FragmentManager // FragmentManager

    private var layoutManager: RecyclerView.LayoutManager? = null
    private var rAdapter: MyNotesInfoRecycleAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_my_notes, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.mynotesInfoRecyclerView)
        val noNotesTextView = view.findViewById<TextView>(R.id.noNotesTextView)

        fragmentManager = requireActivity().supportFragmentManager

        currentUser = DatabaseAdapter.returnUser()!!
        databaseReference = DatabaseAdapter.myNotes

        layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        recyclerView.layoutManager = layoutManager
        rAdapter = MyNotesInfoRecycleAdapter(requireContext(),fragmentManager) // Initialize adapter
        recyclerView.adapter = rAdapter

        val myNotesList = ArrayList<String>()

        databaseReference.child(currentUser.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (userSnapshot in snapshot.children) {
                        // Extract user data and add to the list
                        val notedId = userSnapshot.key
                        val noteName = userSnapshot.child("title").value.toString()
                        val noteDes = userSnapshot.child("description").value.toString()
                        val noteUrlPDF = userSnapshot.child("noteUrlPDF").value.toString()
                        val timestamp = userSnapshot.child("timestamp").value.toString()
                        myNotesList.add("$noteName\n$noteDes\n$timestamp\n$noteUrlPDF\n" +
                                "$notedId")

                        noNotesTextView.visibility = View.GONE // Hide message if list is not empty
                    }
                    rAdapter?.updateData(myNotesList)
                }

                override fun onCancelled(error: DatabaseError) {
                    if (isAdded) {
                        (requireActivity() as BaseActivity).showToast(
                            requireContext(),
                            "Failed to update profile: ${error.message}"
                        )
                    }
                }
            })

        return view
    }
}