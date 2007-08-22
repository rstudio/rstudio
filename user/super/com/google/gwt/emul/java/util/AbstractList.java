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
package java.util;

/**
 * Abstract base class for list implementations.
 * 
 * @param <E> the element type.
 */
public abstract class AbstractList<E> extends AbstractCollection<E>
    implements List<E> {

  private class IteratorImpl implements Iterator<E> {
    /*
     * i is the index of the item that will be returned on the next call to
     * next() last is the index of the item that was returned on the previous
     * call to next() or previous (for ListIterator), -1 if no such item exists.
     */

    int i = 0, last = -1;

    public boolean hasNext() {
      return i < AbstractList.this.size();
    }

    public E next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      return AbstractList.this.get(last = i++);
    }

    public void remove() {
      if (last < 0) {
        throw new IllegalStateException();
      }
      AbstractList.this.remove(last);
      i = last;
      last = -1;
    }
  }

  /**
   * Implementation of <code>ListIterator</code> for abstract lists.
   */
  private final class ListIteratorImpl extends IteratorImpl implements
      ListIterator<E> {
    /*
     * i is the index of the item that will be returned on the next call to
     * next() last is the index of the item that was returned on the previous
     * call to next() or previous (for ListIterator), -1 if no such item exists.
     */

    private ListIteratorImpl() {
      // Nothing to do
    }

    private ListIteratorImpl(int start) {
      int size = AbstractList.this.size();
      if (start < 0 || start > size) {
        AbstractList.this.indexOutOfBounds(start);
      }
      i = start;
    }

    public void add(E o) {
      AbstractList.this.add(i++, o);
      last = -1;
    }

    public boolean hasPrevious() {
      return i > 0;
    }

    public int nextIndex() {
      return i;
    }

    public E previous() {
      if (!hasPrevious()) {
        throw new NoSuchElementException();
      }
      return AbstractList.this.get(last = --i);
    }

    public int previousIndex() {
      return i - 1;
    }

    public void set(E o) {
      if (last == -1) {
        throw new IllegalStateException();
      }
      AbstractList.this.set(last, o);
    }
  }

  public void add(int index, E element) {
    throw new UnsupportedOperationException("add");
  }

  public boolean add(E obj) {
    add(size(), obj);
    return true;
  }

  public boolean addAll(int index, Collection<? extends E> c) {
    Iterator<? extends E> iter = c.iterator();
    while (iter.hasNext()) {
      add(index, iter.next());
      ++index;
    }
    return !c.isEmpty();
  }

  public void clear() {
    removeRange(0, size());
  }

  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }

    if (!(o instanceof List)) {
      return false;
    }

    List other = (List) o;
    if (size() != other.size()) {
      return false;
    }

    Iterator iter = iterator();
    Iterator iterOther = other.iterator();

    while (iter.hasNext()) {
      Object elem = iter.next();
      Object elemOther = iterOther.next();

      if (!(elem == null ? elemOther == null : elem.equals(elemOther))) {
        return false;
      }
    }

    return true;
  }

  public abstract E get(int index);

  public int hashCode() {
    int k = 1;
    final int coeff = 31;
    Iterator iter = iterator();
    while (iter.hasNext()) {
      Object obj = iter.next();
      k = coeff * k + (obj == null ? 0 : obj.hashCode());
    }
    return k;
  }

  public int indexOf(Object toFind) {
    for (int i = 0, n = size(); i < n; ++i) {
      if (toFind == null ? get(i) == null : toFind.equals(get(i))) {
        return i;
      }
    }
    return -1;
  }

  public Iterator<E> iterator() {
    return new IteratorImpl();
  }

  public int lastIndexOf(Object toFind) {
    for (int i = size() - 1; i > -1; --i) {
      if (toFind == null ? get(i) == null : toFind.equals(get(i))) {
        return i;
      }
    }
    return -1;
  }

  public ListIterator<E> listIterator() {
    return new ListIteratorImpl();
  }

  public ListIterator<E> listIterator(int from) {
    return new ListIteratorImpl(from);
  }

  public E remove(int index) {
    throw new UnsupportedOperationException("remove");
  }

  public E set(int index, E o) {
    throw new UnsupportedOperationException("set");
  }

  /**
   * Throws an <code>indexOutOfBoundsException</code>.
   */
  protected void indexOutOfBounds(int i) {
    throw new IndexOutOfBoundsException("Index: " + i + ", Size: "
        + this.size());
  }

  protected void removeRange(int fromIndex, int endIndex) {
    ListIterator<E> iter = listIterator(fromIndex);
    for (int i = fromIndex; i < endIndex; ++i) {
      iter.next();
      iter.remove();
    }
  }
}
