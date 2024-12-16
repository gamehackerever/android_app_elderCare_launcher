package com.srinand.induction

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.utils.widget.ImageFilterButton
import androidx.recyclerview.widget.RecyclerView

class ContactsAdapter(private val contacts: List<String>, private val onDeleteClick: (String) -> Unit) :
    RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.contact_item, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contacts[position]
        holder.bind(contact)
    }

    override fun getItemCount(): Int = contacts.size

    inner class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val contactText: TextView = itemView.findViewById(R.id.contactText)
        private val deleteButton: ImageView = itemView.findViewById(R.id.delete)

        fun bind(contact: String) {
            contactText.text = contact
            deleteButton.setOnClickListener { onDeleteClick(contact) }
        }
    }
}
