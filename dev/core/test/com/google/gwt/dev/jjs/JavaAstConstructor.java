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
package com.google.gwt.dev.jjs;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.impl.JavaResourceBase;
import com.google.gwt.dev.javac.impl.MockJavaResource;
import com.google.gwt.dev.jdt.AbstractCompiler.CompilationResults;
import com.google.gwt.dev.jdt.BasicWebModeCompiler;
import com.google.gwt.dev.jdt.FindDeferredBindingSitesVisitor;
import com.google.gwt.dev.jjs.CorrelationFactory.DummyCorrelationFactory;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.impl.AssertionNormalizer;
import com.google.gwt.dev.jjs.impl.BuildTypeMap;
import com.google.gwt.dev.jjs.impl.TypeLinker;
import com.google.gwt.dev.jjs.impl.FixAssignmentToUnbox;
import com.google.gwt.dev.jjs.impl.GenerateJavaAST;
import com.google.gwt.dev.jjs.impl.JavaScriptObjectNormalizer;
import com.google.gwt.dev.jjs.impl.TypeMap;
import com.google.gwt.dev.js.ast.JsProgram;

import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Constructs a Java AST for testing.
 */
public class JavaAstConstructor {

  public static final MockJavaResource ARRAY = new MockJavaResource(
      "com.google.gwt.lang.Array") {
    @Override
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.lang;\n");
      code.append("public final class Array {\n");
      code.append("  public int length = 0;\n");
      code.append("  protected Class<?> arrayClass = null;\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource CLASS = new MockJavaResource(
      "java.lang.Class") {
    @Override
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package java.lang;\n");
      code.append("import com.google.gwt.core.client.JavaScriptObject;\n");
      code.append("public final class Class<T> {\n");
      code.append("  static <T> Class<T> createForArray(String packageName, String className, String seedName, Class<?> componentType) { return new Class<T>(); }\n");
      code.append("  static <T> Class<T> createForClass(String packageName, String className, String seedName, Class<? super T> superclass) { return new Class<T>(); }\n");
      code.append("  static <T> Class<T> createForEnum(String packageName, String className, String seedName, Class<? super T> superclass, JavaScriptObject enumConstantsFunc, JavaScriptObject enumValueOfFunc) { return new Class<T>(); }\n");
      code.append("  static <T> Class<T> createForInterface(String packageName, String className) { return new Class<T>(); }\n");
      code.append("  static <T> Class<T> createForPrimitive(String packageName, String className, String jni) { return new Class<T>(); }\n");
      code.append("  static boolean isClassMetadataEnabled() { return true; }\n");
      code.append("  public boolean desiredAssertionStatus() { return true; }\n");
      code.append("  public String getName() { return null; }\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource CLASSLITERALHOLDER = new MockJavaResource(
      "com.google.gwt.lang.ClassLiteralHolder") {
    @Override
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.lang;\n");
      code.append("final class ClassLiteralHolder {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource ENUM = new MockJavaResource(
      "java.lang.Enum") {
    @Override
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package java.lang;\n");
      code.append("import java.io.Serializable;\n");
      code.append("import com.google.gwt.core.client.JavaScriptObject;\n");
      code.append("public abstract class Enum<E extends Enum<E>> implements Serializable {\n");
      code.append("  protected Enum(String name, int ordinal) {}\n");
      code.append("  protected static <T extends Enum<T>> JavaScriptObject createValueOfMap(T[] enumConstants) { return null; }\n");
      code.append("  protected static <T extends Enum<T>> T valueOf(JavaScriptObject map, String name) { return null; }\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource GWT = new MockJavaResource(
      "com.google.gwt.core.client.GWT") {
    @Override
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.core.client;\n");
      code.append("public final class GWT {\n");
      code.append("  public boolean isClient() { return true; };\n");
      code.append("  public boolean isProdMode() { return true; };\n");
      code.append("  public boolean isScript() { return true; };\n");
      code.append("  public static void runAsync(RunAsyncCallback callback) { }\n");
      code.append("  public static void runAsync(Class<?> name, RunAsyncCallback callback) { }\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource RUNASYNCCALLBACK = new MockJavaResource(
      "com.google.gwt.core.client.RunAsyncCallback") {
    @Override
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.core.client;\n");
      code.append("public interface RunAsyncCallback { }\n");
      return code;
    }
  };
  public static final MockJavaResource STATS = new MockJavaResource(
      "com.google.gwt.lang.Stats") {
    @Override
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.lang;\n");
      code.append("public class Stats {\n");
      code.append("  public boolean isStatsAvailable() { return false; };\n");
      code.append("}\n");
      return code;
    }
  };

  public static JProgram construct(TreeLogger logger, CompilationState state,
      String... entryPoints) throws UnableToCompleteException {
    return construct(logger, state, TypeLinker.NULL_TYPE_LINKER,
        entryPoints);
  }

  public static JProgram construct(TreeLogger logger, CompilationState state,
      TypeLinker linker, String... entryPoints)
      throws UnableToCompleteException {
    Set<String> allRootTypes = new TreeSet<String>(Arrays.asList(entryPoints));
    for (MockJavaResource resource : getCompilerTypes()) {
      allRootTypes.add(resource.getTypeName());
    }

    CompilationResults units = 
        BasicWebModeCompiler.getCompilationUnitDeclarations(logger, state,
        linker, allRootTypes.toArray(new String[allRootTypes.size()]));

    CompilationUnitDeclaration[] goldenCuds = units.compiledUnits;

    // Check for compilation problems. We don't log here because any problems
    // found here will have already been logged by AbstractCompiler.
    //
    JavaToJavaScriptCompiler.checkForErrors(logger, goldenCuds, false);

    /*
     * FindDeferredBindingSitesVisitor detects errors in usage of magic methods
     * in the GWT class.
     */
    for (CompilationUnitDeclaration jdtCud : goldenCuds) {
      jdtCud.traverse(new FindDeferredBindingSitesVisitor(), jdtCud.scope);
    }

    JavaToJavaScriptCompiler.checkForErrors(logger, goldenCuds, true);

    CorrelationFactory correlator = new DummyCorrelationFactory();
    JProgram jprogram = new JProgram(correlator);
    JsProgram jsProgram = new JsProgram(correlator);

    /*
     * (1) Build a flattened map of TypeDeclarations => JType. The resulting map
     * contains entries for all reference types. BuildTypeMap also parses all
     * JSNI.
     */
    TypeMap typeMap = new TypeMap(jprogram);
    TypeDeclaration[] allTypeDeclarations = BuildTypeMap.exec(typeMap,
        units, jsProgram, linker);

    // BuildTypeMap can uncover syntactic JSNI errors; report & abort
    JavaToJavaScriptCompiler.checkForErrors(logger, goldenCuds, true);

    // Compute all super type/sub type info
    jprogram.typeOracle.computeBeforeAST();

    // (2) Create our own Java AST from the JDT AST.
    JJSOptionsImpl options = new JJSOptionsImpl();
    options.setEnableAssertions(true);
    GenerateJavaAST.exec(allTypeDeclarations, typeMap, jprogram, jsProgram,
        options);

    // GenerateJavaAST can uncover semantic JSNI errors; report & abort
    JavaToJavaScriptCompiler.checkForErrors(logger, goldenCuds, true);

    // (3) Perform Java AST normalizations.
    FixAssignmentToUnbox.exec(jprogram);
    // Turn into assertion checking calls.
    AssertionNormalizer.exec(jprogram);

    // Add entry methods for entry points.
    for (String entryPoint : entryPoints) {
      JDeclaredType entryType = jprogram.getFromTypeMap(entryPoint);
      for (JMethod method : entryType.getMethods()) {
        if (method.isStatic() && JProgram.isClinit(method)) {
          jprogram.addEntryMethod(method);
        }
      }
    }
    // Replace references to JSO subtypes with JSO itself.
    JavaScriptObjectNormalizer.exec(jprogram);

    // Tree is now ready to optimize.
    return jprogram;
  }

  public static MockJavaResource[] getCompilerTypes() {
    List<MockJavaResource> result = new ArrayList<MockJavaResource>();
    Collections.addAll(result, JavaResourceBase.getStandardResources());
    // Replace the basic Class and Enum with a compiler-specific one.
    result.remove(JavaResourceBase.CLASS);
    result.remove(JavaResourceBase.ENUM);
    Collections.addAll(result, ARRAY, CLASS, CLASSLITERALHOLDER, ENUM, GWT,
        RUNASYNCCALLBACK);
    return result.toArray(new MockJavaResource[result.size()]);
  }
}
