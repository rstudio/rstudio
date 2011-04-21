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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base class for all the ApiCompatibility Testing. Encapsulates two api
 * containers and a test method.
 * 
 */
public class ApiCompatibilityUnitTest extends TestCase {

  /**
   * Mock class to test if the correct ApiChange(s) are being returned.
   * 
   */
  static class MockApiElement implements ApiElement {

    final String signature;

    public MockApiElement(String signature) {
      this.signature = signature;
    }

    public String getRelativeSignature() {
      return signature;
    }
  }

  private static class FinalKeywordRefactoring {
    private static String getFirstApiSourceForObject() {
      StringBuffer sb = new StringBuffer();
      sb.append("package java.lang;\n");
      sb.append("public final class Object {\n");
      sb.append("\tpublic Object foo;\n");
      sb.append("\tpublic void bar() {}\n");
      sb.append("}\n");
      return sb.toString();
    }

    private static String getSecondApiSourceForObject() {
      StringBuffer sb = new StringBuffer();
      sb.append("package java.lang;\n");
      sb.append("public class Object {\n");
      sb.append("\tpublic final Object foo;\n");
      sb.append("\tpublic final void bar() {}\n");
      sb.append("}\n");
      return sb.toString();
    }

    void testBothWays() throws NotFoundException, UnableToCompleteException {
      Map<String, String> firstApi = new HashMap<String, String>();
      firstApi.put("java.lang.Object", getFirstApiSourceForObject());
      Map<String, String> secondApi = new HashMap<String, String>();
      secondApi.put("java.lang.Object", getSecondApiSourceForObject());

      // firstApi is the reference Api
      Collection<ApiChange> apiChanges = getApiChanges(firstApi, secondApi);
      assertEquals(Arrays.asList(new ApiChange[] {new ApiChange(new MockApiElement(
          "java.lang.Object::foo"), ApiChange.Status.FINAL_ADDED),}), apiChanges);

      // secondApi is the reference Api
      apiChanges = getApiChanges(secondApi, firstApi);
      assertEquals(Arrays.asList(new ApiChange[] {new ApiChange(new MockApiElement(
          "java.lang.Object"), ApiChange.Status.FINAL_ADDED),}), apiChanges);
    }
  }

  /**
   * Test when constructor overloading results in Api incompatibilities.
   * <p>
   * Imagine a class Foo had a constructor Foo(String ..). If in the new Api, a
   * constructor Foo(Integer ..) is added, ApiChecker should output a
   * OVERLOADED_METHOD_CALL Api change (because Foo(null) cannot be compiled).
   * However, if Foo(Object ..) is added, it should be okay since JLS matches
   * from the most specific to the least specific.
   */
  private static class OverloadedConstructorRefactoring {
    private static String getFirstApiSourceForObject() {
      StringBuffer sb = new StringBuffer();
      sb.append("package java.lang;\n");
      sb.append("public class Object {\n");
      sb.append("\tpublic static class Foo extends java.lang.Object {\n");
      sb.append("\tpublic Foo(Foo x){}\n");
      sb.append("\t}\n");
      sb.append("\tpublic static class Bar extends java.lang.Object {\n");
      sb.append("\tpublic Bar(Bar y){}\n");
      sb.append("\t}\n");
      sb.append("}\n");
      return sb.toString();
    }

    private static String getSecondApiSourceForObject() {
      StringBuffer sb = new StringBuffer();
      sb.append("package java.lang;\n");
      sb.append("public class Object {\n");
      sb.append("\tpublic static class Foo extends java.lang.Object {\n");
      sb.append("\tpublic Foo(Foo x){}\n");
      sb.append("\tpublic Foo(Object x){}\n");
      sb.append("\t}\n");
      sb.append("\tpublic static class Bar extends java.lang.Object {\n");
      sb.append("\tpublic Bar(Bar y){}\n");
      sb.append("\tpublic Bar(Foo y){}\n");
      sb.append("\t}\n");
      sb.append("}\n");
      return sb.toString();
    }

    void testBothWays() throws NotFoundException, UnableToCompleteException {
      Map<String, String> firstApi = new HashMap<String, String>();
      firstApi.put("java.lang.Object", getFirstApiSourceForObject());
      Map<String, String> secondApi = new HashMap<String, String>();
      secondApi.put("java.lang.Object", getSecondApiSourceForObject());

      // firstApi is the reference Api
      Collection<ApiChange> apiChanges = getApiChanges(firstApi, secondApi);
      assertEquals(Arrays.asList(new ApiChange[] {new ApiChange(new MockApiElement(
          "java.lang.Object.Bar::Bar(Ljava/lang/Object$Bar;)"),
          ApiChange.Status.OVERLOADED_METHOD_CALL),}), apiChanges);

      // secondApi is the reference Api
      apiChanges = getApiChanges(secondApi, firstApi);
      assertEquals(Arrays.asList(new ApiChange[] {
          new ApiChange(new MockApiElement("java.lang.Object.Foo::Foo(Ljava/lang/Object;)"),
              ApiChange.Status.MISSING),
          new ApiChange(new MockApiElement("java.lang.Object.Bar::Bar(Ljava/lang/Object$Foo;)"),
              ApiChange.Status.MISSING),}), apiChanges);
    }
  }

  /**
   * Test when method overloading results in Api incompatibilities.
   * <p>
   * Imagine a class Foo had a method foo(String ..). If in the new Api, a
   * method foo(Integer ..) is added, ApiChecker should output a
   * OVERLOADED_METHOD_CALL Api change (because foo(null) cannot be compiled).
   * However, if foo(Object ..) is added, it should be okay since JLS matches
   * from the most specific to the least specific.
   */
  private static class OverloadedMethodRefactoring {
    private static String getFirstApiSourceForObject() {
      StringBuffer sb = new StringBuffer();
      sb.append("package java.lang;\n");
      sb.append("public class Object {\n");
      sb.append("\tstatic class Foo extends java.lang.Object {\n");
      sb.append("\t}\n");
      sb.append("\tstatic class Bar extends java.lang.Object {\n");
      sb.append("\t}\n");
      sb.append("\tpublic void fooObject(Foo x){}\n");
      sb.append("\tpublic void fooBar(Foo y){}\n");
      sb.append("}\n");
      return sb.toString();
    }

    private static String getSecondApiSourceForObject() {
      StringBuffer sb = new StringBuffer();
      sb.append("package java.lang;\n");
      sb.append("public class Object {\n");
      sb.append("\tstatic class Foo extends java.lang.Object {\n");
      sb.append("\t}\n");
      sb.append("\tstatic class Bar extends java.lang.Object {\n");
      sb.append("\t}\n");
      sb.append("\tpublic void fooObject(Foo x){}\n");
      sb.append("\tpublic void fooObject(Object x){}\n");
      sb.append("\tpublic void fooBar(Foo y){}\n");
      sb.append("\tpublic void fooBar(Bar y){}\n");
      sb.append("}\n");
      return sb.toString();
    }

    void testBothWays() throws NotFoundException, UnableToCompleteException {
      Map<String, String> firstApi = new HashMap<String, String>();
      firstApi.put("java.lang.Object", getFirstApiSourceForObject());
      Map<String, String> secondApi = new HashMap<String, String>();
      secondApi.put("java.lang.Object", getSecondApiSourceForObject());

      // firstApi is the reference Api
      Collection<ApiChange> apiChanges = getApiChanges(firstApi, secondApi);
      assertEquals(Arrays.asList(new ApiChange[] {new ApiChange(new MockApiElement(
          "java.lang.Object::fooBar(Ljava/lang/Object$Foo;)"),
          ApiChange.Status.OVERLOADED_METHOD_CALL),}), apiChanges);

      // secondApi is the reference Api
      apiChanges = getApiChanges(secondApi, firstApi);
      assertEquals(Arrays.asList(new ApiChange[] {
          new ApiChange(new MockApiElement("java.lang.Object::fooBar(Ljava/lang/Object$Bar;)"),
              ApiChange.Status.MISSING),
          new ApiChange(new MockApiElement("java.lang.Object::fooObject(Ljava/lang/Object;)"),
              ApiChange.Status.MISSING),}), apiChanges);
    }
  }

  /**
   * Test whether the ApiChecker correctly identifies moving fields and methods
   * to a super class.
   * 
   */
  private static class SuperClassRefactoring {
    private static String getFirstApiSourceForObject() {
      StringBuffer sb = new StringBuffer();
      sb.append("package java.lang;\n");
      sb.append("public class Object {\n");
      sb.append("\t\tpublic static void staticMethod(){}\n");
      sb.append("\t\tpublic static int staticField = 1;\n");
      sb.append("\t\tpublic int instanceField = 2;\n");
      sb.append("\tpublic static class Foo extends java.lang.Object {\n");
      sb.append("\t}\n");
      sb.append("}\n");
      return sb.toString();
    }

    private static String getSecondApiSourceForObject() {
      StringBuffer sb = new StringBuffer();
      sb.append("package java.lang;\n");
      sb.append("public class Object {\n");
      sb.append("\tpublic static class Foo extends java.lang.Object {\n");
      sb.append("\t\tpublic static void staticMethod(){}\n");
      sb.append("\t\tpublic static int staticField = 1;\n");
      sb.append("\t\tpublic int instanceField = 2;\n");
      sb.append("\t}\n");
      sb.append("}\n");
      return sb.toString();
    }

    void testBothWays() throws NotFoundException, UnableToCompleteException {
      Map<String, String> firstApi = new HashMap<String, String>();
      firstApi.put("java.lang.Object", getFirstApiSourceForObject());
      Map<String, String> secondApi = new HashMap<String, String>();
      secondApi.put("java.lang.Object", getSecondApiSourceForObject());

      // firstApi is the reference Api
      Collection<ApiChange> apiChanges = getApiChanges(firstApi, secondApi);
      assertEquals(Arrays.asList(new ApiChange[] {
          new ApiChange(new MockApiElement("java.lang.Object::instanceField"),
              ApiChange.Status.MISSING),
          new ApiChange(new MockApiElement("java.lang.Object::staticField"),
              ApiChange.Status.MISSING),
          new ApiChange(new MockApiElement("java.lang.Object::staticMethod()"),
              ApiChange.Status.MISSING),}), apiChanges);

      // secondApi is the reference Api
      apiChanges = getApiChanges(secondApi, firstApi);
      assertEquals(0, apiChanges.size());
    }
  }

  /**
   * Assert that two ApiChanges are equal.
   */
  static void assertEquals(ApiChange apiChange1, ApiChange apiChange2) {
    assert apiChange1 != null;
    assert apiChange2 != null;
    assertEquals(apiChange1.getStatus(), apiChange2.getStatus());
    assertEquals(apiChange1.getApiElement().getRelativeSignature(), apiChange2.getApiElement()
        .getRelativeSignature());
  }

  /**
   * Assert that two sets of ApiChanges are equal.
   */
  static void assertEquals(Collection<ApiChange> collection1, Collection<ApiChange> collection2) {
    assertEquals(collection1.size(), collection2.size());

    List<ApiChange> list1 = new ArrayList<ApiChange>();
    list1.addAll(collection1);
    Collections.sort(list1);

    List<ApiChange> list2 = new ArrayList<ApiChange>();
    list2.addAll(collection2);
    Collections.sort(list2);

    for (int i = 0; i < list1.size(); i++) {
      assertEquals(list1.get(i), list2.get(i));
    }
  }

  /**
   * Returns the apiChanges from moving to an existing api to a new Api.
   * 
   * @param existingTypesToSourcesMap existing Api
   * @param newTypesToSourcesMap new Api
   * @return A collection of ApiChange
   */
  static Collection<ApiChange> getApiChanges(Map<String, String> existingTypesToSourcesMap,
      Map<String, String> newTypesToSourcesMap) throws UnableToCompleteException, NotFoundException {

    AbstractTreeLogger logger = new PrintWriterTreeLogger();
    logger.setMaxDetail(TreeLogger.ERROR);

    Set<Resource> set1 = new HashSet<Resource>();
    for (Map.Entry<String, String> entry : existingTypesToSourcesMap.entrySet()) {
      set1.add(new StaticJavaResource(entry.getKey(), entry.getValue()));
    }
    Set<String> emptyList = Collections.emptySet();
    Set<Resource> set2 = new HashSet<Resource>();
    for (String type : existingTypesToSourcesMap.keySet()) {
      set2.add(new StaticJavaResource(type, newTypesToSourcesMap.get(type)));
    }

    ApiContainer existingApi = new ApiContainer("existingApi", set1, emptyList, logger);
    ApiContainer newApi = new ApiContainer("newApi", set2, emptyList, logger);
    return ApiCompatibilityChecker.getApiDiff(newApi, existingApi, emptyList);
  }

  public void testConstructorOverloading() throws NotFoundException, UnableToCompleteException {
    new OverloadedConstructorRefactoring().testBothWays();
  }

  public void testFinalKeywordRefactoring() throws NotFoundException, UnableToCompleteException {
    new FinalKeywordRefactoring().testBothWays();
  }

  public void testMethodOverloading() throws NotFoundException, UnableToCompleteException {
    new OverloadedMethodRefactoring().testBothWays();
  }

  public void testSuperClassRefactoring() throws NotFoundException, UnableToCompleteException {
    new SuperClassRefactoring().testBothWays();
  }

}
