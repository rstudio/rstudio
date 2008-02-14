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
package com.google.gwt.dev.linker;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.About;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.Util;

/**
 * A Linker for producing a single JavaScript file from a GWT module. The use of
 * this Linker requires that the module has exactly one distinct compilation
 * result.
 */
public class SingleScriptLinker extends SelectionScriptLinker {
  public String getDescription() {
    return "Single Script";
  }

  /**
   * Guard against more than one CompilationResult and delegate to super-class.
   */
  @Override
  protected void doEmitArtifacts(TreeLogger logger, LinkerContext context)
      throws UnableToCompleteException {
    if (context.getCompilations().size() != 1) {
      logger = logger.branch(TreeLogger.ERROR,
          "The module must have exactly one distinct"
              + " permutation when using the " + getDescription() + " Linker.",
          null);

      int count = 0;
      for (CompilationResult result : context.getCompilations()) {
        logger.log(TreeLogger.INFO, "Permutation " + ++count + ": "
            + result.toString(), null);
      }

      throw new UnableToCompleteException();
    }
    super.doEmitArtifacts(logger, context);
  }

  /**
   * Emits the single compilation wrapped in an anonymous function block.
   */
  @Override
  protected void doEmitCompilation(TreeLogger logger, LinkerContext context,
      CompilationResult result) throws UnableToCompleteException {
  }

  @Override
  protected void emitSelectionScript(TreeLogger logger, LinkerContext context)
      throws UnableToCompleteException {

    DefaultTextOutput out = new DefaultTextOutput(true);

    // Emit the selection script in a function closure.
    out.print("(function () {");
    out.newlineOpt();
    String bootstrap = generateSelectionScript(logger, context);
    bootstrap = context.optimizeJavaScript(logger, bootstrap);
    out.print(bootstrap);
    out.print("})();");
    out.newlineOpt();

    // Emit the module's JS in another closure
    out.print("(function () {");
    out.newlineOpt();
    out.print("var $gwt_version = \"" + About.GWT_VERSION_NUM + "\";");
    out.newlineOpt();
    out.print("var $wnd = window;");
    out.newlineOpt();
    out.print("var $doc = $wnd.document;");
    out.newlineOpt();
    out.print("var $moduleName, $moduleBase;");
    out.newlineOpt();

    CompilationResult result = context.getCompilations().first();
    out.print(result.getJavaScript());

    // Add a callback to the selection script
    out.newlineOpt();
    out.print("if (" + context.getModuleFunctionName() + ") {");
    out.newlineOpt();
    out.print("  var __gwt_initHandlers = " + context.getModuleFunctionName()
        + ".__gwt_initHandlers;");
    out.print("  " + context.getModuleFunctionName()
        + ".onScriptLoad(gwtOnLoad);");
    out.newlineOpt();
    out.print("}");
    out.newlineOpt();
    out.print("})();");
    out.newlineOpt();

    byte[] selectionScriptBytes = Util.getBytes(out.toString());
    doEmit(logger, context, selectionScriptBytes, context.getModuleName()
        + ".nocache.js");
  }

  /**
   * Unimplemented. Normally required by
   * {@link #doEmitCompilation(TreeLogger, LinkerContext, CompilationResult).
   */
  @Override
  protected String getCompilationExtension(TreeLogger logger,
      LinkerContext context) throws UnableToCompleteException {
    throw new UnableToCompleteException();
  }

  /**
   * Unimplemented. Normally required by
   * {@link #doEmitCompilation(TreeLogger, LinkerContext, CompilationResult).
   */
  @Override
  protected String getModulePrefix(TreeLogger logger, LinkerContext context)
      throws UnableToCompleteException {
    throw new UnableToCompleteException();
  }

  /**
   * Unimplemented. Normally required by
   * {@link #doEmitCompilation(TreeLogger, LinkerContext, CompilationResult).
   */
  @Override
  protected String getModuleSuffix(TreeLogger logger, LinkerContext context)
      throws UnableToCompleteException {
    throw new UnableToCompleteException();
  }

  @Override
  protected String getSelectionScriptTemplate(TreeLogger logger,
      LinkerContext context) throws UnableToCompleteException {
    return "com/google/gwt/dev/linker/SSOTemplate.js";
  }
}
