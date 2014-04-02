/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev.shell;

import junit.framework.TestCase;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

/**
 * Tests DispatchClassInfoTest.
 */
public class DispatchClassInfoTest extends TestCase {
  /**
   * Used in {@link #testInterface()}.
   */
  private interface Intf {
    void bar(double x);

    void bar(int x);

    void foo(int x);
  }

  public void testBasics() {
    @SuppressWarnings("unused")
    class Foo {
      int field;

      void nonOverloaded(int x) {
      }

      void overloaded(double x) {
      }

      void overloaded(int x) {
      }
    }

    DispatchClassInfo dci = new DispatchClassInfo(Foo.class, 42);
    assertField(dci, "field", "field");
    assertNonExistent(dci, "bogofield");
    assertMethod(dci, "nonOverloaded(I)", Foo.class, "nonOverloaded",
        Integer.TYPE);
    assertMethod(dci, "nonOverloaded(*)", Foo.class, "nonOverloaded",
        Integer.TYPE);
    assertMethod(dci, "overloaded(I)", Foo.class, "overloaded", Integer.TYPE);
    assertMethod(dci, "overloaded(D)", Foo.class, "overloaded", Double.TYPE);
    assertNonExistent(dci, "overloaded(*)");
    assertNonExistent(dci, "bogometh(I)");
    assertNonExistent(dci, "bogometh(*)");
  }

  public void testInheritance() {
    /*
     * In these two classes, foo is overloaded in the superclass but not the
     * subclass, and bar vice versa.
     */
    @SuppressWarnings("unused")
    class Super {
      int field;

      void bar(int x) {
      }

      void foo(double x) {
      }

      void foo(int x) {
      }

      void nonover(int x) {
      }

      void over(double x) {
      }

      void over(int x) {
      }
    }

    @SuppressWarnings("unused")
    class Sub extends Super {
      void bar(double x) {
      }

      @Override
      void bar(int x) {
      }

      @Override
      void foo(int x) {
      }
    }

    DispatchClassInfo dci = new DispatchClassInfo(Sub.class, 42);

    assertField(dci, "field", "field");
    assertMethod(dci, "foo(I)", Sub.class, "foo", Integer.TYPE);
    assertMethod(dci, "bar(I)", Sub.class, "bar", Integer.TYPE);
    assertMethod(dci, "bar(D)", Sub.class, "bar", Double.TYPE);
    assertNonExistent(dci, "foo(*)");
    assertNonExistent(dci, "bar(*)");

    assertMethod(dci, "nonover(I)", Super.class, "nonover", Integer.TYPE);
    assertMethod(dci, "nonover(*)", Super.class, "nonover", Integer.TYPE);

    assertMethod(dci, "over(I)", Super.class, "over", Integer.TYPE);
    assertMethod(dci, "over(D)", Super.class, "over", Double.TYPE);
    assertNonExistent(dci, "over(*)");
  }

  public void testInterface() {
    DispatchClassInfo dci = new DispatchClassInfo(Intf.class, 42);

    assertMethod(dci, "foo(I)", Intf.class, "foo", Integer.TYPE);
    assertMethod(dci, "foo(*)", Intf.class, "foo", Integer.TYPE);
    assertMethod(dci, "bar(I)", Intf.class, "bar", Integer.TYPE);
    assertMethod(dci, "bar(D)", Intf.class, "bar", Double.TYPE);
    assertNonExistent(dci, "bar(*)");
  }

  /**
   * Test that bridge methods are ignored for wildcard lookups.
   */
  public void testBridgeMethod() {
    @SuppressWarnings("unused")
    abstract class Super<T> {
      abstract void set(T x);
    }

    class Sub extends Super<String> {
      @Override
      void set(String x) {
      }
    }

    DispatchClassInfo dci = new DispatchClassInfo(Sub.class, 42);

    assertMethod(dci, "set(Ljava/lang/String;)", Sub.class, "set", String.class);
    assertMethod(dci, "set(*)", Sub.class, "set", String.class);
    
    // For backward compatibility, allow calling a bridge method directly
    assertMethod(dci, "set(Ljava/lang/Object;)", Sub.class, "set", Object.class);
  }

  private void assertField(DispatchClassInfo dci, String ref, String fieldName) {
    Member member = lookupMember(dci, ref);
    Field field = (Field) member;
    assertEquals(fieldName, field.getName());
  }

  private void assertMethod(DispatchClassInfo dci, String ref,
      Class<?> methodClass, String methodName, Class<?> paramType) {
    Member member = lookupMember(dci, ref);
    Method method = (Method) member;
    assertSame(methodClass, member.getDeclaringClass());
    assertEquals(methodName, method.getName());
    assertEquals(1, method.getParameterTypes().length);
    assertEquals(paramType, method.getParameterTypes()[0]);
  }

  private void assertNonExistent(DispatchClassInfo dci, String badref) {
    int handle = dci.getMemberId(badref);
    assertTrue("expected to be a bad reference: " + badref, handle < 0);
  }

  private Member lookupMember(DispatchClassInfo dci, String ref) {
    int handle = dci.getMemberId(ref);
    assertTrue("ref lookup failed: " + ref, handle >= 0);
    Member member = dci.getMember(handle);
    return member;
  }
}
