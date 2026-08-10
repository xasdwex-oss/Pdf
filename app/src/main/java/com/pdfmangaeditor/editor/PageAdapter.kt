package com.pdfmangaeditor.editor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pdfmangaeditor.databinding.ItemPageBinding
import com.pdfmangaeditor.models.MangaPage
import com.pdfmangaeditor.utils.PageRenderer

class PageAdapter(
    private val onDeleteClick: (Int) -> Unit,
    private val onPageClick: (Int) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit
) : RecyclerView.Adapter<PageAdapter.PageViewHolder>() {

    private var pages: MutableList<MangaPage> = mutableListOf()

    fun submitList(newPages: MutableList<MangaPage>) {
        pages = newPages
        notifyDataSetChanged()
    }

    fun getPages(): MutableList<MangaPage> = pages

    fun moveItem(from: Int, to: Int) {
        val item = pages.removeAt(from)
        pages.add(to, item)
        notifyItemMoved(from, to)
        notifyItemRangeChanged(minOf(from, to), kotlin.math.abs(from - to) + 1)
    }

    fun removeItem(position: Int) {
        pages.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, pages.size - position)
    }

    inner class PageViewHolder(val binding: ItemPageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val page = pages[position]

        if (page.thumbnailDirty || page.cachedThumbnail == null) {
            page.cachedThumbnail = PageRenderer.flattenThumbnail(page)
            page.thumbnailDirty = false
        }

        holder.binding.imgThumbnail.setImageBitmap(page.cachedThumbnail)
        holder.binding.tvPageNumber.text = (position + 1).toString()

        holder.binding.btnDelete.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onDeleteClick(pos)
        }
        holder.binding.root.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onPageClick(pos)
        }
        holder.binding.dragHandle.setOnTouchListener { _, event ->
            if (event.actionMasked == android.view.MotionEvent.ACTION_DOWN) onStartDrag(holder)
            false
        }
    }

    override fun getItemCount() = pages.size
}
