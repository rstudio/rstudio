/*
 * Copyright 2016 Google Inc.
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
package com.google.gwt.emultest.java8.util;

import com.google.gwt.junit.client.GWTTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Supplier;

/**
 * Tests for Spliterators JRE emulation.
 */
public class SpliteratorsTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  public void testEmptySpliterator() {
    testSpliterator(new Object[0], Spliterators::emptySpliterator, true);
  }

  public void testEmptyDoubleSpliterator() {
    testDoubleSpliterator(new double[0], Spliterators::emptyDoubleSpliterator, true);
  }

  public void testEmptyIntSpliterator() {
    testIntSpliterator(new int[0], Spliterators::emptyIntSpliterator, true);
  }

  public void testEmptyLongSpliterator() {
    testLongSpliterator(new long[0], Spliterators::emptyLongSpliterator, true);
  }

  public void testSpliterator() {
    final String[] original = {"1", "2", "3", "4"};
    testSpliterator(original, () -> Spliterators.spliterator(Arrays.asList(original), 0), true);
    testSpliterator(original, () -> Spliterators.spliterator(Arrays.asList(original).iterator(), original.length, 0), true);
    testSpliterator(original, () -> Spliterators.spliteratorUnknownSize(Arrays.asList(original).iterator(), 0), false);
    testSpliterator(original, () -> Spliterators.spliterator(original, 0), true);
    testSpliterator(Arrays.copyOfRange(original, 1, 3), () -> Spliterators.spliterator(original, 1, 3, 0), true);
  }

  public void testDoubleSpliterator() {
    final double[] original = {1., 2., 3., 4.};
    testDoubleSpliterator(original, () -> Spliterators.spliterator(original, 0), true);
    testDoubleSpliterator(Arrays.copyOfRange(original, 1, 3), () -> Spliterators.spliterator(original, 1, 3, 0), true);
    testDoubleSpliterator(original, () -> Spliterators.spliterator(createPrimitiveDoubleIterator(original), original.length, 0), true);
  }

  public void testIntSpliterator() {
    final int[] original = {1, 2, 3, 4};
    testIntSpliterator(original, () -> Spliterators.spliterator(original, 0), true);
    testIntSpliterator(Arrays.copyOfRange(original, 1, 3), () -> Spliterators.spliterator(original, 1, 3, 0), true);
    testIntSpliterator(original, () -> Spliterators.spliterator(createPrimitiveIntIterator(original), original.length, 0), true);
  }

  public void testLongSpliterator() {
    final long[] original = {1, 2, 3, 4};
    testLongSpliterator(original, () -> Spliterators.spliterator(original, 0), true);
    testLongSpliterator(Arrays.copyOfRange(original, 1, 3), () -> Spliterators.spliterator(original, 1, 3, 0), true);
    testLongSpliterator(original, () -> Spliterators.spliterator(createPrimitiveLongIterator(original), original.length, 0), true);
  }

  public void testIterator() {
    final String[] original = {"1", "2", "3", "4"};
    Spliterator<String> spliterator = Spliterators.spliterator(Arrays.asList(original), 0);
    Iterator<String> it = Spliterators.iterator(spliterator);

    try {
      it.remove();
      fail();
    } catch (UnsupportedOperationException expected) {
    }

    Deque<String> values = new LinkedList<>(Arrays.asList(original));
    it.forEachRemaining(value -> assertEquals(values.pop(), value));
    assertEquals(0, values.size());
  }

  public void testDoubleIterator() {
    final double[] original = {1., 2., 3., 4.};
    Spliterator.OfDouble spliterator = Spliterators.spliterator(original, 0);
    PrimitiveIterator.OfDouble it = Spliterators.iterator(spliterator);

    try {
      it.remove();
      fail();
    } catch (UnsupportedOperationException expected) {
    }

    Deque<Double> values = new LinkedList<>(toDoubleCollection(original));
    it.forEachRemaining((double value) -> assertEquals((double) values.pop(), value));
    assertEquals(0, values.size());
  }

  public void testIntIterator() {
    final int[] original = {1, 2, 3, 4};
    Spliterator.OfInt spliterator = Spliterators.spliterator(original, 0);
    PrimitiveIterator.OfInt it = Spliterators.iterator(spliterator);

    try {
      it.remove();
      fail();
    } catch (UnsupportedOperationException expected) {
    }

    Deque<Integer> values = new LinkedList<>(toIntCollection(original));
    it.forEachRemaining((int value) -> assertEquals((int) values.pop(), value));
    assertEquals(0, values.size());
  }

  public void testLongIterator() {
    final long[] original = {1, 2, 3, 4};
    Spliterator.OfLong spliterator = Spliterators.spliterator(original, 0);
    PrimitiveIterator.OfLong it = Spliterators.iterator(spliterator);

    try {
      it.remove();
      fail();
    } catch (UnsupportedOperationException expected) {
    }

    Deque<Long> values = new LinkedList<>(toLongCollection(original));
    it.forEachRemaining((long value) -> assertEquals((long) values.pop(), value));
    assertEquals(0, values.size());
  }

  private <T> void testSpliterator(T[] original, Supplier<Spliterator<T>> supplier, boolean sizeKnown) {
    Spliterator<T> spliterator = supplier.get();
    if (sizeKnown) {
      assertEquals(original.length, spliterator.estimateSize());
      assertEquals(original.length, spliterator.getExactSizeIfKnown());
    } else {
      assertEquals(Long.MAX_VALUE, spliterator.estimateSize());
      assertEquals(-1L, spliterator.getExactSizeIfKnown());
    }

    Deque<T> values = new LinkedList<>(Arrays.asList(original));
    spliterator.forEachRemaining(value -> assertEquals(values.pop(), value));
    spliterator.forEachRemaining(value -> fail());
    assertEquals(0, values.size());

    spliterator = supplier.get();
    for (T originalValue : original) {
      assertTrue(spliterator.tryAdvance(value -> assertEquals(originalValue, value)));
    }
    assertFalse(spliterator.tryAdvance(value -> fail()));
  }

  private void testDoubleSpliterator(double[] original, Supplier<Spliterator.OfDouble> supplier, boolean sizeKnown) {
    Spliterator.OfDouble spliterator = supplier.get();
    if (sizeKnown) {
      assertEquals(original.length, spliterator.estimateSize());
      assertEquals(original.length, spliterator.getExactSizeIfKnown());
    } else {
      assertEquals(Long.MAX_VALUE, spliterator.estimateSize());
      assertEquals(-1L, spliterator.getExactSizeIfKnown());
    }

    Deque<Double> values = new LinkedList<>(toDoubleCollection(original));
    spliterator.forEachRemaining((double value) -> assertEquals((double) values.pop(), value));
    spliterator.forEachRemaining((double value) -> fail());
    assertEquals(0, values.size());

    spliterator = supplier.get();
    for (double originalValue : original) {
      assertTrue(spliterator.tryAdvance((double value) -> assertEquals(originalValue, value)));
    }
    assertFalse(spliterator.tryAdvance((double value) -> fail()));
  }

  private void testIntSpliterator(int[] original, Supplier<Spliterator.OfInt> supplier, boolean sizeKnown) {
    Spliterator.OfInt spliterator = supplier.get();
    if (sizeKnown) {
      assertEquals(original.length, spliterator.estimateSize());
      assertEquals(original.length, spliterator.getExactSizeIfKnown());
    } else {
      assertEquals(Long.MAX_VALUE, spliterator.estimateSize());
      assertEquals(-1L, spliterator.getExactSizeIfKnown());
    }

    Deque<Integer> values = new LinkedList<>(toIntCollection(original));
    spliterator.forEachRemaining((int value) -> assertEquals((int) values.pop(), value));
    spliterator.forEachRemaining((int value) -> fail());
    assertEquals(0, values.size());

    spliterator = supplier.get();
    for (int originalValue : original) {
      assertTrue(spliterator.tryAdvance((int value) -> assertEquals(originalValue, value)));
    }
    assertFalse(spliterator.tryAdvance((int value) -> fail()));
  }

  private void testLongSpliterator(long[] original, Supplier<Spliterator.OfLong> supplier, boolean sizeKnown) {
    Spliterator.OfLong spliterator = supplier.get();
    if (sizeKnown) {
      assertEquals(original.length, spliterator.estimateSize());
      assertEquals(original.length, spliterator.getExactSizeIfKnown());
    } else {
      assertEquals(Long.MAX_VALUE, spliterator.estimateSize());
      assertEquals(-1L, spliterator.getExactSizeIfKnown());
    }

    Deque<Long> values = new LinkedList<>(toLongCollection(original));
    spliterator.forEachRemaining((long value) -> assertEquals((long) values.pop(), value));
    spliterator.forEachRemaining((long value) -> fail());
    assertEquals(0, values.size());

    spliterator = supplier.get();
    for (long originalValue : original) {
      assertTrue(spliterator.tryAdvance((long value) -> assertEquals(originalValue, value)));
    }
    assertFalse(spliterator.tryAdvance((long value) -> fail()));
  }

  private static Collection<Double> toDoubleCollection(double[] values) {
    ArrayList<Double> c = new ArrayList<>();
    for (double value : values) {
      c.add(value);
    }
    return c;
  }

  private static Collection<Integer> toIntCollection(int[] values) {
    ArrayList<Integer> c = new ArrayList<>();
    for (int value : values) {
      c.add(value);
    }
    return c;
  }

  private static Collection<Long> toLongCollection(long[] values) {
    ArrayList<Long> c = new ArrayList<>();
    for (long value : values) {
      c.add(value);
    }
    return c;
  }

  private static PrimitiveIterator.OfDouble createPrimitiveDoubleIterator(double[] values) {
    final Iterator<Double> it = toDoubleCollection(values).iterator();
    return new PrimitiveIterator.OfDouble() {
      @Override
      public double nextDouble() {
        return it.next();
      }

      @Override
      public boolean hasNext() {
        return it.hasNext();
      }
    };
  }

  private static PrimitiveIterator.OfInt createPrimitiveIntIterator(int[] values) {
    final Iterator<Integer> it = toIntCollection(values).iterator();
    return new PrimitiveIterator.OfInt() {
      @Override
      public int nextInt() {
        return it.next();
      }

      @Override
      public boolean hasNext() {
        return it.hasNext();
      }
    };
  }

  private static PrimitiveIterator.OfLong createPrimitiveLongIterator(long[] values) {
    final Iterator<Long> it = toLongCollection(values).iterator();
    return new PrimitiveIterator.OfLong() {
      @Override
      public long nextLong() {
        return it.next();
      }

      @Override
      public boolean hasNext() {
        return it.hasNext();
      }
    };
  }

}
