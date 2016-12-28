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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Simple internal implementation of a collector, holding each of the functions in a field.
 */
final class CollectorImpl<T, A, R> implements Collector<T, A, R> {
  private final Supplier<A> supplier;
  private final BiConsumer<A, T> accumulator;
  private final Set<Characteristics> characteristics;
  private final BinaryOperator<A> combiner;
  private final Function<A, R> finisher;

  public CollectorImpl(
      Supplier<A> supplier,
      BiConsumer<A, T> accumulator,
      BinaryOperator<A> combiner,
      Function<A, R> finisher,
      Characteristics... characteristics) {
    this.supplier = supplier;
    this.accumulator = accumulator;
    if (characteristics.length == 0) {
      this.characteristics = Collections.emptySet();
    } else if (characteristics.length == 1) {
      this.characteristics = Collections.singleton(characteristics[0]);
    } else {
      this.characteristics =
          Collections.unmodifiableSet(EnumSet.of(characteristics[0], characteristics));
    }
    this.combiner = combiner;
    this.finisher = finisher;
  }

  public CollectorImpl(
      Supplier<A> supplier,
      BiConsumer<A, T> accumulator,
      BinaryOperator<A> combiner,
      Function<A, R> finisher,
      Set<Characteristics> characteristics) {
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
