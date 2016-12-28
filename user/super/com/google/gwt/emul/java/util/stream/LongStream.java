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
import java.util.LongSummaryStatistics;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.Spliterators.AbstractLongSpliterator;
import java.util.function.BiConsumer;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongSupplier;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;

/**
 * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/stream/LongStream.html">
 * the official Java API doc</a> for details.
 */
public interface LongStream extends BaseStream<Long, LongStream> {

  /**
   * See <a
   * href="https://docs.oracle.com/javase/8/docs/api/java/util/stream/LongStream.Builder.html">the
   * official Java API doc</a> for details.
   */
  interface Builder extends LongConsumer {
    @Override
    void accept(long t);

    default LongStream.Builder add(long t) {
      accept(t);
      return this;
    }

    LongStream build();
  }

  static Builder builder() {
    return new Builder() {
      private long[] items = new long[0];

      @Override
      public void accept(long t) {
        checkState(items != null, "Builder already built");
        items[items.length] = t;
      }

      @Override
      public LongStream build() {
        checkState(items != null, "Builder already built");
        LongStream stream = Arrays.stream(items);
        items = null;
        return stream;
      }
    };
  }

  static LongStream concat(LongStream a, LongStream b) {
    // This is nearly the same as flatMap, but inlined, wrapped around a single spliterator of
    // these two objects, and without close() called as the stream progresses. Instead, close is
    // invoked as part of the resulting stream's own onClose, so that either can fail without
    // affecting the other, and correctly collecting suppressed exceptions.

    // TODO replace this flatMap-ish spliterator with one that directly combines the two root
    // streams
    Spliterator<? extends LongStream> spliteratorOfStreams = Arrays.asList(a, b).spliterator();

    AbstractLongSpliterator spliterator =
        new Spliterators.AbstractLongSpliterator(Long.MAX_VALUE, 0) {
          Spliterator.OfLong next;

          @Override
          public boolean tryAdvance(LongConsumer action) {
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

    LongStream result = new LongStreamImpl(null, spliterator);

    result.onClose(a::close);
    result.onClose(b::close);

    return result;
  }

  static LongStream empty() {
    return new LongStreamImpl.Empty(null);
  }

  static LongStream generate(LongSupplier s) {
    AbstractLongSpliterator spltierator =
        new Spliterators.AbstractLongSpliterator(
            Long.MAX_VALUE, Spliterator.IMMUTABLE | Spliterator.ORDERED) {
          @Override
          public boolean tryAdvance(LongConsumer action) {
            action.accept(s.getAsLong());
            return true;
          }
        };

    return StreamSupport.longStream(spltierator, false);
  }

  static LongStream iterate(long seed, LongUnaryOperator f) {
    AbstractLongSpliterator spliterator =
        new Spliterators.AbstractLongSpliterator(
            Long.MAX_VALUE, Spliterator.IMMUTABLE | Spliterator.ORDERED) {
          private long next = seed;

          @Override
          public boolean tryAdvance(LongConsumer action) {
            action.accept(next);
            next = f.applyAsLong(next);
            return true;
          }
        };
    return StreamSupport.longStream(spliterator, false);
  }

  static LongStream of(long... values) {
    return Arrays.stream(values);
  }

  static LongStream of(long t) {
    // TODO consider a splittable that returns only a single value
    return of(new long[] {t});
  }

  static LongStream range(long startInclusive, long endExclusive) {
    if (startInclusive >= endExclusive) {
      return empty();
    }
    return rangeClosed(startInclusive, endExclusive - 1);
  }

  static LongStream rangeClosed(long startInclusive, long endInclusive) {
    if (startInclusive > endInclusive) {
      return empty();
    }
    long count = endInclusive - startInclusive + 1;

    AbstractLongSpliterator spliterator =
        new Spliterators.AbstractLongSpliterator(
            count,
            Spliterator.IMMUTABLE
                | Spliterator.SIZED
                | Spliterator.SUBSIZED
                | Spliterator.ORDERED
                | Spliterator.SORTED
                | Spliterator.DISTINCT) {
          private long next = startInclusive;

          @Override
          public Comparator<? super Long> getComparator() {
            return null;
          }

          @Override
          public boolean tryAdvance(LongConsumer action) {
            if (next <= endInclusive) {
              action.accept(next++);
              return true;
            }
            return false;
          }
        };

    return StreamSupport.longStream(spliterator, false);
  }

  boolean allMatch(LongPredicate predicate);

  boolean anyMatch(LongPredicate predicate);

  DoubleStream asDoubleStream();

  OptionalDouble average();

  Stream<Long> boxed();

  <R> R collect(Supplier<R> supplier, ObjLongConsumer<R> accumulator, BiConsumer<R, R> combiner);

  long count();

  LongStream distinct();

  LongStream filter(LongPredicate predicate);

  OptionalLong findAny();

  OptionalLong findFirst();

  LongStream flatMap(LongFunction<? extends LongStream> mapper);

  void forEach(LongConsumer action);

  void forEachOrdered(LongConsumer action);

  @Override
  PrimitiveIterator.OfLong iterator();

  LongStream limit(long maxSize);

  LongStream map(LongUnaryOperator mapper);

  DoubleStream mapToDouble(LongToDoubleFunction mapper);

  IntStream mapToInt(LongToIntFunction mapper);

  <U> Stream<U> mapToObj(LongFunction<? extends U> mapper);

  OptionalLong max();

  OptionalLong min();

  boolean noneMatch(LongPredicate predicate);

  @Override
  LongStream parallel();

  LongStream peek(LongConsumer action);

  OptionalLong reduce(LongBinaryOperator op);

  long reduce(long identity, LongBinaryOperator op);

  @Override
  LongStream sequential();

  LongStream skip(long n);

  LongStream sorted();

  @Override
  Spliterator.OfLong spliterator();

  long sum();

  LongSummaryStatistics summaryStatistics();

  long[] toArray();
}
