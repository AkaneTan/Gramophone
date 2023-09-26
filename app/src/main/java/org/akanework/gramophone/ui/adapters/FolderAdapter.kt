package org.akanework.gramophone.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.ui.fragments.FolderBrowserFragment

class FolderAdapter(private val folderList: MutableList<MediaStoreUtils.FileNode>,
                    private val supportFragmentManager: FragmentManager)
    : RecyclerView.Adapter<FolderAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderAdapter.ViewHolder =
        ViewHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.adapter_folder_card, parent, false),
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.folderName.text = folderList[holder.bindingAdapterPosition].folderName
        holder.itemView.setOnClickListener {
            supportFragmentManager
                .beginTransaction()
                .addToBackStack("BROWSABLE")
                .replace(R.id.browser, FolderBrowserFragment(folderList[holder.bindingAdapterPosition]))
                .commit()
        }
    }

    override fun getItemCount(): Int = folderList.size

    inner class ViewHolder(
        view: View,
    ) : RecyclerView.ViewHolder(view) {
        val folderName: TextView = view.findViewById(R.id.title)
    }

}


class FolderPopAdapter(private val supportFragmentManager: FragmentManager)
    : RecyclerView.Adapter<FolderPopAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderPopAdapter.ViewHolder =
        ViewHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.adapter_folder_popup, parent, false),
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.setOnClickListener {
            supportFragmentManager.popBackStack()
        }
    }

    override fun getItemCount(): Int = 1

    inner class ViewHolder(
        view: View,
    ) : RecyclerView.ViewHolder(view)

}