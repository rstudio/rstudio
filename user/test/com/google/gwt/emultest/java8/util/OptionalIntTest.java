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
import java.util.OptionalInt;

/**
 * Tests for OptionalInt JRE emulation.
 */
public class OptionalIntTest extends GWTTestCase {

  private static final int REFERENCE = 10;
  private static final int OTHER_REFERENCE = 20;
  private boolean[] mutableFlag;
  private OptionalInt empty;
  private OptionalInt present;

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  @Override
  protected void gwtSetUp() throws Exception {
    super.gwtSetUp();
    mutableFlag = new boolean[1];
    empty = OptionalInt.empty();
    present = OptionalInt.of(REFERENCE);
  }

  public void testIsPresent() {
    // empty case
    assertFalse(empty.isPresent());

    // non-empty case
    assertTrue(present.isPresent());
  }

  public void testGetAsInt() {
    // empty case
    try {
      empty.getAsInt();
      fail("Empty Optional should throw NoSuchElementException");
    } catch (NoSuchElementException e) {
      // expected
    }

    // non-empty case
    assertEquals(REFERENCE, present.getAsInt());
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
      assertEquals(REFERENCE, wrapped);
      mutableFlag[0] = true;
    });
    assertTrue("Consumer not executed", mutableFlag[0]);
  }

  public void testOrElse() {
    // empty case
    assertEquals(OTHER_REFERENCE, empty.orElse(OTHER_REFERENCE));

    // non-empty case
    assertEquals(REFERENCE, present.orElse(OTHER_REFERENCE));
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

    assertEquals(OTHER_REFERENCE, empty.orElseGet(() -> OTHER_REFERENCE));

    // non-empty case
    assertEquals(REFERENCE, present.orElseGet(() -> {
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
      empty.orElseThrow(() -> null);
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
      assertEquals(REFERENCE, reference);
    } catch (NullPointerException e) {
      fail("Optional must not throw NullPointerException if supplier is null");
    }

    assertEquals(REFERENCE, present.orElseThrow(() -> {
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
    assertTrue(empty.equals(OptionalInt.empty()));

    // non empty case
    assertFalse(present.equals(null));
    assertFalse(present.equals("should not be equal"));
    assertFalse(present.equals(empty));
    assertFalse(present.equals(OptionalInt.of(OTHER_REFERENCE)));
    assertTrue(present.equals(present));
    assertTrue(present.equals(OptionalInt.of(REFERENCE)));
  }

  public void testHashcode() {
    // empty case
    assertEquals(0, empty.hashCode());

    // non empty case
    assertEquals(Integer.hashCode(REFERENCE), present.hashCode());
  }

}
