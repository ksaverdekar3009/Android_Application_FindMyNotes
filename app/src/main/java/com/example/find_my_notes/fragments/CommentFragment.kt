package com.example.find_my_notes.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.find_my_notes.R
import com.example.find_my_notes.activities.BaseActivity
import com.example.find_my_notes.adapters.CommentAdapter
import com.example.find_my_notes.adapters.DatabaseAdapter
import com.example.find_my_notes.databinding.FragmentCommentBinding
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener


class CommentFragment : Fragment() {
    private var b: FragmentCommentBinding? = null

    private var noteId: String? = null

    private lateinit var fragmentManager: FragmentManager // FragmentManager
    private lateinit var currentUser: FirebaseUser
    private lateinit var dbNotesRef: DatabaseReference
    private lateinit var dbMyNotesRef: DatabaseReference
    private lateinit var dbCommentRef: DatabaseReference


    private var layoutManager: RecyclerView.LayoutManager? = null
    private var rAdapter: CommentAdapter? = null

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
        b = FragmentCommentBinding.inflate(inflater, container, false)
        val recyclerView = b?.commentRecyclerView
        noteId = arguments?.getString("noteId")

        fragmentManager = requireActivity().supportFragmentManager

        currentUser = DatabaseAdapter.returnUser()!!
        dbNotesRef = DatabaseAdapter.notes
        dbMyNotesRef = DatabaseAdapter.myNotes
        dbCommentRef = noteId?.let { DatabaseAdapter.comments.child(it) }!!

        layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        recyclerView?.layoutManager = layoutManager
        rAdapter = CommentAdapter(requireContext(), fragmentManager)
        recyclerView?.adapter = rAdapter

//        val commentList = ArrayList<String>()

        // Fetch comments from the 'comments' table
        dbCommentRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(commentsSnapshot: DataSnapshot) {
                val commentList = ArrayList<String>()

                for (commentSnapshot in commentsSnapshot.children) {
                    val commentText = commentSnapshot.child("comment").value.toString()
                    val commenterUid = commentSnapshot.child("commenterUid").value.toString()

                    // Fetch user details using commenterUid
                    DatabaseAdapter.users.child(commenterUid).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(userSnapshot: DataSnapshot) {
                            val userName = userSnapshot.child("fullname").value.toString()
                            val userImageUrl = userSnapshot.child("imageUrl").value.toString()

                            // Combine comment information
                            val commentInfo = "$commentText\n$userName\n$userImageUrl"

                            commentList.add(commentInfo)
                            rAdapter?.updateData(commentList)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Handle error
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })


        b?.txtSend?.setOnClickListener {
            val edtxtComment = b?.edtxtComment?.text.toString().trim()
            if (edtxtComment.isEmpty()) {
                if (isAdded) {
                    (requireActivity() as BaseActivity).showToast(
                        requireContext(),
                        "Please Write Your Comment"
                    )
                }
            } else {
                val commentsMap = HashMap<String, Any>()
                commentsMap["comment"] = edtxtComment
                commentsMap["commenterUid"] = currentUser.uid

                dbCommentRef.push().setValue(commentsMap).addOnSuccessListener {
                    val fragment = CommentFragment()
                    val bundle = Bundle()
                    // Add data to the bundle
                    bundle.putString("noteId", noteId)
                    fragment.arguments = bundle

                    val transaction = fragmentManager.beginTransaction()
                    transaction.replace(R.id.fragment_container, fragment)
//                    transaction.addToBackStack(null)
                    transaction.commit()
                    if (isAdded) {
                        (requireActivity() as BaseActivity).showToast(
                            requireContext(),
                            "Comment done Successfully"
                        )
                    }
                }
                    .addOnFailureListener { exception ->
                        if (isAdded) {
                            (requireActivity() as BaseActivity).showToast(
                                requireContext(),
                                "Failed to Upload Comment: ${exception.message}"
                            )
                        }
                    }
                b?.edtxtComment?.text?.clear()
            }
        }


        return b?.root
    }

    override fun onResume() {
        super.onResume()
        requireActivity().title = "Add Comment" // Set the toolbar title
//
//        // Hide the bottom navigation (assuming it has an ID like `bottomNavigationView`)
//        val bottomNavigationView = requireActivity().findViewById<View>(R.id.bottomNavigation)
//        bottomNavigationView.visibility = View.GONE
    }
}