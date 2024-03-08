/*
 * Copyright (C) 2009 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package gde.histo.innercache;

import static gde.histo.utils.Preconditions.checkState;

import java.io.Serializable;
import java.lang.ref.ReferenceQueue;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractQueue;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;

import com.sun.istack.Nullable;

import gde.histo.innercache.AbstractCache.SimpleStatsCounter;
import gde.histo.innercache.AbstractCache.StatsCounter;
import gde.histo.innercache.CacheBuilder.NullListener;
import gde.histo.innercache.CacheBuilder.OneWeigher;
import gde.histo.innercache.CacheLoader.InvalidCacheLoadException;
import gde.histo.innercache.CacheLoader.UnsupportedLoadingOperationException;
import gde.histo.utils.Stopwatch;
import gde.histo.utils.Stopwatch.Ticker;
import gde.log.Level;

/**
 * The non-thread-safe implementation built by {@link CacheBuilder}.
 *
 * <p>Is based on a concurrent hash map due to minor performance loss
 * compared with a hash map.
 * @see <a href="https://stackoverflow.com/questions/1378310/performance-concurrenthashmap-vs-hashmap">Performance ConcurrentHashMap vs HashMap</a>
 *
 * @author Charles Fry
 * @author Doug Lea ({@code ConcurrentHashMap})
 * @author Thomas Eickert (USER)
 */
class SynchronousCache<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V> {

	/*
	 * The basic strategy is to subdivide the table among Segments, each of which itself is a
	 * hash table. The map supports non-blocking reads and
	 * concurrent writes across different segments.
	 *
	 * A best-effort bounding is performed per segment, using a
	 * page-replacement algorithm to determine which entries to evict when the capacity has been
	 * exceeded.
	 *
	 * This implementation uses a per-segment queue to record a memento of the
	 * accesses that were performed on the map. The queue is drained on writes and when it exceeds
	 * its capacity threshold.
	 *
	 * The Least Recently Used page replacement algorithm was chosen due to its simplicity, high hit
	 * rate, and ability to be implemented with O(1) time complexity. The initial LRU implementation
	 * operates per-segment rather than globally for increased implementation simplicity. We expect
	 * the cache hit rate to be similar to that of a global LRU algorithm.
	 *
	 * <p>ET: <b>Changes to the Guava implementation:</b>
	 * <ul>
	 * <li>cache writes and removals are not tracked internally
	 * <li>{@code ImmutableMap} replaced by {@code CompletableFuture}.
	 * <li>{@code Uninterruptibles.getUninterruptibly} circumvented
	 * <li>replace all Guava Preconditions.checkNotNull calls by Objects.requireNonNull
	 * </ul>
	 */

	// Constants

	/**
	 * The maximum capacity, used if a higher value is implicitly specified by either of the
	 * constructors with arguments. MUST be a power of two {@code <= 1<<30} to ensure that entries are
	 * indexable using ints.
	 */
	static final int												MAXIMUM_CAPACITY				= 1 << 30;

	/** The maximum number of segments to allow; used to bound constructor arguments. */
	static final int												MINIMUM_CAPACITY				= 1 << 4;																							// slightly conservative

	// ET there is no need to have more than one segment.
	// ET However, we do not expect a significant performance loss or capacity loss due to the segment threshold
	/** The maximum number of segments to allow; used to bound constructor arguments. */
	static final int												MAX_SEGMENTS						= 1 << 16;																						// slightly conservative

	/** Number of (unsynchronized) retries in the containsValue method. */
	static final int												CONTAINS_VALUE_RETRIES	= 1;																									// ET one try only

	/**
	 * Number of cache access operations that can be buffered per segment before the cache's recency
	 * ordering information is updated. This is used to avoid lock contention by recording a memento
	 * of reads and delaying a lock acquisition until the threshold is crossed or a mutation occurs.
	 *
	 * <p>This must be a (2^n)-1 as it is used as a mask.
	 */
	static final int												DRAIN_THRESHOLD					= 0x3F;

	// ET no keyReferenceQueue, no valueReferenceQueue implemented

	// Fields

	static final Logger											logger									= Logger.getLogger(SynchronousCache.class.getName());

	/**
	 * The data segment.
	 * Replaces Guava's segments, each of which was a specialized hash table.
	 */
	final Segment<K, V>											segments;

	/** The concurrency level. */
	final int																concurrencyLevel;

	// ET no weak keys, no soft keys implemented - equivalence is based on standard 'equals' functions

	/** Strategy for referencing keys. */
	final Strength													keyStrength;

	/** Strategy for referencing values. */
	final Strength													valueStrength;

	/** The maximum weight of this map. UNSET_INT if there is no maximum. */
	final long															maxWeight;

	/** Weigher to weigh cache entries. */
	final Weigher<K, V>											weigher;

	// ET no expiration, no refresh implemented

	/** Entries waiting to be consumed by the removal listener. */
	final Queue<RemovalNotification<K, V>>	removalNotificationQueue;

	/**
	 * A listener that is invoked when an entry is removed due to expiration or garbage collection of
	 * soft/weak entries.
	 */
	final RemovalListener<K, V>							removalListener;

	/** Measures time in a testable way. */
	final Ticker														ticker;

	/** Factory used to create new entries. */
	final EntryFactory											entryFactory;

	/**
	 * Accumulates global cache statistics. Note that there are also per-segments stats counters which
	 * must be aggregated to obtain a global stats view.
	 */
	final StatsCounter											globalStatsCounter;

	/**
	 * The default cache loader to use on loading operations.
	 */
	@Nullable
	final CacheLoader<? super K, V>					defaultLoader;

	/**
	 * Creates a new, empty map with the specified strategy, initial capacity and concurrency level.
	 */
	SynchronousCache(CacheBuilder<? super K, ? super V> builder, @Nullable CacheLoader<? super K, V> loader) {
		concurrencyLevel = Math.min(builder.getConcurrencyLevel(), MAX_SEGMENTS);

		// ET no weak keys, no soft keys implemented - equivalence is based on standard 'equals' functions
		keyStrength = Strength.STRONG;
		valueStrength = Strength.STRONG;

		maxWeight = builder.getMaximumWeight();
		weigher = builder.getWeigher();

		removalListener = builder.getRemovalListener();
		removalNotificationQueue = (removalListener == NullListener.INSTANCE) ? SynchronousCache.<RemovalNotification<K, V>> discardingQueue() : new ConcurrentLinkedQueue<RemovalNotification<K, V>>();

		ticker = builder.getTicker(recordsTime());
		entryFactory = EntryFactory.getFactory(keyStrength, usesAccessEntries(), usesWriteEntries());
		globalStatsCounter = builder.getStatsCounterSupplier().get();
		defaultLoader = loader;

		int initialCapacity = Math.min(builder.getInitialCapacity(), MAXIMUM_CAPACITY);
		if (evictsBySize() && !customWeigher()) {
			initialCapacity = Math.min(initialCapacity, (int) maxWeight);
		}

		this.segments = createSegment(MINIMUM_CAPACITY, maxWeight, builder.getStatsCounterSupplier().get());
	}

	boolean evictsBySize() {
		return maxWeight >= 0;
	}

	boolean customWeigher() {
		return weigher != OneWeigher.INSTANCE;
	}

	boolean usesAccessQueue() {
		return evictsBySize();
	}

	boolean recordsTime() {
		return false; // ET recordsWrite() || recordsAccess();
	}

	boolean usesWriteEntries() {
		return false; // ET usesWriteQueue() || recordsWrite();
	}

	boolean usesAccessEntries() {
		return usesAccessQueue(); // ET || recordsAccess();
	}

	enum Strength {

		STRONG {
			@Override
			<K, V> ValueReference<K, V> referenceValue(Segment<K, V> segment, ReferenceEntry<K, V> entry, V value, int weight) {
				return (weight == 1) ? new StrongValueReference<K, V>(value) : new WeightedStrongValueReference<K, V>(value, weight);
			}
		};

		/**
		 * Creates a reference for the given value according to this value strength.
		 */
		abstract <K, V> ValueReference<K, V> referenceValue(Segment<K, V> segment, ReferenceEntry<K, V> entry, V value, int weight);
	}

	/**
	 * Creates new entries.
	 */
	enum EntryFactory {
		STRONG {
			@Override
			<K, V> ReferenceEntry<K, V> newEntry(Segment<K, V> segment, K key) {
				return new StrongEntry<K, V>(key);
			}
		},
		STRONG_ACCESS {
			@Override
			<K, V> ReferenceEntry<K, V> newEntry(Segment<K, V> segment, K key) {
				return new StrongAccessEntry<K, V>(key);
			}

			@Override
			<K, V> ReferenceEntry<K, V> copyEntry(Segment<K, V> segment, ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
				ReferenceEntry<K, V> newEntry = super.copyEntry(segment, original, newNext);
				copyAccessEntry(original, newEntry);
				return newEntry;
			}
		};

		/**
		 * Masks used to compute indices in the following table.
		 */
		static final int						ACCESS_MASK	= 1;
		static final int						WRITE_MASK	= 2;													// ET no expiration, no refresh implemented
		static final int						WEAK_MASK		= 4;											// ET no weak keys, no soft keys implemented - equivalence is based on standard 'equals' functions

		/**
		 * Look-up table for factories.
		 */
		static final EntryFactory[]	factories		= { STRONG, STRONG_ACCESS };

		static EntryFactory getFactory(@SuppressWarnings("unused") Strength keyStrength, boolean usesAccessQueue, boolean usesWriteQueue) {
			int flags = (false ? WEAK_MASK : 0) // ET no weak keys, no soft keys implemented - equivalence is based on standard 'equals' functions
					| (usesAccessQueue ? ACCESS_MASK : 0) | (usesWriteQueue ? WRITE_MASK : 0); // ET no expiration, no refresh implemented
			return factories[flags];
		}

		/**
		 * Creates a new entry.
		 *
		 * @param segment to create the entry for
		 * @param key of the entry
		 * @param hash of the key
		 * @param next entry in the same bucket
		 */
		abstract <K, V> ReferenceEntry<K, V> newEntry(Segment<K, V> segment, K key);

		/**
		 * Copies an entry, assigning it a new {@code next} entry.
		 *
		 * @param original the entry to copy
		 * @param newNext entry in the same bucket
		 */
		<K, V> ReferenceEntry<K, V> copyEntry(Segment<K, V> segment, ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
			return newEntry(segment, original.getKey());
		}

		<K, V> void copyAccessEntry(ReferenceEntry<K, V> original, ReferenceEntry<K, V> newEntry) {
			newEntry.setAccessTime(original.getAccessTime());

			connectAccessOrder(original.getPreviousInAccessQueue(), newEntry);
			connectAccessOrder(newEntry, original.getNextInAccessQueue());

			nullifyAccessOrder(original);
		}

		<K, V> void copyWriteEntry(ReferenceEntry<K, V> original, ReferenceEntry<K, V> newEntry) {
			newEntry.setWriteTime(original.getWriteTime());

			connectWriteOrder(original.getPreviousInWriteQueue(), newEntry);
			connectWriteOrder(newEntry, original.getNextInWriteQueue());

			nullifyWriteOrder(original);
		}
	}

	/**
	 * A reference to a value.
	 */
	interface ValueReference<K, V> {
		/**
		 * Returns the value. Does not block or throw exceptions.
		 */
		@Nullable
		V get();

		/**
		 * Waits for a value that may still be loading. Unlike get(), this method can block (in the case
		 * of FutureValueReference).
		 *
		 * @throws ExecutionException if the loading thread throws an exception
		 * @throws ExecutionError if the loading thread throws an error
		 */
		@Deprecated // ET this is not a thread-safe implementation
		V waitForValue() throws ExecutionException;

		/**
		 * Returns the weight of this entry. This is assumed to be static between calls to setValue.
		 */
		int getWeight();

		/**
		 * Returns the entry associated with this value reference, or {@code null} if this value
		 * reference is independent of any entry.
		 */
		@Nullable
		ReferenceEntry<K, V> getEntry();

		/**
		 * Creates a copy of this reference for the given entry.
		 *
		 * <p>{@code value} may be null only for a loading reference.
		 */
		@Deprecated // ET required only for WeakValueReferences and SoftValueReferences which are not used for this synchronous cache
		ValueReference<K, V> copyFor(ReferenceQueue<V> queue, @Nullable V value, ReferenceEntry<K, V> entry);

		/**
		 * Notify pending loads that a new value was set. This is only relevant to loading value
		 * references.
		 */
		@Deprecated // ET required only for asynchronous loading
		void notifyNewValue(@Nullable V newValue);

		/**
		 * Returns true if a new value is currently loading, regardless of whether or not there is an
		 * existing value. It is assumed that the return value of this method is constant for any given
		 * ValueReference instance.
		 */
		@Deprecated // ET required only for asynchronous loading
		boolean isLoading();

		/**
		 * Returns true if this reference contains an active value, meaning one that is still considered
		 * present in the cache. Active values consist of live values, which are returned by cache
		 * lookups, and dead values, which have been evicted but awaiting removal. Non-active values
		 * consist strictly of loading values, though during refresh a value may be both active and
		 * loading.
		 */
		@Deprecated // ET required only for WeakValueReferences and SoftValueReferences which are not used for this synchronous cache
		boolean isActive();
	}

	/**
	 * Placeholder. Indicates that the value hasn't been set yet.
	 */
	static final ValueReference<Object, Object> UNSET = new ValueReference<Object, Object>() {
		@Override
		public Object get() {
			return null;
		}

		@Override
		public int getWeight() {
			return 0;
		}

		@Override
		public ReferenceEntry<Object, Object> getEntry() {
			return null;
		}

		@Override
		@Deprecated // ET required only for WeakValueReferences and SoftValueReferences which are not uses for the synchronous cache
		public ValueReference<Object, Object> copyFor(ReferenceQueue<Object> queue, @Nullable Object value, ReferenceEntry<Object, Object> entry) {
			return this;
		}

		@Override
		@Deprecated // ET loading is done synchronously and isLoading is always false
		public boolean isLoading() {
			return false;
		}

		@Override
		@Deprecated // ET required only for WeakValueReferences and SoftValueReferences which are not uses for the synchronous cache
		public boolean isActive() {
			return false;
		}

		@Override
		@Deprecated // ET this is not a thread-safe implementation
		public Object waitForValue() {
			return null;
		}

		@Override
		@Deprecated // ET required only for asynchronous loading
		public void notifyNewValue(Object newValue) {
		}
	};

	/**
	 * Singleton placeholder that indicates a value is being loaded.
	 */
	@SuppressWarnings("unchecked") // impl never uses a parameter or returns any non-null value
	static <K, V> ValueReference<K, V> unset() {
		return (ValueReference<K, V>) UNSET;
	}

	/**
	 * An entry in a reference map.
	 *
	 * Entries in the map can be in the following states:
	 *
	 * Valid:
	 *
	 * - Live: valid key/value are set
	 *
	 * - Loading: loading is pending (ET asynchronous loading is not implemented)
	 *
	 * Invalid:
	 *
	 * - Expired: time expired (key/value may still be set) (ET expiring is not implemented)
	 *
	 * - Collected: key/value was partially collected, but not yet cleaned up (ET ??)
	 *
	 * - Unset: marked as unset, awaiting cleanup or reuse (ET ???)
	 */
	interface ReferenceEntry<K, V> {
		/**
		 * Returns the value reference from this entry.
		 */
		ValueReference<K, V> getValueReference();

		/**
		 * Sets the value reference for this entry.
		 */
		void setValueReference(ValueReference<K, V> valueReference);

		/**
		 * Returns the next entry in the chain.
		 */
		@Nullable
		ReferenceEntry<K, V> getNext();

		/**
		 * Returns the entry's hash.
		 */
		int getHash();

		/**
		 * Returns the key for this entry.
		 */
		@Nullable
		K getKey();

		/*
		 * Used by entries that use access order. Access entries are maintained in a doubly-linked list.
		 * New entries are added at the tail of the list at write time; stale entries are expired from
		 * the head of the list.
		 */

		/**
		 * Returns the time that this entry was last accessed, in ns.
		 */
		long getAccessTime();

		/**
		 * Sets the entry access time in ns.
		 */
		void setAccessTime(long time);

		/**
		 * Returns the next entry in the access queue.
		 */
		ReferenceEntry<K, V> getNextInAccessQueue();

		/**
		 * Sets the next entry in the access queue.
		 */
		void setNextInAccessQueue(ReferenceEntry<K, V> next);

		/**
		 * Returns the previous entry in the access queue.
		 */
		ReferenceEntry<K, V> getPreviousInAccessQueue();

		/**
		 * Sets the previous entry in the access queue.
		 */
		void setPreviousInAccessQueue(ReferenceEntry<K, V> previous);

		/*
		 * Implemented by entries that use write order. Write entries are maintained in a doubly-linked
		 * list. New entries are added at the tail of the list at write time and stale entries are
		 * expired from the head of the list.
		 */

		/**
		 * Returns the time that this entry was last written, in ns.
		 */
		long getWriteTime();

		/**
		 * Sets the entry write time in ns.
		 */
		void setWriteTime(long time);

		/**
		 * Returns the next entry in the write queue.
		 */
		ReferenceEntry<K, V> getNextInWriteQueue();

		/**
		 * Sets the next entry in the write queue.
		 */
		void setNextInWriteQueue(ReferenceEntry<K, V> next);

		/**
		 * Returns the previous entry in the write queue.
		 */
		ReferenceEntry<K, V> getPreviousInWriteQueue();

		/**
		 * Sets the previous entry in the write queue.
		 */
		void setPreviousInWriteQueue(ReferenceEntry<K, V> previous);
	}

	private enum NullEntry implements ReferenceEntry<Object, Object> {
		INSTANCE;

		@Override
		public ValueReference<Object, Object> getValueReference() {
			return null;
		}

		@Override
		public void setValueReference(ValueReference<Object, Object> valueReference) {
		}

		@Override
		public ReferenceEntry<Object, Object> getNext() {
			return null;
		}

		@Override
		public int getHash() {
			return 0;
		}

		@Override
		public Object getKey() {
			return null;
		}

		@Override
		public long getAccessTime() {
			return 0;
		}

		@Override
		public void setAccessTime(long time) {
		}

		@Override
		public ReferenceEntry<Object, Object> getNextInAccessQueue() {
			return this;
		}

		@Override
		public void setNextInAccessQueue(ReferenceEntry<Object, Object> next) {
		}

		@Override
		public ReferenceEntry<Object, Object> getPreviousInAccessQueue() {
			return this;
		}

		@Override
		public void setPreviousInAccessQueue(ReferenceEntry<Object, Object> previous) {
		}

		@Override
		public long getWriteTime() {
			return 0;
		}

		@Override
		public void setWriteTime(long time) {
		}

		@Override
		public ReferenceEntry<Object, Object> getNextInWriteQueue() {
			return this;
		}

		@Override
		public void setNextInWriteQueue(ReferenceEntry<Object, Object> next) {
		}

		@Override
		public ReferenceEntry<Object, Object> getPreviousInWriteQueue() {
			return this;
		}

		@Override
		public void setPreviousInWriteQueue(ReferenceEntry<Object, Object> previous) {
		}
	}

	abstract static class AbstractReferenceEntry<K, V> implements ReferenceEntry<K, V> {
		@Override
		public ValueReference<K, V> getValueReference() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setValueReference(ValueReference<K, V> valueReference) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ReferenceEntry<K, V> getNext() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int getHash() {
			throw new UnsupportedOperationException();
		}

		@Override
		public K getKey() {
			throw new UnsupportedOperationException();
		}

		@Override
		public long getAccessTime() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setAccessTime(long time) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ReferenceEntry<K, V> getNextInAccessQueue() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setNextInAccessQueue(ReferenceEntry<K, V> next) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ReferenceEntry<K, V> getPreviousInAccessQueue() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setPreviousInAccessQueue(ReferenceEntry<K, V> previous) {
			throw new UnsupportedOperationException();
		}

		@Override
		public long getWriteTime() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setWriteTime(long time) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ReferenceEntry<K, V> getNextInWriteQueue() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setNextInWriteQueue(ReferenceEntry<K, V> next) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ReferenceEntry<K, V> getPreviousInWriteQueue() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setPreviousInWriteQueue(ReferenceEntry<K, V> previous) {
			throw new UnsupportedOperationException();
		}
	}

	@SuppressWarnings("unchecked") // impl never uses a parameter or returns any non-null value
	static <K, V> ReferenceEntry<K, V> nullEntry() {
		return (ReferenceEntry<K, V>) NullEntry.INSTANCE;
	}

	static final Queue<? extends Object> DISCARDING_QUEUE = new AbstractQueue<Object>() {
		@Override
		public boolean offer(Object o) {
			return true;
		}

		@Override
		public Object peek() {
			return null;
		}

		@Override
		public Object poll() {
			return null;
		}

		@Override
		public int size() {
			return 0;
		}

		@Override
		public Iterator<Object> iterator() {
			return Collections.emptyIterator(); // ET ImmutableSet.of().iterator();
		}
	};

	/**
	 * Queue that discards all elements.
	 */
	@SuppressWarnings("unchecked") // impl never uses a parameter or returns any non-null value
	static <E> Queue<E> discardingQueue() {
		return (Queue<E>) DISCARDING_QUEUE;
	}

	/*
	 * Note: All of this duplicate code sucks, but it saves a lot of memory. If only Java had mixins!
	 * To maintain this code, make a change for the strong reference type. Then, cut and paste, and
	 * replace "Strong" with "Soft" or "Weak" within the pasted text. The primary difference is that
	 * strong entries store the key reference directly while soft and weak entries delegate to their
	 * respective superclasses.
	 */

	/**
	 * Used for strongly-referenced keys.
	 */
	static class StrongEntry<K, V> extends AbstractReferenceEntry<K, V> {
		final K key;

		StrongEntry(K key) {
			this.key = key;
		}

		@Override
		public K getKey() {
			return this.key;
		}

		// The code below is exactly the same for each entry type.

		volatile ValueReference<K, V> valueReference = unset();

		@Override
		public ValueReference<K, V> getValueReference() {
			return valueReference;
		}

		@Override
		public void setValueReference(ValueReference<K, V> valueReference) {
			this.valueReference = valueReference;
		}
	}

	static final class StrongAccessEntry<K, V> extends StrongEntry<K, V> {
		StrongAccessEntry(K key) {
			super(key);
		}

		// The code below is exactly the same for each access entry type.

		volatile long accessTime = Long.MAX_VALUE;

		@Override
		public long getAccessTime() {
			return accessTime;
		}

		@Override
		public void setAccessTime(long time) {
			this.accessTime = time;
		}

		// Guarded By Segment.this
		ReferenceEntry<K, V> nextAccess = nullEntry();

		@Override
		public ReferenceEntry<K, V> getNextInAccessQueue() {
			return nextAccess;
		}

		@Override
		public void setNextInAccessQueue(ReferenceEntry<K, V> next) {
			this.nextAccess = next;
		}

		// Guarded By Segment.this
		ReferenceEntry<K, V> previousAccess = nullEntry();

		@Override
		public ReferenceEntry<K, V> getPreviousInAccessQueue() {
			return previousAccess;
		}

		@Override
		public void setPreviousInAccessQueue(ReferenceEntry<K, V> previous) {
			this.previousAccess = previous;
		}
	}

	/**
	 * References a strong value.
	 */
	static class StrongValueReference<K, V> implements ValueReference<K, V> {
		final V referent;

		StrongValueReference(V referent) {
			this.referent = referent;
		}

		@Override
		public V get() {
			return referent;
		}

		@Override
		public int getWeight() {
			return 1;
		}

		@Override
		public ReferenceEntry<K, V> getEntry() {
			return null;
		}

		@Override
		@Deprecated // ET required only for WeakValueReferences and SoftValueReferences which are not uses for the synchronous cache
		public ValueReference<K, V> copyFor(ReferenceQueue<V> queue, V value, ReferenceEntry<K, V> entry) {
			return this;
		}

		@Override
		@Deprecated // ET loading is done synchronously and isLoading is always false
		public boolean isLoading() {
			return false;
		}

		@Override
		@Deprecated // ET required only for WeakValueReferences and SoftValueReferences which are not uses for the synchronous cache
		public boolean isActive() {
			return true;
		}

		@Override
		@Deprecated // ET this is not a thread-safe implementation
		public V waitForValue() {
			return get();
		}

		@Override
		@Deprecated // ET required only for asynchronous loading
		public void notifyNewValue(V newValue) {
		}
	}

	/**
	 * References a strong value.
	 */
	static final class WeightedStrongValueReference<K, V> extends StrongValueReference<K, V> {
		final int weight;

		WeightedStrongValueReference(V referent, int weight) {
			super(referent);
			this.weight = weight;
		}

		@Override
		public int getWeight() {
			return weight;
		}
	}

	/**
	 * This method is a convenience for testing. Code should call {@link Segment#newEntry} directly.
	 */
	ReferenceEntry<K, V> newEntry(K key, int hash, @Nullable ReferenceEntry<K, V> next) {
		try {
			return segments.newEntry(key);
		} finally {
		}
	}

	/**
	 * This method is a convenience for testing. Code should call {@link Segment#copyEntry} directly.
	 */
	ReferenceEntry<K, V> copyEntry(ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
		return segments.copyEntry(original, newNext);
	}

	Segment<K, V> createSegment(@Deprecated int initialCapacity, long maxSegmentWeight, StatsCounter statsCounter) {
		return new Segment<K, V>(this, initialCapacity, maxSegmentWeight, statsCounter);
	}

	/**
	 * Gets the value from an entry. Returns null if the entry is invalid, partially-collected,
	 * loading, or expired. Unlike {@link Segment#getLiveValue} this method does not attempt to
	 * cleanup stale entries. As such it should only be called outside of a segment context, such as
	 * during iteration.
	 */
	@Nullable
	V getLiveValue(ReferenceEntry<K, V> entry, long now) {
		if (entry.getKey() == null) {
			return null;
		}
		V value = entry.getValueReference().get();
		if (value == null) {
			return null;
		}
		return value;
	}

	// queues

	static <K, V> void connectAccessOrder(ReferenceEntry<K, V> previous, ReferenceEntry<K, V> next) {
		previous.setNextInAccessQueue(next);
		next.setPreviousInAccessQueue(previous);
	}

	static <K, V> void nullifyAccessOrder(ReferenceEntry<K, V> nulled) {
		ReferenceEntry<K, V> nullEntry = nullEntry();
		nulled.setNextInAccessQueue(nullEntry);
		nulled.setPreviousInAccessQueue(nullEntry);
	}

	static <K, V> void connectWriteOrder(ReferenceEntry<K, V> previous, ReferenceEntry<K, V> next) {
		previous.setNextInWriteQueue(next);
		next.setPreviousInWriteQueue(previous);
	}

	static <K, V> void nullifyWriteOrder(ReferenceEntry<K, V> nulled) {
		ReferenceEntry<K, V> nullEntry = nullEntry();
		nulled.setNextInWriteQueue(nullEntry);
		nulled.setPreviousInWriteQueue(nullEntry);
	}

	/**
	 * Notifies listeners that an entry has been automatically removed due to expiration, eviction, or
	 * eligibility for garbage collection. This should be called every time expireEntries or
	 * evictEntry is called (once the lock is released).
	 */
	void processPendingNotifications() {
		RemovalNotification<K, V> notification;
		while ((notification = removalNotificationQueue.poll()) != null) {
			try {
				removalListener.onRemoval(notification);
			} catch (Throwable e) {
				logger.log(Level.WARNING, "Exception thrown by removal listener", e);
			}
		}
	}

	@SuppressWarnings("unchecked")
	final Segment<K, V>[] newSegmentArray(int ssize) {
		return new Segment[ssize];
	}

	// Inner Classes

	/**
	 * Segments are specialized versions of hash tables.
	 */
	static class Segment<K, V> {

		/*
		 * A segment maintains a linked hashmap of reference entries.
		 * The hashmap implements the overall eviction strategy.
		 * A reference entry wraps both key and value and some additional values used for the eviction
		 * algorithms.
		 *
		 * In addition, a segment might maintain additional maps required for elaborated eviction policies.
		 */

		final SynchronousCache<K, V> map;

		/**
		 * The number of live elements in this segment's region.
		 */
		int count() {
			return table.size();
		}

		/**
		 * The weight of the live elements in this segment's region.
		 */
		long																		totalWeight;

		/**
		 * Number of updates that alter the size of the table. This is used during bulk-read methods to
		 * make sure they see a consistent snapshot: If modCounts change during a traversal of segments
		 * loading size or checking containsValue, then we might have an inconsistent view of state so
		 * (usually) must retry.
		 */
		int																			modCount;

		/**
		 * The table is expanded when its size exceeds this threshold. (The value of this field is
		 * always {@code (int) (capacity * 0.75)}.)
		 */
		int																			threshold;

		/**
		 * The per-segment table.
		 */
		LinkedHashMap<K, ReferenceEntry<K, V>>	table;

		/**
		 * The maximum weight of this segment. UNSET_INT if there is no maximum.
		 */
		final long															maxSegmentWeight;

		/**
		 * The recency queue is used to record which entries were accessed for updating the access
		 * list's ordering. It is drained as a batch operation when either the DRAIN_THRESHOLD is
		 * crossed or a write occurs on the segment.
		 */
		final Queue<ReferenceEntry<K, V>>				recencyQueue;

		/**
		 * A counter of the number of reads since the last write, used to drain queues on a small
		 * fraction of read operations.
		 */
		final AtomicInteger											readCount	= new AtomicInteger();

		/**
		 * A queue of elements currently in the map, ordered by access time. Elements are added to the
		 * tail of the queue on access (note that writes count as accesses).
		 */
		final Queue<ReferenceEntry<K, V>>				accessQueue;

		/** Accumulates cache statistics. */
		final StatsCounter											statsCounter;

		Segment(SynchronousCache<K, V> map, @Deprecated int initialCapacity, long maxSegmentWeight, StatsCounter statsCounter) {
			if (statsCounter == null) throw new IllegalArgumentException();
			this.map = map;
			this.maxSegmentWeight = maxSegmentWeight;
			this.statsCounter = Objects.requireNonNull(statsCounter);
			initTable(newEntryContainer(initialCapacity));

			recencyQueue = map.usesAccessQueue() ? new ConcurrentLinkedQueue<ReferenceEntry<K, V>>() : SynchronousCache.<ReferenceEntry<K, V>> discardingQueue();

			accessQueue = map.usesAccessQueue() ? new AccessQueue<K, V>() : SynchronousCache.<ReferenceEntry<K, V>> discardingQueue();
		}

		/**
		 * ET: replaces Guava's array
		 */
		private LinkedHashMap<K, ReferenceEntry<K, V>> newEntryContainer(@Deprecated int size) {
			return new LinkedHashMap<K, ReferenceEntry<K, V>>(16, 0.75f, true); // todo strategy
		}

		private void initTable(LinkedHashMap<K, ReferenceEntry<K, V>> newTable) {
			this.table = newTable;
		}

		ReferenceEntry<K, V> newEntry(K key) {
			return map.entryFactory.newEntry(this, Objects.requireNonNull(key));
		}

		/**
		 * Copies {@code original} into a new entry chained to {@code newNext}. Returns the new entry,
		 * or {@code null} if {@code original} was already garbage collected.
		 */
		ReferenceEntry<K, V> copyEntry(ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
			if (original.getKey() == null) {
				// key collected
				return null;
			}

			ValueReference<K, V> valueReference = original.getValueReference();
			V value = valueReference.get();
			if ((value == null) && valueReference.isActive()) {
				// value collected
				return null;
			}

			ReferenceEntry<K, V> newEntry = map.entryFactory.copyEntry(this, original, newNext);
			newEntry.setValueReference(valueReference);
			return newEntry;
		}

		/**
		 * Sets a new value of an entry. Adds newly created entries at the end of the access queue.
		 */
		private void setValue(ReferenceEntry<K, V> entry, K key, V value, long now) {
			ValueReference<K, V> previous = entry.getValueReference();
			int weight = map.weigher.weigh(key, value);
			checkState(weight >= 0, "Weights must be non-negative");

			ValueReference<K, V> valueReference = map.valueStrength.referenceValue(this, entry, value, weight);
			entry.setValueReference(valueReference);
			recordWrite(entry, weight, now);
			previous.notifyNewValue(value);
		}

		// loading

		V get(K key, CacheLoader<? super K, V> loader) throws ExecutionException {
			Objects.requireNonNull(key);
			Objects.requireNonNull(loader);
			try {
				ReferenceEntry<K, V> e = table.get(key);
				long now = map.ticker.read();
				if (e != null) {
					V value = e.getValueReference().get();
					if (value != null) {
						recordRead(e, now);
						statsCounter.recordHits(1);
						return value;
					}
				}
				// at this point the entry is null //ET either null or expired;
				return getOrLoad(key, loader);
			} catch (ExecutionException ee) {
				Throwable cause = ee.getCause();
				if (cause instanceof Error) {
					throw new ExecutionError((Error) cause);
				} else if (cause instanceof RuntimeException) {
					throw new UncheckedExecutionException(cause);
				}
				throw ee;
			} finally {
				postReadCleanup();
			}
		}

		/**
		 * Synchronous access.
		 * @since ET 0.1
		 */
		private V getOrLoad(K key, CacheLoader<? super K, V> loader) throws ExecutionException {
			ReferenceEntry<K, V> e;
			ValueReference<K, V> valueReference = null;
			AccessValueReference<K, V> accessValueReference = null;
			boolean createNewEntry = true;

			try {
				// re-read ticker once inside the lock
				long now = map.ticker.read();
				preWriteCleanup(now);

				e = table.get(key);
				if (e != null) {
					valueReference = e.getValueReference();
					V value = valueReference.get();
					if (value == null) {
						// ET was kept for future code comparisons with Guava
					} else {
						recordRead(e, now);
						statsCounter.recordHits(1);
						return value;
					}
				}
				if (createNewEntry) {
					accessValueReference = new AccessValueReference<K, V>();

					if (e == null) {
						e = newEntry(key);
						e.setValueReference(accessValueReference);
						table.put(key, e);
					} else {
						e.setValueReference(accessValueReference);
					}
				}
			} finally {
				// unlock();
				postWriteCleanup(); // ET strange name for a method which is called also in case we didn't write an entry
			}

			if (createNewEntry) {
				try {
					// Synchronizes on the entry to allow failing fast when a recursive load is
					// detected. This may be circumvented when an entry is copied, but will fail fast most
					// of the time.
					synchronized (e) {
						return loadSync(key, accessValueReference, loader);
					}
				} finally {
					statsCounter.recordMisses(1);
				}
			} else {
				throw new UnsupportedOperationException("ET2 null entry values do not exist due to synchronous loading");
			}
		}

		// loadSync may be called for any given AccessValueReference

		private V loadSync(K key, AccessValueReference<K, V> accessValueReference, CacheLoader<? super K, V> loader) throws ExecutionException {
			CompletableFuture<V> loadingFuture = accessValueReference.loadSynced(key, loader);
			return getAndRecordStats(key, accessValueReference, loadingFuture);
		}

		/**
		 * Waits uninterruptibly for {@code newValue} to be loaded, and then records loading stats.
		 */
		private V getAndRecordStats(K key, AccessValueReference<K, V> accessValueReference, CompletableFuture<V> newValue) throws ExecutionException {
			V value = null;
			try {
				value = newValue.get();
				if (value == null) {
					throw new InvalidCacheLoadException("CacheLoader returned null for key " + key + ".");
				}
				statsCounter.recordLoadSuccess(accessValueReference.elapsedNanos());
				storeAccessedValue(key, accessValueReference, value);
				return value;
			} catch (InterruptedException e) {
				e.printStackTrace();
				return null;
			} finally {
				if (value == null) {
					throw new UnsupportedOperationException("ET null entry values do not exist due to synchronous loading");
				}
			}
		}

		// recency queue, shared by expiration and eviction

		/**
		 * Records the relative order in which this read was performed by adding {@code entry} to the
		 * recency queue. At write-time, or when the queue is full past the threshold, the queue will be
		 * drained and the entries therein processed.
		 *
		 * <p>Note: locked reads should use {@link #recordLockedRead}.
		 */
		private void recordRead(ReferenceEntry<K, V> entry, long now) {
			recencyQueue.add(entry);
		}

		/**
		 * Updates eviction metadata that {@code entry} was just written. This currently amounts to
		 * adding {@code entry} to relevant eviction lists.
		 */
		private void recordWrite(ReferenceEntry<K, V> entry, int weight, long now) {
			totalWeight += weight;

			accessQueue.add(entry);
		}

		/**
		 * Drains the recency queue, updating eviction metadata that the entries therein were read in
		 * the specified relative order. This currently amounts to adding them to relevant eviction
		 * lists (accounting for the fact that they could have been removed from the map since being
		 * added to the recency queue).
		 */
		private void drainRecencyQueue() {
			ReferenceEntry<K, V> e;
			while ((e = recencyQueue.poll()) != null) {
				// An entry may be in the recency queue despite it being removed from
				// the map . This can occur when the entry was concurrently read while a
				// writer is removing it from the segment or after a clear has removed
				// all of the segment's entries.
				if (accessQueue.contains(e)) {
					accessQueue.add(e);
				}
			}
		}

		// eviction

		private void enqueueNotification(@Nullable K key, @Nullable V value, int weight, RemovalCause cause) {
			totalWeight -= weight;
			if (cause.wasEvicted()) {
				statsCounter.recordEviction();
			}
			if (map.removalNotificationQueue != DISCARDING_QUEUE) {
				RemovalNotification<K, V> notification = RemovalNotification.create(key, value, cause);
				map.removalNotificationQueue.offer(notification);
			}
		}

		/**
		 * Performs eviction if the segment is over capacity. Avoids flushing the entire cache if the
		 * newest entry exceeds the maximum weight all on its own.
		 *
		 * @param newest the most recently added entry
		 */
		private void evictEntries(ReferenceEntry<K, V> newest) {
			if (!map.evictsBySize()) {
				return;
			}

			drainRecencyQueue();

			// If the newest entry by itself is too heavy for the segment, don't bother evicting
			// anything else, just that
			if (newest.getValueReference().getWeight() > maxSegmentWeight) {
				if (!removeEntry(newest, RemovalCause.SIZE)) {
					throw new AssertionError();
				}
			}

			while (totalWeight > maxSegmentWeight) {
				ReferenceEntry<K, V> e = getNextEvictable();
				if (!removeEntry(e, RemovalCause.SIZE)) {
					throw new AssertionError();
				}
			}
		}

		// fry_TODO: instead implement this with an eviction head
		private ReferenceEntry<K, V> getNextEvictable() {
			for (ReferenceEntry<K, V> e : accessQueue) {
				int weight = e.getValueReference().getWeight();
				if (weight > 0) {
					return e;
				}
			}
			throw new AssertionError();
		}

		// Specialized implementations of map methods

		@Nullable
		ReferenceEntry<K, V> getEntry(Object key) {
			return table.get(key);
		}

		@Nullable
		private ReferenceEntry<K, V> getLiveEntry(Object key, long now) {
			ReferenceEntry<K, V> e = getEntry(key);
			if (e == null) {
				return null;
			}
			return e;
		}

		/**
		 * Gets the value from an entry. Returns null if the entry is invalid.
		 */
		V getLiveValue(ReferenceEntry<K, V> entry, long now) {
			V value = entry.getValueReference().get();
			return value;
		}

		@Nullable
		V get(Object key) {
			try {
				ReferenceEntry<K, V> e = table.get(key);
				long now = map.ticker.read();
				if (e != null) {
					V value = e.getValueReference().get();
					if (value != null) {
						recordRead(e, now);
						return value;
					}
				}
				return null;
			} finally {
				postReadCleanup();
			}
		}

		boolean containsKey(Object key) {
			try {
				return table.containsKey(key);
			} finally {
				postReadCleanup();
			}
		}

		/**
		 * This method is a convenience for testing.
		 */
		boolean containsValue(Object value) {
			try {
				if (count() != 0) {
					return table.containsValue(value);
				}

				return false;
			} finally {
				postReadCleanup();
			}
		}

		@Nullable
		V put(K key, V value, boolean onlyIfAbsent) {
			try { // ET was kept for future code comparisons with Guava
				long now = map.ticker.read();
				preWriteCleanup(now);

				ReferenceEntry<K, V> e = table.get(key);
				if (e != null) {
					ValueReference<K, V> valueReference = e.getValueReference();
					V entryValue = valueReference.get();
					if (onlyIfAbsent) {
						recordRead(e, now);
						return entryValue;
					} else {
						// clobber existing entry, count remains unchanged
						++modCount;
						enqueueNotification(key, entryValue, valueReference.getWeight(), RemovalCause.REPLACED);
						setValue(e, key, value, now);
						evictEntries(e);
						return entryValue;
					}
				}

				// Create a new entry.
				++modCount;
				ReferenceEntry<K, V> newEntry = newEntry(key);
				setValue(newEntry, key, value, now);
				table.put(key, newEntry);
				evictEntries(newEntry);
				return null;
			} finally {
				postWriteCleanup();
			}
		}

		boolean replace(K key, V oldValue, V newValue) {
			try { // ET was kept for future code comparisons with Guava
				long now = map.ticker.read();
				preWriteCleanup(now);

				ReferenceEntry<K, V> e = table.get(key);
				if (e != null) {
					ValueReference<K, V> valueReference = e.getValueReference();
					V entryValue = valueReference.get();
					if (oldValue.equals(entryValue)) {
						++modCount;
						enqueueNotification(key, entryValue, valueReference.getWeight(), RemovalCause.REPLACED);
						setValue(e, key, newValue, now);
						evictEntries(e);
						return true;
					} else {
						recordRead(e, now);
						return false;
					}
				}
				return false;
			} finally {
				postWriteCleanup();
			}
		}

		@Nullable
		V replace(K key, V newValue) {
			try { // ET was kept for future code comparisons with Guava
				long now = map.ticker.read();
				preWriteCleanup(now);

				ReferenceEntry<K, V> e = table.get(key);
				if (e != null) {
					ValueReference<K, V> valueReference = e.getValueReference();
					V entryValue = valueReference.get();
					++modCount;
					enqueueNotification(key, entryValue, valueReference.getWeight(), RemovalCause.REPLACED);
					setValue(e, key, newValue, now);
					evictEntries(e);
					return entryValue;
				}
				return null;
			} finally {
				postWriteCleanup();
			}
		}

		@Nullable
		V remove(Object key) {
			try { // ET was kept for future code comparisons with Guava
				long now = map.ticker.read();
				preWriteCleanup(now);

				ReferenceEntry<K, V> e = table.get(key);
				if (e != null) {
					RemovalCause cause = RemovalCause.EXPLICIT;
					V entryValue = e.getValueReference().get();
					++modCount;
					removeEntry(e, cause);
					return entryValue;
				}
				return null;
			} finally {
				postWriteCleanup();
			}
		}

		/**
		 * Supports synchronous access.
		 * @return
		 * @since ET 0.1
		 */
		private boolean storeAccessedValue(K key, AccessValueReference<K, V> oldValueReference, V newValue) {
			try { // ET was kept for future code comparisons with Guava
				long now = map.ticker.read();
				preWriteCleanup(now); // ET strange name for a method call when we don't know if we actually write an entry

				ReferenceEntry<K, V> e = table.get(key);
				if (e != null) {
					ValueReference<K, V> valueReference = e.getValueReference();
					V entryValue = valueReference.get();
					if (oldValueReference == valueReference || (entryValue == null && valueReference != UNSET)) {
						++modCount;
						setValue(e, key, newValue, now);
						evictEntries(e);
						return true;
					}
				}

				++modCount;
				ReferenceEntry<K, V> newEntry = newEntry(key);
				setValue(newEntry, key, newValue, now);
				table.put(key, newEntry);
				// this.count = newCount; // write-volatile
				evictEntries(newEntry);
				return true;
			} finally {
				postWriteCleanup(); // ET strange name for a method which is called also in case we didn't write an entry
			}
		}

		boolean remove(Object key, Object value) {
			try { // ET was kept for future code comparisons with Guava
				long now = map.ticker.read();
				preWriteCleanup(now);

				ReferenceEntry<K, V> e = table.get(key);
				if (e != null) {
					V entryValue = e.getValueReference().get();
					if (value.equals(entryValue)) {
						RemovalCause cause = RemovalCause.EXPLICIT;
						++modCount;
						removeEntry(e, cause);
						return true;
					}
				}

				return false;
			} finally {
				postWriteCleanup();
			}
		}

		void clear() {
			if (!table.isEmpty()) {
				try { // ET was kept for future code comparisons with Guava
					long now = map.ticker.read();
					preWriteCleanup(now);

					for (Iterator<Entry<K, ReferenceEntry<K, V>>> iterator = this.table.entrySet().iterator(); iterator.hasNext();) {
						Entry<K, ReferenceEntry<K, V>> entry = iterator.next();
						ReferenceEntry<K, V> e = entry.getValue();
						ValueReference<K, V> valueReference = e.getValueReference();
						V value = valueReference.get();
						iterator.remove();
						enqueueNotification(entry.getKey(), value, valueReference.getWeight(), RemovalCause.EXPLICIT);
					}

					accessQueue.clear();
					readCount.set(0);

					++modCount;
				} finally {
					// unlock();
					postWriteCleanup();
				}
			}
		}

		@Nullable
		private boolean removeEntry(ReferenceEntry<K, V> e, RemovalCause cause) {
			ValueReference<K, V> valueReference = e.getValueReference();
			V value = valueReference.get();
			enqueueNotification(e.getKey(), value, valueReference.getWeight(), cause);
			accessQueue.remove(e);

			return table.remove(e.getKey()) != null;
		}

		/**
		 * Performs routine cleanup following a read. Normally cleanup happens during writes. If cleanup
		 * is not observed after a sufficient number of reads, try cleaning up from the read thread.
		 */
		private void postReadCleanup() {
			if ((readCount.incrementAndGet() & DRAIN_THRESHOLD) == 0) {
				cleanUp();
			}
		}

		/**
		 * Performs routine cleanup prior to executing a write.
		 * ET deleted: This should be called every time a write
		 * thread acquires the segment lock, immediately after acquiring the lock.
		 *
		 * ET strange name for a method call when we don't know if we actually write an entry
		 *
		 * <p>Post-condition: drainRecencyQueue has been run and readCount is zero.
		 */
		private void preWriteCleanup(long now) {
			runLockedCleanup(now);
		}

		/**
		 * Performs routine cleanup following a write.
		 */
		private void postWriteCleanup() {
			runUnlockedCleanup();
		}

		void cleanUp() {
			long now = map.ticker.read();
			runLockedCleanup(now);
			runUnlockedCleanup();
		}

		private void runLockedCleanup(long now) {
			drainRecencyQueue(); // ET added
			readCount.set(0);
		}

		private void runUnlockedCleanup() {
			map.processPendingNotifications();
		}
	}

	/**
	 * A reference to a cache value via synchronous access.
	 * Was derived from the LoadingValueReference class.
	 * The access uses the callable function.
	 * @author Thomas Eickert (USER)
	 * @since ET 0.1
	 */
	static class AccessValueReference<K, V> implements ValueReference<K, V> {
		volatile ValueReference<K, V>	oldValue;

		final CompletableFuture<V>		futureValue	= new CompletableFuture<>();
		final Stopwatch								stopwatch		= Stopwatch.createUnstarted();

		public AccessValueReference() {
			this(null);
		}

		public AccessValueReference(ValueReference<K, V> oldValue) {
			this.oldValue = (oldValue == null) ? SynchronousCache.<K, V> unset() : oldValue;
		}

		@Override
		@Deprecated // ET required only for asynchronous loading
		public boolean isLoading() {
			return false;
		}

		@Override
		@Deprecated // ET required only for WeakValueReferences and SoftValueReferences which are not used for the synchronous cache
		public boolean isActive() {
			return false;
		}

		@Override
		public int getWeight() {
			return 1;
		}

		/**
		 * Sets the completed futureValue.
		 * @param newValue
		 * @return
		 */
		public boolean set(@Nullable V newValue) {
			return futureValue.complete(newValue);
		}

		public boolean setException(Throwable t) {
			return futureValue.completeExceptionally(t);
		}

		@Override
		@Deprecated // ET required only for asynchronous loading
		public void notifyNewValue(@Nullable V newValue) {
			if (newValue != null) {
				// The pending load was clobbered by a manual write.
				// Unblock all pending gets, and have them return the new value.
				set(newValue);
			} else {
				// The pending load was removed. Delay notifications until loading completes.
				oldValue = unset();
			}
		}

		/**
		 * @param key
		 * @param loader
		 * @return the loading result as a COMPLETED completable future or as exception.
		 */
		public CompletableFuture<V> loadSynced(K key, CacheLoader<? super K, V> loader) {
			try {
				stopwatch.start();
				// ET old values are not supported
				// ET V previousValue = oldValue.get();

				set(loader.load(key));
				return futureValue;
			} catch (Throwable t) {
				setException(t);
				return futureValue;
			}
		}

		@Deprecated // ET ???
		public V compute(K key, BiFunction<? super K, ? super V, ? extends V> function) {
			stopwatch.start();
			V previousValue;
			try {
				previousValue = oldValue.waitForValue();
			} catch (ExecutionException e) {
				previousValue = null;
			}
			V newValue = function.apply(key, previousValue);
			this.set(newValue);
			return newValue;
		}

		public long elapsedNanos() {
			return stopwatch.elapsed(java.util.concurrent.TimeUnit.NANOSECONDS);
		}

		@Override
		@Deprecated // ET this is a synchronous implementation
		public V waitForValue() throws ExecutionException {
			throw new UnsupportedOperationException("ET");
			// return getUninterruptibly(futureValue);
		}

		@Override
		public V get() {
			return oldValue.get();
		}

		public ValueReference<K, V> getOldValue() {
			return oldValue;
		}

		@Override
		public ReferenceEntry<K, V> getEntry() {
			return null;
		}

		@Override
		@Deprecated // ET required only for WeakValueReferences and SoftValueReferences which are not used for the synchronous cache
		public ValueReference<K, V> copyFor(ReferenceQueue<V> queue, @Nullable V value, ReferenceEntry<K, V> entry) {
			return this;
		}
	}

	// Queues

	/**
	 * A custom queue for managing access order. Note that this is tightly integrated with
	 * {@code ReferenceEntry}, upon which it relies to perform its linking.
	 *
	 * <p>Note that this entire implementation makes the assumption that all elements which are in the
	 * map are also in this queue, and that all elements not in the queue are not in the map.
	 *
	 * <p>The benefits of creating our own queue are that (1) we can replace elements in the middle of
	 * the queue as part of copyWriteEntry, and (2) the contains method is highly optimized for the
	 * current model.
	 */
	static final class AccessQueue<K, V> extends AbstractQueue<ReferenceEntry<K, V>> {

		final ReferenceEntry<K, V> head = new AbstractReferenceEntry<K, V>() {

			@Override
			public long getAccessTime() {
				return Long.MAX_VALUE;
			}

			@Override
			public void setAccessTime(long time) {
			}

			ReferenceEntry<K, V> nextAccess = this;

			@Override
			public ReferenceEntry<K, V> getNextInAccessQueue() {
				return nextAccess;
			}

			@Override
			public void setNextInAccessQueue(ReferenceEntry<K, V> next) {
				this.nextAccess = next;
			}

			ReferenceEntry<K, V> previousAccess = this;

			@Override
			public ReferenceEntry<K, V> getPreviousInAccessQueue() {
				return previousAccess;
			}

			@Override
			public void setPreviousInAccessQueue(ReferenceEntry<K, V> previous) {
				this.previousAccess = previous;
			}
		};

		// implements Queue

		@Override
		public boolean offer(ReferenceEntry<K, V> entry) {
			// unlink
			connectAccessOrder(entry.getPreviousInAccessQueue(), entry.getNextInAccessQueue());

			// add to tail
			connectAccessOrder(head.getPreviousInAccessQueue(), entry);
			connectAccessOrder(entry, head);

			return true;
		}

		@Override
		public ReferenceEntry<K, V> peek() {
			ReferenceEntry<K, V> next = head.getNextInAccessQueue();
			return (next == head) ? null : next;
		}

		@Override
		public ReferenceEntry<K, V> poll() {
			ReferenceEntry<K, V> next = head.getNextInAccessQueue();
			if (next == head) {
				return null;
			}

			remove(next);
			return next;
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean remove(Object o) {
			ReferenceEntry<K, V> e = (ReferenceEntry<K, V>) o;
			ReferenceEntry<K, V> previous = e.getPreviousInAccessQueue();
			ReferenceEntry<K, V> next = e.getNextInAccessQueue();
			connectAccessOrder(previous, next);
			nullifyAccessOrder(e);

			return next != NullEntry.INSTANCE;
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean contains(Object o) {
			ReferenceEntry<K, V> e = (ReferenceEntry<K, V>) o;
			return e.getNextInAccessQueue() != NullEntry.INSTANCE;
		}

		@Override
		public boolean isEmpty() {
			return head.getNextInAccessQueue() == head;
		}

		@Override
		public int size() {
			int size = 0;
			for (ReferenceEntry<K, V> e = head.getNextInAccessQueue(); e != head; e = e.getNextInAccessQueue()) {
				size++;
			}
			return size;
		}

		@Override
		public void clear() {
			ReferenceEntry<K, V> e = head.getNextInAccessQueue();
			while (e != head) {
				ReferenceEntry<K, V> next = e.getNextInAccessQueue();
				nullifyAccessOrder(e);
				e = next;
			}

			head.setNextInAccessQueue(head);
			head.setPreviousInAccessQueue(head);
		}

		@Override
		public Iterator<ReferenceEntry<K, V>> iterator() {
			return new AbstractSequentialIterator<ReferenceEntry<K, V>>(peek()) {
				@Override
				protected ReferenceEntry<K, V> computeNext(ReferenceEntry<K, V> previous) {
					ReferenceEntry<K, V> next = previous.getNextInAccessQueue();
					return (next == head) ? null : next;
				}
			};
		}
	}

	// Cache support

	public void cleanUp() {
		segments.cleanUp();
	}

	// Map methods

	@Override
	public boolean isEmpty() {
		return segments.count() == 0;
	}

	@Override
	public int size() {
		return segments.count();
	}

	@Override
	@Nullable
	public V get(@Nullable Object key) {
		if (key == null) {
			return null;
		}
		return segments.get(key);
	}

	@Nullable
	public V getIfPresent(Object key) {
		if (key == null) throw new IllegalArgumentException();
		V value = segments.get(key);
		if (value == null) {
			globalStatsCounter.recordMisses(1);
		} else {
			globalStatsCounter.recordHits(1);
		}
		return value;
	}

	// Only becomes available in Java 8 when it's on the interface.
	// @Override
	@Override
	@Nullable
	public V getOrDefault(@Nullable Object key, @Nullable V defaultValue) {
		V result = get(key);
		return (result != null) ? result : defaultValue;
	}

	V get(K key, CacheLoader<? super K, V> loader) throws ExecutionException {
		if (key == null) throw new IllegalArgumentException();
		return segments.get(key, loader);
	}

	Map<K, V> getAllPresent(Iterable<?> keys) {
		int hits = 0;
		int misses = 0;

		Map<K, V> result = new LinkedHashMap<>();
		for (Object key : keys) {
			V value = get(key);
			if (value == null) {
				misses++;
			} else {
				@SuppressWarnings("unchecked")
				K castKey = (K) key;
				result.put(castKey, value);
				hits++;
			}
		}
		globalStatsCounter.recordHits(hits);
		globalStatsCounter.recordMisses(misses);
		return result;
	}

	Map<K, V> getAll(Iterable<? extends K> keys) throws ExecutionException {
		int hits = 0;
		int misses = 0;

		Map<K, V> result = new LinkedHashMap<>();
		Set<K> keysToLoad = new LinkedHashSet<>();
		for (K key : keys) {
			V value = get(key);
			if (!result.containsKey(key)) {
				result.put(key, value);
				if (value == null) {
					misses++;
					keysToLoad.add(key);
				} else {
					hits++;
				}
			}
		}

		try {
			if (!keysToLoad.isEmpty()) {
				try {
					Map<K, V> newEntries = loadAll(keysToLoad, defaultLoader);
					for (K key : keysToLoad) {
						V value = newEntries.get(key);
						if (value == null) {
							throw new InvalidCacheLoadException("loadAll failed to return a value for " + key);
						}
						result.put(key, value);
					}
				} catch (UnsupportedLoadingOperationException e) {
					// loadAll not implemented, fallback to load
					for (K key : keysToLoad) {
						misses--; // get will count this miss
						result.put(key, get(key, defaultLoader));
					}
				}
			}
			return result;
		} finally {
			globalStatsCounter.recordHits(hits);
			globalStatsCounter.recordMisses(misses);
		}
	}

	/**
	 * Returns the result of calling {@link CacheLoader#loadAll}, or null if {@code loader} doesn't
	 * implement {@code loadAll}.
	 */
	@Nullable
	Map<K, V> loadAll(Set<? extends K> keys, CacheLoader<? super K, V> loader) throws ExecutionException {
		if (loader == null) throw new IllegalArgumentException();
		if (keys == null) throw new IllegalArgumentException("keys");
		Stopwatch stopwatch = Stopwatch.createStarted();
		Map<K, V> result;
		boolean success = false;
		try {
			@SuppressWarnings("unchecked") // safe since all keys extend K
			Map<K, V> map = (Map<K, V>) loader.loadAll(keys);
			result = map;
			success = true;
		} catch (UnsupportedLoadingOperationException e) {
			success = true;
			throw e;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ExecutionException(e);
		} catch (RuntimeException e) {
			throw new UncheckedExecutionException(e);
		} catch (Exception e) {
			throw new ExecutionException(e);
		} catch (Error e) {
			throw new ExecutionError(e);
		} finally {
			if (!success) {
				globalStatsCounter.recordLoadException(stopwatch.elapsed(java.util.concurrent.TimeUnit.NANOSECONDS));
			}
		}

		if (result == null) {
			globalStatsCounter.recordLoadException(stopwatch.elapsed(java.util.concurrent.TimeUnit.NANOSECONDS));
			throw new InvalidCacheLoadException(loader + " returned null map from loadAll");
		}

		stopwatch.stop();
		boolean nullsPresent = false;
		for (Map.Entry<K, V> entry : result.entrySet()) {
			K key = entry.getKey();
			V value = entry.getValue();
			if (key == null || value == null) {
				// delay failure until non-null entries are stored
				nullsPresent = true;
			} else {
				put(key, value);
			}
		}

		if (nullsPresent) {
			globalStatsCounter.recordLoadException(stopwatch.elapsed(java.util.concurrent.TimeUnit.NANOSECONDS));
			throw new InvalidCacheLoadException(loader + " returned null keys or values from loadAll");
		}

		// fry_TODO: record count of loaded entries
		globalStatsCounter.recordLoadSuccess(stopwatch.elapsed(java.util.concurrent.TimeUnit.NANOSECONDS));
		return result;
	}

	/**
	 * Returns the internal entry for the specified key. The entry may be loading, expired, or
	 * partially collected.
	 */
	ReferenceEntry<K, V> getEntry(@Nullable Object key) {
		// does not impact recency ordering
		if (key == null) {
			return null;
		}
		return segments.getEntry(key);
	}

	@Override
	public boolean containsKey(@Nullable Object key) {
		// does not impact recency ordering
		if (key == null) {
			return false;
		}
		return segments.containsKey(key);
	}

	@Override
	public boolean containsValue(@Nullable Object value) {
		return segments.containsValue(value);
	}

	@Override
	public V put(K key, V value) {
		if (key == null) throw new IllegalArgumentException("key must not be null");
		if (value == null) throw new IllegalArgumentException("value must not be null");
		return segments.put(key, value, false);
	}

	@Override
	public V putIfAbsent(K key, V value) {
		if (key == null) throw new IllegalArgumentException("key must not be null");
		if (value == null) throw new IllegalArgumentException("value must not be null");
		return segments.put(key, value, true);
	}

	@Override
	public V compute(K key, BiFunction<? super K, ? super V, ? extends V> function) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V computeIfAbsent(K key, Function<? super K, ? extends V> function) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> function) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V merge(K key, V newValue, BiFunction<? super V, ? super V, ? extends V> function) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		for (Entry<? extends K, ? extends V> e : m.entrySet()) {
			put(e.getKey(), e.getValue());
		}
	}

	@Override
	public V remove(@Nullable Object key) {
		if (key == null) {
			return null;
		}
		return segments.remove(key);
	}

	@Override
	public boolean remove(@Nullable Object key, @Nullable Object value) {
		if (key == null || value == null) {
			return false;
		}
		return segments.remove(key, value);
	}

	@Override
	public boolean replace(K key, @Nullable V oldValue, V newValue) {
		if (key == null) throw new IllegalArgumentException("key must not be null");
		if (newValue == null) throw new IllegalArgumentException("value must not be null");
		if (oldValue == null) {
			return false;
		}
		return segments.replace(key, oldValue, newValue);
	}

	@Override
	public V replace(K key, V newValue) {
		if (key == null) throw new IllegalArgumentException("key must not be null");
		if (newValue == null) throw new IllegalArgumentException("value must not be null");
		return segments.replace(key, newValue);
	}

	@Override
	public void clear() {
		segments.clear();
	}

	void invalidateAll(Iterable<?> keys) {
		for (Object key : keys) {
			remove(key);
		}
	}

	Set<K> keySet;

	@Override
	public Set<K> keySet() {
		// does not impact recency ordering
		Set<K> ks = keySet;
		return (ks != null) ? ks : (keySet = new KeySet(this));
	}

	Collection<V> values;

	@Override
	public Collection<V> values() {
		// does not impact recency ordering
		Collection<V> vs = values;
		return (vs != null) ? vs : (values = new Values(this));
	}

	Set<Entry<K, V>> entrySet;

	@Override
	public Set<Entry<K, V>> entrySet() {
		// does not impact recency ordering
		Set<Entry<K, V>> es = entrySet;
		return (es != null) ? es : (entrySet = new EntrySet(this));
	}

	// Iterator Support

	/**
	 * This class provides a skeletal implementation of the {@code Iterator}
	 * interface for sequences whose next element can always be derived from the
	 * previous element. Null elements are not supported, nor is the
	 * {@link #remove()} method.
	 *
	 * <p>ET: <b>Changes to the Guava implementation:</b>
	 * <ul>
	 * <li>is now independent from Guava
	 * <li>has been moved from an abstract utils class to an abstract inner class
	 * </ul>
	 *
	 * <p>Example: <pre>   {@code
	 *
	 *   Iterator<Integer> powersOfTwo =
	 *       new AbstractSequentialIterator<Integer>(1) {
	 * 		protected Integer computeNext(Integer previous) {
	 * 			return (previous == 1 << 30) ? null : previous * 2;
	 * 		}
	 *       };}</pre>
	 *
	 * @author Chris Povirk
	 * @author Thomas Eickert (USER)
	 */
	abstract static class AbstractSequentialIterator<T> implements Iterator<T> {
		private T nextOrNull;

		/**
		 * Creates a new iterator with the given first element, or, if {@code
		 * firstOrNull} is null, creates a new empty iterator.
		 */
		protected AbstractSequentialIterator(@Nullable T firstOrNull) {
			this.nextOrNull = firstOrNull;
		}

		/**
		 * Returns the element that follows {@code previous}, or returns {@code null}
		 * if no elements remain. This method is invoked during each call to
		 * {@link #next()} in order to compute the result of a <i>future</i> call to
		 * {@code next()}.
		 */
		protected abstract T computeNext(T previous);

		@Override
		public final boolean hasNext() {
			return nextOrNull != null;
		}

		@Override
		public final T next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			try {
				return nextOrNull;
			} finally {
				nextOrNull = computeNext(nextOrNull);
			}
		}
	}

	abstract class HashIterator<T> implements Iterator<T> {

		Segment<K, V>															currentSegment;
		LinkedHashMap<K, ReferenceEntry<K, V>>		currentTable;
		Iterator<Entry<K, ReferenceEntry<K, V>>>	iterator;

		HashIterator() {
			currentSegment = segments;
			currentTable = currentSegment.table;
			iterator = currentTable.entrySet().iterator();
		}

		@Override
		public abstract T next();

		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		WriteThroughEntry nextEntry() {
			Entry<K, ReferenceEntry<K, V>> next = iterator.next();
			currentSegment.postReadCleanup();
			return new WriteThroughEntry(next.getKey(), next.getValue().getValueReference().get());
		}

		@Override
		public void remove() {
			iterator.remove();
		}
	}

	final class KeyIterator extends HashIterator<K> {

		@Override
		public K next() {
			return nextEntry().getKey();
		}
	}

	final class ValueIterator extends HashIterator<V> {

		@Override
		public V next() {
			return nextEntry().getValue();
		}
	}

	/**
	 * Custom Entry class used by EntryIterator.next(), that relays setValue changes to the underlying
	 * map.
	 */
	final class WriteThroughEntry implements Entry<K, V> {
		final K	key;		// non-null
		V				value;	// non-null

		WriteThroughEntry(K key, V value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public K getKey() {
			return key;
		}

		@Override
		public V getValue() {
			return value;
		}

		@Override
		public boolean equals(@Nullable Object object) {
			// Cannot use key and value equivalence
			if (object instanceof Entry) {
				Entry<?, ?> that = (Entry<?, ?>) object;
				return key.equals(that.getKey()) && value.equals(that.getValue());
			}
			return false;
		}

		@Override
		public int hashCode() {
			// Cannot use key and value equivalence
			return key.hashCode() ^ value.hashCode();
		}

		@Override
		public V setValue(V newValue) {
			V oldValue = put(key, newValue);
			value = newValue; // only if put succeeds
			return oldValue;
		}

		@Override
		public String toString() {
			return getKey() + "=" + getValue();
		}
	}

	final class EntryIterator extends HashIterator<Entry<K, V>> {

		@Override
		public Entry<K, V> next() {
			return nextEntry();
		}
	}

	abstract class AbstractCacheSet<T> extends AbstractSet<T> {
		final Map<?, ?> map;

		AbstractCacheSet(Map<?, ?> map) {
			this.map = map;
		}

		@Override
		public int size() {
			return map.size();
		}

		@Override
		public boolean isEmpty() {
			return map.isEmpty();
		}

		@Override
		public void clear() {
			map.clear();
		}

		// super.toArray() may misbehave if size() is inaccurate, at least on old versions of Android.
		// https://code.google.com/p/android/issues/detail?id=36519 / https://r.android.com/47508

		@Override
		public Object[] toArray() {
			return toArrayList(this).toArray();
		}

		@Override
		public <E> E[] toArray(E[] a) {
			return toArrayList(this).toArray(a);
		}
	}

	private static <E> ArrayList<E> toArrayList(Collection<E> c) {
		// Avoid calling ArrayList(Collection), which may call back into toArray.
		ArrayList<E> result = new ArrayList<E>(c.size());
		for (E e : c) {
			result.add(e);
		}
		return result;
	}

	boolean removeIf(BiPredicate<? super K, ? super V> filter) {
		if (filter == null) throw new IllegalArgumentException();
		boolean changed = false;
		for (K key : keySet()) {
			while (true) {
				V value = get(key);
				if (value == null || !filter.test(key, value)) {
					break;
				} else if (SynchronousCache.this.remove(key, value)) {
					changed = true;
					break;
				}
			}
		}
		return changed;
	}

	final class KeySet extends AbstractCacheSet<K> {

		KeySet(Map<?, ?> map) {
			super(map);
		}

		@Override
		public Iterator<K> iterator() {
			return new KeyIterator();
		}

		@Override
		public boolean contains(Object o) {
			return map.containsKey(o);
		}

		@Override
		public boolean remove(Object o) {
			return map.remove(o) != null;
		}
	}

	final class Values extends AbstractCollection<V> {
		private final ConcurrentMap<?, ?> map;

		Values(ConcurrentMap<?, ?> map) {
			this.map = map;
		}

		@Override
		public int size() {
			return map.size();
		}

		@Override
		public boolean isEmpty() {
			return map.isEmpty();
		}

		@Override
		public void clear() {
			map.clear();
		}

		@Override
		public Iterator<V> iterator() {
			return new ValueIterator();
		}

		@Override
		public boolean removeIf(Predicate<? super V> filter) {
			if (filter == null) throw new IllegalArgumentException();
			return SynchronousCache.this.removeIf((k, v) -> filter.test(v));
		}

		@Override
		public boolean contains(Object o) {
			return map.containsValue(o);
		}

		// super.toArray() may misbehave if size() is inaccurate, at least on old versions of Android.
		// https://code.google.com/p/android/issues/detail?id=36519 / https://r.android.com/47508

		@Override
		public Object[] toArray() {
			return toArrayList(this).toArray();
		}

		@Override
		public <E> E[] toArray(E[] a) {
			return toArrayList(this).toArray(a);
		}
	}

	final class EntrySet extends AbstractCacheSet<Entry<K, V>> {

		EntrySet(Map<?, ?> map) {
			super(map);
		}

		@Override
		public Iterator<Entry<K, V>> iterator() {
			return new EntryIterator();
		}

		@Override
		public boolean removeIf(Predicate<? super Entry<K, V>> filter) {
			if (filter == null) throw new IllegalArgumentException();
			return SynchronousCache.this.removeIf((k, v) -> filter.test(new AbstractMap.SimpleEntry<K, V>(k, v))); // ET Maps.immutableEntry(k, v)));
		}

		@Override
		public boolean contains(Object o) {
			if (!(o instanceof Entry)) {
				return false;
			}
			Entry<?, ?> e = (Entry<?, ?>) o;
			Object key = e.getKey();
			if (key == null) {
				return false;
			}
			V v = SynchronousCache.this.get(key);

			return v != null; // ET && valueEquivalence.equivalent(e.getValue(), v);
		}

		@Override
		public boolean remove(Object o) {
			if (!(o instanceof Entry)) {
				return false;
			}
			Entry<?, ?> e = (Entry<?, ?>) o;
			Object key = e.getKey();
			return key != null && SynchronousCache.this.remove(key, e.getValue());
		}
	}

	@SuppressWarnings("serial")
	static class SynchronousManualCache<K, V> implements Cache<K, V>, Serializable {
		final SynchronousCache<K, V> localCache;

		SynchronousManualCache(CacheBuilder<? super K, ? super V> builder) {
			this(new SynchronousCache<K, V>(builder, null));
		}

		private SynchronousManualCache(SynchronousCache<K, V> localCache) {
			this.localCache = localCache;
		}

		// Cache methods

		@Override
		@Nullable
		public V getIfPresent(Object key) {
			return localCache.getIfPresent(key);
		}

		@Override
		public V get(K key, final Callable<? extends V> valueLoader) throws ExecutionException {
			if (valueLoader == null) throw new IllegalArgumentException();
			return localCache.get(key, new CacheLoader<Object, V>() {
				@Override
				public V load(Object key) throws Exception {
					return valueLoader.call();
				}
			});
		}

		@Override
		public Map<K, V> getAllPresent(Iterable<?> keys) {
			return localCache.getAllPresent(keys);
		}

		@Override
		public void put(K key, V value) {
			localCache.put(key, value);
		}

		@Override
		public void putAll(Map<? extends K, ? extends V> m) {
			localCache.putAll(m);
		}

		@Override
		public void invalidate(Object key) {
			if (key == null) throw new IllegalArgumentException();
			localCache.remove(key);
		}

		@Override
		public void invalidateAll(Iterable<?> keys) {
			localCache.invalidateAll(keys);
		}

		@Override
		public void invalidateAll() {
			localCache.clear();
		}

		@Override
		public long size() {
			return localCache.size();
		}

		@Override
		public ConcurrentMap<K, V> asMap() {
			return localCache;
		}

		@Override
		public CacheStats stats() {
			SimpleStatsCounter aggregator = new SimpleStatsCounter();
			aggregator.incrementBy(localCache.globalStatsCounter);
			aggregator.incrementBy(localCache.segments.statsCounter);
			return aggregator.snapshot();
		}

		@Override
		public void cleanUp() {
			localCache.cleanUp();
		}
	}

}
