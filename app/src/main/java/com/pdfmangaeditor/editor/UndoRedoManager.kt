package com.pdfmangaeditor.editor

class UndoRedoManager<S>(
    private val maxHistory: Int = 30,
    private val deepCopy: (S) -> S
) {
    private val undoStack = ArrayDeque<S>()
    private val redoStack = ArrayDeque<S>()

    fun recordState(current: S) {
        undoStack.addLast(deepCopy(current))
        if (undoStack.size > maxHistory) undoStack.removeFirst()
        redoStack.clear()
    }

    fun undo(current: S): S? {
        if (undoStack.isEmpty()) return null
        redoStack.addLast(deepCopy(current))
        return undoStack.removeLast()
    }

    fun redo(current: S): S? {
        if (redoStack.isEmpty()) return null
        undoStack.addLast(deepCopy(current))
        return redoStack.removeLast()
    }

    fun canUndo() = undoStack.isNotEmpty()
    fun canRedo() = redoStack.isNotEmpty()

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
