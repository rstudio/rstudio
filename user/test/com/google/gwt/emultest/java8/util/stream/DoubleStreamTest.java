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
import java.util.DoubleSummaryStatistics;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Spliterator;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

/**
 * Tests {@link DoubleStream}.
 */
public class DoubleStreamTest extends EmulTestBase {

  public void testEmptyStream() {
    DoubleStream empty = DoubleStream.empty();
    assertEquals(0, empty.count());
    try {
      empty.count();
      fail("second terminal operation should have thrown IllegalStateEx");
    } catch (IllegalStateException expected) {
      // expected
    }

    assertEquals(0, DoubleStream.empty().limit(2).toArray().length);
    assertEquals(0L, DoubleStream.empty().count());
    assertEquals(0L, DoubleStream.empty().limit(2).count());

    assertFalse(DoubleStream.empty().findFirst().isPresent());
    assertFalse(DoubleStream.empty().findAny().isPresent());
    assertFalse(DoubleStream.empty().max().isPresent());
    assertFalse(DoubleStream.empty().min().isPresent());
    assertTrue(DoubleStream.empty().noneMatch(item -> true));
    assertTrue(DoubleStream.empty().allMatch(item -> true));
    assertFalse(DoubleStream.empty().anyMatch(item -> true));
    assertEquals(new double[0], DoubleStream.empty().toArray());
  }

  public void testStreamOfOne() {
    Supplier<DoubleStream> one = () -> DoubleStream.of(1);
    assertEquals(new double[] {1d}, one.get().toArray());
    assertEquals(1L, one.get().count());
    assertEquals(1d, one.get().findFirst().getAsDouble(), 0d);
    assertEquals(1d, one.get().findAny().getAsDouble(), 0d);
  }

  public void testBuilder() {
    DoubleStream s = DoubleStream.builder().add(1d).add(3d).add(2d).build();

    assertEquals(new double[] {1d, 3d, 2d}, s.toArray());

    DoubleStream.Builder builder = DoubleStream.builder();
    DoubleStream built = builder.build();
    assertEquals(0, built.count());
    try {
      builder.build();
      fail("build() after build() should fail");
    } catch (IllegalStateException expected) {
      // expected
    }
    try {
      builder.add(10d);
      fail("add() after build() should fail");
    } catch (IllegalStateException expected) {
      // expected
    }
  }

  public void testConcat() {
    Supplier<DoubleStream> adbc =
        () -> {
          return DoubleStream.concat(DoubleStream.of(1, 4), DoubleStream.of(2, 3));
        };

    assertEquals(new double[] {1d, 4d, 2d, 3d}, adbc.get().toArray());
    assertEquals(new double[] {1d, 2d, 3d, 4d}, adbc.get().sorted().toArray());

    List<String> closed = new ArrayList<>();
    DoubleStream first = DoubleStream.of(1d).onClose(() -> closed.add("first"));
    DoubleStream second = DoubleStream.of(2d).onClose(() -> closed.add("second"));

    DoubleStream concat = DoubleStream.concat(first, second);

    // read everything, make sure we saw it all and didn't close automatically
    double collectedAll = concat.sum();
    assertEquals(3d, collectedAll);
    assertEquals(0, closed.size());

    concat.close();
    assertEquals(Arrays.asList("first", "second"), closed);
  }

  public void testIterate() {
    assertEquals(
        new double[] {10d, 11d, 12d, 13d, 14d},
        DoubleStream.iterate(0d, l -> l + 1d).skip(10).limit(5).toArray());
  }

  public void testGenerate() {
    // infinite, but if you limit it is already too short to skip much
    assertEquals(new double[0], DoubleStream.generate(makeGenerator()).limit(4).skip(5).toArray());

    assertEquals(
        new double[] {10d, 11d, 12d, 13d, 14d},
        DoubleStream.generate(makeGenerator()).skip(10).limit(5).toArray());
  }

  private DoubleSupplier makeGenerator() {
    return new DoubleSupplier() {
      double next = 0d;

      @Override
      public double getAsDouble() {
        return next++;
      }
    };
  }

  public void testToArray() {
    assertEquals(new double[0], DoubleStream.of().toArray());
    assertEquals(new double[] {1d}, DoubleStream.of(1d).toArray());
    assertEquals(new double[] {3d, 2d, 0d}, DoubleStream.of(3d, 2d, 0d).toArray());
  }

  public void testReduce() {
    double reduced = DoubleStream.of(1d, 2d, 4d).reduce(0, Double::sum);
    assertEquals(7d, reduced, 0d);

    reduced = DoubleStream.of().reduce(0, Double::sum);
    assertEquals(0d, reduced, 0d);

    OptionalDouble maybe = DoubleStream.of(1d, 4d, 8d).reduce(Double::sum);
    assertTrue(maybe.isPresent());
    assertEquals(13, maybe.getAsDouble(), 0d);
    maybe = DoubleStream.of().reduce(Double::sum);
    assertFalse(maybe.isPresent());
  }

  public void testFilter() {
    // unconsumed stream never runs filter
    boolean[] data = {false};
    DoubleStream.of(1d, 2d, 3d).filter(i -> data[0] |= true);
    assertFalse(data[0]);

    // Nothing's *defined* to care about the Spliterator characteristics, but the implementation
    // can't actually know the size before executing, so we check the characteristics explicitly.
    assertFalse(
        DoubleStream.of(1d, 2d, 3d)
            .filter(a -> a == 1d)
            .spliterator()
            .hasCharacteristics(Spliterator.SIZED | Spliterator.SUBSIZED));

    // one result
    assertEquals(
        new double[] {1d}, DoubleStream.of(1d, 2d, 3d, 4d, 3d).filter(a -> a == 1).toArray());
    // zero results
    assertEquals(new double[0], DoubleStream.of(1d, 2d, 3d, 4d, 3d).filter(a -> false).toArray());
    // two results
    assertEquals(
        new double[] {2d, 4d},
        DoubleStream.of(1d, 2d, 3d, 4d, 3d).filter(a -> a % 2 == 0).toArray());
    // all
    assertEquals(
        new double[] {1d, 2d, 3d, 4d, 3d},
        DoubleStream.of(1d, 2d, 3d, 4d, 3d).filter(a -> true).toArray());
  }

  public void testMap() {
    // unconsumed stream never runs map
    int[] data = {0};
    DoubleStream.of(1d, 2d, 3d).map(i -> data[0]++);
    assertEquals(0, data[0]);

    assertEquals(new double[] {2d, 4d, 6d}, DoubleStream.of(1d, 2d, 3d).map(i -> i * 2).toArray());
  }

  public void testPeek() {
    // unconsumed stream never peeks
    boolean[] data = {false};
    DoubleStream.of(1d, 2d, 3d).peek(i -> data[0] |= true);
    assertFalse(data[0]);

    // make sure we saw it all in order
    double[] items = new double[] {1d, 2d, 3d};
    List<Double> peeked = new ArrayList<>();
    DoubleStream.of(items)
        .peek(peeked::add)
        .forEach(
            item -> {
              // do nothing, just run
            });
    assertEquals(items.length, peeked.size());
    for (int i = 0; i < items.length; i++) {
      assertEquals(items[i], peeked.get(i), 0d);
    }
  }

  // same impl, no parallel in browser
  public void testFindFirstOrAny() {
    OptionalDouble any = DoubleStream.of(1d, 2d).findAny();
    assertTrue(any.isPresent());
    assertEquals(1d, any.getAsDouble(), 0d);
  }

  public void testAnyMatch() {
    // all
    assertTrue(DoubleStream.of(1d, 2d).anyMatch(s -> true));

    // some
    assertTrue(DoubleStream.of(1d, 2d).anyMatch(s -> s == 1d));

    // none
    assertFalse(DoubleStream.of(1d, 2d).anyMatch(s -> false));
  }

  public void testAllMatch() {
    // all
    assertTrue(DoubleStream.of(1d, 2d).allMatch(s -> true));

    // some
    assertFalse(DoubleStream.of(1d, 2d).allMatch(s -> s == 1d));

    // none
    assertFalse(DoubleStream.of(1d, 2d).allMatch(s -> false));
  }

  public void testNoneMatch() {
    // all
    assertFalse(DoubleStream.of(1d, 2d).noneMatch(s -> true));

    // some
    assertFalse(DoubleStream.of(1d, 2d).noneMatch(s -> s == 1d));

    // none
    assertTrue(DoubleStream.of(1d, 2d).noneMatch(s -> false));
  }

  public void testFlatMap() {
    assertEquals(0L, DoubleStream.empty().flatMap(value -> DoubleStream.of(1d)).count());
    assertEquals(0L, DoubleStream.of(1d).flatMap(value -> DoubleStream.empty()).count());
    assertEquals(0L, DoubleStream.of(1d).flatMap(value -> DoubleStream.of()).count());
    assertEquals(0L, DoubleStream.of().flatMap(value -> DoubleStream.of(1d)).count());
    assertEquals(1L, DoubleStream.of(1d).flatMap(value -> DoubleStream.of(1d)).count());

    DoubleStream values = DoubleStream.of(1d, 2d, 3d);

    assertEquals(
        new double[] {1d, 2d, 2d, 4d, 3d, 6d},
        values.flatMap(i -> DoubleStream.of(i, i * 2d)).toArray());
  }

  public void testMapToOthers() {
    Supplier<DoubleStream> s = () -> DoubleStream.of(1d, 2d, 10d);

    assertEquals(
        new String[] {"1", "2", "10"},
        s.get().mapToObj(DoubleStreamTest::toIntegralString).toArray(String[]::new));

    assertEquals(new long[] {1L, 2L, 10L}, s.get().mapToLong(i -> (long) i).toArray());

    assertEquals(new int[] {1, 2, 10}, s.get().mapToInt(i -> (int) i).toArray());
  }

  public void testDistinct() {
    double[] distinct = DoubleStream.of(1d, 2d, 3d, 2d).distinct().toArray();
    assertEquals(3, distinct.length);
    assertEquals(1d + 2d + 3d, distinct[0] + distinct[1] + distinct[2], 0d);
  }

  public void testSorted() {
    double[] sorted = DoubleStream.of(3d, 1d, 2d).sorted().toArray();
    assertEquals(new double[] {1d, 2d, 3d}, sorted);
  }

  public void testMinMax() {
    Supplier<DoubleStream> stream = () -> DoubleStream.of(2d, 3d, 4d, 1d);

    assertEquals(1d, stream.get().min().orElse(0), 0d);
    assertEquals(4d, stream.get().max().orElse(0), 0d);

    assertFalse(stream.get().filter(a -> false).max().isPresent());
    assertFalse(stream.get().filter(a -> false).min().isPresent());
  }

  public void testCountLimitSkip() {
    Supplier<DoubleStream> stream = () -> DoubleStream.of(1d, 2d, 3d, 4d);

    assertEquals(4L, stream.get().count());

    assertEquals(4L, stream.get().limit(4).count());
    assertEquals(4L, stream.get().limit(5).count());
    assertEquals(3L, stream.get().limit(3).count());

    assertEquals(3L, stream.get().skip(1).limit(3).count());

    assertEquals(2L, stream.get().limit(3).skip(1).count());

    assertEquals(1L, stream.get().skip(3).count());

    assertEquals(new double[] {3d, 4d}, stream.get().skip(2).limit(3).toArray());
    assertEquals(new double[] {3d}, stream.get().skip(2).limit(1).toArray());

    assertEquals(new double[] {4d}, stream.get().skip(3).toArray());
    assertEquals(new double[] {}, stream.get().skip(5).toArray());

    assertEquals(new double[] {1d, 2d}, stream.get().limit(2).toArray());

    assertEquals(new double[] {2d}, stream.get().limit(2).skip(1).toArray());
  }

  public void testBoxed() {
    Supplier<DoubleStream> stream = () -> DoubleStream.of(1d, 2d);
    Stream<Double> expected = stream.get().mapToObj(Double::valueOf);
    assertEquals(expected.toArray(), stream.get().boxed().toArray());
  }

  public void testSummaryStats() {
    Supplier<DoubleStream> stream = () -> DoubleStream.of(1d, 2d, 3d);
    DoubleSummaryStatistics summaryStats = stream.get().summaryStatistics();
    assertEquals(3L, summaryStats.getCount());
    assertEquals(1d, summaryStats.getMin(), 0d);
    assertEquals(2d, summaryStats.getAverage(), 0d);
    assertEquals(3d, summaryStats.getMax(), 0d);
    assertEquals(6d, summaryStats.getSum(), 0d);

    summaryStats.accept(6L);
    assertEquals(4L, summaryStats.getCount());
    assertEquals(1d, summaryStats.getMin(), 0d);
    assertEquals(3d, summaryStats.getAverage(), 0d);
    assertEquals(6d, summaryStats.getMax(), 0d);
    assertEquals(12d, summaryStats.getSum(), 0d);

    DoubleSummaryStatistics combinedSumStats = stream.get().summaryStatistics();
    combinedSumStats.combine(DoubleStream.of(4d, 5d, 6d, 0d).summaryStatistics());
    assertEquals(7L, combinedSumStats.getCount());
    assertEquals(0d, combinedSumStats.getMin(), 0d);
    assertEquals(3d, combinedSumStats.getAverage(), 0d);
    assertEquals(6d, combinedSumStats.getMax(), 0d);
    assertEquals(21d, combinedSumStats.getSum(), 0d);
  }

  public void testAverage() {
    assertFalse(DoubleStream.empty().average().isPresent());
    assertEquals(2.0d, DoubleStream.of(1d, 2d, 3d).average().getAsDouble(), 0d);
    assertEquals(0d, DoubleStream.of(1d, 2d, -3d).average().getAsDouble(), 0d);
    assertEquals(-2.0d, DoubleStream.of(-1d, -2d, -3d).average().getAsDouble(), 0d);
  }

  public void testSum() {
    assertEquals(6d, DoubleStream.of(1d, 2d, 3d).sum(), 0d);
    assertEquals(0d, DoubleStream.of(1d, 2d, -3d).sum(), 0d);
    assertEquals(-6d, DoubleStream.of(-1d, -2d, -3d).sum(), 0d);
  }

  public void testCollect() {
    // noinspection StringConcatenationInsideStringBufferAppend
    String val =
        DoubleStream.of(1d, 2d, 3d, 4d, 5d)
            .collect(
                StringBuilder::new,
                (stringBuilder, d) -> stringBuilder.append(toIntegralString(d)),
                StringBuilder::append)
            .toString();

    assertEquals("12345", val);
  }

  public void testForEach() {
    List<Double> vals = new ArrayList<>();
    DoubleStream.of(1d, 2d, 3d, 4d, 5d).forEach(vals::add);
    assertEquals(5, vals.size());
    assertEquals(new Double[] {1d, 2d, 3d, 4d, 5d}, vals.toArray(new Double[vals.size()]));
  }

  public void testIterator() {
    List<Double> vals = new ArrayList<>();
    Iterator<Double> iterator = DoubleStream.of(1d, 2d, 3d, 4d, 5d).iterator();
    while (iterator.hasNext()) {
      vals.add(iterator.next());
    }
    assertEquals(5, vals.size());
    assertEquals(new Double[] {1d, 2d, 3d, 4d, 5d}, vals.toArray(new Double[vals.size()]));
  }

  public void testSpliterator() {
    Spliterator<Double> spliterator = DoubleStream.of(1d, 2d, 3d, 4d, 5d).spliterator();
    assertEquals(5, spliterator.estimateSize());
    assertEquals(5, spliterator.getExactSizeIfKnown());

    List<Double> vals = new ArrayList<>();
    while (spliterator.tryAdvance(vals::add)) {
      // work is all done in the condition
    }

    assertEquals(5, vals.size());
    assertEquals(new Double[] {1d, 2d, 3d, 4d, 5d}, vals.toArray(new Double[vals.size()]));
  }

  // See https://github.com/gwtproject/gwt/issues/8615
  private static String toIntegralString(double value) {
    return "" + (int) value;
  }
}
