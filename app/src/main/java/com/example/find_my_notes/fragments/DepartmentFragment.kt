package com.example.find_my_notes.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.find_my_notes.activities.BaseActivity
import com.example.find_my_notes.R
import com.example.find_my_notes.adapters.DatabaseAdapter
import com.example.find_my_notes.adapters.NotesHomeRecycleAdapter
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import java.util.Locale

class DepartmentFragment : Fragment() {
    private var department: String? = null

    private lateinit var currentUser: FirebaseUser
    private lateinit var databaseRefUsers: DatabaseReference
    private lateinit var databaseRefDept: DatabaseReference

    private lateinit var fragmentManager: FragmentManager // FragmentManager

    private var layoutManager: RecyclerView.LayoutManager? = null
    private var rAdapter: NotesHomeRecycleAdapter? = null

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
        val view = inflater.inflate(R.layout.fragment_department, container, false)
        department = arguments?.getString("DEPARTMENT_KEY")

        val recyclerView = view.findViewById<RecyclerView>(R.id.deptRecyclerView)
        val editTextSearch = view.findViewById<EditText>(R.id.txtDeptSearchNote)
        val noNotesTextView = view.findViewById<TextView>(R.id.noNotesTextView)

        fragmentManager = requireActivity().supportFragmentManager

        currentUser = DatabaseAdapter.returnUser()!!
        databaseRefUsers = DatabaseAdapter.users

        databaseRefDept = DatabaseAdapter.dept

        layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        recyclerView.layoutManager = layoutManager
        rAdapter = NotesHomeRecycleAdapter(requireContext(), fragmentManager)
        recyclerView.adapter = rAdapter

        val noteList = ArrayList<String>()

//        if (isAdded) {
//            (requireContext() as BaseActivity).showProgressBar()
//        }
        // Fetch notes from the 'notes' table
        department?.let {
            databaseRefDept.child(it).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(notesSnapshot: DataSnapshot) {

                    for (noteSnapshot in notesSnapshot.children) {
                        val noteId = noteSnapshot.key.toString()
                        val noteName = noteSnapshot.child("title").value.toString()
                        val noteDesc = noteSnapshot.child("description").value.toString()
                        val noteUrlPDF = noteSnapshot.child("noteUrlPDF").value.toString()
                        val currentUid = noteSnapshot.child("currentUser").value.toString()
                        val timestamp = noteSnapshot.child("timestamp").value.toString()

                        // Fetch user information from the 'users' table based on 'currentuid'
                        databaseRefUsers.child(currentUid)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(userSnapshot: DataSnapshot) {
                                    val fullname = userSnapshot.child("fullname").value.toString()
                                    val email = userSnapshot.child("email").value.toString()
                                    val imageUrl = userSnapshot.child("imageUrl").value.toString()

                                    // Combine note and user information
                                    val combinedInfo =
                                        "$fullname\n$email\n$imageUrl\n$noteName\n$noteDesc\n$noteUrlPDF\n$timestamp\n$currentUid\n$noteId"
                                    noteList.add(combinedInfo)
                                    noNotesTextView?.visibility =
                                        View.GONE

                                    // Update adapter when all data is collected
                                    if (noteList.size == notesSnapshot.childrenCount.toInt()) {
//                                        if (isAdded) {
//                                            (requireContext() as BaseActivity).hideProgressBar()
//                                        }
                                        rAdapter?.updateData(noteList)
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
//                                    if (isAdded) {
//                                        (requireContext() as BaseActivity).hideProgressBar()
//                                    }
                                    if (isAdded) {
                                        Utils.showToast(
                                            requireContext(),
                                            "Failed to Load Data: ${error.message}"
                                        )
                                    }
                                }
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
//                    if (isAdded) {
//                        (requireContext() as BaseActivity).hideProgressBar()
//                    }
                    if (isAdded) {
                        Utils.showToast(
                            requireContext(),
                            "Failed to Load Data: ${error.message}"
                        )
                    }
                }
            })
        }


        // Set up text change listener for search functionality
        editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No implementation needed
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Filter notes based on the search query if noteList is not empty
                val searchQuery = s.toString().trim()
                val filteredList = ArrayList<String>()
                if (noteList.isNotEmpty()) {
                    for (note in noteList) {
                        if (note.toLowerCase(Locale.getDefault())
                                .contains(searchQuery.toLowerCase(Locale.getDefault()))
                        ) {
                            filteredList.add(note)
                        }
                    }
                }
                rAdapter!!.updateData(filteredList)
            }

            override fun afterTextChanged(s: Editable?) {
                // No implementation needed
            }
        })
        return view
    }


    override fun onResume() {
        super.onResume()
        requireActivity().title = "$department Notes" // Set the toolbar title
    }
}