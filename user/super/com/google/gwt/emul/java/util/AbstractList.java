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
 * Skeletal implementation of the List interface. <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/AbstractList.html">[Sun
 * docs]</a>
 * 
 * @param <E> the element type.
 */
public abstract class AbstractList<E> extends AbstractCollection<E> implements
    List<E> {

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
        indexOutOfBounds(start, size);
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

  private static class SubList<E> extends AbstractList<E> {
    private final List<E> wrapped;
    private final int fromIndex;
    private int size;

    public SubList(List<E> wrapped, int fromIndex, int toIndex) {
      this.wrapped = wrapped;
      this.fromIndex = fromIndex;
      size = getSize(fromIndex, toIndex);
      if (fromIndex > toIndex) {
        throw new IllegalArgumentException("fromIndex: " + fromIndex + 
            " > toIndex: " + toIndex);
      }
      if (fromIndex < 0) {
        throw new IndexOutOfBoundsException("fromIndex: " + fromIndex + 
            " < 0");
      }
      if (toIndex > wrapped.size()) {
        throw new IndexOutOfBoundsException("toIndex: " + toIndex +
            " > wrapped.size() " + wrapped.size());
      }
    }

    @Override
    public void add(int index, E element) {
      checkIndexForAdd(index);
      size++;
      wrapped.add(fromIndex + index, element);   
    }

    @Override
    public E get(int index) {
      checkIndex(index);
      return wrapped.get(fromIndex + index);
    }

    @Override
    public E remove(int index) {
      checkIndex(index);
      E result = wrapped.remove(fromIndex + index);
      size--;
      return result;
    }

    @Override
    public E set(int index, E element) {
      checkIndex(index);
      return wrapped.set(fromIndex + index, element);
    }

    @Override
    public int size() {
      return size;
    }
    
    private void checkIndex(int index) {
      checkIndex(index, size);
    }
        
    private void checkIndexForAdd(int index) {
      checkIndex(index, size + 1);
    }

    private int getSize(int fromIndex, int toIndex) {
      return toIndex - fromIndex;
    }
  }

  protected static void checkIndex(int index, int size) {
    if (index < 0 || index >= size) {
      indexOutOfBounds(index, size);
    }
  }

  /**
   * Throws an <code>indexOutOfBoundsException</code>.
   */
  protected static void indexOutOfBounds(int index, int size) {
    throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
  }

  protected transient int modCount = 0;

  protected AbstractList() {
  }

  @Override
  public boolean add(E obj) {
    add(size(), obj);
    return true;
  }

  public void add(int index, E element) {
    throw new UnsupportedOperationException("Add not supported on this list");
  }

  public boolean addAll(int index, Collection<? extends E> c) {
    Iterator<? extends E> iter = c.iterator();
    boolean changed = false;
    while (iter.hasNext()) {
      add(index++, iter.next());
      changed = true;
    }
    return changed;
  }

  @Override
  public void clear() {
    removeRange(0, size());
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }

    if (!(o instanceof List)) {
      return false;
    }

    List<?> other = (List<?>) o;
    if (size() != other.size()) {
      return false;
    }

    Iterator<E> iter = iterator();
    Iterator<?> iterOther = other.iterator();

    while (iter.hasNext()) {
      E elem = iter.next();
      Object elemOther = iterOther.next();

      if (!(elem == null ? elemOther == null : elem.equals(elemOther))) {
        return false;
      }
    }

    return true;
  }

  public abstract E get(int index);

  @Override
  public int hashCode() {
    int k = 1;
    final int coeff = 31;
    Iterator<E> iter = iterator();
    while (iter.hasNext()) {
      E obj = iter.next();
      k = coeff * k + (obj == null ? 0 : obj.hashCode());
      k = ~~k;
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

  @Override
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
    return listIterator(0);
  }

  public ListIterator<E> listIterator(int from) {
    return new ListIteratorImpl(from);
  }

  public E remove(int index) {
    throw new UnsupportedOperationException("Remove not supported on this list");
  }

  public E set(int index, E o) {
    throw new UnsupportedOperationException("Set not supported on this list");
  }

  public List<E> subList(int fromIndex, int toIndex) {
    return new SubList<E>(this, fromIndex, toIndex);
  }

  protected void removeRange(int fromIndex, int endIndex) {
    ListIterator<E> iter = listIterator(fromIndex);
    for (int i = fromIndex; i < endIndex; ++i) {
      iter.next();
      iter.remove();
    }
  }
}
