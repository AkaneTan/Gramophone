package org.akanework.gramophone.ui.adapters

import android.net.Uri
import org.akanework.gramophone.logic.utils.SupportComparator

class Sorter<T>(val sortingHelper: Helper<T>,
                private val naturalOrderHelper: NaturalOrderHelper<T>?) {

	abstract class Helper<T>(typesSupported: Set<Type>) {
		init {
			if (typesSupported.contains(Type.NaturalOrder) || typesSupported.contains(Type.None))
				throw IllegalStateException()
		}
		val typesSupported = typesSupported.toMutableSet().apply { add(Type.None) }.toSet()
		abstract fun getTitle(item: T): String?
		abstract fun getId(item: T): String
		abstract fun getCover(item: T): Uri?
		open fun getArtist(item: T): String? = throw UnsupportedOperationException()
		open fun getAlbumTitle(item: T): String? = throw UnsupportedOperationException()
		open fun getAlbumArtist(item: T): String? = throw UnsupportedOperationException()
		open fun getSize(item: T): Int = throw UnsupportedOperationException()
		fun canGetTitle(): Boolean = typesSupported.contains(Type.ByTitleAscending)
				|| typesSupported.contains(Type.ByTitleDescending)
		fun canGetArtist(): Boolean = typesSupported.contains(Type.ByArtistAscending)
				|| typesSupported.contains(Type.ByArtistDescending)
		fun canGetAlbumTitle(): Boolean = typesSupported.contains(Type.ByAlbumTitleAscending)
				|| typesSupported.contains(Type.ByAlbumTitleDescending)
		fun canGetAlbumArtist(): Boolean = typesSupported.contains(Type.ByAlbumArtistAscending)
				|| typesSupported.contains(Type.ByAlbumArtistDescending)
		fun canGetSize(): Boolean = typesSupported.contains(Type.BySizeAscending)
				|| typesSupported.contains(Type.BySizeDescending)
	}

	fun interface NaturalOrderHelper<T> {
		fun lookup(item: T): Int
	}

	enum class Type {
		ByTitleDescending, ByTitleAscending,
		ByArtistDescending, ByArtistAscending,
		ByAlbumTitleDescending, ByAlbumTitleAscending,
		ByAlbumArtistDescending, ByAlbumArtistAscending,
		BySizeDescending, BySizeAscending,
		NaturalOrder, None
	}

	fun getSupportedTypes(): Set<Type> {
		return sortingHelper.typesSupported.let {
			if (naturalOrderHelper != null)
				it + Type.NaturalOrder
			else it }
	}

	fun getComparator(type: Type): HintedComparator<T> {
		if (!getSupportedTypes().contains(type))
			throw IllegalArgumentException("Unsupported type ${type.name}")
		return WrappingHintedComparator(type, when (type) {
			Type.ByTitleDescending -> {
				SupportComparator.createAlphanumericComparator(true) {
					sortingHelper.getTitle(it) ?: ""
				}
			}
			Type.ByTitleAscending -> {
				SupportComparator.createAlphanumericComparator(false) {
					sortingHelper.getTitle(it) ?: ""
				}
			}
			Type.ByArtistDescending -> {
				SupportComparator.createAlphanumericComparator(true) {
					sortingHelper.getArtist(it) ?: ""
				}
			}
			Type.ByArtistAscending -> {
				SupportComparator.createAlphanumericComparator(false) {
					sortingHelper.getArtist(it) ?: ""
				}
			}
			Type.ByAlbumTitleDescending -> {
				SupportComparator.createAlphanumericComparator(true) {
					sortingHelper.getAlbumTitle(it) ?: ""
				}
			}
			Type.ByAlbumTitleAscending -> {
				SupportComparator.createAlphanumericComparator(false) {
					sortingHelper.getAlbumTitle(it) ?: ""
				}
			}
			Type.ByAlbumArtistDescending -> {
				SupportComparator.createAlphanumericComparator(true) {
					sortingHelper.getAlbumArtist(it) ?: ""
				}
			}
			Type.ByAlbumArtistAscending -> {
				SupportComparator.createAlphanumericComparator(false) {
					sortingHelper.getAlbumArtist(it) ?: ""
				}
			}
			Type.BySizeDescending -> {
				SupportComparator.createInversionComparator(
					compareBy { sortingHelper.getSize(it) }, true
				)
			}
			Type.BySizeAscending -> {
				SupportComparator.createInversionComparator(
					compareBy { sortingHelper.getSize(it) }, false
				)
			}
			Type.NaturalOrder -> {
				SupportComparator.createInversionComparator(
					compareBy { naturalOrderHelper!!.lookup(it) }, false
				)
			}
			Type.None -> SupportComparator.createDummyComparator()
		})
	}

	fun getFastScrollHintFor(item: T, sortType: Type): String? {
		return when (sortType) {
			Type.ByTitleDescending, Type.ByTitleAscending -> {
				(sortingHelper.getTitle(item) ?: "-").firstOrNull()?.toString()
			}
			Type.ByArtistDescending, Type.ByArtistAscending -> {
				(sortingHelper.getArtist(item) ?: "-").firstOrNull()?.toString()
			}
			Type.ByAlbumTitleDescending, Type.ByAlbumTitleAscending -> {
				(sortingHelper.getAlbumTitle(item) ?: "-").firstOrNull()?.toString()
			}
			Type.ByAlbumArtistDescending, Type.ByAlbumArtistAscending -> {
				(sortingHelper.getAlbumArtist(item) ?: "-").firstOrNull()?.toString()
			}
			Type.BySizeDescending, Type.BySizeAscending -> {
				sortingHelper.getSize(item).toString()
			}
			Type.NaturalOrder -> {
				naturalOrderHelper!!.lookup(item).toString()
			}
			Type.None -> null
		}?.ifEmpty { null }
	}

	abstract class HintedComparator<T>(val type: Type) : Comparator<T>
	private class WrappingHintedComparator<T>(type: Type, private val comparator: Comparator<T>)
		: HintedComparator<T>(type) {
		override fun compare(o1: T, o2: T): Int {
			return comparator.compare(o1, o2)
		}
	}
}
