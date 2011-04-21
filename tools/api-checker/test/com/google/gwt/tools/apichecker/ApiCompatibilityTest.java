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
import com.google.gwt.dev.javac.testing.impl.StaticJavaResource;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.log.AbstractTreeLogger;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

/**
 * 
 * Test 2 ApiContainers for compatibility
 * 
 * test compatibility output if returnType changes, parameters change, method
 * overload compatibility, exception compatibility; abstract added, final added
 * to both ApiClass, apiMethods.
 * 
 * test white-list support.
 * 
 * TODO(amitmanjhi): (1) Re-factor this code as much as possible into smaller
 * separate components similar to the ApiCompatibilityUnit class. (2) Use
 * MockApiElement instead of String comparisons.
 */
public class ApiCompatibilityTest extends TestCase {

  // These cups are slightly different from the cups in ApiContainerTest
  private static final boolean DEBUG = false;

  private static StaticJavaResource[] getScuArray() {
    return new StaticJavaResource[] {
        new StaticJavaResource("test.apicontainer.ApiClass", getSourceForApiClass()),
        new StaticJavaResource("test.apicontainer.NonApiClass", getSourceForNonApiClass()),
        new StaticJavaResource("test.nonapipackage.TestClass", getSourceForTestClass()),
        new StaticJavaResource("java.lang.Object", getSourceForObject()),
        new StaticJavaResource("java.lang.Throwable", getSourceForThrowable()),
        new StaticJavaResource("test.apicontainer.OneMoreApiClass", getSourceForOneMoreApiClass()),
        new StaticJavaResource("java.lang.RuntimeException", getSourceForRuntimeException()),};
  }

  private static String getSourceForApiClass() {
    StringBuffer sb = new StringBuffer();
    sb.append("package test.apicontainer;\n");
    sb.append("public class ApiClass extends NonApiClass {\n");
    sb.append("\tpublic void apiMethod() { };\n");
    sb.append("\tpublic void checkParametersAndReturnTypes(java.lang.Object x) throws java.lang.Throwable { };\n");
    sb.append("\tpublic final void checkParametersAndReturnTypesFinalVersion(java.lang.Object x) throws java.lang.Throwable { };\n");
    sb.append("};\n");
    return sb.toString();
  }

  private static String getSourceForNonApiClass() {
    StringBuffer sb = new StringBuffer();
    sb.append("package test.apicontainer;\n");
    sb.append("class NonApiClass extends java.lang.Object {\n");
    sb.append("\tpublic void methodInNonApiClass(ApiClassInNonApiClass o) { };\n");
    sb.append("\tpublic void methodInNonApiClass(NonApiClass t) { };\n");
    sb.append("\tpublic int fieldInNonApiClass = 3;\n");
    sb.append("\tprotected abstract class ApiClassInNonApiClass {\n");
    sb.append("\tprotected ApiClassInNonApiClass() { }\n");
    sb.append("\t}\n");
    sb.append("}\n");
    return sb.toString();
  }

  private static String getSourceForObject() {
    StringBuffer sb = new StringBuffer();
    sb.append("package java.lang;\n");
    sb.append("public class Object {\n");
    sb.append("\tpublic void apiMethod() { }\n");
    sb.append("\tprotected void checkOverloadedAndOverridableDetection(java.lang.Object b) { }\n");
    sb.append("\tprotected final void checkOverloadedMethodAccounted(java.lang.Object b) { }\n");
    sb.append("\tprivate void internalMethod() { }\n");
    sb.append("\tprotected final void protectedMethod() { }\n");
    sb.append("\tpublic final int apiField = 0;\n");
    sb.append("\tprivate int internalField = 0;\n");
    sb.append("\tprotected static int protectedField=2;\n");
    sb.append("}\n");
    return sb.toString();
  }

  private static String getSourceForOneMoreApiClass() {
    StringBuffer sb = new StringBuffer();
    sb.append("package test.apicontainer;\n");
    sb.append("public class OneMoreApiClass extends java.lang.Object {\n");
    sb.append("\tprotected void checkOverloadedAndOverridableDetection(test.apicontainer.OneMoreApiClass b) { }\n");
    sb.append("\tprotected final void checkOverloadedMethodAccounted(test.apicontainer.ApiClass b) throws java.lang.Throwable { }\n");
    sb.append("\tpublic void testUncheckedExceptions() throws RuntimeException { }\n");
    sb.append("};\n");
    return sb.toString();
  }

  private static String getSourceForRuntimeException() {
    StringBuffer sb = new StringBuffer();
    sb.append("package java.lang;\n");
    sb.append("public class RuntimeException extends Throwable {\n");
    sb.append("}\n");
    return sb.toString();
  }

  private static String getSourceForTestClass() {
    StringBuffer sb = new StringBuffer();
    sb.append("package test.nonapipackage;\n");
    sb.append("class TestClass extends java.lang.Object {\n");
    sb.append("\tpublic void method() { }\n");
    sb.append("}\n");
    return sb.toString();
  }

  private static String getSourceForThrowable() {
    StringBuffer sb = new StringBuffer();
    sb.append("package java.lang;\n");
    sb.append("public class Throwable extends Object {\n");
    sb.append("}\n");
    return sb.toString();
  }

  ApiContainer api1 = null;
  ApiContainer api2 = null;
  ApiContainer apiSameAs1 = null;

  @Override
  public void setUp() throws UnableToCompleteException {
    AbstractTreeLogger logger = new PrintWriterTreeLogger();
    logger.setMaxDetail(TreeLogger.ERROR);

    api1 =
        new ApiContainer("Api1", new HashSet<Resource>(Arrays
            .asList(ApiContainerTest.getScuArray())), new HashSet<String>(), logger);
    apiSameAs1 =
        new ApiContainer("ApiSameAs1", new HashSet<Resource>(Arrays.asList(ApiContainerTest
            .getScuArray())), new HashSet<String>(), logger);
    api2 =
        new ApiContainer("Api2", new HashSet<Resource>(Arrays.asList(getScuArray())),
            new HashSet<String>(), logger);
  }

  // setup is called before every test*. To avoid the overhead of setUp() each
  // time, test everything together.
  public void testEverything() throws NotFoundException {
    checkBasicStuff();
    checkWhiteList();
  }

  private void checkBasicStuff() throws NotFoundException {
    HashSet<String> hashSet = new HashSet<String>();
    assertEquals(0, ApiCompatibilityChecker.getApiDiff(api1, apiSameAs1, hashSet).size());
    ApiDiffGenerator apiDiff = new ApiDiffGenerator(api2, api1);
    String strWithDuplicates =
        getStringRepresentation(ApiCompatibilityChecker.getApiDiff(apiDiff, hashSet,
            !ApiCompatibilityChecker.FILTER_DUPLICATES));
    if (DEBUG) {
      System.out.println("computing apiDiff, now with duplicates");
      System.out.println(strWithDuplicates);
    }
    String strWithoutDuplicates =
        getStringRepresentation(ApiCompatibilityChecker.getApiDiff(apiDiff, hashSet,
            ApiCompatibilityChecker.FILTER_DUPLICATES));
    if (DEBUG) {
      System.out.println("computing apiDiff, now without duplicates");
      System.out.println(strWithoutDuplicates);
    }

    String delimiter = ApiDiffGenerator.DELIMITER;
    // test if missing packages are reported correctly
    String statusString = "java.newpackage" + delimiter + ApiChange.Status.MISSING;
    assertEquals(1, countPresence(statusString, strWithDuplicates));
    assertEquals(1, countPresence(statusString, strWithoutDuplicates));

    // test if missing classes are reported correctly
    assertEquals(1, countPresence("test.apicontainer.NonApiClass.AnotherApiClassInNonApiClass"
        + delimiter + ApiChange.Status.MISSING, strWithoutDuplicates));

    // test if modifier changes of a class are reported
    assertEquals(1, countPresence("test.apicontainer.NonApiClass.ApiClassInNonApiClass" + delimiter
        + ApiChange.Status.ABSTRACT_ADDED, strWithoutDuplicates));

    // test if methods are still reported even if class becomes abstract (as
    // long as it is sub-classable)
    assertEquals(0, countPresence(
        "test.apicontainer.NonApiClass.ApiClassInNonApiClass::ApiClassInNonApiClass()" + delimiter
            + ApiChange.Status.MISSING, strWithoutDuplicates));
    assertEquals(0, countPresence(
        "test.apicontainer.NonApiClass.ApiClassInNonApiClass::protectedMethod()" + delimiter
            + ApiChange.Status.MISSING, strWithoutDuplicates));

    // test if modifier changes of fields and methods are reported
    assertEquals(1, countPresence("java.lang.Object::apiField" + delimiter
        + ApiChange.Status.FINAL_ADDED, strWithoutDuplicates));
    assertEquals(1, countPresence("java.lang.Object::protectedMethod()" + delimiter
        + ApiChange.Status.FINAL_ADDED, strWithoutDuplicates));

    // test if duplicates are weeded out from intersecting methods
    assertEquals(4, countPresence("protectedMethod()" + delimiter + ApiChange.Status.FINAL_ADDED,
        strWithDuplicates));
    assertEquals(1, countPresence("protectedMethod()" + delimiter + ApiChange.Status.FINAL_ADDED,
        strWithoutDuplicates));

    // test if duplicates are weeded out from missing fields
    assertEquals(4, countPresence("apiFieldWillBeMissing" + delimiter + ApiChange.Status.MISSING,
        strWithDuplicates));
    assertEquals(1, countPresence("apiFieldWillBeMissing" + delimiter + ApiChange.Status.MISSING,
        strWithoutDuplicates));

    // test error in non-final version
    String nonFinalMethodSignature = "checkParametersAndReturnTypes(Ltest/apicontainer/ApiClass;)";
    for (ApiChange.Status status : new ApiChange.Status[] {
        ApiChange.Status.OVERRIDABLE_METHOD_ARGUMENT_TYPE_CHANGE,
        ApiChange.Status.OVERRIDABLE_METHOD_EXCEPTION_TYPE_CHANGE,
        ApiChange.Status.OVERRIDABLE_METHOD_RETURN_TYPE_CHANGE}) {
      assertEquals(1, countPresence(nonFinalMethodSignature + delimiter + status,
          strWithoutDuplicates));
    }
    // test return type and exception type error in final version
    String finalMethodSignature =
        "checkParametersAndReturnTypesFinalVersion(Ltest/apicontainer/ApiClass;)";
    assertEquals(1, countPresence(finalMethodSignature + delimiter
        + ApiChange.Status.RETURN_TYPE_ERROR, strWithoutDuplicates));
    assertEquals(1, countPresence(finalMethodSignature + delimiter
        + ApiChange.Status.EXCEPTION_TYPE_ERROR, strWithoutDuplicates));

    // checking if changes in parameter types were okay
    assertEquals(2, countPresence(finalMethodSignature, strWithoutDuplicates));

    // test method_overloading
    finalMethodSignature = "methodInNonApiClass(Ltest/apicontainer/NonApiClass;)";
    assertEquals(1, countPresence(finalMethodSignature + delimiter
        + ApiChange.Status.OVERLOADED_METHOD_CALL, strWithoutDuplicates));

    // test unchecked exceptions
    assertEquals(0, countPresence("testUncheckedExceptions", strWithoutDuplicates));

    // test overloaded and overridable detection
    String methodSignature =
        "test.apicontainer.OneMoreApiClass::checkOverloadedAndOverridableDetection(Ljava/lang/Object;)";
    for (ApiChange.Status status : new ApiChange.Status[] {
        ApiChange.Status.OVERRIDABLE_METHOD_ARGUMENT_TYPE_CHANGE,
        ApiChange.Status.OVERRIDABLE_METHOD_EXCEPTION_TYPE_CHANGE,
        ApiChange.Status.OVERRIDABLE_METHOD_RETURN_TYPE_CHANGE}) {
      assertEquals(0, countPresence(methodSignature + delimiter + status, strWithoutDuplicates));
    }

    // the method should be satisfied by the method in the super-class
    methodSignature =
        "test.apicontainer.OneMoreApiClass::checkOverloadedMethodAccounted(Ltest/apicontainer/OneMoreApiClass;)";
    assertEquals(0, countPresence(methodSignature + delimiter + ApiChange.Status.MISSING,
        strWithoutDuplicates));

    // the method should throw unchecked exceptions error
    methodSignature =
        "test.apicontainer.OneMoreApiClass::checkOverloadedMethodAccounted(Ljava/lang/Object;)";
    assertEquals(1, countPresence(methodSignature + delimiter
        + ApiChange.Status.EXCEPTION_TYPE_ERROR, strWithoutDuplicates));
  }

  private void checkWhiteList() throws NotFoundException {
    ApiDiffGenerator apiDiff = new ApiDiffGenerator(api2, api1);
    boolean removeDuplicates = false;
    String whiteList = "java.newpackage" + ApiDiffGenerator.DELIMITER + ApiChange.Status.MISSING;
    HashSet<String> hashSet = new HashSet<String>();
    hashSet.add(whiteList);
    String strWithoutDuplicates =
        getStringRepresentation(ApiCompatibilityChecker.getApiDiff(apiDiff, hashSet,
            !removeDuplicates));

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

  private String getStringRepresentation(Collection<ApiChange> collection) {
    StringBuffer sb = new StringBuffer();
    for (ApiChange apiChange : collection) {
      sb.append(apiChange);
      sb.append("\n");
    }
    return sb.toString();
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
