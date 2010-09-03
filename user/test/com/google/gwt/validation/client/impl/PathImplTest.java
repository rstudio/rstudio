/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.validation.client.impl;

import junit.framework.TestCase;

/**
 * Tests for {@link PathImpl}.
 */
public class PathImplTest extends TestCase {

  public void testEquals_root() {
    PathImpl root = new PathImpl();
    PathImpl rootCopy = new PathImpl();
    assertEqualsAndHash(root, rootCopy);
  }

  public void testEquals_foo() {
    PathImpl foo = new PathImpl().append("foo");
    PathImpl fooCopy = new PathImpl().append("foo");
    assertEqualsAndHash(foo, fooCopy);
  }

  public void testEqual_fooBarKey() {
    PathImpl fooBarKey = new PathImpl().append("foo").appendKey("bar", "key");
    PathImpl fooBarKeyCopy = new PathImpl().append("foo").appendKey("bar",
        "key");
    assertEqualsAndHash(fooBarKey, fooBarKeyCopy);
  }

  public void testEquals_fooBar1() {
    PathImpl fooBar1 = new PathImpl().append("foo").appendIndex("bar", 1);
    PathImpl fooBar1Copy = new PathImpl().append("foo").appendIndex("bar", 1);
    assertEqualsAndHash(fooBar1, fooBar1Copy);
  }

  public void testEquals_not() {
    PathImpl root = new PathImpl();
    PathImpl foo = new PathImpl().append("foo");
    assertNotEqual(root, foo);

    PathImpl fooBarKey = new PathImpl().append("foo").appendKey("bar", "key");
    PathImpl fooBarNote = new PathImpl().append("foo").appendKey("bar", "note");
    assertNotEqual(root, fooBarKey);
    assertNotEqual(foo, fooBarKey);
    assertNotEqual(fooBarNote, fooBarKey);

    PathImpl fooBar1 = new PathImpl().append("foo").appendIndex("bar", 1);
    PathImpl fooBar2 = new PathImpl().append("foo").appendIndex("bar", 2);
    assertNotEqual(root, fooBar1);
    assertNotEqual(foo, fooBar1);
    assertNotEqual(fooBarKey, fooBar1);
    assertNotEqual(fooBar2, fooBar1);
  }

  protected void assertNotEqual(Object lhs, Object rhs) {
    assertFalse(lhs + "should not equal " + rhs, lhs.equals(rhs));
  }

  protected void assertEqualsAndHash(Object lhs, Object rhs) {
    assertEquals(lhs, rhs);
    assertEquals("hashCode", lhs.hashCode(), rhs.hashCode());
  }
}
