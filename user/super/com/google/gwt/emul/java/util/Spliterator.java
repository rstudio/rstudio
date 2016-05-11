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
package java.util;

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

/**
 * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/Spliterator.html">
 * the official Java API doc</a> for details.
 *
 * @param <T> the type of elements returned by Spliterator.
 */
public interface Spliterator<T> {

  int DISTINCT = 0x00000001;

  int ORDERED = 0x00000010;

  int NONNULL = 0x00000100;

  int CONCURRENT = 0x00001000;

  int SORTED = 0x00000004;

  int SIZED = 0x00000040;

  int IMMUTABLE = 0x00000400;

  int SUBSIZED = 0x00004000;

  int characteristics();

  long estimateSize();

  default void forEachRemaining(Consumer<? super T> consumer) {
    while (tryAdvance(consumer)) { }
  }

  default Comparator<? super T> getComparator() {
    throw new IllegalStateException();
  }

  default long getExactSizeIfKnown() {
    return hasCharacteristics(SIZED) ? estimateSize() : -1L;
  }

  default boolean hasCharacteristics(int characteristics) {
    return (characteristics() & characteristics) != 0;
  }

  boolean tryAdvance(Consumer<? super T> consumer);

  Spliterator<T> trySplit();

  /**
   * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/Spliterator.OfPrimitive.html">
   * the official Java API doc</a> for details.
   *
   * @param <T> the type of elements returned by this Spliterator.
   * @param <C> the type of primitive Consumer.
   * @param <S> the type of primitive Spliterator.
   */
  interface OfPrimitive<T, C, S extends OfPrimitive<T, C, S>> extends Spliterator<T> {

    boolean tryAdvance(C consumer);

    @Override
    S trySplit();

    default void forEachRemaining(C consumer) {
      while (tryAdvance(consumer)) { }
    }
  }

  /**
   * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/Spliterator.OfDouble.html">
   * the official Java API doc</a> for details.
   */
  interface OfDouble extends OfPrimitive<Double, DoubleConsumer, OfDouble> {
    @Override
    default boolean tryAdvance(Consumer<? super Double> consumer) {
      if (consumer instanceof DoubleConsumer) {
        return tryAdvance((DoubleConsumer) consumer);
      } else {
        return tryAdvance((DoubleConsumer) consumer::accept);
      }
    }

    @Override
    default void forEachRemaining(Consumer<? super Double> consumer) {
      if (consumer instanceof DoubleConsumer) {
        forEachRemaining((DoubleConsumer) consumer);
      } else {
        forEachRemaining((DoubleConsumer) consumer::accept);
      }
    }
  }

  /**
   * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/Spliterator.OfInt.html">
   * the official Java API doc</a> for details.
   */
  interface OfInt extends OfPrimitive<Integer, IntConsumer, OfInt> {
    @Override
    default boolean tryAdvance(Consumer<? super Integer> consumer) {
      if (consumer instanceof IntConsumer) {
        return tryAdvance((IntConsumer) consumer);
      } else {
        return tryAdvance((IntConsumer) consumer::accept);
      }
    }

    @Override
    default void forEachRemaining(Consumer<? super Integer> consumer) {
      if (consumer instanceof IntConsumer) {
        forEachRemaining((IntConsumer) consumer);
      } else {
        forEachRemaining((IntConsumer) consumer::accept);
      }
    }
  }

  /**
   * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/Spliterator.OfLong.html">
   * the official Java API doc</a> for details.
   */
  interface OfLong extends OfPrimitive<Long, LongConsumer, OfLong> {
    @Override
    default boolean tryAdvance(Consumer<? super Long> consumer) {
      if (consumer instanceof LongConsumer) {
        return tryAdvance((LongConsumer) consumer);
      } else {
        return tryAdvance((LongConsumer) consumer::accept);
      }
    }

    @Override
    default void forEachRemaining(Consumer<? super Long> consumer) {
      if (consumer instanceof LongConsumer) {
        forEachRemaining((LongConsumer) consumer);
      } else {
        forEachRemaining((LongConsumer) consumer::accept);
      }
    }
  }
}
