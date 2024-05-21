package com.example.find_my_notes.fragments

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.find_my_notes.activities.BaseActivity
import com.example.find_my_notes.R
import com.example.find_my_notes.adapters.DatabaseAdapter
import com.example.find_my_notes.adapters.MyNotesInfoRecycleAdapter
import com.example.find_my_notes.databinding.FragmentUserProfileBinding
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserProfileFragment : Fragment() {

    private var b: FragmentUserProfileBinding? = null
    private lateinit var fragmentManager: FragmentManager // FragmentManager

    private lateinit var currentUser: FirebaseUser
    private lateinit var databaseRefUser: DatabaseReference
    private lateinit var databaseRefNotes: DatabaseReference
    private var userId: String? = null

    private var layoutManager: RecyclerView.LayoutManager? = null
    private var rAdapter: MyNotesInfoRecycleAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getString("USER_ID_KEY") // Retrieve user ID from arguments
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        b = FragmentUserProfileBinding.inflate(inflater, container, false)
        val recyclerView = b!!.userNotesRecyclerView

        fragmentManager = requireActivity().supportFragmentManager

        currentUser = DatabaseAdapter.returnUser()!!
        databaseRefUser = DatabaseAdapter.users

//        if (isAdded) {
//            (requireActivity() as BaseActivity).showProgressBar()
//        }
        // Retrieve the details from the database
        userId?.let {
            databaseRefUser.child(it).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    b!!.txtUserFullName.text = dataSnapshot.child("fullname").value.toString()
                    b!!.txtUserEmail.text = dataSnapshot.child("email").value.toString()
                    b!!.txtUserProg.text = dataSnapshot.child("program").value.toString()
                    val imageUrl = dataSnapshot.child("imageUrl").value.toString()

                    // Load image using Picasso
                    Picasso.get().load(imageUrl).placeholder(R.drawable.profile_pic)
                        .into(b!!.imgUserProfilePic)
//                    if (isAdded) {
//                        (requireActivity() as BaseActivity).hideProgressBar()
//                    }

                    // Set click listener for profile image
                    b!!.imgUserProfilePic.setOnClickListener {
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
                    b!!.txtUserFullName.text = ""
                    b!!.txtUserEmail.text = ""
                    b!!.txtUserProg.text = ""
//                    if (isAdded) {
//                        (requireActivity() as BaseActivity).hideProgressBar()
//                    }
                }
            })
        }

        //Recycleview
        databaseRefNotes = DatabaseAdapter.myNotes

        layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        recyclerView.layoutManager = layoutManager
        if (isAdded) {
            rAdapter = MyNotesInfoRecycleAdapter(requireContext(),fragmentManager) // Initialize adapter
        }
        recyclerView.adapter = rAdapter


        userId?.let {
            databaseRefNotes.child(it).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val myNotesList = ArrayList<String>()

                    for (userSnapshot in snapshot.children) {
                        // Extract user data and add to the list
                        val notedId = userSnapshot.key
                        val noteName = userSnapshot.child("title").value.toString()
                        val noteDes = userSnapshot.child("description").value.toString()
                        val noteUrlPDF = userSnapshot.child("noteUrlPDF").value.toString()
                        val timestamp = userSnapshot.child("timestamp").value.toString()
                        myNotesList.add("$noteName\n$noteDes\n$timestamp\n$noteUrlPDF\n$notedId")

                        b?.noNotesTextView?.visibility =
                            View.GONE // Hide message if list is not empty

                    }
                    rAdapter?.updateData(myNotesList)
                }

                override fun onCancelled(error: DatabaseError) {
                    if(isAdded){
                    (requireActivity() as BaseActivity).showToast(
                        requireContext(),
                        "Failed try again later: ${error.message}"
                    )}
                }
            })
        }

        // Inside setOnClickListener of txtUserReport
        b?.txtUserReport?.setOnClickListener {
            // Create an AlertDialog to confirm the report
            AlertDialog.Builder(context)
                .setTitle("Report User")
                .setMessage("Are you sure you want to report this user?")
                .setPositiveButton("Yes") { dialog, _ ->
                    // Assuming you have the necessary information to add a user report
                    val reportData = HashMap<String, Any>()
                    reportData["userId"] = userId.toString()
                    reportData["reporterId"] = currentUser.uid
                    reportData["timestamp"] = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                        Date()
                    )

                    // Assuming you have a reference to the userReports node in the Firebase database
                    val userReportsRef = DatabaseAdapter.userReports

                    if (userId != currentUser.uid) {
                        // Push the report data to the userReports node
                        userReportsRef.child(userId.toString()).push().setValue(reportData)
                            .addOnSuccessListener {
                                if(isAdded){
                                    (requireActivity() as BaseActivity).showToast(
                                        requireContext(),
                                        "User Reported Successfully"
                                    )}
                            }
                            .addOnFailureListener { e ->
                                if(isAdded){
                                    (requireActivity() as BaseActivity).showToast(
                                        requireContext(),
                                        "Failed to report this user: ${e.message}"
                                    )}
                            }
                    } else {
                        if(isAdded){
                            (requireActivity() as BaseActivity).showToast(
                                requireContext(),
                                "You cannot report yourself."
                            )}
                    }
                }
                .setNegativeButton("No") { dialog, _ ->
                    // User canceled the report
                    dialog.dismiss()
                }
                .show()
        }

        return b?.root
    }

    override fun onResume() {
        super.onResume()
        requireActivity().title = "User Profile"
    }
}
