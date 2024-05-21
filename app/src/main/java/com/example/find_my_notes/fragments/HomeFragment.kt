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

class HomeFragment : Fragment() {

    private lateinit var currentUser: FirebaseUser
    private lateinit var databaseRefUsers: DatabaseReference
    private lateinit var databaseRefNotes: DatabaseReference

    private lateinit var fragmentManager: FragmentManager // FragmentManager

    private var layoutManager: RecyclerView.LayoutManager? = null
    private var rAdapter: NotesHomeRecycleAdapter? = null

    private var backPressedTime: Long = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.homeRecyclerView)
        val editTextSearch = view.findViewById<EditText>(R.id.txtHomeSearchNote)

        val cvHomeBca = view.findViewById<CardView>(R.id.cvHomeBca)
        val cvHomeMca = view.findViewById<CardView>(R.id.cvHomeMca)
        val cvHomeBscit = view.findViewById<CardView>(R.id.cvHomeBscit)
        val cvHomeMscit = view.findViewById<CardView>(R.id.cvHomeMscit)

        fragmentManager = requireActivity().supportFragmentManager

        currentUser = DatabaseAdapter.returnUser()!!
        databaseRefUsers = DatabaseAdapter.users

        databaseRefNotes = DatabaseAdapter.notes

        layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        recyclerView.layoutManager = layoutManager
        rAdapter = NotesHomeRecycleAdapter(requireContext(), fragmentManager)
        recyclerView.adapter = rAdapter

        val noteList = ArrayList<String>()


        if (isAdded) {
//            (requireContext() as BaseActivity).showProgressBar()
        }
        // Fetch notes from the 'notes' table
        databaseRefNotes.addListenerForSingleValueEvent(object : ValueEventListener {
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
                                    "$fullname\n$email\n$imageUrl\n$noteName\n$noteDesc\n$noteUrlPDF\n$timestamp\n$currentUid\n" +
                                            "$noteId"
                                noteList.add(combinedInfo)

                                // Update adapter when all data is collected
                                if (noteList.size == notesSnapshot.childrenCount.toInt()) {
                                    if (isAdded) {
//                                        (requireContext() as BaseActivity).hideProgressBar()
                                    }
                                    noteList.shuffle()
                                    rAdapter?.updateData(noteList)
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                if (isAdded) {
//                                    (requireContext() as BaseActivity).hideProgressBar()
                                }
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
                if (isAdded) {
//                    (requireContext() as BaseActivity).hideProgressBar()
                }
                if (isAdded) {
                    Utils.showToast(
                        requireContext(),
                        "Failed to Load Data: ${error.message}"
                    )
                }
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

        //Find Notes Department Wise
        cvHomeBca.setOnClickListener {
            navigateToDepartmentFragment("BCA")
        }

        cvHomeMca.setOnClickListener {
            navigateToDepartmentFragment("MCA")
        }

        cvHomeBscit.setOnClickListener {
            navigateToDepartmentFragment("BScit")
        }

        cvHomeMscit.setOnClickListener {
            navigateToDepartmentFragment("MScit")
        }

        return view
    }

    private fun navigateToDepartmentFragment(department: String) {
        val fragment = DepartmentFragment()
        val bundle = Bundle()
        bundle.putString("DEPARTMENT_KEY", department)
        fragment.arguments = bundle

        fragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
//            .addToBackStack(null)
            .commit()
    }

    override fun onResume() {
        super.onResume()
        requireActivity().title = "Home" // Set the toolbar title

        val bottomNavigationView = requireActivity().findViewById<View>(R.id.bottomNavigation)
        bottomNavigationView.visibility = View.VISIBLE

        //Find the TextViews and ImageView from the layout
        val txtUserCName = view?.findViewById<TextView>(R.id.txtHomeCUser)

        databaseRefUsers = DatabaseAdapter.users
        currentUser = DatabaseAdapter.returnUser()!!

        // Retrieve the username from the database
        databaseRefUsers.child(currentUser.uid).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val username = dataSnapshot.child("fullname").value.toString()
                if (txtUserCName != null) {
                    txtUserCName.text = "Hello, $username !!"
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                if (txtUserCName != null) {
                    txtUserCName.text = "Welcome to My App"
                }
            }
        })
        backPressedTime = 0
    }
}

