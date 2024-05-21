package com.example.find_my_notes.activities

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Patterns
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.find_my_notes.databinding.ActivitySignupBinding
import com.example.find_my_notes.models.User
import com.example.find_my_notes.models.addUserToDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SignupActivity : BaseActivity() {
    private var b: ActivitySignupBinding? = null
    private lateinit var auth: FirebaseAuth

    private var fileUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        b = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(b?.root)
        auth = FirebaseAuth.getInstance()
        val database = FirebaseDatabase.getInstance()

        //Spinner Code
        val progs = arrayOf("BCA", "BScit", "MCA", "MScit")
        val arrAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, progs)
        b?.spProgram?.adapter = arrAdapter


        //Add Photo
        b?.btnAddPhoto?.setOnClickListener {
//            val intent = Intent()
//            intent.type = "image/*"
//            intent.action = Intent.ACTION_GET_CONTENT
//            startActivityForResult(
//                Intent.createChooser(intent, "Choose Image to Upload"), 0
//            )
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, 0)
        }

        //Open Login Page on Signup btn clicked
        b?.btnSignup?.setOnClickListener {

            val fullname = b?.edtxtName?.text.toString()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                .trim()
            val email = b?.edtxtEmail?.text.toString().lowercase().trim()
            val mobile = b?.edtxtMob?.text.toString().trim()
            val gender = when {
                b?.rbFemale?.isChecked == true -> "Female"
                b?.rbPreferNotToSay?.isChecked == true -> "Prefer not to say"
                b?.rbMale?.isChecked == true -> "Male"
                else -> "Prefer not to say"
            }

            val sprogram = b?.spProgram?.selectedItem.toString().trim()
            val password = b?.edtxtPassword?.text.toString()
            val cPassword = b?.edtxtCpassword?.text.toString()
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

            if (email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email)
                    .matches() && password.isNotEmpty() && cPassword.isNotEmpty() && fullname.isNotEmpty() && mobile.isNotEmpty() && Patterns.PHONE.matcher(
                    mobile
                )
                    .matches() && fileUri != null
            ) {
                if (email.endsWith("charusat.edu.in") or email.endsWith("charusat.ac.in")) {
                    if (password == cPassword) {
                        try {
                            showProgressBar()
                            auth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener(this) { task ->
                                    try {
                                        if (task.isSuccessful) {
                                            val user = auth.currentUser
                                            user?.sendEmailVerification()
                                                ?.addOnCompleteListener(this) {
                                                    if (it.isSuccessful) {
                                                        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

                                                        //Save image to database
                                                        val ref: StorageReference =
                                                            FirebaseStorage.getInstance().reference.child("users").child(userId)
                                                        ref.putFile(fileUri!!)
                                                            .addOnSuccessListener {
                                                                ref.downloadUrl.addOnSuccessListener { uri ->
                                                                    val imageUrl = uri.toString()

                                                                    val newUser = User(email,fullname, mobile,sprogram,gender,password,imageUrl,timestamp)
                                                                    addUserToDatabase(userId, newUser)

                                                                    hideProgressBar()
                                                                    showToast(
                                                                        this,
                                                                        "Please Verify your Email !"
                                                                    )
                                                                    startActivity(
                                                                        Intent(
                                                                            this,
                                                                            LoginActivity::class.java
                                                                        )
                                                                    )
                                                                    finish()
                                                                }
                                                            }.addOnFailureListener {
                                                                hideProgressBar()
                                                                showToast(
                                                                    this,
                                                                    "Image Not Uploaded"
                                                                )
                                                            }
                                                    } else {
                                                        showToast(
                                                            this,
                                                            "Failed to send verification email"
                                                        )
                                                    }
                                                }
                                        } else {
                                            // User creation failed, handle exceptions
                                            throw task.exception
                                                ?: Exception("Unknown error occurred")
                                        }
                                    } catch (e: FirebaseAuthUserCollisionException) {
                                        hideProgressBar()
                                        showToast(this, "User Already Exists")
                                    } catch (e: FirebaseAuthInvalidUserException) {
                                        hideProgressBar()
                                        showToast(this, "Invalid User")
                                    } catch (e: FirebaseAuthWeakPasswordException) {
                                        hideProgressBar()
                                        showToast(
                                            this,
                                            "Password should be at least 6 characters long"
                                        )
                                    } catch (e: Exception) {
                                        hideProgressBar()
                                        showToast(this, "Unknown error occurred")
                                    }
                                }
                        } catch (e: Exception) {
                            hideProgressBar()
                            showToast(this, e.message.toString())
                        }
                    } else {
                        showToast(this, "Password should match")
                    }
                } else {
                    showToast(this, "Only charusat emails are allowed")
                }
            } else {
                showToast(this, "Please Fill out All Details with Proper Format  !!")
            }
        }

        b?.txtLinkLogin?.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

//    @Deprecated("Deprecated in Java")
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
////        if (requestCode == 0 && resultCode == RESULT_OK && data != null && data.data != null) {
////            fileUri = data.data
////            try {
////                val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, fileUri)
////                b?.imgProfilePic?.setImageBitmap(bitmap)
////            } catch (e: Exception) {
////                Log.e("Exception", "Error $e")
////            }
////        }
//
//        // Handle image selection and cropping
//        if ((requestCode == 0) && (resultCode == AppCompatActivity.RESULT_OK) && (data != null) && (data.data != null)) {
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
//            val bitmap: Bitmap =
//                MediaStore.Images.Media.getBitmap(contentResolver, fileUri)
//            bitmap.let {
//                // Update the ImageView with the cropped image
//                b?.imgProfilePic?.setImageBitmap(it)
//            }
//        }
//    }

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
                    fileUri = getImageUri(it)

                    // Update the ImageView with the cropped image
                    b?.imgProfilePic?.setImageBitmap(it)

                    // Now, you can upload the cropped image to the database using fileUri
                    // Add your code to upload the cropped image here
                }
            }
        }
    }

    private fun getImageUri(inImage: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(contentResolver, inImage, "Title", null)
        return Uri.parse(path)
    }
}