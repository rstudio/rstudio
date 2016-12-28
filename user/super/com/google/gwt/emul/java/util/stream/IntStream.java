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
import java.util.Comparator;
import java.util.IntSummaryStatistics;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.Spliterators.AbstractIntSpliterator;
import java.util.function.BiConsumer;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;

/**
 * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/stream/IntStream.html">
 * the official Java API doc</a> for details.
 */
public interface IntStream extends BaseStream<Integer, IntStream> {

  /**
   * See <a
   * href="https://docs.oracle.com/javase/8/docs/api/java/util/stream/IntStream.Builder.html">the
   * official Java API doc</a> for details.
   */
  interface Builder extends IntConsumer {
    @Override
    void accept(int t);

    default IntStream.Builder add(int t) {
      accept(t);
      return this;
    }

    IntStream build();
  }

  static Builder builder() {
    return new Builder() {
      private int[] items = new int[0];

      @Override
      public void accept(int t) {
        checkState(items != null, "Builder already built");
        items[items.length] = t;
      }

      @Override
      public IntStream build() {
        checkState(items != null, "Builder already built");
        IntStream stream = Arrays.stream(items);
        items = null;
        return stream;
      }
    };
  }

  static IntStream concat(IntStream a, IntStream b) {
    // This is nearly the same as flatMap, but inlined, wrapped around a single spliterator of
    // these two objects, and without close() called as the stream progresses. Instead, close is
    // invoked as part of the resulting stream's own onClose, so that either can fail without
    // affecting the other, and correctly collecting suppressed exceptions.

    // TODO replace this flatMap-ish spliterator with one that directly combines the two root
    // streams
    Spliterator<? extends IntStream> spliteratorOfStreams = Arrays.asList(a, b).spliterator();

    Spliterator.OfInt spliterator =
        new Spliterators.AbstractIntSpliterator(Long.MAX_VALUE, 0) {
          Spliterator.OfInt next;

          @Override
          public boolean tryAdvance(IntConsumer action) {
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

    IntStream result = new IntStreamImpl(null, spliterator);

    result.onClose(a::close);
    result.onClose(b::close);

    return result;
  }

  static IntStream empty() {
    return new IntStreamImpl.Empty(null);
  }

  static IntStream generate(final IntSupplier s) {
    AbstractIntSpliterator spliterator =
        new Spliterators.AbstractIntSpliterator(
            Long.MAX_VALUE, Spliterator.IMMUTABLE | Spliterator.ORDERED) {
          @Override
          public boolean tryAdvance(IntConsumer action) {
            action.accept(s.getAsInt());
            return true;
          }
        };

    return StreamSupport.intStream(spliterator, false);
  }

  static IntStream iterate(int seed, IntUnaryOperator f) {

    AbstractIntSpliterator spliterator =
        new Spliterators.AbstractIntSpliterator(
            Long.MAX_VALUE, Spliterator.IMMUTABLE | Spliterator.ORDERED) {
          private int next = seed;

          @Override
          public boolean tryAdvance(IntConsumer action) {
            action.accept(next);
            next = f.applyAsInt(next);
            return true;
          }
        };

    return StreamSupport.intStream(spliterator, false);
  }

  static IntStream of(int... values) {
    return Arrays.stream(values);
  }

  static IntStream of(int t) {
    // TODO consider a splittable that returns only a single value
    return of(new int[] {t});
  }

  static IntStream range(int startInclusive, int endExclusive) {
    if (startInclusive >= endExclusive) {
      return empty();
    }
    return rangeClosed(startInclusive, endExclusive - 1);
  }

  static IntStream rangeClosed(int startInclusive, int endInclusive) {
    if (startInclusive > endInclusive) {
      return empty();
    }
    int count = endInclusive - startInclusive + 1;

    AbstractIntSpliterator spliterator =
        new Spliterators.AbstractIntSpliterator(
            count,
            Spliterator.IMMUTABLE
                | Spliterator.SIZED
                | Spliterator.SUBSIZED
                | Spliterator.ORDERED
                | Spliterator.SORTED
                | Spliterator.DISTINCT) {
          private int next = startInclusive;

          @Override
          public Comparator<? super Integer> getComparator() {
            return null;
          }

          @Override
          public boolean tryAdvance(IntConsumer action) {
            if (next <= endInclusive) {
              action.accept(next++);
              return true;
            }
            return false;
          }
        };

    return StreamSupport.intStream(spliterator, false);
  }

  boolean allMatch(IntPredicate predicate);

  boolean anyMatch(IntPredicate predicate);

  DoubleStream asDoubleStream();

  LongStream asLongStream();

  OptionalDouble average();

  Stream<Integer> boxed();

  <R> R collect(Supplier<R> supplier, ObjIntConsumer<R> accumulator, BiConsumer<R, R> combiner);

  long count();

  IntStream distinct();

  IntStream filter(IntPredicate predicate);

  OptionalInt findAny();

  OptionalInt findFirst();

  IntStream flatMap(IntFunction<? extends IntStream> mapper);

  void forEach(IntConsumer action);

  void forEachOrdered(IntConsumer action);

  @Override
  PrimitiveIterator.OfInt iterator();

  IntStream limit(long maxSize);

  IntStream map(IntUnaryOperator mapper);

  DoubleStream mapToDouble(IntToDoubleFunction mapper);

  LongStream mapToLong(IntToLongFunction mapper);

  <U> Stream<U> mapToObj(IntFunction<? extends U> mapper);

  OptionalInt max();

  OptionalInt min();

  boolean noneMatch(IntPredicate predicate);

  @Override
  IntStream parallel();

  IntStream peek(IntConsumer action);

  OptionalInt reduce(IntBinaryOperator op);

  int reduce(int identity, IntBinaryOperator op);

  @Override
  IntStream sequential();

  IntStream skip(long n);

  IntStream sorted();

  @Override
  Spliterator.OfInt spliterator();

  int sum();

  IntSummaryStatistics summaryStatistics();

  int[] toArray();
}
