/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.core.ext.linker;

import com.google.gwt.thirdparty.guava.common.base.Supplier;
import com.google.gwt.thirdparty.guava.common.collect.ForwardingIterator;
import com.google.gwt.thirdparty.guava.common.collect.ForwardingSortedSet;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableSortedSet;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;
import com.google.gwt.thirdparty.guava.common.collect.Multimaps;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A SortedSet that maintains an index of its members by concrete type using
 * a {@link TypeIndex}.
 */
class TypeIndexedSet<T extends Comparable> extends ForwardingSortedSet<T> implements Serializable {
  /**
   * TypeIndexedSet delegates to this set for its regular SortedSet operations.
   */
  private SortedSet<T> treeSet;

  /**
   * Groups set members by their concrete type.  This set is shared between
   * the root TypeIndexedSet and views created through subSet, headSet, and
   * tailSet so that mutates through those sets will be reflected in this set.
   *
   * <p>This is maintained to speed-up the find operation.  It is generated on
   * the first call to {@link TypeIndex#findAssignableTo(Class)} and kept
   * up-to-date during each subsequent set mutation.
   *
   * <p>For find, all members that are assignable to a type can be quickly
   * located by iterating through the keys of this index, avoiding the need to
   * individually check each member of the set; since they share a concrete
   * type, they will collectively be assignable or not assignable to the type
   * being found.
   */
  private transient TypeIndex byType;

  protected TypeIndexedSet(SortedSet<T> backing) {
    this(backing, new TypeIndex(backing));
  }

  private TypeIndexedSet(SortedSet<T> backing, TypeIndex byType) {
    this.treeSet = backing;
    this.byType = byType;
  }

  @Override
  protected SortedSet<T> delegate() {
    return treeSet;
  }

  /**
   * Returns the type index for the root set, i.e. the set that is not a view of
   * other sets.
   */
  TypeIndex getTypeIndex() {
    return byType;
  }

  /**
   * Creates a view of this Set.  Mutates to the view will update the type
   * index.
   */
  private SortedSet<T> createView(final SortedSet<T> rawView) {
    return new ForwardingSortedSet<T>() {
      private final TypeIndexedSet<T> wrappedForIndexMutates =
          new TypeIndexedSet<T>(rawView, byType);

      @Override
      protected SortedSet<T> delegate() {
        return wrappedForIndexMutates;
      }
    };
  }

  @Override
  public boolean add(T o) {
    if (super.add(o)) {
      byType.add(o);
      return true;
    }
    return false;
  }

  @Override
  public boolean addAll(Collection<? extends T> c) {
    boolean somethingChanged = false;
    for (T a : c) {
      somethingChanged |= add(a);
    }
    return somethingChanged;
  }

  @Override
  public void clear() {
    super.clear();
    byType.clear();
  }

  /**
   * Prevent further modification of the Set. Any attempts to alter
   * the Set after invoking this method will result in an
   * UnsupportedOperationException.
   */
  public void freeze() {
    // This is not comprehensive.  If a subset/headset/tailset is returned prior
    // to freeze, then this set can still be modified post-freeze through those
    // sets.
    if (treeSet instanceof TreeSet<?>) {
      treeSet = Collections.unmodifiableSortedSet(treeSet);
    }
  }

  @Override
  public SortedSet<T> headSet(T toElement) {
    return createView(super.headSet(toElement));
  }

  @Override
  public Iterator<T> iterator() {
    final Iterator<T> backing = super.iterator();

    return new ForwardingIterator<T>() {
      private T previous;

      @Override
      protected Iterator<T> delegate() {
        return backing;
      }

      @Override
      public T next() {
        previous = delegate().next();
        return previous;
      }

      @Override
      public void remove() {
        delegate().remove();
        byType.remove(previous);
      }
    };
  }

  @Override
  public boolean remove(Object o) {
    if (super.remove(o)) {
      byType.remove(o);
      return true;
    }
    return false;
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    boolean somethingChanged = false;
    for (Object o : c) {
      somethingChanged |= remove(o);
    }
    return somethingChanged;
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    if (super.retainAll(c)) {
      byType.clear();
      return true;
    }
    return false;
  }

  @Override
  public SortedSet<T> subSet(T fromElement, T toElement) {
    return createView(super.subSet(fromElement, toElement));
  }

  @Override
  public SortedSet<T> tailSet(T fromElement) {
    return createView(super.tailSet(fromElement));
  }

  /**
   * Organizes set members by their concrete type.
   */
  static final class TypeIndex {
    private static final Supplier<SortedSet<Comparable>> TREE_SETS =
        new Supplier<SortedSet<Comparable>>() {
      @Override
      public SortedSet<Comparable> get() {
        return new TreeSet<Comparable>();
      }
    };

    private final Iterable<? extends Comparable> elements;
    private transient Multimap<Class<?>, Comparable> index = null;

    /**
     * Caches the results of {@link #find(Class)}.  If an entry for the Class
     * being found is located in the cache, it can be returned immediately.
     *
     * <p>If a set is mutated, the cache is cleared.
     */
    private transient Map<Class<?>, SortedSet<?>> findCache;

    TypeIndex(Iterable<? extends Comparable> elements) {
      this.elements = elements;
    }

    void clear() {
      findCache = null;
      index = null;
    }

    /**
     * Adds an item to the index.  If the initial index has not yet been
     * created, no action is taken.
     */
    void add(Comparable o) {
      if (index != null) {
        index.put(o.getClass(), o);
      }
      findCache = null;
    }

    /**
     * Removes an item from the index.  If the initial index has not yet been
     * created, no action is taken.
     */
    void remove(Object o) {
      if (index != null) {
        index.remove(o.getClass(), o);
      }
      findCache = null;
    }

    /**
     * Locates all indexed members that can be assigned to type.
     */
    @SuppressWarnings("unchecked")
    <T extends Comparable> SortedSet<T> findAssignableTo(Class<T> type) {
      if (findCache == null) {
        // Create the cache since it did not previously exist.
        findCache = new HashMap<Class<?>, SortedSet<?>>();
      } else {
        // The cache previously existed, so see if it has an entry that matches
        // type.
        SortedSet<?> s = findCache.get(type);
        if (s != null) {
          return (SortedSet<T>) s;
        }
      }

      // Cache miss.  Do this the harder way.

      // Create a type index if it did not previously exist.  The type index is
      // necessary below.
      if (index == null) {
        generateTypeIndex();
      }

      // Using the type index, gather all members assignable to type.
      ImmutableSortedSet.Builder<T> builder = ImmutableSortedSet.<T>naturalOrder();
      for (Map.Entry<Class<?>, ?> entry : index.asMap().entrySet()) {
        if (type.isAssignableFrom(entry.getKey())) {
          builder.addAll((SortedSet<? extends T>) entry.getValue());
        }
      }
      SortedSet<T> toReturn = builder.build();

      // Cache the result
      findCache.put(type, toReturn);
      return toReturn;
    }

    /**
     * Groups all set members by their concrete type and puts the result into
     * the 'byType' index.
     */
    private void generateTypeIndex() {
      index = Multimaps.newSortedSetMultimap(
          new HashMap<Class<?>, Collection<Comparable>>(), TREE_SETS);
      for (Comparable o : elements) {
        index.put(o.getClass(), o);
      }
    }
  }
}
