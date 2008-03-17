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

/**
 * Generates a cross-site compatible bootstrap sequence.
 */
public class XSLinker extends SelectionScriptLinker {

  public String getDescription() {
    return "Cross-Site";
  }

  @Override
  protected String getCompilationExtension(TreeLogger logger,
      LinkerContext context) throws UnableToCompleteException {
    return ".cache.js";
  }

  @Override
  protected String getModulePrefix(TreeLogger logger, LinkerContext context)
      throws UnableToCompleteException {
    DefaultTextOutput out = new DefaultTextOutput(true);

    out.print("(function(){");
    out.newlineOpt();

    // Setup the well-known variables.
    //
    out.print("var $gwt_version = \"" + About.GWT_VERSION_NUM + "\";");
    out.newlineOpt();
    out.print("var $wnd = window;");
    out.newlineOpt();
    out.print("var $doc = $wnd.document;");
    out.newlineOpt();
    out.print("var $moduleName, $moduleBase;");
    out.newlineOpt();
    out.print("var $stats = $wnd.__gwtstatsEvent ? function(a,b,c,d){$wnd.__gwtstatsEvent(a,b,c,d)} : null;");
    out.newlineOpt();
    out.print("$stats && $stats('" + context.getModuleName()
        + "', 'startup', 'moduleEvalStart', {millis:(new Date()).getTime()});");
    out.newlineOpt();

    return out.toString();
  }

  @Override
  protected String getModuleSuffix(TreeLogger logger, LinkerContext context)
      throws UnableToCompleteException {
    DefaultTextOutput out = new DefaultTextOutput(true);

    out.newlineOpt();
    out.print("$stats && $stats('" + context.getModuleName()
        + "', 'startup', 'moduleEvalEnd', {millis:(new Date()).getTime()});");
    // Generate the call to tell the bootstrap code that we're ready to go.
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

    return out.toString();
  }

  @Override
  protected String getSelectionScriptTemplate(TreeLogger logger,
      LinkerContext context) throws UnableToCompleteException {
    return "com/google/gwt/dev/linker/XSTemplate.js";
  }
}
