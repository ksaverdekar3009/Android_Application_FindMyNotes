package com.example.find_my_notes.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.example.find_my_notes.R
import com.squareup.picasso.Picasso

class CommentAdapter(private val context: Context, private val fragmentManager: FragmentManager) :
    RecyclerView.Adapter<CommentAdapter.ViewHolder>() {

    private val commentList = ArrayList<String>()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtCommentUserName: TextView = itemView.findViewById(R.id.txtCommentCommenterName)
        val txtCommentDescription: TextView = itemView.findViewById(R.id.txtCommentDescription)
        val commentImg: ImageView = itemView.findViewById(R.id.commentImg)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.comments_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentAdapter.ViewHolder, position: Int) {
        val commentData = commentList[position].split("\n")
        holder.txtCommentDescription.text = commentData[0]
        holder.txtCommentUserName.text = commentData[1]
        Picasso.get().load(commentData[2]).placeholder(R.drawable.profile_pic).into(holder.commentImg)
    }

    override fun getItemCount(): Int {
        return commentList.size
    }

    fun updateData(newList: List<String>) {
        commentList.clear()
        commentList.addAll(newList)
        notifyDataSetChanged()
    }
}
