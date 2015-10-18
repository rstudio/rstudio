/*
 * Copyright 2015 Google Inc.
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

import static javaemul.internal.InternalPreconditions.checkCriticalNotNull;
import static javaemul.internal.InternalPreconditions.checkNotNull;

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

/**
 * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/PrimitiveIterator.html">
 * the official Java API doc</a> for details.
 *
 * @param <T> element type
 * @param <C> consumer type
 */
public interface PrimitiveIterator<T, C> extends Iterator<T> {

  void forEachRemaining(C consumer);

  /**
   * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/PrimitiveIterator.OfDouble.html">
   * the official Java API doc</a> for details.
   */
  interface OfDouble extends PrimitiveIterator<Double, DoubleConsumer> {
    double nextDouble();

    @Override
    default Double next() {
      return nextDouble();
    }

    @Override
    default void forEachRemaining(DoubleConsumer consumer) {
      checkNotNull(consumer);
      while (hasNext()) {
        consumer.accept(nextDouble());
      }
    }

    @Override
    default void forEachRemaining(Consumer<? super Double> consumer) {
      if (consumer instanceof DoubleConsumer) {
        forEachRemaining((DoubleConsumer) consumer);
      } else {
        checkCriticalNotNull(consumer);
        forEachRemaining((DoubleConsumer) consumer::accept);
      }
    }
  }

  /**
   * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/PrimitiveIterator.OfInt.html">
   * the official Java API doc</a> for details.
   */
  interface OfInt extends PrimitiveIterator<Integer, IntConsumer> {
    int nextInt();

    @Override
    default Integer next() {
      return nextInt();
    }

    @Override
    default void forEachRemaining(IntConsumer consumer) {
      checkNotNull(consumer);
      while (hasNext()) {
        consumer.accept(nextInt());
      }
    }

    @Override
    default void forEachRemaining(Consumer<? super Integer> consumer) {
      if (consumer instanceof IntConsumer) {
        forEachRemaining((IntConsumer) consumer);
      } else {
        checkCriticalNotNull(consumer);
        forEachRemaining((IntConsumer) consumer::accept);
      }
    }
  }

  /**
   * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/PrimitiveIterator.OfLong.html">
   * the official Java API doc</a> for details.
   */
  interface OfLong extends PrimitiveIterator<Long, LongConsumer> {
    long nextLong();

    @Override
    default Long next() {
      return nextLong();
    }

    @Override
    default void forEachRemaining(LongConsumer consumer) {
      checkNotNull(consumer);
      while (hasNext()) {
        consumer.accept(nextLong());
      }
    }

    @Override
    default void forEachRemaining(Consumer<? super Long> consumer) {
      if (consumer instanceof LongConsumer) {
        forEachRemaining((LongConsumer) consumer);
      } else {
        checkCriticalNotNull(consumer);
        forEachRemaining((LongConsumer) consumer::accept);
      }
    }
  }

}
