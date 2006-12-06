// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.test;

import com.google.gwt.junit.client.GWTTestCase;

public class MethodBindTest extends GWTTestCase {

  private static abstract class Abstract implements Go {
    private final class Nested implements Go {
      public void go() {
        result = "wrong";
      }

      void nestedTrigger() {
        Abstract.this.go();
      }
    }

    private final class Nested2 extends Abstract {
      public void go() {
        result = "wrong";
      }

      void nestedTrigger() {
        Abstract.this.go();
      }
    }

    void trigger() {
      new Nested().nestedTrigger();
    }

    void trigger2() {
      new Nested2().nestedTrigger();
    }
  }

  private final class Concrete extends Abstract {
    public void go() {
      result = "right";
    }
  }

  private interface Go {
    void go();
  }

  private static String result;

  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public void testMethodBinding() {
    result = null;
    new Concrete().trigger();
    assertEquals("right", result);
    result = null;
    new Concrete().trigger2();
    assertEquals("right", result);
  }

}