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

      public int checkDispatch() {
        return 2;
      }
      
      public int checkDispatchFromSub1() {
        return super.checkDispatch();
      }

      public int checkDispatchFromSub2() {
        return new Outer(1) {
          public int go() {
            return OuterIsSuper.super.checkDispatch();
          }
        }.go();
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
    
    public int checkDispatch() {
      return 1;
    }
  }

  private final Outer outer  = new Outer(1);

  private final Outer.OuterIsSuper outerIsSuper = outer.new OuterIsSuper(2);

  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public void testOuterIsNotSuper() {
    Outer.OuterIsNotSuper x = outerIsSuper.new OuterIsNotSuper();
    assertEquals(2, x.getValue());
  }

  public void testOuterIsNotSuperAnon() {
    Outer.OuterIsNotSuper x = outerIsSuper.new OuterIsNotSuper() {
    };
    assertEquals(2, x.getValue());
  }

  public void testQualifiedSuperCall() {
    Outer.TestQualifiedSuperCall x = new Outer.TestQualifiedSuperCall();
    assertEquals(2, x.getValue());
  }

  public void testQualifiedSuperCallAnon() {
    Outer.TestQualifiedSuperCall x = new Outer.TestQualifiedSuperCall() {
    };
    assertEquals(2, x.getValue());
  }

  public void testSuperDispatch() {
    assertEquals(1, outerIsSuper.checkDispatchFromSub1());
    assertEquals(1, outerIsSuper.checkDispatchFromSub2());
  }

  public void testUnqualifiedAlloc() {
    Outer.OuterIsNotSuper x = outerIsSuper.unqualifiedAlloc();
    assertEquals(2, x.getValue());
  }

  public void testUnqualifiedSuperCall() {
    Outer.TestUnqualifiedSuperCall x = outerIsSuper.new TestUnqualifiedSuperCall();
    assertEquals(2, x.getValue());
  }

  public void testUnqualifiedSuperCallAnon() {
    Outer.TestUnqualifiedSuperCall x = outerIsSuper.new TestUnqualifiedSuperCall() {
    };
    assertEquals(2, x.getValue());
  }
}
