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
package com.google.gwt.core.linker;

import com.google.gwt.core.ext.LinkerContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.CompilationResult;
import com.google.gwt.core.ext.linker.EmittedArtifact;
import com.google.gwt.core.ext.linker.LinkerOrder;
import com.google.gwt.core.ext.linker.LinkerOrder.Order;
import com.google.gwt.core.ext.linker.impl.SelectionScriptLinker;
import com.google.gwt.dev.About;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.Util;

import java.util.Set;

/**
 * A Linker for producing a single JavaScript file from a GWT module. The use of
 * this Linker requires that the module has exactly one distinct compilation
 * result.
 */
@LinkerOrder(Order.PRIMARY)
public class SingleScriptLinker extends SelectionScriptLinker {
  public String getDescription() {
    return "Single Script";
  }

  @Override
  public ArtifactSet link(TreeLogger logger, LinkerContext context,
      ArtifactSet artifacts) throws UnableToCompleteException {
    ArtifactSet toReturn = new ArtifactSet(artifacts);

    toReturn.add(emitSelectionScript(logger, context, artifacts));

    return toReturn;
  }

  @Override
  protected EmittedArtifact emitSelectionScript(TreeLogger logger,
      LinkerContext context, ArtifactSet artifacts)
      throws UnableToCompleteException {

    DefaultTextOutput out = new DefaultTextOutput(true);

    // Emit the selection script in a function closure.
    out.print("(function () {");
    out.newlineOpt();
    String bootstrap = generateSelectionScript(logger, context, artifacts);
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
    out.print("var $stats = $wnd.__gwtstatsEvent ? function(a,b,c,d) {$wnd.__gwtstatsEvent(a,b,c,d)} : null;");
    out.newlineOpt();

    // Find the single CompilationResult
    Set<CompilationResult> results = artifacts.find(CompilationResult.class);
    if (results.size() != 1) {
      logger = logger.branch(TreeLogger.ERROR,
          "The module must have exactly one distinct"
              + " permutation when using the " + getDescription() + " Linker.",
          null);
      throw new UnableToCompleteException();
    }
    CompilationResult result = results.iterator().next();

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
    return emitBytes(logger, selectionScriptBytes, context.getModuleName()
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
    return "com/google/gwt/core/linker/SingleScriptTemplate.js";
  }
}
