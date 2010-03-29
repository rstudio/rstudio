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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.cfg.ConfigurationProperty;
import com.google.gwt.dev.cfg.StaticPropertyOracle;
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.dev.jdt.FindDeferredBindingSitesVisitor;

import java.io.PrintWriter;

/**
 * Generates code for loading an island. The pattern of generated classes is
 * more complicated than otherwise necessary so that the loader code is
 * precisely handled by TypeTightener and LivenessAnalyzer.
 * 
 * TODO(spoon) Remove this generator by making LivenessAnalyzer know about
 * runAsync and how it works.
 */
public class FragmentLoaderCreator {
  public static final String ASYNC_FRAGMENT_LOADER = "com.google.gwt.core.client.impl.AsyncFragmentLoader";
  public static final String ASYNC_LOADER_CLASS_PREFIX = "AsyncLoader";
  public static final String ASYNC_LOADER_PACKAGE = "com.google.gwt.lang.asyncloaders";
  public static final String BROWSER_LOADER = "AsyncFragmentLoader.BROWSER_LOADER";
  public static final String LOADER_METHOD_RUN_ASYNC = "runAsync";
  public static final String RUN_ASYNC_CALLBACK = "com.google.gwt.core.client.RunAsyncCallback";
  private static final String GWT_CLASS = FindDeferredBindingSitesVisitor.MAGIC_CLASS;
  private static final String PROP_RUN_ASYNC_NEVER_RUNS = "gwt.jjs.runAsyncNeverRuns";
  private static final String UNCAUGHT_EXCEPTION_HANDLER_CLASS = "GWT.UncaughtExceptionHandler";

  private final StandardGeneratorContext context;
  private int entryNumber = 0;
  private final PropertyOracle propOracle;

  /**
   * Construct a FragmentLoaderCreator. The reason it needs so many parameters
   * is that it uses generator infrastructure.
   */
  public FragmentLoaderCreator(StandardGeneratorContext context) {
    // An empty property oracle is fine, because fragment loaders aren't
    // affected by properties anyway
    this.propOracle = new StaticPropertyOracle(new BindingProperty[0],
        new String[0], new ConfigurationProperty[0]);
    this.context = context;
  }

  public String create(TreeLogger logger) throws UnableToCompleteException {
    // First entry is 1.
    ++entryNumber;
    context.setPropertyOracle(propOracle);
    PrintWriter loaderWriter = getSourceWriterForLoader(logger, context);
    if (loaderWriter == null) {
      logger.log(TreeLogger.ERROR, "Failed to create island loader named "
          + getLoaderQualifiedName());
      throw new UnableToCompleteException();
    }

    generateLoaderFields(loaderWriter);
    generateOnLoadMethod(loaderWriter);
    generateRunAsyncMethod(loaderWriter);
    generateRunCallbacksMethod(loaderWriter);
    generateRunCallbackOnFailuresMethod(loaderWriter);

    loaderWriter.println("}");
    loaderWriter.close();
    context.commit(logger, loaderWriter);

    writeCallbackListClass(logger, context);

    return getLoaderQualifiedName();
  }

  private void generateLoaderFields(PrintWriter srcWriter) {
    srcWriter.println("// Callbacks that are pending");
    srcWriter.println("private static " + getCallbackListSimpleName()
        + " callbacksHead = null;");

    srcWriter.println("// The tail of the callbacks list");
    srcWriter.println("private static " + getCallbackListSimpleName()
        + " callbacksTail = null;");

    srcWriter.println("// A callback caller for this entry point");
    srcWriter.println("private static " + getLoaderSimpleName()
        + " instance = null;");
  }

  private void generateOnLoadMethod(PrintWriter srcWriter) {
    srcWriter.println("public static void onLoad() {");
    srcWriter.println("instance = new " + getLoaderSimpleName() + "();");
    srcWriter.println(BROWSER_LOADER + ".fragmentHasLoaded(" + entryNumber
        + ");");

    srcWriter.println(BROWSER_LOADER + ".logEventProgress(\"runCallbacks"
        + entryNumber + "\", \"begin\");");
    srcWriter.println("instance.runCallbacks();");
    srcWriter.println(BROWSER_LOADER + ".logEventProgress(\"runCallbacks"
        + entryNumber + "\", \"end\");");

    srcWriter.println("}");
  }

  /**
   * Generate the <code>runAsync</code> method. Calls to
   * <code>GWT.runAsync</code> are replaced by calls to this method.
   */
  private void generateRunAsyncMethod(PrintWriter srcWriter) {
    srcWriter.println("public static void " + LOADER_METHOD_RUN_ASYNC
        + "(RunAsyncCallback callback) {");
    srcWriter.println(getCallbackListSimpleName() + " newCallback = new "
        + getCallbackListSimpleName() + "();");
    srcWriter.println("newCallback.callback = callback;");

    srcWriter.println("if (callbacksTail != null) {");
    srcWriter.println("  callbacksTail.next = newCallback;");
    srcWriter.println("}");

    srcWriter.println("callbacksTail = newCallback;");
    srcWriter.println("if (callbacksHead == null) {");
    srcWriter.println("  callbacksHead = newCallback;");
    srcWriter.println("}");

    srcWriter.println("if (instance != null) {");
    srcWriter.println("  instance.runCallbacks();");
    srcWriter.println("  return;");
    srcWriter.println("}");
    srcWriter.println("if (!" + BROWSER_LOADER + ".isLoading(" + entryNumber
        + ")) {");
    srcWriter.println("  " + BROWSER_LOADER + ".inject(" + entryNumber + ",");
    srcWriter.println("  new AsyncFragmentLoader.LoadTerminatedHandler() {");
    srcWriter.println("    public void loadTerminated(Throwable reason) {");
    srcWriter.println("      runCallbackOnFailures(reason);");
    srcWriter.println("    }");
    srcWriter.println("  });");
    srcWriter.println("}");
    srcWriter.println("}");
  }

  private void generateRunCallbackOnFailuresMethod(PrintWriter srcWriter) {
    srcWriter.println("private static void runCallbackOnFailures(Throwable e) {");
    srcWriter.println("while (callbacksHead != null) {");
    srcWriter.println("  callbacksHead.callback.onFailure(e);");
    srcWriter.println("  callbacksHead = callbacksHead.next;");
    srcWriter.println("}");
    srcWriter.println("callbacksTail = null;");
    srcWriter.println("}");
  }

  private void generateRunCallbacksMethod(PrintWriter srcWriter) {
    srcWriter.println("public void runCallbacks() {");

    srcWriter.println("while (callbacksHead != null) {");

    srcWriter.println("  " + UNCAUGHT_EXCEPTION_HANDLER_CLASS + " handler = "
        + "GWT.getUncaughtExceptionHandler();");

    srcWriter.println("  " + getCallbackListSimpleName()
        + " next = callbacksHead;");
    srcWriter.println("  callbacksHead = callbacksHead.next;");
    srcWriter.println("  if (callbacksHead == null) {");
    srcWriter.println("    callbacksTail = null;");
    srcWriter.println("  }");

    if (!Boolean.getBoolean(PROP_RUN_ASYNC_NEVER_RUNS)) {
      // TODO(spoon): this runs the callbacks immediately; deferred would be
      // better
      srcWriter.println("  if (handler == null) {");
      srcWriter.println("    next.callback.onSuccess();");
      srcWriter.println("  } else {");
      srcWriter.println("    try {");
      srcWriter.println("      next.callback.onSuccess();");
      srcWriter.println("    } catch (Throwable e) {");
      srcWriter.println("      handler.onUncaughtException(e);");
      srcWriter.println("    }");
      srcWriter.println("  }");
    }

    srcWriter.println("}");
    srcWriter.println("}");
  }

  private String getCallbackListQualifiedName() {
    return ASYNC_LOADER_PACKAGE + "__Callback";
  }

  private String getCallbackListSimpleName() {
    return getLoaderSimpleName() + "__Callback";
  }

  private String getLoaderQualifiedName() {
    return ASYNC_LOADER_PACKAGE + "." + getLoaderSimpleName();
  }

  private String getLoaderSimpleName() {
    return ASYNC_LOADER_CLASS_PREFIX + entryNumber;
  }

  private String getPackage() {
    return ASYNC_LOADER_PACKAGE;
  }

  private PrintWriter getSourceWriterForLoader(TreeLogger logger,
      GeneratorContext ctx) {
    PrintWriter printWriter = ctx.tryCreate(logger, getPackage(),
        getLoaderSimpleName());
    if (printWriter == null) {
      return null;
    }

    printWriter.println("package " + getPackage() + ";");
    String[] imports = new String[] {
        GWT_CLASS, RUN_ASYNC_CALLBACK, ASYNC_FRAGMENT_LOADER};
    for (String imp : imports) {
      printWriter.println("import " + imp + ";");
    }

    printWriter.println("public class " + getLoaderSimpleName() + " {");
    return printWriter;
  }

  private void writeCallbackListClass(TreeLogger logger, GeneratorContext ctx)
      throws UnableToCompleteException {
    PrintWriter printWriter = ctx.tryCreate(logger, getPackage(),
        getCallbackListSimpleName());
    if (printWriter == null) {
      logger.log(TreeLogger.ERROR, "Could not create type: "
          + getCallbackListQualifiedName());
      throw new UnableToCompleteException();
    }

    printWriter.println("package " + getPackage() + ";");
    printWriter.println("public class " + getCallbackListSimpleName() + "{");
    printWriter.println(RUN_ASYNC_CALLBACK + " callback;");
    printWriter.println(getCallbackListSimpleName() + " next;");
    printWriter.println("}");

    printWriter.close();
    ctx.commit(logger, printWriter);
  }
}
