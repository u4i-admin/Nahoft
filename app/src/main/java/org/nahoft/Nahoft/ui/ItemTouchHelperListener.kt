package org.nahoft.nahoft.ui

import androidx.recyclerview.widget.RecyclerView

interface ItemTouchHelperListener {

    fun onItemDismiss(viewHolder: RecyclerView.ViewHolder, position: Int)
    fun onCancel(position: Int)
}