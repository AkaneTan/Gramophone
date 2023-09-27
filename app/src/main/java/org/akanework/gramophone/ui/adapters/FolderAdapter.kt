package org.akanework.gramophone.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.ui.fragments.FolderBrowserFragment

class FolderAdapter(private var folderList: MutableList<MediaStoreUtils.FileNode>,
                    frag: FragmentManager)
    : FolderCardAdapter(frag) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.folderName.text = folderList[position].folderName
        holder.itemView.setOnClickListener {
            supportFragmentManager
                .beginTransaction()
                .addToBackStack("BROWSABLE")
                .replace(R.id.browser, FolderBrowserFragment(folderList[position]))
                .commit()
        }
    }

    override fun getItemCount(): Int = folderList.size

    fun updateList(newList: MutableList<MediaStoreUtils.FileNode>) {
        val diffResult = DiffUtil.calculateDiff(DiffCallback(folderList, newList))
        folderList = newList
        diffResult.dispatchUpdatesTo(this)
    }

    private inner class DiffCallback(
        private val oldList: MutableList<MediaStoreUtils.FileNode>,
        private val newList: MutableList<MediaStoreUtils.FileNode>,
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(
            oldItemPosition: Int,
            newItemPosition: Int,
        ) = oldList[oldItemPosition].folderName == newList[newItemPosition].folderName

        override fun areContentsTheSame(
            oldItemPosition: Int,
            newItemPosition: Int,
        ) = oldList[oldItemPosition] == newList[newItemPosition]
    }
}

class FolderPopAdapter(frag: FragmentManager) : FolderCardAdapter(frag) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        super.onCreateViewHolder(parent, viewType).apply {
            folderName.text = parent.context.getString(R.string.upper_folder)
        }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.setOnClickListener {
            supportFragmentManager.popBackStack()
        }
    }

    override fun getItemCount(): Int = 1
}

abstract class FolderCardAdapter(protected val supportFragmentManager: FragmentManager)
    : RecyclerView.Adapter<FolderCardAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.adapter_folder_card, parent, false),
        )

    inner class ViewHolder(
        view: View,
    ) : RecyclerView.ViewHolder(view) {
        val folderName: TextView = view.findViewById(R.id.title)
    }
}