/**
 * This file (ReflectionAdapter.kt) is dual-licensed (you can choose one):
 * - Project license (as of writing GPL-3.0-or-later)
 * - Apache-2.0
 * Because it's really only useful for debugging, and I hope this will be of use for someone.
 * Copyright (C) 2024 nift4
 */

package org.akanework.gramophone

import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

class ReflectionAdapter(
	private val obj: Any?,
	parent: ReflectionAdapter?,
	private val cb: (ReflectionAdapter) -> Unit,
	listPreview: Boolean
) : RecyclerView.Adapter<ReflectionAdapter.SimpleVH>() {
	private val nextObjects: ArrayList<Pair<String, Pair<(() -> ReflectionAdapter)?, (() -> Any?)?>>> = arrayListOf()
	init {
		if (parent != null) {
			nextObjects.add(Pair("..", Pair({ parent }, null)))
		}
		if (obj != null) {
			if (obj is List<*>) {
				nextObjects.add(Pair(obj::class.qualifiedName + " with entry count " + obj.size, Pair({ ReflectionAdapter(obj, parent, cb, !listPreview) }, null)))
				for (i in 0..<obj.size) {
					nextObjects.add(Pair(i.toString() + if (listPreview) (": " + obj[i].toString()) else "", Pair(null) { obj[i] }))
				}
			} else {
				nextObjects.add(Pair(obj.toString(), Pair(null, null)))
				val mbz = obj::class.memberProperties
				for (m in mbz) {
					try {
						m.isAccessible = true
						nextObjects.add(Pair(m.name, Pair(null) { m.call(obj) }))
					} catch (e: Exception) {
						nextObjects.add(Pair("INACCESSIBLE: " + m.name, Pair(null, null)))
					} catch (e: Error) {
						nextObjects.add(Pair("INACCESSIBLE: " + m.name, Pair(null, null)))
					}
				}
				val fnz = obj::class.memberFunctions
				for (m in fnz) {
					try {
						m.isAccessible = true
						nextObjects.add(Pair(m.name + '(' + m.toString()
							.substringAfter('(', missingDelimiterValue = "NOT CALLABLE)"),
							Pair(null) { m.call(obj) }))
					} catch (e: Exception) {
						nextObjects.add(Pair("INACCESSIBLE: " + m.name, Pair(null, null)))
					} catch (e: Error) {
						nextObjects.add(Pair("INACCESSIBLE: " + m.name, Pair(null, null)))
					}
				}
			}
		} else nextObjects.add(Pair("null", Pair(null, null)))
	}

	inner class SimpleVH(itemView: TextView)
		: RecyclerView.ViewHolder(itemView), OnClickListener {
		init {
			itemView.setOnClickListener(this)
		}

		private lateinit var nextObj: Pair<(() -> ReflectionAdapter)?, (() -> Any?)?>

		override fun onClick(v: View?) {
			if (nextObj.first != null) {
				cb(nextObj.first!!())
			} else if (nextObj.second != null) {
				cb(ReflectionAdapter(nextObj.second!!(), this@ReflectionAdapter, cb, false))
			}
		}

		fun bind(text: CharSequence, nextObj: Pair<(() -> ReflectionAdapter)?, (() -> Any?)?>) {
			(itemView as TextView).text = text
			this.nextObj = nextObj
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup, type: Int): SimpleVH {
		return SimpleVH(
			LayoutInflater.from(parent.context)
				.inflate(android.R.layout.simple_list_item_1, parent, false) as TextView
		)
	}

	override fun getItemCount(): Int = nextObjects.size

	override fun onBindViewHolder(viewHolder: SimpleVH, position: Int) {
		val item = nextObjects[position]
		viewHolder.bind(item.first, item.second)
	}

}