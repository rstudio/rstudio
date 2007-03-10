/*
 * Copyright 2007 Google Inc.
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
 * TODO: document me.
 */
public class InnerOuterSuperTest extends GWTTestCase {

  /**
   * TODO: document me.
   */
  public static class Outer {

    /**
     * TODO: document me.
     */
    public class OuterIsNotSuper {

      public int getValue() {
        return value;
      }
    }

    /**
     * TODO: document me.
     */
    public class OuterIsSuper extends Outer {

      public OuterIsSuper(int i) {
        super(i);
      }

      public OuterIsNotSuper unqualifiedAlloc() {
        return new OuterIsNotSuper();
      }
    }

    /**
     * TODO: document me.
     */
    public static class TestQualifiedSuperCall extends OuterIsNotSuper {
      public TestQualifiedSuperCall() {
        new Outer(1).new OuterIsSuper(2).super();
      }
    }

    /**
     * TODO: document me.
     */
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