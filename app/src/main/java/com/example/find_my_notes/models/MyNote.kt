package com.example.find_my_notes.models

data class MyNote(

    val noteId: String,
    val title: String,
    val description: String,
    val noteUrlPDF: String,
    val currentUser : String,
    val timestamp: String
)
