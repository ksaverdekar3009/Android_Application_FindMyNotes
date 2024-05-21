package com.example.find_my_notes.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.example.find_my_notes.R
import com.example.find_my_notes.adapters.DatabaseAdapter
import com.example.find_my_notes.databinding.ActivityMainBinding
import com.example.find_my_notes.fragments.AboutUsFragment
import com.example.find_my_notes.fragments.ChangePasswordFragment
import com.example.find_my_notes.fragments.EditProfileFragment
import com.example.find_my_notes.fragments.FeedbackFragment
import com.example.find_my_notes.fragments.HomeFragment
import com.example.find_my_notes.fragments.PostFragment
import com.example.find_my_notes.fragments.ProfileFragment
import com.example.find_my_notes.fragments.SavedNotesFragment
import com.example.find_my_notes.fragments.SearchFragment
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso

class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {
    private var b: ActivityMainBinding? = null
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var auth: FirebaseAuth
    private var backPressedOnce = false
    private var currentFragment: Fragment? = null
    
    private lateinit var currentUser: FirebaseUser
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b?.root)
        auth = FirebaseAuth.getInstance()
        setSupportActionBar(b?.toolbar)

        if (auth.currentUser == null) {
            logout()
            return
        }

        toggle = ActionBarDrawerToggle(
            this,
            b?.layoutDrawer,
            b?.toolbar,
            R.string.nav_open,
            R.string.nav_close
        )
        b?.layoutDrawer?.addDrawerListener(toggle)

        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        b?.toolbar?.setNavigationIcon(R.drawable.baseline_menu_open_24)
        b?.navigationDrawer?.setNavigationItemSelectedListener(this)

        //Load Details in Navigation Header
        val headerView = b!!.navigationDrawer.getHeaderView(0)
        val navProfilePic: ImageView = headerView.findViewById(R.id.navProfilePic)
        val navFullName: TextView = headerView.findViewById(R.id.navFullName)
        val navEmail: TextView = headerView.findViewById(R.id.navEmail)

        currentUser = DatabaseAdapter.returnUser()!!
        databaseReference = DatabaseAdapter.users

        // Retrieve the details from the database
        databaseReference.child(currentUser.uid).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val imageUrl = dataSnapshot.child("imageUrl").value.toString()
                navFullName.text = dataSnapshot.child("fullname").value.toString()
                navEmail.text = dataSnapshot.child("email").value.toString()

                // Load image from the URL into navProfilePic ImageView
                Picasso.get().load(imageUrl).placeholder(R.drawable.profile_pic).into(navProfilePic)

            }

            override fun onCancelled(databaseError: DatabaseError) {
                navFullName.text = ""
                navEmail.text = ""
                navProfilePic.setImageResource(R.drawable.profile_pic)
            }
        })

        //Bottom Navigation
        b?.bottomNavigation?.background = null
        b?.bottomNavigation?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.bottomHome -> {
                    b?.bottomNavigation?.visibility = View.VISIBLE
                    replaceFragment(HomeFragment())

                }

                R.id.bottomPost -> {
                    b?.bottomNavigation?.visibility = View.VISIBLE
                    replaceFragment(PostFragment())
                }

                R.id.bottomProfile -> {
                    b?.bottomNavigation?.visibility = View.VISIBLE
                    replaceFragment(ProfileFragment())
                }

                R.id.bottomSearch -> {
                    b?.bottomNavigation?.visibility = View.VISIBLE
                    replaceFragment(SearchFragment())
                }

                else -> false
            }
        }
        b?.bottomNavigation?.selectedItemId = R.id.bottomHome
        replaceFragment(HomeFragment())

        //Set Toolbar home to
        b?.toolbarHomeIcon?.setOnClickListener {
            b?.bottomNavigation?.selectedItemId = R.id.bottomHome
            replaceFragment(HomeFragment())
        }
    }


    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.navHome -> {
                replaceFragment(HomeFragment())
                b?.bottomNavigation?.selectedItemId = R.id.bottomHome
            }

            R.id.navSavedNotes -> replaceFragment(SavedNotesFragment())
            R.id.navChangePassword -> replaceFragment(ChangePasswordFragment())
            R.id.navLogout -> logout()
            R.id.RateUs -> showRateUsDialog()
            R.id.Help -> openHelpPage()
            R.id.Share -> shareApp()
            R.id.AboutUs -> replaceFragment(AboutUsFragment())
            R.id.Feedback -> replaceFragment(FeedbackFragment())
        }
        b?.layoutDrawer?.closeDrawer(GravityCompat.START)
        return true
    }

    git init
    fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item))
            return true
        return super.onOptionsItemSelected(item)
    }

    @Deprecated("Deprecated in Java", level = DeprecationLevel.WARNING)
    override fun onBackPressed() {
        if (b?.layoutDrawer?.isDrawerOpen(GravityCompat.START) == true) {
            b?.layoutDrawer?.closeDrawer(GravityCompat.START)
        } else {
            if (backPressedOnce) {
                super.onBackPressed()
            } else {
                backPressedOnce = true
                showToast(this, "Press Back again to exit")
                Handler().postDelayed(
                    { backPressedOnce = false },
                    2000
                )
            }
        }
    }

    private fun showRateUsDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Rate Us")
        builder.setMessage("If you enjoy using this app, please take a moment to rate it. Thanks for your support!")
        builder.setPositiveButton("Rate Now") { _, _ ->
            // Open app in Google Play Store for rating
            val uri = Uri.parse("market://details?id=${packageName}")
            val rateIntent = Intent(Intent.ACTION_VIEW, uri)
            try {
                startActivity(rateIntent)
            } catch (e: ActivityNotFoundException) {
                // Handle case where Google Play Store app is not installed
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=${packageName}")
                )
                startActivity(webIntent)
            }
        }
        builder.setNegativeButton("Not Now") { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun shareApp() {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Check out this app!")
        shareIntent.putExtra(
            Intent.EXTRA_TEXT,
            "I found this amazing app. Check it out: https://play.google.com/store/apps/details?id=${packageName}"
        )
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    private fun openHelpPage() {
        val googleHelpUrl = "https://support.google.com"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(googleHelpUrl))
        startActivity(intent)
    }

    private fun logout() {
        showProgressBar()
        if (auth.currentUser != null) {
            hideProgressBar()
            auth.signOut()
            showToast(this, "Logged out Successfully")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            hideProgressBar()
            showToast(this, "Unknown Error")
        }
    }

    private fun replaceFragment(fragment: Fragment): Boolean {
        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
//        transaction.addToBackStack(null)
        transaction.commit()
        return true
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser == null) {
            logout()
            return
        }
    }

    override fun onResume() {
        super.onResume()
        if (auth.currentUser == null) {
            logout()
            return
        }
    }
}