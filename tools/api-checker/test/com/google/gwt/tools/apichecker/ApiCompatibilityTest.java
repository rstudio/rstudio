/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.tools.apichecker;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.CompilationUnit;
import com.google.gwt.dev.javac.JdtCompiler;
import com.google.gwt.dev.javac.TypeOracleMediator;
import com.google.gwt.dev.util.log.AbstractTreeLogger;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import junit.framework.TestCase;

import java.util.HashSet;
import java.util.Set;

/**
 * 
 * Test 2 ApiContainers for compatibility
 * 
 * test compatibility output if returnType changes, parameters change, method
 * overload compatibility, exception compatibility; abstract added, final added
 * to both ApiClass, apiMethods.
 * 
 * test white-list support.
 */
public class ApiCompatibilityTest extends TestCase {

  static class StaticCompilationUnit extends CompilationUnit {

    private final char[] source;
    private final String typeName;

    public StaticCompilationUnit(String typeName, char[] source) {
      this.typeName = typeName;
      this.source = source;
    }

    @Override
    public String getDisplayLocation() {
      return "/mock/" + typeName;
    }

    @Override
    public String getSource() {
      return String.valueOf(source);
    }

    @Override
    public String getTypeName() {
      return typeName;
    }

    @Override
    public boolean isGenerated() {
      return false;
    }
  }

  // These cups are slightly different from the cups in ApiContainerTest
  static StaticCompilationUnit cuApiClass = new StaticCompilationUnit(
      "test.apicontainer.ApiClass", getSourceForApiClass());
  static StaticCompilationUnit cuNonApiClass = new StaticCompilationUnit(
      "test.apicontainer.NonApiClass", getSourceForNonApiClass());
  static StaticCompilationUnit cuNonApiPackage = new StaticCompilationUnit(
      "test.nonapipackage.TestClass", getSourceForTestClass());
  static StaticCompilationUnit cuObject = new StaticCompilationUnit(
      "java.lang.Object", getSourceForObject());
  static StaticCompilationUnit cuThrowable = new StaticCompilationUnit(
      "java.lang.Throwable", getSourceForThrowable());

  private static char[] getSourceForApiClass() {
    StringBuffer sb = new StringBuffer();
    sb.append("package test.apicontainer;\n");
    sb.append("public class ApiClass extends NonApiClass {\n");
    sb.append("\tpublic void apiMethod() { };\n");
    sb.append("\tpublic void checkParametersAndReturnTypes(java.lang.Object x) throws java.lang.Throwable { };\n");
    sb.append("};\n");
    return sb.toString().toCharArray();
  }

  private static char[] getSourceForNonApiClass() {
    StringBuffer sb = new StringBuffer();
    sb.append("package test.apicontainer;\n");
    sb.append("class NonApiClass extends java.lang.Object {\n");
    sb.append("\tpublic void methodInNonApiClass(java.lang.Object o) { };\n");
    sb.append("\tpublic void methodInNonApiClass(test.apicontainer.NonApiClass t) { };\n");
    sb.append("\tpublic int fieldInNonApiClass = 3;\n");
    sb.append("\tprotected abstract class ApiClassInNonApiClass {\n");
    sb.append("\tprotected ApiClassInNonApiClass() { }\n");
    sb.append("\t}\n");
    sb.append("}\n");
    return sb.toString().toCharArray();
  }

  private static char[] getSourceForObject() {
    StringBuffer sb = new StringBuffer();
    sb.append("package java.lang;\n");
    sb.append("public class Object {\n");
    sb.append("\tpublic void apiMethod() { }\n");
    sb.append("\tprivate void internalMethod() { }\n");
    sb.append("\tprotected final void protectedMethod() { }\n");
    sb.append("\tpublic final int apiField = 0;\n");
    sb.append("\tprivate int internalField = 0;\n");
    sb.append("\tprotected static int protectedField=2;\n");
    sb.append("}\n");
    return sb.toString().toCharArray();
  }

  private static char[] getSourceForTestClass() {
    StringBuffer sb = new StringBuffer();
    sb.append("package test.nonapipackage;\n");
    sb.append("class TestClass extends java.lang.Object {\n");
    sb.append("\tpublic void method() { }\n");
    sb.append("}\n");
    return sb.toString().toCharArray();
  }

  private static char[] getSourceForThrowable() {
    StringBuffer sb = new StringBuffer();
    sb.append("package java.lang;\n");
    sb.append("public class Throwable extends Object {\n");
    sb.append("}\n");
    return sb.toString().toCharArray();
  }

  ApiContainer api1 = null;
  ApiContainer api2 = null;

  ApiContainer apiSameAs1 = null;

  public TypeOracle getNewTypeOracleWithCompilationUnitsAdded(
      AbstractTreeLogger logger) throws UnableToCompleteException {
    TypeOracleMediator mediator = new TypeOracleMediator();
    Set<CompilationUnit> units = new HashSet<CompilationUnit>();
    units.add(cuObject);
    units.add(cuNonApiClass);
    units.add(cuApiClass);
    units.add(cuNonApiPackage);
    units.add(cuThrowable);
    JdtCompiler.compile(units);
    mediator.refresh(logger, units);
    return mediator.getTypeOracle();
  }

  @Override
  public void setUp() {
    AbstractTreeLogger logger = new PrintWriterTreeLogger();
    logger.setMaxDetail(TreeLogger.ERROR);
    try {
      api1 = new ApiContainer("Api1", logger,
          new ApiContainerTest().getNewTypeOracleWithCompilationUnitsAdded());
      apiSameAs1 = new ApiContainer("Api2", logger,
          new ApiContainerTest().getNewTypeOracleWithCompilationUnitsAdded());
      api2 = new ApiContainer("Api2", logger,
          getNewTypeOracleWithCompilationUnitsAdded(logger));
    } catch (Exception ex) {
      assertEquals("JSNI checks are probably active", "failed");
    }
  }

  public void testBasicStuff() throws NotFoundException {
    HashSet<String> hashSet = new HashSet<String>();
    assertEquals("", ApiCompatibilityChecker.getApiDiff(new ApiDiffGenerator(
        api1, apiSameAs1), hashSet, false));
    ApiDiffGenerator apiDiff = new ApiDiffGenerator(api2, api1);
    boolean removeDuplicates = false;
    String strWithDuplicates = ApiCompatibilityChecker.getApiDiff(apiDiff,
        hashSet, removeDuplicates);
    String strWithoutDuplicates = ApiCompatibilityChecker.getApiDiff(apiDiff,
        hashSet, !removeDuplicates);

    String delimiter = ApiDiffGenerator.DELIMITER;
    // test if missing packages are reported correctly
    String status = "java.newpackage" + delimiter + ApiChange.Status.MISSING;
    assertEquals(1, countPresence(status, strWithDuplicates));
    assertEquals(1, countPresence(status, strWithoutDuplicates));

    // test if missing classes are reported correctly
    assertEquals(1, countPresence(
        "test.apicontainer.NonApiClass.AnotherApiClassInNonApiClass"
            + delimiter + ApiChange.Status.MISSING, strWithoutDuplicates));
    // System.out.println(strWithDuplicates);
    // System.out.println(strWithoutDuplicates);

    // test if modifier changes of a class are reported
    assertEquals(1, countPresence(
        "test.apicontainer.NonApiClass.ApiClassInNonApiClass" + delimiter
            + ApiChange.Status.ABSTRACT_ADDED, strWithoutDuplicates));

    // test if methods are reported as missing due to class becoming abstract
    if (ApiCompatibilityChecker.REMOVE_ABSTRACT_CLASS_FROM_API) {
      assertEquals(1, countPresence(
          "test.apicontainer.NonApiClass.ApiClassInNonApiClass::ApiClassInNonApiClass()"
              + delimiter + ApiChange.Status.MISSING, strWithoutDuplicates));
      assertEquals(1, countPresence(
          "test.apicontainer.NonApiClass.ApiClassInNonApiClass::protectedMethod()"
              + delimiter + ApiChange.Status.MISSING, strWithoutDuplicates));
    }

    // test if modifier changes of fields and methods are reported
    assertEquals(1, countPresence("java.lang.Object::apiField" + delimiter
        + ApiChange.Status.FINAL_ADDED, strWithoutDuplicates));
    assertEquals(1, countPresence("java.lang.Object::protectedMethod()"
        + delimiter + ApiChange.Status.FINAL_ADDED, strWithoutDuplicates));

    // test if duplicates are weeded out.
    if (ApiCompatibilityChecker.REMOVE_ABSTRACT_CLASS_FROM_API) {
      assertEquals(2, countPresence("protectedMethod()" + delimiter
          + ApiChange.Status.FINAL_ADDED, strWithDuplicates));
    } else {
      assertEquals(3, countPresence("protectedMethod()" + delimiter
          + ApiChange.Status.FINAL_ADDED, strWithDuplicates));
    }
    // test returnType error
    String methodSignature = "checkParametersAndReturnTypes(Ltest/apicontainer/ApiClass;)";
    assertEquals(1, countPresence(methodSignature + delimiter
        + ApiChange.Status.RETURN_TYPE_ERROR, strWithoutDuplicates));
    // test method exceptions
    assertEquals(1, countPresence(methodSignature + delimiter
        + ApiChange.Status.EXCEPTIONS_ERROR, strWithoutDuplicates));

    // checking if changes in parameter types were okay
    assertEquals(2, countPresence(methodSignature, strWithoutDuplicates));

    // test method_overloading
    methodSignature = "methodInNonApiClass(Ljava/lang/Object;)";
    assertEquals(1, countPresence(methodSignature + delimiter
        + ApiChange.Status.OVERLOADED, strWithoutDuplicates));
  }

  public void testWhiteList() throws NotFoundException {
    ApiDiffGenerator apiDiff = new ApiDiffGenerator(api2, api1);
    boolean removeDuplicates = false;
    String whiteList = "java.newpackage" + ApiDiffGenerator.DELIMITER
        + ApiChange.Status.MISSING;
    HashSet<String> hashSet = new HashSet<String>();
    hashSet.add(whiteList);
    String strWithoutDuplicates = ApiCompatibilityChecker.getApiDiff(apiDiff,
        hashSet, !removeDuplicates);

    // test if missing packages are reported correctly
    assertEquals(0, countPresence(whiteList, strWithoutDuplicates));
  }

  private int countPresence(String needle, String hay) {
    int count = 0;
    int needleLength = needle.length();
    int index = -needleLength;
    while ((index = hay.indexOf(needle, index + needleLength)) != -1) {
      count++;
    }
    return count;
  }

}

// abstract methods can't be static

class TestAA {
  static int j = 10;

  static void printX() {
    System.err.println("2");
  }

  int i = 5;

  private TestAA() {
  }
}

class TestAB {
  static void test() {
    TestAA.printX();
  }
}
