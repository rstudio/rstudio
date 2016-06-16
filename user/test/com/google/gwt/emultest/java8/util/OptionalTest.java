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

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.junit.client.GWTTestCase;

import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Tests for Optional JRE emulation.
 */
public class OptionalTest extends GWTTestCase {

  private static final Object REFERENCE = new Object();
  private static final Object OTHER_REFERENCE = new Object();
  private boolean[] mutableFlag;
  private Optional<Object> empty;
  private Optional<Object> present;

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  @Override
  protected void gwtSetUp() throws Exception {
    super.gwtSetUp();
    mutableFlag = new boolean[1];
    empty = Optional.empty();
    present = Optional.of(REFERENCE);
  }

  public void testIsPresent() {
    // empty case
    assertFalse(empty.isPresent());

    empty = Optional.ofNullable(null);
    assertFalse(empty.isPresent());

    // non-empty case
    assertTrue(present.isPresent());

    present = Optional.ofNullable(REFERENCE);
    assertTrue(present.isPresent());
  }

  public void testGet() {
    // empty case
    try {
      empty.get();
      fail("Empty Optional should throw NoSuchElementException");
    } catch (NoSuchElementException e) {
      // expected
    }

    // non-empty case
    assertSame(REFERENCE, present.get());
  }

  public void testIfPresent() {
    // empty case
    empty.ifPresent(null); // should not fail as per JavaDoc
    empty.ifPresent(wrapped -> fail("Empty Optional should not execute consumer"));

    // non-empty case
    try {
      present.ifPresent(null);
      fail("Non-Empty Optional must throw NullPointerException if consumer is null");
    } catch (NullPointerException e) {
      // expected
    } catch (JavaScriptException e) {
      // expected
    }

    present.ifPresent((wrapped) -> {
      assertSame(REFERENCE, wrapped);
      mutableFlag[0] = true;
    });
    assertTrue("Consumer not executed", mutableFlag[0]);
  }

  public void testFilter() {
    // empty case
    try {
      empty.filter(null);
      fail("Optional must throw NullPointerException if predicate is null");
    } catch (NullPointerException e) {
      // expected
    }

    Optional<Object> filtered = empty.filter(wrapped -> true);
    assertFalse(filtered.isPresent());

    filtered = empty.filter(wrapped -> false);
    assertFalse(filtered.isPresent());

    // non-empty case
    try {
      present.filter(null);
      fail("Optional must throw NullPointerException if predicate is null");
    } catch (NullPointerException e) {
      // expected
    }

    filtered = present.filter(wrapped -> true);
    assertSame(REFERENCE, filtered.get());

    filtered = present.filter(wrapped -> false);
    assertFalse(filtered.isPresent());
  }

  public void testMap() {
    // empty case
    try {
      empty.map(null);
      fail("Optional must throw NullPointerException if mapper is null");
    } catch (NullPointerException e) {
      // expected
    }

    empty.map(wrapped -> {
      fail("Empty Optional must not execute mapper");
      return "should not execute";
    });

    // non-empty case
    try {
      present.map(null);
      fail("Optional must throw NullPointerException if mapper is null");
    } catch (NullPointerException e) {
      // expected
    }
    Optional<String> mapped = present.map(wrapped -> null);
    assertFalse(mapped.isPresent());

    mapped = present.map(Object::toString);
    assertEquals(REFERENCE.toString(), mapped.get());
  }

  public void testFlatMap() {
    // empty case
    try {
      empty.flatMap(null);
      fail("Optional must throw NullPointerException if mapper is null");
    } catch (NullPointerException e) {
      // expected
    }

    empty.flatMap(wrapped -> {
      fail("Empty Optional must not execute mapper");
      return Optional.of("should not execute");
    });

    // non-empty case
    try {
      present.flatMap(null);
      fail("Optional must throw NullPointerException if mapper is null");
    } catch (NullPointerException e) {
      // expected
    }

    try {
      present.flatMap(wrapped -> null);
      fail("Optional must throw NullPointerException if mapper returns null");
    } catch (NullPointerException e) {
      // expected
    }

    Optional<String> mapped = present.flatMap(wrapped -> Optional.empty());
    assertFalse(mapped.isPresent());

    mapped = present.flatMap(wrapped -> Optional.of(wrapped.toString()));
    assertEquals(REFERENCE.toString(), mapped.get());
  }

  public void testOrElse() {
    // empty case
    assertSame(OTHER_REFERENCE, empty.orElse(OTHER_REFERENCE));

    // non-empty case
    assertSame(REFERENCE, present.orElse(OTHER_REFERENCE));
  }

  public void testOrElseGet() {
    // empty case
    try {
      empty.orElseGet(null);
      fail("Empty Optional must throw NullPointerException if supplier is null");
    } catch (NullPointerException e) {
      // expected
    } catch (JavaScriptException e) {
      // expected
    }

    assertSame(OTHER_REFERENCE, empty.orElseGet(() -> OTHER_REFERENCE));

    // non-empty case
    assertSame(REFERENCE, present.orElseGet(() -> {
      fail("Optional must not execute supplier");
      return OTHER_REFERENCE;
    }));
  }

  public void testOrElseThrow() {
    // empty case
    try {
      empty.orElseThrow(null);
      fail("Empty Optional must throw NullPointerException if supplier is null");
    } catch (NullPointerException e) {
      // expected
    } catch (JavaScriptException e) {
      // expected
    }

    try {
      empty.<RuntimeException>orElseThrow(() -> null);
      fail("Empty Optional must throw NullPointerException if supplier returns null");
    } catch (NullPointerException e) {
      // expected
    } catch (JavaScriptException e) {
      // expected
    }

    try {
      empty.orElseThrow(IllegalStateException::new);
      fail("Empty Optional must throw supplied exception");
    } catch (IllegalStateException e) {
      // expected
    }

    // non-empty case
    try {
      Object reference = present.orElseThrow(null);
      assertSame(REFERENCE, reference);
    } catch (NullPointerException e) {
      fail("Optional must not throw NullPointerException if supplier is null");
    }

    assertSame(REFERENCE, present.orElseThrow(() -> {
      fail("Optional must not execute supplier");
      return new RuntimeException("should not execute");
    }));
  }

  public void testEquals() {
    // empty case
    assertFalse(empty.equals(null));
    assertFalse(empty.equals("should not be equal"));
    assertFalse(empty.equals(present));
    assertTrue(empty.equals(empty));
    assertTrue(empty.equals(Optional.empty()));

    // non empty case
    assertFalse(present.equals(null));
    assertFalse(present.equals("should not be equal"));
    assertFalse(present.equals(empty));
    assertFalse(present.equals(Optional.of(OTHER_REFERENCE)));
    assertTrue(present.equals(present));
    assertTrue(present.equals(Optional.of(REFERENCE)));
  }

  public void testHashcode() {
    // empty case
    assertEquals(0, empty.hashCode());

    // non empty case
    assertEquals(REFERENCE.hashCode(), present.hashCode());
  }

}
