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
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.function.UnaryOperator;

/**
 * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.html">
 * the official Java API doc</a> for details.
 *
 * @param <T> the type of data being streamed
 */
public interface Stream<T> extends BaseStream<T, Stream<T>> {

  /**
   * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.Builder.html">
   * the official Java API doc</a> for details.
   */
  interface Builder<T> extends Consumer<T> {
    @Override
    void accept(T t);

    default Stream.Builder<T> add(T t) {
      accept(t);
      return this;
    }

    Stream<T> build();
  }

  static <T> Stream.Builder<T> builder() {
    return new Builder<T>() {
      private Object[] items = new Object[0];

      @Override
      public void accept(T t) {
        checkState(items != null, "Builder already built");
        items[items.length] = t;
      }

      @Override
      @SuppressWarnings("unchecked")
      public Stream<T> build() {
        checkState(items != null, "Builder already built");
        Stream<T> stream = (Stream<T>) Arrays.stream(items);
        items = null;
        return stream;
      }
    };
  }

  static <T> Stream<T> concat(Stream<? extends T> a, Stream<? extends T> b) {
    // This is nearly the same as flatMap, but inlined, wrapped around a single spliterator of
    // these two objects, and without close() called as the stream progresses. Instead, close is
    // invoked as part of the resulting stream's own onClose, so that either can fail without
    // affecting the other, and correctly collecting suppressed exceptions.

    // TODO replace this flatMap-ish spliterator with one that directly combines the two root
    // streams
    Spliterator<? extends Stream<? extends T>> spliteratorOfStreams =
        Arrays.asList(a, b).spliterator();

    AbstractSpliterator<T> spliterator =
        new Spliterators.AbstractSpliterator<T>(Long.MAX_VALUE, 0) {
          Spliterator<? extends T> next;

          @Override
          public boolean tryAdvance(Consumer<? super T> action) {
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

    Stream<T> result = new StreamImpl<T>(null, spliterator);

    result.onClose(a::close);
    result.onClose(b::close);

    return result;
  }

  static <T> Stream<T> empty() {
    return new StreamImpl.Empty<T>(null);
  }

  static <T> Stream<T> generate(Supplier<T> s) {
    AbstractSpliterator<T> spliterator =
        new Spliterators.AbstractSpliterator<T>(
            Long.MAX_VALUE, Spliterator.IMMUTABLE | Spliterator.ORDERED) {
          @Override
          public boolean tryAdvance(Consumer<? super T> action) {
            action.accept(s.get());
            return true;
          }
        };
    return StreamSupport.stream(spliterator, false);
  }

  static <T> Stream<T> iterate(T seed, UnaryOperator<T> f) {
    AbstractSpliterator<T> spliterator =
        new Spliterators.AbstractSpliterator<T>(
            Long.MAX_VALUE, Spliterator.IMMUTABLE | Spliterator.ORDERED) {
          private T next = seed;

          @Override
          public boolean tryAdvance(Consumer<? super T> action) {
            action.accept(next);
            next = f.apply(next);
            return true;
          }
        };
    return StreamSupport.stream(spliterator, false);
  }

  static <T> Stream<T> of(T t) {
    // TODO consider a splittable that returns only a single value, either for use here or in the
    //      singleton collection types
    return Collections.singleton(t).stream();
  }

  static <T> Stream<T> of(T... values) {
    return Arrays.stream(values);
  }

  boolean allMatch(Predicate<? super T> predicate);

  boolean anyMatch(Predicate<? super T> predicate);

  <R, A> R collect(Collector<? super T, A, R> collector);

  <R> R collect(
      Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner);

  long count();

  Stream<T> distinct();

  Stream<T> filter(Predicate<? super T> predicate);

  Optional<T> findAny();

  Optional<T> findFirst();

  <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper);

  DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper);

  IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper);

  LongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper);

  void forEach(Consumer<? super T> action);

  void forEachOrdered(Consumer<? super T> action);

  Stream<T> limit(long maxSize);

  <R> Stream<R> map(Function<? super T, ? extends R> mapper);

  DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper);

  IntStream mapToInt(ToIntFunction<? super T> mapper);

  LongStream mapToLong(ToLongFunction<? super T> mapper);

  Optional<T> max(Comparator<? super T> comparator);

  Optional<T> min(Comparator<? super T> comparator);

  boolean noneMatch(Predicate<? super T> predicate);

  Stream<T> peek(Consumer<? super T> action);

  Optional<T> reduce(BinaryOperator<T> accumulator);

  T reduce(T identity, BinaryOperator<T> accumulator);

  <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner);

  Stream<T> skip(long n);

  Stream<T> sorted();

  Stream<T> sorted(Comparator<? super T> comparator);

  Object[] toArray();

  <A> A[] toArray(IntFunction<A[]> generator);
}
