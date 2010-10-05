/*
 * Copyright 2010 Google Inc.
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
import com.google.gwt.core.ext.linker.LinkerOrder;
import com.google.gwt.core.ext.linker.Shardable;
import com.google.gwt.core.ext.linker.LinkerOrder.Order;
import com.google.gwt.core.ext.linker.impl.SelectionScriptLinker;
import com.google.gwt.dev.About;
import com.google.gwt.dev.js.JsToStringGenerationVisitor;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.TextOutput;
import com.google.gwt.dev.util.Util;
import com.google.gwt.util.tools.Utility;

import java.io.IOException;

/**
 * This linker uses an iframe to hold the code and a script tag to download the
 * code. It can download code cross-site, because it uses a script tag to
 * download it and because it never uses XHR. The iframe, meanwhile, makes it
 * trivial to install additional code as the app runs.
 */
@LinkerOrder(Order.PRIMARY)
@Shardable
public class CrossSiteIframeLinker extends SelectionScriptLinker {
  // TODO(unnurg): For each of the following properties, decide whether to make
  // it a gwt.xml configuration property, a constant which can be overridden
  // by subclasses, or not configurable at all.
  private static final String installLocationJsProperty = 
    "com/google/gwt/core/ext/linker/impl/installLocationIframe.js";
  private static final boolean processMetasProperty = true;
  private static final String scriptBaseProperty = "";
  private static final boolean startDownloadImmediatelyProperty = true;
  
  private static final String WAIT_FOR_BODY_LOADED_JS =
    "com/google/gwt/core/ext/linker/impl/waitForBodyLoaded.js";
  
  private static final boolean waitForBodyLoadedProperty = true;

  @Override
  public String getDescription() {
    return "Cross-Site-Iframe";
  }

  @Override
  protected byte[] generatePrimaryFragment(TreeLogger logger,
      LinkerContext context, CompilationResult result, String[] js) {
    TextOutput script = new DefaultTextOutput(context.isOutputCompact());
    script.print(getModulePrefix(context, result.getStrongName()));
    script.print(js[0]);
    script.print(getModuleSuffix(logger, context));
    StringBuffer out = new StringBuffer();

    if (startDownloadImmediatelyProperty) {
      // Rewrite the code so it can be installed with
      // __MODULE_FUNC__.onScriptDownloaded
      out.append(context.getModuleFunctionName());
      out.append(".onScriptDownloaded(");
      out.append(JsToStringGenerationVisitor.javaScriptString(script.toString()));
      out.append(")");
    } else {
      out.append(script.toString());
    }

    return Util.getBytes(out.toString());
  }
  
  @Override
  protected String generateSelectionScript(TreeLogger logger,
      LinkerContext context, ArtifactSet artifacts) throws UnableToCompleteException {
    StringBuffer selectionScript = getSelectionScriptStringBuffer(logger, context);
    
    String waitForBodyLoadedJs;
    String installLocationJs;
    String computeScriptBase;
    String processMetas;

    try {
      waitForBodyLoadedJs = Utility.getFileFromClassPath(WAIT_FOR_BODY_LOADED_JS);
      processMetas = Utility.getFileFromClassPath(PROCESS_METAS_JS);
      installLocationJs = Utility.getFileFromClassPath(installLocationJsProperty);
      computeScriptBase = Utility.getFileFromClassPath(COMPUTE_SCRIPT_BASE_JS);
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Unable to read selection script template",
          e);
      throw new UnableToCompleteException();
    }
    
    replaceAll(selectionScript, "__INSTALL_LOCATION__", installLocationJs);

    if (waitForBodyLoadedProperty) {
      replaceAll(selectionScript, "__WAIT_FOR_BODY_LOADED__", waitForBodyLoadedJs);
    } else {
      String waitForBodyLoadedNullImpl = 
      "function setupWaitForBodyLoad(callback) { callback(); }";
      replaceAll(selectionScript, "__WAIT_FOR_BODY_LOADED__", waitForBodyLoadedNullImpl);
    }

    if (startDownloadImmediatelyProperty) {
      replaceAll(selectionScript, "__START_DOWNLOAD_IMMEDIATELY__", "true");
    } else {
      replaceAll(selectionScript, "__START_DOWNLOAD_IMMEDIATELY__", "false");
    }
    
    if (processMetasProperty) {
      replaceAll(selectionScript, "__PROCESS_METAS__", processMetas);
    } else {
      String processMetasNullImpl =
        "function processMetas() { }";
      replaceAll(selectionScript, "__PROCESS_METAS__", processMetasNullImpl);
    }
    
    String scriptBase = scriptBaseProperty;
    if ("".equals(scriptBase)) {
      replaceAll(selectionScript, "__COMPUTE_SCRIPT_BASE__", computeScriptBase);
    } else {
      String computeScriptBaseNullImpl =
        "function computeScriptBase() { return '" + scriptBase + "';}";
      replaceAll(selectionScript, "__COMPUTE_SCRIPT_BASE__", computeScriptBaseNullImpl);
    }

    // This method needs to be called after all of the .js files have been
    // swapped into the selectionScript since it will fill in __MODULE_NAME__
    // and many of the .js files contain that template variable
    selectionScript =
      processSelectionScriptCommon(selectionScript, logger, context);

    return selectionScript.toString();
  }

  @Override
  protected String getCompilationExtension(TreeLogger logger,
      LinkerContext context) {
    return ".cache.js";
  }

  @Override
  protected String getHostedFilename() {
    return "devmode.js";
  }
  
  @Override
  protected String getModulePrefix(TreeLogger logger, LinkerContext context,
      String strongName) {
    throw new UnsupportedOperationException("Should not be called");
  }

  @Override
  protected String getModulePrefix(TreeLogger logger, LinkerContext context,
      String strongName, int numFragments) {
    throw new UnsupportedOperationException("Should not be called");
  }

  @Override
  protected String getModuleSuffix(TreeLogger logger, LinkerContext context) {
    DefaultTextOutput out = new DefaultTextOutput(context.isOutputCompact());

    out.print("$sendStats('moduleStartup', 'moduleEvalEnd');");
    out.newlineOpt();
    out.print("gwtOnLoad("
        + "__gwtModuleFunction.__errFn, "
        + "__gwtModuleFunction.__moduleName, "
        + "__gwtModuleFunction.__moduleBase, "
        + "__gwtModuleFunction.__softPermutationId);");
    out.newlineOpt();
    out.print("$sendStats('moduleStartup', 'end');");

    return out.toString();
  }

  @Override
  protected String getSelectionScriptTemplate(TreeLogger logger,
      LinkerContext context) {
    return "com/google/gwt/core/linker/CrossSiteIframeTemplate.js";
  }

  private String getModulePrefix(LinkerContext context, String strongName) {
    TextOutput out = new DefaultTextOutput(context.isOutputCompact());
    out.print("var __gwtModuleFunction = $wnd." + context.getModuleFunctionName() + ";");
    out.newlineOpt();
    out.print("var $sendStats = __gwtModuleFunction.__sendStats;");
    out.newlineOpt();
    out.print("$sendStats('moduleStartup', 'moduleEvalStart');");
    out.newlineOpt();
    out.print("var $gwt_version = \"" + About.getGwtVersionNum() + "\";");
    out.newlineOpt();
    out.print("var $strongName = '" + strongName + "';");
    out.newlineOpt();
    out.print("var $doc = $wnd.document;");

    // Even though we call the $sendStats function in the code written in this
    // linker, some of the compilation code still needs the $stats and $sessionId
    // variables to be available.
    out.print("var $stats = $wnd.__gwtStatsEvent ? function(a) {return $wnd.__gwtStatsEvent(a);} : null;");
    out.newlineOpt();
    out.print("var $sessionId = $wnd.__gwtStatsSessionId ? $wnd.__gwtStatsSessionId : null;");
    out.newlineOpt();

    return out.toString();
  }
  
}
