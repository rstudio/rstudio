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
package com.google.gwt.sample.expenses.gwt.client;

import com.google.gwt.bikeshed.list.client.Column;
import com.google.gwt.cell.client.Cell;

import java.util.Comparator;

/**
 * A column that provides forward and reverse {@link Comparator}s.
 * 
 * @param <T> the row type
 * @param <C> the column type
 */
public abstract class SortableColumn<T, C> extends Column<T, C> {
  private Comparator<T> forwardComparator;

  private Comparator<T> reverseComparator;

  public SortableColumn(Cell<C> cell) {
    super(cell);
  }

  /**
   * Convenience method to return a {@link Comparator} that may be used to sort
   * records of type T by the values of this column, using the natural ordering
   * of the column type C. If C does not implement Comparable<C>, a runtime
   * exception will be thrown when the returned comparator's
   * {@link Comparator#compare(Object, Object) compare} method is called. If
   * reverse is true, the returned comparator will sort in reverse order. The
   * returned comparator instances are cached for future calls.
   * 
   * @param reverse if true, sort in reverse
   * @return an instance of Comparator<T>
   */
  public Comparator<T> getComparator(final boolean reverse) {
    if (!reverse && forwardComparator != null) {
      return forwardComparator;
    }
    if (reverse && reverseComparator != null) {
      return reverseComparator;
    }
    Comparator<T> comparator = new Comparator<T>() {
      @SuppressWarnings("unchecked")
      public int compare(T o1, T o2) {
        C c1 = getValue(o1);
        C c2 = getValue(o2);
        int comparison = ((Comparable<C>) c1).compareTo(c2);
        return reverse ? -comparison : comparison;
      }
    };

    if (reverse) {
      reverseComparator = comparator;
    } else {
      forwardComparator = comparator;
    }
    return comparator;
  }
}
