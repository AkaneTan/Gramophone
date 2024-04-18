package org.akanework.gramophone.logic.utils

import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.ShuffleOrder
import androidx.media3.session.MediaController
import kotlin.random.Random

/**
 * This shuffle order will take "firstIndex" as first song and play all songs after it.
 */
@UnstableApi
class CircularShuffleOrder private constructor(
	private val listener: Listener,
	private val shuffled: IntArray,
	private val random: Random
) : ShuffleOrder {
	private val indexInShuffled = IntArray(shuffled.size)

	init {
		for (i in shuffled.indices) {
			indexInShuffled[shuffled[i]] = i
		}
	}

	companion object {
		private const val TAG = "GramophoneShuffleOrder"
		private fun calculateShuffledList(offset: Int, length: Int, random: Random): IntArray {
			val shuffled = IntArray(length)
			var swapIndex: Int
			for (i in shuffled.indices) {
				swapIndex = random.nextInt(i + 1)
				shuffled[i] = shuffled[swapIndex]
				shuffled[swapIndex] = offset + i
			}
			return shuffled
		}

		private fun calculateListWithFirstIndex(shuffled: IntArray, firstIndex: Int): IntArray {
			if (shuffled.isEmpty() && firstIndex == 0) return shuffled
			if (shuffled.size <= firstIndex) throw IllegalArgumentException("${shuffled.size} <= $firstIndex")
			val fi = shuffled.indexOf(firstIndex)
			val before = shuffled.slice(0..<fi)
			val inclAndAfter = shuffled.slice(fi..<shuffled.size)
			return (inclAndAfter + before).toIntArray()
		}
	}

	constructor(listener: Listener, firstIndex: Int, length: Int, randomSeed: Long) :
			this(listener, firstIndex, length, Random(randomSeed))

	private constructor(listener: Listener, firstIndex: Int, length: Int, random: Random) :
			this(listener, calculateListWithFirstIndex(calculateShuffledList(0, length, random), firstIndex), random)

	constructor(listener: Listener, shuffledIndices: IntArray, randomSeed: Long) :
			this(listener, shuffledIndices.copyOf(), Random(randomSeed))

	override fun getLength(): Int {
		return shuffled.size
	}

	override fun getNextIndex(index: Int): Int {
		val shuffledIndex = indexInShuffled[index] + 1
		return if (shuffledIndex < shuffled.size) shuffled[shuffledIndex] else C.INDEX_UNSET
	}

	override fun getPreviousIndex(index: Int): Int {
		val shuffledIndex = indexInShuffled[index] - 1
		return if (shuffledIndex >= 0) shuffled[shuffledIndex] else C.INDEX_UNSET
	}

	override fun getLastIndex(): Int {
		return if (shuffled.isNotEmpty()) shuffled[shuffled.size - 1] else C.INDEX_UNSET
	}

	override fun getFirstIndex(): Int {
		return if (shuffled.isNotEmpty()) shuffled[0] else C.INDEX_UNSET
	}

	// This shuffles the inserted items among themselves and then adds them after
	// the previous index into shuffled - so if song A is playing and we add three songs B, C and D,
	// B,C,D will be shuffled among themselves to ie D,B,C and then this list will be inserted after
	// A so that song list will now be A,D,B,C,...
	override fun cloneAndInsert(insertionIndex: Int, insertionCount: Int): ShuffleOrder {
		if (shuffled.isEmpty()) {
			listener.onLazilySetShuffleOrder {
				CircularShuffleOrder(listener, it, insertionCount, random.nextLong()) }
			return CircularShuffleOrder(listener, 0, insertionCount, random.nextLong())
				.also { listener.onPersistableDataUpdated(Persistent(it)) }
		}
		// the original list: [0, 1, 2] shuffled: [2, 0, 1] indexInShuffled: [1, 2, 0]
		// insertionIndex for adding after 1 would be 2, 2 is at index 0 in shuffled list, after 0
		// would be 1 so we want to insert into shuffled at index 1 here.
		// If insertionIndex is 0, just add it to the very beginning.
		val ii = if (insertionIndex > 0) indexInShuffled[insertionIndex - 1] + 1 else 0
		val insertionPoints = (ii..<(ii+insertionCount)).toList().toIntArray()
		val insertionValues = calculateShuffledList(insertionIndex, insertionCount, random)
		val newShuffled = IntArray(shuffled.size + insertionCount)
		var indexInInsertionList = 0
		var indexInOldShuffled = 0

		for (i in 0 until shuffled.size + insertionCount) {
			if (indexInInsertionList < insertionCount && indexInOldShuffled == insertionPoints[indexInInsertionList]) {
				newShuffled[i] = insertionValues[indexInInsertionList++]
			} else {
				newShuffled[i] = shuffled[indexInOldShuffled++]
				if (newShuffled[i] >= insertionIndex) {
					newShuffled[i] += insertionCount
				}
			}
		}

		return CircularShuffleOrder(listener, newShuffled, Random(random.nextLong()))
			.also { listener.onPersistableDataUpdated(Persistent(it)) }
	}

	override fun cloneAndRemove(indexFrom: Int, indexToExclusive: Int): ShuffleOrder {
		val numberOfElementsToRemove = indexToExclusive - indexFrom
		val newShuffled = IntArray(shuffled.size - numberOfElementsToRemove)
		var foundElementsCount = 0

		for (i in shuffled.indices) {
			if (shuffled[i] in indexFrom..<indexToExclusive) {
				++foundElementsCount
			} else {
				newShuffled[i - foundElementsCount] =
					if (shuffled[i] >= indexFrom) shuffled[i] - numberOfElementsToRemove else shuffled[i]
			}
		}

		return CircularShuffleOrder(listener, newShuffled, Random(random.nextLong()))
			.also { listener.onPersistableDataUpdated(Persistent(it)) }
	}

	override fun cloneAndClear(): ShuffleOrder {
		return CircularShuffleOrder(listener, 0, 0, Random(random.nextLong()))
			.also { listener.onPersistableDataUpdated(Persistent(it)) }
	}

	interface Listener {
		fun onPersistableDataUpdated(order: Persistent)
		fun onLazilySetShuffleOrder(factory: (Int) -> CircularShuffleOrder)
	}

	//TODO play next persistent doesn't seem to work either
	class Persistent private constructor(private val seed: Long, private val data: IntArray?) {
		constructor(order: CircularShuffleOrder) : this(order.random.nextLong(), order.shuffled)
		companion object {
			fun deserialize(data: String?): Persistent {
				if (data == null || data.length < 2) return Persistent(Random.nextLong(), null)
				val split = data.split(';')
				return Persistent(split[0].toLong(), if (split.size > 1) split[1]
					.split(',').map(String::toInt).toIntArray() else null)
			}
		}

		override fun toString(): String {
			return if (data != null) "$seed;${data.joinToString(",")}" else seed.toString()
		}

		fun toFactory(listener: Listener, controller: MediaController): (Int) -> CircularShuffleOrder {
			if (data == null) {
				return { CircularShuffleOrder(listener, it, controller.mediaItemCount, seed) }
			} else {
				return {
					if (controller.mediaItemCount != data.size) {
						throw IllegalStateException(
							"CircularShuffleOrder.Persistable: " +
									"${controller.mediaItemCount} != ${data.size}"
						).also {
							Log.e(TAG, Log.getStackTraceString(it))
						}
					}
					CircularShuffleOrder(listener, data, seed)
				}
			}
		}
	}

}