/*
 * Copyright 2013 Google Inc.
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Tests Java 7 features. It is super sourced so that gwt can be compiles under Java 6.
 *
 * IMPORTANT: For each test here there must exist the corresponding method in the non super sourced
 * version.
 *
 * Eventually this test will graduate and not be super sourced.
 */
public class Java7Test extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.Java7Test";
  }

  // new style class literals
// CHECKSTYLE:OFF
  int million = 1_000_000;

  int five = 0b101;
// CHECKSTYLE:ON

  public void testNewStyleLiterals() {
    assertEquals(1000000, million);
    assertEquals(5, five);
  }

  public void testSwitchOnString() {

    String s = "AA";
    int result = -1;
    switch (s) {
      case "BB":
        result = 0;
        break;
      case "CC":
      case "AA":
        result = 1;
        break;
    }
    assertEquals(1, result);
  }

  final List<String> log = new ArrayList<String>();

  public class Resource implements AutoCloseable {

    String name;
    public Resource(String name) {
      this.name = name;
      log.add("Open " + name);
    }

    public void doSomething() {
      log.add("doSomething " + name);
    }

    public void throwException(String text) throws E1 {
      throw new E1(text + " in " + name);
    }

    public void close() throws Exception {
      log.add("Close " + name);
    }
  }

  public class ResourceWithExceptionOnClose extends Resource {


    public ResourceWithExceptionOnClose(String name) {
      super(name);
    }

    public void close() throws Exception {
      throw new E1("Exception in close " + name);
    }
  }

  public void testResource() throws Exception{
    log.clear();
    try (Resource c = new Resource("A")) {

      c.doSomething();
    }

    assertContentsInOrder(log,
        "Open A",
        "doSomething A",
        "Close A");
  }

  public void test3Resources() throws Exception{
    log.clear();
    try (Resource rA = new Resource("A");
        Resource rB = new Resource("B");
        Resource rC = new Resource("C")) {

      rA.doSomething();
      rB.doSomething();
      rC.doSomething();
    }

    assertContentsInOrder(log,
        "Open A",
        "Open B",
        "Open C",
        "doSomething A",
        "doSomething B",
        "doSomething C",
        "Close C",
        "Close B",
        "Close A"
    );
  }

  public void testResourcesWithExceptions() throws Exception{
    log.clear();
    try (Resource rA = new Resource("A");
        Resource rB = new ResourceWithExceptionOnClose("B");
        Resource rC = new Resource("C")) {

      rA.doSomething();
      rB.doSomething();
      rC.doSomething();
    } catch (Exception e) {
      log.add(e.getMessage());
    } finally {
      log.add("finally");
    }

    assertContentsInOrder(log,
        "Open A",
        "Open B",
        "Open C",
        "doSomething A",
        "doSomething B",
        "doSomething C",
        "Close C",
        "Close A",
        "Exception in close B",
        "finally"
        );
  }

  public void testResourcesWithSuppressedExceptions() throws Exception{
    log.clear();
    try (Resource rA = new ResourceWithExceptionOnClose("A");
        Resource rB = new Resource("B");
        Resource rC = new ResourceWithExceptionOnClose("C")) {

      rA.doSomething();
      rB.doSomething();
      rC.doSomething();
    } catch (Exception e) {
      log.add(e.getMessage());
      for (Throwable t : e.getSuppressed()) {
        log.add("Suppressed: " + t.getMessage());
      }
    }

    assertContentsInOrder(log,
        "Open A",
        "Open B",
        "Open C",
        "doSomething A",
        "doSomething B",
        "doSomething C",
        "Close B",
        "Exception in close C",
        "Suppressed: Exception in close A"
    );

    log.clear();
    try (Resource rA = new Resource("A");
        Resource rB = new ResourceWithExceptionOnClose("B");
        Resource rC = new Resource("C")) {

      rA.doSomething();
      rB.throwException("E1 here");
      rC.doSomething();
    } catch (Exception e) {
      log.add(e.getMessage());
      for (Throwable t : e.getSuppressed()) {
        log.add("Suppressed: " + t.getMessage());
      }
    } finally {
      log.add("finally");
    }

    assertContentsInOrder(log,
        "Open A",
        "Open B",
        "Open C",
        "doSomething A",
        "Close C",
        "Close A",
        "E1 here in B",
        "Suppressed: Exception in close B",
        "finally"
        );
  }

  public void testAddSuppressedExceptions() {
    Throwable throwable = new Throwable("primary");
    assertNotNull(throwable.getSuppressed());
    assertEquals(0, throwable.getSuppressed().length);
    Throwable suppressed1 = new Throwable("suppressed1");
    throwable.addSuppressed(suppressed1);
    assertEquals(1, throwable.getSuppressed().length);
    assertEquals(suppressed1, throwable.getSuppressed()[0]);
    Throwable suppressed2 = new Throwable("suppressed2");
    throwable.addSuppressed(suppressed2);
    assertEquals(2, throwable.getSuppressed().length);
    assertEquals(suppressed1, throwable.getSuppressed()[0]);
    assertEquals(suppressed2, throwable.getSuppressed()[1]);
  }

  private void assertContentsInOrder(Iterable<String> contents, String... elements ) {
    int sz = elements.length;
    Iterator<String> it = contents.iterator();
    for (int i = 0; i < sz; i++) {
      assertTrue(it.hasNext());
      String expected = it.next();
      assertEquals(elements[i], expected);
    }
    assertFalse(it.hasNext());
  }

  public static class E1 extends Exception {
    String name;
    public E1(String name) {
      this.name = name;
    }

    public int methodE1() {
      return 0;
    }

    @Override
    public String getMessage() {
      return name;
    }
  }

  public static class E2 extends E1 {
    public E2(String name) {
      super(name);
    }

    public int methodE2() {
      return 1;
    }
  }

  public static class E3 extends E1 {
    public E3(String name) {
      super(name);
    }

    public int methodE3() {
      return 2;
    }
  }

  public void testMultiExceptions() {

    int choose = 0;

    try {
      if (choose == 0) {
        throw new E1("e1");
      } else if (choose ==1) {
        throw new E2("e2");
      }

      fail("Exception was not trown");
    } catch (E2 | E3 x) {
      // The compiler will assign x a common supertype/superinterface of E2 and E3.
      // Here we make sure that this clause is not entered when the supertype is thrown.
      fail("Caught E1 instead of E2|E3");
    } catch (E1 x) {
    }
  }

  private Object unoptimizableId(Object o) {
    if (Math.random() > -10) {
      return o;
    }
    return null;
  }

  public void testPrimitiveCastsFromObject() {
    Object o = unoptimizableId((byte) 2);
    assertEquals((byte) 2, (byte) o);
    o = unoptimizableId((short) 3);
    assertEquals((short) 3, (short) o);
    o = unoptimizableId(1);
    assertEquals(1, (int) o);
    o = unoptimizableId(1L);
    assertEquals(1L, (long) o);
    o = unoptimizableId(0.1f);
    assertEquals(0.1f, (float) o);
    o = unoptimizableId(0.1);
    assertEquals(0.1, (double) o);
    o = unoptimizableId(true);
    assertEquals(true, (boolean) o);
    o = unoptimizableId('a');
    assertEquals('a', (char) o);
    // Test cast from supers.
    // TODO(rluble): enable these after JDT upgrade as the currenct JDT will
    // give compilation errors.
    // Number n = (Number) unoptimizableId(5);
    // assertEquals(5, (int) n);
    // Serializable s = (Serializable) unoptimizableId(6);
    // assertEquals(6, (int) s);
    // Comparable<Integer> c = (Comparable<Integer>) unoptimizableId(7);
    // assertEquals(7, (int) c);

    // Failing casts.
    try {
      Object boxedChar = unoptimizableId('a');
      boolean b = (boolean) boxedChar;
      fail("Should have thrown a ClassCastException");
    } catch (ClassCastException e) {
      // Expected.
    }

    try {
      Object string = unoptimizableId("string");
      int n = (int) string;
      fail("Should have thrown a ClassCastException");
    } catch (ClassCastException e) {
      // Expected.
    }
  }
}
