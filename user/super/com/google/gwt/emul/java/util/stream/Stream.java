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

import static javaemul.internal.InternalPreconditions.checkCriticalState;
import static javaemul.internal.InternalPreconditions.checkNotNull;
import static javaemul.internal.InternalPreconditions.checkState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.LongConsumer;
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
   * Value holder for various stream operations.
   */
  static final class ValueConsumer<T> implements Consumer<T> {
    T value;

    @Override
    public void accept(T value) {
      this.value = value;
    }
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
        checkCriticalState(items != null, "Builder already built");
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
    Stream<T> result =
        new StreamSource<T>(
            null,
            new Spliterators.AbstractSpliterator<T>(Long.MAX_VALUE, 0) {
              Stream<? extends T> nextStream;
              Spliterator<? extends T> next;

              @Override
              public boolean tryAdvance(Consumer<? super T> action) {
                // look for a new spliterator
                while (advanceToNextSpliterator()) {
                  // if we have one, try to read and use it
                  if (next.tryAdvance(action)) {
                    return true;
                  } else {
                    nextStream = null;
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
                          nextStream = n;
                          next = n.spliterator();
                        }
                      })) {
                    return false;
                  }
                }
                return true;
              }
            });

    result.onClose(a::close);
    result.onClose(b::close);

    return result;
  }

  static <T> Stream<T> empty() {
    return new EmptyStreamSource<T>(null);
  }

  static <T> Stream<T> generate(Supplier<T> s) {
    return StreamSupport.stream(
        new Spliterators.AbstractSpliterator<T>(
            Long.MAX_VALUE, Spliterator.IMMUTABLE | Spliterator.ORDERED) {
          @Override
          public boolean tryAdvance(Consumer<? super T> action) {
            action.accept(s.get());
            return true;
          }
        },
        false);
  }

  static <T> Stream<T> iterate(T seed, UnaryOperator<T> f) {
    return StreamSupport.stream(
        new Spliterators.AbstractSpliterator<T>(
            Long.MAX_VALUE, Spliterator.IMMUTABLE | Spliterator.ORDERED) {
          private T next = seed;

          @Override
          public boolean tryAdvance(Consumer<? super T> action) {
            action.accept(next);
            next = f.apply(next);
            return true;
          }
        },
        false);
  }

  static <T> Stream<T> of(T t) {
    // TODO consider a splittable that returns only a single value, either for use here or in the
    //      singleton collection types
    return Collections.singleton(t).stream();
  }

  static <T> Stream<T> of(T... values) {
    return Arrays.stream(values);
  }

  /**
   * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.Builder.html">
   * the official Java API doc</a> for details.
   */
  public interface Builder<T> extends Consumer<T> {
    @Override
    void accept(T t);

    default Stream.Builder<T> add(T t) {
      accept(t);
      return this;
    }

    Stream<T> build();
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

  /**
   * Represents an empty stream, doing nothing for all methods.
   */
  static class EmptyStreamSource<T> extends TerminatableStream<EmptyStreamSource<T>>
      implements Stream<T> {

    public EmptyStreamSource(TerminatableStream<?> previous) {
      super(previous);
    }

    @Override
    public Stream<T> filter(Predicate<? super T> predicate) {
      throwIfTerminated();
      return this;
    }

    @Override
    public <R> Stream<R> map(Function<? super T, ? extends R> mapper) {
      throwIfTerminated();
      return (Stream) this;
    }

    @Override
    public IntStream mapToInt(ToIntFunction<? super T> mapper) {
      throwIfTerminated();
      return new IntStream.EmptyIntStreamSource(this);
    }

    @Override
    public LongStream mapToLong(ToLongFunction<? super T> mapper) {
      throwIfTerminated();
      return new LongStream.EmptyLongStreamSource(this);
    }

    @Override
    public DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper) {
      throwIfTerminated();
      return new DoubleStream.EmptyDoubleStreamSource(this);
    }

    @Override
    public <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
      throwIfTerminated();
      return (Stream) this;
    }

    @Override
    public IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper) {
      throwIfTerminated();
      return new IntStream.EmptyIntStreamSource(this);
    }

    @Override
    public LongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper) {
      throwIfTerminated();
      return new LongStream.EmptyLongStreamSource(this);
    }

    @Override
    public DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper) {
      throwIfTerminated();
      return new DoubleStream.EmptyDoubleStreamSource(this);
    }

    @Override
    public Stream<T> distinct() {
      throwIfTerminated();
      return this;
    }

    @Override
    public Stream<T> sorted() {
      throwIfTerminated();
      return this;
    }

    @Override
    public Stream<T> sorted(Comparator<? super T> comparator) {
      throwIfTerminated();
      return this;
    }

    @Override
    public Stream<T> peek(Consumer<? super T> action) {
      throwIfTerminated();
      return this;
    }

    @Override
    public Stream<T> limit(long maxSize) {
      throwIfTerminated();
      checkState(maxSize >= 0, "maxSize may not be negative");
      return this;
    }

    @Override
    public Stream<T> skip(long n) {
      throwIfTerminated();
      checkState(n >= 0, "n may not be negative");
      return this;
    }

    @Override
    public void forEach(Consumer<? super T> action) {
      terminate();
      // nothing to do
    }

    @Override
    public void forEachOrdered(Consumer<? super T> action) {
      terminate();
      // nothing to do
    }

    @Override
    public Object[] toArray() {
      terminate();
      return new Object[0];
    }

    @Override
    public <A> A[] toArray(IntFunction<A[]> generator) {
      terminate();
      return generator.apply(0);
    }

    @Override
    public T reduce(T identity, BinaryOperator<T> accumulator) {
      terminate();
      return identity;
    }

    @Override
    public Optional<T> reduce(BinaryOperator<T> accumulator) {
      terminate();
      return Optional.empty();
    }

    @Override
    public <U> U reduce(
        U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
      terminate();
      return identity;
    }

    @Override
    public <R> R collect(
        Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
      terminate();
      return supplier.get();
    }

    @Override
    public <R, A> R collect(Collector<? super T, A, R> collector) {
      terminate();
      return collector.finisher().apply(collector.supplier().get());
    }

    @Override
    public Optional<T> min(Comparator<? super T> comparator) {
      terminate();
      return Optional.empty();
    }

    @Override
    public Optional<T> max(Comparator<? super T> comparator) {
      terminate();
      return Optional.empty();
    }

    @Override
    public long count() {
      terminate();
      return 0;
    }

    @Override
    public boolean anyMatch(Predicate<? super T> predicate) {
      terminate();
      return false;
    }

    @Override
    public boolean allMatch(Predicate<? super T> predicate) {
      terminate();
      return true;
    }

    @Override
    public boolean noneMatch(Predicate<? super T> predicate) {
      terminate();
      return true;
    }

    @Override
    public Optional<T> findFirst() {
      terminate();
      return Optional.empty();
    }

    @Override
    public Optional<T> findAny() {
      terminate();
      return Optional.empty();
    }

    @Override
    public Iterator<T> iterator() {
      terminate();
      return Collections.emptyIterator();
    }

    @Override
    public Spliterator<T> spliterator() {
      terminate();
      return Spliterators.emptySpliterator();
    }

    @Override
    public boolean isParallel() {
      throwIfTerminated();
      return false;
    }

    @Override
    public Stream<T> sequential() {
      throwIfTerminated();
      return this;
    }

    @Override
    public Stream<T> parallel() {
      throwIfTerminated();
      return this;
    }

    @Override
    public Stream<T> unordered() {
      throwIfTerminated();
      return this;
    }
  }

  /**
   * Object to Object map spliterator.
   * @param <U> the input type
   * @param <T> the output type
   */
  static final class MapToObjSpliterator<U, T> extends Spliterators.AbstractSpliterator<T> {
    private final Function<? super U, ? extends T> map;
    private final Spliterator<U> original;

    public MapToObjSpliterator(Function<? super U, ? extends T> map, Spliterator<U> original) {
      super(
          original.estimateSize(),
          original.characteristics() & ~(Spliterator.SORTED | Spliterator.DISTINCT));
      checkNotNull(map);
      this.map = map;
      this.original = original;
    }

    @Override
    public boolean tryAdvance(final Consumer<? super T> action) {
      return original.tryAdvance(u -> action.accept(map.apply(u)));
    }
  }

  /**
   * Object to Int map spliterator.
   * @param <T> the input type
   */
  static final class MapToIntSpliterator<T> extends Spliterators.AbstractIntSpliterator {
    private final ToIntFunction<? super T> map;
    private final Spliterator<T> original;

    public MapToIntSpliterator(ToIntFunction<? super T> map, Spliterator<T> original) {
      super(
          original.estimateSize(),
          original.characteristics() & ~(Spliterator.SORTED | Spliterator.DISTINCT));
      checkNotNull(map);
      this.map = map;
      this.original = original;
    }

    @Override
    public boolean tryAdvance(final IntConsumer action) {
      return original.tryAdvance(u -> action.accept(map.applyAsInt(u)));
    }
  }

  /**
   * Object to Long map spliterator.
   * @param <T> the input type
   */
  static final class MapToLongSpliterator<T> extends Spliterators.AbstractLongSpliterator {
    private final ToLongFunction<? super T> map;
    private final Spliterator<T> original;

    public MapToLongSpliterator(ToLongFunction<? super T> map, Spliterator<T> original) {
      super(
          original.estimateSize(),
          original.characteristics() & ~(Spliterator.SORTED | Spliterator.DISTINCT));
      checkNotNull(map);
      this.map = map;
      this.original = original;
    }

    @Override
    public boolean tryAdvance(final LongConsumer action) {
      return original.tryAdvance(u -> action.accept(map.applyAsLong(u)));
    }
  }

  /**
   * Object to Double map spliterator.
   * @param <T> the input type
   */
  static final class MapToDoubleSpliterator<T> extends Spliterators.AbstractDoubleSpliterator {
    private final ToDoubleFunction<? super T> map;
    private final Spliterator<T> original;

    public MapToDoubleSpliterator(ToDoubleFunction<? super T> map, Spliterator<T> original) {
      super(
          original.estimateSize(),
          original.characteristics() & ~(Spliterator.SORTED | Spliterator.DISTINCT));
      checkNotNull(map);
      this.map = map;
      this.original = original;
    }

    @Override
    public boolean tryAdvance(final DoubleConsumer action) {
      return original.tryAdvance(u -> action.accept(map.applyAsDouble(u)));
    }
  }

  /**
   * Object filter spliterator.
   * @param <T> the type of data to iterate over
   */
  static final class FilterSpliterator<T> extends Spliterators.AbstractSpliterator<T> {
    private final Predicate<? super T> filter;
    private final Spliterator<T> original;

    private boolean found;

    public FilterSpliterator(Predicate<? super T> filter, Spliterator<T> original) {
      super(original.estimateSize(), original.characteristics() & ~Spliterator.SIZED);
      checkNotNull(filter);
      this.filter = filter;
      this.original = original;
    }

    @Override
    public Comparator<? super T> getComparator() {
      return original.getComparator();
    }

    @Override
    public boolean tryAdvance(final Consumer<? super T> action) {
      found = false;
      while (!found
          && original.tryAdvance(
              item -> {
                if (filter.test(item)) {
                  found = true;
                  action.accept(item);
                }
              })) {
        // do nothing, work is done in tryAdvance
      }

      return found;
    }
  }

  /**
   * Object skip spliterator.
   * @param <T> the type of data to iterate over
   */
  static final class SkipSpliterator<T> extends Spliterators.AbstractSpliterator<T> {
    private long skip;
    private final Spliterator<T> original;

    public SkipSpliterator(long skip, Spliterator<T> original) {
      super(
          original.hasCharacteristics(Spliterator.SIZED)
              ? Math.max(0, original.estimateSize() - skip)
              : Long.MAX_VALUE,
          original.characteristics());
      this.skip = skip;
      this.original = original;
    }

    @Override
    public Comparator<? super T> getComparator() {
      return original.getComparator();
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
      while (skip > 0) {
        if (!original.tryAdvance(ignore -> {})) {
          return false;
        }
        skip--;
      }
      return original.tryAdvance(action);
    }
  }

  /**
   * Object limit spliterator.
   * @param <T> the type of data to iterate over
   */
  static final class LimitSpliterator<T> extends Spliterators.AbstractSpliterator<T> {
    private final long limit;
    private final Spliterator<T> original;
    private int position = 0;

    public LimitSpliterator(long limit, Spliterator<T> original) {
      super(
          original.hasCharacteristics(Spliterator.SIZED)
              ? Math.min(original.estimateSize(), limit)
              : Long.MAX_VALUE,
          original.characteristics());
      this.limit = limit;
      this.original = original;
    }

    @Override
    public Comparator<? super T> getComparator() {
      return original.getComparator();
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
      if (limit <= position) {
        return false;
      }
      boolean result = original.tryAdvance(action);
      position++;
      return result;
    }
  }

  /**
   * Main implementation of Stream, wrapping a single spliterator and an optional parent stream.
   * @param <T>
   */
  static class StreamSource<T> extends TerminatableStream<StreamSource<T>> implements Stream<T> {
    private final Spliterator<T> spliterator;

    public StreamSource(TerminatableStream<?> prev, Spliterator<T> spliterator) {
      super(prev);
      this.spliterator = spliterator;
    }

    // terminal
    @Override
    public Spliterator<T> spliterator() {
      terminate();
      return spliterator;
    }

    @Override
    public Iterator<T> iterator() {
      return Spliterators.iterator(spliterator());
    }

    @Override
    public long count() {
      terminate();
      long count = 0;
      while (spliterator.tryAdvance(a -> {})) {
        count++;
      }
      return count;
    }

    @Override
    public void forEach(Consumer<? super T> action) {
      forEachOrdered(action);
    }

    @Override
    public void forEachOrdered(Consumer<? super T> action) {
      terminate();
      spliterator.forEachRemaining(action);
    }

    @Override
    public Object[] toArray() {
      return toArray(Object[]::new);
    }

    @Override
    public <A> A[] toArray(IntFunction<A[]> generator) {
      List<T> collected = collect(Collectors.toList());
      return collected.toArray(generator.apply(collected.size()));
    }

    @Override
    public <R> R collect(
        Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
      return collect(
          Collector.of(
              supplier,
              accumulator,
              (a, b) -> {
                combiner.accept(a, b);
                return a;
              }));
    }

    @Override
    public <R, A> R collect(final Collector<? super T, A, R> collector) {
      return collector
          .finisher()
          .apply(
              reduce(
                  collector.supplier().get(),
                  (a, t) -> {
                    collector.accumulator().accept(a, t);
                    return a;
                  },
                  collector.combiner()));
    }

    @Override
    public Optional<T> findFirst() {
      terminate();
      ValueConsumer<T> holder = new ValueConsumer<T>();
      if (spliterator.tryAdvance(holder)) {
        return Optional.of(holder.value);
      }
      return Optional.empty();
    }

    @Override
    public Optional<T> findAny() {
      return findFirst();
    }

    @Override
    public boolean anyMatch(Predicate<? super T> predicate) {
      return filter(predicate).findFirst().isPresent();
    }

    @Override
    public boolean allMatch(final Predicate<? super T> predicate) {
      return !anyMatch(predicate.negate());
    }

    @Override
    public boolean noneMatch(final Predicate<? super T> predicate) {
      return !anyMatch(predicate);
    }

    @Override
    public Optional<T> min(final Comparator<? super T> comparator) {
      return reduce(BinaryOperator.minBy(comparator));
    }

    @Override
    public Optional<T> max(final Comparator<? super T> comparator) {
      return reduce(BinaryOperator.maxBy(comparator));
    }

    @Override
    public T reduce(T identity, BinaryOperator<T> accumulator) {
      return reduce(identity, accumulator, accumulator);
    }

    @Override
    public Optional<T> reduce(BinaryOperator<T> accumulator) {
      ValueConsumer<T> consumer = new ValueConsumer<T>();
      if (!spliterator.tryAdvance(consumer)) {
        terminate();
        return Optional.empty();
      }
      return Optional.of(reduce(consumer.value, accumulator));
    }

    // combiner is ignored, since we don't parallelize
    @Override
    public <U> U reduce(
        U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
      terminate();
      final ValueConsumer<U> consumer = new ValueConsumer<U>();
      consumer.value = identity;
      spliterator.forEachRemaining(
          item -> {
            consumer.accept(accumulator.apply(consumer.value, item));
          });
      return consumer.value;
    }
    // end terminal

    // intermediate
    @Override
    public Stream<T> filter(Predicate<? super T> predicate) {
      throwIfTerminated();
      return new StreamSource<>(this, new FilterSpliterator<>(predicate, spliterator));
    }

    @Override
    public <R> Stream<R> map(Function<? super T, ? extends R> mapper) {
      throwIfTerminated();
      return new StreamSource<>(this, new MapToObjSpliterator<>(mapper, spliterator));
    }

    @Override
    public IntStream mapToInt(ToIntFunction<? super T> mapper) {
      throwIfTerminated();
      return new IntStream.IntStreamSource(this, new MapToIntSpliterator<>(mapper, spliterator));
    }

    @Override
    public LongStream mapToLong(ToLongFunction<? super T> mapper) {
      throwIfTerminated();
      return new LongStream.LongStreamSource(this, new MapToLongSpliterator<>(mapper, spliterator));
    }

    @Override
    public DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper) {
      throwIfTerminated();
      return new DoubleStream.DoubleStreamSource(
          this, new MapToDoubleSpliterator<>(mapper, spliterator));
    }

    @Override
    public <R> Stream<R> flatMap(final Function<? super T, ? extends Stream<? extends R>> mapper) {
      throwIfTerminated();
      final Spliterator<? extends Stream<? extends R>> spliteratorOfStreams =
          new MapToObjSpliterator<>(mapper, spliterator);
      return new StreamSource<R>(
          this,
          new Spliterators.AbstractSpliterator<R>(Long.MAX_VALUE, 0) {
            Stream<? extends R> nextStream;
            Spliterator<? extends R> next;

            @Override
            public boolean tryAdvance(Consumer<? super R> action) {
              // look for a new spliterator
              while (advanceToNextSpliterator()) {
                // if we have one, try to read and use it
                if (next.tryAdvance(action)) {
                  return true;
                } else {
                  nextStream.close();
                  nextStream = null;
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
                        nextStream = n;
                        next = n.spliterator();
                      }
                    })) {
                  return false;
                }
              }
              return true;
            }
          });
    }

    @Override
    public IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper) {
      throwIfTerminated();
      final Spliterator<? extends IntStream> spliteratorOfStreams =
          new MapToObjSpliterator<>(mapper, spliterator);
      return new IntStream.IntStreamSource(
          this,
          new Spliterators.AbstractIntSpliterator(Long.MAX_VALUE, 0) {
            IntStream nextStream;
            Spliterator.OfInt next;

            @Override
            public boolean tryAdvance(IntConsumer action) {
              // look for a new spliterator
              while (advanceToNextSpliterator()) {
                // if we have one, try to read and use it
                if (next.tryAdvance(action)) {
                  return true;
                } else {
                  nextStream.close();
                  nextStream = null;
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
                        nextStream = n;
                        next = n.spliterator();
                      }
                    })) {
                  return false;
                }
              }
              return true;
            }
          });
    }

    @Override
    public LongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper) {
      throwIfTerminated();
      final Spliterator<? extends LongStream> spliteratorOfStreams =
          new MapToObjSpliterator<>(mapper, spliterator);
      return new LongStream.LongStreamSource(
          this,
          new Spliterators.AbstractLongSpliterator(Long.MAX_VALUE, 0) {
            LongStream nextStream;
            Spliterator.OfLong next;

            @Override
            public boolean tryAdvance(LongConsumer action) {
              // look for a new spliterator
              while (advanceToNextSpliterator()) {
                // if we have one, try to read and use it
                if (next.tryAdvance(action)) {
                  return true;
                } else {
                  nextStream.close();
                  nextStream = null;
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
                        nextStream = n;
                        next = n.spliterator();
                      }
                    })) {
                  return false;
                }
              }
              return true;
            }
          });
    }

    @Override
    public DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper) {
      throwIfTerminated();
      final Spliterator<? extends DoubleStream> spliteratorOfStreams =
          new MapToObjSpliterator<>(mapper, spliterator);
      return new DoubleStream.DoubleStreamSource(
          this,
          new Spliterators.AbstractDoubleSpliterator(Long.MAX_VALUE, 0) {
            DoubleStream nextStream;
            Spliterator.OfDouble next;

            @Override
            public boolean tryAdvance(DoubleConsumer action) {
              // look for a new spliterator
              while (advanceToNextSpliterator()) {
                // if we have one, try to read and use it
                if (next.tryAdvance(action)) {
                  return true;
                } else {
                  nextStream.close();
                  nextStream = null;
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
                        nextStream = n;
                        next = n.spliterator();
                      }
                    })) {
                  return false;
                }
              }
              return true;
            }
          });
    }

    @Override
    public Stream<T> distinct() {
      throwIfTerminated();
      HashSet<T> seen = new HashSet<>();
      return filter(seen::add);
    }

    @Override
    public Stream<T> sorted() {
      throwIfTerminated();
      Comparator<T> c = (Comparator) Comparator.naturalOrder();
      return sorted(c);
    }

    @Override
    public Stream<T> sorted(final Comparator<? super T> comparator) {
      throwIfTerminated();
      return new StreamSource<>(
          this,
          new Spliterators.AbstractSpliterator<T>(
              spliterator.estimateSize(), spliterator.characteristics() | Spliterator.SORTED) {
            Spliterator<T> ordered = null;

            @Override
            public Comparator<? super T> getComparator() {
              return comparator == Comparator.naturalOrder() ? null : comparator;
            }

            @Override
            public boolean tryAdvance(Consumer<? super T> action) {
              if (ordered == null) {
                List<T> list = new ArrayList<>();
                spliterator.forEachRemaining(list::add);
                Collections.sort(list, comparator);
                ordered = list.spliterator();
              }
              return ordered.tryAdvance(action);
            }
          });
    }

    @Override
    public Stream<T> peek(final Consumer<? super T> action) {
      checkNotNull(action);
      throwIfTerminated();
      return new StreamSource<>(
          this,
          new Spliterators.AbstractSpliterator<T>(
              spliterator.estimateSize(), spliterator.characteristics()) {
            @Override
            public boolean tryAdvance(final Consumer<? super T> innerAction) {
              return spliterator.tryAdvance(
                  item -> {
                    action.accept(item);
                    innerAction.accept(item);
                  });
            }
          });
    }

    @Override
    public Stream<T> limit(long maxSize) {
      throwIfTerminated();
      checkState(maxSize >= 0, "maxSize may not be negative");
      return new StreamSource<>(this, new LimitSpliterator<>(maxSize, spliterator));
    }

    @Override
    public Stream<T> skip(long n) {
      throwIfTerminated();
      checkState(n >= 0, "n may not be negative");
      if (n == 0) {
        return this;
      }
      return new StreamSource<>(this, new SkipSpliterator<>(n, spliterator));
    }

    @Override
    public boolean isParallel() {
      throwIfTerminated();
      return false;
    }

    @Override
    public Stream<T> sequential() {
      throwIfTerminated();
      return this;
    }

    @Override
    public Stream<T> parallel() {
      throwIfTerminated();
      // do nothing, no such thing as gwt+parallel
      return this;
    }

    @Override
    public Stream<T> unordered() {
      throwIfTerminated();
      return this;
    }
  }
}
