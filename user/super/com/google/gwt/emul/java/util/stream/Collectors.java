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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IntSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

/**
 * See <a
 * href="https://docs.oracle.com/javase/8/docs/api/java/util/stream/Collectors.html">the
 * official Java API doc</a> for details.
 */
public final class Collectors {
  public static <T> Collector<T,?,Double> averagingDouble(ToDoubleFunction<? super T> mapper) {
    // TODO simplify to only collect average if possible
    return collectingAndThen(summarizingDouble(mapper), DoubleSummaryStatistics::getAverage);
  }

  public static <T> Collector<T,?,Double> averagingInt(ToIntFunction<? super T> mapper) {
    // TODO simplify to only collect average if possible
    return collectingAndThen(summarizingInt(mapper), IntSummaryStatistics::getAverage);
  }

  public static <T> Collector<T,?,Double> averagingLong(ToLongFunction<? super T> mapper) {
    // TODO simplify to only collect average if possible
    return collectingAndThen(summarizingLong(mapper), LongSummaryStatistics::getAverage);
  }

  public static <T, A, R, RR> Collector<T, A, RR> collectingAndThen(
      Collector<T, A, R> downstream, Function<R, RR> finisher) {
    return new Collector.CollectorImpl<>(
        downstream.supplier(),
        downstream.accumulator(),
        downstream.combiner(),
        downstream.finisher().andThen(finisher),
        removeIdentFinisher(downstream.characteristics()));
  }

  public static <T> Collector<T,?,Long> counting() {
    // Using Long::sum here fails in JDT
    return reducing(0L, item -> 1L, (a, b) -> (Long) a.longValue() + b.longValue());
  }

  public static <T, K> Collector<T, ?, Map<K, List<T>>> groupingBy(
      Function<? super T, ? extends K> classifier) {
    // TODO inline this and avoid the finisher extra work of copying from a map to another map
    //      kept separate for now to unify implementations and reduce testing required
    return groupingBy(classifier, toList());
  }

  public static <T, K, A, D> Collector<T, ?, Map<K, D>> groupingBy(
      Function<? super T, ? extends K> classifier, Collector<? super T, A, D> downstream) {
    return groupingBy(classifier, HashMap::new, downstream);
  }

  public static <T, K, D, A, M extends Map<K, D>> Collector<T, ?, M> groupingBy(
      Function<? super T, ? extends K> classifier,
      Supplier<M> mapFactory,
      Collector<? super T, A, D> downstream) {
    return Collector.<T, Map<K, List<T>>, M>of(
        LinkedHashMap::new,
        (m, o) -> {
          K k = classifier.apply(o);
          List<T> l = m.get(k);
          if (l == null) {
            l = new ArrayList<>();
            m.put(k, l);
          }
          l.add(o);

        },
        (m1, m2) -> mergeAll(m1, m2, Collectors::addAll),
        m -> {
          M result = mapFactory.get();
          for (Map.Entry<K, List<T>> entry : m.entrySet()) {
            result.put(entry.getKey(), streamAndCollect(downstream, entry.getValue()));
          }
          return result;
        });
  }

//  not supported
//  public static <T,K> Collector<T,?,ConcurrentMap<K,List<T>>> groupingByConcurrent(
//      Function<? super T,? extends K> classifier)
//  public static <T,K,A,D> Collector<T,?,ConcurrentMap<K,D>> groupingByConcurrent(
//      Function<? super T,? extends K> classifier, Collector<? super T,A,D> downstream)
//  public static <T,K,A,D,M extends ConcurrentMap<K,D>> Collector<T,?,M> groupingByConcurrent(
//      Function<? super T,? extends K> classifier, Supplier<M> mapFactory,
//      Collector<? super T,A,D> downstream)

  public static Collector<CharSequence,?,String> joining() {
    // specific implementation rather than calling joining("") since we don't need to worry about
    // appending delimiters between empty strings
    return Collector.of(
        StringBuilder::new,
        StringBuilder::append,
        StringBuilder::append,
        StringBuilder::toString
    );
  }

  public static Collector<CharSequence,?,String> joining(CharSequence delimiter) {
    return joining(delimiter, "", "");
  }

  public static Collector<CharSequence, ?, String> joining(
      final CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
    return Collector.of(
        () -> new StringJoiner(delimiter, prefix, suffix),
        StringJoiner::add,
        StringJoiner::merge,
        StringJoiner::toString
    );
  }

  public static <T, U, A, R> Collector<T, ?, R> mapping(
      final Function<? super T, ? extends U> mapper, final Collector<? super U, A, R> downstream) {
    return new Collector.CollectorImpl<>(
        downstream.supplier(),
        (BiConsumer<A, T>) (A a, T t) -> {
          downstream.accumulator().accept(a, mapper.apply(t));
        },
        downstream.combiner(),
        downstream.finisher(),
        downstream.characteristics()
    );
  }

  public static <T> Collector<T,?,Optional<T>> maxBy(Comparator<? super T> comparator) {
    return minBy(comparator.reversed());
  }

  public static <T> Collector<T,?,Optional<T>> minBy(final Comparator<? super T> comparator) {
    return reducing((a, b) -> comparator.compare(a, b) < 0 ? a : b);
  }

  public static <T> Collector<T, ?, Map<Boolean, List<T>>> partitioningBy(
      Predicate<? super T> predicate) {
    // calling groupBy directly rather than partioningBy so that it can be optimized later
    return groupingBy(predicate::test);
  }

  public static <T, D, A> Collector<T, ?, Map<Boolean, D>> partitioningBy(
      Predicate<? super T> predicate, Collector<? super T, A, D> downstream) {
    return groupingBy(predicate::test, downstream);
  }

  public static <T> Collector<T,?,Optional<T>> reducing(BinaryOperator<T> op) {
    return reducing(Optional.empty(), Optional::of, (a, b) -> {
      if (!a.isPresent()) {
        return b;
      }
      if (!b.isPresent()) {
        return a;
      }
      return Optional.of(op.apply(a.get(), b.get()));
    });
  }

  public static <T> Collector<T,?,T> reducing(T identity, BinaryOperator<T> op) {
    return reducing(identity, Function.identity(), op);
  }

  @SuppressWarnings("unchecked")
  public static <T, U> Collector<T, ?, U> reducing(
      final U identity, final Function<? super T, ? extends U> mapper, BinaryOperator<U> op) {
    return Collector.of(
      () -> new Object[]{identity},
      (u, t) -> u[0] = op.apply((U) u[0], mapper.apply(t)),
      (Object[] u1, Object[] u2) -> {
        u1[0] = op.apply((U) u1[0], (U) u2[0]);
        return u1;
      },
      (Object[] a) -> (U) a[0]
    );
  }

  public static <T> Collector<T, ?, DoubleSummaryStatistics> summarizingDouble(
      ToDoubleFunction<? super T> mapper) {
    return Collector.of(
        DoubleSummaryStatistics::new,
        (stats, item) -> stats.accept(mapper.applyAsDouble(item)),
        (t, u) -> {
          t.combine(u);
          return t;
        },
        Collector.Characteristics.UNORDERED, Collector.Characteristics.IDENTITY_FINISH
    );
  }

  public static <T> Collector<T, ?, IntSummaryStatistics> summarizingInt(
      ToIntFunction<? super T> mapper) {
    return Collector.of(
        IntSummaryStatistics::new,
        (stats, item) -> stats.accept(mapper.applyAsInt(item)),
        (t, u) -> {
          t.combine(u);
          return t;
        },
        Collector.Characteristics.UNORDERED, Collector.Characteristics.IDENTITY_FINISH
    );
  }

  public static <T> Collector<T, ?, LongSummaryStatistics> summarizingLong(
      ToLongFunction<? super T> mapper) {
    return Collector.of(
        LongSummaryStatistics::new,
        (stats, item) -> stats.accept(mapper.applyAsLong(item)),
        (t, u) -> {
          t.combine(u);
          return t;
        },
        Collector.Characteristics.UNORDERED, Collector.Characteristics.IDENTITY_FINISH
    );
  }

  public static <T> Collector<T,?,Double> summingDouble(final ToDoubleFunction<? super T> mapper) {
    // TODO simplify to only collect sum if possible
    return collectingAndThen(summarizingDouble(mapper), DoubleSummaryStatistics::getSum);
  }

  public static <T> Collector<T,?,Integer> summingInt(ToIntFunction<? super T> mapper) {
    // TODO simplify to only collect sum if possible
    return collectingAndThen(
        summarizingInt(mapper), intSummaryStatistics -> (int) intSummaryStatistics.getSum());
  }

  public static <T> Collector<T,?,Long> summingLong(ToLongFunction<? super T> mapper) {
    // TODO simplify to only collect sum if possible
    return collectingAndThen(summarizingLong(mapper), LongSummaryStatistics::getSum);
  }

  public static <T, C extends Collection<T>> Collector<T, ?, C> toCollection(
      final Supplier<C> collectionFactory) {
    return Collector.of(
        collectionFactory,
        Collection::add,
        // TODO switch to a lambda reference once #9333 is fixed
        (c1, c2) -> addAll(c1, c2),
        Collector.Characteristics.IDENTITY_FINISH
    );
  }

//  not supported
//  public static <T,K,U> Collector<T,?,ConcurrentMap<K,U>> toConcurrentMap(
//      Function<? super T,? extends K> keyMapper, Function<? super T,? extends U> valueMapper)
//  public static <T,K,U> Collector<T,?,ConcurrentMap<K,U>> toConcurrentMap(
//      Function<? super T,? extends K> keyMapper, Function<? super T,? extends U> valueMapper,
//      BinaryOperator<U> mergeFunction)
//  public static <T,K,U,M extends ConcurrentMap<K,U>> Collector<T,?,M> toConcurrentMap(
//      Function<? super T,? extends K> keyMapper, Function<? super T,? extends U> valueMapper,
//      BinaryOperator<U> mergeFunction, Supplier<M> mapSupplier)

  public static <T> Collector<T,?,List<T>> toList() {
    return toCollection(ArrayList::new);
  }

  public static <T, K, U> Collector<T, ?, Map<K, U>> toMap(
      final Function<? super T, ? extends K> keyMapper,
      final Function<? super T, ? extends U> valueMapper) {
    return toMap(
        keyMapper,
        valueMapper,
        (m1, m2) -> {
          throw new IllegalStateException("Can't assign multiple values to the same key");
        });
  }

  public static <T, K, U> Collector<T, ?, Map<K, U>> toMap(
      Function<? super T, ? extends K> keyMapper,
      Function<? super T, ? extends U> valueMapper,
      BinaryOperator<U> mergeFunction) {
    return toMap(keyMapper, valueMapper, mergeFunction, HashMap::new);
  }

  public static <T, K, U, M extends Map<K, U>> Collector<T, ?, M> toMap(
      final Function<? super T, ? extends K> keyMapper,
      final Function<? super T, ? extends U> valueMapper,
      final BinaryOperator<U> mergeFunction,
      final Supplier<M> mapSupplier) {
    return Collector.of(
        mapSupplier,
        (map, item) -> {
          K key = keyMapper.apply(item);
          U newValue = valueMapper.apply(item);
          if (map.containsKey(key)) {
            map.put(key, mergeFunction.apply(map.get(key), newValue));
          } else {
            map.put(key, newValue);
          }
        },
        (m1, m2) -> mergeAll(m1, m2, mergeFunction),
        Collector.Characteristics.IDENTITY_FINISH);
  }

  public static <T> Collector<T,?,Set<T>> toSet() {
    return Collector.<T, HashSet<T>, Set<T>>of(
        HashSet::new,
        HashSet::add,
        // TODO switch to a lambda reference once #9333 is fixed
        (c1, c2) -> addAll(c1, c2),
        // this is Function.identity, but Java doesn't like it here to change types.
        s -> s,
        Collector.Characteristics.UNORDERED, Collector.Characteristics.IDENTITY_FINISH
    );
  }

  private static Set<Collector.Characteristics> removeIdentFinisher(
      Set<Collector.Characteristics> characteristics) {
    if (!characteristics.contains(Collector.Characteristics.IDENTITY_FINISH)) {
      return characteristics;
    }

    if (characteristics.size() == 1) {
      return Collections.emptySet();
    }

    EnumSet<Collector.Characteristics> result = EnumSet.copyOf(characteristics);
    result.remove(Collector.Characteristics.IDENTITY_FINISH);
    return Collections.unmodifiableSet(result);
  }

  private static <T, D, A> D streamAndCollect(Collector<? super T, A, D> downstream, List<T> list) {
    A a = downstream.supplier().get();
    for (T t : list) {
      downstream.accumulator().accept(a, t);
    }
    return downstream.finisher().apply(a);
  }

  private static <K, V, M extends Map<K, V>> M mergeAll(
      M m1, M m2, BinaryOperator<V> mergeFunction) {
    for (Map.Entry<K, V> entry : m2.entrySet()) {
      m1.merge(entry.getKey(), entry.getValue(), mergeFunction);
    }
    return m1;
  }

  private static <T, C extends Collection<T>> C addAll(C collection, Collection<T> items) {
    collection.addAll(items);
    return collection;
  }
}
