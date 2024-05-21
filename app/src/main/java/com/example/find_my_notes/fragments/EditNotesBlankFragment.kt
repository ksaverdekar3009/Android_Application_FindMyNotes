package com.example.find_my_notes.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Adapter
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.room.util.findColumnIndexBySuffix
import com.example.find_my_notes.R
import com.example.find_my_notes.activities.BaseActivity
import com.example.find_my_notes.adapters.DatabaseAdapter
import com.example.find_my_notes.databinding.FragmentEditNotesBlankBinding
import com.example.find_my_notes.models.MyNote
import com.example.find_my_notes.models.Note
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.StorageReference
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EditNotesBlankFragment : Fragment() {

    private var b: FragmentEditNotesBlankBinding? = null
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
        b = FragmentEditNotesBlankBinding.inflate(inflater, container, false)

        return b?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentUser = DatabaseAdapter.returnUser()!!
        databaseReference = DatabaseAdapter.notes
        dbDeptRef = DatabaseAdapter.dept
        storageReference = DatabaseAdapter.notesPdf
        dbRef = DatabaseAdapter.myNotes.child(currentUser.uid)

        var noteId: String? = null
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

        // Fetch note data and populate the autocomplete TextView
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val titleList = mutableListOf<String>()

                // Check if there are any notes uploaded by the current user
                if (snapshot.exists()) {
                    // Iterate through each note ID
                    for (noteSnapshot in snapshot.children) {
                        // Retrieve the title for the current note ID
                        b?.noNotesTextView?.visibility = View.GONE // Hide message if list is not empty
                        b?.linearView?.visibility = View.VISIBLE
                        val title = noteSnapshot.child("title").getValue(String::class.java)
                        title?.let { titleList.add(it) }
                    }
                } else {
                    b?.noNotesTextView?.visibility = View.GONE // Hide message if list is not empty
                    val transaction =
                        requireActivity().supportFragmentManager.beginTransaction()
                    transaction.replace(
                        R.id.fragment_container,
                        PostFragment()
                    ) // Replace fragment_container with your actual container id
//                    transaction.addToBackStack(null)
                    transaction.commit()
                }

                // Create an ArrayAdapter with the titleList
                val adapter =
                    ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, titleList)

                // Set the adapter to the autocomplete TextView
                b?.spinnerNoteTitle?.adapter = adapter

                // Add a listener to the spinner
                b?.spinnerNoteTitle?.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {
                            // Get the selected item from the spinner
                            val selectedItem = parent?.getItemAtPosition(position).toString()

                            // Find the corresponding note in the database and update the description EditText
                            for (noteSnapshot in snapshot.children) {
                                val title = noteSnapshot.child("title").getValue(String::class.java)
                                if (title == selectedItem) {
                                    val description = noteSnapshot.child("description")
                                        .getValue(String::class.java)
                                    description?.let {
                                        b?.edtxtEditNoteDescription?.setText(it)
                                    }
                                    noteId = noteSnapshot.key
                                    break  // Once found, exit the loop
                                }
                            }
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {
                            // Handle case when nothing is selected (if needed)
                        }
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })

        b?.cvSelectPDF?.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "application/pdf"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            startActivityForResult(intent, PICK_PDF_FILE)
        }

        b?.btnEditNote?.setOnClickListener {
            val titleNote = b?.spinnerNoteTitle?.selectedItem.toString()
            val descNote =
                b?.edtxtEditNoteDescription?.text?.trim().toString().capitalize(Locale.ROOT)

            if (noteId != null) {
                val fileSize = fileUri?.let { getFileSize(it) }
                if ((fileSize != null) && (fileSize <= MAX_FILE_SIZE_BYTES)) {
                    // Proceed with the upload
                    val ref = storageReference

                    val fileReference = ref.child("$noteId.pdf")
                    val uploadTask = fileReference.putFile(fileUri!!)

                    val timestamp =
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                            Date()
                        )

                    if (isAdded) {
                        (requireActivity() as BaseActivity).showProgressBar()
                    }

                    uploadTask.addOnSuccessListener {
                        // Get the PDF URL from the taskSnapshot
                        fileReference.downloadUrl.addOnSuccessListener { pdfUrl ->
                            // Create a Note object with the PDF URL
                            val note = noteId?.let {
                                Note(
                                    it,
                                    titleNote,
                                    descNote,
                                    pdfUrl.toString(),
                                    currentUser.uid,
                                    timestamp
                                )
                            }

                            val myNote = noteId?.let {
                                MyNote(
                                    it, titleNote, descNote,
                                    pdfUrl.toString(),
                                    currentUser.uid,
                                    timestamp
                                )
                            }

                            // Push the note object to Firebase Realtime Database
                            if (noteId != null) {
                                DatabaseAdapter.notes.child(noteId!!).setValue(note)
                                DatabaseAdapter.myNotes.child(currentUser.uid).child(noteId!!)
                                    .setValue(myNote)
                                DatabaseAdapter.dept.child(dept).child(noteId!!).setValue(note)
                                    .addOnSuccessListener {
                                        // Clear fields
                                        b?.edtxtEditNoteDescription?.text?.clear()
                                        b?.txtFileName?.text = "No file chosen"
                                        selectedFileName = null
                                        fileUri = null

                                        if (isAdded) {
                                            (requireActivity() as BaseActivity).hideProgressBar()
                                            val transaction =
                                                requireActivity().supportFragmentManager.beginTransaction()
                                            transaction.replace(
                                                R.id.fragment_container,
                                                PostFragment()
                                            ) // Replace fragment_container with your actual container id
                                            transaction.addToBackStack(null)
                                            transaction.commit()
                                            // Show toast message indicating success
                                            (requireActivity() as BaseActivity).showToast(
                                                requireContext(),
                                                "Note Updated Successfully"
                                            )
                                        }
                                    }
                                    .addOnFailureListener { exception ->
                                        // Failed to update note
                                        if (isAdded) {
                                            (requireActivity() as BaseActivity).hideProgressBar()
                                            (requireActivity() as BaseActivity).showToast(
                                                requireContext(),
                                                "Failed to Update Note: ${exception.message}"
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
                    }.addOnFailureListener { exception ->
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
                    // PDF is null or exceeds the file size limit, keep retrieved data
                    // Clear fields related to the PDF selection
                    b?.txtFileName?.text = "*Size must be less than 5mb"
                    selectedFileName = null
                    fileUri = null

                    // Show a toast message indicating that the PDF is null or exceeds the file size limit
                    Toast.makeText(
                        requireContext(),
                        "Please select a PDF file within 5MB size limit",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                // Note ID not found (should not happen if spinner and database are in sync)
                (requireActivity() as BaseActivity).showToast(
                    requireContext(),
                    "Failed to Edit Note: Note not found"
                )
            }
        }

        b?.btnDeleteNote?.setOnClickListener {
            val titleNote = b?.spinnerNoteTitle?.selectedItem.toString()

            // Find the corresponding note ID based on the selected title
            dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (noteSnapshot in snapshot.children) {
                        val title = noteSnapshot.child("title").getValue(String::class.java)
                        if (title == titleNote) {
                            noteId = noteSnapshot.key
                            break
                        }
                    }

                    // Check if a note ID is found
                    if (noteId != null) {
                        if (isAdded) {
                            (requireActivity() as BaseActivity).showProgressBar()
                        }
                        // Delete the note from the database
                        DatabaseAdapter.likes.child(noteId!!).removeValue()
                        DatabaseAdapter.saves.child(noteId!!).removeValue()
                        DatabaseAdapter.notes.child(noteId!!).removeValue()
                        DatabaseAdapter.myNotes.child(currentUser.uid).child(noteId!!).removeValue()
                        DatabaseAdapter.dept.child(dept).child(noteId!!).removeValue()
                            .addOnSuccessListener {
                                // Note successfully deleted
                                // Clear fields related to the deleted note
                                b?.edtxtEditNoteDescription?.text?.clear()
                                b?.txtFileName?.text = "No file chosen"
                                selectedFileName = null
                                fileUri = null

                                // Delete the file from storage
                                val ref = storageReference.child("$noteId.pdf")
                                ref.delete()
                                    .addOnSuccessListener {
                                        // File successfully deleted from storage
                                        val transaction = requireActivity().supportFragmentManager.beginTransaction()
                                        transaction.replace(R.id.fragment_container, EditNotesBlankFragment()) // Replace fragment_container with your actual container id
//                                        transaction.addToBackStack(null)
                                        transaction.commit()
                                        if (isAdded) {
                                            (requireActivity() as BaseActivity).hideProgressBar()
                                        (requireActivity() as BaseActivity).showToast(
                                            requireContext(),
                                            "Note and File Deleted Successfully"
                                        )
                                        }
                                    }
                                    .addOnFailureListener { exception ->
                                        if (isAdded) {
                                            (requireActivity() as BaseActivity).showProgressBar()
                                        // Failed to delete file from storage
                                        // Show a toast message indicating the failure
                                        (requireActivity() as BaseActivity).showToast(
                                            requireContext(),
                                            "Failed to Delete File from Storage: ${exception.message}"
                                        )
                                        }
                                    }
                            }
                            .addOnFailureListener { exception ->
                                // Failed to delete note
                                // Show a toast message indicating the failure
                                if (isAdded) {
                                    (requireActivity() as BaseActivity).hideProgressBar()
                                    (requireActivity() as BaseActivity).showToast(
                                        requireContext(),
                                        "Failed to Delete Note: ${exception.message}"
                                    )
                                }
                            }
                    } else {
                        // Note ID not found (should not happen if spinner and database are in sync)
                        // Show a toast message indicating the failure
                        if (isAdded) {
                            (requireActivity() as BaseActivity).hideProgressBar()
                            (requireActivity() as BaseActivity).showToast(
                                requireContext(),
                                "Failed to Delete Note: Note not found"
                            )
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                    // Show a toast message indicating the failure
                    (requireActivity() as BaseActivity).showToast(
                        requireContext(),
                        "Failed to Delete Note: ${error.message}"
                    )
                }
            })
        }

    }

    override fun onResume() {
        super.onResume()
        requireActivity().title = "Edit Note" // Set the toolbar title
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