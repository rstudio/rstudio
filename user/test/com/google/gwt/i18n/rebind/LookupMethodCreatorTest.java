/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.i18n.rebind;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.core.ext.typeinfo.TypeOracleException;
import com.google.gwt.dev.javac.TypeOracleTestingUtils;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.shell.FailErrorLogger;
import com.google.gwt.i18n.rebind.AbstractResource.ResourceList;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.gwt.user.rebind.StringSourceWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link LookupMethodCreator}.
 */
public class LookupMethodCreatorTest {

  private TreeLogger logger = new FailErrorLogger();

  private LookupMethodCreator underTest;

  private SourceWriter sw = new StringSourceWriter();

  private TypeOracle oracle;

  private JMethod method;

  private static final MockJavaResource SINGLE_ENTRY_MESSAGES = new MockJavaResource(
      "foo.SingleEntryMessage") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package foo;\n");
      code.append("public interface SingleEntryMessage extends foo.Lookup {\n");
      code.append(" String singleEntry();\n");
      code.append("}");
      return code;
    }
  };

  private static final MockJavaResource FOUR_ENTRY_MESSAGES = new MockJavaResource(
      "foo.FourEntryMessage") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package foo;\n");
      code.append("public interface FourEntryMessage extends foo.Lookup {\n");
      code.append(" String first();\n");
      code.append(" String second();\n");
      code.append(" String third();\n");
      code.append(" String fourth();\n");
      code.append("}");
      return code;
    }
  };

  private static final MockJavaResource LOOKUP = new MockJavaResource("foo.Lookup") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package foo;\n");
      code.append("public interface Lookup {\n");
      code.append(" String getString(String arg0);\n");
      code.append("}");
      return code;
    }
  };

  private void initLookupMethodCreator(MockJavaResource resource, int partitionsSize) {
    JClassType clazz = oracle.findType(resource.getTypeName());
    ConstantsWithLookupImplCreator mockCreator;
    try {
      mockCreator = new ConstantsWithLookupImplCreator(logger, sw, clazz, mock(ResourceList.class),
          oracle);

      JType stringType = oracle.findType("java.lang.String");
      method = oracle.findType(LOOKUP.getTypeName()).findMethod("getString", new JType[] {
          stringType});
      underTest = new LookupMethodCreator(mockCreator, stringType, partitionsSize);
    } catch (UnableToCompleteException e) {
      fail(e.getMessage());
    }
  }

  @Before
  public void setupUp() throws TypeOracleException, UnableToCompleteException {
    oracle = TypeOracleTestingUtils.buildStandardTypeOracleWith(logger, SINGLE_ENTRY_MESSAGES,
        FOUR_ENTRY_MESSAGES, LOOKUP);
    initLookupMethodCreator(SINGLE_ENTRY_MESSAGES, 3);
  }

  /**
   * Test the "golden output".<br>
   * In general, those tests are more trouble than they are worth but this will be a simple test for
   * verify the general structure.
   * 
   * old version looks: <pre>
   * java.lang.String target = (java.lang.String) cache.get(arg0);
   * if (target != null) {
   *   return target;
   * }
   * if(arg0.equals("singleEntry")) {
   *   return singleEntry();
   * }
   * throw new java.util.MissingResourceException("Cannot find constant '" +arg0 + "'; expecting a method name", "foo.TestMessage", arg0);
   * </pre>
   *
   * with first partition lookup it generates:
   *
   * <pre>
   * java.lang.String target = (java.lang.String) cache.get(arg0);
   * if (target != null) {
   *   return target;
   * }
   * java.lang.String tmp;
   * tmp = getString0(arg0);
   * if (tmp != null) {
   *   return tmp;
   * }
   * throw new java.util.MissingResourceException("Cannot find constant '" +arg0 + "'; expecting a method name", "foo.TestMessage", arg0);
   * }
   *
   * public java.lang.String getString0(java.lang.String arg0) {
   *   if(arg0.equals("singleEntry")) {
   *     return singleEntry();
   *   }
   *   return null;
   *
   * </pre>
   */
  @Test
  public void testCreateMethodForJMethodForSingleEntry() {
    underTest.createMethodFor(method);

    SourceWriter expected = new StringSourceWriter();
    expected.println("java.lang.String target = (java.lang.String) cache.get(arg0);");
    expected.println("if (target != null) {");
    expected.indent();
    expected.println("return target;");
    expected.outdent();
    expected.println("}");

    expected.println("java.lang.String tmp;");
    expected.println("tmp = getString0(arg0);");
    expected.println("if (tmp != null) {");
    expected.indent();
    expected.println("return tmp;");
    expected.outdent();
    expected.println("}");

    expected.println("throw new java.util.MissingResourceException("
        + "\"Cannot find constant '\" +arg0 + \"'; expecting a method name\", \"foo.SingleEntryMessage\", arg0);");

    // check partition method
    assertTrue("No partition lookup created.", sw.toString().contains(
        "java.lang.String getString0(java.lang.String arg0) {"));

    expected.println("}");
    expected.println();
    expected.println("private java.lang.String getString0(java.lang.String arg0) {");
    expected.indent();
    expected.println("if(arg0.equals(\"singleEntry\")) {");
    expected.indent();
    expected.println("return singleEntry();");
    expected.outdent();
    expected.println("}");
    expected.println("return null;");
    expected.outdent();

    assertEquals("Wrong source Lookup created.", expected.toString(), sw.toString());
  }

  @Test
  public void testCreateMethodForJMethodForMultiMessageEntryCreateTwoPartitions() {
    initLookupMethodCreator(FOUR_ENTRY_MESSAGES, 3);
    underTest.createMethodFor(method);

    String actual = sw.toString();

    assertTrue("Missing partition lookup method (getString0).", actual.contains(
        "java.lang.String getString0(java.lang.String arg0) {"));

    assertTrue("Missing partition lookup method (getString1).", actual.contains(
        "java.lang.String getString1(java.lang.String arg0) {"));
  }

  @Test
  public void testPrintFound() {
    underTest.printFound("callTest");

    String returnStatement = sw.toString();
    assertEquals("return callTest();\n", returnStatement);
  }

  @Test
  public void testGetReturnTypeName() {
    String returnType = underTest.getReturnTypeName();
    assertEquals("java.lang.String", returnType);
  }

  @Test
  public void testGetReturnTypeNameForPrimitveTypes() {
    for (JPrimitiveType primitiveType : JPrimitiveType.values()) {
      LookupMethodCreator primitiveMethodCreator = new LookupMethodCreator(null, primitiveType, 2);
      String returnType = primitiveMethodCreator.getReturnTypeName();
      String expectedType = primitiveType.getQualifiedBoxedSourceName().substring("java.lang."
          .length());
      assertEquals("Wrong Return Type for primitve type", expectedType, returnType);
    }
  }

}
