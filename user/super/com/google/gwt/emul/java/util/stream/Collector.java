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

import static javaemul.internal.InternalPreconditions.checkNotNull;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * See <a
 * href="https://docs.oracle.com/javase/8/docs/api/java/util/stream/Collector.html">the
 * official Java API doc</a> for details.
 * @param <T> the type of data to be collected
 * @param <A> the type of accumulator used to track results
 * @param <R> the final output data type
 */
public interface Collector<T,A,R> {

  /**
   * See <a
   * href="https://docs.oracle.com/javase/8/docs/api/java/util/stream/Collector.Characteristics.html">
   * the official Java API doc</a> for details.
   */
  enum Characteristics { CONCURRENT, IDENTITY_FINISH, UNORDERED }

  static <T, A, R> Collector<T, A, R> of(
      Supplier<A> supplier,
      BiConsumer<A, T> accumulator,
      BinaryOperator<A> combiner,
      Function<A, R> finisher,
      Characteristics... characteristics) {
    checkNotNull(supplier);
    checkNotNull(accumulator);
    checkNotNull(combiner);
    checkNotNull(finisher);
    checkNotNull(characteristics);
    return new CollectorImpl<>(
        supplier,
        accumulator,
        combiner,
        finisher,
        characteristics
    );
  }

  static <T, R> Collector<T, R, R> of(
      Supplier<R> supplier,
      BiConsumer<R, T> accumulator,
      BinaryOperator<R> combiner,
      Characteristics... characteristics) {
    checkNotNull(supplier);
    checkNotNull(accumulator);
    checkNotNull(combiner);
    checkNotNull(characteristics);
    return new CollectorImpl<T, R, R>(
        supplier,
        accumulator,
        combiner,
        Function.identity(),
        Collections.unmodifiableSet(EnumSet.of(Characteristics.IDENTITY_FINISH, characteristics))
    );
  }

  Supplier<A> supplier();

  BiConsumer<A,T> accumulator();

  Set<Characteristics> characteristics();

  BinaryOperator<A> combiner();

  Function<A,R> finisher();
}