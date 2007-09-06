/*
 * Copyright 2007 Google Inc.
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
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import java.io.PrintWriter;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

/**
 * This class generates a stub class for classes that derive from GWTTestCase.
 * This stub class provides the necessary bridge between our Hosted or Hybrid
 * mode classes and the JUnit system.
 *
 */
public class JUnitTestCaseStubGenerator extends Generator {

  interface MethodFilter {
    public boolean accept( JMethod method );
  }

  private static final String GWT_TESTCASE_CLASS_NAME = "com.google.gwt.junit.client.GWTTestCase";

  /**
   * Returns the method names for the set of methods that are strictly JUnit
   * test methods (have no arguments).
   *
   * @param requestedClass
   */
  public static String[] getTestMethodNames(JClassType requestedClass) {
    return getAllMethods( requestedClass, new MethodFilter() {
      public boolean accept(JMethod method) {
        return isJUnitTestMethod(method,false);
      }
    } ).keySet().toArray( new String[] {} );
  }

  /**
   * Like JClassType.getMethod( String name ), except:
   *
   *  <li>it accepts a filter</li>
   *  <li>it searches the inheritance hierarchy (includes subclasses)</li>
   *
   * For methods which are overriden, only the most derived implementations are included.
   *
   * @param type The type to search. Must not be null
   * @return Map<String.List<JMethod>> The set of matching methods. Will not be null.
   */
  static Map<String,List<JMethod>> getAllMethods( JClassType type, MethodFilter filter ) {
    Map<String,List<JMethod>> methods = new HashMap<String,List<JMethod>>();
    JClassType cls = type;

    while (cls != null) {
      JMethod[] clsDeclMethods = cls.getMethods();

      // For every method, include it iff our filter accepts it
      // and we don't already have a matching method
      for (int i = 0, n = clsDeclMethods.length; i < n; ++i) {

        JMethod declMethod = clsDeclMethods[i];

        if ( ! filter.accept(declMethod) ) {
          continue;
        }

        List<JMethod> list = methods.get(declMethod.getName());

        if (list == null) {
          list = new ArrayList<JMethod>();
          methods.put(declMethod.getName(),list);
          list.add(declMethod);
          continue;
        }

        JParameter[] declParams = declMethod.getParameters();

        for (int j = 0; j < list.size(); ++j) {
          JMethod method = list.get(j);
          JParameter[] parameters = method.getParameters();
          if ( ! equals( declParams, parameters )) {
            list.add(declMethod );
          }
        }
      }
      cls = cls.getSuperclass();
    }

    return methods;
  }

  /**
   * Returns true if the method is considered to be a valid JUnit test method.
   * The criteria are that the method's name begin with "test" and have public
   * access. The method may be static. You must choose to include or exclude
   * methods which have arguments.
   *
   */
  static boolean isJUnitTestMethod(JMethod method, boolean acceptArgs) {
    if (!method.getName().startsWith("test")) {
      return false;
    }

    if (!method.isPublic()) {
      return false;
    }

    return acceptArgs || method.getParameters().length == 0 && ! acceptArgs;
  }

  /**
   * Returns true iff the two sets of parameters are of the same lengths and types.
   *
   * @param params1 must not be null
   * @param params2 must not be null
   */
  private static boolean equals( JParameter[] params1, JParameter[] params2 ) {
    if ( params1.length != params2.length ) {
      return false;
    }
    for ( int i = 0; i < params1.length; ++i ) {
      if ( params1[ i ].getType() != params2[ i ].getType() ) {
        return false;
      }
    }
    return true;
  }

  String qualifiedStubClassName;
  String simpleStubClassName;
  String typeName;
  TreeLogger logger;
  String packageName;

  private JClassType requestedClass;
  private SourceWriter sourceWriter;
  private TypeOracle typeOracle;

  /**
   * Create a new type that statisfies the rebind request.
   */
  @Override
  public String generate(TreeLogger logger, GeneratorContext context,
      String typeName) throws UnableToCompleteException {

    if ( ! init( logger, context, typeName ) ) {
      return qualifiedStubClassName;
    }

    writeSource();
    sourceWriter.commit( logger );

    return qualifiedStubClassName;
  }

  public JClassType getRequestedClass() {
    return requestedClass;
  }

  public SourceWriter getSourceWriter() {
    return sourceWriter;
  }

  public TypeOracle getTypeOracle() {
    return typeOracle;
  }

  boolean init(TreeLogger logger, GeneratorContext context,String typeName) throws
      UnableToCompleteException {

    this.typeName = typeName;
    this.logger = logger;
    typeOracle = context.getTypeOracle();
    assert typeOracle != null;

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
    simpleStubClassName = getSimpleStubClassName(requestedClass);
    packageName = requestedClass.getPackage().getName();
    qualifiedStubClassName = packageName + "." + simpleStubClassName;

    sourceWriter = getSourceWriter(logger, context, packageName,
        simpleStubClassName, requestedClass.getQualifiedSourceName());

    return sourceWriter != null;
  }

  @SuppressWarnings("unused")
  void writeSource() throws UnableToCompleteException {
    String[] testMethods = getTestMethodNames(requestedClass);
    writeGetNewTestCase(simpleStubClassName, sourceWriter);
    writeDoRunTestMethod(testMethods, sourceWriter);
    writeGetTestName(typeName, sourceWriter);
  }

  /**
   * Gets the name of the native stub class.
   */
  private String getSimpleStubClassName(JClassType baseClass) {
    return "__" + baseClass.getSimpleSourceName() + "_unitTestImpl";
  }

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
