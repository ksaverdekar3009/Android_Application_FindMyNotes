package com.example.find_my_notes.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.find_my_notes.activities.ForgotpasswordActivity
import com.example.find_my_notes.R
import com.example.find_my_notes.activities.BaseActivity
import com.example.find_my_notes.adapters.DatabaseAdapter
import com.example.find_my_notes.databinding.FragmentChangePasswordBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference

class ChangePasswordFragment : Fragment() {

    private var b: FragmentChangePasswordBinding? = null
    private lateinit var currentUser: FirebaseUser
    private lateinit var databaseRefUsers: DatabaseReference

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
        b = FragmentChangePasswordBinding.inflate(inflater, container, false)

        currentUser = DatabaseAdapter.returnUser()!!
        databaseRefUsers = DatabaseAdapter.users

        b?.btnChangePasswordCurrent?.setOnClickListener {
            val currentPassword = b?.edtxtChangePasswordCurrent?.text.toString()
            val newPassword = b?.edtxtChangePasswordNew?.text.toString()
            val confirmNewPassword = b?.edtxtChangePasswordConfirmCurrent?.text.toString()

            if (isAdded) {
                (requireActivity() as BaseActivity).showProgressBar()
            }
            if (newPassword == confirmNewPassword) {
                val credential =
                    EmailAuthProvider.getCredential(currentUser.email!!, currentPassword)
                currentUser.reauthenticate(credential)
                    .addOnCompleteListener { reAuthTask ->
                        if (reAuthTask.isSuccessful) {
                            currentUser.updatePassword(newPassword)
                                .addOnCompleteListener { updatePasswordTask ->
                                    if (updatePasswordTask.isSuccessful) {
                                        // Update password in Realtime Database
                                        databaseRefUsers.child(currentUser.uid)
                                            .child("password")
                                            .setValue(newPassword)
                                        //Clear Fields
                                        b?.edtxtChangePasswordCurrent?.text?.clear()
                                        b?.edtxtChangePasswordNew?.text?.clear()
                                        b?.edtxtChangePasswordConfirmCurrent?.text?.clear()
                                        //Show message
                                        if (isAdded) {
                                            (requireActivity() as BaseActivity).hideProgressBar()
                                            Utils.showToast(
                                                requireContext(),
                                                "Password changed successfully",
                                            )
                                        }
                                    } else {
                                        // Handle password update failure
                                        if (updatePasswordTask.exception is FirebaseAuthWeakPasswordException) {
                                            // Handle weak password exception
                                            val weakPasswordException =
                                                updatePasswordTask.exception as FirebaseAuthWeakPasswordException
                                            val errorReason =
                                                weakPasswordException.reason
                                                    ?: "Password should be at least 6 characters long"
                                            if (isAdded) {
                                                (requireActivity() as BaseActivity).hideProgressBar()
                                                Utils.showToast(
                                                    requireContext(),
                                                    errorReason,
                                                )
                                            }
                                        }
                                    }
                                }
                        } else {
                            // Handle reauthentication failure
                            if (isAdded) {
                                (requireActivity() as BaseActivity).hideProgressBar()
                                Utils.showToast(
                                    requireContext(),
                                    "Please Enter Valid Current Password",
                                )
                            }
                        }
                    }
            } else {
                // New password and confirm new password don't match
                if (isAdded) {
                    (requireActivity() as BaseActivity).hideProgressBar()
                    Utils.showToast(
                        requireContext(),
                        "New password and confirm new password don't match",
                    )
                }
            }
        }

        // Start ForgotPasswordActivity
        b?.txtChangePasswordForgotLink?.setOnClickListener {
            val intent = Intent(requireContext(), ForgotpasswordActivity::class.java)
            startActivity(intent)
        }
        return b?.root
    }


    override fun onResume() {
        super.onResume()
        requireActivity().title = "Change Password" // Set the toolbar title

        // Hide the bottom navigation (assuming it has an ID like `bottomNavigationView`)
        val bottomNavigationView = requireActivity().findViewById<View>(R.id.bottomNavigation)
        bottomNavigationView.visibility = View.GONE
    }
}