package com.example.find_my_notes.activities

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import com.example.find_my_notes.adapters.DatabaseAdapter
import com.example.find_my_notes.databinding.ActivityForgotpasswordBinding


class ForgotpasswordActivity : BaseActivity() {
    private var b: ActivityForgotpasswordBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityForgotpasswordBinding.inflate(layoutInflater)
        setContentView(b?.root)

        b?.btnReset?.setOnClickListener {
            val email = b?.edtxtFEmail?.text.toString()
            if (email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showProgressBar() // Show progress bar
                DatabaseAdapter.passwordResetMail(email) { result ->
                    hideProgressBar() // Hide progress bar after operation is complete
                    when (result) {
                        "true" -> {
                            showToast(this, "Password reset email sent successfully")
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                        "exist" -> showToast(this, "User does not exist")
                        "false" -> showToast(this, "Failed to send password reset email")
                    }
                }
            } else {
                showToast(this, "Please Enter Valid Email")
            }
        }
    }
}