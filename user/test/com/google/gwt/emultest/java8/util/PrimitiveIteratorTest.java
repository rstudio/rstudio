/*
 * Copyright 2015 Google Inc.
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
package com.google.gwt.emultest.java8.util;

import com.google.gwt.junit.client.GWTTestCase;

import java.util.Collections;
import java.util.Iterator;
import java.util.PrimitiveIterator;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

/**
 * Tests for PrimitiveIterator JRE emulation.
 */
public class PrimitiveIteratorTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  public void testForEachRemainingDoubleConsumer() {
    PrimitiveIterator.OfDouble it = createTestPrimitiveDoubleIterator();
    it.forEachRemaining((Consumer<Double>) new JanusDoubleConsumer() {
      @Override
      public void accept(Double value) {
        fail();
      }

      @Override
      public void accept(double value) {
      }
    });

    try {
      it.forEachRemaining((Consumer<Double>) null);
      fail();
    } catch (NullPointerException expected) {
    }

    try {
      it.forEachRemaining((DoubleConsumer) null);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testForEachRemainingIntConsumer() {
    PrimitiveIterator.OfInt it = createTestPrimitiveIntIterator();
    it.forEachRemaining((Consumer<Integer>) new JanusIntConsumer() {
      @Override
      public void accept(Integer value) {
        fail();
      }

      @Override
      public void accept(int value) {
      }
    });

    try {
      it.forEachRemaining((Consumer<Integer>) null);
      fail();
    } catch (NullPointerException expected) {
    }

    try {
      it.forEachRemaining((IntConsumer) null);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testForEachRemainingLongConsumer() {
    PrimitiveIterator.OfLong it = createTestPrimitiveLongIterator();
    it.forEachRemaining((Consumer<Long>) new JanusLongConsumer() {
      @Override
      public void accept(Long value) {
        fail();
      }

      @Override
      public void accept(long value) {
      }
    });

    try {
      it.forEachRemaining((Consumer<Long>) null);
      fail();
    } catch (NullPointerException expected) {
    }

    try {
      it.forEachRemaining((LongConsumer) null);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  private static PrimitiveIterator.OfDouble createTestPrimitiveDoubleIterator() {
    final Iterator<Double> it = Collections.singletonList(1.).iterator();
    return new PrimitiveIterator.OfDouble() {
      @Override
      public double nextDouble() {
        return it.next();
      }

      @Override
      public boolean hasNext() {
        return it.hasNext();
      }
    };
  }

  private static PrimitiveIterator.OfInt createTestPrimitiveIntIterator() {
    final Iterator<Integer> it = Collections.singletonList(1).iterator();
    return new PrimitiveIterator.OfInt() {
      @Override
      public int nextInt() {
        return it.next();
      }

      @Override
      public boolean hasNext() {
        return it.hasNext();
      }
    };
  }

  private static PrimitiveIterator.OfLong createTestPrimitiveLongIterator() {
    final Iterator<Long> it = Collections.singletonList(1L).iterator();
    return new PrimitiveIterator.OfLong() {
      @Override
      public long nextLong() {
        return it.next();
      }

      @Override
      public boolean hasNext() {
        return it.hasNext();
      }
    };
  }

  private interface JanusDoubleConsumer extends Consumer<Double>, DoubleConsumer { }

  private interface JanusIntConsumer extends Consumer<Integer>, IntConsumer { }

  private interface JanusLongConsumer extends Consumer<Long>, LongConsumer { }

}
