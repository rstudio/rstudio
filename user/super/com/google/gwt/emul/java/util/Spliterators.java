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
package java.util;

import static javaemul.internal.InternalPreconditions.checkCriticalArrayBounds;
import static javaemul.internal.InternalPreconditions.checkCriticalElement;
import static javaemul.internal.InternalPreconditions.checkCriticalState;
import static javaemul.internal.InternalPreconditions.checkNotNull;

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

/**
 * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/Spliterators.html">
 * the official Java API doc</a> for details.
 *
 * Since it's hard to implement parallel algorithms in the browser environment
 * and to keep code simple, implementation does not provide splitting.
 */
public final class Spliterators {

  private abstract static class BaseSpliterator<T, S extends Spliterator<T>>
      implements Spliterator<T> {
    private final int characteristics;
    private long sizeEstimate;

    BaseSpliterator(long size, int characteristics) {
      this.sizeEstimate = size;
      this.characteristics = (characteristics & Spliterator.SIZED) != 0 ?
          characteristics | Spliterator.SUBSIZED : characteristics;
    }

    public int characteristics() {
      return characteristics;
    }

    public long estimateSize() {
      return sizeEstimate;
    }

    public S trySplit() {
      // see javadoc for java.util.Spliterator
      return null;
    }
  }

  /**
   * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/Spliterators.AbstractSpliterator.html">
   * the official Java API doc</a> for details.
   */
  public abstract static class AbstractSpliterator<T>
      extends BaseSpliterator<T, Spliterator<T>> implements Spliterator<T> {

    protected AbstractSpliterator(long size, int characteristics) {
      super(size, characteristics);
    }
  }

  /**
   * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/Spliterators.AbstractDoubleSpliterator.html">
   * the official Java API doc</a> for details.
   */
  public abstract static class AbstractDoubleSpliterator
      extends BaseSpliterator<Double, Spliterator.OfDouble> implements Spliterator.OfDouble {

    protected AbstractDoubleSpliterator(long size, int characteristics) {
      super(size, characteristics);
    }
  }

  /**
   * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/Spliterators.AbstractIntSpliterator.html">
   * the official Java API doc</a> for details.
   */
  public abstract static class AbstractIntSpliterator
      extends BaseSpliterator<Integer, Spliterator.OfInt> implements Spliterator.OfInt {

    protected AbstractIntSpliterator(long size, int characteristics) {
      super(size, characteristics);
    }
  }

  /**
   * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/Spliterators.AbstractLongSpliterator.html">
   * the official Java API doc</a> for details.
   */
  public abstract static class AbstractLongSpliterator
      extends BaseSpliterator<Long, Spliterator.OfLong> implements Spliterator.OfLong {

    protected AbstractLongSpliterator(long size, int characteristics) {
      super(size, characteristics);
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> Spliterator<T> emptySpliterator() {
    return (Spliterator<T>) EmptySpliterator.OF_REF;
  }

  public static Spliterator.OfDouble emptyDoubleSpliterator() {
    return EmptySpliterator.OF_DOUBLE;
  }

  public static Spliterator.OfInt emptyIntSpliterator() {
    return EmptySpliterator.OF_INT;
  }

  public static Spliterator.OfLong emptyLongSpliterator() {
    return EmptySpliterator.OF_LONG;
  }

  public static <T> Spliterator<T> spliterator(Object[] array, int characteristics) {
    return new ArraySpliterator<>(array, characteristics);
  }

  public static <T> Spliterator<T> spliterator(Object[] array, int fromIndex, int toIndex,
                                               int characteristics) {
    return new ArraySpliterator<>(array, fromIndex, toIndex, characteristics);
  }

  public static Spliterator.OfInt spliterator(int[] array, int characteristics) {
    return new IntArraySpliterator(array, characteristics);
  }

  public static Spliterator.OfInt spliterator(int[] array, int fromIndex, int toIndex,
                                              int characteristics) {
    return new IntArraySpliterator(array, fromIndex, toIndex, characteristics);
  }

  public static Spliterator.OfLong spliterator(long[] array, int characteristics) {
    return new LongArraySpliterator(array, characteristics);
  }

  public static Spliterator.OfLong spliterator(long[] array, int fromIndex, int toIndex,
                                               int characteristics) {
    return new LongArraySpliterator(array, fromIndex, toIndex, characteristics);
  }

  public static Spliterator.OfDouble spliterator(double[] array, int characteristics) {
    return new DoubleArraySpliterator(array, characteristics);
  }

  public static Spliterator.OfDouble spliterator(double[] array, int fromIndex, int toIndex,
                                                 int characteristics) {
    return new DoubleArraySpliterator(array, fromIndex, toIndex, characteristics);
  }

  public static <T> Spliterator<T> spliterator(Collection<? extends T> c, int characteristics) {
    return new IteratorSpliterator<>(c, characteristics);
  }

  public static <T> Spliterator<T> spliterator(Iterator<? extends T> it, long size,
                                               int characteristics) {
    return new IteratorSpliterator<>(it, size, characteristics);
  }

  public static <T> Spliterator<T> spliteratorUnknownSize(Iterator<? extends T> it,
                                                          int characteristics) {
    return new IteratorSpliterator<>(it, characteristics);
  }

  public static Spliterator.OfInt spliterator(PrimitiveIterator.OfInt it, long size,
                                              int characteristics) {
    return new IntIteratorSpliterator(it, size, characteristics);
  }

  public static Spliterator.OfInt spliteratorUnknownSize(PrimitiveIterator.OfInt it,
                                                         int characteristics) {
    return new IntIteratorSpliterator(it, characteristics);
  }

  public static Spliterator.OfLong spliterator(PrimitiveIterator.OfLong it, long size,
                                               int characteristics) {
    return new LongIteratorSpliterator(it, size, characteristics);
  }

  public static Spliterator.OfLong spliteratorUnknownSize(PrimitiveIterator.OfLong it,
                                                          int characteristics) {
    return new LongIteratorSpliterator(it, characteristics);
  }

  public static Spliterator.OfDouble spliterator(PrimitiveIterator.OfDouble it, long size,
                                                 int characteristics) {
    return new DoubleIteratorSpliterator(it, size, characteristics);
  }

  public static Spliterator.OfDouble spliteratorUnknownSize(PrimitiveIterator.OfDouble it,
                                                            int characteristics) {
    return new DoubleIteratorSpliterator(it, characteristics);
  }

  public static <T> Iterator<T> iterator(Spliterator<? extends T> spliterator) {
    return new ConsumerIterator<>(spliterator);
  }

  public static PrimitiveIterator.OfDouble iterator(Spliterator.OfDouble spliterator) {
    return new DoubleConsumerIterator(spliterator);
  }

  public static PrimitiveIterator.OfInt iterator(Spliterator.OfInt spliterator) {
    return new IntConsumerIterator(spliterator);
  }

  public static PrimitiveIterator.OfLong iterator(Spliterator.OfLong spliterator) {
    return new LongConsumerIterator(spliterator);
  }

  private abstract static class EmptySpliterator<T, S extends Spliterator<T>, C>
      implements Spliterator<T> {

    static final Spliterator<Object> OF_REF = new EmptySpliterator.OfRef<>();
    static final Spliterator.OfDouble OF_DOUBLE = new EmptySpliterator.OfDouble();
    static final Spliterator.OfInt OF_INT = new EmptySpliterator.OfInt();
    static final Spliterator.OfLong OF_LONG = new EmptySpliterator.OfLong();

    public int characteristics() {
      return Spliterator.SIZED | Spliterator.SUBSIZED;
    }

    public long estimateSize() {
      return 0;
    }

    public void forEachRemaining(C consumer) {
      checkNotNull(consumer);
    }

    public boolean tryAdvance(C consumer) {
      checkNotNull(consumer);
      return false;
    }

    public S trySplit() {
      return null;
    }

    private static final class OfRef<T>
        extends EmptySpliterator<T, Spliterator<T>, Consumer<? super T>>
        implements Spliterator<T> {

      OfRef() { }
    }

    private static final class OfDouble
        extends EmptySpliterator<Double, Spliterator.OfDouble, DoubleConsumer>
        implements Spliterator.OfDouble {

      OfDouble() { }
    }

    private static final class OfInt
        extends EmptySpliterator<Integer, Spliterator.OfInt, IntConsumer>
        implements Spliterator.OfInt {

      OfInt() { }
    }

    private static final class OfLong
        extends EmptySpliterator<Long, Spliterator.OfLong, LongConsumer>
        implements Spliterator.OfLong {

      OfLong() { }
    }
  }

  private static final class ConsumerIterator<T> implements Consumer<T>, Iterator<T> {
    private final Spliterator<? extends T> spliterator;
    private T nextElement;
    private boolean hasElement = false;

    ConsumerIterator(Spliterator<? extends T> spliterator) {
      this.spliterator = checkNotNull(spliterator);
    }

    @Override
    public void accept(T element) {
      nextElement = element;
    }

    @Override
    public boolean hasNext() {
      if (!hasElement) {
        hasElement = spliterator.tryAdvance(this);
      }
      return hasElement;
    }

    @Override
    public T next() {
      checkCriticalElement(hasNext());
      hasElement = false;
      T element = nextElement;
      nextElement = null;
      return element;
    }
  }

  private static final class DoubleConsumerIterator
      implements DoubleConsumer, PrimitiveIterator.OfDouble {

    private final Spliterator.OfDouble spliterator;
    private double nextElement;
    private boolean hasElement = false;

    DoubleConsumerIterator(Spliterator.OfDouble spliterator) {
      this.spliterator = checkNotNull(spliterator);
    }

    @Override
    public void accept(double d) {
      nextElement = d;
    }

    @Override
    public boolean hasNext() {
      if (!hasElement) {
        hasElement = spliterator.tryAdvance(this);
      }
      return hasElement;
    }

    @Override
    public double nextDouble() {
      checkCriticalElement(hasNext());
      hasElement = false;
      return nextElement;
    }
  }

  private static final class IntConsumerIterator
      implements IntConsumer, PrimitiveIterator.OfInt {

    private final Spliterator.OfInt spliterator;
    private int nextElement;
    private boolean hasElement = false;

    IntConsumerIterator(Spliterator.OfInt spliterator) {
      this.spliterator = checkNotNull(spliterator);
    }

    @Override
    public void accept(int i) {
      nextElement = i;
    }

    @Override
    public boolean hasNext() {
      if (!hasElement) {
        hasElement = spliterator.tryAdvance(this);
      }
      return hasElement;
    }

    @Override
    public int nextInt() {
      checkCriticalElement(hasNext());
      hasElement = false;
      return nextElement;
    }
  }

  private static final class LongConsumerIterator
      implements LongConsumer, PrimitiveIterator.OfLong {

    private final Spliterator.OfLong spliterator;
    private long nextElement;
    private boolean hasElement = false;

    LongConsumerIterator(Spliterator.OfLong spliterator) {
      this.spliterator = checkNotNull(spliterator);
    }

    @Override
    public void accept(long l) {
      nextElement = l;
    }

    @Override
    public boolean hasNext() {
      if (!hasElement) {
        hasElement = spliterator.tryAdvance(this);
      }
      return hasElement;
    }

    @Override
    public long nextLong() {
      checkCriticalElement(hasNext());
      hasElement = false;
      return nextElement;
    }
  }

  static class IteratorSpliterator<T> implements Spliterator<T> {
    private Collection<? extends T> collection;
    private Iterator<? extends T> it;
    private final int characteristics;
    private long estimateSize;

    IteratorSpliterator(Collection<? extends T> collection, int characteristics) {
      this.collection = checkNotNull(collection);
      this.characteristics = sizeKnownIteratorSpliteratorCharacteristics(characteristics);
    }

    IteratorSpliterator(Iterator<? extends T> it, long size, int characteristics) {
      this.it = checkNotNull(it);
      this.characteristics = sizeKnownIteratorSpliteratorCharacteristics(characteristics);
      this.estimateSize = size;
    }

    IteratorSpliterator(Iterator<? extends T> it, int characteristics) {
      this.it = checkNotNull(it);
      this.characteristics = sizeUnknownSpliteratorCharacteristics(characteristics);
      this.estimateSize = Long.MAX_VALUE;
    }

    @Override
    public int characteristics() {
      return characteristics;
    }

    @Override
    public long estimateSize() {
      initIterator();
      return estimateSize;
    }

    @Override
    public void forEachRemaining(Consumer<? super T> consumer) {
      initIterator();
      it.forEachRemaining(consumer);
    }

    @Override
    public Comparator<? super T> getComparator() {
      checkSorted(characteristics);
      return null;
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> consumer) {
      checkNotNull(consumer);
      initIterator();
      if (it.hasNext()) {
        consumer.accept(it.next());
        return true;
      }
      return false;
    }

    @Override
    public Spliterator<T> trySplit() {
      // see javadoc for java.util.Spliterator
      return null;
    }

    private void initIterator() {
      if (it == null) {
        it = collection.iterator();
        estimateSize = (long) collection.size();
      }
    }
  }

  private static final class DoubleIteratorSpliterator extends AbstractDoubleSpliterator {
    private final PrimitiveIterator.OfDouble it;

    DoubleIteratorSpliterator(PrimitiveIterator.OfDouble it, long size, int characteristics) {
      super(size, sizeKnownIteratorSpliteratorCharacteristics(characteristics));
      this.it = checkNotNull(it);
    }

    DoubleIteratorSpliterator(PrimitiveIterator.OfDouble it, int characteristics) {
      super(Long.MAX_VALUE, sizeUnknownSpliteratorCharacteristics(characteristics));
      this.it = checkNotNull(it);
    }

    @Override
    public void forEachRemaining(DoubleConsumer consumer) {
      it.forEachRemaining(consumer);
    }

    @Override
    public Comparator<? super Double> getComparator() {
      checkSorted(characteristics());
      return null;
    }

    @Override
    public boolean tryAdvance(DoubleConsumer consumer) {
      checkNotNull(consumer);
      if (it.hasNext()) {
        consumer.accept(it.nextDouble());
        return true;
      }
      return false;
    }
  }

  private static final class IntIteratorSpliterator extends AbstractIntSpliterator {
    private final PrimitiveIterator.OfInt it;

    IntIteratorSpliterator(PrimitiveIterator.OfInt it, long size, int characteristics) {
      super(size, sizeKnownIteratorSpliteratorCharacteristics(characteristics));
      this.it = checkNotNull(it);
    }

    IntIteratorSpliterator(PrimitiveIterator.OfInt it, int characteristics) {
      super(Long.MAX_VALUE, sizeUnknownSpliteratorCharacteristics(characteristics));
      this.it = checkNotNull(it);
    }

    @Override
    public void forEachRemaining(IntConsumer consumer) {
      it.forEachRemaining(consumer);
    }

    @Override
    public Comparator<? super Integer> getComparator() {
      checkSorted(characteristics());
      return null;
    }

    @Override
    public boolean tryAdvance(IntConsumer consumer) {
      checkNotNull(consumer);
      if (it.hasNext()) {
        consumer.accept(it.nextInt());
        return true;
      }
      return false;
    }
  }

  private static final class LongIteratorSpliterator extends AbstractLongSpliterator {
    private final PrimitiveIterator.OfLong it;

    LongIteratorSpliterator(PrimitiveIterator.OfLong it, long size, int characteristics) {
      super(size, sizeKnownIteratorSpliteratorCharacteristics(characteristics));
      this.it = checkNotNull(it);
    }

    LongIteratorSpliterator(PrimitiveIterator.OfLong it, int characteristics) {
      super(Long.MAX_VALUE, sizeUnknownSpliteratorCharacteristics(characteristics));
      this.it = checkNotNull(it);
    }

    @Override
    public void forEachRemaining(LongConsumer consumer) {
      it.forEachRemaining(consumer);
    }

    @Override
    public Comparator<? super Long> getComparator() {
      checkSorted(characteristics());
      return null;
    }

    @Override
    public boolean tryAdvance(LongConsumer consumer) {
      checkNotNull(consumer);
      if (it.hasNext()) {
        consumer.accept(it.nextLong());
        return true;
      }
      return false;
    }
  }

  private abstract static class BaseArraySpliterator<T, S extends Spliterator<T>, C>
      implements Spliterator<T> {
    private int index;
    private final int limit;
    private final int characteristics;

    BaseArraySpliterator(int from, int limit, int size, int characteristics) {
      checkCriticalArrayBounds(from, limit, size);
      this.index = from;
      this.limit = limit;
      this.characteristics = sizeKnownSpliteratorCharacteristics(characteristics);
    }

    public int characteristics() {
      return characteristics;
    }

    public long estimateSize() {
      return limit - index;
    }

    public void forEachRemaining(C consumer) {
      checkNotNull(consumer);
      while (index < limit) {
        consume(consumer, index++);
      }
    }

    public Comparator<? super T> getComparator() {
      checkSorted(characteristics);
      return null;
    }

    public boolean tryAdvance(C consumer) {
      checkNotNull(consumer);
      if (index < limit) {
        consume(consumer, index++);
        return true;
      }
      return false;
    }

    public S trySplit() {
      // see javadoc for java.util.Spliterator
      return null;
    }

    protected abstract void consume(C consumer, int index);
  }

  private static final class ArraySpliterator<T>
      extends BaseArraySpliterator<T, Spliterator<T>, Consumer<? super T>>
      implements Spliterator<T> {

    private final Object[] array;

    ArraySpliterator(Object[] array, int characteristics) {
      this(array, 0, array.length, characteristics);
    }

    ArraySpliterator(Object[] array, int from, int limit, int characteristics) {
      super(from, limit, array.length, characteristics);
      this.array = array;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void consume(Consumer<? super T> consumer, int index) {
      consumer.accept((T) array[index]);
    }
  }

  private static final class DoubleArraySpliterator
      extends BaseArraySpliterator<Double, Spliterator.OfDouble, DoubleConsumer>
      implements Spliterator.OfDouble {

    private final double[] array;

    DoubleArraySpliterator(double[] array, int characteristics) {
      this(array, 0, array.length, characteristics);
    }

    DoubleArraySpliterator(double[] array, int from, int limit, int characteristics) {
      super(from, limit, array.length, characteristics);
      this.array = array;
    }

    @Override
    protected void consume(DoubleConsumer consumer, int index) {
      consumer.accept(array[index]);
    }
  }

  private static final class IntArraySpliterator
      extends BaseArraySpliterator<Integer, Spliterator.OfInt, IntConsumer>
      implements Spliterator.OfInt {

    private final int[] array;

    IntArraySpliterator(int[] array, int characteristics) {
      this(array, 0, array.length, characteristics);
    }

    IntArraySpliterator(int[] array, int from, int limit, int characteristics) {
      super(from, limit, array.length, characteristics);
      this.array = array;
    }

    @Override
    protected void consume(IntConsumer consumer, int index) {
      consumer.accept(array[index]);
    }
  }

  private static final class LongArraySpliterator
      extends BaseArraySpliterator<Long, Spliterator.OfLong, LongConsumer>
      implements Spliterator.OfLong {

    private final long[] array;

    LongArraySpliterator(long[] array, int characteristics) {
      this(array, 0, array.length, characteristics);
    }

    LongArraySpliterator(long[] array, int from, int limit, int characteristics) {
      super(from, limit, array.length, characteristics);
      this.array = array;
    }

    @Override
    protected void consume(LongConsumer consumer, int index) {
      consumer.accept(array[index]);
    }
  }

  private static void checkSorted(int characteristics) {
    checkCriticalState((characteristics & Spliterator.SORTED) != 0);
  }

  private static int sizeKnownSpliteratorCharacteristics(int characteristics) {
    return characteristics | Spliterator.SIZED | Spliterator.SUBSIZED;
  }

  private static int sizeKnownIteratorSpliteratorCharacteristics(int characteristics) {
    return (characteristics & Spliterator.CONCURRENT) == 0 ?
        sizeKnownSpliteratorCharacteristics(characteristics) : characteristics;
  }

  private static int sizeUnknownSpliteratorCharacteristics(int characteristics) {
    return characteristics & ~(Spliterator.SIZED | Spliterator.SUBSIZED);
  }

  private Spliterators() { }

}
