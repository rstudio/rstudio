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
   * href="https://docs.oracle.com/javase/8/docs/api/java/util/stream/Collector.Characteristics.html">the
   * official Java API doc</a> for details.
   */
  enum Characteristics { CONCURRENT, IDENTITY_FINISH, UNORDERED }

  static <T,A,R> Collector<T,A,R> of(Supplier<A> supplier, BiConsumer<A,T> accumulator, BinaryOperator<A> combiner, Function<A,R> finisher, Collector.Characteristics... characteristics) {
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

  static <T,R> Collector<T,R,R> of(Supplier<R> supplier, BiConsumer<R,T> accumulator, BinaryOperator<R> combiner, Collector.Characteristics... characteristics) {
    checkNotNull(supplier);
    checkNotNull(accumulator);
    checkNotNull(combiner);
    checkNotNull(characteristics);
    return new CollectorImpl<>(
        supplier,
        accumulator,
        combiner,
        Function.identity(),
        Collections.unmodifiableSet(EnumSet.of(Characteristics.IDENTITY_FINISH, characteristics))
    );
  }

  Supplier<A> supplier();

  BiConsumer<A,T> accumulator();

  Set<Collector.Characteristics> characteristics();

  BinaryOperator<A> combiner();

  Function<A,R> finisher();

  /**
   * Simple internal implementation of a collector, holding each of the functions in a field.
   */
  static final class CollectorImpl<T, A, R> implements Collector<T, A, R> {
    private final Supplier<A> supplier;
    private final BiConsumer<A, T> accumulator;
    private final Set<Collector.Characteristics> characteristics;
    private final BinaryOperator<A> combiner;
    private final Function<A, R> finisher;

    public CollectorImpl(Supplier<A> supplier, BiConsumer<A, T> accumulator, BinaryOperator<A> combiner, Function<A, R> finisher, Characteristics... characteristics) {
      this.supplier = supplier;
      this.accumulator = accumulator;
      if (characteristics.length == 0) {
        this.characteristics = Collections.emptySet();
      } else if (characteristics.length == 1) {
        this.characteristics = Collections.singleton(characteristics[0]);
      } else {
        this.characteristics = Collections.unmodifiableSet(EnumSet.of(characteristics[0], characteristics));
      }
      this.combiner = combiner;
      this.finisher = finisher;
    }

    public CollectorImpl(Supplier<A> supplier, BiConsumer<A, T> accumulator, BinaryOperator<A> combiner, Function<A, R> finisher, Set<Characteristics> characteristics) {
      this.supplier = supplier;
      this.accumulator = accumulator;
      this.combiner = combiner;
      this.finisher = finisher;
      this.characteristics = characteristics;
    }

    @Override
    public Supplier<A> supplier() {
      return supplier;
    }

    @Override
    public BiConsumer<A, T> accumulator() {
      return accumulator;
    }

    @Override
    public BinaryOperator<A> combiner() {
      return combiner;
    }

    @Override
    public Function<A, R> finisher() {
      return finisher;
    }

    @Override
    public Set<Characteristics> characteristics() {
      return characteristics;
    }
  }
}