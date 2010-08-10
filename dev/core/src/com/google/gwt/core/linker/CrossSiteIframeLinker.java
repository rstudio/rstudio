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

/**
 * This linker uses an iframe to hold the code and a script tag to download the
 * code. It can download code cross-site, because it uses a script tag to
 * download it and because it never uses XHR. The iframe, meanwhile, makes it
 * trivial to install additional code as the app runs.
 */
@LinkerOrder(Order.PRIMARY)
@Shardable
public class CrossSiteIframeLinker extends SelectionScriptLinker {
  @Override
  public String getDescription() {
    return "Cross-Site-Iframe";
  }

  @Override
  protected String generateDeferredFragment(TreeLogger logger,
      LinkerContext context, int fragment, String js) {
    StringBuilder sb = new StringBuilder();
    sb.append("$wnd.");
    sb.append(context.getModuleFunctionName());
    sb.append(".runAsyncCallback");
    sb.append(fragment);
    sb.append("(");
    sb.append(JsToStringGenerationVisitor.javaScriptString(js));
    sb.append(");\n");
    return sb.toString();
  }

  @Override
  protected byte[] generatePrimaryFragment(TreeLogger logger,
      LinkerContext context, CompilationResult result, String[] js) {
    // Wrap the script code with its prefix and suffix
    TextOutput script = new DefaultTextOutput(context.isOutputCompact());
    script.print(getModulePrefix(context, result.getStrongName(), js.length > 1));
    script.print(js[0]);
    script.print(getModuleSuffix(logger, context));

    // Rewrite the code so it can be installed with
    // __MODULE_FUNC__.onScriptDownloaded

    StringBuffer out = new StringBuffer();
    out.append(context.getModuleFunctionName());
    out.append(".onScriptDownloaded(");
    out.append(JsToStringGenerationVisitor.javaScriptString(script.toString()));
    out.append(")");

    return Util.getBytes(out.toString());
  }

  @Override
  protected String getCompilationExtension(TreeLogger logger,
      LinkerContext context) {
    return ".cache.js";
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

    out.print("$stats && $stats({moduleName:'" + context.getModuleName()
        + "',sessionId:$sessionId"
        + ",subSystem:'startup',evtGroup:'moduleStartup'"
        + ",millis:(new Date()).getTime(),type:'moduleEvalEnd'});");

    // Generate the call to tell the bootstrap code that we're ready to go.
    out.newlineOpt();
    out.print("if ($wnd." + context.getModuleFunctionName() + " && $wnd."
        + context.getModuleFunctionName() + ".onScriptInstalled) $wnd."
        + context.getModuleFunctionName() + ".onScriptInstalled(gwtOnLoad);");
    out.newlineOpt();

    return out.toString();
  }

  @Override
  protected String getSelectionScriptTemplate(TreeLogger logger,
      LinkerContext context) {
    return "com/google/gwt/core/linker/CrossSiteIframeTemplate.js";
  }

  private String getModulePrefix(LinkerContext context, String strongName,
      boolean supportRunAsync) {
    TextOutput out = new DefaultTextOutput(context.isOutputCompact());

    out.print("var $gwt_version = \"" + About.getGwtVersionNum() + "\";");
    out.newlineOpt();
    out.print("var $wnd = window.parent;");
    out.newlineOpt();
    out.print("var $doc = $wnd.document;");
    out.newlineOpt();
    out.print("var $moduleName, $moduleBase;");
    out.newlineOpt();
    out.print("var $strongName = '" + strongName + "';");
    out.newlineOpt();
    out.print("var $stats = $wnd.__gwtStatsEvent ? function(a) {return $wnd.__gwtStatsEvent(a);} : null;");
    out.newlineOpt();
    out.print("var $sessionId = $wnd.__gwtStatsSessionId ? $wnd.__gwtStatsSessionId : null;");
    out.newlineOpt();

    out.print("$stats && $stats({moduleName:'" + context.getModuleName()
        + "',sessionId:$sessionId"
        + ",subSystem:'startup',evtGroup:'moduleStartup'"
        + ",millis:(new Date()).getTime(),type:'moduleEvalStart'});");
    out.newlineOpt();

    if (supportRunAsync) {
      out.print("var __gwtModuleFunction = $wnd.");
      out.print(context.getModuleFunctionName());
      out.print(";");
      out.newlineOpt();
    }

    return out.toString();
  }
}
