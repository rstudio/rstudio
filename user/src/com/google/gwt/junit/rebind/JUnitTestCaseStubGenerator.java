/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.junit.rebind;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import java.io.PrintWriter;
import java.util.HashSet;

/**
 * This class generates a stub class for classes that derive from GWTTestCase.
 * This stub class provides the necessary bridge between our Hosted or Hybrid
 * mode classes and the JUnit system.
 */
public class JUnitTestCaseStubGenerator extends Generator {

  private static final String GWT_TESTCASE_CLASS_NAME = GWTTestCase.class.getName();

  /**
   * Create a new type that statisfies the rebind request.
   */
  public String generate(TreeLogger logger, GeneratorContext context,
      String typeName) throws UnableToCompleteException {

    TypeOracle typeOracle = context.getTypeOracle();
    assert typeOracle != null;

    JClassType requestedClass;
    try {
      requestedClass = typeOracle.getType(typeName);
    } catch (NotFoundException e) {
      logger.log(TreeLogger.ERROR, "Could not find type '" + typeName
        + "'; please see the log, as this usually indicates a previous error ",
        e);
      throw new UnableToCompleteException();
    }

    // Get the stub class name, and see if its source file exists.
    //
    String simpleStubClassName = getSimpleStubClassName(requestedClass);

    String packageName = requestedClass.getPackage().getName();
    String qualifiedStubClassName = packageName + "." + simpleStubClassName;

    SourceWriter sw = getSourceWriter(logger, context, packageName,
      simpleStubClassName, requestedClass.getQualifiedSourceName());
    if (sw == null) {
      return qualifiedStubClassName;
    }

    String[] testMethods = getTestMethodNames(requestedClass);
    writeGetNewTestCase(simpleStubClassName, sw);
    writeDoRunTestMethod(testMethods, sw);
    writeGetTestName(typeName, sw);

    sw.commit(logger);

    return qualifiedStubClassName;
  }

  /**
   * Gets the name of the native stub class.
   */
  private String getSimpleStubClassName(JClassType baseClass) {
    return "__" + baseClass.getSimpleSourceName() + "_unitTestImpl";
  }

  /**
   * 
   */
  private SourceWriter getSourceWriter(TreeLogger logger, GeneratorContext ctx,
      String packageName, String className, String superclassName) {

    PrintWriter printWriter = ctx.tryCreate(logger, packageName, className);
    if (printWriter == null) {
      return null;
    }

    ClassSourceFileComposerFactory composerFactory = new ClassSourceFileComposerFactory(
      packageName, className);

    composerFactory.setSuperclass(superclassName);

    return composerFactory.createSourceWriter(ctx, printWriter);
  }

  /**
   * Given a class return all methods that are considered JUnit test methods up
   * to but not including the declared methods of the class named
   * GWT_TESTCASE_CLASS_NAME.
   */
  private String[] getTestMethodNames(JClassType requestedClass) {
    HashSet testMethodNames = new HashSet();
    JClassType cls = requestedClass;

    while (true) {
      // We do not consider methods in the GWT superclass or above
      //
      if (isGWTTestCaseClass(cls)) {
        break;
      }

      JMethod[] clsDeclMethods = cls.getMethods();
      for (int i = 0, n = clsDeclMethods.length; i < n; ++i) {
        JMethod declMethod = clsDeclMethods[i];

        // Skip methods that are not JUnit test methods.
        //
        if (!isJUnitTestMethod(declMethod)) {
          continue;
        }

        if (testMethodNames.contains(declMethod.getName())) {
          continue;
        }

        testMethodNames.add(declMethod.getName());
      }

      cls = cls.getSuperclass();
    }

    return (String[]) testMethodNames.toArray(new String[testMethodNames.size()]);
  }

  /**
   * Returns true if the class is the special GWT Test Case derived class.
   */
  private boolean isGWTTestCaseClass(JClassType cls) {
    return cls.getQualifiedSourceName().equalsIgnoreCase(
      GWT_TESTCASE_CLASS_NAME);
  }

  /**
   * Returns true if the method is considered to be a valid JUnit test method.
   * The criteria are that the method's name begin with "test", have public
   * access, and not be static.
   */
  private boolean isJUnitTestMethod(JMethod method) {
    if (!method.getName().startsWith("test")) {
      return false;
    }

    if (!method.isPublic() || method.isStatic()) {
      return false;
    }

    return true;
  }

  private void writeDoRunTestMethod(String[] testMethodNames, SourceWriter sw) {
    sw.println();
    sw.println("protected final void doRunTest(String name) throws Throwable {");
    sw.indent();
    for (int i = 0, n = testMethodNames.length; i < n; ++i) {
      String methodName = testMethodNames[i];

      if (i > 0) {
        sw.print("else ");
      }

      sw.println("if (name.equals(\"" + methodName + "\")) {");
      sw.indentln(methodName + "();");
      sw.println("}");
    }
    sw.outdent();
    sw.println("}"); // finish doRunTest();
  }

  /**
   * Create the appMain method that is the main entry point for the GWT
   * application.
   */
  private void writeGetNewTestCase(String stubClassName, SourceWriter sw) {
    sw.println();
    sw.println("public final " + GWT_TESTCASE_CLASS_NAME
      + " getNewTestCase() {");
    sw.indent();
    sw.println("return new " + stubClassName + "();");
    sw.outdent();
    sw.println("}"); // finish getNewTestCase();
  }

  /**
   * Create the appMain method that is the main entry point for the GWT
   * application.
   */
  private void writeGetTestName(String testClassName, SourceWriter sw) {
    sw.println();
    sw.println("public final String getTestName() {");
    sw.indent();
    sw.println("return \"" + testClassName + "\";");
    sw.outdent();
    sw.println("}"); // finish getNewTestCase();
  }
}
