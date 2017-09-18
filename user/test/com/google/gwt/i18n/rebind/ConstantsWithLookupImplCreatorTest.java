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

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests for {@link ConstantsWithLookupImplCreator}.
 */
public class ConstantsWithLookupImplCreatorTest extends TestCase {

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

  private GwtLocale locale;

  private ConstantsWithLookupImplCreator constantsWithLookupImplCreator;

  private SourceWriter sw = new StringSourceWriter();

  private TypeOracle oracle;

  private JMethod stringMethod;

  private JMethod intMethod;

  @Override
  public void setUp() throws TypeOracleException, UnableToCompleteException {
    GwtLocaleFactory factory = new GwtLocaleFactoryImpl();
    locale = factory.fromString("en");
    oracle = TypeOracleTestingUtils.buildStandardTypeOracleWith(logger, SINGLE_ENTRY_MESSAGES,
        FOUR_ENTRY_MESSAGES, LOOKUP, PARTITION_SIZE_ENTRY_MESSAGES);
    initLookupMethodCreator(SINGLE_ENTRY_MESSAGES);
  }

  public void testClassEpilogCallCreatesNeededPartitonLookups() {
    List<JMethod> missingPartition0 = Arrays.asList(stringMethod, intMethod);
    List<List<JMethod>> methodToCreatePartitionLookups = new ArrayList<>();
    methodToCreatePartitionLookups.add(missingPartition0);
    constantsWithLookupImplCreator.addNeededPartitionLookups(stringMethod,
        methodToCreatePartitionLookups);

    constantsWithLookupImplCreator.classEpilog();

    String actual = sw.toString().trim();

    assertTrue("Missing partition lookup method (getStringFromPartition0).", actual.contains(
        "java.lang.String getStringFromPartition0(java.lang.String arg0) {"));
  }

  public void testCodeForIntIsGeneratedSameAsWithoutPartitions() throws TypeOracleException,
      UnableToCompleteException {
    initLookupMethodCreator(SINGLE_ENTRY_MESSAGES);
    constantsWithLookupImplCreator.emitMethodBody(logger, intMethod, locale);

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

  public void testCodeIsGeneratedSameAsWithoutPartitions() throws TypeOracleException,
      UnableToCompleteException {
    initLookupMethodCreator(PARTITION_SIZE_ENTRY_MESSAGES);
    constantsWithLookupImplCreator.emitMethodBody(logger, stringMethod, locale);

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
    expected.print("String answer = lookupMethod1();");
    expected.print("\n");
    expected.print("cache.put(\"lookupMethod1\",answer);");
    expected.print("\n");
    expected.println("return answer;");
    expected.outdent();
    expected.println("}");

    expected.println("if (arg0.equals(\"lookupMethod2\")) {");
    expected.indent();
    expected.print("String answer = lookupMethod2();");
    expected.print("\n");
    expected.print("cache.put(\"lookupMethod2\",answer);");
    expected.print("\n");
    expected.println("return answer;");
    expected.outdent();
    expected.println("}");

    expected.println("if (arg0.equals(\"lookupMethod3\")) {");
    expected.indent();
    expected.print("String answer = lookupMethod3();");
    expected.print("\n");
    expected.print("cache.put(\"lookupMethod3\",answer);");
    expected.print("\n");
    expected.println("return answer;");
    expected.outdent();
    expected.println("}");

    expected.println("throw new java.util.MissingResourceException("
        + "\"Cannot find constant '\" +arg0 + \"'; expecting a method name\", \"foo.PartitionSizeEntryMessage\", arg0);");
    expected.outdent();

    String actual = sw.toString();
    assertEquals("Wrong source Lookup created.", expected.toString(), actual);
  }

  public void testCreateMethodForJMethodForSingleEntry() throws UnableToCompleteException {
    constantsWithLookupImplCreator.emitMethodBody(logger, stringMethod, locale);

    assertFalse("No partition lookup should created.", sw.toString().contains(
        "java.lang.String getString0(java.lang.String arg0) {"));
  }

  public void testCreateMethodForJMethodForSingleIntEntry() throws UnableToCompleteException {
    constantsWithLookupImplCreator.emitMethodBody(logger, intMethod, locale);

    String createdSource = sw.toString();
    assertTrue("Lookup for intMethod not created.", createdSource.contains(
        "int answer = intEntry();"));
  }

  public void testCreateMethodForPartitionSizeEntriesNoPartitionNeeded() throws TypeOracleException,
      UnableToCompleteException {
    initLookupMethodCreator(PARTITION_SIZE_ENTRY_MESSAGES);
    constantsWithLookupImplCreator.emitMethodBody(logger, stringMethod, locale);

    String actual = sw.toString();

    assertFalse("Missing partition lookup method (getStringFromPartitionN).", actual.contains(
        "getStringFromPartition"));
  }

  public void testCreatePartitionMethodName() {
    String partitionMethodName = constantsWithLookupImplCreator.createPartitionMethodName(
        stringMethod, 1);
    String expectedPartitionMethodName = "getStringFromPartition1";
    assertEquals(expectedPartitionMethodName, partitionMethodName);
  }

  public void testEmitClassWithMultiMessageEntryCreateOneAdditionalPartition()
      throws TypeOracleException, UnableToCompleteException {
    initLookupMethodCreator(FOUR_ENTRY_MESSAGES);
    constantsWithLookupImplCreator.emitClass(logger, locale);

    String actual = sw.toString().trim();

    assertTrue("Missing partition lookup method (getStringFromPartition0).", actual.contains(
        "java.lang.String getStringFromPartition0(java.lang.String arg0) {"));
  }

  public void testEmitMethodBodyForJMethodForMultiMessageEntryNeedOneAdditionalPartition()
      throws TypeOracleException, UnableToCompleteException {
    initLookupMethodCreator(FOUR_ENTRY_MESSAGES);
    constantsWithLookupImplCreator.emitMethodBody(logger, stringMethod, locale);

    String actual = sw.toString().trim();

    assertTrue("Method should end with call partition lookup method.", actual.endsWith(
        "return getStringFromPartition0(arg0);"));
    assertFalse("Partition method should not be created", actual.contains(
        "java.lang.String getStringFromPartition0(java.lang.String arg0) {"));

    assertEquals(1, constantsWithLookupImplCreator.getNeededPartitionLookups().size());
  }

  public void testFindMethodsToCreate() throws TypeOracleException {
    initLookupMethodCreator(SINGLE_ENTRY_MESSAGES);

    JType intType = oracle.parse(int.class.getName());
    List<JMethod> intMethods = constantsWithLookupImplCreator.findAllMethodsToCreate(intMethod,
        intType);

    JMethod expectedIntMethod = oracle.findType(SINGLE_ENTRY_MESSAGES.getTypeName()).findMethod(
        "intEntry", new JType[0]);

    assertEquals(1, intMethods.size());
    assertEquals(expectedIntMethod, intMethods.get(0));
  }

  public void testFindMethodsToCreateWithDifferentTargetMethod() throws TypeOracleException {
    initLookupMethodCreator(SINGLE_ENTRY_MESSAGES);

    JType intType = oracle.parse(int.class.getName());
    List<JMethod> foundMethods = constantsWithLookupImplCreator.findAllMethodsToCreate(stringMethod,
        intType);

    JMethod expectedIntMethod = oracle.findType(SINGLE_ENTRY_MESSAGES.getTypeName()).findMethod(
        "intEntry", new JType[0]);

    assertEquals(2, foundMethods.size());
    assertTrue(foundMethods.contains(intMethod));
    assertTrue(foundMethods.contains(expectedIntMethod));
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

  private void initLookupMethodCreator(MockJavaResource resource) throws TypeOracleException {
    initLookupMethodCreator(resource, TEST_PARTITION_SIZE);
  }

  private void initLookupMethodCreator(MockJavaResource resource, int partitionsSize)
      throws TypeOracleException {
    JClassType clazz = oracle.findType(resource.getTypeName());
    try {
      ResourceList resourceList = mock(ResourceList.class);
      when(resourceList.getRequiredStringExt(anyString(), anyString())).thenReturn(
          "Required value");
      constantsWithLookupImplCreator = new ConstantsWithLookupImplCreator(logger, sw, clazz,
          resourceList, oracle, partitionsSize);

      JType stringType = oracle.parse(String.class.getName());
      stringMethod = oracle.findType(LOOKUP.getTypeName()).findMethod("getString", new JType[] {
          stringType});
      intMethod = oracle.findType(LOOKUP.getTypeName()).findMethod("getInt", new JType[] {
          stringType});
    } catch (UnableToCompleteException e) {
      fail(e.getMessage());
    }
  }

}
