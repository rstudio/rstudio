/*
 * Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.gwt.dev.util;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/**
 * A utility class for reducing String memory waste.
 *
 * <p> We don't use the String.intern() method because it would prevent GC and fill the PermGen
 * space. We also don't use Guava (for now) due to a class loader GC issue with the old
 * version of Guava we're using. </p>
 *
 * <p> Thread-safe. The implementation uses shards to reduce thread contention. </p>
 */
public class StringInterner {
  // chosen via performance testing with AWFE. (See AWFE load times speadsheet.)
  private static int SHARD_BITS = 10;

  static int SHARD_COUNT = 1 << SHARD_BITS;
  private static int SHARD_MASK = SHARD_COUNT - 1;

  private static final StringInterner instance = new StringInterner();

  public static StringInterner get() {
    return instance;
  }

  private final Shard[] shards = new Shard[SHARD_COUNT];

  protected StringInterner() {
    for (int i = 0; i < SHARD_COUNT; i++) {
      shards[i] = new Shard();
    }
  }

  /**
   * Returns a string equal to the input string, but not always the same.
   */
  public String intern(String original) {
    int shardId = getShardId(original);
    return shards[shardId].intern(original);
  }

  /* visible for testing */
  int getShardId(String original) {
    int hashCode = original.hashCode();
    return (hashCode ^ (hashCode >> SHARD_BITS)) & SHARD_MASK;
  }

  private static class Shard {
    private final WeakHashMap<String, WeakReference<String>> map =
        new WeakHashMap<String, WeakReference<String>>();

    private synchronized String intern(String original) {
      WeakReference<String> ref = map.get(original);
      String interned = ref == null ? null : ref.get();
      if (interned == null) {
        ref = new WeakReference<String>(original);
        map.put(original, ref);
        return original;
      } else {
        return interned;
      }
    }
  }
}
