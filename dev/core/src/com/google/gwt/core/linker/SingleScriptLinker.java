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
import com.google.gwt.core.ext.linker.Artifact;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.CompilationResult;
import com.google.gwt.core.ext.linker.EmittedArtifact;
import com.google.gwt.core.ext.linker.LinkerOrder;
import com.google.gwt.core.ext.linker.Shardable;
import com.google.gwt.core.ext.linker.LinkerOrder.Order;
import com.google.gwt.core.ext.linker.impl.SelectionScriptLinker;
import com.google.gwt.dev.About;
import com.google.gwt.dev.util.DefaultTextOutput;

import java.util.Collection;
import java.util.Set;

/**
 * A Linker for producing a single JavaScript file from a GWT module. The use of
 * this Linker requires that the module has exactly one distinct compilation
 * result.
 */
@LinkerOrder(Order.PRIMARY)
@Shardable
public class SingleScriptLinker extends SelectionScriptLinker {
  @Override
  public String getDescription() {
    return "Single Script";
  }

  @Override
  public ArtifactSet link(TreeLogger logger, LinkerContext context,
      ArtifactSet artifacts, boolean onePermutation)
      throws UnableToCompleteException {
    if (onePermutation) {
      processSelectionInformation(artifacts);
      ArtifactSet toReturn = new ArtifactSet(artifacts);
      toReturn.add(emitSelectionScript(logger, context, artifacts));
      return toReturn;
    } else {
      return artifacts;
    }
  }

  @Override
  protected Collection<Artifact<?>> doEmitCompilation(TreeLogger logger,
      LinkerContext context, CompilationResult result)
      throws UnableToCompleteException {
    if (result.getJavaScript().length != 1) {
      logger.branch(TreeLogger.ERROR,
          "The module must not have multiple fragments when using the "
              + getDescription() + " Linker.", null);
      throw new UnableToCompleteException();
    }
    return super.doEmitCompilation(logger, context, result);
  }

  @Override
  protected EmittedArtifact emitSelectionScript(TreeLogger logger,
      LinkerContext context, ArtifactSet artifacts)
      throws UnableToCompleteException {

    DefaultTextOutput out = new DefaultTextOutput(true);
    
    // Emit the selection script.
    String bootstrap = generateSelectionScript(logger, context, artifacts);
    bootstrap = context.optimizeJavaScript(logger, bootstrap);
    out.print(bootstrap);
    out.newlineOpt();

    // Emit the module's JS a closure.
    out.print("(function () {");
    out.newlineOpt();
    out.print("var $gwt_version = \"" + About.getGwtVersionNum() + "\";");
    out.newlineOpt();
    out.print("var $wnd = window;");
    out.newlineOpt();
    out.print("var $doc = $wnd.document;");
    out.newlineOpt();
    out.print("var $moduleName, $moduleBase;");
    out.newlineOpt();
    out.print("var $stats = $wnd.__gwtStatsEvent ? function(a) {$wnd.__gwtStatsEvent(a)} : null;");
    out.newlineOpt();

    // Find the single CompilationResult
    Set<CompilationResult> results = artifacts.find(CompilationResult.class);
    if (results.size() != 1) {
      logger.log(TreeLogger.ERROR, "The module must have exactly one distinct"
          + " permutation when using the " + getDescription() + " Linker.",
          null);
      throw new UnableToCompleteException();
    }
    CompilationResult result = results.iterator().next();

    out.print("var $strongName = '" + result.getStrongName() + "';");
    out.newlineOpt();

    String[] js = result.getJavaScript();
    if (js.length != 1) {
      logger.log(TreeLogger.ERROR,
          "The module must not have multiple fragments when using the "
              + getDescription() + " Linker.", null);
      throw new UnableToCompleteException();
    }
    out.print(js[0]);

    // Generate the call to tell the bootstrap code that we're ready to go.
    out.newlineOpt();
    out.print("if (" + context.getModuleFunctionName() + ") "
        + context.getModuleFunctionName() + ".onScriptLoad(gwtOnLoad);");
    out.newlineOpt();
    out.print("})();");
    out.newlineOpt();

    return emitString(logger, out.toString(), context.getModuleName()
        + ".nocache.js");
  }

  /**
   * Unimplemented. Normally required by
   * {@link #doEmitCompilation(TreeLogger, LinkerContext, CompilationResult)}.
   */
  @Override
  protected String getCompilationExtension(TreeLogger logger,
      LinkerContext context) throws UnableToCompleteException {
    throw new UnableToCompleteException();
  }

  /**
   * Unimplemented. Normally required by
   * {@link #doEmitCompilation(TreeLogger, LinkerContext, CompilationResult)}.
   */
  @Override
  protected String getModulePrefix(TreeLogger logger, LinkerContext context,
      String strongName) throws UnableToCompleteException {
    throw new UnableToCompleteException();
  }

  /**
   * Unimplemented. Normally required by
   * {@link #doEmitCompilation(TreeLogger, LinkerContext, CompilationResult)}.
   */
  @Override
  protected String getModuleSuffix(TreeLogger logger, LinkerContext context)
      throws UnableToCompleteException {
    throw new UnableToCompleteException();
  }

  @Override
  protected String getSelectionScriptTemplate(TreeLogger logger, LinkerContext context)
      throws UnableToCompleteException {
    return "com/google/gwt/core/linker/SingleScriptTemplate.js";
  }
}
