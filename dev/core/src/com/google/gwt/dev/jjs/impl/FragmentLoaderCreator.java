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
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.cfg.ConfigurationProperty;
import com.google.gwt.dev.cfg.PublicOracle;
import com.google.gwt.dev.cfg.StaticPropertyOracle;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.shell.StandardGeneratorContext;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates code for loading an island. The pattern of generated classes is
 * more complicated than otherwise necessary so that the loader code is
 * precisely handled by TypeTightener and LivenessAnalyzer.
 * 
 * TODO(spoon) Remove this generator by making LivenessAnalyzer know about
 * runAsync and how it works.
 */
public class FragmentLoaderCreator {
  public static final String ASYNC_FRAGMENT_LOADER = "com.google.gwt.core.client.AsyncFragmentLoader";
  public static final String ASYNC_LOADER_CLASS_PREFIX = "AsyncLoader";
  public static final String ASYNC_LOADER_PACKAGE = "com.google.gwt.lang.asyncloaders";
  public static final String RUN_ASYNC_CALLBACK = "com.google.gwt.core.client.RunAsyncCallback";
  private static final String PROP_RUN_ASYNC_NEVER_RUNS = "gwt.jjs.runAsyncNeverRuns";

  private final ArtifactSet artifactSet;
  private final CompilationState compilationState;
  private int entryNumber;
  private final File genDir;
  private final File outDir;
  private final PublicOracle publicOracle;

  /**
   * Construct a FragmentLoaderCreator. The reason it needs so many parameters
   * is that it uses generator infrastructure.
   */
  public FragmentLoaderCreator(CompilationState compilationState,
      PublicOracle publicOracle, File genDir, File moduleOutDir,
      ArtifactSet artifactSet) {
    this.compilationState = compilationState;
    this.publicOracle = publicOracle;
    this.genDir = genDir;
    this.outDir = moduleOutDir;
    this.artifactSet = artifactSet;
  }

  public String create(TreeLogger logger) throws UnableToCompleteException {
    chooseEntryNumber();
    StandardGeneratorContext context = makeGeneratorContext();

    PrintWriter loaderWriter = getSourceWriterForLoader(logger, context);
    if (loaderWriter == null) {
      logger.log(TreeLogger.ERROR, "Failed to create island loader named "
          + getLoaderQualifiedName());
      throw new UnableToCompleteException();
    }

    generateLoaderFields(loaderWriter);
    generateOnErrorMethod(loaderWriter);
    generateOnLoadMethod(loaderWriter);
    generateRunAsyncMethod(loaderWriter);
    generateRunCallbacksMethod(loaderWriter);
    generateRunCallbackOnFailuresMethod(loaderWriter);

    loaderWriter.println("}");
    loaderWriter.close();
    context.commit(logger, loaderWriter);

    writeCallbackListClass(logger, context);
    writeLoaderSuperclass(logger, context);

    context.finish(logger);

    return getLoaderQualifiedName();
  }

  /**
   * Pick the lowest-numbered entry number that has not yet had loaders
   * generated.
   */
  private void chooseEntryNumber() {
    entryNumber = 1;
    while (compilationState.getTypeOracle().findType(getLoaderQualifiedName()) != null) {
      entryNumber++;
    }
  }

  private void generateLoaderFields(PrintWriter srcWriter) {
    srcWriter.println("private static boolean loaded = false;");
    srcWriter.println("private static boolean loading = false;");
    srcWriter.println("private static " + getLoaderSuperclassSimpleName()
        + " instance = new " + getLoaderSuperclassSimpleName() + "();");
    srcWriter.println("private static " + getCallbackListSimpleName()
        + " callbacks = null;");
  }

  private void generateOnErrorMethod(PrintWriter srcWriter) {
    srcWriter.println("public static void onError(Throwable e) {");
    srcWriter.println("loading = false;");
    srcWriter.println("runCallbackOnFailures(e);");
    srcWriter.println("}");
  }

  private void generateOnLoadMethod(PrintWriter srcWriter) {
    srcWriter.println("public static void onLoad() {");
    srcWriter.println(ASYNC_FRAGMENT_LOADER + ".logEventProgress(\"download"
        + entryNumber + "\", \"end\");");
    srcWriter.println("loaded = true;");
    srcWriter.println("instance = new " + getLoaderSimpleName() + "();");
    srcWriter.println(ASYNC_FRAGMENT_LOADER + ".fragmentHasLoaded("
        + entryNumber + ");");

    srcWriter.println("instance.runCallbacks();");

    srcWriter.println("}");
  }

  /**
   * Generate the <code>runAsync</code> method. Calls to
   * <code>GWT.runAsync</code> are replaced by calls to this method.
   */
  private void generateRunAsyncMethod(PrintWriter srcWriter) {
    srcWriter.println("public static void runAsync(RunAsyncCallback callback) {");
    srcWriter.println(getCallbackListSimpleName() + " newCallbackList = new "
        + getCallbackListSimpleName() + "();");
    srcWriter.println("newCallbackList.callback = callback;");
    srcWriter.println("newCallbackList.next = callbacks;");
    srcWriter.println("callbacks = newCallbackList;");
    srcWriter.println("if (loaded) {");
    srcWriter.println("instance.runCallbacks();");
    srcWriter.println("return;");
    srcWriter.println("}");
    srcWriter.println("if (!loading) {");
    srcWriter.println("loading = true;");
    srcWriter.println("AsyncFragmentLoader.inject(" + entryNumber + ");");
    srcWriter.println("}");
    srcWriter.println("}");
  }

  private void generateRunCallbackOnFailuresMethod(PrintWriter srcWriter) {
    srcWriter.println("private static void runCallbackOnFailures(Throwable e) {");
    // TODO(spoon): this runs the callbacks in reverse order
    srcWriter.println(getCallbackListSimpleName() + " callback = callbacks;");
    srcWriter.println("callbacks = null;");
    srcWriter.println("while (callback != null) {");
    srcWriter.println("callback.callback.onFailure(e);");
    srcWriter.println("callback = callback.next;");
    srcWriter.println("}");
    srcWriter.println("}");
  }

  private void generateRunCallbacksMethod(PrintWriter srcWriter) {
    srcWriter.println("public void runCallbacks() {");
    // TODO(spoon): this runs the callbacks in reverse order
    // TODO(spoon): this runs the callbacks immediately; deferred would be
    // better
    srcWriter.println(getCallbackListSimpleName() + " callback = callbacks;");
    srcWriter.println("callbacks = null;");
    if (!Boolean.getBoolean(PROP_RUN_ASYNC_NEVER_RUNS)) {
      srcWriter.println("while (callback != null) {");
      srcWriter.println("callback.callback.onSuccess();");
      srcWriter.println("callback = callback.next;");
      srcWriter.println("}");
    }
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

  private String getLoaderSuperclassQualifiedName() {
    return ASYNC_LOADER_PACKAGE + getLoaderSuperclassSimpleName();
  }

  private String getLoaderSuperclassSimpleName() {
    return getLoaderSimpleName() + "__Super";
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
        RUN_ASYNC_CALLBACK, List.class.getCanonicalName(),
        ArrayList.class.getCanonicalName(), ASYNC_FRAGMENT_LOADER};
    for (String imp : imports) {
      printWriter.println("import " + imp + ";");
    }

    printWriter.println("public class " + getLoaderSimpleName() + " extends "
        + getLoaderSuperclassSimpleName() + " {");

    return printWriter;
  }

  private StandardGeneratorContext makeGeneratorContext() {
    // An empty property oracle is fine, because fragment loaders aren't
    // affected by properties anyway
    PropertyOracle propOracle = new StaticPropertyOracle(
        new BindingProperty[0], new String[0], new ConfigurationProperty[0]);
    StandardGeneratorContext context = new StandardGeneratorContext(
        compilationState, propOracle, publicOracle, genDir, outDir, artifactSet);
    return context;
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

  /**
   * Create a stand-in superclass of the actual loader. This is used to keep the
   * liveness analyzer from thinking the real <code>runCallbacks()</code>
   * method is available until <code>onLoad</code> has been called and the
   * real loader instantiated. A little work on TypeTightener could prevent the
   * need for this class.
   */
  private void writeLoaderSuperclass(TreeLogger logger, GeneratorContext ctx)
      throws UnableToCompleteException {
    PrintWriter printWriter = ctx.tryCreate(logger, getPackage(),
        getLoaderSuperclassSimpleName());
    if (printWriter == null) {
      logger.log(TreeLogger.ERROR, "Could not create type: "
          + getLoaderSuperclassQualifiedName());
      throw new UnableToCompleteException();
    }

    printWriter.println("package " + getPackage() + ";");
    printWriter.println("public class " + getLoaderSuperclassSimpleName()
        + " {");
    printWriter.println("public void runCallbacks() { }");
    printWriter.println("}");

    printWriter.close();
    ctx.commit(logger, printWriter);
  }
}
