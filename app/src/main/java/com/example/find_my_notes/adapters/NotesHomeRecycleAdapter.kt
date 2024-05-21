package com.example.find_my_notes.adapters

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.find_my_notes.R
import com.example.find_my_notes.fragments.CommentFragment
import com.example.find_my_notes.fragments.EditNotesBlankFragment
import com.example.find_my_notes.fragments.ProfileFragment
import com.example.find_my_notes.fragments.UserProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotesHomeRecycleAdapter(
    private val context: Context,
    private val fragmentManager: FragmentManager
) :
    RecyclerView.Adapter<NotesHomeRecycleAdapter.ViewHolder>() {

    private var noteList: List<String> = ArrayList()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgHomeUser: ImageView = itemView.findViewById(R.id.imgHomeUser)
        val txtHomeUserName: TextView = itemView.findViewById(R.id.txtHomeUserName)
        val txtHomeUserEmail: TextView = itemView.findViewById(R.id.txtHomeUserEmail)
        val txtHomeNoteName: TextView = itemView.findViewById(R.id.txtHomeNoteName)
        val txtHomeNoteDescription: TextView = itemView.findViewById(R.id.txtHomeNoteDescription)
        val txtHomeNoteTimestamp: TextView = itemView.findViewById(R.id.txtHomeUserTimestamp)
        val txtLike: TextView = itemView.findViewById(R.id.txtLike)
        val txtComment: TextView = itemView.findViewById(R.id.txtComment)
        val txtSave: TextView = itemView.findViewById(R.id.txtSave)
        val txtNotesReport: TextView = itemView.findViewById(R.id.txtNotesReport)

        //        val bottomNavigation: BottomNavigationView = itemView.findViewById(R.id.bottomNavigation)
        private val cvHomeDownloadPDF: CardView = itemView.findViewById(R.id.cvHomeDownloadPDF)
        private val linearSearchUser: LinearLayout = itemView.findViewById(R.id.linearSearchUser)

        init {
            //Open User Profile
            linearSearchUser.setOnClickListener {
                val userInfo = noteList[adapterPosition].split("\n")
                val userId = userInfo[7] // Assuming user ID is stored at index 7
                val currentUser = DatabaseAdapter.getCurrentUserId()

                // Check if the clicked user is the current user
                if (userId == currentUser) {
                    // If the clicked user is the current user, navigate to the user's own profile fragment
                    fragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, ProfileFragment())
//                        .addToBackStack(null)
                        .commit()
                } else {
                    // If the clicked user is not the current user, navigate to UserProfileFragment for the clicked user
                    val fragment = UserProfileFragment()
                    val bundle = Bundle()
                    bundle.putString("USER_ID_KEY", userId)
                    fragment.arguments = bundle
                    fragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, fragment)
//                        .addToBackStack(null)
                        .commit()
                }
            }


            //Download PDF
            cvHomeDownloadPDF.setOnClickListener {
                val noteData = noteList[adapterPosition].split("\n")
                val noteName = noteData[3]
                val pdfUrl = noteData[5] // Assuming the PDF URL is at index 5

                // Check if the PDF URL is valid
                if (pdfUrl.isNotEmpty()) {
                    // Initiate PDF download (Using DownloadManager)
                    val downloadManager =
                        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val request = DownloadManager.Request(Uri.parse(pdfUrl))
                    request.setTitle("PDF Download")
                    request.setDescription("Downloading PDF")
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    request.setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        "$noteName.pdf"
                    ) // Change filename if needed
                    request.setAllowedOverMetered(true) // Allow download over mobile network
                    request.setAllowedOverRoaming(true) // Allow download while roaming
                    val downloadId = downloadManager.enqueue(request)

                    // Start an activity to view the downloaded PDF
                    val viewPdfIntent = Intent(Intent.ACTION_VIEW)
                    viewPdfIntent.setDataAndType(Uri.parse(pdfUrl), "application/pdf")
                    viewPdfIntent.flags =
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    try {
                        context.startActivity(viewPdfIntent)
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(context, "No PDF viewer installed", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    Toast.makeText(context, "Invalid PDF URL", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.notes_search_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val noteData = noteList[position].split("\n")
        holder.txtHomeUserName.text = noteData[0] // Fullname
        holder.txtHomeUserEmail.text = noteData[1] // Email
        val userId = noteData[7]

        // Load image using Picasso
        Picasso.get()
            .load(noteData[2]) // Image URL
            .placeholder(R.drawable.profile_pic)
            .into(holder.imgHomeUser)

        holder.txtHomeNoteName.text = noteData[3] // Note Name
        holder.txtHomeNoteDescription.text = noteData[4] // Note Description
        holder.txtHomeNoteTimestamp.text = noteData[6]
        val noteId = noteData[8]

        if (userId == DatabaseAdapter.getCurrentUserId()) {
            Log.d("Visibility", "Hiding txtNotesReport")
            holder.txtNotesReport.visibility = View.GONE
        } else {
            Log.d("Visibility", "Showing txtNotesReport")
            holder.txtNotesReport.visibility = View.VISIBLE
        }

        // Inside onBindViewHolder method of MyNotesInfoRecycleAdapter
        holder.txtNotesReport.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle("Report Note")
                .setMessage("Are you sure you want to report this note?")
                .setPositiveButton("Yes") { dialog, _ ->
                    if (userId != DatabaseAdapter.getCurrentUserId()) {
                        // Report the note
                        DatabaseAdapter.getCurrentUserId()?.let { reporterId ->
                            val timestamp = SimpleDateFormat(
                                "yyyy-MM-dd HH:mm:ss",
                                Locale.getDefault()
                            ).format(Date())

                            // Create a HashMap to hold the report data
                            val reportData = hashMapOf(
                                "noteId" to noteId,
                                "reporterId" to reporterId,
                                "reportedId" to userId,
                                "timestamp" to timestamp
                            )

                            val notesReportRef = DatabaseAdapter.notesReports

                            notesReportRef.child(noteId).child(reporterId).push().setValue(reportData)
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        context,
                                        "Note reported successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        context,
                                        "Failed to report note: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "You cannot report your own note",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .setNegativeButton("No") { dialog, _ ->
                    // User cancelled the report action, do nothing
                    dialog.dismiss()
                }
                .show()
        }



        numberOfComments(holder.txtComment, noteId)
        holder.txtComment.setOnClickListener {
            val fragment = CommentFragment()
            val bundle = Bundle()
            // Add data to the bundle
            bundle.putString("noteId", noteData[8])
            fragment.arguments = bundle

            val transaction = fragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, fragment)
//            transaction.addToBackStack(null)
            transaction.commit()
        }


        isLike(
            noteId,
            holder.txtLike
        ) // Check if the current user has liked this item and set the appropriate drawable and tag
        numberOfLikes(holder.txtLike, noteId)
        holder.txtLike.setOnClickListener {
            if (holder.txtLike.tag == "Like") {
                // User is liking the item
                DatabaseAdapter.getCurrentUserId()?.let { userId ->
                    FirebaseDatabase.getInstance().reference.child("likes").child(noteId)
                        .child(userId).setValue(true)
                    // Update the UI to indicate that the item is liked
                    val drawableLiked =
                        context.resources.getDrawable(R.drawable.baseline_favorite_24)
                    holder.txtLike.setCompoundDrawablesWithIntrinsicBounds(
                        drawableLiked,
                        null,
                        null,
                        null
                    )
                    holder.txtLike.tag = "Liked"
                    numberOfLikes(holder.txtLike, noteId)
                }
            } else {
                // User is unliking the item
                DatabaseAdapter.getCurrentUserId()?.let { userId ->
                    FirebaseDatabase.getInstance().reference.child("likes").child(noteId)
                        .child(userId).removeValue()
                    // Update the UI to indicate that the item is unliked
                    val drawableUnliked =
                        context.resources.getDrawable(R.drawable.baseline_favorite_border_24)
                    holder.txtLike.setCompoundDrawablesWithIntrinsicBounds(
                        drawableUnliked,
                        null,
                        null,
                        null
                    )
                    holder.txtLike.tag = "Like"
                    numberOfLikes(holder.txtLike, noteId) // Update text after unliking
                }
            }
        }

        isSave(
            noteId,
            holder.txtSave
        ) // Check if the current user has liked this item and set the appropriate drawable and tag
        numberOfSaves(holder.txtSave, noteId)
        holder.txtSave.setOnClickListener {
            if (holder.txtSave.tag == "Save") {
                // User is liking the item
                DatabaseAdapter.getCurrentUserId()?.let { userId ->
                    FirebaseDatabase.getInstance().reference.child("saves").child(noteId)
                        .child(userId).setValue(true)
                    // Update the UI to indicate that the item is liked
                    val drawableSaved =
                        context.resources.getDrawable(R.drawable.baseline_bookmark_24)
                    holder.txtSave.setCompoundDrawablesWithIntrinsicBounds(
                        drawableSaved,
                        null,
                        null,
                        null
                    )
                    holder.txtSave.tag = "Saved"
                    numberOfSaves(holder.txtSave, noteId)
                }
            } else {
                // User is unliking the item
                DatabaseAdapter.getCurrentUserId()?.let { userId ->
                    FirebaseDatabase.getInstance().reference.child("saves").child(noteId)
                        .child(userId).removeValue()
                    // Update the UI to indicate that the item is unliked
                    val drawableUnsaved =
                        context.resources.getDrawable(R.drawable.baseline_bookmark_border_24)
                    holder.txtSave.setCompoundDrawablesWithIntrinsicBounds(
                        drawableUnsaved,
                        null,
                        null,
                        null
                    )
                    holder.txtSave.tag = "Save"
                    numberOfSaves(holder.txtSave, noteId) // Update text after Saving
                }
            }
        }
    }

    //Comments
    private fun numberOfComments(txtComment: TextView, s: String) {
        val commentsRef = FirebaseDatabase.getInstance().reference.child("comments").child(s)
        commentsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                txtComment.text = snapshot.childrenCount.toString()
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    //Likes
    private fun numberOfLikes(txtLike: TextView, s: String) {
        val likesRef = FirebaseDatabase.getInstance().reference.child("likes").child(s)
        likesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                txtLike.text = snapshot.childrenCount.toString()
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    private fun isLike(s: String, txtLike: TextView) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser

        val likesRef = FirebaseDatabase.getInstance().reference.child("likes").child(s)
        likesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (firebaseUser != null) {
                    val liked = snapshot.child(firebaseUser.uid).exists()
                    val drawableResId =
                        if (liked) R.drawable.baseline_favorite_24 else R.drawable.baseline_favorite_border_24
                    val drawable = ContextCompat.getDrawable(txtLike.context, drawableResId)
                    // Set the drawable start (left) of the TextView
                    txtLike.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
                    // Set the tag to indicate the like status
                    txtLike.tag = if (liked) "Liked" else "Like"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle onCancelled event if needed
            }
        })
    }

    //Saves
    private fun numberOfSaves(txtSave: TextView, s: String) {
        val likesRef = FirebaseDatabase.getInstance().reference.child("saves").child(s)
        likesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                txtSave.text = snapshot.childrenCount.toString()
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    private fun isSave(s: String, txtSave: TextView) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser

        val savesRef = FirebaseDatabase.getInstance().reference.child("saves").child(s)
        savesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (firebaseUser != null) {
                    val saved = snapshot.child(firebaseUser.uid).exists()
                    val drawableResId =
                        if (saved) R.drawable.baseline_bookmark_24 else R.drawable.baseline_bookmark_border_24
                    val drawable = ContextCompat.getDrawable(txtSave.context, drawableResId)
                    // Set the drawable start (left) of the TextView
                    txtSave.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
                    // Set the tag to indicate the like status
                    txtSave.tag = if (saved) "Saved" else "Save"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle onCancelled event if needed
            }
        })
    }


    override fun getItemCount(): Int {
        return noteList.size
    }

    fun updateData(newList: List<String>) {
        noteList = newList
        notifyDataSetChanged()
    }
}
