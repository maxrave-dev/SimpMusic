package com.maxrave.simpmusic.adapter.queue

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class RecyclerRowMoveCallback(
    private val touchHelperContract: RecyclerViewRowTouchHelperContract,
) : ItemTouchHelper.Callback() {
    private var dragFrom: Int? = null
    private var dragTo: Int? = null

    override fun isLongPressDragEnabled(): Boolean = true

    override fun isItemViewSwipeEnabled(): Boolean = false

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
    ): Int {
        val dragFlag = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        return makeMovementFlags(dragFlag, 0)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder,
    ): Boolean {
        if (viewHolder.itemViewType != target.itemViewType) {
            return false
        }

        // remember FIRST from position
        if (dragFrom == null) {
            dragFrom = viewHolder.bindingAdapterPosition
        }
        dragTo = target.bindingAdapterPosition

        // Notify the adapter of the move
        touchHelperContract.onRowMove(
            viewHolder.bindingAdapterPosition,
            target.bindingAdapterPosition,
        )
        return true
    }

    override fun onSelectedChanged(
        viewHolder: RecyclerView.ViewHolder?,
        actionState: Int,
    ) {
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
            if (viewHolder is QueueAdapter.QueueViewHolder) {
                val myViewHolder: QueueAdapter.QueueViewHolder =
                    viewHolder
                touchHelperContract.onRowSelected(myViewHolder)
            }
        }
        super.onSelectedChanged(viewHolder, actionState)
    }

    override fun clearView(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
    ) {
        super.clearView(recyclerView, viewHolder)
        if (viewHolder is QueueAdapter.QueueViewHolder) {
            val myViewHolder: QueueAdapter.QueueViewHolder =
                viewHolder
            touchHelperContract.onRowClear(myViewHolder)
        }
        val from = dragFrom
        val to = dragTo
        if (from != null && to != null) {
            reallyMoved(from, to)
        }

        // clear saved positions
        dragTo = null
        dragFrom = null
    }

    private fun reallyMoved(
        from: Int,
        to: Int,
    ) {
        touchHelperContract.onRowMoved(from, to)
    }

    override fun onSwiped(
        viewHolder: RecyclerView.ViewHolder,
        direction: Int,
    ) {}

    interface RecyclerViewRowTouchHelperContract {
        fun onRowMoved(
            from: Int,
            to: Int,
        )

        fun onRowMove(
            from: Int,
            to: Int,
        )

        fun onRowSelected(myViewHolder: QueueAdapter.QueueViewHolder?)

        fun onRowClear(myViewHolder: QueueAdapter.QueueViewHolder?)
    }
}