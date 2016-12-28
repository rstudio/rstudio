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

import static javaemul.internal.InternalPreconditions.checkState;

import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.OptionalDouble;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;

/**
 * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/stream/DoubleStream.html">
 * the official Java API doc</a> for details.
 */
public interface DoubleStream extends BaseStream<Double, DoubleStream> {

  /**
   * See <a
   * href="https://docs.oracle.com/javase/8/docs/api/java/util/stream/DoubleStream.Builder.html">the
   * official Java API doc</a> for details.
   */
  interface Builder extends DoubleConsumer {
    @Override
    void accept(double t);

    default DoubleStream.Builder add(double t) {
      accept(t);
      return this;
    }

    DoubleStream build();
  }

  static Builder builder() {
    return new Builder() {
      private double[] items = new double[0];

      @Override
      public void accept(double t) {
        checkState(items != null, "Builder already built");
        items[items.length] = t;
      }

      @Override
      public DoubleStream build() {
        checkState(items != null, "Builder already built");
        DoubleStream stream = Arrays.stream(items);
        items = null;
        return stream;
      }
    };
  }

  static DoubleStream concat(DoubleStream a, DoubleStream b) {
    // This is nearly the same as flatMap, but inlined, wrapped around a single spliterator of
    // these two objects, and without close() called as the stream progresses. Instead, close is
    // invoked as part of the resulting stream's own onClose, so that either can fail without
    // affecting the other, and correctly collecting suppressed exceptions.

    // TODO replace this flatMap-ish spliterator with one that directly combines the two root
    // streams
    Spliterator<? extends DoubleStream> spliteratorOfStreams = Arrays.asList(a, b).spliterator();

    Spliterator.OfDouble spliterator =
        new Spliterators.AbstractDoubleSpliterator(Long.MAX_VALUE, 0) {
          Spliterator.OfDouble next;

          @Override
          public boolean tryAdvance(DoubleConsumer action) {
            // look for a new spliterator
            while (advanceToNextSpliterator()) {
              // if we have one, try to read and use it
              if (next.tryAdvance(action)) {
                return true;
              } else {
                // failed, null it out so we can find another
                next = null;
              }
            }
            return false;
          }

          private boolean advanceToNextSpliterator() {
            while (next == null) {
              if (!spliteratorOfStreams.tryAdvance(
                  n -> {
                    if (n != null) {
                      next = n.spliterator();
                    }
                  })) {
                return false;
              }
            }
            return true;
          }
        };

    DoubleStream result = new DoubleStreamImpl(null, spliterator);

    result.onClose(a::close);
    result.onClose(b::close);

    return result;
  }

  static DoubleStream empty() {
    return new DoubleStreamImpl.Empty(null);
  }

  static DoubleStream generate(DoubleSupplier s) {
    Spliterator.OfDouble spliterator =
        new Spliterators.AbstractDoubleSpliterator(
            Long.MAX_VALUE, Spliterator.IMMUTABLE | Spliterator.ORDERED) {
          @Override
          public boolean tryAdvance(DoubleConsumer action) {
            action.accept(s.getAsDouble());
            return true;
          }
        };

    return StreamSupport.doubleStream(spliterator, false);
  }

  static DoubleStream iterate(double seed, DoubleUnaryOperator f) {
    Spliterator.OfDouble spliterator =
        new Spliterators.AbstractDoubleSpliterator(
            Long.MAX_VALUE, Spliterator.IMMUTABLE | Spliterator.ORDERED) {
          private double next = seed;

          @Override
          public boolean tryAdvance(DoubleConsumer action) {
            action.accept(next);
            next = f.applyAsDouble(next);
            return true;
          }
        };

    return StreamSupport.doubleStream(spliterator, false);
  }

  static DoubleStream of(double... values) {
    return Arrays.stream(values);
  }

  static DoubleStream of(double t) {
    // TODO consider a splittable that returns only a single value
    return of(new double[] {t});
  }

  boolean allMatch(DoublePredicate predicate);

  boolean anyMatch(DoublePredicate predicate);

  OptionalDouble average();

  Stream<Double> boxed();

  <R> R collect(Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner);

  long count();

  DoubleStream distinct();

  DoubleStream filter(DoublePredicate predicate);

  OptionalDouble findAny();

  OptionalDouble findFirst();

  DoubleStream flatMap(DoubleFunction<? extends DoubleStream> mapper);

  void forEach(DoubleConsumer action);

  void forEachOrdered(DoubleConsumer action);

  @Override
  PrimitiveIterator.OfDouble iterator();

  DoubleStream limit(long maxSize);

  DoubleStream map(DoubleUnaryOperator mapper);

  IntStream mapToInt(DoubleToIntFunction mapper);

  LongStream mapToLong(DoubleToLongFunction mapper);

  <U> Stream<U> mapToObj(DoubleFunction<? extends U> mapper);

  OptionalDouble max();

  OptionalDouble min();

  boolean noneMatch(DoublePredicate predicate);

  @Override
  DoubleStream parallel();

  DoubleStream peek(DoubleConsumer action);

  OptionalDouble reduce(DoubleBinaryOperator op);

  double reduce(double identity, DoubleBinaryOperator op);

  @Override
  DoubleStream sequential();

  DoubleStream skip(long n);

  DoubleStream sorted();

  @Override
  Spliterator.OfDouble spliterator();

  double sum();

  DoubleSummaryStatistics summaryStatistics();

  double[] toArray();
}
