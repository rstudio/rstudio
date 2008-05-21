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
package com.google.gwt.core.ext.linker.impl;

import com.google.gwt.core.ext.LinkerContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.AbstractLinker;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.CompilationResult;
import com.google.gwt.core.ext.linker.EmittedArtifact;
import com.google.gwt.core.ext.linker.ScriptReference;
import com.google.gwt.core.ext.linker.SelectionProperty;
import com.google.gwt.core.ext.linker.StylesheetReference;
import com.google.gwt.dev.util.Util;
import com.google.gwt.util.tools.Utility;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;

/**
 * A base class for Linkers that use an external script to boostrap the GWT
 * module. This implementation injects JavaScript snippits into a JS program
 * defined in an external file.
 */
public abstract class SelectionScriptLinker extends AbstractLinker {
  /**
   * TODO(bobv): Move this class into c.g.g.core.linker when HostedModeLinker
   * goes away?
   */

  /**
   * Determines whether or not the URL is relative.
   * 
   * @param src the test url
   * @return <code>true</code> if the URL is relative, <code>false</code> if
   *         not
   */
  @SuppressWarnings("unused")
  protected static boolean isRelativeURL(String src) {
    // A straight absolute url for the same domain, server, and protocol.
    if (src.startsWith("/")) {
      return false;
    }

    // If it can be parsed as a URL, then it's probably absolute.
    try {
      URL testUrl = new URL(src);

      // Let's guess that it is absolute (thus, not relative).
      return false;
    } catch (MalformedURLException e) {
      // Do nothing, since it was a speculative parse.
    }

    // Since none of the above matched, let's guess that it's relative.
    return true;
  }

  protected static void replaceAll(StringBuffer buf, String search,
      String replace) {
    int len = search.length();
    for (int pos = buf.indexOf(search); pos >= 0; pos = buf.indexOf(search,
        pos + 1)) {
      buf.replace(pos, pos + len, replace);
    }
  }

  private final Map<CompilationResult, String> compilationPartialPaths = new IdentityHashMap<CompilationResult, String>();

  @Override
  public ArtifactSet link(TreeLogger logger, LinkerContext context,
      ArtifactSet artifacts) throws UnableToCompleteException {
    ArtifactSet toReturn = new ArtifactSet(artifacts);

    for (CompilationResult compilation : toReturn.find(CompilationResult.class)) {
      toReturn.add(doEmitCompilation(logger, context, compilation));
    }

    toReturn.add(emitSelectionScript(logger, context, artifacts));
    return toReturn;
  }

  protected EmittedArtifact doEmitCompilation(TreeLogger logger,
      LinkerContext context, CompilationResult result)
      throws UnableToCompleteException {
    StringBuffer b = new StringBuffer();
    b.append(getModulePrefix(logger, context));
    b.append(result.getJavaScript());
    b.append(getModuleSuffix(logger, context));
    EmittedArtifact toReturn = emitWithStrongName(logger,
        Util.getBytes(b.toString()), "", getCompilationExtension(logger,
            context));
    compilationPartialPaths.put(result, toReturn.getPartialPath());
    return toReturn;
  }

  protected EmittedArtifact emitSelectionScript(TreeLogger logger,
      LinkerContext context, ArtifactSet artifacts)
      throws UnableToCompleteException {
    String selectionScript = generateSelectionScript(logger, context, artifacts);
    byte[] selectionScriptBytes = Util.getBytes(context.optimizeJavaScript(
        logger, selectionScript));
    return emitBytes(logger, selectionScriptBytes, context.getModuleName()
        + ".nocache.js");
  }

  protected String generatePropertyProvider(SelectionProperty prop) {
    StringBuffer toReturn = new StringBuffer();

    if (prop.tryGetValue() == null) {
      toReturn.append("providers['" + prop.getName() + "'] = function()");
      toReturn.append(prop.getPropertyProvider());
      toReturn.append(";");

      toReturn.append("values['" + prop.getName() + "'] = {");
      boolean needsComma = false;
      int counter = 0;
      for (String value : prop.getPossibleValues()) {
        if (needsComma) {
          toReturn.append(",");
        } else {
          needsComma = true;
        }
        toReturn.append("'" + value + "':");
        toReturn.append(counter++);
      }
      toReturn.append("};");
    }

    return toReturn.toString();
  }

  protected String generateScriptInjector(String scriptUrl) {
    if (isRelativeURL(scriptUrl)) {
      return "  if (!__gwt_scriptsLoaded['"
          + scriptUrl
          + "']) {\n"
          + "    __gwt_scriptsLoaded['"
          + scriptUrl
          + "'] = true;\n"
          + "    document.write('<script language=\\\"javascript\\\" src=\\\"'+base+'"
          + scriptUrl + "\\\"></script>');\n" + "  }\n";
    } else {
      return "  if (!__gwt_scriptsLoaded['" + scriptUrl + "']) {\n"
          + "    __gwt_scriptsLoaded['" + scriptUrl + "'] = true;\n"
          + "    document.write('<script language=\\\"javascript\\\" src=\\\""
          + scriptUrl + "\\\"></script>');\n" + "  }\n";
    }
  }

  protected String generateSelectionScript(TreeLogger logger,
      LinkerContext context, ArtifactSet artifacts)
      throws UnableToCompleteException {

    StringBuffer selectionScript;
    try {
      selectionScript = new StringBuffer(
          Utility.getFileFromClassPath(getSelectionScriptTemplate(logger,
              context)));
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Unable to read selection script template",
          e);
      throw new UnableToCompleteException();
    }

    replaceAll(selectionScript, "__MODULE_FUNC__",
        context.getModuleFunctionName());
    replaceAll(selectionScript, "__MODULE_NAME__", context.getModuleName());

    int startPos;

    // Add external dependencies
    startPos = selectionScript.indexOf("// __MODULE_STYLES_END__");
    if (startPos != -1) {
      for (StylesheetReference resource : artifacts.find(StylesheetReference.class)) {
        String text = generateStylesheetInjector(resource.getSrc());
        selectionScript.insert(startPos, text);
        startPos += text.length();
      }
    }

    startPos = selectionScript.indexOf("// __MODULE_SCRIPTS_END__");
    if (startPos != -1) {
      for (ScriptReference resource : artifacts.find(ScriptReference.class)) {
        String text = generateScriptInjector(resource.getSrc());
        selectionScript.insert(startPos, text);
        startPos += text.length();
      }
    }

    // Add property providers
    startPos = selectionScript.indexOf("// __PROPERTIES_END__");
    if (startPos != -1) {
      for (SelectionProperty p : context.getProperties()) {
        String text = generatePropertyProvider(p);
        selectionScript.insert(startPos, text);
        startPos += text.length();
      }
    }

    // Possibly add permutations
    SortedSet<CompilationResult> compilations = artifacts.find(CompilationResult.class);
    startPos = selectionScript.indexOf("// __PERMUTATIONS_END__");
    if (startPos != -1) {
      StringBuffer text = new StringBuffer();
      if (compilations.size() == 0) {
        // We'll see this when running in Hosted Mode. The way the selection
        // templates are structured is such that this line won't be executed
        text.append("strongName = null;");

      } else if (compilations.size() == 1) {
        // Just one distinct compilation; no need to evaluate properties
        Iterator<CompilationResult> iter = compilations.iterator();
        CompilationResult result = iter.next();
        text.append("strongName = '" + compilationPartialPaths.get(result)
            + "';");

      } else {
        for (CompilationResult r : compilations) {
          for (Map<SelectionProperty, String> propertyMap : r.getPropertyMap()) {
            // unflatten([v1, v2, v3], 'strongName');
            text.append("unflattenKeylistIntoAnswers([");
            boolean needsComma = false;
            for (SelectionProperty p : context.getProperties()) {
              if (!propertyMap.containsKey(p)) {
                continue;
              }

              if (needsComma) {
                text.append(",");
              } else {
                needsComma = true;
              }
              text.append("'" + propertyMap.get(p) + "'");
            }
            text.append("], '").append(compilationPartialPaths.get(r)).append(
                "');\n");
          }
        }

        // strongName = answers[compute('p1')][compute('p2')];
        text.append("strongName = answers[");
        boolean needsIndexMarkers = false;
        for (SelectionProperty p : context.getProperties()) {
          if (p.tryGetValue() != null) {
            continue;
          }
          if (needsIndexMarkers) {
            text.append("][");
          } else {
            needsIndexMarkers = true;
          }
          text.append("computePropValue('" + p.getName() + "')");
        }
        text.append("];");
      }
      selectionScript.insert(startPos, text);
    }

    return selectionScript.toString();
  }

  /**
   * Generate a snippit of JavaScript to inject an external stylesheet.
   * 
   * <pre>
   * if (!__gwt_stylesLoaded['URL']) {
   *   var l = $doc.createElement('link');
   *   __gwt_styleLoaded['URL'] = l;
   *   l.setAttribute('rel', 'stylesheet');
   *   l.setAttribute('href', HREF_EXPR);
   *   $doc.getElementsByTagName('head')[0].appendChild(l);
   * }
   * </pre>
   */
  protected String generateStylesheetInjector(String stylesheetUrl) {
    String hrefExpr = "'" + stylesheetUrl + "'";
    if (isRelativeURL(stylesheetUrl)) {
      hrefExpr = "base + " + hrefExpr;
    }

    return "if (!__gwt_stylesLoaded['" + stylesheetUrl + "']) {\n           "
        + "  var l = $doc.createElement('link');\n                          "
        + "  __gwt_stylesLoaded['" + stylesheetUrl + "'] = l;\n             "
        + "  l.setAttribute('rel', 'stylesheet');\n                         "
        + "  l.setAttribute('href', " + hrefExpr + ");\n                    "
        + "  $doc.getElementsByTagName('head')[0].appendChild(l);\n         "
        + "}\n";
  }

  protected abstract String getCompilationExtension(TreeLogger logger,
      LinkerContext context) throws UnableToCompleteException;

  /**
   * Get the partial path on which a CompilationResult has been emitted.
   * 
   * @return the partial path, or <code>null</code> if the CompilationResult
   *         has not been emitted.
   */
  protected String getCompilationPartialPath(CompilationResult result) {
    return compilationPartialPaths.get(result);
  }

  protected abstract String getModulePrefix(TreeLogger logger,
      LinkerContext context) throws UnableToCompleteException;

  protected abstract String getModuleSuffix(TreeLogger logger,
      LinkerContext context) throws UnableToCompleteException;

  protected abstract String getSelectionScriptTemplate(TreeLogger logger,
      LinkerContext context) throws UnableToCompleteException;
}
