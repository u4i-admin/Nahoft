//package org.nahoft.nahoft
//
//import android.view.View
//import android.view.ViewGroup
//import androidx.recyclerview.widget.RecyclerView
//import org.nahoft.util.inflate
//
//class ContactsRecyclerAdapter(private val contacts: ArrayList<Contact>) : RecyclerView.Adapter<ContactsRecyclerAdapter.ContactViewHolder>()
//{
//    var onItemClick: ((Contact) -> Unit)? = null
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder
//    {
//        val inflatedView = parent.inflate(R.layout.contact_recyclerview_item_row, false)
//        return ContactViewHolder(inflatedView)
//    }
//
//    override fun onBindViewHolder(holder: ContactViewHolder, position: Int)
//    {
//        val itemContact = contacts[position]
//        holder.bindContact(itemContact)
//        holder.itemView.setOnClickListener {
//            onItemClick?.invoke(itemContact)
//        }
//    }
//
//    override fun getItemCount() = contacts.size
//
//    fun cleanup()
//    {
//        contacts.clear()
//    }
//
//    inner class ContactViewHolder(v: View) : RecyclerView.ViewHolder(v)
//    {
//        private var contact: Contact? = null
//        private var view: View = v
//
//        init
//        {
//            v.setOnClickListener {
//                contact?.let { contact ->
//                    onItemClick?.invoke(contact)
//                }
//            }
//        }
//
//        fun bindContact(newContact: Contact)
//        {
//            this.contact = newContact
//            this.view.contact_name_text_view.text = newContact.name
//            this.view.phone_number_text_view.text = newContact.number
//            this.view.friend_picture.text = newContact.name.substring(0, 1)
//        }
//    }
//}
