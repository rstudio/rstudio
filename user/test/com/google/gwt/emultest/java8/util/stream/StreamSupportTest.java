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

import static java.util.stream.StreamSupport.doubleStream;
import static java.util.stream.StreamSupport.intStream;
import static java.util.stream.StreamSupport.longStream;
import static java.util.stream.StreamSupport.stream;

import com.google.gwt.emultest.java.util.EmulTestBase;

import java.util.Spliterators;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * Tests {@link java.util.stream.StreamSupport}.
 */
public class StreamSupportTest extends EmulTestBase {

  public void testDoubleStream() {
    DoubleStream doubles =
        doubleStream(Spliterators.spliterator(new double[] {1d, 2d, 3d, 4d}, 0), false);

    assertNotNull(doubles);
    assertEquals(new double[] {1d, 2d, 3d}, doubles.limit(3).toArray());

    doubles =
        doubleStream(() -> Spliterators.spliterator(new double[] {1d, 2d, 3d, 4d}, 0), 0, false);

    assertNotNull(doubles);
    assertEquals(new double[] {2d, 3d}, doubles.skip(1).limit(2).toArray());
  }

  public void testIntStream() {
    IntStream ints = intStream(Spliterators.spliterator(new int[] {1, 2, 3, 4}, 0), false);

    assertNotNull(ints);
    assertEquals(new int[] {1, 2, 3}, ints.limit(3).toArray());

    ints = intStream(() -> Spliterators.spliterator(new int[] {1, 2, 3, 4}, 0), 0, false);

    assertNotNull(ints);
    assertEquals(new int[] {2, 3}, ints.skip(1).limit(2).toArray());
  }

  public void testLongStream() {
    LongStream longs = longStream(Spliterators.spliterator(new long[] {1L, 2L, 3L, 4L}, 0), false);

    assertNotNull(longs);
    assertEquals(new long[] {1L, 2L, 3L}, longs.limit(3).toArray());

    longs = longStream(() -> Spliterators.spliterator(new long[] {1L, 2L, 3L, 4L}, 0), 0, false);

    assertNotNull(longs);
    assertEquals(new long[] {2L, 3L}, longs.skip(1).limit(2).toArray());
  }

  public void testStream() {
    Stream<String> strings =
        stream(Spliterators.spliterator(new String[] {"a", "b", "c", "d"}, 0), false);

    assertNotNull(strings);
    assertEquals(new String[] {"a", "b", "c"}, strings.limit(3).toArray());

    strings =
        stream(() -> Spliterators.spliterator(new String[] {"a", "b", "c", "d"}, 0), 0, false);

    assertNotNull(strings);
    assertEquals(new String[] {"b", "c"}, strings.skip(1).limit(2).toArray());
  }
}
