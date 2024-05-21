package com.example.find_my_notes.adapters

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage

class DatabaseAdapter {
    //Static methods and variables
    companion object {
        private lateinit var auth: FirebaseAuth

        //DB tables
        var users = Firebase.database.getReference("users")
        var notes = Firebase.database.getReference("notes")
        var myNotes = Firebase.database.getReference("myNotes")
        var likes = Firebase.database.getReference("likes")
        var saves = Firebase.database.getReference("saves")
        var comments = Firebase.database.getReference("comments")
        var dept = Firebase.database.getReference("program")
        var userReports = Firebase.database.getReference("userReports")
        var notesReports = Firebase.database.getReference("notesReports")
        var feedback = Firebase.database.getReference("feedback")

        //Storage
        var userImage = FirebaseStorage.getInstance().getReference("users")
        var notesPdf = FirebaseStorage.getInstance().getReference("notes")

        //Current user key
        var key = ""

        //Global variables
        var contactName = ""
        var contactNumber = ""

        fun returnUser(): FirebaseUser? {
            auth = FirebaseAuth.getInstance()

            return auth.currentUser
        }

        fun getCurrentUserId(): String? {
            val currentUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser
            return currentUser?.uid
        }

        fun verifyEmail(callback: (Boolean) -> Unit) {

            auth = FirebaseAuth.getInstance()

            val user = auth.currentUser

            try {
                user?.sendEmailVerification()?.addOnCompleteListener {
                    if (it.isSuccessful) {
                        callback(true)
                    } else {
                        callback(false)
                        Log.d("DB_ERROR", it.toString())
                    }
                }
            } catch (e: Exception) {
                callback(false)
                Log.d("DB_ERROR", e.toString())
            }
        }

        fun signUpWithMail(mail: String, password: String, callback: (String) -> Unit) {
            auth = FirebaseAuth.getInstance()

            try {
                auth.createUserWithEmailAndPassword(mail, password).addOnCompleteListener {
                    if (it.isSuccessful) {
                        callback("true")
                    } else {
                        if (it.exception is FirebaseAuthUserCollisionException) {
                            callback("exist") // Email already exists
                        } else {
                            Log.e("TAG", "Account creation failed: ${it.exception}")
                            callback("false") // Other error
                        }
                    }
                }.addOnFailureListener {
                    callback("false")
                    Log.d("DB_ERROR", it.toString())
                }
            } catch (e: Exception) {
                Log.d("DB_ERROR", e.toString())
            }
        }

        fun signInWithMail(mail: String, password: String, callback: (Boolean) -> Unit) {
            auth = FirebaseAuth.getInstance()

            try {
                auth.signInWithEmailAndPassword(mail, password).addOnCompleteListener {
                    if (it.isSuccessful) {
                        callback(true)
                    } else {
                        callback(false)
                    }
                }.addOnFailureListener {
                    callback(false)
                }
            } catch (e: Exception) {
                Log.d("DB_ERROR", e.toString())
            }
        }

        fun passwordResetMail(mail: String, callback: (String) -> Unit) {
            auth = FirebaseAuth.getInstance()

            try {
                auth.sendPasswordResetEmail(mail).addOnCompleteListener {
                    if (it.isSuccessful) {
                        callback("true")
                    } else {
                        if (it.exception is FirebaseAuthInvalidUserException) {
                            callback("exist")
                        } else {
                            callback("false")
                        }
                    }
                }.addOnFailureListener {
                    callback("false")
                }
            } catch (e: Exception) {
                Log.d("DB_ERROR", e.toString())
            }
        }
    }
}