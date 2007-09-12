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
 * A {@link java.util.Set} of {@link Enum}s. <a href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/EnumSet.html">[Sun
 * docs]</a>
 *
 * @param <E> enumeration type
 */
public abstract class EnumSet<E extends Enum<E>> extends AbstractSet<E> { 

  /**
   * An implementation of EnumSet that works for Enums with arbitrarily large
   * numbers of values.
   *
   * TODO(tobyr) Consider implementing this like SimpleEnumSet, but backed by
   * int[]'s instead of ints.
   */
  static class LargeEnumSet<E extends Enum<E>> extends EnumSet<E> {

    HashSet<E> set = new HashSet<E>();

    private E[] allEnums;  // Must not be modified

    LargeEnumSet(E[] allValues) {
      this.allEnums = allValues;
    }

    LargeEnumSet(E first, E... rest) {
      allEnums = getEnums(first.getDeclaringClass());
      add(first);
      for (E e : rest) {
        add(e);
      }
    }

    @Override
    public boolean add(E e) {
      if (e == null) {
        throw new NullPointerException("Can't add null to an EnumSet");
      }
      return set.add(e);
    }

    public LargeEnumSet<E> clone() {
      LargeEnumSet<E> newSet = new LargeEnumSet<E>(this.allEnums);
      newSet.set = new HashSet<E>(this.set);
      return newSet;
    }

    @Override
    public boolean contains(Object object) {
      return set.contains(object);
    }

    @Override
    public Iterator<E> iterator() {
      return set.iterator();
    }

    @Override
    public boolean remove(Object o) {
      return set.remove(o);
    }

    @Override
    public int size() {
      return set.size();
    }

    @Override
    void addRange(E from, E to) {
      for (int i = from.ordinal(), end = to.ordinal(); i <= end; ++i) {
        add(allEnums[i]);
      }
    }

    @Override
    void complement() {
      for (int i = 0; i < allEnums.length; ++i) {
        E e = allEnums[i];
        if (set.contains(e)) {
          set.remove(e);
        } else {
          set.add(e);
        }
      }
    }
  }

  /**
   * A fast implementation of EnumSet for enums with less than 32 values. A Java
   * EnumSet can support 63 bits easily with a primitive long, but JavaScript
   * generally represents long values as floating point numbers.
   *
   * LargeEnumSet is used to support enums with > 31 values using a map-backed
   * implementation.
   */
  static class SimpleEnumSet<E extends Enum<E>> extends EnumSet<E> {

    // TODO(tobyr)
    // Consider optimizing this iterator by walking the values using a
    // combination of lowestOneBit and numberOfLeadingZeros.
    // This is low priority as iterating over enums is not the common usecase.
    class SimpleIterator implements Iterator<E> {

      private Iterator<E> iterator;

      private E value;

      SimpleIterator() {
        List<E> values = new ArrayList<E>();
        for (int i = 0; i < 31; ++i) {
          int mask = 0x1 << i;
          if ((enumValues & mask) != 0) {
            values.add(allEnums[i]);
          }
        }

        iterator = values.iterator();
      }

      public boolean hasNext() {
        return iterator.hasNext();
      }

      public E next() {
        value = iterator.next();
        return value;
      }

      public void remove() {
        SimpleEnumSet.this.remove(value);
      }
    }

    private static int ALL_BITS_SET = 0x7FFFFFFF;

    static <E extends Enum<E>> SimpleEnumSet<E> all(Class<E> enumClass) {
      SimpleEnumSet<E> set = new SimpleEnumSet<E>(enumClass);
      set.enumValues = ALL_BITS_SET;
      return set;
    }

    static <E extends Enum<E>> SimpleEnumSet<E> none(Class<E> enumClass) {
      return new SimpleEnumSet<E>(enumClass);
    }

    private int enumValues;

    private E[] allEnums;

    private Class<E> declaringClass;

    // For use only by clone
    private SimpleEnumSet() {
    }

    SimpleEnumSet(Class<E> enumClass) {
      declaringClass = enumClass;
      allEnums = getEnums(declaringClass);
    }

    SimpleEnumSet(E first, E... rest) {
      declaringClass = first.getDeclaringClass();
      allEnums = getEnums(declaringClass);
      add(first);
      for (E e : rest) {
        add(e);
      }
    }

    SimpleEnumSet(List<E> enums) {
      declaringClass = enums.get(0).getDeclaringClass();
      allEnums = getEnums(declaringClass);
      for (E e : enums) {
        add(e);
      }
    }

    @Override
    public boolean add(E o) {
      // Throws NullPointerException according to spec
      int value = 1 << o.ordinal();
      boolean exists = (enumValues & value) != 0;
      enumValues |= value;
      return exists;
    }

    @Override
    public void addRange(E from, E to) {
      for (int i = from.ordinal(), end = to.ordinal(); i <= end; ++i) {
        add(allEnums[i]);
      }
    }

    public SimpleEnumSet<E> clone() {
      SimpleEnumSet<E> set = new SimpleEnumSet<E>();
      set.declaringClass = this.declaringClass;
      set.enumValues = this.enumValues;
      set.allEnums = this.allEnums;
      return set;
    }

    @Override
    public void complement() {
      enumValues ^= ALL_BITS_SET;
    }

    @Override
    public boolean contains(Object obj) {
      return contains(asE(obj));
    }

    @Override
    public Iterator<E> iterator() {
      return new SimpleIterator();
    }

    @Override
    public boolean remove(Object obj) {
      return remove(asE(obj));
    }

    @Override
    public int size() {
      return Integer.bitCount(enumValues);
    }

    boolean contains(E e) {
      if (e == null) {
        return false;
      }
      int value = 1 << e.ordinal();
      return (enumValues & value) != 0;
    }

    boolean remove(E e) {
      if (e == null) {
        return false;
      }

      int value = 1 << e.ordinal();
      boolean exists = (enumValues & value) != 0;
      if (exists) {
        enumValues ^= value;
      }
      return exists;
    }

    /**
     * Returns <code>obj</code> as an E if it is an E. Otherwise, returns null.
     */
    @SuppressWarnings("unchecked")
    private E asE(Object obj) {
      if (!(obj instanceof Enum)) {
        return null;
      }
      Enum e = (Enum) obj;
      return e.getDeclaringClass() == declaringClass ? (E) e : null;
    }
  }

  public static <E extends Enum<E>> EnumSet<E> allOf(Class<E> elementType) {
    E[] enums = getEnums(elementType);

    if (enums.length < 32) {
      return SimpleEnumSet.all(elementType);
    }

    EnumSet<E> largeEnumSet = new LargeEnumSet<E>(enums);
    for (E e : enums) {
      largeEnumSet.add(e);
    }

    return largeEnumSet;
  }

  public static <E extends Enum<E>> EnumSet<E> complementOf(EnumSet<E> s) {
    EnumSet<E> set = copyOf(s);
    set.complement();
    return set;
  }

  public static <E extends Enum<E>> EnumSet<E> copyOf(Collection<E> c) {
    if (c instanceof EnumSet) {
      return EnumSet.copyOf((EnumSet<E>) c);
    }

    Iterator<E> it = c.iterator();
    E first = it.next();
    Class<E> clazz = first.getDeclaringClass();
    EnumSet<E> set = EnumSet.noneOf(clazz);
    set.add(first);

    while (it.hasNext()) {
      set.add(it.next());
    }

    return set;
  }

  public static <E extends Enum<E>> EnumSet<E> copyOf(EnumSet<E> s) {
    return s.clone();
  }

  public static <E extends Enum<E>> EnumSet<E> noneOf(Class<E> elementType) {
    E[] enums = getEnums(elementType);

    if (enums.length < 32) {
      return SimpleEnumSet.none(elementType);
    }
    return new LargeEnumSet<E>(enums);
  }

  public static <E extends Enum<E>> EnumSet<E> of(E first, E... rest) {
    Class<E> c = first.getDeclaringClass();
    EnumSet<E> set = noneOf(c);
    set.add(first);

    for (E e : rest) {
      set.add(e);
    }

    return set;
  }

  public static <E extends Enum<E>> EnumSet<E> range(E from, E to) {
    if (from.compareTo(to) > 0) {
      throw new IllegalArgumentException(from + " > " + to);
    }
    EnumSet<E> set = noneOf(from.getDeclaringClass());
    set.addRange(from, to);
    return set;
  }

  private static <T extends Enum<T>> T[] getEnums(Class<T> clazz) {
    return clazz.getEnumConstants();
  }

  protected EnumSet() {
  }

  public abstract EnumSet<E> clone();

  abstract void addRange(E from, E to);

  abstract void complement();
}
