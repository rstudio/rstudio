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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * A utility class for reducing String memory waste.
 *
 * <p>We don't use the String.intern() method because it would prevent GC and fill the PermGen
 * space. It also runs comparatively slowly. We also don't use Guava (for now) due to a class
 * loader GC issue with the old version of Guava we're using. </p>
 *
 * <p>Thread-safe.</p>
 */
public final class StringInterner {
  private static final StringInterner instance = new StringInterner(16);

  public static StringInterner get() {
    return instance;
  }

  /*
   * A non-blocking, hash table-based, weak-referencing interner.
   *
   * Implementation notes:
   *
   * - Buckets are linked-lists of entries that hash to that bucket.
   *
   * - For a given bucket, all mutates happen at the head of the bucket, which
   *   makes it easier to deconflict concurrent actors using
   *   AtomicReferenceArray.  If two writes happen at the same time, one will
   *   win, and the other will lose in a detectable way where it can be
   *   trivially retried.
   *
   * - While rehashing to a larger table, the rehasher will place forwarding
   *   markers at the buckets that have been moved to the new table.  This
   *   prevents other threads from writing to an old-table bucket after it has
   *   moved.
   *
   * - Dead WeakReferences are collected only during rehashing operations.
   *
   * - Tables are always power-of-two size, and they double during rehashing.
   *   Additionally, hash codes are taken verbatim.
   */

  /**
   * Marks valid entry types for the table.
   */
  private interface Entry {
  }

  /**
   * An entry that bears an interned String and perhaps a pointer to another
   * entry that also lives in the same bucket of the table.
   */
  private static final class ValueEntry extends WeakReference<String>
      implements Entry {

    /**
     * The next entry at this bucket.
     */
    final ValueEntry next;

    ValueEntry(String s, ValueEntry next) {
      super(s);
      this.next = next;
    }
  }

  /**
   * An entry that points to a successor array for the current {@code table}.
   *
   * <p>Finding one of these means that a rehashing operation has copied the
   * prior contents of this bucket to a new table.
   */
  private static final class ForwardingEntry implements Entry {

    /**
     * Points at the successor table, where this bucket's contents have moved
     * to.
     */
    final AtomicReferenceArray<Entry> forward;

    ForwardingEntry(AtomicReferenceArray<Entry> forward) {
      this.forward = forward;
    }
  }

  /**
   * When the table has more than this percentage of entries (whether alive or
   * dead due to the GC having collected a weak reference), the table will
   * rehash.
   */
  private static final int LOAD_FACTOR_PERCENT = 75;

  /**
   * Flag that is atomically set by a thread as it begins a rehashing operation.
   * Acts as a light-weight lock; only one thread can set it at a time, so it
   * prevents multiple threads from performing a rehash.
   */
  private final AtomicBoolean rehashing = new AtomicBoolean(false);

  /**
   * The number of entries in the table.
   */
  private final AtomicInteger entries = new AtomicInteger(0);

  /**
   * The table itself.
   */
  private volatile AtomicReferenceArray<Entry> table;

  /**
   * When the number of entries exceeds this value, a rehash operation should
   * start.
   */
  private volatile int rehashAt;

  /**
   * Creates a StringInterner that initially has 2 ^ {@code twoPower} buckets.
   */
  public StringInterner(int twoPower) {
    int length = 1 << twoPower;
    rehashAt = (LOAD_FACTOR_PERCENT * length) / 100;
    table = new AtomicReferenceArray<StringInterner.Entry>(length);
  }

  private static int getMask(AtomicReferenceArray<Entry> table) {
    return table.length() - 1;
  }

  /**
   * For all strings that are returned from this method, ensures that there are
   * never two {@code equals()} intern'ed strings alive in the system at the
   * same time.
   */
  public String intern(String original) {
    AtomicReferenceArray<Entry> currentTable = table;
    ValueEntry runUntil = null;

    while (true) {
      int index = original.hashCode() & getMask(currentTable);
      Entry head = currentTable.get(index);

      // Look for a forward
      if (head instanceof ForwardingEntry) {
        // Some other thread has rehashed this bucket
        currentTable = ((ForwardingEntry) head).forward;
        runUntil = null;
        continue;
      }

      // Look for an existing match
      ValueEntry vHead = (ValueEntry) head;
      for (ValueEntry current = vHead; current != runUntil; current = current.next) {
        String s = current.get();
        if (original.equals(s)) {
          // Found an interned version
          return s;
        }
      }

      // Store a newly interned string
      ValueEntry newEntry = new ValueEntry(original, vHead);
      if (currentTable.compareAndSet(index, head, newEntry)) {
        // Successfully added the new entry
        maybeRehash();
        return original;
      }
      runUntil = vHead;
    }
  }

  /**
   * If the number of entries exceeds the load-factor threshold for the hash
   * table, rehash all of the entries into a table twice the size.
   */
  private void maybeRehash() {
    if ((entries.incrementAndGet() > rehashAt) && !rehashing.getAndSet(true)) {
      // Create a new table
      int length = table.length() << 1;
      AtomicReferenceArray<Entry> newTable =
          new AtomicReferenceArray<StringInterner.Entry>(length);

      // Set this early.  It saves other threads a trivial amount of work.
      rehashAt = (LOAD_FACTOR_PERCENT * length) / 100;

      // This points concurrent actors towards the new table once we've
      // completed work on a bucket.
      ForwardingEntry forwardingEntry = new ForwardingEntry(newTable);
      int mask = getMask(newTable);

      int emptyEntries = 0;
      for (int i = 0, c = table.length(); i < c; i++) {
        ValueEntry head = null;
        do {
          ValueEntry runUntil = head; // the old head or null (true tail on first pass)
          head = (ValueEntry) table.get(i); // get the new head
          for (ValueEntry e = head, nextE; e != runUntil; e = nextE) {
            nextE = e.next;
            String s = e.get();
            if (s != null) {
              int index = s.hashCode() & mask;
              ValueEntry copiedEntry =
                  new ValueEntry(s, (ValueEntry) newTable.get(index));
              newTable.set(index, copiedEntry);
            } else {
              emptyEntries += 1;
            }
          }
        } while (!table.compareAndSet(i, head, forwardingEntry));
      }
      entries.addAndGet(-emptyEntries);
      table = newTable;

      rehashing.set(false);
    }
  }
}
