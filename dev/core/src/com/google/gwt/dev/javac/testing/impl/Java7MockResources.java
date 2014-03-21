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
package com.google.gwt.dev.javac.testing.impl;

/**
 * Contains Java 7 source files used for testing.
 */
public class Java7MockResources {
  public static final MockJavaResource NEW_INTEGER_LITERALS_TEST =
      JavaResourceBase.createMockJavaResource("com.google.gwt.NewIntegerLiteralsTest",
          "package com.google.gwt;",
          "public class NewIntegerLiteralsTest {",
          "  int million = 1_000_000;",
          "}");

  public static final MockJavaResource SWITCH_ON_STRINGS_TEST =
      JavaResourceBase.createMockJavaResource("com.google.gwt.SwitchOnStringsTest",
          "package com.google.gwt;",
          "public class SwitchOnStringsTest {",
          "  int test() {",
          "    int result = 0;",
          "    String f = \"AA\";",
          "    switch(f) {",
          "      case \"CC\": result = - 1; break;",
          "      case \"BB\": result = 1;",
          "      case \"AA\": result = result + 1; break;",
          "      default: result = -2; break;",
          "    }",
          "  return result;",
          "  }",
          "}");

  public static final MockJavaResource DIAMOND_OPERATOR_TEST =
      JavaResourceBase.createMockJavaResource("com.google.gwt.DiamondOperatorTest",
          "package com.google.gwt;",
          "import java.util.List;",
          "import java.util.ArrayList;",
          "public class DiamondOperatorTest {",
          "  void test() {",
          "    List<String> list = new ArrayList<>();",
          "  }  ",
          "}");

  public static final MockJavaResource TRY_WITH_RESOURCES_TEST =
      JavaResourceBase.createMockJavaResource("com.google.gwt.TryWithResourcesTest",
          "package com.google.gwt;",
          "import com.google.gwt.TestResource;",
          "public class TryWithResourcesTest {",
          "  void test() { ",
          "    try (TestResource tr1 = new TestResource(); ",
          "         TestResource tr2 = new TestResource()) {",
          "    }  ",
          "  }  ",
          "}");

  public static final MockJavaResource MULTI_EXCEPTION_TEST =
      JavaResourceBase.createMockJavaResource("com.google.gwt.MultiExceptionTest",
          "package com.google.gwt;",
          "import com.google.gwt.Exception1;",
          "import com.google.gwt.Exception2;",
          "public class MultiExceptionTest {",
          "  void test() { ",
          "    int i = 1;",
          "    try {",
          "      if (i > 0) {",
          "        throw new Exception1();",
          "      } else {",
          "        throw new Exception2();",
          "      }",
          "    } catch (Exception1 | Exception2 e) { ",
          "    }",
          "  } ",
          "}");

  public static final MockJavaResource TEST_RESOURCE =
      JavaResourceBase.createMockJavaResource("com.google.gwt.TestResource",
          "package com.google.gwt;",
          "public class TestResource implements AutoCloseable {",
          "  public void close() { }",
          "}");

  public static final MockJavaResource EXCEPTION1 =
      JavaResourceBase.createMockJavaResource("com.google.gwt.Exception1",
          "package com.google.gwt;",
          "import java.lang.Exception;",
          "public class Exception1 extends Exception {",
          "}");

  public static final MockJavaResource EXCEPTION2 =
      JavaResourceBase.createMockJavaResource("com.google.gwt.Exception2",
          "package com.google.gwt;",
          "import java.lang.Exception;",
          "public class Exception2 extends Exception {",
          "}");
}
