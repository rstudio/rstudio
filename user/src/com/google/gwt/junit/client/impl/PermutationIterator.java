/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.junit.client.impl;

import com.google.gwt.junit.client.Range;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Iterates over all the possible permutations available in a list of
 * {@link com.google.gwt.junit.client.Range}s.
 *
 * <p>The simplest way to iterate over the permutations of multiple iterators
 * is in a nested for loop. The PermutationIterator turns that for loop inside
 * out into a single iterator, which enables you to access each permutation
 * in a piecemeal fashion.</p>
 *
 */
public class PermutationIterator implements Iterator {

  /**
   * A single permutation of all the iterators. Contains the current value
   * of each iterator for the permutation.
   *
   */
  public static class Permutation {
    private List values;
    public Permutation( List values ) {
      this.values = new ArrayList( values );
    }
    public List getValues() {
      return values;
    }
    public String toString() {
      return values.toString();
    }
  }

  private static class ListRange implements Range {
    private List list;
    public ListRange( List list ) {
      this.list = list;
    }
    public Iterator iterator() {
      return list.iterator();
    }
  }
  public static void main( String[] args ) {
    List ranges = Arrays.asList(
      new Range[] {
        new ListRange( Arrays.asList( new String[] {"a", "b", "c" } ) ),
        new ListRange( Arrays.asList( new String[] {"1", "2", "3" } ) ),
        new ListRange( Arrays.asList( new String[] {"alpha", "beta", "gamma", "delta" } ) ),
      }
    );

    System.out.println("Testing normal iteration.");
    for ( Iterator it = new PermutationIterator(ranges); it.hasNext(); ) {
      Permutation p = (Permutation) it.next();
      System.out.println(p);
    }

    System.out.println("\nTesting skipping iteration.");

    Iterator skipIterator = Arrays.asList( new String[] {"alpha", "beta", "gamma", "delta" } ).iterator();
    boolean skipped = true;
    String skipValue = null;
    for ( PermutationIterator it = new PermutationIterator(ranges); it.hasNext(); ) {
      Permutation p = (Permutation) it.next();

      if ( skipped ) {
        if ( skipIterator.hasNext() ) {
          skipValue = (String) skipIterator.next();
          skipped = false;
        }
      }

      System.out.println(p);

      String value = (String) p.getValues().get(p.getValues().size() - 1);

      if ( value.equals(skipValue) ) {
        it.skipCurrentRange();
        skipped = true;
      }
    }
  }
  private boolean firstRun = true;
  private List iterators;
  private boolean maybeHaveMore = true;
  private List ranges;

  private boolean rangeSkipped = false;

  private List values;

  /**
   * Constructs a new PermutationIterator that provides the values for each
   * possible permutation of <code>ranges</code>.
   *
   * @param ranges non-null. Each {@link com.google.gwt.junit.client.Range}
   * must have at least one element. ranges.size() must be > 1
   *
   * TODO(tobyr) Consider if empty Ranges ever make sense in the context of
   * permutations.
   *
   */
  public PermutationIterator( List ranges ) {
    this.ranges = ranges;

    iterators = new ArrayList();

    for ( int i = 0; i < ranges.size(); ++i ) {
      Range r = ( Range ) ranges.get( i );
      iterators.add( r.iterator() );
    }

    values = new ArrayList();
  }

  /**
   * Returns a new <code>Permutation</code> containing the values of the next
   * permutation.
   *
   * @return a non-null <code>Permutation</code>
   */
  public boolean hasNext() {

    if ( ! maybeHaveMore ) {
      return false;
    }

    // Walk the iterators from bottom to top checking to see if any still have
    // any available values

    for ( int currentIterator = iterators.size() - 1; currentIterator >= 0; --currentIterator ) {
      Iterator it = (Iterator) iterators.get( currentIterator );
      if ( it.hasNext() ) {
        return true;
      }
    }

    return false;
  }

  public Object next() {
    assert hasNext() : "No more available permutations in this iterator.";

    if ( firstRun ) {

      // Initialize all of our iterators and values on the first run
      for ( int i = 0; i < iterators.size(); ++i ) {
        Iterator it = ( Iterator ) iterators.get( i );
        values.add( it.next() );
      }
      firstRun = false;
      return new Permutation( values );
    }

    if ( rangeSkipped ) {
      rangeSkipped = false;
      return new Permutation( values );
    }

    // Walk through the iterators from bottom to top, finding the first one
    // which has a value available. Increment it, reset all of the subsequent
    // iterators, and then return the current permutation.
    for ( int currentIteratorIndex = iterators.size() - 1; currentIteratorIndex >= 0; --currentIteratorIndex ) {
      Iterator it = (Iterator) iterators.get( currentIteratorIndex );
      if ( it.hasNext() ) {
        values.set( currentIteratorIndex, it.next() );
        for ( int i = currentIteratorIndex + 1; i < iterators.size(); ++i ) {
          Range resetRange = (Range) ranges.get( i );
          Iterator resetIterator = resetRange.iterator();
          iterators.set(i, resetIterator);
          values.set( i, resetIterator.next() );
        }

        return new Permutation( values );
      }
    }

    throw new AssertionError( "Assertion failed - Couldn't find a non-empty iterator." );
  }

  public void remove() {
    throw new UnsupportedOperationException();
  }

  /**
   * Skips the remaining set of values in the bottom 
   * {@link com.google.gwt.junit.client.Range}. This method affects the results
   * of both hasNext() and next().
   *
   */
  public void skipCurrentRange() {

    rangeSkipped = true;

    for ( int currentIteratorIndex = iterators.size() - 2; currentIteratorIndex >= 0; --currentIteratorIndex ) {
      Iterator it = (Iterator) iterators.get( currentIteratorIndex );
      if ( it.hasNext() ) {
        values.set( currentIteratorIndex, it.next() );
        for ( int i = currentIteratorIndex + 1; i < iterators.size(); ++i ) {
          Range resetRange = (Range) ranges.get( i );
          Iterator resetIterator = resetRange.iterator();
          iterators.set( i, resetIterator );
          values.set( i, resetIterator.next() );
        }
        return;
      }
    }

    maybeHaveMore = false;
  }
}