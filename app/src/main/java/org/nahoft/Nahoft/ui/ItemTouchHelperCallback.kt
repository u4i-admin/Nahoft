package org.nahoft.nahoft.ui

import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.nahoft.nahoft.R

class ItemTouchHelperCallback(private val listener: ItemTouchHelperListener, private val context: Context) : ItemTouchHelper.Callback() {

    override fun isLongPressDragEnabled()= false
    override fun isItemViewSwipeEnabled() = true


    private val background = ColorDrawable()
    private val backgroundColor = Color.parseColor("#f44336")
    private val clearPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        // Pay attention if the user drags/swipes the item left or right
        return makeMovementFlags(0, ItemTouchHelper.START)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val alertBuilder = AlertDialog.Builder(viewHolder.itemView.context)
            .setTitle(context.getString(R.string.alert_title_confirm_delete))
            .setMessage(context.getString(R.string.alert_text_confirm_delete))
            .setPositiveButton(R.string.button_label_delete){
                    dialog, id -> listener.onItemDismiss(viewHolder, viewHolder.adapterPosition)
            }
            .setNegativeButton("Cancel"){
                    dialog, id -> listener.onCancel(viewHolder.adapterPosition)
            }

        val deleteAlert = alertBuilder.create()
        deleteAlert.show()
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_remove_circle_outline_24px)
        val intrinsicWidth = deleteIcon!!.intrinsicWidth
        val intrinsicHeight = deleteIcon.intrinsicHeight
        val itemView = viewHolder.itemView
        val itemHeight = itemView.bottom - itemView.top
        val isCanceled = dX == 0f && !isCurrentlyActive

        if (isCanceled) {
            clearCanvas(c, itemView.right + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            return
        }

        // Draw the red delete background
        background.color = backgroundColor
        background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
        background.draw(c)

        // Calculate position of delete icon
        val deleteIconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
        val deleteIconMargin = (itemHeight - intrinsicHeight) / 2
        val deleteIconLeft = itemView.right - deleteIconMargin - intrinsicWidth
        val deleteIconRight = itemView.right - deleteIconMargin
        val deleteIconBottom = deleteIconTop + intrinsicHeight

        // Draw the delete icon
        deleteIcon.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom)
        deleteIcon.draw(c)

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    private fun clearCanvas(c: Canvas?, left: Float, top: Float, right: Float, bottom: Float) {
        c?.drawRect(left, top, right, bottom, clearPaint)
    }

}