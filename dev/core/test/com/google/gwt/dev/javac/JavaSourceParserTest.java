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
package com.google.gwt.dev.javac;

import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.javac.typemodel.JClassType;
import com.google.gwt.dev.javac.typemodel.JMethod;

import java.util.Arrays;
import java.util.List;

/**
 * Tests for {@link JavaSourceParser}.
 */
public class JavaSourceParserTest extends CompilationStateTestBase {

  private static final MockJavaResource BAR = new MockJavaResource("test.Bar") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package test;\n");
      code.append("public class Bar {\n");
      code.append("  public String value(String a, int val) { return \"Bar\"; }\n");
      code.append("  public String value(String a) { return \"Bar\"; }\n");
      code.append("  public String value(int val) { return \"Bar\"; }\n");
      code.append("}\n");
      return code;
    }
  };

  private static final MockJavaResource BAZ = new MockJavaResource("test.Baz") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package test;\n");
      code.append("public class Baz {\n");
      code.append("  public static class Baz1 {\n");
      code.append("    public String value(String a) { return \"Baz1\"; }\n");
      code.append("    public String value(int val) { return \"Baz1\"; }\n");
      code.append("   }\n");
      code.append("  public class Baz2 {\n");
      code.append("    public String value(String a) { return \"Baz2\"; }\n");
      code.append("    public String value(int val) { return \"Baz2\"; }\n");
      code.append("   }\n");
      code.append("}\n");
      return code;
    }
  };

  private static final MockJavaResource FOO = new MockJavaResource("test.Foo") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package test;\n");
      code.append("public class Foo {\n");
      code.append("  public String value(String a, int val) { return \"Foo\"; }\n");
      code.append("}\n");
      return code;
    }
  };

  private static final MockJavaResource FOO_INT = new MockJavaResource(
      "test.FooInt") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package test;\n");
      code.append("public interface FooInt {\n");
      code.append("  String value(String a, int val);\n");
      code.append("}\n");
      return code;
    }
  };

  /**
   * Test method for {@link JavaSourceParser#getClassChain(java.lang.String)}.
   */
  public void testGetClassChain() {
    assertExpected(JavaSourceParser.getClassChain("Foo"), "Foo");
    assertExpected(JavaSourceParser.getClassChain("test.Foo"), "Foo");
    assertExpected(JavaSourceParser.getClassChain("Foo$Bar"), "Foo", "Bar");
    assertExpected(JavaSourceParser.getClassChain("test.Foo$Bar"), "Foo", "Bar");
    assertExpected(JavaSourceParser.getClassChain("test.test2.Foo$Bar$Baz"),
        "Foo", "Bar", "Baz");
  }

  public void testLookup() throws NotFoundException {
    JavaSourceParser parser = new JavaSourceParser();
    addGeneratedUnits(FOO);
    addGeneratedUnits(BAR);
    addGeneratedUnits(BAZ);
    JClassType string = state.getTypeOracle().getType("java.lang.String");
    JClassType foo = state.getTypeOracle().getType("test.Foo");
    parser.addSourceForType(foo, FOO);
    JClassType bar = state.getTypeOracle().getType("test.Bar");
    parser.addSourceForType(bar, BAR);
    JClassType baz = state.getTypeOracle().getType("test.Baz");
    parser.addSourceForType(baz, BAZ);
    JClassType baz1 = state.getTypeOracle().getType("test.Baz.Baz1");
    JClassType baz2 = state.getTypeOracle().getType("test.Baz.Baz2");
    JMethod method = foo.getMethod("value", new JType[]{
        string, JPrimitiveType.INT});
    String[] arguments = parser.getArguments(method);
    assertNotNull(arguments);
    assertEquals(2, arguments.length);
    assertEquals("a", arguments[0]);
    assertEquals("val", arguments[1]);
    method = bar.getMethod("value", new JType[]{string, JPrimitiveType.INT});
    arguments = parser.getArguments(method);
    assertNotNull(arguments);
    assertEquals(2, arguments.length);
    assertEquals("a", arguments[0]);
    assertEquals("val", arguments[1]);
    method = bar.getMethod("value", new JType[]{JPrimitiveType.INT});
    arguments = parser.getArguments(method);
    assertNotNull(arguments);
    assertEquals(1, arguments.length);
    assertEquals("val", arguments[0]);
    method = bar.getMethod("value", new JType[]{string});
    arguments = parser.getArguments(method);
    assertNotNull(arguments);
    assertEquals(1, arguments.length);
    assertEquals("a", arguments[0]);
    method = baz1.getMethod("value", new JType[]{JPrimitiveType.INT});
    arguments = parser.getArguments(method);
    assertNotNull(arguments);
    assertEquals(1, arguments.length);
    assertEquals("val", arguments[0]);
    method = baz1.getMethod("value", new JType[]{string});
    arguments = parser.getArguments(method);
    assertNotNull(arguments);
    assertEquals(1, arguments.length);
    assertEquals("a", arguments[0]);
    method = baz2.getMethod("value", new JType[]{JPrimitiveType.INT});
    arguments = parser.getArguments(method);
    assertNotNull(arguments);
    assertEquals(1, arguments.length);
    assertEquals("val", arguments[0]);
    method = baz2.getMethod("value", new JType[]{string});
    arguments = parser.getArguments(method);
    assertNotNull(arguments);
    assertEquals(1, arguments.length);
    assertEquals("a", arguments[0]);
  }

  public void testParamNames() throws NotFoundException {
    JavaSourceParser parser = new JavaSourceParser();
    addGeneratedUnits(FOO_INT);
    JClassType string = state.getTypeOracle().getType("java.lang.String");
    JClassType fooInt = state.getTypeOracle().getType("test.FooInt");
    parser.addSourceForType(fooInt, FOO_INT);
    JMethod method = fooInt.getMethod("value", new JType[]{
        string, JPrimitiveType.INT});
    String[] arguments = parser.getArguments(method);
    assertNotNull(arguments);
    assertEquals(2, arguments.length);
    assertEquals("a", arguments[0]);
    assertEquals("val", arguments[1]);
  }

  private void assertExpected(List<char[]> actual, String... expected) {
    if (actual.size() != expected.length) {
      fail("Expected " + Arrays.deepToString(expected) + ", got " + actual);
    }
    for (int i = 0; i < expected.length; ++i) {
      assertTrue("index " + i + " should be " + expected[i] + ", got "
          + Arrays.toString(actual.get(i)), Arrays.equals(actual.get(i),
          expected[i].toCharArray()));
    }
  }
}
