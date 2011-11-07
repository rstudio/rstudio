/*
 * Copyright 2011 Google Inc.
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

import junit.framework.TestCase;

/**
 * Verifies that we can intern strings.
 *
 * @author skybrian@google.com (Brian Slesinsky)
 */
public class StringInternerTest extends TestCase {
  private StringInterner interner;

  protected void setUp() throws Exception {
    super.setUp();
    interner = StringInterner.get();
  }

  public void testCanInternString() throws Exception {
    String firstIn = "fnord123";
    String secondIn = new String(firstIn);
    assertFalse(firstIn == secondIn);

    String firstOut = interner.intern(firstIn);
    String secondOut = interner.intern(secondIn);
    assertSame(firstOut, secondOut);
  }

  public void testCanFreeString() throws Exception {
    // Intern about a gigabyte of data.
    // If we don't free any interned strings, we should run out of memory.
    // (Verified that it fails using Interns.newStrongInterner().)
    String prefix = repeat('a', 1000);
    for (int i = 0; i < 1000 * 1000; i++) {
      interner.intern(prefix + i);
    }
  }

  public void testShardsAreSomewhatBalanced() throws Exception {

    // Simulate adding a million strings. We use the production algorithm to choose the shard,
    // but increment a counter instead. This avoids the WeakHashMap's nondeterministic
    // behavior due to garbage collection.
    int meanShardSize = 1000;
    int shardSizes[] = new int[StringInterner.SHARD_COUNT];
    int stringsToAdd = StringInterner.SHARD_COUNT * meanShardSize;
    for (int i = 0; i < stringsToAdd; i++) {
      int shardId = interner.getShardId("foo" + i);
      shardSizes[shardId]++;
    }

    // Verify that no shards are too big. (A shard that's oversized could create lock contention.)

    int expectedMaxShardSize = meanShardSize * 2;
    int maxShardSize = 0;
    int tooBigShardCount = 0;
    for (int shardSize : shardSizes) {
      maxShardSize = Math.max(maxShardSize, shardSize);
      if (shardSize > expectedMaxShardSize) {
        tooBigShardCount++;
      }
    }

    if (tooBigShardCount > 0) {
      fail(tooBigShardCount + " of " + shardSizes.length + " shards are too big (more than " +
          expectedMaxShardSize + " entries); largest shard has " + maxShardSize + " entries.");
    }
  }

  private static String repeat(char c, int length) {
    StringBuilder buffer = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      buffer.append(c);
    }
    return buffer.toString();
  }
}
