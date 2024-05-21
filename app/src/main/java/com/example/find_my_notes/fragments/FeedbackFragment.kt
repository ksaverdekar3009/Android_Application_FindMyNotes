package com.example.find_my_notes.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.example.find_my_notes.R
import com.example.find_my_notes.activities.BaseActivity
import com.example.find_my_notes.adapters.DatabaseAdapter
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class FeedbackFragment : Fragment() {

    private lateinit var edtxtFeedback: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_feedback, container, false)
        edtxtFeedback = view.findViewById(R.id.edtxtFeedback)
        val btnSubmitFeedback: Button = view.findViewById(R.id.btnSubmitFeedback)
        btnSubmitFeedback.setOnClickListener {
            submitFeedback()
        }
        return view
    }

    private fun submitFeedback() {

        // Get the feedback message
        val feedbackMessage = edtxtFeedback.text.toString().trim()

        // Check if feedback message is not empty
        if (feedbackMessage.isNotEmpty()) {
            // Get current user ID
            val currentUserID = FirebaseAuth.getInstance().currentUser?.uid

            // Generate a unique feedback ID
            val feedbackID = DatabaseAdapter.feedback.push().key ?: ""

            // Get current timestamp
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

            // Create a data structure to hold the feedback information
            val feedbackData = hashMapOf(
                "userId" to currentUserID,
                "feedbackId" to feedbackID,
                "message" to feedbackMessage,
                "timestamp" to timestamp
            )

            if (isAdded) {
                (requireActivity() as BaseActivity).showProgressBar()
            }
            // Store the feedback in the Firebase Realtime Database
            DatabaseAdapter.feedback.child(currentUserID ?: "").child(feedbackID).setValue(feedbackData)
                .addOnSuccessListener {
                    if (isAdded) {
                        (requireActivity() as BaseActivity).hideProgressBar()
                        Utils.showToast(
                            requireContext(),
                            "Feedback Submited Successfully",
                        )
                    }
                }
                .addOnFailureListener { e ->
                    if (isAdded) {
                        (requireActivity() as BaseActivity).hideProgressBar()
                        Utils.showToast(
                            requireContext(),
                            "Failed To submit Feedback : ${e.message}",
                        )
                    }
                }
        } else {
            if (isAdded) {
                Utils.showToast(
                    requireContext(),
                    "Feedback cannot be empty.",
                )
            }
        }
    }
}
