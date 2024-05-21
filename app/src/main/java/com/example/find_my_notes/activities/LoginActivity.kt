package com.example.find_my_notes.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import com.example.find_my_notes.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException

class LoginActivity : BaseActivity() {
    private var b: ActivityLoginBinding? = null
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b?.root)
        auth = FirebaseAuth.getInstance()

        //  Open Home(Main) Page on click of Login
        b?.btnLogin?.setOnClickListener {
            val email = b?.edtxtUserEmail?.text.toString().trim()
            val password = b?.edtxtPassword?.text.toString().trim()

            if (email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email)
                    .matches() && password.isNotEmpty()
            ) {
                try {
                    showProgressBar()
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                try {
                                    val verification = auth.currentUser?.isEmailVerified
                                    if (verification == true) {
                                        hideProgressBar()
                                        showToast(this, "Login Successful")
                                        val intent = Intent(this, MainActivity::class.java)
                                        intent.putExtra("uEmail", email)
                                        startActivity(intent)
                                        finish()
                                    } else {
                                        hideProgressBar()
                                        showToast(this, "Please Verify your Email")
                                    }
                                } catch (e: Exception) {
                                    hideProgressBar()
                                    showToast(this, "Error : ${e.message}")
                                }
                            } else if (task.exception is FirebaseAuthInvalidCredentialsException) {
                                hideProgressBar()
                                showToast(this, "Please Enter Valid Email and Password")
                            } else {
                                hideProgressBar()
                                showToast(this, "Please Enter Valid Email and Password")
                            }
                        }.addOnFailureListener {
                            hideProgressBar()
                            Log.e("Exception", "Error " + it.message.toString())
                        }
                } catch (e: Exception) {
                    showToast(this, e.message.toString())
                }
            } else {
                showToast(this, "Please Enter Email and Password")
            }
        }

        //  Open Signup Page on click of Login
        b?.txtSignupLink?.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
            finish()
        }

        //  Open ForgotPassword Dialog Page on click of Login
        b?.txtForgotPasswordLink?.setOnClickListener {
            startActivity(Intent(this, ForgotpasswordActivity::class.java))
            finish()
        }
    }

    public override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            if (currentUser.isEmailVerified) {
                // If user is signed in and email is verified, navigate to home page
                startActivity(Intent(this, MainActivity::class.java))
                finish() // Finish current activity to prevent user from navigating back
            }
        }
    }
}