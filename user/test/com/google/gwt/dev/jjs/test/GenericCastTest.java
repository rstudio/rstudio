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

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests for appropriate generation of type checks when generic methods/fields
 * are referenced. This is actually testing GenerateJavaAST's appropriate use of
 * maybeCast(). We test such references in so many contexts (field, method, as
 * qualifier, etc) to ensures we cover all the code paths, because JDT has a lot
 * of representation variants with respect to fields (SingleNameReference,
 * QualifiedNameReference, FieldAccess).
 */
@SuppressWarnings("unused")
public class GenericCastTest extends GWTTestCase {

  /**
   * Always contains an Object internally, the parameterization is a lie.
   */
  static class Liar<T> {
    @SuppressWarnings("unchecked")
    public final T value = (T) new Object();

    public T get() {
      return value;
    }
  }
  static class LiarFoo extends Liar<Foo> {
    public void testOuterField() {
      new Runnable() {
        public void run() {
          // Should succeed
          Object a = value;

          try {
            Foo b = value;
            fail("Expected ClassCastException 1");
          } catch (ClassCastException expected) {
          }
          try {
            String s = value.bar;
            fail("Expected ClassCastException 2");
          } catch (ClassCastException expected) {
          }
          try {
            String s = value.baz();
            fail("Expected ClassCastException 3");
          } catch (ClassCastException expected) {
          }
        }
      }.run();
    }

    public void testOuterMethod() {
      new Runnable() {
        public void run() {
          // Should succeed
          Object a = get();

          try {
            Foo b = get();
            fail("Expected ClassCastException 1");
          } catch (ClassCastException expected) {
          }
          try {
            String s = get().bar;
            fail("Expected ClassCastException 2");
          } catch (ClassCastException expected) {
          }
          try {
            String s = get().baz();
            fail("Expected ClassCastException 3");
          } catch (ClassCastException expected) {
          }
        }
      }.run();
    }

    public void testSuperField() {
      // Should succeed
      Object a = value;

      try {
        Foo b = value;
        fail("Expected ClassCastException 1");
      } catch (ClassCastException expected) {
      }
      try {
        String s = value.bar;
        fail("Expected ClassCastException 2");
      } catch (ClassCastException expected) {
      }
      try {
        String s = value.baz();
        fail("Expected ClassCastException 3");
      } catch (ClassCastException expected) {
      }
    }

    public void testSuperMethod() {
      // Should succeed
      Object a = get();

      try {
        Foo b = get();
        fail("Expected ClassCastException 1");
      } catch (ClassCastException expected) {
      }
      try {
        String s = get().bar;
        fail("Expected ClassCastException 2");
      } catch (ClassCastException expected) {
      }
      try {
        String s = get().baz();
        fail("Expected ClassCastException 3");
      } catch (ClassCastException expected) {
      }
    }

    void testInternalAccess() {
      new Runnable() {
        public void run() {
          Object a = get();
          try {
            Foo b = get();
            fail("Expected ClassCastException 5a");
          } catch (ClassCastException expected) {
          }
          try {
            String s = get().bar;
            fail("Expected ClassCastException 5b");
          } catch (ClassCastException expected) {
          }
          try {
            String s = get().baz();
            fail("Expected ClassCastException 5c");
          } catch (ClassCastException expected) {
          }

          Object c = value;
          try {
            Foo d = value;
            fail("Expected ClassCastException 6a");
          } catch (ClassCastException expected) {
          }
          try {
            String s = value.bar;
            fail("Expected ClassCastException 6b");
          } catch (ClassCastException expected) {
          }
          try {
            String s = value.baz();
            fail("Expected ClassCastException 6c");
          } catch (ClassCastException expected) {
          }
        }
      }.run();
    }
  }

  static class Foo {
    public String bar = "w00t";

    public String baz() {
      return bar;
    }
  }

  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  /**
   * Test explicit references through a local variable qualifier.
   */
  public void testExplicitField() {
    Liar<Foo> bug = new Liar<Foo>();

    // Should succeed
    Object a = bug.value;

    try {
      Foo b = bug.value;
      fail("Expected ClassCastException 1");
    } catch (ClassCastException expected) {
    }
    try {
      String s = bug.value.bar;
      fail("Expected ClassCastException 2");
    } catch (ClassCastException expected) {
    }
    try {
      String s = bug.value.baz();
      fail("Expected ClassCastException 3");
    } catch (ClassCastException expected) {
    }
  }

  /**
   * Test explicit references through a local variable qualifier.
   */
  public void testExplicitMethod() {
    Liar<Foo> bug = new Liar<Foo>();

    // Should succeed
    Object a = bug.get();

    try {
      Foo b = bug.get();
      fail("Expected ClassCastException 1");
    } catch (ClassCastException expected) {
    }
    try {
      String s = bug.get().bar;
      fail("Expected ClassCastException 2");
    } catch (ClassCastException expected) {
    }
    try {
      String s = bug.get().baz();
      fail("Expected ClassCastException 3");
    } catch (ClassCastException expected) {
    }
  }

  /**
   * Test implicit references through an outer class.
   */
  public void testOuterField() {
    new LiarFoo().testSuperField();
  }

  /**
   * Test implicit references through an outer class.
   */
  public void testOuterMethod() {
    new LiarFoo().testSuperMethod();
  }

  /**
   * Test implicit references through a super class.
   */
  public void testSuperField() {
    new LiarFoo().testSuperField();
  }

  /**
   * Test implicit references through a super class.
   */
  public void testSuperMethod() {
    new LiarFoo().testSuperMethod();
  }
}
