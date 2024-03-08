/*
 * Copyright (C) 2011 The Guava Authors
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

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * The reason why a cached entry was removed.
 *
 * <p>Changes to the Guava implementation:
 * <ul>
 * <li>NO:  time-based expiration of entries
 * <li>NO:  keys automatically wrapped in weak references
 * </ul>
 *
 * @author Charles Fry
 * @author Thomas Eickert (USER)
 */
public enum RemovalCause {
  /**
   * The entry was manually removed by the user. This can result from the user invoking
   * {@link Cache#invalidate}, {@link Cache#invalidateAll(Iterable)}, {@link Cache#invalidateAll()},
   * {@link Map#remove}, {@link ConcurrentMap#remove}, or {@link Iterator#remove}.
   */
  EXPLICIT {
    @Override
    boolean wasEvicted() {
      return false;
    }
  },

  /**
   * The entry itself was not actually removed, but its value was replaced by the user. This can
   * result from the user invoking {@link Cache#put}, {@link Map#put},
   * {@link Map#putAll}, {@link ConcurrentMap#replace(Object, Object)}, or
   * {@link ConcurrentMap#replace(Object, Object, Object)}.
   */
  REPLACED {
    @Override
    boolean wasEvicted() {
      return false;
    }
  },

  /**
   * The entry was evicted due to size constraints. This can occur when using
   * {@link CacheBuilder#maximumSize} or {@link CacheBuilder#maximumWeight}.
   */
  SIZE {
    @Override
    boolean wasEvicted() {
      return true;
    }
  };

  /**
   * Returns {@code true} if there was an automatic removal due to eviction (the cause is neither
   * {@link #EXPLICIT} nor {@link #REPLACED}).
   */
  abstract boolean wasEvicted();
}
