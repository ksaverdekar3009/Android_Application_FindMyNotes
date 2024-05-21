package com.example.find_my_notes.fragments

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.find_my_notes.activities.BaseActivity
import com.example.find_my_notes.activities.LoginActivity
import com.example.find_my_notes.R
import com.example.find_my_notes.adapters.DatabaseAdapter
import com.example.find_my_notes.databinding.FragmentEditProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class EditProfileFragment : Fragment() {

    private var b: FragmentEditProfileBinding? = null
    private var fileUri: Uri? = null

    private val IMAGE_PICK_CODE = 1000
    private val IMAGE_CROP_CODE = 1001

    private lateinit var currentUser: FirebaseUser
    private lateinit var databaseReference: DatabaseReference
    private lateinit var storageReference: StorageReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        b = FragmentEditProfileBinding.inflate(inflater, container, false)

        currentUser = DatabaseAdapter.returnUser()!!
        databaseReference = DatabaseAdapter.users
        storageReference = DatabaseAdapter.userImage

//        if (isAdded) {
//            (requireActivity() as BaseActivity).showProgressBar()
//        }

        // Retrieve the details from the database
        databaseReference.child(currentUser.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val imageUrl = dataSnapshot.child("imageUrl").value.toString()

                    // Set values to EditText after converting to Editable
                    b!!.edtxtEditProName.text = Editable.Factory.getInstance()
                        .newEditable(dataSnapshot.child("fullname").value.toString())
                    b!!.edtxtEditProMob.text = Editable.Factory.getInstance()
                        .newEditable(dataSnapshot.child("mobile").value.toString())

                    // Check the gender value and set the appropriate radio button
                    when (dataSnapshot.child("gender").value.toString()) {
                        "Male" -> b?.rbMale?.isChecked = true
                        "Female" -> b?.rbFemale?.isChecked = true
                        "Prefer not to say" -> b?.rbPreferNotToSay?.isChecked = true
                        // Handle the case if gender value doesn't match any of the options
                        else -> b?.rbPreferNotToSay?.isChecked = true
                    }

                    // Load image from the URL into ProfilePic ImageView
                    Picasso.get().load(imageUrl).placeholder(R.drawable.profile_pic)
                        .into(b!!.imgEditProfile)
                    if (isAdded) {
//                        (requireActivity() as BaseActivity).hideProgressBar()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    b!!.edtxtEditProName.text = Editable.Factory.getInstance().newEditable("")
                    b!!.edtxtEditProName.text = Editable.Factory.getInstance().newEditable("")
                    b?.rbPreferNotToSay?.isChecked = true
                    b!!.imgEditProfile.setImageResource(R.drawable.profile_pic)
                    if (isAdded) {
//                        (requireActivity() as BaseActivity).hideProgressBar()
                    }
                }
            })

        //Select Photo
        b?.btnSelectNewPhoto?.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, 0)
        }
        b?.btnUpdateProfile?.setOnClickListener {
            // Get the values from EditText fields
            val fullname = b?.edtxtEditProName?.text.toString().trim()
            val mobile = b?.edtxtEditProMob?.text.toString().trim()
            val gender = when {
                b?.rbFemale?.isChecked == true -> "Female"
                b?.rbPreferNotToSay?.isChecked == true -> "Prefer not to say"
                b?.rbMale?.isChecked == true -> "Male"
                else -> "Prefer not to say"
            }

            // Show progress bar
            if (isAdded) {
                (requireActivity() as BaseActivity).showProgressBar()
            }

            if (fileUri != null) {
                // Save image to Firebase Storage if a new image is selected
                val ref = storageReference.child(currentUser.uid)
                ref.putFile(fileUri!!).addOnSuccessListener { uploadTask ->
                    // Get the image URL from Firebase Storage
                    uploadTask.storage.downloadUrl.addOnSuccessListener { imageUrl ->
                        // Image upload successful, get the URL
                        val imageURL = imageUrl.toString()

                        // Update user profile in the database
                        val userRef = databaseReference.child(currentUser.uid)
                        userRef.child("fullname").setValue(fullname)
                        userRef.child("mobile").setValue(mobile)
                        userRef.child("gender").setValue(gender)
                        userRef.child("imageUrl").setValue(imageURL).addOnSuccessListener {
                            // Profile update successful
                            if (isAdded) {
                                (requireActivity() as BaseActivity).hideProgressBar()
                            }
                            if (isAdded) {
                                (requireActivity() as BaseActivity).showToast(
                                    requireContext(), "Profile Updated Successfully"
                                )
                            }
                        }.addOnFailureListener { exception ->
                            // Failed to update profile
                            if (isAdded) {
                                (requireActivity() as BaseActivity).hideProgressBar()
                            }
                            if (isAdded) {
                                (requireActivity() as BaseActivity).showToast(
                                    requireContext(),
                                    "Failed Try Again Later: ${exception.message}"
                                )
                            }
                        }
                    }
                }.addOnFailureListener { exception ->
                    // Failed to upload image
                    if (isAdded) {
                        (requireActivity() as BaseActivity).hideProgressBar()
                    }
                    if (isAdded) {
                        (requireActivity() as BaseActivity).showToast(
                            requireContext(),
                            "Failed to Upload Image try again Later: ${exception.message}"
                        )
                    }
                }
            } else {
                // If fileUri is null, update profile without changing the image
                val userRef = databaseReference.child(currentUser.uid)
                userRef.child("fullname").setValue(fullname)
                userRef.child("mobile").setValue(mobile)
                userRef.child("gender").setValue(gender).addOnSuccessListener {
                    // Profile update successful
                    if (isAdded) {
                        (requireActivity() as BaseActivity).hideProgressBar()
                        (requireActivity() as BaseActivity).showToast(
                            requireContext(), "Profile Updated Successfully"
                        )
                    }
                }.addOnFailureListener { exception ->
                    // Failed to update profile
                    if (isAdded) {
                        (requireActivity() as BaseActivity).hideProgressBar()
                        (requireActivity() as BaseActivity).showToast(
                            requireContext(), "${exception.message}"
                        )
                    }
                }
            }
        }

        // Inside setOnClickListener of btnDeleteProfile
        b?.btnDeleteProfile?.setOnClickListener {
            // Create an AlertDialog to confirm the deletion request
            context?.let { it1 ->
                AlertDialog.Builder(it1)
                    .setTitle("Delete Account")
                    .setMessage("Are you sure you want to delete your account?")
                    .setPositiveButton("Yes") { dialog, _ ->
                        // Get the current user ID
                        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

                        // Check if the current user ID is not null
                        currentUserId?.let { userId ->
                            // Get the timestamp
                            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                                Date()
                            )

                            // Create a HashMap to hold the delete request data
                            val deleteRequestData = hashMapOf(
                                "userId" to userId,
                                "timestamp" to timestamp
                            )

                            // Get a reference to the "accountDeleteRequest" table
                            val accountDeleteRequestRef = FirebaseDatabase.getInstance().reference.child("accountDeleteRequest")

                            // Push the delete request data to the "accountDeleteRequest" table
                            accountDeleteRequestRef.child(userId).setValue(deleteRequestData)
                                .addOnSuccessListener {
                                    // Handle success - request pushed successfully
                                    Toast.makeText(context, "Account deletion request sent", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    // Handle failure - request failed to push
                                    Toast.makeText(context, "Failed to send account deletion request: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        } ?: run {
                            // Handle case where current user ID is null
                            Toast.makeText(context, "Failed to get current user ID", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        // User canceled the account deletion request
                        dialog.dismiss()
                    }
                    .show()
            }
        }



        return b?.root
    }

    override fun onResume() {
        super.onResume()
        requireActivity().title = "Edit Profile" // Set the toolbar title
    }

//    @Deprecated("Deprecated in Java")
//    @Suppress("DEPRECATION")
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
////        if ((requestCode == 0) && (resultCode == AppCompatActivity.RESULT_OK) && (data != null) && (data.data != null)) {
////            fileUri = data.data
////            try {
////                val bitmap: Bitmap =
////                    MediaStore.Images.Media.getBitmap(requireContext().contentResolver, fileUri)
////                b?.imgEditProfile?.setImageBitmap(bitmap)
////            } catch (e: Exception) {
////                Log.e("Exception", "Error $e")
////            }
////        }
//
//        // Handle image selection and cropping
//        if (requestCode == 0 && resultCode == AppCompatActivity.RESULT_OK && data != null && data.data != null) {
//            // Image selection successful, proceed to crop
//            fileUri = data.data
//            fileUri?.let {
//                // Start cropping activity
//                val cropIntent = Intent("com.android.camera.action.CROP")
//                cropIntent.setDataAndType(it, "image/*")
//                cropIntent.putExtra("crop", "true")
//                cropIntent.putExtra("aspectX", 1)
//                cropIntent.putExtra("aspectY", 1)
//                cropIntent.putExtra("outputX", 256) // Set output size as needed
//                cropIntent.putExtra("outputY", 256) // Set output size as needed
//                cropIntent.putExtra("return-data", true)
//                startActivityForResult(cropIntent, 1) // Use a different request code for crop result
//            }
//        } else if (requestCode == 1 && resultCode == AppCompatActivity.RESULT_OK && data != null) {
//            // Handle crop result
//            val bundle = data.extras
//            if (bundle != null) {
//                val bitmap = bundle.getParcelable<Bitmap>("data")
//                bitmap?.let {
//                    // Update the ImageView with the cropped image
//                    b?.imgEditProfile?.setImageBitmap(it)
//                }
//            }
//        }
//    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 0 && resultCode == AppCompatActivity.RESULT_OK && data != null && data.data != null) {
            // Image selection successful, proceed to crop
            fileUri = data.data
            fileUri?.let {
                // Start cropping activity
                val cropIntent = Intent("com.android.camera.action.CROP")
                cropIntent.setDataAndType(it, "image/*")
                cropIntent.putExtra("crop", "true")
                cropIntent.putExtra("aspectX", 1)
                cropIntent.putExtra("aspectY", 1)
                cropIntent.putExtra("outputX", 256) // Set output size as needed
                cropIntent.putExtra("outputY", 256) // Set output size as needed
                cropIntent.putExtra("return-data", true)
                startActivityForResult(cropIntent, 1) // Use a different request code for crop result
            }
        } else if (requestCode == 1 && resultCode == AppCompatActivity.RESULT_OK && data != null) {
            // Handle crop result
            val bundle = data.extras
            if (bundle != null) {
                val croppedBitmap = bundle.getParcelable<Bitmap>("data")
                croppedBitmap?.let {
                    // Convert cropped bitmap to a file and update fileUri
                    fileUri = getImageUri(requireContext(), it)

                    // Update the ImageView with the cropped image
                    b?.imgEditProfile?.setImageBitmap(it)

                    // Now, you can upload the cropped image to the database using fileUri
                    // Add your code to upload the cropped image here
                }
            }
        }
    }

    private fun getImageUri(inContext: Context, inImage: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(inContext.contentResolver, inImage, "Title", null)
        return Uri.parse(path)
    }


}