package com.qmobile.qmobileui.activity.mainactivity

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.Action

class ActionDropDownAdapter(
    val context: Context,
    private val items: ArrayList<Action>,
    val onMenuItemClick: (Action) -> Unit
) :
    BaseAdapter() {
    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(position: Int): Action {
        return items[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var newConvertView = convertView
        val item = getItem(position)
        if (newConvertView == null) {
            val inflater = context
                .getSystemService(AppCompatActivity.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            newConvertView = inflater.inflate(R.layout.drop_down_item_row, parent, false)
        }
        val itemText = newConvertView?.findViewById<View>(R.id.dropDownItemName) as TextView
        val itemImage: ImageView =
            newConvertView.findViewById<View>(R.id.dropDownItemImage) as ImageView
        itemText.text = item.getPreferredName()

        val iconDrawablePath = item.getIconDrawablePath()
        val resId = if (iconDrawablePath != null) {
            context.resources.getIdentifier(
                iconDrawablePath,
                "drawable",
                context.packageName
            )
        } else {
            0
        }
        itemImage.setImageResource(resId)
        newConvertView.setOnClickListener { _ ->
            onMenuItemClick(item)
        }
        return newConvertView
    }
}
