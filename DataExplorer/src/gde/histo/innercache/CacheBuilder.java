/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <https://www.gnu.org/licenses/>.

    Copyright (c) 2017,2018,2019 Thomas Eickert
****************************************************************************************/
/*
 * Copyright (C) 2009 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package gde.histo.innercache;

import static gde.histo.utils.Preconditions.checkArgument;
import static gde.histo.utils.Preconditions.checkState;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Logger;

import gde.histo.innercache.AbstractCache.StatsCounter;
import gde.histo.utils.Stopwatch.Ticker;
import gde.log.Level;

/**
 * <p>A builder of {@link LoadingCache} and {@link Cache} instances having any combination of the
 * following features:
 *
 * <ul>
 * <li>automatic loading of entries into the cache
 * <li>least-recently-used eviction when a maximum size is exceeded
 * <li>accumulation of cache access statistics
 * </ul>
 *
 * <p>These features are all optional; caches can be created using all or none of them. By default
 * cache instances created by {@code CacheBuilder} will not perform any type of eviction.
 *
 * <p>Changes to the Guava implementation:
 * <ul>
 * <li>This implementations is NOT thread-safe.
 * Please use Guava for this requirement.
 * <li>Is based on Guava, but uses specific functions to a minimum extent.
 * <li>NO:  time-based expiration of entries
 * <li>NO:  keys automatically wrapped in weak references
 * <li>NO:  values automatically wrapped in weak or soft references
 * <li>replace all Guava Preconditions.checkNotNull calls by Objects.requireNonNull
 * <li>Various google.common classes replaced by:
 * gde.histo.utils.Preconditions,
 * gde.histo.utils.Ticker,
 * java.util.function.Supplier
 * <li>{code @CheckReturnValue} deleted
 * </ul>
 *
 *
 * <p>Usage example: <pre>   {@code
 *
 *   LoadingCache<Key, Graph> graphs = CacheBuilder.newBuilder()
 *       .maximumSize(10000)
 *       .removalListener(MY_LISTENER)
 *       .build(
 *           new CacheLoader<Key, Graph>() {
 *             public Graph load(Key key) throws AnyException {
 *               return createExpensiveGraph(key);
 *             }
 *           });}</pre>
 *
 *
 * <p>The returned cache is implemented as a hash table with similar performance characteristics to
 * {@link ConcurrentHashMap}. It implements all optional operations of the {@link LoadingCache} and
 * {@link Cache} interfaces.
 *
 * <p>The returned cache uses equality comparisons (the
 * {@link Object#equals equals} method) to determine equality for keys or values.
 *
 * @param <K> the base key type for all caches created by this builder
 * @param <V> the base value type for all caches created by this builder
 * @author Charles Fry
 * @author Kevin Bourrillion
 * @author Thomas Eickert (USER)
 */
public final class CacheBuilder<K, V> {
	private static final int											DEFAULT_INITIAL_CAPACITY	= 16;
	private static final int											DEFAULT_CONCURRENCY_LEVEL	= 4;																		//ET concurrency not implemented ; keep value of 4 as this value determines the number of segments

	static final Supplier<? extends StatsCounter>	NULL_STATS_COUNTER				= () -> new StatsCounter() {
						@Override
            public void recordHits(int count) {}

						@Override
            public void recordMisses(int count) {}

						@Override
            public void recordLoadSuccess(long loadTime) {}

						@Override
            public void recordLoadException(long loadTime) {}

						@Override
            public void recordEviction() {}

            @Override
            public CacheStats snapshot() {
              return EMPTY_STATS;
            }
          };
	static final CacheStats												EMPTY_STATS								= new CacheStats(0, 0, 0, 0, 0, 0);

	static final Supplier<StatsCounter>						CACHE_STATS_COUNTER				= new Supplier<StatsCounter>() {
						@Override
						public StatsCounter get() {
							return new AbstractCache.SimpleStatsCounter();
						}
					};

	enum NullListener implements RemovalListener<Object, Object> {
		INSTANCE;

		@Override
    public void onRemoval(RemovalNotification<Object, Object> notification) {}
	}

	enum OneWeigher implements Weigher<Object, Object> {
		INSTANCE;

		@Override
		public int weigh(Object key, Object value) {
			return 1;
		}
	}

  static final Ticker NULL_TICKER =
      new Ticker() {
		    @Override
		    public long read() {
		      return 0;
		    }
		  };

	private static final Logger				logger									= Logger.getLogger(CacheBuilder.class.getName());

	static final int									UNSET_INT								= -1;

	int																initialCapacity					= UNSET_INT;
	int																concurrencyLevel				= UNSET_INT;
	long															maximumSize							= UNSET_INT;
	long															maximumWeight						= UNSET_INT;
	Weigher<? super K, ? super V>			weigher;

	long															expireAfterWriteNanos		= UNSET_INT;																			//ET not implemented
	long															expireAfterAccessNanos	= UNSET_INT;																			//ET not implemented
	long															refreshNanos						= UNSET_INT;																			//ET not implemented

  RemovalListener<? super K, ? super V> removalListener;
	Ticker														ticker;

	Supplier<? extends StatsCounter>	statsCounterSupplier		= NULL_STATS_COUNTER;

	/**
	 * Constructs a new {@code CacheBuilder} instance with default settings and no automatic eviction of any kind.
	 */
	public static CacheBuilder<Object, Object> newBuilder() {
		return new CacheBuilder<Object, Object>();
	}

	/**
	 * Sets the minimum total size for the internal hash tables.
	 *
	 * @return this {@code CacheBuilder} instance (for chaining)
	 * @throws IllegalArgumentException if {@code initialCapacity} is negative
	 * @throws IllegalStateException if an initial capacity was already set
	 */
	public CacheBuilder<K, V> initialCapacity(int initialCapacity) {
    checkState(
        this.initialCapacity == UNSET_INT,
        "initial capacity was already set to %s",
        this.initialCapacity);
		checkArgument(initialCapacity >= 0);
		this.initialCapacity = initialCapacity;
		return this;
	}

	int getInitialCapacity() {
		return (initialCapacity == UNSET_INT) ? DEFAULT_INITIAL_CAPACITY : initialCapacity;
	}

  /**
   * Used as a hint for internal sizing.
   * <p>The current implementation uses the concurrency level to create a fixed number of hashtable
   * segments. As such, when writing unit tests it is not
   * uncommon to specify {@code concurrencyLevel(1)} in order to achieve more deterministic eviction
   * behavior.
   */
	int getConcurrencyLevel() {
		return (concurrencyLevel == UNSET_INT) ? DEFAULT_CONCURRENCY_LEVEL : concurrencyLevel;
	}

	/**
	 * Specifies the maximum number of entries the cache may contain.
	 *
	 * <p>When eviction is necessary, the cache evicts entries that are less likely to be used again.
	 * For example, the cache may evict an entry because it hasn't been used recently or very often.
	 *
	 * <p>If {@code maximumSize} is zero, elements will be evicted immediately after being loaded into
	 * cache. This can be useful in testing, or to disable caching temporarily.
	 *
	 * @param maximumSize the maximum size of the cache
	 * @return this {@code CacheBuilder} instance (for chaining)
	 * @throws IllegalArgumentException if {@code maximumSize} is negative
	 * @throws IllegalStateException if a maximum size or weight was already set
	 */
	public CacheBuilder<K, V> maximumSize(long maximumSize) {
		checkState(this.maximumSize == UNSET_INT, "maximum size was already set to %s", this.maximumSize);
		checkState(this.maximumWeight == UNSET_INT, "maximum weight was already set to %s", this.maximumWeight);
		checkState(this.weigher == null, "maximum size can not be combined with weigher");
		checkArgument(maximumSize >= 0, "maximum size must not be negative");
		this.maximumSize = maximumSize;
		return this;
	}

  long getMaximumWeight() {
	if (expireAfterWriteNanos == 0 || expireAfterAccessNanos == 0) {
		return 0;
  }
    return (weigher == null) ? maximumSize : maximumWeight;
  }

	// Make a safe contravariant cast now so we don't have to do it over and over.
	@SuppressWarnings("unchecked")
	<K1 extends K, V1 extends V> Weigher<K1, V1> getWeigher() {
		return (weigher != null) ? (Weigher<K1, V1>) weigher : (Weigher<K1, V1>) OneWeigher.INSTANCE;
	}

	/**
	 * Specifies a nanosecond-precision time source for this cache. By default,
	 * {@link System#nanoTime} is used.
	 *
	 * <p>The primary intent of this method is to facilitate testing of caches with a fake or mock
	 * time source.
	 *
	 * @return this {@code CacheBuilder} instance (for chaining)
	 * @throws IllegalStateException if a ticker was already set
	 */
	public CacheBuilder<K, V> ticker(Ticker ticker) {
		checkState(this.ticker == null);
		this.ticker = Objects.requireNonNull(ticker);
		return this;
	}

	Ticker getTicker(boolean recordsTime) {
		if (ticker != null) {
			return ticker;
		}
		return recordsTime ? Ticker.systemTicker() : NULL_TICKER;
	}

	/**
	 * Specifies a listener instance that caches should notify each time an entry is removed for any
	 * {@linkplain RemovalCause reason}. Each cache created by this builder will invoke this listener
	 * as part of the routine maintenance described in the class documentation above.
	 *
	 * <p><b>Warning:</b> after invoking this method, do not continue to use <i>this</i> cache builder
	 * reference; instead use the reference this method <i>returns</i>. At runtime, these point to the
	 * same instance, but only the returned reference has the correct generic type information so as
	 * to ensure type safety. For best results, use the standard method-chaining idiom illustrated in
	 * the class documentation above, configuring a builder and building your cache in a single
	 * statement. Failure to heed this advice can result in a {@link ClassCastException} being thrown
	 * by a cache operation at some <i>undefined</i> point in the future.
	 *
	 * <p><b>Warning:</b> any exception thrown by {@code listener} will <i>not</i> be propagated to
	 * the {@code Cache} user, only logged via a {@link Logger}.
	 *
	 * @return the cache builder {@code CacheBuilder} instance (for chaining) that should be used instead of {@code this} for any
	 *     remaining configuration and cache building
	 * @throws IllegalStateException if a removal listener was already set
	 */
	public <K1 extends K, V1 extends V> CacheBuilder<K1, V1> removalListener(RemovalListener<? super K1, ? super V1> listener) {
		checkState(this.removalListener == null);

		// safely limiting the kinds of caches this can produce
		@SuppressWarnings("unchecked")
		CacheBuilder<K1, V1> me = (CacheBuilder<K1, V1>) this;
		me.removalListener = Objects.requireNonNull(listener);
		return me;
	}

	// Make a safe contravariant cast now so we don't have to do it over and over.
	@SuppressWarnings("unchecked")
	<K1 extends K, V1 extends V> RemovalListener<K1, V1> getRemovalListener() {
		return  removalListener != null ? (RemovalListener<K1, V1>) removalListener : (RemovalListener<K1, V1>) NullListener.INSTANCE;
	}

	/**
	 * Enable the accumulation of {@link CacheStats} during the operation of the cache. Without this
	 * {@link Cache#stats} will return zero for all statistics. Note that recording stats requires
	 * bookkeeping to be performed with each operation, and thus imposes a performance penalty on
	 * cache operation.
	 *
	 * @return this {@code CacheBuilder} instance (for chaining)
	 * @since 12.0 (previously, stats collection was automatic)
	 */
	public CacheBuilder<K, V> recordStats() {
		statsCounterSupplier = CACHE_STATS_COUNTER;
		return this;
	}

	boolean isRecordingStats() {
		return statsCounterSupplier == CACHE_STATS_COUNTER;
	}

	Supplier<? extends StatsCounter> getStatsCounterSupplier() {
		return statsCounterSupplier;
	}

	/**
	 * Builds a cache which does not automatically load values when keys are requested.
	 *
	 * <p>Consider {@link #build(CacheLoader)} instead, if it is feasible to implement a
	 * {@code CacheLoader}.
	 *
	 * <p>This method does not alter the state of this {@code CacheBuilder} instance, so it can be
	 * invoked again to create multiple independent caches.
	 *
	 * @return a cache having the requested features
	 * @since 11.0
	 */
	public <K1 extends K, V1 extends V> Cache<K1, V1> build() {
		    checkWeightWithWeigher();
		return new SynchronousCache.SynchronousManualCache<K1, V1>(this);
	}

  private void checkWeightWithWeigher() {
    if (weigher == null) {
      checkState(maximumWeight == UNSET_INT, "maximumWeight requires weigher");
    } else {
      if (maximumWeight == UNSET_INT) {
        logger.log(Level.WARNING, "ignoring weigher specified without maximumWeight");
      }
    }
  }

	/**
	 * Returns a string representation for this CacheBuilder instance. The exact form of the returned
	 * string is not specified.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.getClass().getSimpleName());
		if (initialCapacity != UNSET_INT) {
			sb.append("initialCapacity=" + initialCapacity);
		}
		if (concurrencyLevel != UNSET_INT) {
			sb.append("concurrencyLevel=" + concurrencyLevel);
		}
		if (maximumSize != UNSET_INT) {
			sb.append("maximumSize=" + maximumSize);
		}
		if (maximumWeight != UNSET_INT) {
			sb.append("maximumWeight=" + maximumWeight);
		}
    if (removalListener != null) {
    	sb.append("removalListener=" + removalListener);
    }
		return sb.toString();
	}
}
