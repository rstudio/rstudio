// CHECKSTYLE_OFF: Copyrighted to members of JCP JSR-166 Expert Group.
/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/licenses/publicdomain
 * Other contributors include Andrew Wright, Jeffrey Hayes,
 * Pat Fisher, Mike Judd.
 */
// CHECKSTYLE_ON
package com.google.gwt.emultest.java.util.concurrent.atomic;

import com.google.gwt.emultest.java.util.EmulTestBase;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests for {@link java.util.concurrent.atomic.AtomicInteger}.
 * It's adopted from tests from {@code git://android.git.kernel.org/platform/dalvik.git}:
 */
public class AtomicIntegerTest extends EmulTestBase {

  /** constructor initializes to given value */
  public void testConstructor() {
    AtomicInteger ai = new AtomicInteger(1);
    assertEquals(1, ai.get());
  }

  /** default constructed initializes to zero */
  public void testConstructor2() {
    AtomicInteger ai = new AtomicInteger();
    assertEquals(0, ai.get());
  }

  /** get returns the last value set */
  public void testGetSet() {
    AtomicInteger ai = new AtomicInteger(1);
    assertEquals(1, ai.get());
    ai.set(2);
    assertEquals(2, ai.get());
    ai.set(-3);
    assertEquals(-3, ai.get());
  }

  /** compareAndSet succeeds in changing value if equal to expected else fails */
  public void testCompareAndSet() {
    AtomicInteger ai = new AtomicInteger(1);
    assertTrue(ai.compareAndSet(1, 2));
    assertTrue(ai.compareAndSet(2, -4));
    assertEquals(-4, ai.get());
    assertFalse(ai.compareAndSet(-5, 7));
    assertFalse((7 == ai.get()));
    assertTrue(ai.compareAndSet(-4, 7));
    assertEquals(7, ai.get());
  }

  /** getAndSet returns previous value and sets to given value */
  public void testGetAndSet() {
    AtomicInteger ai = new AtomicInteger(1);
    assertEquals(1, ai.getAndSet(0));
    assertEquals(0, ai.getAndSet(-10));
    assertEquals(-10, ai.getAndSet(1));
  }

  /** getAndAdd returns previous value and adds given value */
  public void testGetAndAdd() {
    AtomicInteger ai = new AtomicInteger(1);
    assertEquals(1, ai.getAndAdd(2));
    assertEquals(3, ai.get());
    assertEquals(3, ai.getAndAdd(-4));
    assertEquals(-1, ai.get());
  }

  /** getAndDecrement returns previous value and decrements */
  public void testGetAndDecrement() {
    AtomicInteger ai = new AtomicInteger(1);
    assertEquals(1, ai.getAndDecrement());
    assertEquals(0, ai.getAndDecrement());
    assertEquals(-1, ai.getAndDecrement());
  }

  /** getAndIncrement returns previous value and increments */
  public void testGetAndIncrement() {
    AtomicInteger ai = new AtomicInteger(1);
    assertEquals(1, ai.getAndIncrement());
    assertEquals(2, ai.get());
    ai.set(-2);
    assertEquals(-2, ai.getAndIncrement());
    assertEquals(-1, ai.getAndIncrement());
    assertEquals(0, ai.getAndIncrement());
    assertEquals(1, ai.get());
  }

  /** addAndGet adds given value to current, and returns current value */
  public void testAddAndGet() {
    AtomicInteger ai = new AtomicInteger(1);
    assertEquals(3, ai.addAndGet(2));
    assertEquals(3, ai.get());
    assertEquals(-1, ai.addAndGet(-4));
    assertEquals(-1, ai.get());
  }

  /** decrementAndGet decrements and returns current value */
  public void testDecrementAndGet() {
    AtomicInteger ai = new AtomicInteger(1);
    assertEquals(0, ai.decrementAndGet());
    assertEquals(-1, ai.decrementAndGet());
    assertEquals(-2, ai.decrementAndGet());
    assertEquals(-2, ai.get());
  }

  /** incrementAndGet increments and returns current value */
  public void testIncrementAndGet() {
    AtomicInteger ai = new AtomicInteger(1);
    assertEquals(2, ai.incrementAndGet());
    assertEquals(2, ai.get());
    ai.set(-2);
    assertEquals(-1, ai.incrementAndGet());
    assertEquals(0, ai.incrementAndGet());
    assertEquals(1, ai.incrementAndGet());
    assertEquals(1, ai.get());
  }

  /** toString returns current value. */
  public void testToString() {
    AtomicInteger ai = new AtomicInteger();
    for (int i = -12; i < 6; ++i) {
      ai.set(i);
      assertEquals(ai.toString(), Integer.toString(i));
    }
  }

  /** intValue returns current value. */
  public void testIntValue() {
    AtomicInteger ai = new AtomicInteger();
    for (int i = -12; i < 6; ++i) {
      ai.set(i);
      assertEquals(i, ai.intValue());
    }
  }

  /** longValue returns current value. */
  public void testLongValue() {
    AtomicInteger ai = new AtomicInteger();
    for (int i = -12; i < 6; ++i) {
      ai.set(i);
      assertEquals(i, ai.longValue());
    }
  }

  /** floatValue returns current value. */
  public void testFloatValue() {
    AtomicInteger ai = new AtomicInteger();
    for (int i = -12; i < 6; ++i) {
      ai.set(i);
      assertEquals((float) i, ai.floatValue(), 0.0);
    }
  }

  /** doubleValue returns current value. */
  public void testDoubleValue() {
    AtomicInteger ai = new AtomicInteger();
    for (int i = -12; i < 6; ++i) {
      ai.set(i);
      assertEquals((double) i, ai.doubleValue(), 0.0);
    }
  }
}
