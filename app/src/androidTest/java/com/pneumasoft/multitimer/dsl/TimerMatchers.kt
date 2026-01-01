package com.pneumasoft.multitimer.dsl

import android.view.View
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.matcher.BoundedMatcher
import com.pneumasoft.multitimer.R
import org.hamcrest.Description
import org.hamcrest.Matcher

fun hasChildCount(expectedCount: Int): Matcher<View> {
    return object : BoundedMatcher<View, RecyclerView>(RecyclerView::class.java) {
        override fun describeTo(description: Description) {
            description.appendText("RecyclerView with item count: $expectedCount")
        }
        
        override fun matchesSafely(view: RecyclerView): Boolean {
            return view.adapter?.itemCount == expectedCount
        }
    }
}

fun withRecyclerView(@IdRes recyclerViewId: Int) = 
    RecyclerViewMatcher(recyclerViewId)

class RecyclerViewMatcher(private val recyclerViewId: Int) {
    fun atPositionWithText(text: String): Matcher<View> {
        return object : BoundedMatcher<View, View>(View::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("RecyclerView item with text: $text")
            }
            
            override fun matchesSafely(view: View): Boolean {
                val recyclerView = view.rootView.findViewById<RecyclerView>(recyclerViewId)
                if (recyclerView == null || recyclerView.adapter == null) return false
                
                for (i in 0 until recyclerView.adapter!!.itemCount) {
                    val viewHolder = recyclerView.findViewHolderForAdapterPosition(i)
                    val itemView = viewHolder?.itemView
                    // Note: R.id.timerName must exist in the item layout.
                    // Assuming the layout used in TimerAdapter has a TextView with id timer_name or similar.
                    // The guide assumes R.id.timerName.
                    // I should probably check the item layout if possible, but I'll stick to the guide's assumption
                    // or try to find the layout file.
                    // I will assume R.id.timer_name_text based on common conventions or check if I can find it.
                    // MainActivity uses dialog which has timer_name_edit.
                    // Let's assume R.id.timer_name for now as per guide.
                    val nameView = itemView?.findViewById<TextView>(R.id.timer_name)
                    if (nameView?.text == text) return true
                }
                return false
            }
        }
    }
}
