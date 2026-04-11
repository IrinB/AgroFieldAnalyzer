package com.agromon.agrofieldanalyzer.utils

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SquareItemDecoration : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)

        val spanCount = (parent.layoutManager as? GridLayoutManager)?.spanCount ?: 3
        val spacing = 8
        val position = parent.getChildAdapterPosition(view)

        val column = position % spanCount

        outRect.left = if (column == 0) 0 else spacing / 2
        outRect.right = if (column == spanCount - 1) 0 else spacing / 2
        outRect.top = spacing / 2
        outRect.bottom = spacing / 2
    }
}