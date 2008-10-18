/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.dev.jjs.test;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Test comparison of various types of objects under various circumstances. Its
 * main purpose is to ensure that identity comparisons work properly whether
 * object values are null, non-null, or undefined (which is now being treated as
 * equivalent to null); and also ensure no String equality comparisons occur.
 */
public class ObjectIdentityTest extends GWTTestCase {

  /**
   * An object that toString()s to a given value, used to ensure String equality
   * is never used inappropriately.
   */
  private static class Foo {
    private String s;

    public Foo(String s) {
      this.s = s;
    }

    @Override
    public String toString() {
      return s;
    }
  }

  private static volatile boolean TRUE = true;

  /*
   * Using volatile to ensure that the compiler is unsure about null-ness.
   */
  private volatile String maybeNullStringIsNull = null;
  private volatile Object maybeNullObjectIsNull = null;
  private volatile Foo maybeNullFooIsNull = null;
  private Object maybeNullTightenStringIsNull = maybeNullStringIsNull;
  private Object maybeNullTightenFooIsNull = maybeNullFooIsNull;

  private volatile String maybeNullStringIsUndefined = undefinedString();
  private volatile Object maybeNullObjectIsUndefined = undefinedObject();
  private volatile Foo maybeNullFooIsUndefined = undefinedFoo();
  private Object maybeNullTightenStringIsUndefined = maybeNullStringIsUndefined;
  private Object maybeNullTightenFooIsUndefined = maybeNullFooIsUndefined;

  private String notNullString = "foo";
  private String notNullStringOther = "bar";
  private Object notNullObject = TRUE ? new Foo(notNullString) : new Object();
  private Object notNullObjectOther = TRUE ? new Foo(notNullStringOther)
      : new Object();
  private Foo notNullFoo = new Foo(notNullString);
  private Foo notNullFooOther = new Foo(notNullStringOther);
  private Object notNullTightenString = notNullString;
  private Object notNullTightenFoo = notNullFoo;

  private volatile String maybeNullString = notNullString;
  private volatile String maybeNullStringOther = notNullStringOther;
  private volatile Object maybeNullObject = notNullObject;
  private volatile Object maybeNullObjectOther = notNullObjectOther;
  private volatile Foo maybeNullFoo = notNullFoo;
  private volatile Foo maybeNullFooOther = notNullFooOther;
  private Object maybeNullTightenString = maybeNullString;
  private Object maybeNullTightenFoo = maybeNullFoo;

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public void test_MaybeNullFoo_MaybeNullFoo() {
    assertTrue(maybeNullFoo == maybeNullFoo);
    assertTrue(maybeNullFoo != maybeNullFooOther);
    assertTrue(maybeNullFooIsNull != maybeNullFoo);
    assertTrue(maybeNullFooIsUndefined != maybeNullFoo);
    assertTrue(maybeNullFooIsNull == maybeNullFooIsUndefined);
    assertTrue(maybeNullFooIsUndefined == maybeNullFooIsNull);
  }

  public void test_MaybeNullFoo_MaybeNullObject() {
    assertTrue(maybeNullFoo != maybeNullObject);
    assertTrue(maybeNullFooIsNull != maybeNullObject);
    assertTrue(maybeNullFooIsUndefined != maybeNullObject);
    assertTrue(maybeNullFooIsNull == maybeNullObjectIsNull);
    assertTrue(maybeNullFooIsNull == maybeNullObjectIsUndefined);
    assertTrue(maybeNullFooIsUndefined == maybeNullObjectIsNull);
    assertTrue(maybeNullFooIsUndefined == maybeNullObjectIsUndefined);
  }

  public void test_MaybeNullFoo_MaybeNullString() {
    assertTrue(maybeNullFoo != maybeNullTightenString);
    assertTrue(maybeNullFooIsNull != maybeNullTightenString);
    assertTrue(maybeNullFooIsUndefined != maybeNullTightenString);
    assertTrue(maybeNullFooIsNull == maybeNullTightenStringIsNull);
    assertTrue(maybeNullFooIsNull == maybeNullTightenStringIsUndefined);
    assertTrue(maybeNullFooIsUndefined == maybeNullTightenStringIsNull);
    assertTrue(maybeNullFooIsUndefined == maybeNullTightenStringIsUndefined);
  }

  public void test_MaybeNullFoo_NotNullFoo() {
    assertTrue(maybeNullFoo == notNullFoo);
    assertTrue(maybeNullFoo != notNullFooOther);
    assertTrue(maybeNullFooIsNull != notNullFoo);
    assertTrue(maybeNullFooIsUndefined != notNullFoo);
  }

  public void test_MaybeNullFoo_NotNullObject() {
    assertTrue(maybeNullFoo != notNullObject);
    assertTrue(maybeNullFooIsNull != notNullObject);
    assertTrue(maybeNullFooIsUndefined != notNullObject);
  }

  public void test_MaybeNullFoo_NotNullString() {
    assertTrue(maybeNullFoo != notNullTightenString);
    assertTrue(maybeNullFooIsNull != notNullTightenString);
    assertTrue(maybeNullFooIsUndefined != notNullTightenString);
  }

  public void test_MaybeNullFoo_null() {
    assertTrue(maybeNullFoo != null);
    assertTrue(maybeNullFooIsNull == null);
    assertTrue(maybeNullFooIsUndefined == null);
  }

  public void test_MaybeNullObject_MaybeNullFoo() {
    assertTrue(maybeNullObject != maybeNullFoo);
    assertTrue(maybeNullObjectIsNull != maybeNullFoo);
    assertTrue(maybeNullObjectIsUndefined != maybeNullFoo);
    assertTrue(maybeNullObjectIsNull == maybeNullFooIsNull);
    assertTrue(maybeNullObjectIsNull == maybeNullFooIsUndefined);
    assertTrue(maybeNullObjectIsUndefined == maybeNullFooIsNull);
    assertTrue(maybeNullObjectIsUndefined == maybeNullFooIsUndefined);
  }

  public void test_MaybeNullObject_MaybeNullObject() {
    assertTrue(maybeNullObject == maybeNullObject);
    assertTrue(maybeNullObject != maybeNullObjectOther);
    assertTrue(maybeNullObjectIsNull != maybeNullObject);
    assertTrue(maybeNullObjectIsUndefined != maybeNullObject);
    assertTrue(maybeNullObjectIsNull == maybeNullObjectIsUndefined);
    assertTrue(maybeNullObjectIsUndefined == maybeNullObjectIsNull);
  }

  public void test_MaybeNullObject_MaybeNullString() {
    assertTrue(maybeNullObject != maybeNullString);
    assertTrue(maybeNullObjectIsNull != maybeNullString);
    assertTrue(maybeNullObjectIsUndefined != maybeNullString);
    assertTrue(maybeNullObjectIsNull == maybeNullStringIsNull);
    assertTrue(maybeNullObjectIsNull == maybeNullStringIsUndefined);
    assertTrue(maybeNullObjectIsUndefined == maybeNullStringIsNull);
    assertTrue(maybeNullObjectIsUndefined == maybeNullStringIsUndefined);
  }

  public void test_MaybeNullObject_NotNullFoo() {
    assertTrue(maybeNullObject != notNullFoo);
    assertTrue(maybeNullObjectIsNull != notNullFoo);
    assertTrue(maybeNullObjectIsUndefined != notNullFoo);
  }

  public void test_MaybeNullObject_NotNullObject() {
    assertTrue(maybeNullObject == notNullObject);
    assertTrue(maybeNullObjectIsNull != notNullObject);
    assertTrue(maybeNullObjectIsUndefined != notNullObject);
  }

  public void test_MaybeNullObject_NotNullString() {
    assertTrue(maybeNullObject != notNullString);
    assertTrue(maybeNullObjectIsNull != notNullString);
    assertTrue(maybeNullObjectIsUndefined != notNullString);
  }

  public void test_MaybeNullObject_null() {
    assertTrue(maybeNullObject != null);
    assertTrue(maybeNullObjectIsNull == null);
    assertTrue(maybeNullObjectIsUndefined == null);
  }

  public void test_MaybeNullString_MaybeNullFoo() {
    assertTrue(maybeNullString != maybeNullTightenFoo);
    assertTrue(maybeNullStringIsNull != maybeNullTightenFoo);
    assertTrue(maybeNullStringIsUndefined != maybeNullTightenFoo);
    assertTrue(maybeNullStringIsNull == maybeNullTightenFooIsNull);
    assertTrue(maybeNullStringIsNull == maybeNullTightenFooIsUndefined);
    assertTrue(maybeNullStringIsUndefined == maybeNullTightenFooIsNull);
    assertTrue(maybeNullStringIsUndefined == maybeNullTightenFooIsUndefined);
  }

  public void test_MaybeNullString_MaybeNullObject() {
    assertTrue(maybeNullString != maybeNullObject);
    assertTrue(maybeNullStringIsNull != maybeNullObject);
    assertTrue(maybeNullStringIsUndefined != maybeNullObject);
    assertTrue(maybeNullStringIsUndefined == maybeNullObjectIsNull);
    assertTrue(maybeNullStringIsUndefined == maybeNullObjectIsUndefined);
    assertTrue(maybeNullStringIsUndefined == maybeNullObjectIsNull);
    assertTrue(maybeNullStringIsUndefined == maybeNullObjectIsUndefined);
  }

  public void test_MaybeNullString_MaybeNullString() {
    assertTrue(maybeNullString == maybeNullString);
    assertTrue(maybeNullString != maybeNullStringOther);
    assertTrue(maybeNullStringIsNull != maybeNullString);
    assertTrue(maybeNullStringIsUndefined != maybeNullString);
    assertTrue(maybeNullStringIsNull == maybeNullStringIsNull);
    assertTrue(maybeNullStringIsNull == maybeNullStringIsUndefined);
    assertTrue(maybeNullStringIsUndefined == maybeNullStringIsNull);
    assertTrue(maybeNullStringIsUndefined == maybeNullStringIsUndefined);
  }

  public void test_MaybeNullString_NotNullFoo() {
    assertTrue(maybeNullString != notNullTightenFoo);
    assertTrue(maybeNullStringIsNull != notNullTightenFoo);
    assertTrue(maybeNullStringIsUndefined != notNullTightenFoo);
  }

  public void test_MaybeNullString_NotNullObject() {
    assertTrue(maybeNullString != notNullObject);
    assertTrue(maybeNullStringIsNull != notNullObject);
    assertTrue(maybeNullStringIsUndefined != notNullObject);
  }

  public void test_MaybeNullString_NotNullString() {
    assertTrue(maybeNullString == notNullString);
    assertTrue(maybeNullStringIsNull != notNullString);
    assertTrue(maybeNullStringIsUndefined != notNullString);
  }

  public void test_MaybeNullString_null() {
    assertTrue(maybeNullString != null);
    assertTrue(maybeNullStringIsNull == null);
    assertTrue(maybeNullStringIsUndefined == null);
  }

  public void test_NotNullFoo_NotNullFoo() {
    assertTrue(notNullFoo == notNullFoo);
    assertTrue(notNullFoo != notNullFooOther);
  }

  public void test_NotNullFoo_NotNullObject() {
    assertTrue(notNullFoo != notNullObject);
  }

  public void test_NotNullFoo_NotNullString() {
    assertTrue(notNullFoo != notNullTightenString);
  }

  public void test_NotNullFoo_null() {
    assertTrue(notNullFoo != null);
  }

  public void test_NotNullObject_NotNullFoo() {
    assertTrue(notNullObject != notNullFoo);
  }

  public void test_NotNullObject_NotNullObject() {
    assertTrue(notNullObject == notNullObject);
    assertTrue(notNullObject != notNullObjectOther);
  }

  public void test_NotNullObject_NotNullString() {
    assertTrue(notNullObject != notNullString);
  }

  public void test_NotNullObject_null() {
    assertTrue(notNullObject != null);
  }

  public void test_NotNullString_NotNullFoo() {
    assertTrue(notNullString != notNullTightenFoo);
  }

  public void test_NotNullString_NotNullObject() {
    assertTrue(notNullString != notNullObject);
  }

  public void test_NotNullString_NotNullString() {
    assertTrue(notNullString == notNullString);
    assertTrue(notNullString != notNullStringOther);
  }

  public void test_NotNullString_null() {
    assertTrue(notNullString != null);
  }
  public void test_null_MaybeNullFoo() {
    assertTrue(null != maybeNullFoo);
    assertTrue(null == maybeNullFooIsNull);
    assertTrue(null == maybeNullFooIsUndefined);
  }

  public void test_null_MaybeNullObject() {
    assertTrue(null != maybeNullObject);
    assertTrue(null == maybeNullObjectIsNull);
    assertTrue(null == maybeNullObjectIsUndefined);
  }

  public void test_null_MaybeNullString() {
    assertTrue(null != maybeNullString);
    assertTrue(null == maybeNullStringIsNull);
    assertTrue(null == maybeNullStringIsUndefined);
  }

  public void test_null_NotNullFoo() {
    assertTrue(null != notNullFoo);
  }

  public void test_null_NotNullObject() {
    assertTrue(null != notNullObject);
  }

  public void test_null_NotNullString() {
    assertTrue(null != notNullString);
  }

  public void test_null_null() {
    assertTrue(null == null);
  }

  private Foo undefinedFoo() {
    return GWT.isClient() ? undefinedFoo0() : null;
  }

  private Object undefinedObject() {
    return GWT.isClient() ? undefinedObject0() : null;
  }

  private String undefinedString() {
    return GWT.isClient() ? undefinedString0() : null;
  }

  private native Foo undefinedFoo0() /*-{
  }-*/;

  private native Object undefinedObject0() /*-{
  }-*/;

  private native String undefinedString0() /*-{
  }-*/;
}
