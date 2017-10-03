/*
 * Copyright 2017 Google Inc.
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
package com.google.gwt.emultest.java.util.concurrent.atomic;

import com.google.gwt.emultest.java.util.EmulTestBase;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Tests for {@link java.util.concurrent.atomic.AtomicReferenceArray}.
 */
public class AtomicReferenceArrayTest extends EmulTestBase {

  public void testArrayConstructor() {
    Object object = new Object();

    AtomicReferenceArray<Object> refArray = new AtomicReferenceArray<>(new Object[] {object});

    assertSame(object, refArray.get(0));
    assertEquals(1, refArray.length());
  }

  public void testLengthConstructor() {
    AtomicReferenceArray<Object> refArray = new AtomicReferenceArray<>(1);

    assertNull(refArray.get(0));
    assertSame(1, refArray.length());
  }

  public void testCompareAndSet() {
    Object expect = new ArrayList<Object>();
    // This object is the same by |.equals| equality, but |compareAndSet| should use |==|.
    Object notExpect = new ArrayList<Object>();
    Object update = new Object();

    AtomicReferenceArray<Object> refArray = new AtomicReferenceArray<>(new Object[] {expect});

    assertFalse(refArray.compareAndSet(0, notExpect, new Object()));
    assertSame(expect, refArray.get(0));
    assertTrue(refArray.compareAndSet(0, expect, update));
    assertSame(update, refArray.get(0));
  }

  public void testGet() {
    Object expect = new Object();

    AtomicReferenceArray<Object> refArray = new AtomicReferenceArray<>(new Object[] {expect});

    assertSame(expect, refArray.get(0));
  }

  public void testGetAndSet() {
    Object expect = new Object();
    Object update = new Object();

    AtomicReferenceArray<Object> refArray = new AtomicReferenceArray<>(new Object[] {expect});

    assertSame(expect, refArray.getAndSet(0, update));
    assertSame(update, refArray.get(0));
  }

  public void testLazySet() {
    Object update = new Object();

    AtomicReferenceArray<Object> refArray = new AtomicReferenceArray<>(new Object[] {new Object()});
    refArray.lazySet(0, update);

    assertSame(update, refArray.get(0));
  }

  public void testZeroLength() {
    AtomicReferenceArray<Object> refArray = new AtomicReferenceArray<>(new Object[] {});

    assertEquals(0, refArray.length());
  }

  public void testZeroLengthWithLengthConstructor() {
    AtomicReferenceArray<Object> refArray = new AtomicReferenceArray<>(0);

    assertEquals(0, refArray.length());
  }

  public void testSet() {
    Object update = new Object();

    AtomicReferenceArray<Object> refArray = new AtomicReferenceArray<>(new Object[] {new Object()});
    refArray.lazySet(0, update);

    assertSame(update, refArray.get(0));
  }

  public void testWeakCompareAndSet() {
    Object expect = new ArrayList<Object>();
    // This object is the same by |.equals| equality, but |compareAndSet| should use |==|.
    Object notExpect = new ArrayList<Object>();
    Object update = new Object();

    AtomicReferenceArray<Object> refArray = new AtomicReferenceArray<>(new Object[] {expect});

    assertFalse(refArray.compareAndSet(0, notExpect, new Object()));
    assertTrue(refArray.compareAndSet(0, expect, update));
    assertSame(update, refArray.get(0));
  }
}
