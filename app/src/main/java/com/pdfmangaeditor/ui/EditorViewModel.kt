package com.pdfmangaeditor.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pdfmangaeditor.models.MangaPage

class EditorViewModel : ViewModel() {

    private val _pages = MutableLiveData<MutableList<MangaPage>>(mutableListOf())
    val pages: LiveData<MutableList<MangaPage>> = _pages

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isDirty = MutableLiveData(false)
    val isDirty: LiveData<Boolean> = _isDirty

    fun setPages(newPages: MutableList<MangaPage>) {
        _pages.value = newPages
        _isDirty.value = false
    }

    fun deletePage(position: Int) {
        val list = _pages.value ?: return
        if (position in list.indices) {
            list.removeAt(position)
            _pages.value = list
            _isDirty.value = true
        }
    }

    fun movePage(from: Int, to: Int) {
        val list = _pages.value ?: return
        if (from !in list.indices || to !in list.indices) return
        val item = list.removeAt(from)
        list.add(to, item)
        _pages.value = list
        _isDirty.value = true
    }

    fun addPage(page: MangaPage, atIndex: Int = -1) {
        val list = _pages.value ?: mutableListOf()
        if (atIndex in list.indices) list.add(atIndex, page) else list.add(page)
        _pages.value = list
        _isDirty.value = true
    }

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    fun currentPages(): MutableList<MangaPage> = _pages.value ?: mutableListOf()
}
