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
import com.google.gwt.core.ext.typeinfo.JAbstractMethod;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.jdt.CacheManager;
import com.google.gwt.dev.jdt.StaticCompilationUnitProvider;
import com.google.gwt.dev.jdt.TypeOracleBuilder;
import com.google.gwt.dev.util.log.AbstractTreeLogger;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import junit.framework.TestCase;

/**
 * Test ApiContainer.
 */
public class ApiContainerTest extends TestCase {
  @SuppressWarnings("unused")
  class TestA {
    public TestA(String args) {
    }

    protected TestA(TestA a) {
    }

    public String valueOf(Object o) {
      return "a";
    }

    public String valueOf(String s) {
      return "";
    }
  }

  class TestB extends TestA {
    public TestB(TestA a) {
      super(a);
    }

    public void main(String args[]) {
      TestA x1 = new TestA("test");
      x1.valueOf("ab");
      TestB x2 = new TestB(x1);
      new TestB(x2);
    }
  }

  static StaticCompilationUnitProvider cuApiClass = new StaticCompilationUnitProvider(
      "test.apicontainer", "ApiClass", getSourceForApiClass());

  static StaticCompilationUnitProvider cuNewPackage = new StaticCompilationUnitProvider(
      "java.newpackage", "Test", getSourceForTest());

  static StaticCompilationUnitProvider cuNonApiClass = new StaticCompilationUnitProvider(
      "test.apicontainer", "NonApiClass", getSourceForNonApiClass());
  static StaticCompilationUnitProvider cuNonApiPackage = new StaticCompilationUnitProvider(
      "test.nonapipackage", "TestClass", getSourceForTestClass());

  static StaticCompilationUnitProvider cuObject = new StaticCompilationUnitProvider(
      "java.lang", "Object", getSourceForObject());

  private static JAbstractMethod getMethodByName(String name, ApiClass apiClass) {
    return (apiClass.getApiMethodsByName(name, ApiClass.MethodType.METHOD).toArray(
        new ApiAbstractMethod[0])[0]).getMethodObject();
  }

  private static char[] getSourceForApiClass() {
    StringBuffer sb = new StringBuffer();
    sb.append("package test.apicontainer;\n");
    sb.append("public class ApiClass extends NonApiClass {\n");
    sb.append("\tpublic void apiMethod() { };\n");
    sb.append("\tpublic java.lang.Object checkParametersAndReturnTypes(ApiClass a) { return this; };\n");
    sb.append("};\n");
    return sb.toString().toCharArray();
  }

  private static char[] getSourceForNonApiClass() {
    StringBuffer sb = new StringBuffer();
    sb.append("package test.apicontainer;\n");
    sb.append("class NonApiClass extends java.lang.Object {\n");
    sb.append("\tpublic void methodInNonApiClass(java.lang.Object a) { };\n");
    sb.append("\tpublic int fieldInNonApiClass = 3;\n");
    sb.append("\tprotected class ApiClassInNonApiClass {\n");
    sb.append("\tprotected ApiClassInNonApiClass() { }\n");
    sb.append("\t}\n");
    sb.append("\tprotected final class AnotherApiClassInNonApiClass {\n");
    sb.append("\tprivate AnotherApiClassInNonApiClass() { }\n");
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
    sb.append("\tprotected native long protectedMethod();\n");
    sb.append("\tpublic int apiField = 0;\n");
    sb.append("\tprivate int internalField = 0;\n");
    sb.append("\tprotected int protectedField=2;\n");
    sb.append("}\n");
    return sb.toString().toCharArray();
  }

  private static char[] getSourceForTest() {
    StringBuffer sb = new StringBuffer();
    sb.append("package java.newpackage;\n");
    sb.append("public class Test { }\n");
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

  ApiContainer api1 = null;

  // TODO (amitmanjhi): Try using UnitTestTreeLogger to capture log messages
  public TypeOracle getNewTypeOracleWithCompilationUnitsAdded()
      throws UnableToCompleteException {

    // Build onto an empty type oracle.
    TypeOracleBuilder builder1 = new TypeOracleBuilder(new CacheManager(null,
        null, ApiCompatibilityChecker.DISABLE_CHECKS));
    builder1.addCompilationUnit(cuObject);
    builder1.addCompilationUnit(cuNonApiClass);
    builder1.addCompilationUnit(cuApiClass);
    builder1.addCompilationUnit(cuNonApiPackage);
    builder1.addCompilationUnit(cuNewPackage);
    AbstractTreeLogger logger = new PrintWriterTreeLogger();
    logger.setMaxDetail(TreeLogger.ERROR);
    TypeOracle typeOracle = builder1.build(logger);
    return typeOracle;
  }

  /**
   * Class hierarchy. public java.lang.Object -- test.apicontainer.NonApiClass
   * (encloses ApiClassInNonApiClass and AnotherApiClassInNonApiClass) -- public
   * test.apicontainer.ApiClass -- test.nonapipackage.TestClass
   */
  @Override
  public void setUp() {
    AbstractTreeLogger logger1 = new PrintWriterTreeLogger();
    logger1.setMaxDetail(TreeLogger.ERROR);

    try {
      api1 = new ApiContainer("ApiContainerTest", logger1,
          getNewTypeOracleWithCompilationUnitsAdded());
    } catch (Exception ex) {
      // JSNI checks are active.
      assertEquals("jsni checks are probably active", "failed");
    }
  }

  /**
   * Check if apiClasses are determined correctly. Check if inner classes are
   * classified correctly as api classes.
   */
  public void testApiClass() {
    ApiPackage package1 = api1.getApiPackage("java.lang");
    ApiPackage package2 = api1.getApiPackage("test.apicontainer");
    assertNotNull(package1);
    assertNotNull(package2);

    assertNull(package2.getApiClass("test.apicontainer.NonApiClass"));
    assertNotNull(package1.getApiClass("java.lang.Object"));
    assertNotNull(package2.getApiClass("test.apicontainer.ApiClass"));
    assertNotNull(package2.getApiClass("test.apicontainer.NonApiClass.ApiClassInNonApiClass"));
    assertNotNull(package2.getApiClass("test.apicontainer.NonApiClass.AnotherApiClassInNonApiClass"));
    assertEquals(1, package1.getApiClassNames().size());
    assertEquals(3, package2.getApiClassNames().size());
  }

  /**
   * Since constructors and methods use the same code, check methods in most
   * cases. Also need to check apiFields.
   * 
   * For methods, check if: (a) inherited methods are identified correctly as
   * apiMethods, (b) method overloading is done correctly
   * 
   */
  public void testApiMembers() {
    ApiClass object = api1.getApiPackage("java.lang").getApiClass(
        "java.lang.Object");
    ApiClass apiClass = api1.getApiPackage("test.apicontainer").getApiClass(
        "test.apicontainer.ApiClass");
    ApiClass innerClass = api1.getApiPackage("test.apicontainer").getApiClass(
        "test.apicontainer.NonApiClass.ApiClassInNonApiClass");

    // constructors
    assertEquals(1, innerClass.getApiMemberNames(
        ApiClass.MethodType.CONSTRUCTOR).size());

    // fields
    assertEquals(2, object.getApiFieldNames().size());
    assertEquals(3, apiClass.getApiFieldNames().size());

    // methods
    assertEquals(2, object.getApiMemberNames(ApiClass.MethodType.METHOD).size());
    assertEquals(4,
        apiClass.getApiMemberNames(ApiClass.MethodType.METHOD).size());
    // the method definition lowest in the class hierarchy is kept
    assertNotSame(getMethodByName("apiMethod0", apiClass), getMethodByName(
        "apiMethod0", object));
    assertEquals(getMethodByName("protectedMethod0", apiClass),
        getMethodByName("protectedMethod0", object));
    assertNotNull(getMethodByName("methodInNonApiClass1", apiClass));
  }

  /**
   * Test if apiPackages are identified correctly.
   */
  public void testApiPackages() {
    assertNotNull(api1.getApiPackage("java.lang"));
    assertNotNull(api1.getApiPackage("test.apicontainer"));
    assertEquals(3, api1.getApiPackageNames().size());
  }

}
