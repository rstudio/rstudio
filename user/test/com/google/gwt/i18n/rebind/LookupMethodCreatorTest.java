/*
 * Copyright 2017 Google Inc.
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
import com.google.gwt.i18n.server.GwtLocaleFactoryImpl;
import com.google.gwt.i18n.shared.GwtLocale;
import com.google.gwt.i18n.shared.GwtLocaleFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.gwt.user.rebind.StringSourceWriter;

import junit.framework.TestCase;

import static org.mockito.Mockito.mock;

import java.util.List;

/**
 * Tests for {@link LookupMethodCreator}.
 */
public class LookupMethodCreatorTest extends TestCase {

  private static final int TEST_PARTITION_SIZE = 3;

  private static final MockJavaResource SINGLE_ENTRY_MESSAGES = new MockJavaResource(
      "foo.SingleEntryMessage") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package foo;\n");
      code.append("public interface SingleEntryMessage extends foo.Lookup {\n");
      code.append(" String stringEntry();\n");
      code.append(" int intEntry();\n");
      code.append("}");
      return code;
    }
  };
  private static final MockJavaResource PARTITION_SIZE_ENTRY_MESSAGES = new MockJavaResource(
      "foo.PartitionSizeEntryMessage") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package foo;\n");
      code.append("public interface PartitionSizeEntryMessage extends foo.Lookup {\n");
      for (int i = 1; i <= TEST_PARTITION_SIZE; i++) {
        code.append(" String lookupMethod");
        code.append(i);
        code.append("();\n");
      }
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
      code.append(" int getInt(String arg0);\n");
      code.append("}");
      return code;
    }
  };

  private TreeLogger logger = new FailErrorLogger();

  private LookupMethodCreator stringCreator;
  private LookupMethodCreator intCreator;

  private SourceWriter sw = new StringSourceWriter();

  private TypeOracle oracle;

  private JMethod stringMethod;

  private JMethod intMethod;

  private GwtLocale locale;

  @Override
  public void setUp() throws TypeOracleException, UnableToCompleteException {
    GwtLocaleFactory factory = new GwtLocaleFactoryImpl();
    locale = factory.fromString("en");
    oracle = TypeOracleTestingUtils.buildStandardTypeOracleWith(logger, SINGLE_ENTRY_MESSAGES,
        LOOKUP, PARTITION_SIZE_ENTRY_MESSAGES);
    initLookupMethodCreator(SINGLE_ENTRY_MESSAGES);
  }

  public void testCodeForIntIsGeneratedSameAsWithoutPartitions() throws TypeOracleException {
    initLookupMethodCreator(SINGLE_ENTRY_MESSAGES);
    intCreator.createMethodFor(logger, intMethod, intMethod.getName(), null, locale);

    // Same generated code as in version 2.8.1
    SourceWriter expected = new StringSourceWriter();
    expected.println("Integer target = (Integer) cache.get(arg0);");
    expected.println("if (target != null) {");
    expected.indent();
    expected.println("return target.intValue();");
    expected.outdent();
    expected.println("}");

    expected.println("if (arg0.equals(\"intEntry\")) {");
    expected.indent();
    expected.println("int answer = intEntry();");
    // Needed because the return template use '\n'
    expected.outdent();
    expected.println("cache.put(\"intEntry\",new Integer(answer));");
    expected.println("return answer;");
    expected.outdent();
    expected.println("}");

    expected.println("throw new java.util.MissingResourceException("
        + "\"Cannot find constant '\" +arg0 + \"'; expecting a method name\", \"foo.SingleEntryMessage\", arg0);");
    expected.outdent();

    String actual = sw.toString();
    assertEquals("Wrong source Lookup created.", expected.toString(), actual);
  }

  public void testCodeIsGeneratedSameAsWithoutPartitions() throws TypeOracleException {
    initLookupMethodCreator(PARTITION_SIZE_ENTRY_MESSAGES);
    stringCreator.createMethodFor(logger, stringMethod, stringMethod.getName(), null, locale);

    // Same generated code as in version 2.8.1
    SourceWriter expected = new StringSourceWriter();
    expected.println("java.lang.String target = (java.lang.String) cache.get(arg0);");
    expected.println("if (target != null) {");
    expected.indent();
    expected.println("return target;");
    expected.outdent();
    expected.println("}");

    expected.println("if (arg0.equals(\"lookupMethod1\")) {");
    expected.indent();
    expected.println("return lookupMethod1();");
    expected.outdent();
    expected.println("}");

    expected.println("if (arg0.equals(\"lookupMethod2\")) {");
    expected.indent();
    expected.println("return lookupMethod2();");
    expected.outdent();
    expected.println("}");

    expected.println("if (arg0.equals(\"lookupMethod3\")) {");
    expected.indent();
    expected.println("return lookupMethod3();");
    expected.outdent();
    expected.println("}");

    expected.println("throw new java.util.MissingResourceException("
        + "\"Cannot find constant '\" +arg0 + \"'; expecting a method name\", \"foo.PartitionSizeEntryMessage\", arg0);");
    expected.outdent();

    String actual = sw.toString();
    assertEquals("Wrong source Lookup created.", expected.toString(), actual);
  }

  public void testCreateMethodForJMethodForSingleEntry() throws TypeOracleException {
    JType stringType = oracle.parse(String.class.getName());
    List<JMethod> methodsToCreate = stringCreator.getConstantsWithLookupCreator()
        .findAllMethodsToCreate(stringMethod, stringType);
    stringCreator.createMethodFor(stringMethod, methodsToCreate, null);

    assertFalse("No partition lookup should created.", sw.toString().contains(
        "java.lang.String getString0(java.lang.String arg0) {"));
  }

  public void testCreateMethodForJMethodForSingleIntEntry() throws TypeOracleException {
    JType intType = oracle.parse(int.class.getName());
    List<JMethod> methodsToCreate = intCreator.getConstantsWithLookupCreator()
        .findAllMethodsToCreate(intMethod, intType);
    intCreator.createMethodFor(intMethod, methodsToCreate, null);

    String createdSource = sw.toString();
    assertTrue("Lookup for intMethod not created.", createdSource.contains(
        "int answer = intEntry();"));
  }

  public void testGetReturnTypeName() {
    String returnType = stringCreator.getReturnTypeName();
    assertEquals("java.lang.String", returnType);
  }

  public void testGetReturnTypeNameForPrimitveTypes() {
    for (JPrimitiveType primitiveType : JPrimitiveType.values()) {
      LookupMethodCreator primitiveMethodCreator = new LookupMethodCreator(null, primitiveType);
      String returnType = primitiveMethodCreator.getReturnTypeName();
      String expectedType = primitiveType.getQualifiedBoxedSourceName().substring("java.lang."
          .length());
      assertEquals("Wrong Return Type for primitve type", expectedType, returnType);
    }
  }

  public void testPrintFound() {
    stringCreator.printFound("callTest");

    String returnStatement = sw.toString();
    assertEquals("return callTest();\n", returnStatement);
  }

  private void initLookupMethodCreator(MockJavaResource resource) throws TypeOracleException {
    initLookupMethodCreator(resource, TEST_PARTITION_SIZE);
  }

  private void initLookupMethodCreator(MockJavaResource resource, int partitionsSize)
      throws TypeOracleException {
    JClassType clazz = oracle.findType(resource.getTypeName());
    ConstantsWithLookupImplCreator mockCreator;
    try {
      mockCreator = new ConstantsWithLookupImplCreator(logger, sw, clazz, mock(ResourceList.class),
          oracle, partitionsSize);

      JType stringType = oracle.parse(String.class.getName());
      stringMethod = oracle.findType(LOOKUP.getTypeName()).findMethod("getString", new JType[] {
          stringType});
      JType intType = oracle.parse(int.class.getName());
      intMethod = oracle.findType(LOOKUP.getTypeName()).findMethod("getInt", new JType[] {
          stringType});

      stringCreator = new LookupMethodCreator(mockCreator, stringType);
      intCreator = new LookupMethodCreator(mockCreator, intType) {
        @Override
        public void printReturnTarget() {
          println("return target.intValue();");
        }

        @Override
        public String returnTemplate() {
          return "int answer = {0}();\ncache.put(\"{0}\",new Integer(answer));\nreturn answer;";
        }
      };
    } catch (UnableToCompleteException e) {
      fail(e.getMessage());
    }
  }

}
