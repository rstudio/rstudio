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
import java.util.Iterator;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.OptionalLong;
import java.util.Spliterator;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * Tests {@link LongStream}.
 */
public class LongStreamTest extends EmulTestBase {

  public void testEmptyStream() {
    LongStream empty = LongStream.empty();
    assertEquals(0, empty.count());
    try {
      empty.count();
      fail("second terminal operation should have thrown IllegalStateEx");
    } catch (IllegalStateException expected) {
      // expected
    }

    assertEquals(0, LongStream.empty().limit(2).toArray().length);
    assertEquals(0L, LongStream.empty().count());
    assertEquals(0L, LongStream.empty().limit(2).count());

    assertFalse(LongStream.empty().findFirst().isPresent());
    assertFalse(LongStream.empty().findAny().isPresent());
    assertFalse(LongStream.empty().max().isPresent());
    assertFalse(LongStream.empty().min().isPresent());
    assertTrue(LongStream.empty().noneMatch(item -> true));
    assertTrue(LongStream.empty().allMatch(item -> true));
    assertFalse(LongStream.empty().anyMatch(item -> true));
    assertEquals(new long[0], LongStream.empty().toArray());
  }

  public void testStreamOfOne() {
    Supplier<LongStream> one = () -> LongStream.of(1);
    assertEquals(new long[] {1L}, one.get().toArray());
    assertEquals(1L, one.get().count());
    assertEquals(1, one.get().findFirst().getAsLong());
    assertEquals(1, one.get().findAny().getAsLong());
  }

  public void testBuilder() {
    LongStream s = LongStream.builder().add(1L).add(3L).add(2L).build();

    assertEquals(new long[] {1L, 3L, 2L}, s.toArray());

    LongStream.Builder builder = LongStream.builder();
    LongStream built = builder.build();
    assertEquals(0L, built.count());
    try {
      builder.build();
      fail("build() after build() should fail");
    } catch (IllegalStateException expected) {
      // expected
    }
    try {
      builder.add(10L);
      fail("add() after build() should fail");
    } catch (IllegalStateException expected) {
      // expected
    }
  }

  public void testConcat() {
    Supplier<LongStream> adbc = () -> LongStream.concat(LongStream.of(1, 4), LongStream.of(2, 3));

    assertEquals(new long[] {1L, 4L, 2L, 3L}, adbc.get().toArray());
    assertEquals(new long[] {1L, 2L, 3L, 4L}, adbc.get().sorted().toArray());

    List<String> closed = new ArrayList<>();
    LongStream first = LongStream.of(1L).onClose(() -> closed.add("first"));
    LongStream second = LongStream.of(2L).onClose(() -> closed.add("second"));

    LongStream concat = LongStream.concat(first, second);

    // read everything, make sure we saw it all and didn't close automatically
    long collectedAll = concat.sum();
    assertEquals(3L, collectedAll);
    assertEquals(0, closed.size());

    concat.close();
    assertEquals(Arrays.asList("first", "second"), closed);
  }

  public void testIterate() {
    assertEquals(
        new long[] {10L, 11L, 12L, 13L, 14L},
        LongStream.iterate(0L, l -> l + 1L).skip(10).limit(5).toArray());
  }

  public void testGenerate() {
    // infinite, but if you limit it is already too short to skip much
    assertEquals(new long[0], LongStream.generate(makeGenerator()).limit(4).skip(5).toArray());

    assertEquals(
        new long[] {10L, 11L, 12L, 13L, 14L},
        LongStream.generate(makeGenerator()).skip(10).limit(5).toArray());
  }

  private LongSupplier makeGenerator() {
    return new LongSupplier() {
      long next = 0L;

      @Override
      public long getAsLong() {
        return next++;
      }
    };
  }

  public void testRange() {
    assertEquals(new long[] {1L, 2L, 3L, 4L}, LongStream.range(1, 5).toArray());
    assertEquals(new long[] {-1L, 0L, 1L, 2L, 3L, 4L}, LongStream.range(-1, 5).toArray());
    assertEquals(new long[] {}, LongStream.range(1, -5).toArray());
    assertEquals(new long[] {}, LongStream.range(-1, -5).toArray());
  }

  public void testRangeClosed() {
    assertEquals(new long[] {1L, 2L, 3L, 4L, 5L}, LongStream.rangeClosed(1, 5).toArray());
    assertEquals(new long[] {-1L, 0L, 1L, 2L, 3L, 4L, 5L}, LongStream.rangeClosed(-1, 5).toArray());
    assertEquals(new long[] {}, LongStream.rangeClosed(1, -5).toArray());
    assertEquals(new long[] {}, LongStream.rangeClosed(-1, -5).toArray());
  }

  public void testToArray() {
    assertEquals(new long[0], LongStream.of().toArray());
    assertEquals(new long[] {1L}, LongStream.of(1L).toArray());
    assertEquals(new long[] {3L, 2L, 0L}, LongStream.of(3L, 2L, 0L).toArray());
  }

  public void testReduce() {
    long reduced = LongStream.of(1L, 2L, 4L).reduce(0, Long::sum);
    assertEquals(7, reduced);

    reduced = LongStream.of().reduce(0, Long::sum);
    assertEquals(0L, reduced);

    OptionalLong maybe = LongStream.of(1L, 4L, 8L).reduce(Long::sum);
    assertTrue(maybe.isPresent());
    assertEquals(13L, maybe.getAsLong());
    maybe = LongStream.of().reduce(Long::sum);
    assertFalse(maybe.isPresent());
  }

  public void testFilter() {
    // unconsumed stream never runs filter
    boolean[] data = {false};
    LongStream.of(1L, 2L, 3L).filter(i -> data[0] |= true);
    assertFalse(data[0]);

    // Nothing's *defined* to care about the Spliterator characteristics, but the implementation
    // can't actually know the size before executing, so we check the characteristics explicitly.
    assertFalse(
        LongStream.of(1L, 2L, 3L)
            .filter(a -> a == 1L)
            .spliterator()
            .hasCharacteristics(Spliterator.SIZED | Spliterator.SUBSIZED));

    // one result
    assertEquals(new long[] {1L}, LongStream.of(1L, 2L, 3L, 4L, 3L).filter(a -> a == 1).toArray());
    // zero results
    assertEquals(new long[0], LongStream.of(1L, 2L, 3L, 4L, 3L).filter(a -> false).toArray());
    // two results
    assertEquals(
        new long[] {2L, 4L}, LongStream.of(1L, 2L, 3L, 4L, 3L).filter(a -> a % 2 == 0).toArray());
    // all
    assertEquals(
        new long[] {1L, 2L, 3L, 4L, 3L},
        LongStream.of(1L, 2L, 3L, 4L, 3L).filter(a -> true).toArray());
  }

  public void testMap() {
    // unconsumed stream never runs map
    int[] data = {0};
    LongStream.of(1L, 2L, 3L).map(i -> data[0]++);
    assertEquals(0, data[0]);

    assertEquals(new long[] {2L, 4L, 6L}, LongStream.of(1L, 2L, 3L).map(i -> i * 2).toArray());
  }

  public void testPeek() {
    // unconsumed stream never peeks
    boolean[] data = {false};
    LongStream.of(1L, 2L, 3L).peek(i -> data[0] |= true);
    assertFalse(data[0]);

    // make sure we saw it all in order
    long[] items = new long[] {1L, 2L, 3L};
    List<Long> peeked = new ArrayList<>();
    LongStream.of(items)
        .peek(peeked::add)
        .forEach(
            item -> {
              // do nothing, just run
            });
    assertEquals(items.length, peeked.size());
    for (int i = 0; i < items.length; i++) {
      assertEquals(items[i], (long) peeked.get(i));
    }
  }

  // same impl, no parallel in browser
  public void testFindFirstOrAny() {
    OptionalLong any = LongStream.of(1L, 2L).findAny();
    assertTrue(any.isPresent());
    assertEquals(1L, any.getAsLong());
  }

  public void testAnyMatch() {
    // all
    assertTrue(LongStream.of(1L, 2L).anyMatch(s -> true));

    // some
    assertTrue(LongStream.of(1L, 2L).anyMatch(s -> s == 1L));

    // none
    assertFalse(LongStream.of(1L, 2L).anyMatch(s -> false));
  }

  public void testAllMatch() {
    // all
    assertTrue(LongStream.of(1L, 2L).allMatch(s -> true));

    // some
    assertFalse(LongStream.of(1L, 2L).allMatch(s -> s == 1L));

    // none
    assertFalse(LongStream.of(1L, 2L).allMatch(s -> false));
  }

  public void testNoneMatch() {
    // all
    assertFalse(LongStream.of(1L, 2L).noneMatch(s -> true));

    // some
    assertFalse(LongStream.of(1L, 2L).noneMatch(s -> s == 1L));

    // none
    assertTrue(LongStream.of(1L, 2L).noneMatch(s -> false));
  }

  public void testFlatMap() {
    assertEquals(0L, LongStream.empty().flatMap(value -> LongStream.of(1L)).count());
    assertEquals(0L, LongStream.of(1L).flatMap(value -> LongStream.empty()).count());
    assertEquals(0L, LongStream.of(1L).flatMap(value -> LongStream.of()).count());
    assertEquals(0L, LongStream.of().flatMap(value -> LongStream.of(1L)).count());
    assertEquals(1L, LongStream.of(1L).flatMap(value -> LongStream.of(1L)).count());

    LongStream values = LongStream.of(1L, 2L, 3L);

    assertEquals(
        new long[] {1L, 2L, 2L, 4L, 3L, 6L},
        values.flatMap(i -> LongStream.of(i, i * 2)).toArray());
  }

  public void testMapToOthers() {
    Supplier<LongStream> s = () -> LongStream.of(1, 2, 10);

    assertEquals(
        new String[] {"1", "2", "10"}, s.get().mapToObj(String::valueOf).toArray(String[]::new));

    assertEquals(new int[] {1, 2, 10}, s.get().mapToInt(i -> (int) i).toArray());

    assertEquals(new double[] {1d, 2d, 10d}, s.get().mapToDouble(i -> (double) i).toArray());
  }

  public void testDistinct() {
    long[] distinct = LongStream.of(1L, 2L, 3L, 2L).distinct().toArray();
    assertEquals(3, distinct.length);
    assertEquals(1L + 2L + 3L, distinct[0] + distinct[1] + distinct[2]);
  }

  public void testSorted() {
    long[] sorted = LongStream.of(3L, 1L, 2L).sorted().toArray();
    assertEquals(new long[] {1L, 2L, 3L}, sorted);
  }

  public void testMinMax() {
    Supplier<LongStream> stream = () -> LongStream.of(2L, 3L, 4L, 1L);

    assertEquals(1L, stream.get().min().orElse(0));
    assertEquals(4L, stream.get().max().orElse(0));

    assertFalse(stream.get().filter(a -> false).max().isPresent());
    assertFalse(stream.get().filter(a -> false).min().isPresent());
  }

  public void testCountLimitSkip() {
    Supplier<LongStream> stream = () -> LongStream.of(1L, 2L, 3L, 4L);

    assertEquals(4L, stream.get().count());

    assertEquals(4L, stream.get().limit(4).count());
    assertEquals(4L, stream.get().limit(5).count());
    assertEquals(3L, stream.get().limit(3).count());

    assertEquals(3L, stream.get().skip(1).limit(3).count());

    assertEquals(2L, stream.get().limit(3).skip(1).count());

    assertEquals(1L, stream.get().skip(3).count());

    assertEquals(new long[] {3L, 4L}, stream.get().skip(2).limit(3).toArray());
    assertEquals(new long[] {3L}, stream.get().skip(2).limit(1).toArray());

    assertEquals(new long[] {4L}, stream.get().skip(3).toArray());
    assertEquals(new long[] {}, stream.get().skip(5).toArray());

    assertEquals(new long[] {1L, 2L}, stream.get().limit(2).toArray());

    assertEquals(new long[] {2L}, stream.get().limit(2).skip(1).toArray());
  }

  public void testBoxed() {
    Supplier<LongStream> stream = () -> LongStream.of(1L, 2L);
    Stream<Long> expected = stream.get().mapToObj(Long::valueOf);
    assertEquals(expected.toArray(), stream.get().boxed().toArray());
  }

  public void testAsOtherPrimitive() {
    Supplier<LongStream> stream = () -> LongStream.of(1L, 2L);
    DoubleStream actualDoubleStream = stream.get().asDoubleStream();
    assertEquals(new double[] {1, 2}, actualDoubleStream.toArray());
  }

  public void testSummaryStats() {
    Supplier<LongStream> stream = () -> LongStream.of(1L, 2L, 3L);
    LongSummaryStatistics summaryStats = stream.get().summaryStatistics();
    assertEquals(3L, summaryStats.getCount());
    assertEquals(1L, summaryStats.getMin());
    assertEquals(2L, summaryStats.getAverage(), 0d);
    assertEquals(3L, summaryStats.getMax());
    assertEquals(6L, summaryStats.getSum());

    summaryStats.accept(6L);
    assertEquals(4L, summaryStats.getCount());
    assertEquals(1L, summaryStats.getMin());
    assertEquals(3L, summaryStats.getAverage(), 0d);
    assertEquals(6L, summaryStats.getMax());
    assertEquals(12L, summaryStats.getSum());

    LongSummaryStatistics combinedSumStats = stream.get().summaryStatistics();
    combinedSumStats.combine(LongStream.of(4L, 5L, 6L, 0L).summaryStatistics());
    assertEquals(7L, combinedSumStats.getCount());
    assertEquals(0L, combinedSumStats.getMin());
    assertEquals(3L, combinedSumStats.getAverage(), 0d);
    assertEquals(6L, combinedSumStats.getMax());
    assertEquals(21L, combinedSumStats.getSum());
  }

  public void testAverage() {
    assertFalse(LongStream.empty().average().isPresent());
    assertEquals(2.0d, LongStream.of(1L, 2L, 3L).average().getAsDouble(), 0d);
    assertEquals(0d, LongStream.of(1L, 2L, -3L).average().getAsDouble(), 0d);
    assertEquals(-2.0d, LongStream.of(-1L, -2L, -3L).average().getAsDouble(), 0d);
  }

  public void testSum() {
    assertEquals(6L, LongStream.of(1L, 2L, 3L).sum());
    assertEquals(0L, LongStream.of(1L, 2L, -3L).sum());
    assertEquals(-6L, LongStream.of(-1L, -2L, -3L).sum());
  }

  public void testCollect() {
    String val =
        LongStream.of(1L, 2L, 3L, 4L, 5L)
            .collect(
                StringBuilder::new,
                // TODO switch to a lambda reference once #9340 is fixed
                (stringBuilder, lng) -> stringBuilder.append(lng),
                StringBuilder::append)
            .toString();

    assertEquals("12345", val);
  }

  public void testForEach() {
    List<Long> vals = new ArrayList<>();
    LongStream.of(1L, 2L, 3L, 4L, 5L).forEach(vals::add);
    assertEquals(5, vals.size());
    assertEquals(new Long[] {1L, 2L, 3L, 4L, 5L}, vals.toArray(new Long[vals.size()]));
  }

  public void testIterator() {
    List<Long> vals = new ArrayList<>();
    Iterator<Long> iterator = LongStream.of(1L, 2L, 3L, 4L, 5L).iterator();
    while (iterator.hasNext()) {
      vals.add(iterator.next());
    }
    assertEquals(5, vals.size());
    assertEquals(new Long[] {1L, 2L, 3L, 4L, 5L}, vals.toArray(new Long[vals.size()]));
  }

  public void testSpliterator() {
    Spliterator<Long> spliterator = LongStream.of(1L, 2L, 3L, 4L, 5L).spliterator();
    assertEquals(5, spliterator.estimateSize());
    assertEquals(5, spliterator.getExactSizeIfKnown());

    List<Long> vals = new ArrayList<>();
    while (spliterator.tryAdvance(vals::add)) {
      // work is all done in the condition
    }

    assertEquals(5, vals.size());
    assertEquals(new Long[] {1L, 2L, 3L, 4L, 5L}, vals.toArray(new Long[vals.size()]));
  }
}
