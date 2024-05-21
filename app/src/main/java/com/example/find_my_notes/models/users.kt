package com.example.find_my_notes.models

import com.google.firebase.database.FirebaseDatabase


data class User(
    val email: String,
    val fullname: String,
    val mobile: String,
    val program: String,
    val gender: String,
    val password: String,
    val imageUrl: String, // Assuming imageUrl instead of image_url for naming convention
    val timestamp: String
)

// Function to add a new user to the database
fun addUserToDatabase(userId: String, user: User) {
    val usersRef = FirebaseDatabase.getInstance().getReference("users")
    usersRef.child(userId).setValue(user)
}
