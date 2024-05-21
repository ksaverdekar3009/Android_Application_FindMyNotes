package com.example.find_my_notes.fragments

import UserSearchRecycleAdapter
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.find_my_notes.activities.BaseActivity
import com.example.find_my_notes.R
import com.example.find_my_notes.adapters.DatabaseAdapter
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import java.util.Locale

class SearchFragment : Fragment() {

    private lateinit var currentUser: FirebaseUser
    private lateinit var databaseReference: DatabaseReference
    private lateinit var fragmentManager: FragmentManager // FragmentManager

    private var layoutManager: RecyclerView.LayoutManager? = null
    private var rAdapter: UserSearchRecycleAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.searchRecyclerView)
        val searchBox = view.findViewById<EditText>(R.id.sBox)

        fragmentManager = requireActivity().supportFragmentManager
        currentUser = DatabaseAdapter.returnUser()!!
        databaseReference = DatabaseAdapter.users

        layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        recyclerView.layoutManager = layoutManager
        rAdapter = UserSearchRecycleAdapter(requireContext(), fragmentManager) // Initialize adapter
        recyclerView.adapter = rAdapter

        val userList = ArrayList<String>()

//        if (isAdded){ (requireActivity() as BaseActivity).showProgressBar() }
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (userSnapshot in snapshot.children) {
                    if (currentUser.uid != userSnapshot.key) {
                        // Extract user data and add to the list
                        val fullname = userSnapshot.child("fullname").value.toString()
                        val userid = userSnapshot.key.toString()
                        val email = userSnapshot.child("email").value.toString()
                        val dept = userSnapshot.child("program").value.toString()
                        val imageUrl = userSnapshot.child("imageUrl").value.toString()
                        userList.add("$fullname\n$email\n$dept\n$imageUrl\n$userid")
//                        if(isAdded){ (requireActivity() as BaseActivity).hideProgressBar() }
                    }
                }
                userList.shuffle()
                rAdapter?.updateData(userList)
            }
            override fun onCancelled(error: DatabaseError) {
                if(isAdded){
                    (requireActivity() as BaseActivity).showToast(
                        requireContext(),
                        "Failed to update profile: ${error.message}"
                    )
//                    (requireActivity() as BaseActivity).hideProgressBar()
                }
            }
        })

        // Set up text change listener for search functionality
        searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No implementation needed
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Filter notes based on the search query if noteList is not empty
                val searchQuery = s.toString().trim()
                val filteredList = ArrayList<String>()
                if (userList.isNotEmpty()) {
                    for (note in userList) {
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
        requireActivity().title = "Search User"
    }
}
