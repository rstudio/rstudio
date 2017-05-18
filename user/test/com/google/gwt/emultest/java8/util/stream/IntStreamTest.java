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

package com.google.gwt.emultest.java8.util.stream;

import com.google.gwt.emultest.java.util.EmulTestBase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IntSummaryStatistics;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;
import java.util.Spliterator;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * Tests {@link IntStream}.
 */
public class IntStreamTest extends EmulTestBase {

  public void testEmptyStream() {
    IntStream empty = IntStream.empty();
    assertEquals(0, empty.count());
    try {
      empty.count();
      fail("second terminal operation should have thrown IllegalStateEx");
    } catch (IllegalStateException expected) {
      // expected
    }

    assertEquals(0, IntStream.empty().limit(2).toArray().length);
    assertEquals(0L, IntStream.empty().count());
    assertEquals(0L, IntStream.empty().limit(2).count());

    assertFalse(IntStream.empty().findFirst().isPresent());
    assertFalse(IntStream.empty().findAny().isPresent());
    assertFalse(IntStream.empty().max().isPresent());
    assertFalse(IntStream.empty().min().isPresent());
    assertTrue(IntStream.empty().noneMatch(item -> true));
    assertTrue(IntStream.empty().allMatch(item -> true));
    assertFalse(IntStream.empty().anyMatch(item -> true));
    assertEquals(new int[0], IntStream.empty().toArray());
  }

  public void testStreamOfOne() {
    Supplier<IntStream> one = () -> IntStream.of(1);
    assertEquals(new int[] {1}, one.get().toArray());
    assertEquals(1L, one.get().count());
    assertEquals(1, one.get().findFirst().getAsInt());
    assertEquals(1, one.get().findAny().getAsInt());
  }

  public void testBuilder() {
    IntStream s = IntStream.builder().add(1).add(3).add(2).build();

    assertEquals(new int[] {1, 3, 2}, s.toArray());

    IntStream.Builder builder = IntStream.builder();
    IntStream built = builder.build();
    assertEquals(0, built.count());
    try {
      builder.build();
      fail("build() after build() should fail");
    } catch (IllegalStateException expected) {
      // expected
    }
    try {
      builder.add(10);
      fail("add() after build() should fail");
    } catch (IllegalStateException expected) {
      // expected
    }
  }

  public void testConcat() {
    Supplier<IntStream> adbc = () -> IntStream.concat(IntStream.of(1, 4), IntStream.of(2, 3));

    assertEquals(new int[] {1, 4, 2, 3}, adbc.get().toArray());
    assertEquals(new int[] {1, 2, 3, 4}, adbc.get().sorted().toArray());

    List<String> closed = new ArrayList<>();
    IntStream first = IntStream.of(1).onClose(() -> closed.add("first"));
    IntStream second = IntStream.of(2).onClose(() -> closed.add("second"));

    IntStream concat = IntStream.concat(first, second);

    // read everything, make sure we saw it all and didn't close automatically
    int collectedAll = concat.sum();
    assertEquals(3, collectedAll);
    assertEquals(0, closed.size());

    concat.close();
    assertEquals(Arrays.asList("first", "second"), closed);
  }

  public void testIterate() {
    assertEquals(
        new int[] {10, 11, 12, 13, 14},
        IntStream.iterate(0, i -> i + 1).skip(10).limit(5).toArray());
  }

  public void testGenerate() {
    // infinite, but if you limit it is already too short to skip much
    assertEquals(new int[0], IntStream.generate(makeGenerator()).limit(4).skip(5).toArray());

    assertEquals(
        new int[] {10, 11, 12, 13, 14},
        IntStream.generate(makeGenerator()).skip(10).limit(5).toArray());
  }

  private IntSupplier makeGenerator() {
    return new IntSupplier() {
      int next = 0;

      @Override
      public int getAsInt() {
        return next++;
      }
    };
  }

  public void testRange() {
    assertEquals(new int[] {1, 2, 3, 4}, IntStream.range(1, 5).toArray());
    assertEquals(new int[] {-1, 0, 1, 2, 3, 4}, IntStream.range(-1, 5).toArray());
    assertEquals(new int[] {}, IntStream.range(1, -5).toArray());
    assertEquals(new int[] {}, IntStream.range(-1, -5).toArray());
  }

  public void testRangeClosed() {
    assertEquals(new int[] {1, 2, 3, 4, 5}, IntStream.rangeClosed(1, 5).toArray());
    assertEquals(new int[] {-1, 0, 1, 2, 3, 4, 5}, IntStream.rangeClosed(-1, 5).toArray());
    assertEquals(new int[] {}, IntStream.rangeClosed(1, -5).toArray());
    assertEquals(new int[] {}, IntStream.rangeClosed(-1, -5).toArray());
  }

  public void testToArray() {
    assertEquals(new int[0], IntStream.of().toArray());
    assertEquals(new int[] {1}, IntStream.of(1).toArray());
    assertEquals(new int[] {3, 2, 0}, IntStream.of(3, 2, 0).toArray());
  }

  public void testReduce() {
    int reduced = IntStream.of(1, 2, 4).reduce(0, Integer::sum);
    assertEquals(7, reduced);

    reduced = IntStream.of().reduce(0, Integer::sum);
    assertEquals(0, reduced);

    OptionalInt maybe = IntStream.of(1, 4, 8).reduce(Integer::sum);
    assertTrue(maybe.isPresent());
    assertEquals(13, maybe.getAsInt());
    maybe = IntStream.of().reduce(Integer::sum);
    assertFalse(maybe.isPresent());
  }

  public void testFilter() {
    // unconsumed stream never runs filter
    boolean[] data = {false};
    IntStream.of(1, 2, 3).filter(i -> data[0] |= true);
    assertFalse(data[0]);

    // Nothing's *defined* to care about the Spliterator characteristics, but the implementation
    // can't actually know the size before executing, so we check the characteristics explicitly.
    assertFalse(
        IntStream.of(1, 2, 3)
            .filter(a -> a == 1)
            .spliterator()
            .hasCharacteristics(Spliterator.SIZED | Spliterator.SUBSIZED));

    // one result
    assertEquals(new int[] {1}, IntStream.of(1, 2, 3, 4, 3).filter(a -> a == 1).toArray());
    // zero results
    assertEquals(new int[0], IntStream.of(1, 2, 3, 4, 3).filter(a -> false).toArray());
    // two results
    assertEquals(new int[] {2, 4}, IntStream.of(1, 2, 3, 4, 3).filter(a -> a % 2 == 0).toArray());
    // all
    assertEquals(
        new int[] {1, 2, 3, 4, 3}, IntStream.of(1, 2, 3, 4, 3).filter(a -> true).toArray());
  }

  public void testMap() {
    // unconsumed stream never runs map
    int[] data = {0};
    IntStream.of(1, 2, 3).map(i -> data[0]++);
    assertEquals(0, data[0]);

    assertEquals(new int[] {2, 4, 6}, IntStream.of(1, 2, 3).map(i -> i * 2).toArray());
  }

  public void testPeek() {
    // unconsumed stream never peeks
    boolean[] data = {false};
    IntStream.of(1, 2, 3).peek(i -> data[0] |= true);
    assertFalse(data[0]);

    // make sure we saw it all in order
    int[] items = new int[] {1, 2, 3};
    List<Integer> peeked = new ArrayList<>();
    IntStream.of(items)
        .peek(peeked::add)
        .forEach(
            item -> {
              // do nothing, just run
            });
    assertEquals(items.length, peeked.size());
    for (int i = 0; i < items.length; i++) {
      assertEquals(items[i], (int) peeked.get(i));
    }
  }

  // same impl, no parallel in browser
  public void testFindFirstOrAny() {
    OptionalInt any = IntStream.of(1, 2).findAny();
    assertTrue(any.isPresent());
    assertEquals(1, any.getAsInt());
  }

  public void testAnyMatch() {
    // all
    assertTrue(IntStream.of(1, 2).anyMatch(s -> true));

    // some
    assertTrue(IntStream.of(1, 2).anyMatch(s -> s == 1));

    // none
    assertFalse(IntStream.of(1, 2).anyMatch(s -> false));
  }

  public void testAllMatch() {
    // all
    assertTrue(IntStream.of(1, 2).allMatch(s -> true));

    // some
    assertFalse(IntStream.of(1, 2).allMatch(s -> s == 1));

    // none
    assertFalse(IntStream.of(1, 2).allMatch(s -> false));
  }

  public void testNoneMatch() {
    // all
    assertFalse(IntStream.of(1, 2).noneMatch(s -> true));

    // some
    assertFalse(IntStream.of(1, 2).noneMatch(s -> s == 1));

    // none
    assertTrue(IntStream.of(1, 2).noneMatch(s -> false));
  }

  public void testFlatMap() {
    assertEquals(0, IntStream.empty().flatMap(value -> IntStream.of(1)).count());
    assertEquals(0, IntStream.of(1).flatMap(value -> IntStream.empty()).count());
    assertEquals(0, IntStream.of(1).flatMap(value -> IntStream.of()).count());
    assertEquals(0, IntStream.of().flatMap(value -> IntStream.of(1)).count());
    assertEquals(1, IntStream.of(1).flatMap(value -> IntStream.of(1)).count());

    IntStream values = IntStream.of(1, 2, 3);

    assertEquals(
        new int[] {1, 2, 2, 4, 3, 6}, values.flatMap(i -> IntStream.of(i, i * 2)).toArray());
  }

  public void testMapToOthers() {
    Supplier<IntStream> s = () -> IntStream.of(1, 2, 10);

    assertEquals(
        new String[] {"1", "2", "10"}, s.get().mapToObj(String::valueOf).toArray(String[]::new));

    assertEquals(new long[] {1, 2, 10}, s.get().mapToLong(i -> (long) i).toArray());

    assertEquals(new double[] {1, 2, 10}, s.get().mapToDouble(i -> (double) i).toArray());
  }

  public void testDistinct() {
    int[] distinct = IntStream.of(1, 2, 3, 2).distinct().toArray();
    assertEquals(3, distinct.length);
    assertEquals(1 + 2 + 3, distinct[0] + distinct[1] + distinct[2]);
  }

  public void testSorted() {
    int[] sorted = IntStream.of(3, 1, 2).sorted().toArray();
    assertEquals(new int[] {1, 2, 3}, sorted);
  }

  public void testMinMax() {
    Supplier<IntStream> stream = () -> IntStream.of(2, 3, 4, 1);

    assertEquals(1, stream.get().min().orElse(0));
    assertEquals(4, stream.get().max().orElse(0));

    assertFalse(stream.get().filter(a -> false).max().isPresent());
    assertFalse(stream.get().filter(a -> false).min().isPresent());
  }

  public void testCountLimitSkip() {
    Supplier<IntStream> stream = () -> IntStream.of(1, 2, 3, 4);

    assertEquals(4, stream.get().count());

    assertEquals(4, stream.get().limit(4).count());
    assertEquals(4, stream.get().limit(5).count());
    assertEquals(3, stream.get().limit(3).count());

    assertEquals(3, stream.get().skip(1).limit(3).count());

    assertEquals(2, stream.get().limit(3).skip(1).count());

    assertEquals(1, stream.get().skip(3).count());

    assertEquals(new int[] {3, 4}, stream.get().skip(2).limit(3).toArray());
    assertEquals(new int[] {3}, stream.get().skip(2).limit(1).toArray());

    assertEquals(new int[] {4}, stream.get().skip(3).toArray());
    assertEquals(new int[] {}, stream.get().skip(5).toArray());

    assertEquals(new int[] {1, 2}, stream.get().limit(2).toArray());

    assertEquals(new int[] {2}, stream.get().limit(2).skip(1).toArray());
  }

  public void testBoxed() {
    Supplier<IntStream> stream = () -> IntStream.of(1, 2);
    Stream<Integer> expected = stream.get().mapToObj(Integer::valueOf);
    assertEquals(expected.toArray(), stream.get().boxed().toArray());
  }

  public void testAsOtherPrimitive() {
    Supplier<IntStream> stream = () -> IntStream.of(1, 2);

    DoubleStream actualDoubleStream = stream.get().asDoubleStream();
    assertEquals(new double[] {1, 2}, actualDoubleStream.toArray());

    LongStream actualLongStream = stream.get().asLongStream();
    assertEquals(new long[] {1, 2}, actualLongStream.toArray());
  }

  public void testSummaryStats() {
    Supplier<IntStream> stream = () -> IntStream.of(1, 2, 3);
    IntSummaryStatistics summaryStats = stream.get().summaryStatistics();
    assertEquals(3, summaryStats.getCount());
    assertEquals(1, summaryStats.getMin());
    assertEquals(2, summaryStats.getAverage(), 0d);
    assertEquals(3, summaryStats.getMax());
    assertEquals(6, summaryStats.getSum());

    summaryStats.accept(6);
    assertEquals(4, summaryStats.getCount());
    assertEquals(1, summaryStats.getMin());
    assertEquals(3, summaryStats.getAverage(), 0d);
    assertEquals(6, summaryStats.getMax());
    assertEquals(12, summaryStats.getSum());

    IntSummaryStatistics combinedSumStats = stream.get().summaryStatistics();
    combinedSumStats.combine(IntStream.of(4, 5, 6, 0).summaryStatistics());
    assertEquals(7, combinedSumStats.getCount());
    assertEquals(0, combinedSumStats.getMin());
    assertEquals(3, combinedSumStats.getAverage(), 0d);
    assertEquals(6, combinedSumStats.getMax());
    assertEquals(21, combinedSumStats.getSum());
  }

  public void testAverage() {
    assertFalse(IntStream.empty().average().isPresent());
    assertEquals(2.0d, IntStream.of(1, 2, 3).average().getAsDouble(), 0d);
    assertEquals(0d, IntStream.of(1, 2, -3).average().getAsDouble(), 0d);
    assertEquals(-2.0d, IntStream.of(-1, -2, -3).average().getAsDouble(), 0d);
  }

  public void testSum() {
    assertEquals(6, IntStream.of(1, 2, 3).sum());
    assertEquals(0, IntStream.of(1, 2, -3).sum());
    assertEquals(-6, IntStream.of(-1, -2, -3).sum());
  }

  public void testCollect() {
    String val =
        IntStream.of(1, 2, 3, 4, 5)
            .collect(
                StringBuilder::new,
                // TODO switch to a lambda reference once #9340 is fixed
                (stringBuilder, i) -> stringBuilder.append(i),
                StringBuilder::append)
            .toString();

    assertEquals("12345", val);
  }

  public void testForEach() {
    List<Integer> vals = new ArrayList<>();
    IntStream.of(1, 2, 3, 4, 5).forEach(vals::add);
    assertEquals(5, vals.size());
    assertEquals(new Integer[] {1, 2, 3, 4, 5}, vals.toArray(new Integer[vals.size()]));
  }

  public void testIterator() {
    List<Integer> vals = new ArrayList<>();
    Iterator<Integer> iterator = IntStream.of(1, 2, 3, 4, 5).iterator();
    while (iterator.hasNext()) {
      vals.add(iterator.next());
    }
    assertEquals(5, vals.size());
    assertEquals(new Integer[] {1, 2, 3, 4, 5}, vals.toArray(new Integer[vals.size()]));
  }

  public void testSpliterator() {
    Spliterator<Integer> spliterator = IntStream.of(1, 2, 3, 4, 5).spliterator();
    assertEquals(5, spliterator.estimateSize());
    assertEquals(5, spliterator.getExactSizeIfKnown());

    List<Integer> vals = new ArrayList<>();
    while (spliterator.tryAdvance(vals::add)) {
      // work is all done in the condition
    }

    assertEquals(5, vals.size());
    assertEquals(new Integer[] {1, 2, 3, 4, 5}, vals.toArray(new Integer[vals.size()]));
  }
}
