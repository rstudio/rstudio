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
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.LinkerOrder;
import com.google.gwt.core.ext.linker.LinkerOrder.Order;
import com.google.gwt.core.ext.linker.Shardable;
import com.google.gwt.core.ext.linker.impl.SelectionScriptLinker;
import com.google.gwt.dev.About;
import com.google.gwt.dev.js.JsToStringGenerationVisitor;
import com.google.gwt.dev.util.DefaultTextOutput;

/**
 * Generates a cross-site compatible bootstrap sequence.
 */
@LinkerOrder(Order.PRIMARY)
@Shardable
public class XSLinker extends SelectionScriptLinker {
  @Override
  public String getDescription() {
    return "Cross-Site";
  }

  @Override
  protected String getCompilationExtension(TreeLogger logger,
      LinkerContext context) {
    return ".cache.js";
  }

  @Override
  protected String getModulePrefix(TreeLogger logger, LinkerContext context,
      String strongName) {
    return getModulePrefix(context, strongName, true);
  }

  @Override
  protected String getModulePrefix(TreeLogger logger, LinkerContext context,
      String strongName, int numFragments) {
    return getModulePrefix(context, strongName, numFragments > 1);
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
    out.print("if (" + context.getModuleFunctionName() + " && "
        + context.getModuleFunctionName() + ".onScriptLoad)"
        + context.getModuleFunctionName() + ".onScriptLoad(gwtOnLoad);");
    out.newlineOpt();
    out.print("})();");
    out.newlineOpt();

    return out.toString();
  }

  @Override
  protected String getSelectionScriptTemplate(TreeLogger logger,
      LinkerContext context) {
    return "com/google/gwt/core/linker/XSTemplate.js";
  }

   @Override
  protected String wrapDeferredFragment(TreeLogger logger,
      LinkerContext context, int fragment, String js, ArtifactSet artifacts) {
    return String.format("%s.runAsyncCallback%d(%s)\n",
        context.getModuleFunctionName(),
        fragment,
        JsToStringGenerationVisitor.javaScriptString(js));
  }

  private String getModulePrefix(LinkerContext context, String strongName,
      boolean supportRunAsync) {
    DefaultTextOutput out = new DefaultTextOutput(context.isOutputCompact());

    out.print("(function(){");
    out.newlineOpt();

    // Setup the well-known variables.
    //
    out.print("var $gwt_version = \"" + About.getGwtVersionNum() + "\";");
    out.newlineOpt();
    out.print("var $wnd = window;");
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
      out.print(context.getModuleFunctionName());
      out.print(".installCode = function(code) { eval(code) };");
      out.newlineOpt();
      out.print("var __gwtModuleFunction = ");
      out.print(context.getModuleFunctionName());
      out.print(";");
      out.newline();
    }

    return out.toString();
  }
}
