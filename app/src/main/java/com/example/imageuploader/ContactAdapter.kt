package com.example.imageuploader

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.contact_item.view.*


class ContactAdapter(var c: Context, var list: ArrayList<ContactItem>) : RecyclerView.Adapter<ContactAdapter.ViewHolder>() {

    private var contactList: ArrayList<ContactItem> = list
    private var context: Context = c
    private val dbHelper: DbHelper = DbHelper(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.contact_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contactName = list[position].name
        val phoneNumber = list[position].phoneNumber
        val isSelected = list[position].isChecked

        holder.nameTextView.text = contactName
        holder.phoneTextView.text = phoneNumber
        holder.checkBox.isChecked = isSelected

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            list[position].isChecked = isChecked
            if (isChecked) {
                dbHelper.insertContact(contactList[position])
            } else {
                dbHelper.deleteContact(contactList[position].id)
            }
        }
    }


    fun updateFilteredList(filteredList: ArrayList<ContactItem>) {
        contactList = filteredList
        println("contact list size: ${filteredList.size}")
        notifyDataSetChanged()
    }

    override fun getItemCount() = list.size

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val nameTextView = itemView.contactNameTextView
        val phoneTextView = itemView.contactPhoneTextView
        val checkBox = itemView.contactCheckBox
    }

}