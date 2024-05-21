import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.example.find_my_notes.R
import com.example.find_my_notes.fragments.ProfileFragment
import com.example.find_my_notes.fragments.UserProfileFragment
import com.squareup.picasso.Picasso

class UserSearchRecycleAdapter(private val context: Context,private val fragmentManager: FragmentManager) : RecyclerView.Adapter<UserSearchRecycleAdapter.ViewHolder>() {

    private var userList: List<String> = ArrayList()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgUserSearch: ImageView = itemView.findViewById(R.id.imgUserSearch)
        val txtUserSearchName: TextView = itemView.findViewById(R.id.txtUserSearchName)
        val txtUserSearchEmail: TextView = itemView.findViewById(R.id.txtUserSearchEmail)
        val txtUserSearchDept: TextView = itemView.findViewById(R.id.txtUserSearchDept)
        private val btnUserSearchProfile: Button = itemView.findViewById(R.id.btnUserSearchProfile)

        init {
            btnUserSearchProfile.setOnClickListener {
                val userInfo = userList[adapterPosition].split("\n")
                val userId = userInfo[4] // Assuming user ID is stored at index 4
                val fragment = UserProfileFragment()
                val bundle = Bundle()
                bundle.putString("USER_ID_KEY", userId)
                fragment.arguments = bundle
                fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.user_search_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val userInfo = userList[position].split("\n")
        holder.txtUserSearchName.text = userInfo[0]
        holder.txtUserSearchEmail.text = userInfo[1]
        holder.txtUserSearchDept.text = userInfo[2]

        // Load image using Picasso
        Picasso.get().load(userInfo[3]).placeholder(R.drawable.profile_pic)
            .into(holder.imgUserSearch)
    }


    override fun getItemCount(): Int {
        return userList.size
    }

    fun updateData(newList: List<String>) {
        userList = newList
        notifyDataSetChanged()
    }
}

