// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.test;

import com.google.gwt.junit.client.GWTTestCase;

public class InnerOuterSuperTest extends GWTTestCase {

  public static class Outer {

    public class OuterIsNotSuper {

      public int getValue() {
        return value;
      }

    }

    public class OuterIsSuper extends Outer {

      public OuterIsSuper(int i) {
        super(i);
      }

      public OuterIsNotSuper unqualifiedAlloc() {
        return new OuterIsNotSuper();
      }

    }

    public static class TestQualifiedSuperCall extends OuterIsNotSuper {
      public TestQualifiedSuperCall() {
        new Outer(1).new OuterIsSuper(2).super();
      }
    }

    public class TestUnqualifiedSuperCall extends OuterIsNotSuper {
      public TestUnqualifiedSuperCall() {
        super();
      }
    }

    protected final int value;

    public Outer(int i) {
      value = i;
    }
  }

  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public void testInnerOuterSuper() {
    Outer outer = new Outer(1);
    Outer.OuterIsSuper outerIsSuper = outer.new OuterIsSuper(2);

    {
      // QualifiedAlloc: outer becomes Outer or OuterIsSuper???
      Outer.OuterIsNotSuper outerIsNotSuper = outerIsSuper.new OuterIsNotSuper();
      int whatIsIt = outerIsNotSuper.getValue();
      assertEquals(2, whatIsIt);
    }

    {
      // [unqualified]Alloc: outer becomes Outer or OuterIsSuper???
      Outer.OuterIsNotSuper outerIsNotSuper = outerIsSuper.unqualifiedAlloc();
      int whatIsIt = outerIsNotSuper.getValue();
      assertEquals(2, whatIsIt);
    }

    {
      // QualifiedSupercall: outer becomes Outer or OuterIsSuper???
      Outer.TestQualifiedSuperCall testQualifiedSuperCall = new Outer.TestQualifiedSuperCall();
      int whatIsIt = testQualifiedSuperCall.getValue();
      assertEquals(2, whatIsIt);
    }

    {
      // UnqualifiedSupercall: outer becomes Outer or OuterIsSuper???
      Outer.TestUnqualifiedSuperCall testUnqualifiedSuperCall = outerIsSuper.new TestUnqualifiedSuperCall();
      int whatIsIt = testUnqualifiedSuperCall.getValue();
      assertEquals(2, whatIsIt);
    }
  }

}