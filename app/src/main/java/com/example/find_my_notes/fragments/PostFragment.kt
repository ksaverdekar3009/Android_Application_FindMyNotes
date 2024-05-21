package com.example.find_my_notes.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.find_my_notes.R
import com.example.find_my_notes.activities.BaseActivity
import com.example.find_my_notes.adapters.DatabaseAdapter
import com.example.find_my_notes.databinding.FragmentPostBinding
import com.example.find_my_notes.models.MyNote
import com.example.find_my_notes.models.Note
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageReference
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostFragment : Fragment() {

    private var b: FragmentPostBinding? = null
    private val PICK_PDF_FILE = 1

    private var fileUri: Uri? = null

    private lateinit var currentUser: FirebaseUser
    private lateinit var databaseReference: DatabaseReference
    private lateinit var dbRef: DatabaseReference
    private lateinit var dbDeptRef: DatabaseReference
    private lateinit var storageReference: StorageReference
    private var selectedFileName: String? = null

    private var dept: String = ""

    // Define the maximum allowed file size in bytes (e.g., 5MB)
    private val MAX_FILE_SIZE_BYTES: Long = 5 * 1024 * 1024

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        b = FragmentPostBinding.inflate(inflater, container, false)
        val view = b?.root

        currentUser = DatabaseAdapter.returnUser()!!
        databaseReference = DatabaseAdapter.notes
        dbRef = DatabaseAdapter.myNotes
        dbDeptRef = DatabaseAdapter.dept
        storageReference = DatabaseAdapter.notesPdf

        b?.cvSelectPDF?.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "application/pdf"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            startActivityForResult(intent, PICK_PDF_FILE)
        }

        // Fetch the program information from the current user's data
        DatabaseAdapter.users.child(currentUser.uid).child("program")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    dept = snapshot.getValue(String::class.java).toString()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle the error (if needed)
                }
            })

        b?.btnUploadNote?.setOnClickListener {
            val titleNote = b?.edtxtUploadTitle?.text?.trim().toString().capitalize(Locale.ROOT)
            val descNote =
                b?.edtxtUploadDescription?.text?.trim().toString().capitalize(Locale.ROOT)

            if (titleNote.isNotEmpty() && descNote.isNotEmpty() && selectedFileName != null) {
                val fileSize = fileUri?.let { getFileSize(it) }
                if ((fileSize != null) && (fileSize <= MAX_FILE_SIZE_BYTES)) {
                    // Proceed with the upload
                    val ref = storageReference
                    val notesRef = databaseReference
                    val myNoteRef = dbRef

                    val noteId = notesRef.push().key  // Generate a unique ID for the note
                    val fileReference = ref.child("$noteId.pdf")
                    val uploadTask = fileUri?.let { it1 -> fileReference.putFile(it1) }

                    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                        Date()
                    )

                    if (isAdded) {
                        (requireActivity() as BaseActivity).showProgressBar()
                    }

                    uploadTask?.addOnSuccessListener {
                        // Get the PDF URL from the taskSnapshot
                        fileReference.downloadUrl.addOnSuccessListener { pdfUrl ->
                            // Create a Note object with the PDF URL
                            val note = noteId?.let { it1 ->
                                Note(
                                    it1,
                                    titleNote,
                                    descNote,
                                    pdfUrl.toString(),
                                    currentUser.uid,
                                    timestamp
                                )
                            }

                            val myNote = noteId?.let { it2 ->
                                MyNote(
                                    it2, titleNote, descNote,
                                    pdfUrl.toString(),
                                    currentUser.uid,
                                    timestamp
                                )
                            }

                            // Push the note object to Firebase Realtime Database
                            if (noteId != null) {
                                notesRef.child(noteId).setValue(note)
                                myNoteRef.child(currentUser.uid).child(noteId).setValue(myNote)
                                dbDeptRef.child(dept).child(noteId).setValue(note)
                                    .addOnSuccessListener {
                                        //clear fields
                                        b?.edtxtUploadTitle?.text?.clear()
                                        b?.edtxtUploadDescription?.text?.clear()
                                        b?.txtFileName?.text = "No file chosen"
                                        selectedFileName = null
                                        fileUri = null

                                        if (isAdded) {
                                            (requireActivity() as BaseActivity).hideProgressBar()
                                            // Show toast message indicating success
                                            (requireActivity() as BaseActivity).showToast(
                                                requireContext(),
                                                "Note Uploaded Successfully"
                                            )
                                        }
                                    }
                                    .addOnFailureListener { exception ->
                                        // Failed to update profile
                                        if (isAdded) {
                                            (requireActivity() as BaseActivity).hideProgressBar()
                                            (requireActivity() as BaseActivity).showToast(
                                                requireContext(),
                                                "Failed to Upload Note: ${exception.message}"
                                            )
                                        }
                                    }
                            }
                        }.addOnFailureListener { exception ->
                            if (isAdded) {
                                (requireActivity() as BaseActivity).hideProgressBar()
                                // Handle failure to get PDF URL
                                (requireActivity() as BaseActivity).showToast(
                                    requireContext(),
                                    "Failed to get PDF URL: ${exception.message}"
                                )
                            }
                        }
                    }?.addOnFailureListener { exception ->
                        if (isAdded) {
                            (requireActivity() as BaseActivity).hideProgressBar()
                            // Handle failure to upload PDF file
                            (requireActivity() as BaseActivity).showToast(
                                requireContext(),
                                "Failed to upload PDF: ${exception.message}"
                            )
                        }
                    }
                } else {
//                    b?.txtFileName?.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                    b?.txtFileName?.text = "*Size must be less than 5 mb"

                    // Show a toast message indicating that the file size exceeds the limit
                    Toast.makeText(requireContext(), "File size exceeds the limit", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Show toast message indicating missing fields
                if (isAdded) {
                    (requireActivity() as BaseActivity).showToast(
                        requireContext(),
                        "Please enter title, description, and select a PDF"
                    )
                }
            }
        }

        b?.txtEditNoteLink?.setOnClickListener {
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, EditNotesBlankFragment()) // Replace fragment_container with your actual container id
//            transaction.addToBackStack(null)
            transaction.commit()
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        requireActivity().title = "Upload PDF" // Set the toolbar title
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_PDF_FILE && resultCode == Activity.RESULT_OK && data != null) {
            fileUri = data.data
            try {
                selectedFileName = getFileName(fileUri!!)
                b?.txtFileName?.text = selectedFileName ?: "No file chosen"
            } catch (e: Exception) {
                Log.e("Exception", "Error $e")
            }
        }
    }

    @SuppressLint("Range")
    private fun getFileName(uri: Uri): String {
        var name = ""
        context?.contentResolver?.query(uri, null, null, null, null)?.use {
            it.moveToFirst()
            name = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
        }
        return name
    }

    private fun getFileSize(uri: Uri): Long? {
        return try {
            val inputStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
            val fileSize: Long? = inputStream?.available()?.toLong()
            inputStream?.close()
            fileSize
        } catch (e: Exception) {
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        b = null
    }
}
