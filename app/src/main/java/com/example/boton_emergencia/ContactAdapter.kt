package com.example.boton_emergencia

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ContactAdapter(
    private var items: MutableList<Contact>,
    private val listener: Listener
) : RecyclerView.Adapter<ContactAdapter.ViewHolder>() {

    interface Listener {
        fun onEdit(contact: Contact)
        fun onDelete(contact: Contact)
        fun onClick(contact: Contact)
        fun onSelect(contact: Contact)
    }

    fun setItems(newItems: List<Contact>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    // single selection position
    private var selectedPos = RecyclerView.NO_POSITION
    private var selectedContactId: Long = -1L

    /**
     * Set selected contact by id (used to restore previous selection)
     */
    fun setSelectedContactId(contactId: Long) {
        selectedContactId = contactId
        selectedPos = items.indexOfFirst { it.contactId == contactId }
        if (selectedPos == -1) selectedPos = RecyclerView.NO_POSITION
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val c = items[position]
        holder.label.text = c.label ?: "(sin etiqueta)"
        holder.phone.text = c.phone

    holder.itemView.setOnClickListener { listener.onClick(c) }
    holder.editBtn.setOnClickListener { listener.onEdit(c) }
    holder.deleteBtn.setOnClickListener { listener.onDelete(c) }
        // checkbox handling: show checked only for selectedPos
        holder.selectCheck.setOnCheckedChangeListener(null)
        holder.selectCheck.isChecked = (position == selectedPos)
        holder.selectCheck.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val previous = selectedPos
                selectedPos = holder.adapterPosition
                notifyItemChanged(previous)
                notifyItemChanged(selectedPos)
                selectedContactId = c.contactId
                listener.onSelect(c)
            } else {
                // if unchecked by user, clear selection
                if (selectedPos == holder.adapterPosition) {
                    selectedPos = RecyclerView.NO_POSITION
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val label: TextView = v.findViewById(R.id.contactLabel)
        val phone: TextView = v.findViewById(R.id.contactPhone)
        val editBtn: Button = v.findViewById(R.id.editContactButton)
        val deleteBtn: Button = v.findViewById(R.id.deleteContactButton)
        val selectCheck: android.widget.CheckBox = v.findViewById(R.id.selectContactCheck)
    }
}
