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

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.ConfigurationProperty;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.SelectionProperty;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JAbstractMethod;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.junit.client.GWTTestCase.TestModuleInfo;
import com.google.gwt.junit.client.impl.GWTRunnerProxy;
import com.google.gwt.junit.client.impl.GWTRunnerProxy.JsniTestAccessor;
import com.google.gwt.junit.client.impl.JUnitHost.TestInfo;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * This class generates a JSNI based {@link GWTRunnerProxy} implementation.
 * <p>
 * For each gwt module following classes will be generated:
 * <li>GwtRunnerProxyImpl abstract class that implements createTestAccessor using JSNI</li>
 * <li>GwtRunnerProxyImplXyz (e.g. GwtRunnerProxyImplSafari) that extends GwtRunnerProxyImpl and
 * implements getUserAgentPropery</li>
 */
public class GWTRunnerProxyGenerator extends Generator {

  private static final String PROXY = GWTRunnerProxy.class.getCanonicalName();
  private static final String JSNI_TEST_ACCESSOR = JsniTestAccessor.class.getCanonicalName();

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
    if (!PROXY.equals(typeName)) {
      logger.log(TreeLogger.ERROR, "This generator may only be used with " + PROXY, null);
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

    String userAgent;
    try {
      SelectionProperty prop = context.getPropertyOracle().getSelectionProperty(
          logger, "user.agent");
      userAgent = prop.getCurrentValue();
    } catch (BadPropertyValueException e) {
      logger.log(TreeLogger.ERROR, "Could not resolve user.agent property", e);
      throw new UnableToCompleteException();
    }

    String packageName = requestedClass.getPackage().getName();

    // Generate the base class shared across different permutations:
    String generatedBaseClass = requestedClass.getName().replace('.', '_') + "Impl";
    SourceWriter sourceWriter =
        getSourceWriter(logger, context, packageName, generatedBaseClass, null, null);
    if (sourceWriter != null) {
      writeMethodCreateTestAccessor(logger, context, moduleName, sourceWriter);
      sourceWriter.commit(logger);
    }

    // Generate the actual class for each permutation"
    String generatedClass = generatedBaseClass + userAgent;
    sourceWriter =
        getSourceWriter(logger, context, packageName, generatedClass, generatedBaseClass, PROXY);
    if (sourceWriter != null) {
      writeGetUserAgentPropertyMethod(userAgent, sourceWriter);
      sourceWriter.commit(logger);
    }

    return packageName + "." + generatedClass;
  }

  /**
   * Will generate following:
   * <pre>
   * public native final JsniTestAccessor createTestAccessor() /*-{
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
  private void writeMethodCreateTestAccessor(
      TreeLogger logger, GeneratorContext context, String moduleName, SourceWriter sw) {
    Set<JClassType> testClasses = getTestClasses(logger, context, moduleName);
    sw.println("public native final %s createTestAccessor() /*-{", JSNI_TEST_ACCESSOR);
    sw.indent();
    sw.println("return {");
    for (JClassType jClassType : testClasses) {
      sw.println("'%s': {", jClassType.getQualifiedBinaryName());
      writeFunctionMap(sw, jClassType);
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

  private void writeGetUserAgentPropertyMethod(String userAgent, SourceWriter sw) {
    sw.println("public final String getUserAgentProperty() {");
    sw.indentln("return \"" + userAgent + "\";");
    sw.println("}");
  }

  private Set<JClassType> getTestClasses(
      TreeLogger logger, GeneratorContext context, String moduleName) {
    // Check the global set of active tests for this module.
    TestModuleInfo moduleInfo = GWTTestCase.getTestsForModule(moduleName);
    Set<TestInfo> moduleTests = (moduleInfo == null) ? null : moduleInfo.getTests();
    if (moduleTests == null || moduleTests.isEmpty()) {
      // Fall back to pulling in all types in the module.
      JClassType[] allTestTypes = getAllPossibleTestTypes(context.getTypeOracle());
      return getTestTypesForModule(logger, moduleName, allTestTypes);
    } else {
      Set<JClassType> testClasses = new LinkedHashSet<JClassType>();
      for (TestInfo testInfo : moduleTests) {
        testClasses.add(context.getTypeOracle().findType(testInfo.getTestClass()));
      }
      return testClasses;
    }
  }

  private JClassType[] getAllPossibleTestTypes(TypeOracle typeOracle) {
    JClassType gwtTestType = typeOracle.findType(GWTTestCase.class.getName());
    if (gwtTestType != null) {
      return gwtTestType.getSubtypes();
    } else {
      return new JClassType[0];
    }
  }

  private SourceWriter getSourceWriter(TreeLogger logger,
      GeneratorContext ctx,
      String packageName,
      String className,
      String superclassName,
      String interfaceName) {
    PrintWriter printWriter = ctx.tryCreate(logger, packageName, className);
    if (printWriter == null) {
      return null;
    }

    ClassSourceFileComposerFactory composerFactory =
        new ClassSourceFileComposerFactory(packageName, className);
    if (superclassName != null) {
      composerFactory.setSuperclass(superclassName);
    }
    if (interfaceName != null) {
      composerFactory.addImplementedInterface(interfaceName);
    }
    return composerFactory.createSourceWriter(ctx, printWriter);
  }

  private Set<JClassType> getTestTypesForModule(
      TreeLogger logger, String moduleName, JClassType[] allTestTypes) {
    // Must use sorted set to prevent nondeterminism.
    Set<JClassType> testClasses = new TreeSet<JClassType>();
    for (JClassType classType : allTestTypes) {
      if (!classType.isPublic() || classType.isAbstract() || !classType.isDefaultInstantiable()) {
        continue;
      }

      String className = getPackagePrefix(classType) + classType.getName().replace('.', '$');

      try {
        Class<?> testClass = Class.forName(className);
        GWTTestCase instantiated = (GWTTestCase) testClass.newInstance();
        if (!moduleName.equals(instantiated.getModuleName())) {
          continue;
        }
      } catch (Throwable e) {
        logger.log(TreeLogger.INFO, "Error determining if test class '" + className
            + "' is a part of the current module; skipping; expect subsequent errors "
            + "if this test class is run", e);
        continue;
      }
      testClasses.add(classType);
    }
    return testClasses;
  }

  private static List<JMethod> getTestMethods(JClassType requestedClass) {
    List<JMethod> list = new ArrayList<JMethod>();
    for (JClassType cls = requestedClass; cls != null; cls = cls.getSuperclass()) {
      for (JMethod declMethod : cls.getMethods()) {
        if (isJUnitTestMethod(declMethod)) {
          list.add(declMethod);
        }
      }
    }
    return list;
  }

  private static boolean isJUnitTestMethod(JMethod m) {
    return m.isPublic() && m.getName().startsWith("test") && m.getParameters().length == 0
        && m.getReturnType().getQualifiedBinaryName().equals(Void.TYPE.getName());
  }
}
