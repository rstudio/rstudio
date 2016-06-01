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

import static javaemul.internal.InternalPreconditions.checkArgument;
import static javaemul.internal.InternalPreconditions.checkElement;
import static javaemul.internal.InternalPreconditions.checkElementIndex;
import static javaemul.internal.InternalPreconditions.checkNotNull;
import static javaemul.internal.InternalPreconditions.checkPositionIndex;
import static javaemul.internal.InternalPreconditions.checkPositionIndexes;
import static javaemul.internal.InternalPreconditions.checkState;

import java.io.Serializable;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import javaemul.internal.ArrayHelper;

/**
 * Resizeable array implementation of the List interface.
 * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/ArrayList.html">
 * the official Java API doc</a> for details.
 *
 * <p>
 * This implementation differs from JDK 1.5 <code>ArrayList</code> in terms of
 * capacity management. There is no speed advantage to pre-allocating array
 * sizes in JavaScript, so this implementation does not include any of the
 * capacity and "growth increment" concepts in the standard ArrayList class.
 * Although <code>ArrayList(int)</code> accepts a value for the initial
 * capacity of the array, this constructor simply delegates to
 * <code>ArrayList()</code>. It is only present for compatibility with JDK
 * 1.5's API.
 * </p>
 *
 * @param <E> the element type.
 */
public class ArrayList<E> extends AbstractList<E> implements List<E>,
    Cloneable, RandomAccess, Serializable {

  /**
   * This field holds a JavaScript array.
   */
  private transient E[] array = (E[]) new Object[0];

  /**
   * Ensures that RPC will consider type parameter E to be exposed. It will be
   * pruned by dead code elimination.
   */
  @SuppressWarnings("unused")
  private E exposeElement;

  public ArrayList() {
  }

  public ArrayList(Collection<? extends E> c) {
    // Avoid calling overridable methods from constructors
    ArrayHelper.insertTo(array, 0, c.toArray());
  }

  public ArrayList(int initialCapacity) {
    // Avoid calling overridable methods from constructors
    checkArgument(initialCapacity >= 0, "Initial capacity must not be negative");
  }

  @Override
  public boolean add(E o) {
    array[array.length] = o;
    return true;
  }

  @Override
  public void add(int index, E o) {
    checkPositionIndex(index, array.length);
    ArrayHelper.insertTo(array, index, o);
  }

  @Override
  public boolean addAll(Collection<? extends E> c) {
    Object[] cArray = c.toArray();
    int len = cArray.length;
    if (len == 0) {
      return false;
    }
    ArrayHelper.insertTo(array, array.length, cArray);
    return true;
  }

  @Override
  public boolean addAll(int index, Collection<? extends E> c) {
    checkPositionIndex(index, array.length);
    Object[] cArray = c.toArray();
    int len = cArray.length;
    if (len == 0) {
      return false;
    }
    ArrayHelper.insertTo(array, index, cArray);
    return true;
  }

  @Override
  public void clear() {
    array = (E[]) new Object[0];
  }

  public Object clone() {
    return new ArrayList<E>(this);
  }

  @Override
  public boolean contains(Object o) {
    return (indexOf(o) != -1);
  }

  public void ensureCapacity(int ignored) {
    // Ignored.
  }

  @Override
  public E get(int index) {
    checkElementIndex(index, array.length);
    return array[index];
  }

  @Override
  public int indexOf(Object o) {
    return indexOf(o, 0);
  }

  @Override
  public Iterator<E> iterator() {
    return new Iterator<E>() {
      int i = 0, last = -1;

      @Override
      public boolean hasNext() {
        return i < array.length;
      }

      @Override
      public E next() {
        checkElement(hasNext());

        last = i++;
        return array[last];
      }

      @Override
      public void remove() {
        checkState(last != -1);

        ArrayList.this.remove(i = last);
        last = -1;
      }
    };
  }

  @Override
  public void forEach(Consumer<? super E> consumer) {
    checkNotNull(consumer);
    for (E e : array) {
      consumer.accept(e);
    }
  }

  @Override
  public boolean isEmpty() {
    return array.length == 0;
  }

  @Override
  public int lastIndexOf(Object o) {
    return lastIndexOf(o, size() - 1);
  }

  @Override
  public E remove(int index) {
    E previous = get(index);
    ArrayHelper.removeFrom(array, index, 1);
    return previous;
  }

  @Override
  public boolean remove(Object o) {
    int i = indexOf(o);
    if (i == -1) {
      return false;
    }
    remove(i);
    return true;
  }

  @Override
  public boolean removeIf(Predicate<? super E> filter) {
    checkNotNull(filter);

    E[] newArray = null;
    int newIndex = 0;
    for (int index = 0; index < array.length; ++index) {
      E e = array[index];

      if (filter.test(e)) {
        if (newArray == null) {
          newArray = ArrayHelper.clone(array, 0, index);
          newIndex = index;
        }
      } else if (newArray != null) {
        newArray[newIndex++] = e;
      }
    }

    if (newArray == null) {
      return false;
    }
    array = newArray;
    return true;
  }

  @Override
  public void replaceAll(UnaryOperator<E> operator) {
    checkNotNull(operator);
    for (int i = 0; i < array.length; i++) {
      array[i] = operator.apply(array[i]);
    }
  }

  @Override
  public E set(int index, E o) {
    E previous = get(index);
    array[index] = o;
    return previous;
  }

  @Override
  public int size() {
    return array.length;
  }

  @Override
  public void sort(Comparator<? super E> c) {
    Arrays.sort(array, 0, array.length, c);
  }

  @Override
  public Object[] toArray() {
    return ArrayHelper.clone(array, 0, array.length);
  }

  /*
   * Faster than the iterator-based implementation in AbstractCollection.
   */
  @SuppressWarnings("unchecked")
  @Override
  public <T> T[] toArray(T[] out) {
    int size = array.length;
    if (out.length < size) {
      out = ArrayHelper.createFrom(out, size);
    }
    for (int i = 0; i < size; ++i) {
      out[i] = (T) array[i];
    }
    if (out.length > size) {
      out[size] = null;
    }
    return out;
  }

  public void trimToSize() {
    // We are always trimmed to size.
  }

  @Override
  protected void removeRange(int fromIndex, int endIndex) {
    checkPositionIndexes(fromIndex, endIndex, array.length);
    int count = endIndex - fromIndex;
    ArrayHelper.removeFrom(array, fromIndex, count);
  }

  /**
   * Used by Vector.
   */
  int indexOf(Object o, int index) {
    for (; index < array.length; ++index) {
      if (Objects.equals(o, array[index])) {
        return index;
      }
    }
    return -1;
  }

  /**
   * Used by Vector.
   */
  int lastIndexOf(Object o, int index) {
    for (; index >= 0; --index) {
      if (Objects.equals(o, array[index])) {
        return index;
      }
    }
    return -1;
  }

  void setSize(int newSize) {
    ArrayHelper.setLength(array, newSize);
  }
}
