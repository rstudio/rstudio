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
package com.google.gwt.dev.javac;

import com.google.gwt.dev.javac.testing.impl.JavaResourceBase;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;

/**
 * Regression tests for {@link CompilationStateBuilder}.
 */
public class CompilationStateBuilderTest extends CheckerTestCase {

  /**
   * Tests that compiling a unit that declares a class that shadows another
   * results in the proper error message.
   */
  public void testNestedClasses_IllegalNaming() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("some.A",
        "package some;",
        "class A {",
        "  static int f;",
        "  static class B {",
        "    static int f;",
        "    static class A {",
        "      static int f;",
        "    }",
        "  }",
        "}");

    shouldGenerateError(buggy, 6, "The nested type A cannot hide an enclosing type");
  }

  /**
   * Tests that compiling a unit that declares a class that shadows another
   * results in the proper error message. Similar to the previous test but
   * used to throw an NPE.
   */
  public void testNestedClassesWithJsni_IllegalNaming() {
    MockJavaResource buggy = JavaResourceBase.createMockJavaResource("some.A",
        "package some;",
        "class A {",
        "  static int f;",
        "  static class B {",
        "    static int f;",
        "    static class A {",
        "      static int f;",
        "      native void jsniMethod() /*-{",
        "        @A::f;",
        "      }-*/;",
        "    }",
        "  }",
        "}");

    shouldGenerateError(buggy, 6, "The nested type A cannot hide an enclosing type");
  }
}
