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
import com.google.gwt.dev.PrecompileTaskOptions;
import com.google.gwt.dev.cfg.Properties;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.testing.impl.JavaResourceBase;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.thirdparty.guava.common.base.Joiner;

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
      return Joiner.on("\n").join(
          "package com.google.gwt.core.client.impl;",
          "import com.google.gwt.core.client.RunAsyncCallback;",
          "public class AsyncFragmentLoader {",
          "  public static void onLoad(int fragment) { }",
          "  public static void runAsync(int fragment, RunAsyncCallback callback) { }",
          "  public static AsyncFragmentLoader BROWSER_LOADER = makeBrowserLoader(1, new int[]{});",
          "  private static AsyncFragmentLoader makeBrowserLoader(int numSp, int[] initial) {",
          "    return null;",
          "  }",
          "}");
    }
  };

  public static final MockJavaResource ARRAY = new MockJavaResource("com.google.gwt.lang.Array") {
    @Override
    public CharSequence getContent() {
      return Joiner.on("\n").join(
          "package com.google.gwt.lang;",
          "import com.google.gwt.core.client.JavaScriptObject;",
          "public final class Array {",
          "  static void setCheck(Array array, int index, Object value) { }",
          "  static void initDim(Class arrayClass, JavaScriptObject castableTypeMap,",
          "      int elementTypeId, int elementTypeClass, int length) { }",
          "  static void initDims(Class arrayClasses[], JavaScriptObject[] castableTypeMapExprs,",
          "      int[] elementTypeIds, int leafElementTypeClass, int[] dimExprs, int count) { }",
          "  static void initValues(Class arrayClass, JavaScriptObject castableTypeMap,",
          "      int elementTypeId, int elementTypeClass, Array array) { }",
          "}"
      );
    }
  };

  public static final MockJavaResource CAST = new MockJavaResource("com.google.gwt.lang.Cast") {
    @Override
    public CharSequence getContent() {
      return Joiner.on("\n").join(
          "package com.google.gwt.lang;",
          "import com.google.gwt.core.client.JavaScriptObject;",
          "public final class Cast {",
          "  private static JavaScriptObject stringCastMap;",
          "  public static native String charToString(char x) /*-{ }-*/;",
          "  public static Object dynamicCast(Object src, int dstId) { return src;}",
          "  public static boolean instanceOf(Object src, int dstId) { return false;}",
          "  public static boolean hasJavaObjectVirtualDispatch(Object o) { return true; }",
          "  public static boolean isJavaArray(Object o) { return false; }",
          "  public static boolean isJavaString(Object o) { return true; }",
          "  public static boolean isJavaScriptObject(Object o) { return true; }",
          "  public static native boolean isNull(Object a) /*-{ }-*/;",
          "  public static native boolean isNotNull(Object a) /*-{ }-*/;",
          "  public static native boolean jsEquals(Object a, Object b) /*-{ }-*/;",
          "  public static native boolean jsNotEquals(Object a, Object b) /*-{ }-*/;",
          "  public static int narrow_int(double x) { return 0; }",
          "  public static byte narrow_long(double x) { return 0; }",
          "}"
      );
    }
  };

  public static final MockJavaResource CLASS = new MockJavaResource("java.lang.Class") {
    @Override
    public CharSequence getContent() {
      return Joiner.on("\n").join(
          "package java.lang;",
          "import com.google.gwt.core.client.JavaScriptObject;",
          "public final class Class<T> {",
          "  static <T> Class<T> createForArray(String packageName, String className,",
          "      String typeId, Class<?> componentType) { return new Class<T>(); }",
          "  static <T> Class<T> createForClass(String packageName, String className,",
          "      String typeId, Class<? super T> superclass) { return new Class<T>(); }",
          "  static <T> Class<T> createForEnum(String packageName, String className,",
          "      String typeId, Class<? super T> superclass, JavaScriptObject enumConstantsFunc,",
          "      JavaScriptObject enumValueOfFunc) { return new Class<T>(); }",
          "  static <T> Class<T> createForInterface(String packageName, String className) {",
          "    return new Class<T>(); }",
          "  static <T> Class<T> createForPrimitive(String packageName, String className,",
          "      String jni) { return new Class<T>(); }",
          "  static boolean isClassMetadataEnabled() { return true; }",
          "  public boolean desiredAssertionStatus() { return true; }",
          "  public String getName() { return null; }",
          "}"
      );
    }
  };

  public static final MockJavaResource CLASSLITERALHOLDER = new MockJavaResource(
      "com.google.gwt.lang.ClassLiteralHolder") {
    @Override
    public CharSequence getContent() {
      return Joiner.on("\n").join(
          "package com.google.gwt.lang;",
          "final class ClassLiteralHolder {",
          "}"
      );
    }
  };

  public static final MockJavaResource COLLAPSED_PROPERTY_HOLDER =
      new MockJavaResource("com.google.gwt.lang.CollapsedPropertyHolder") {
        @Override
        public CharSequence getContent() {
          return Joiner.on("\n").join(
              "package com.google.gwt.lang; public class CollapsedPropertyHolder {",
              "  public static int permutationId = -1;",
              "}"
          );
        }
      };


  public static final MockJavaResource ENUM = new MockJavaResource("java.lang.Enum") {
    @Override
    public CharSequence getContent() {
      return Joiner.on("\n").join(
          "package java.lang;",
          "import java.io.Serializable;",
          "import com.google.gwt.core.client.JavaScriptObject;",
          "public abstract class Enum<E extends Enum<E>> implements Serializable {",
          "  public static native <T extends Enum<T>> T valueOf(Class<T> enumType,",
          "      String name) /*-{ }-*/;",
          "  protected static native <T extends Enum<T>> JavaScriptObject createValueOfMap(",
          "      T[] enumConstants) /*-{ }-*/;",
          "  protected static native <T extends Enum<T>> T valueOf(JavaScriptObject map,",
          "      String name) /*-{ }-*/;",
          "  protected Enum(String name, int ordinal) { ", "    this.name = name;",
          "    this.ordinal = ordinal;}", "  private final String name;",
          "  private final int ordinal;", "  public final String name() { return name; }",
          "  public final int ordinal() { return ordinal; }",
          "}"
      );
    }
  };

  public static final MockJavaResource EXCEPTIONS = new MockJavaResource(
      "com.google.gwt.lang.Exceptions") {
    @Override
    public CharSequence getContent() {
      return Joiner.on("\n").join(
          "package com.google.gwt.lang;",
          "public class Exceptions { ",
          "  static Object wrap(Object e) { return e; }",
          "  static RuntimeException makeAssertionError() { return new RuntimeException(); }",
          "  static Throwable safeClose(AutoCloseable resource, Throwable mainException) {",
          "    return mainException;", "  }",
          "  static <T> T checkNotNull(T value) { return value; }",
          "}"
      );
    }
  };

  public static final MockJavaResource GWT = new MockJavaResource(
      "com.google.gwt.core.client.GWT") {
    @Override
    public CharSequence getContent() {
      return Joiner.on("\n").join(
          "package com.google.gwt.core.client;",
          "public final class GWT {",
          "  public static <T> T create(Class<?> classLiteral) { return null; }",
          "  public static boolean isClient() { return true; };",
          "  public static boolean isProdMode() { return true; };",
          "  public static boolean isScript() { return true; };",
          "  public static void runAsync(RunAsyncCallback callback) { }",
          "  public static void runAsync(Class<?> name, RunAsyncCallback callback) { }",
          "}"
      );
    }
  };
  public static final MockJavaResource GWT_SHARED = new MockJavaResource(
      "com.google.gwt.core.shared.GWT") {
    @Override
    public CharSequence getContent() {
      return Joiner.on("\n").join(
          "package com.google.gwt.core.shared;",
          "public final class GWT {",
          "  public static <T> T create(Class<?> classLiteral) { return null; }",
          "  public static boolean isClient() { return true; };",
          "  public static boolean isProdMode() { return true; };",
          "  public static boolean isScript() { return true; };",
          "  public static void debugger() { }",
          "}"
      );
    }
  };

  public static final MockJavaResource IMPL =
      new MockJavaResource("com.google.gwt.core.client.impl.Impl") {
        @Override
        public CharSequence getContent() {
          return Joiner.on("\n").join(
              "package com.google.gwt.core.client.impl;",
              "public class Impl {",
              "  public static Object registerEntry(){return null;}",
              "  public static String getNameOf(String jsniIdent) { return null; }",
              "}"
          );
        }
      };

  public static final MockJavaResource JAVA_CLASS_HIERARCHY_SETUP_UTIL =
      new MockJavaResource("com.google.gwt.lang.JavaClassHierarchySetupUtil") {
        @Override
        public CharSequence getContent() {
          return Joiner.on("\n").join(
              "package com.google.gwt.lang;",
              "public class JavaClassHierarchySetupUtil {",
              "  public static Object defineClass(int typeId, int superTypeId, Object map) {",
              "    return null;",
              "  }",
              " public static Object defineClassWithPrototype(int typeId, "
                  + "int superTypeId, ",
              " Object map) {",
              "    return null;",
              "  }",
              "  public static void modernizeBrowser() {}",
              "}"
          );
        }
      };

  public static final MockJavaResource LONGLIB = new MockJavaResource(
      "com.google.gwt.lang.LongLib") {
    @Override
    public CharSequence getContent() {
      return Joiner.on("\n").join(
          "package com.google.gwt.lang;",
          "public final class LongLib {",
          "  public static String toString(long a) { return \"\";}",
          "}"
      );
    }
  };
  public static final MockJavaResource RUNASYNCCALLBACK = new MockJavaResource(
      "com.google.gwt.core.client.RunAsyncCallback") {
    @Override
    public CharSequence getContent() {
      return Joiner.on("\n").join(
          "package com.google.gwt.core.client;",
          "public interface RunAsyncCallback {",
          "  void onSuccess();",
          "  void onFailure(Throwable reason);",
          "}"
      );
    }
  };

  public static final MockJavaResource RUNASYNCCODE = new MockJavaResource(
      "com.google.gwt.core.client.prefetch.RunAsyncCode") {
    @Override
    public CharSequence getContent() {
      return Joiner.on("\n").join(
          "package com.google.gwt.core.client.prefetch;",
          "public class RunAsyncCode {",
          "  public static RunAsyncCode runAsyncCode(Class<?> splitPoint) {",
          "    return null;",
          "  }",
          "}"
      );
    }
  };

  public static final MockJavaResource STATS = new MockJavaResource("com.google.gwt.lang.Stats") {
    @Override
    public CharSequence getContent() {
      return Joiner.on("\n").join(
          "package com.google.gwt.lang;",
          "public class Stats {",
          "  static boolean isStatsAvailable() { return false; };",
          "  static boolean onModuleStart(String mainClassName) { return false; }",
          "}"
      );
    }
  };

  public static JProgram construct(TreeLogger logger, CompilationState state,
      PrecompileTaskOptions options, Properties properties,
      String... entryPoints) throws UnableToCompleteException {
    options.setEnableAssertions(true);
    JProgram jprogram = AstConstructor.construct(logger, state, options, properties);

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
    Collections.addAll(result, ASYNCFRAGMENTLOADER, ARRAY, CAST, CLASS, CLASSLITERALHOLDER,
        COLLAPSED_PROPERTY_HOLDER, ENUM, EXCEPTIONS, GWT, GWT_SHARED, IMPL,
        JAVA_CLASS_HIERARCHY_SETUP_UTIL, LONGLIB, RUNASYNCCALLBACK, RUNASYNCCODE);
    return result.toArray(new MockJavaResource[result.size()]);
  }
}
