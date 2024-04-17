package org.akanework.gramophone.ui.adapters

import android.content.SharedPreferences
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.getStringSetStrict
import org.akanework.gramophone.logic.ui.MyRecyclerView

class BlacklistFolderAdapter(
    private val fragment: Fragment,
    private val folderArray: MutableList<String>,
    private val prefs: SharedPreferences
) : MyRecyclerView.Adapter<BlacklistFolderAdapter.ViewHolder>() {
    private val folderFilter = prefs.getStringSetStrict("folderFilter", null)?.
            toMutableSet() ?: mutableSetOf()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(
            fragment.layoutInflater.inflate(
                    R.layout.adapter_blacklist_folder_card,
                    parent,
                    false
                )
        )

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkBox: MaterialCheckBox = view.findViewById(R.id.checkbox)
        val folderLocation: TextView = view.findViewById(R.id.title)
    }

    override fun getItemCount(): Int = folderArray.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.checkBox.isChecked = folderFilter.contains(folderArray[position])
        holder.folderLocation.text = folderArray[position]
        holder.checkBox.setOnClickListener {
            prefs.edit {
                putStringSet("folderFilter",
                    folderFilter.also {
                        if (holder.checkBox.isChecked)
                            it.add(folderArray[position])
                        else
                            it.remove(folderArray[position])
                    })
            }
        }
        holder.itemView.setOnClickListener {
            holder.checkBox.isChecked = !holder.checkBox.isChecked
            holder.checkBox.callOnClick()
        }
    }
}