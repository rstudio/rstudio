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
import com.google.gwt.dev.javac.testing.impl.JavaResourceBase;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Constructs a Java AST for testing.
 */
public class JavaAstConstructor {

  public static final MockJavaResource ASYNCFRAGMENTLOADER = new MockJavaResource(
      "com.google.gwt.core.client.impl.AsyncFragmentLoader") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package com.google.gwt.core.client.impl;\n");
      code.append("import com.google.gwt.core.client.RunAsyncCallback;\n");
      code.append("public class AsyncFragmentLoader {\n");
      code.append("  public static void onLoad(int fragment) { }\n");
      code.append("  public static void runAsync(int fragment, RunAsyncCallback callback) { }\n");
      code.append("  public static AsyncFragmentLoader BROWSER_LOADER =\n");
      code.append("    makeBrowserLoader(1, new int[] {});\n");
      code.append("  private static AsyncFragmentLoader makeBrowserLoader(\n");
      code.append("    int numSp, int[] initial) {\n");
      code.append("    return null;\n");
      code.append("  }\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource ARRAY = new MockJavaResource("com.google.gwt.lang.Array") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package com.google.gwt.lang;\n");
      code.append("import com.google.gwt.core.client.JavaScriptObject;\n");
      code.append("public final class Array {\n");
      code.append("  static void setCheck(Array array, int index, Object value) { }\n");
      code.append("  static void initDim(Class arrayClass, JavaScriptObject castableTypeMap, int queryId, int length, int seedType) { }\n");
      code.append("  static void initDims(Class arrayClasses[], JavaScriptObject[] castableTypeMapExprs, int[] queryIdExprs, int[] dimExprs, int count, int seedType) { }\n");
      code.append("  static void initValues(Class arrayClass, JavaScriptObject castableTypeMap, int queryId, Array array) { }\n");
      code.append("  public int length = 0;\n");
      code.append("  protected Class<?> arrayClass = null;\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource CAST = new MockJavaResource("com.google.gwt.lang.Cast") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package com.google.gwt.lang;\n");
      code.append("public final class Cast {\n");
      code.append("  public static Object dynamicCast(Object src, int dstId) { return src;}\n");
      code.append("  public static boolean instanceOf(Object src, int dstId) { return false;}\n");
      code.append("  public static native boolean isNull(Object a) /*-{ }-*/;\n");
      code.append("  public static native boolean isNotNull(Object a) /*-{ }-*/;\n");
      code.append("  public static native boolean jsEquals(Object a, Object b) /*-{ }-*/;\n");
      code.append("  public static native boolean jsNotEquals(Object a, Object b) /*-{ }-*/;\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource CLASS = new MockJavaResource("java.lang.Class") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
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
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package com.google.gwt.lang;\n");
      code.append("final class ClassLiteralHolder {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource ENUM = new MockJavaResource("java.lang.Enum") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package java.lang;\n");
      code.append("import java.io.Serializable;\n");
      code.append("import com.google.gwt.core.client.JavaScriptObject;\n");
      code.append("public abstract class Enum<E extends Enum<E>> implements Serializable {\n");
      code.append("  public static native <T extends Enum<T>> T valueOf(Class<T> enumType, String name) /*-{ }-*/;\n");
      code.append("  protected static native <T extends Enum<T>> JavaScriptObject createValueOfMap(T[] enumConstants) /*-{ }-*/;\n");
      code.append("  protected static native <T extends Enum<T>> T valueOf(JavaScriptObject map, String name) /*-{ }-*/;\n");
      code.append("  protected Enum(String name, int ordinal) { \n");
      code.append("    this.name = name;\n");
      code.append("    this.ordinal = ordinal;}\n");
      code.append("  private final String name;\n");
      code.append("  private final int ordinal;\n");
      code.append("  public final String name() { return name; }\n");
      code.append("  public final int ordinal() { return ordinal; }\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource GWT =
      new MockJavaResource("com.google.gwt.core.client.GWT") {
        @Override
        public CharSequence getContent() {
          StringBuilder code = new StringBuilder();
          code.append("package com.google.gwt.core.client;\n");
          code.append("public final class GWT {\n");
          code.append("  public static <T> T create(Class<?> classLiteral) { return null; }");
          code.append("  public static boolean isClient() { return true; };\n");
          code.append("  public static boolean isProdMode() { return true; };\n");
          code.append("  public static boolean isScript() { return true; };\n");
          code.append("  public static void runAsync(RunAsyncCallback callback) { }\n");
          code.append("  public static void runAsync(Class<?> name, RunAsyncCallback callback) { }\n");
          code.append("}\n");
          return code;
        }
      };
  public static final MockJavaResource RUNASYNCCALLBACK = new MockJavaResource(
      "com.google.gwt.core.client.RunAsyncCallback") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package com.google.gwt.core.client;\n");
      code.append("public interface RunAsyncCallback {\n");
      code.append("  void onSuccess();\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource RUNASYNCCODE = new MockJavaResource(
      "com.google.gwt.core.client.prefetch.RunAsyncCode") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package com.google.gwt.core.client.prefetch;\n");
      code.append("public class RunAsyncCode {\n");
      code.append("  public static RunAsyncCode runAsyncCode(Class<?> splitPoint) {\n");
      code.append("    return null;\n");
      code.append("  }");
      code.append("}");
      return code;
    }
  };
  public static final MockJavaResource STATS = new MockJavaResource("com.google.gwt.lang.Stats") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package com.google.gwt.lang;\n");
      code.append("public class Stats {\n");
      code.append("  static boolean isStatsAvailable() { return false; };\n");
      code.append("  static boolean onModuleStart(String mainClassName) { return false; }\n");
      code.append("}\n");
      return code;
    }
  };

  public static JProgram construct(TreeLogger logger, CompilationState state, String... entryPoints)
      throws UnableToCompleteException {
    JJSOptionsImpl options = new JJSOptionsImpl();
    options.setEnableAssertions(true);
    JProgram jprogram = AstConstructor.construct(logger, state, options);

    // Add entry methods for entry points.
    for (String entryPoint : entryPoints) {
      JDeclaredType entryType = jprogram.getFromTypeMap(entryPoint);
      for (JMethod method : entryType.getMethods()) {
        if (method.isStatic() && JProgram.isClinit(method)) {
          jprogram.addEntryMethod(method);
        }
      }
    }
    // Tree is now ready to optimize.
    return jprogram;
  }

  public static MockJavaResource[] getCompilerTypes() {
    List<MockJavaResource> result = new ArrayList<MockJavaResource>();
    Collections.addAll(result, JavaResourceBase.getStandardResources());
    // Replace the basic Class and Enum with a compiler-specific one.
    result.remove(JavaResourceBase.CLASS);
    result.remove(JavaResourceBase.ENUM);
    Collections.addAll(result, ASYNCFRAGMENTLOADER, ARRAY, CAST, CLASS, CLASSLITERALHOLDER, ENUM,
        GWT, RUNASYNCCALLBACK, RUNASYNCCODE);
    return result.toArray(new MockJavaResource[result.size()]);
  }
}
