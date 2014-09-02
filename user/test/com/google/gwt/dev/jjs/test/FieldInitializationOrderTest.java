/*
 * Copyright 2014 Google Inc.
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
 * Tests that initialization order for field follow the "quirky" Java semantics.
 */
public class FieldInitializationOrderTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  static abstract class Tester {
    abstract void performTest();
  }

  static class ConstructorCastClasses extends Tester {
    static class Base {
      Base() {
        assertEquals(0, ((Child) this).i);
      }
    }

    static class Child extends Base {
      int i = 1;
    }

    void performTest() {
      new Child();
    }
  }

  static class InitializerCastClasses extends Tester {
    static class Base {
      {
        assertEquals(0, ((Child) this).i);
      }
    }

    static class Child extends Base {
      int i = 1;
    }

    void performTest() {
      new Child();
    }
  }

  static class InitializerPolymorphicDispatchClasses  extends Tester {
    static class Base {
      {
        m();
      }
      void m() { }
    }

    static class Child extends Base {
      int i = 1;
      void m() { assertEquals(0, i); }
    }

    void performTest() {
      // Construct both parent and child to ensure that polymorphic dispatch for m() does not go
      // away
      new Base();
      new Child();
    }
  }

  static class ConstructorPolymorphicDispatchClasses extends Tester {
    static class Base {
      Base() {
        m();
      }
      void m() { }
    }

    static class Child extends Base {
      int i = 1;
      void m() { assertEquals(0, i); }
    }
    void performTest() {
      // Construct both parent and child to ensure that polymorphic dispatch for m() does not go
      // away
      new Base();
      new Child();
    }
  }

  static class IncorrectlyOptimizedCondition extends Tester {
    static class Base {
      Base() {
        m();
      }
      void m() { }
    }

    static class Child extends Base {
      String s = "blah";
      void m() { assertTrue(s == null); }
    }
    void performTest() {
      // Construct both parent and child to ensure that polymorphic dispatch for m() does not go
      // away
      new Base();
      new Child();
    }
  }

  public void testInitializerCast() {
    new InitializerCastClasses().performTest();
  }

  public void testConstructorCast() {
    new ConstructorCastClasses().performTest();
  }

  public void testInitializerPolymorphicDispatch() {
    new InitializerPolymorphicDispatchClasses().performTest();
  }

  public void testConstructorPolymorphicDispatch() {
    new ConstructorPolymorphicDispatchClasses().performTest();
  }

  public void testIncorrectlyOptimizedCondition() {
    new IncorrectlyOptimizedCondition().performTest();
  }
}
