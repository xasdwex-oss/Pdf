package com.pdfmangaeditor.editor

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class PageDragCallback(
    private val adapter: PageAdapter,
    private val onMoveFinished: (from: Int, to: Int) -> Unit
) : ItemTouchHelper.Callback() {

    private var dragFrom = -1
    private var dragTo = -1

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        return makeMovementFlags(dragFlags, 0)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val from = viewHolder.bindingAdapterPosition
        val to = target.bindingAdapterPosition
        if (dragFrom == -1) dragFrom = from
        dragTo = to
        adapter.moveItem(from, to)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        if (dragFrom != -1 && dragTo != -1 && dragFrom != dragTo) {
            onMoveFinished(dragFrom, dragTo)
        }
        dragFrom = -1
        dragTo = -1
    }

    override fun isLongPressDragEnabled(): Boolean = false
}
