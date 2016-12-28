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
import static javaemul.internal.InternalPreconditions.checkState;

import java.util.Arrays;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.HashSet;
import java.util.OptionalDouble;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;

/**
 * Main implementation of DoubleStream, wrapping a single spliterator, and an optional parent
 * stream.
 */
final class DoubleStreamImpl extends TerminatableStream<DoubleStreamImpl> implements DoubleStream {

  /**
   * Represents an empty stream, doing nothing for all methods.
   */
  static class Empty extends TerminatableStream<Empty> implements DoubleStream {
    public Empty(TerminatableStream<?> previous) {
      super(previous);
    }

    @Override
    public DoubleStream filter(DoublePredicate predicate) {
      throwIfTerminated();
      return this;
    }

    @Override
    public DoubleStream map(DoubleUnaryOperator mapper) {
      throwIfTerminated();
      return this;
    }

    @Override
    public <U> Stream<U> mapToObj(DoubleFunction<? extends U> mapper) {
      throwIfTerminated();
      return new StreamImpl.Empty<U>(this);
    }

    @Override
    public IntStream mapToInt(DoubleToIntFunction mapper) {
      throwIfTerminated();
      return new IntStreamImpl.Empty(this);
    }

    @Override
    public LongStream mapToLong(DoubleToLongFunction mapper) {
      throwIfTerminated();
      return new LongStreamImpl.Empty(this);
    }

    @Override
    public DoubleStream flatMap(DoubleFunction<? extends DoubleStream> mapper) {
      throwIfTerminated();
      return this;
    }

    @Override
    public DoubleStream distinct() {
      throwIfTerminated();
      return this;
    }

    @Override
    public DoubleStream sorted() {
      throwIfTerminated();
      return this;
    }

    @Override
    public DoubleStream peek(DoubleConsumer action) {
      throwIfTerminated();
      return this;
    }

    @Override
    public DoubleStream limit(long maxSize) {
      throwIfTerminated();
      checkState(maxSize >= 0, "maxSize may not be negative");
      return this;
    }

    @Override
    public DoubleStream skip(long n) {
      throwIfTerminated();
      checkState(n >= 0, "n may not be negative");
      return this;
    }

    @Override
    public void forEach(DoubleConsumer action) {
      terminate();
      // do nothing
    }

    @Override
    public void forEachOrdered(DoubleConsumer action) {
      terminate();
      // do nothing
    }

    @Override
    public double[] toArray() {
      terminate();
      return new double[0];
    }

    @Override
    public double reduce(double identity, DoubleBinaryOperator op) {
      terminate();
      return identity;
    }

    @Override
    public OptionalDouble reduce(DoubleBinaryOperator op) {
      terminate();
      return OptionalDouble.empty();
    }

    @Override
    public <R> R collect(
        Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner) {
      terminate();
      return supplier.get();
    }

    @Override
    public double sum() {
      terminate();
      return 0;
    }

    @Override
    public OptionalDouble min() {
      terminate();
      return OptionalDouble.empty();
    }

    @Override
    public OptionalDouble max() {
      terminate();
      return OptionalDouble.empty();
    }

    @Override
    public long count() {
      terminate();
      return 0;
    }

    @Override
    public OptionalDouble average() {
      terminate();
      return OptionalDouble.empty();
    }

    @Override
    public DoubleSummaryStatistics summaryStatistics() {
      terminate();
      return new DoubleSummaryStatistics();
    }

    @Override
    public boolean anyMatch(DoublePredicate predicate) {
      terminate();
      return false;
    }

    @Override
    public boolean allMatch(DoublePredicate predicate) {
      terminate();
      return true;
    }

    @Override
    public boolean noneMatch(DoublePredicate predicate) {
      terminate();
      return true;
    }

    @Override
    public OptionalDouble findFirst() {
      terminate();
      return OptionalDouble.empty();
    }

    @Override
    public OptionalDouble findAny() {
      terminate();
      return OptionalDouble.empty();
    }

    @Override
    public Stream<Double> boxed() {
      throwIfTerminated();
      return new StreamImpl.Empty<Double>(this);
    }

    @Override
    public DoubleStream sequential() {
      throwIfTerminated();
      return this;
    }

    @Override
    public DoubleStream parallel() {
      throwIfTerminated();
      return this;
    }

    @Override
    public PrimitiveIterator.OfDouble iterator() {
      return Spliterators.iterator(spliterator());
    }

    @Override
    public Spliterator.OfDouble spliterator() {
      terminate();
      return Spliterators.emptyDoubleSpliterator();
    }

    @Override
    public boolean isParallel() {
      throwIfTerminated();
      return false;
    }

    @Override
    public DoubleStream unordered() {
      throwIfTerminated();
      return this;
    }
  }

  /**
   * Double to Int map spliterator.
   */
  private static final class MapToIntSpliterator extends Spliterators.AbstractIntSpliterator {
    private final DoubleToIntFunction map;
    private final Spliterator.OfDouble original;

    public MapToIntSpliterator(DoubleToIntFunction map, Spliterator.OfDouble original) {
      super(
          original.estimateSize(),
          original.characteristics() & ~(Spliterator.SORTED | Spliterator.DISTINCT));
      checkNotNull(map);
      this.map = map;
      this.original = original;
    }

    @Override
    public boolean tryAdvance(final IntConsumer action) {
      return original.tryAdvance((double u) -> action.accept(map.applyAsInt(u)));
    }
  }

  /**
   * Double to Object map spliterator.
   *
   * @param <T> the type of Object in the spliterator
   */
  private static final class MapToObjSpliterator<T> extends Spliterators.AbstractSpliterator<T> {
    private final DoubleFunction<? extends T> map;
    private final Spliterator.OfDouble original;

    public MapToObjSpliterator(DoubleFunction<? extends T> map, Spliterator.OfDouble original) {
      super(
          original.estimateSize(),
          original.characteristics() & ~(Spliterator.SORTED | Spliterator.DISTINCT));
      checkNotNull(map);
      this.map = map;
      this.original = original;
    }

    @Override
    public boolean tryAdvance(final Consumer<? super T> action) {
      return original.tryAdvance((double u) -> action.accept(map.apply(u)));
    }
  }

  /**
   * Double to Long map spliterator.
   */
  private static final class MapToLongSpliterator extends Spliterators.AbstractLongSpliterator {
    private final DoubleToLongFunction map;
    private final Spliterator.OfDouble original;

    public MapToLongSpliterator(DoubleToLongFunction map, Spliterator.OfDouble original) {
      super(
          original.estimateSize(),
          original.characteristics() & ~(Spliterator.SORTED | Spliterator.DISTINCT));
      checkNotNull(map);
      this.map = map;
      this.original = original;
    }

    @Override
    public boolean tryAdvance(final LongConsumer action) {
      return original.tryAdvance((double u) -> action.accept(map.applyAsLong(u)));
    }
  }

  /**
   * Double to Double map spliterator.
   */
  private static final class MapToDoubleSpliterator extends Spliterators.AbstractDoubleSpliterator {
    private final DoubleUnaryOperator map;
    private final Spliterator.OfDouble original;

    public MapToDoubleSpliterator(DoubleUnaryOperator map, Spliterator.OfDouble original) {
      super(
          original.estimateSize(),
          original.characteristics() & ~(Spliterator.SORTED | Spliterator.DISTINCT));
      checkNotNull(map);
      this.map = map;
      this.original = original;
    }

    @Override
    public boolean tryAdvance(final DoubleConsumer action) {
      return original.tryAdvance((double u) -> action.accept(map.applyAsDouble(u)));
    }
  }

  /**
   * Double filter spliterator.
   */
  private static final class FilterSpliterator extends Spliterators.AbstractDoubleSpliterator {
    private final DoublePredicate filter;
    private final Spliterator.OfDouble original;

    private boolean found;

    public FilterSpliterator(DoublePredicate filter, Spliterator.OfDouble original) {
      super(original.estimateSize(), original.characteristics() & ~Spliterator.SIZED);
      checkNotNull(filter);
      this.filter = filter;
      this.original = original;
    }

    @Override
    public Comparator<? super Double> getComparator() {
      return original.getComparator();
    }

    @Override
    public boolean tryAdvance(final DoubleConsumer action) {
      found = false;
      while (!found
          && original.tryAdvance(
              (double item) -> {
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
   * Double skip spliterator.
   */
  private static final class SkipSpliterator extends Spliterators.AbstractDoubleSpliterator {
    private long skip;
    private final Spliterator.OfDouble original;

    public SkipSpliterator(long skip, Spliterator.OfDouble original) {
      super(
          original.hasCharacteristics(Spliterator.SIZED)
              ? Math.max(0, original.estimateSize() - skip)
              : Long.MAX_VALUE,
          original.characteristics());
      this.skip = skip;
      this.original = original;
    }

    @Override
    public Comparator<? super Double> getComparator() {
      return original.getComparator();
    }

    @Override
    public boolean tryAdvance(DoubleConsumer action) {
      while (skip > 0) {
        if (!original.tryAdvance((double ignore) -> { })) {
          return false;
        }
        skip--;
      }
      return original.tryAdvance(action);
    }
  }

  /**
   * Double limit spliterator.
   */
  private static final class LimitSpliterator extends Spliterators.AbstractDoubleSpliterator {
    private final long limit;
    private final Spliterator.OfDouble original;
    private int position = 0;

    public LimitSpliterator(long limit, Spliterator.OfDouble original) {
      super(
          original.hasCharacteristics(Spliterator.SIZED)
              ? Math.min(original.estimateSize(), limit)
              : Long.MAX_VALUE,
          original.characteristics());
      this.limit = limit;
      this.original = original;
    }

    @Override
    public Comparator<? super Double> getComparator() {
      return original.getComparator();
    }

    @Override
    public boolean tryAdvance(DoubleConsumer action) {
      if (position >= limit) {
        return false;
      }

      boolean result = original.tryAdvance(action);
      position++;
      return result;
    }
  }

  /**
   * Value holder for various stream operations.
   */
  private static class ValueConsumer implements DoubleConsumer {
    double value;

    @Override
    public void accept(double value) {
      this.value = value;
    }
  }

  private final Spliterator.OfDouble spliterator;

  public DoubleStreamImpl(TerminatableStream<?> previous, Spliterator.OfDouble spliterator) {
    super(previous);
    this.spliterator = spliterator;
  }

  // terminals

  @Override
  public void forEach(DoubleConsumer action) {
    forEachOrdered(action);
  }

  @Override
  public void forEachOrdered(DoubleConsumer action) {
    terminate();
    spliterator.forEachRemaining(action);
  }

  @Override
  public double[] toArray() {
    terminate();
    double[] entries = new double[0];
    // this is legal in js, since the array will be backed by a JS array
    spliterator.forEachRemaining((double value) -> entries[entries.length] = value);

    return entries;
  }

  @Override
  public double reduce(double identity, DoubleBinaryOperator op) {
    terminate();
    ValueConsumer holder = new ValueConsumer();
    holder.value = identity;
    spliterator.forEachRemaining(
        (double value) -> {
          holder.accept(op.applyAsDouble(holder.value, value));
        });
    return holder.value;
  }

  @Override
  public OptionalDouble reduce(DoubleBinaryOperator op) {
    ValueConsumer holder = new ValueConsumer();
    if (spliterator.tryAdvance(holder)) {
      return OptionalDouble.of(reduce(holder.value, op));
    }
    terminate();
    return OptionalDouble.empty();
  }

  @Override
  public <R> R collect(
      Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner) {
    terminate();
    final R acc = supplier.get();
    spliterator.forEachRemaining((double value) -> accumulator.accept(acc, value));
    return acc;
  }

  @Override
  public double sum() {
    return summaryStatistics().getSum();
  }

  @Override
  public OptionalDouble min() {
    DoubleSummaryStatistics stats = summaryStatistics();
    if (stats.getCount() == 0) {
      return OptionalDouble.empty();
    }
    return OptionalDouble.of(stats.getMin());
  }

  @Override
  public OptionalDouble max() {
    DoubleSummaryStatistics stats = summaryStatistics();
    if (stats.getCount() == 0) {
      return OptionalDouble.empty();
    }
    return OptionalDouble.of(stats.getMax());
  }

  @Override
  public long count() {
    terminate();
    long count = 0;
    while (spliterator.tryAdvance((double value) -> { })) {
      count++;
    }
    return count;
  }

  @Override
  public OptionalDouble average() {
    DoubleSummaryStatistics stats = summaryStatistics();
    if (stats.getCount() == 0) {
      return OptionalDouble.empty();
    }
    return OptionalDouble.of(stats.getAverage());
  }

  @Override
  public DoubleSummaryStatistics summaryStatistics() {
    return collect(
        DoubleSummaryStatistics::new,
        // TODO switch to a lambda reference once #9340 is fixed
        (doubleSummaryStatistics, value) -> doubleSummaryStatistics.accept(value),
        DoubleSummaryStatistics::combine);
  }

  @Override
  public boolean anyMatch(DoublePredicate predicate) {
    return filter(predicate).findFirst().isPresent();
  }

  @Override
  public boolean allMatch(DoublePredicate predicate) {
    return !anyMatch(predicate.negate());
  }

  @Override
  public boolean noneMatch(DoublePredicate predicate) {
    return !anyMatch(predicate);
  }

  @Override
  public OptionalDouble findFirst() {
    terminate();
    ValueConsumer holder = new ValueConsumer();
    if (spliterator.tryAdvance(holder)) {
      return OptionalDouble.of(holder.value);
    }
    return OptionalDouble.empty();
  }

  @Override
  public OptionalDouble findAny() {
    return findFirst();
  }

  @Override
  public PrimitiveIterator.OfDouble iterator() {
    return Spliterators.iterator(spliterator());
  }

  @Override
  public Spliterator.OfDouble spliterator() {
    terminate();
    return spliterator;
  }

  // end terminals

  // intermediates

  @Override
  public DoubleStream filter(DoublePredicate predicate) {
    throwIfTerminated();
    return new DoubleStreamImpl(this, new FilterSpliterator(predicate, spliterator));
  }

  @Override
  public DoubleStream map(DoubleUnaryOperator mapper) {
    throwIfTerminated();
    return new DoubleStreamImpl(this, new MapToDoubleSpliterator(mapper, spliterator));
  }

  @Override
  public <U> Stream<U> mapToObj(DoubleFunction<? extends U> mapper) {
    throwIfTerminated();
    return new StreamImpl<U>(this, new MapToObjSpliterator<U>(mapper, spliterator));
  }

  @Override
  public IntStream mapToInt(DoubleToIntFunction mapper) {
    throwIfTerminated();
    return new IntStreamImpl(this, new MapToIntSpliterator(mapper, spliterator));
  }

  @Override
  public LongStream mapToLong(DoubleToLongFunction mapper) {
    throwIfTerminated();
    return new LongStreamImpl(this, new MapToLongSpliterator(mapper, spliterator));
  }

  @Override
  public DoubleStream flatMap(DoubleFunction<? extends DoubleStream> mapper) {
    throwIfTerminated();
    final Spliterator<? extends DoubleStream> spliteratorOfStreams =
        new MapToObjSpliterator<DoubleStream>(mapper, spliterator);

    Spliterator.OfDouble flatMapSpliterator =
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
        };

    return new DoubleStreamImpl(this, flatMapSpliterator);
  }

  @Override
  public DoubleStream distinct() {
    throwIfTerminated();
    HashSet<Double> seen = new HashSet<>();
    return filter(seen::add);
  }

  @Override
  public DoubleStream sorted() {
    throwIfTerminated();

    Spliterator.OfDouble sortingSpliterator =
        new Spliterators.AbstractDoubleSpliterator(
            spliterator.estimateSize(), spliterator.characteristics() | Spliterator.SORTED) {
          Spliterator.OfDouble ordered = null;

          @Override
          public Comparator<? super Double> getComparator() {
            return null;
          }

          @Override
          public boolean tryAdvance(DoubleConsumer action) {
            if (ordered == null) {
              double[] list = new double[0];
              spliterator.forEachRemaining((double item) -> list[list.length] = item);
              Arrays.sort(list);
              ordered = Spliterators.spliterator(list, characteristics());
            }
            return ordered.tryAdvance(action);
          }
        };

    return new DoubleStreamImpl(this, sortingSpliterator);
  }

  @Override
  public DoubleStream peek(DoubleConsumer action) {
    checkNotNull(action);
    throwIfTerminated();

    Spliterator.OfDouble peekSpliterator =
        new Spliterators.AbstractDoubleSpliterator(
            spliterator.estimateSize(), spliterator.characteristics()) {
          @Override
          public boolean tryAdvance(final DoubleConsumer innerAction) {
            return spliterator.tryAdvance(action.andThen(innerAction));
          }
        };

    return new DoubleStreamImpl(this, peekSpliterator);
  }

  @Override
  public DoubleStream limit(long maxSize) {
    throwIfTerminated();
    checkState(maxSize >= 0, "maxSize may not be negative");
    return new DoubleStreamImpl(this, new LimitSpliterator(maxSize, spliterator));
  }

  @Override
  public DoubleStream skip(long n) {
    throwIfTerminated();
    checkState(n >= 0, "n may not be negative");
    if (n == 0) {
      return this;
    }
    return new DoubleStreamImpl(this, new SkipSpliterator(n, spliterator));
  }

  @Override
  public Stream<Double> boxed() {
    return mapToObj(Double::valueOf);
  }

  @Override
  public DoubleStream sequential() {
    throwIfTerminated();
    return this;
  }

  @Override
  public DoubleStream parallel() {
    throwIfTerminated();
    return this;
  }

  @Override
  public boolean isParallel() {
    throwIfTerminated();
    return false;
  }

  @Override
  public DoubleStream unordered() {
    throwIfTerminated();
    return this;
  }
}
