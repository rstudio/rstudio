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
package java.util.stream;

import java.util.Spliterator;
import java.util.function.Supplier;

/**
 * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/stream/StreamSupport.html">
 * the official Java API doc</a> for details.
 */
public final class StreamSupport {

  public static DoubleStream doubleStream(Spliterator.OfDouble spliterator, boolean parallel) {
    return new DoubleStreamImpl(null, spliterator);
  }

  public static DoubleStream doubleStream(
      Supplier<? extends Spliterator.OfDouble> supplier, int characteristics, boolean parallel) {
    // TODO this is somewhat convoluted, and would be better served by a lazy singleton spliterator
    return Stream.of(supplier)
        .map(Supplier::get)
        .flatMapToDouble(
            doubleSpliterator -> {
              return doubleStream(doubleSpliterator, parallel);
            });
  }

  public static IntStream intStream(Spliterator.OfInt spliterator, boolean parallel) {
    return new IntStreamImpl(null, spliterator);
  }

  public static IntStream intStream(
      Supplier<? extends Spliterator.OfInt> supplier, int characteristics, boolean parallel) {
    // TODO this is somewhat convoluted, and would be better served by a lazy singleton spliterator
    return Stream.of(supplier)
        .map(Supplier::get)
        .flatMapToInt(
            intSpliterator -> {
              return intStream(intSpliterator, parallel);
            });
  }

  public static LongStream longStream(Spliterator.OfLong spliterator, boolean parallel) {
    return new LongStreamImpl(null, spliterator);
  }

  public static LongStream longStream(
      Supplier<? extends Spliterator.OfLong> supplier,
      int characteristics,
      final boolean parallel) {
    // TODO this is somewhat convoluted, and would be better served by a lazy singleton spliterator
    return Stream.of(supplier)
        .map(Supplier::get)
        .flatMapToLong(
            longSpliterator -> {
              return longStream(longSpliterator, parallel);
            });
  }

  public static <T> Stream<T> stream(Spliterator<T> spliterator, boolean parallel) {
    return new StreamImpl<T>(null, spliterator);
  }

  public static <T> Stream<T> stream(
      Supplier<? extends Spliterator<T>> supplier, int characteristics, final boolean parallel) {
    // TODO this is somewhat convoluted, and would be better served by a lazy singleton spliterator
    return Stream.of(supplier)
        .map(Supplier::get)
        .flatMap(
            spliterator -> {
              return stream(spliterator, parallel);
            });
  }

  private StreamSupport() {
    // prevent instantiation
  }
}
