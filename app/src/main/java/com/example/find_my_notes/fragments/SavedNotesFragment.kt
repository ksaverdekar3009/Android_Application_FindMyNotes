package com.example.find_my_notes.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.find_my_notes.R
import com.example.find_my_notes.adapters.DatabaseAdapter
import com.example.find_my_notes.adapters.NotesHomeRecycleAdapter
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import java.util.Locale


class SavedNotesFragment : Fragment() {
    private lateinit var currentUser: FirebaseUser
    private lateinit var databaseRefUsers: DatabaseReference
    private lateinit var databaseRefNotes: DatabaseReference
    private lateinit var databaseRefSaves: DatabaseReference
    private lateinit var fragmentManager: FragmentManager // FragmentManager
    private var layoutManager: RecyclerView.LayoutManager? = null
    private var rAdapter: NotesHomeRecycleAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_saved_notes, container, false)
        val noNotesTextView = view.findViewById<TextView>(R.id.noNotesTextView)
        val recyclerView = view.findViewById<RecyclerView>(R.id.savedNotesRecyclerView)
        val editTextSearch = view.findViewById<EditText>(R.id.txtSavedNotesSearch)

        fragmentManager = requireActivity().supportFragmentManager

        currentUser = DatabaseAdapter.returnUser()!!
        databaseRefUsers = DatabaseAdapter.users
        databaseRefSaves = DatabaseAdapter.saves

        databaseRefNotes = DatabaseAdapter.notes

        layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        recyclerView.layoutManager = layoutManager
        rAdapter = NotesHomeRecycleAdapter(requireContext(), fragmentManager)
        recyclerView.adapter = rAdapter

        val currentUserUid = currentUser.uid // Get the UID of the current user
        val noteList = ArrayList<String>()


        // Query to fetch noteIds where the current user's ID exists with a value of true
        val savesQuery = databaseRefSaves.orderByChild(currentUserUid).equalTo(true)

        savesQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(savesSnapshot: DataSnapshot) {
                val noteIdsList = ArrayList<String>()

                for (saveSnapshot in savesSnapshot.children) {
                    val noteId = saveSnapshot.key.toString()
                    noteIdsList.add(noteId)
                }

                // Log note IDs
                Log.d("NoteIds", "Note IDs: $noteIdsList")

                // Fetch notes from the 'notes' table based on the noteIdsList
                val totalNotesToFetch = noteIdsList.size

                for (noteId in noteIdsList) {
                    databaseRefNotes.child(noteId)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(noteSnapshot: DataSnapshot) {
                                val noteName = noteSnapshot.child("title").value.toString()
                                val noteDesc = noteSnapshot.child("description").value.toString()
                                val noteUrlPDF = noteSnapshot.child("noteUrlPDF").value.toString()
                                val currentUid = noteSnapshot.child("currentUser").value.toString()
                                val timestamp = noteSnapshot.child("timestamp").value.toString()

                                // Fetch user information from the 'users' table based on 'currentuid'
                                databaseRefUsers.child(currentUid)
                                    .addListenerForSingleValueEvent(object :
                                        ValueEventListener {
                                        override fun onDataChange(userSnapshot: DataSnapshot) {
                                            val fullname =
                                                userSnapshot.child("fullname").value.toString()
                                            val email =
                                                userSnapshot.child("email").value.toString()
                                            val imageUrl =
                                                userSnapshot.child("imageUrl").value.toString()

                                            // Combine note and user information
                                            val combinedInfo =
                                                "$fullname\n$email\n$imageUrl\n$noteName\n$noteDesc\n$noteUrlPDF\n$timestamp\n$currentUid\n$noteId"
                                            noteList.add(combinedInfo)

                                            if (noteList.size == totalNotesToFetch) {
                                                // All notes for the current user have been fetched
                                                if (noteList.isNotEmpty()) {
                                                    noNotesTextView.visibility =
                                                        View.GONE // Hide message if list is not empty
                                                    rAdapter?.updateData(noteList)
                                                } else {
                                                    // Show message that there are no notes
                                                    noNotesTextView.visibility = View.VISIBLE
                                                }
                                            }
                                            // Log notes
                                            Log.d("Notes", "Notes: $noteList")
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            // Handle onCancelled
                                        }
                                    })

                            }

                            override fun onCancelled(error: DatabaseError) {
                                // Handle onCancelled
                            }
                        })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle onCancelled
            }
        })


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
        requireActivity().title = "Favorites" // Set the toolbar title

        // Hide the bottom navigation
        val bottomNavigationView = requireActivity().findViewById<View>(R.id.bottomNavigation)
        bottomNavigationView.visibility = View.GONE
    }
}