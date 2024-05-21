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
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.example.find_my_notes.R
import com.example.find_my_notes.fragments.CommentFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyNotesInfoRecycleAdapter(
    private val context: Context,
    private val fragmentManager: FragmentManager
) : RecyclerView.Adapter<MyNotesInfoRecycleAdapter.ViewHolder>() {
    private var myNotesList: List<String> = ArrayList()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtMyNoteName: TextView = itemView.findViewById(R.id.txtMyNoteName)
        val txtMyNoteDescription: TextView = itemView.findViewById(R.id.txtMyNoteDescription)
        val txtMyNoteTimestamp: TextView = itemView.findViewById(R.id.txtMyNoteTimestamp)
        val txtLike: TextView = itemView.findViewById(R.id.txtLike)
        val txtComment: TextView = itemView.findViewById(R.id.txtComment)
        val txtSave: TextView = itemView.findViewById(R.id.txtSave)
        val txtNotesReport: TextView = itemView.findViewById(R.id.txtNotesReport)


        private val cvMyNoteDownloadPDF: CardView = itemView.findViewById(R.id.cvMyNoteDownloadPDF)

        init {
            cvMyNoteDownloadPDF.setOnClickListener {
                val myNoteData = myNotesList[adapterPosition].split("\n")
                val noteName = myNoteData[0]
                val pdfUrl = myNoteData[3]

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
            .inflate(R.layout.mynotes_info_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyNotesInfoRecycleAdapter.ViewHolder, position: Int) {
        val myNotesList = myNotesList[position].split("\n")
        holder.txtMyNoteName.text = myNotesList[0]
        holder.txtMyNoteDescription.text = myNotesList[1]
        holder.txtMyNoteTimestamp.text = myNotesList[2]
        val noteId = myNotesList[4]

        DatabaseAdapter.notes.child(noteId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val userId = snapshot.child("currentUser").value.toString()
                        Log.d("UserId", "UserId from database: $userId, CurrentUserId: ${DatabaseAdapter.getCurrentUserId()}")
                        if (userId == DatabaseAdapter.getCurrentUserId()) {
                            Log.d("Visibility", "Hiding txtNotesReport")
                            holder.txtNotesReport.visibility = View.GONE
                        } else {
                            Log.d("Visibility", "Showing txtNotesReport")
                            holder.txtNotesReport.visibility = View.VISIBLE
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle onCancelled event if needed
                }
            })

        DatabaseAdapter.notes.child(noteId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val userId = snapshot.child("currentUser").value.toString()

                        // Now that we have the userId, set up the click listener for reporting
                        // Inside onBindViewHolder method of MyNotesInfoRecycleAdapter
                        holder.txtNotesReport.setOnClickListener {
                            AlertDialog.Builder(context)
                                .setTitle("Report Note")
                                .setMessage("Are you sure you want to report this note?")
                                .setPositiveButton("Yes") { dialog, _ ->
                                    // User confirmed to report the note
                                    if (userId != DatabaseAdapter.getCurrentUserId()) {
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

                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    // Handle onCancelled event if needed
                }
            })


        numberOfComments(holder.txtComment, noteId)
        holder.txtComment.setOnClickListener {
            val fragment = CommentFragment()
            val bundle = Bundle()
            // Add data to the bundle
            bundle.putString("noteId", noteId)
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
        ) // Check if the current user has Saved this item and set the appropriate drawable and tag
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
        return myNotesList.size
    }

    fun updateData(newList: List<String>) {
        myNotesList = newList
        notifyDataSetChanged()
    }
}
