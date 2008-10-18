/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.benchmarks.client.impl;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Iterates over all the possible permutations available in a list of {@link
 * Iterable Iterables}.
 * 
 * <p>
 * The simplest way to iterate over the permutations of multiple iterators is in
 * a nested for loop. The PermutationIterator turns that for loop inside out
 * into a single iterator, which enables you to access each permutation in a
 * piecemeal fashion.
 * </p>
 */
public class PermutationIterator implements
    Iterator<PermutationIterator.Permutation> {

  /**
   * A single permutation of all the iterators. Contains the current value of
   * each iterator for the permutation.
   */
  public static class Permutation {

    private List<Object> values;

    public Permutation(List<?> values) {
      this.values = new ArrayList<Object>(values);
    }

    public List<Object> getValues() {
      return values;
    }

    @Override
    public String toString() {
      return values.toString();
    }
  }

  public static void main(String[] args) {
    List<Iterable<String>> iterables = new ArrayList<Iterable<String>>(3);
    iterables.add(Arrays.asList("a", "b", "c"));
    iterables.add(Arrays.asList("1", "2", "3"));
    iterables.add(Arrays.asList("alpha", "beta", "gamma", "delta"));

    System.out.println("Testing normal iteration.");
    for (Iterator<Permutation> it = new PermutationIterator(iterables); it.hasNext();) {
      Permutation p = it.next();
      System.out.println(p);
    }

    System.out.println("\nTesting skipping iteration.");

    Iterator<String> skipIterator = Arrays.asList("alpha", "beta", "gamma",
        "delta").iterator();
    boolean skipped = true;
    String skipValue = null;
    for (PermutationIterator it = new PermutationIterator(iterables); it.hasNext();) {
      Permutation p = it.next();

      if (skipped) {
        if (skipIterator.hasNext()) {
          skipValue = skipIterator.next();
          skipped = false;
        }
      }

      System.out.println(p);

      Object value = p.getValues().get(p.getValues().size() - 1);

      if (value.equals(skipValue)) {
        it.skipCurrentRange();
        skipped = true;
      }
    }
  }

  /**
   * Is this the first run?
   */
  private boolean firstRun = true;

  /**
   * The iterator from every range.
   */
  private List<Iterator<?>> iterators;

  /**
   * Are more permutations available?
   */
  private boolean maybeHaveMore = true;

  /**
   * The {@code Iterables} to permute.
   */
  private List<? extends Iterable<?>> iterables;

  /**
   * Did we just skip a range? If so, the values List already contains the
   * values of the next permutation.
   */
  private boolean rangeSkipped = false;

  /**
   * The current permutation of values.
   */
  private List<Object> values;

  /**
   * Constructs a new PermutationIterator that provides the values for each
   * possible permutation of <code>iterables</code>.
   * 
   * @param iterables non-null. Each {@link Iterable} must have at least one
   *          element. iterables.size() must be > 1
   * 
   * TODO(tobyr) Consider if empty Iterables ever make sense in the context of
   * permutations.
   */
  public PermutationIterator(List<? extends Iterable<?>> iterables) {
    this.iterables = iterables;

    iterators = new ArrayList<Iterator<?>>();

    for (Iterable<?> iterable : iterables) {
      iterators.add(iterable.iterator());
    }

    values = new ArrayList<Object>();
  }

  /**
   * Returns a new <code>Permutation</code> containing the values of the next
   * permutation.
   * 
   * @return a non-null <code>Permutation</code>
   */
  public boolean hasNext() {

    if (!maybeHaveMore) {
      return false;
    }

    // Walk the iterators from bottom to top checking to see if any still have
    // any available values

    for (int currentIterator = iterators.size() - 1; currentIterator >= 0; --currentIterator) {
      Iterator<?> it = iterators.get(currentIterator);
      if (it.hasNext()) {
        return true;
      }
    }

    return false;
  }

  public Permutation next() {
    assert hasNext() : "No more available permutations in this iterator.";

    if (firstRun) {

      // Initialize all of our iterators and values on the first run
      for (Iterator<?> it : iterators) {
        values.add(it.next());
      }
      firstRun = false;
      return new Permutation(values);
    }

    if (rangeSkipped) {
      rangeSkipped = false;
      return new Permutation(values);
    }

    // Walk through the iterators from bottom to top, finding the first one
    // which has a value available. Increment it, reset all of the subsequent
    // iterators, and then return the current permutation.
    for (int currentIteratorIndex = iterators.size() - 1; currentIteratorIndex >= 0; --currentIteratorIndex) {
      Iterator<?> it = iterators.get(currentIteratorIndex);
      if (it.hasNext()) {
        values.set(currentIteratorIndex, it.next());
        for (int i = currentIteratorIndex + 1; i < iterators.size(); ++i) {
          Iterable<?> resetIterable = iterables.get(i);
          Iterator<?> resetIterator = resetIterable.iterator();
          iterators.set(i, resetIterator);
          values.set(i, resetIterator.next());
        }

        return new Permutation(values);
      }
    }

    throw new AssertionError(
        "Assertion failed - Couldn't find a non-empty iterator.");
  }

  public void remove() {
    throw new UnsupportedOperationException();
  }

  /**
   * Skips the remaining set of values in the bottom {@code Iterable}. This
   * method affects the results of both hasNext() and next().
   */
  public void skipCurrentRange() {

    rangeSkipped = true;

    for (int currentIteratorIndex = iterators.size() - 2; currentIteratorIndex >= 0; --currentIteratorIndex) {
      Iterator<?> it = iterators.get(currentIteratorIndex);
      if (it.hasNext()) {
        values.set(currentIteratorIndex, it.next());
        for (int i = currentIteratorIndex + 1; i < iterators.size(); ++i) {
          Iterable<?> resetIterable = iterables.get(i);
          Iterator<?> resetIterator = resetIterable.iterator();
          iterators.set(i, resetIterator);
          values.set(i, resetIterator.next());
        }
        return;
      }
    }

    maybeHaveMore = false;
  }
}