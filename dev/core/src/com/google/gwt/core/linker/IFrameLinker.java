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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.About;
import com.google.gwt.dev.linker.ArtifactSet;
import com.google.gwt.dev.linker.LinkerContext;
import com.google.gwt.dev.linker.LinkerOrder;
import com.google.gwt.dev.linker.LinkerOrder.Order;
import com.google.gwt.dev.linker.impl.SelectionScriptLinker;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.Util;
import com.google.gwt.util.tools.Utility;

import java.io.IOException;

/**
 * Implements the canonical GWT bootstrap sequence that loads the GWT module in
 * a separate iframe.
 */
@LinkerOrder(Order.PRIMARY)
public class IFrameLinker extends SelectionScriptLinker {

  public String getDescription() {
    return "Standard";
  }

  @Override
  public ArtifactSet link(TreeLogger logger, LinkerContext context,
      ArtifactSet artifacts) throws UnableToCompleteException {
    ArtifactSet toReturn = super.link(logger, context, artifacts);

    try {
      // Add hosted mode iframe contents
      // TODO move hosted.html into gwt-user if HostedModeLinker goes away
      String hostedHtml = Utility.getFileFromClassPath("com/google/gwt/dev/linker/impl/hosted.html");
      toReturn.add(emitBytes(logger, Util.getBytes(hostedHtml), "hosted.html"));
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Unable to copy support resource", e);
      throw new UnableToCompleteException();
    }

    return toReturn;
  }

  @Override
  protected String getCompilationExtension(TreeLogger logger,
      LinkerContext context) {
    return ".cache.html";
  }

  @Override
  protected String getModulePrefix(TreeLogger logger, LinkerContext context) {
    DefaultTextOutput out = new DefaultTextOutput(true);
    out.print("<html>");
    out.newlineOpt();

    // Setup the well-known variables.
    //
    out.print("<head><script>");
    out.newlineOpt();
    out.print("var $gwt_version = \"" + About.GWT_VERSION_NUM + "\";");
    out.newlineOpt();
    out.print("var $wnd = parent;");
    out.newlineOpt();
    out.print("var $doc = $wnd.document;");
    out.newlineOpt();
    out.print("var $moduleName, $moduleBase;");
    out.newlineOpt();
    out.print("var $stats = $wnd.__gwtstatsEvent ? function(a,b,c,d) {$wnd.__gwtstatsEvent(a,b,c,d)} : null;");
    out.newlineOpt();
    out.print("</script></head>");
    out.newlineOpt();
    out.print("<body>");
    out.newlineOpt();

    // Begin a script block inside the body. It's commented out so that the
    // browser won't mistake strings containing "<script>" for actual script.
    out.print("<script><!--");
    out.newline();
    out.print("$stats && $stats('" + context.getModuleName()
        + "', 'startup', 'moduleEvalStart', {millis:(new Date()).getTime()});");
    out.newline();
    return out.toString();
  }

  @Override
  protected String getModuleSuffix(TreeLogger logger, LinkerContext context) {
    DefaultTextOutput out = new DefaultTextOutput(true);

    // Generate the call to tell the bootstrap code that we're ready to go.
    out.newlineOpt();
    out.print("$stats && $stats('" + context.getModuleName()
        + "', 'startup', 'moduleEvalEnd', {millis:(new Date()).getTime()});");
    out.print("if ($wnd." + context.getModuleFunctionName() + ") $wnd."
        + context.getModuleFunctionName() + ".onScriptLoad();");
    out.newline();
    out.print("--></script></body></html>");
    out.newlineOpt();

    return out.toString();
  }

  @Override
  protected String getSelectionScriptTemplate(TreeLogger logger,
      LinkerContext context) {
    return "com/google/gwt/core/linker/IFrameTemplate.js";
  }

}
