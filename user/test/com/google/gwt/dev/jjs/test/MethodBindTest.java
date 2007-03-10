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
public class MethodBindTest extends GWTTestCase {

  /**
   * TODO: document me.
   */
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