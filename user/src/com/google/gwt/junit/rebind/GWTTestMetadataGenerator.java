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
package com.google.gwt.junit.rebind;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.ConfigurationProperty;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JAbstractMethod;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.util.collect.HashMap;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.junit.client.GWTTestCase.TestModuleInfo;
import com.google.gwt.junit.client.impl.JUnitHost.TestInfo;
import com.google.gwt.junit.client.impl.MissingTestPlaceHolder;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A generator that generates {@code GWTTestMetadata}.
 */
public class GWTTestMetadataGenerator extends Generator {

  private static final String BASE_CLASS = "com.google.gwt.junit.client.impl.GWTTestMetadata";
  private static final JType[] NO_PARAMS = new JType[0];

  private static String getPackagePrefix(JClassType classType) {
    String name = classType.getPackage().getName();
    return (name.length() == 0) ? name : (name + '.');
  }

  /**
   * Create a new type that satisfies the rebind request.
   */
  @Override
  public String generate(TreeLogger logger, GeneratorContext context, String typeName)
      throws UnableToCompleteException {
    if (!BASE_CLASS.equals(typeName)) {
      logger.log(TreeLogger.ERROR, "This generator may only be used with " + BASE_CLASS, null);
      throw new UnableToCompleteException();
    }
    JClassType requestedClass;
    try {
      requestedClass = context.getTypeOracle().getType(typeName);
    } catch (NotFoundException e) {
      logger.log(
          TreeLogger.ERROR,
          "Could not find type '"
              + typeName
              + "'; please see the log, as this usually indicates a previous error ",
          e);
      throw new UnableToCompleteException();
    }

    String moduleName;
    try {
      ConfigurationProperty prop = context.getPropertyOracle().getConfigurationProperty(
          "junit.moduleName");
      moduleName = prop.getValues().get(0);
    } catch (BadPropertyValueException e) {
      logger.log(TreeLogger.ERROR,
          "Could not resolve junit.moduleName property", e);
      throw new UnableToCompleteException();
    }

    String packageName = requestedClass.getPackage().getName();
    String generatedClass = requestedClass.getName() + "Impl";

    SourceWriter sourceWriter = getSourceWriter(logger, context, packageName, generatedClass);
    if (sourceWriter != null) {
      writeCreateMethod(sourceWriter, getTestClasses(logger, context, moduleName));
      sourceWriter.commit(logger);
    }
    return packageName + "." + generatedClass;
  }

  /**
   * Will generate following:
   * <pre>
   * public native final JavaScriptObject get() /*-{
   *   return {
   *     'a.b.c.X' = {
   *       'new' : function(test) {
   *          return @a.b.c.X::new();
   *       },
   *       'testMethod1' = function(test) {
   *          return test.@a.b.c.X::testMethod1();
   *       },
   *       'testMethod2' = function(test) {
   *          return test.@a.b.c.X::testMethod1();
   *       },
   *      },
   *
   *     'k.l.m.Y' = {
   *       ...
   *     },
   *
   *     ...
   *   };
   * }-{@literal*}/;
   * </pre>
   */
  private void writeCreateMethod(SourceWriter sw, Map<String, JClassType> testClasses) {
    sw.println("public native final %s get() /*-{", JavaScriptObject.class.getCanonicalName());
    sw.indent();
    sw.println("return {");
    for (Map.Entry<String, JClassType> entry : testClasses.entrySet()) {
      sw.println("'%s': {", entry.getKey());
      writeFunctionMap(sw, entry.getValue());
      sw.println("},");
    }
    sw.println("};");
    sw.outdent();
    sw.println("}-*/;");
  }

  private void writeFunctionMap(SourceWriter sw, JClassType jClassType) {
    writeFunction(sw, jClassType.findConstructor(NO_PARAMS), true);
    for (JMethod method : getTestMethods(jClassType)) {
      writeFunction(sw, method, false);
    }
  }

  private void writeFunction(SourceWriter sw, JAbstractMethod method, boolean isConstructor) {
    // Static method are also valid test methods
    String object = (isConstructor || method.isMethod().isStatic()) ? "" : "test.";
    String call = object + method.getJsniSignature();
    String methodName = isConstructor ? "new" : method.getName();
    sw.println("'%s' : function(test) { return %s(); },", methodName, call);
  }

  private Map<String, JClassType> getTestClasses(
      TreeLogger logger, GeneratorContext context, String moduleName)
      throws UnableToCompleteException {
    // Check the global set of active tests for this module.
    TestModuleInfo moduleInfo = GWTTestCase.getTestsForModule(moduleName);
    Set<TestInfo> moduleTests = (moduleInfo == null) ? null : moduleInfo.getTests();
    if (moduleTests == null || moduleTests.isEmpty()) {
      logger.log(TreeLogger.ERROR, "No active tests found in module: " + moduleName);
      throw new UnableToCompleteException();
    }
    Map<String, JClassType> testClasses = new LinkedHashMap<String, JClassType>();
    for (TestInfo testInfo : moduleTests) {
      String testClassName = testInfo.getTestClass();
      testClasses.put(testClassName, getTestClass(context.getTypeOracle(), testClassName));
    }
    return testClasses;
  }

  /**
   * Returns the test class. If the class is not found in {@link TypeOracle}, then a place holder is
   * returned in order to continue code generation.
   * <p>
   * If we don't continue code generation, we usually can't see the real cause of the compilation
   * error in the logs. This also provides the benefit of continuing testing with the rest of test
   * classes that still compiles.
   */
  private JClassType getTestClass(TypeOracle typeOracle, String testClassName) {
    JClassType type = typeOracle.findType(testClassName);
    return type != null ? type
        : typeOracle.findType(MissingTestPlaceHolder.class.getCanonicalName());
  }

  private SourceWriter getSourceWriter(
      TreeLogger logger, GeneratorContext ctx, String packageName, String className) {
    PrintWriter printWriter = ctx.tryCreate(logger, packageName, className);
    if (printWriter == null) {
      return null;
    }

    ClassSourceFileComposerFactory composerFactory =
        new ClassSourceFileComposerFactory(packageName, className);
    composerFactory.setSuperclass(BASE_CLASS);
    return composerFactory.createSourceWriter(ctx, printWriter);
  }

  // This is compatible with how junit3 identifies test methods
  private static Iterable<JMethod> getTestMethods(JClassType requestedClass) {
    Map<String, JMethod> methodMap = new HashMap<String, JMethod>();
    for (JClassType cls = requestedClass; cls != null; cls = cls.getSuperclass()) {
      for (JMethod declMethod : cls.getMethods()) {
        if (isJUnitTestMethod(declMethod)) {
          putIfAbsent(methodMap, declMethod);
        }
      }
    }
    return methodMap.values();
  }

  private static void putIfAbsent(Map<String, JMethod> methodMap, JMethod declMethod) {
    if (!methodMap.containsKey(declMethod.getName())) {
      methodMap.put(declMethod.getName(), declMethod);
    }
  }

  private static boolean isJUnitTestMethod(JMethod m) {
    return m.isPublic() && m.getName().startsWith("test") && m.getParameters().length == 0
        && m.getReturnType().getQualifiedBinaryName().equals(Void.TYPE.getName());
  }
}
