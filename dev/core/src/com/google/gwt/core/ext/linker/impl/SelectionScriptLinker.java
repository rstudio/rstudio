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
import com.google.gwt.core.ext.linker.Artifact;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.CompilationResult;
import com.google.gwt.core.ext.linker.EmittedArtifact;
import com.google.gwt.core.ext.linker.ScriptReference;
import com.google.gwt.core.ext.linker.SelectionProperty;
import com.google.gwt.core.ext.linker.SoftPermutation;
import com.google.gwt.core.ext.linker.StylesheetReference;
import com.google.gwt.dev.util.StringKey;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.collect.HashSet;
import com.google.gwt.dev.util.collect.Lists;
import com.google.gwt.util.tools.Utility;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A base class for Linkers that use an external script to boostrap the GWT
 * module. This implementation injects JavaScript Snippets into a JS program
 * defined in an external file.
 */
public abstract class SelectionScriptLinker extends AbstractLinker {
  /**
   * TODO(bobv): Move this class into c.g.g.core.linker when HostedModeLinker
   * goes away?
   */

  /**
   * This represents the combination of a unique content hash (i.e. the MD5 of
   * the bytes to be written into the cache.html file) and a soft permutation
   * id.
   */
  protected static class PermutationId extends StringKey {
    private final int softPermutationId;
    private final String strongName;

    public PermutationId(String strongName, int softPermutationId) {
      super(strongName + ":" + softPermutationId);
      this.strongName = strongName;
      this.softPermutationId = softPermutationId;
    }

    public int getSoftPermutationId() {
      return softPermutationId;
    }

    public String getStrongName() {
      return strongName;
    }
  }

  /**
   * The extension added to demand-loaded fragment files.
   */
  protected static final String FRAGMENT_EXTENSION = ".cache.js";

  /**
   * A subdirectory to hold all the generated fragments.
   */
  protected static final String FRAGMENT_SUBDIR = "deferredjs";

  /**
   * File name for computeScriptBase.js.
   */
  static final String COMPUTE_SCRIPT_BASE_JS = "com/google/gwt/core/ext/linker/impl/computeScriptBase.js";

  /**
   * File name for processMetas.js.
   */
  static final String PROCESS_METAS_JS = "com/google/gwt/core/ext/linker/impl/processMetas.js";

  /**
   * Determines whether or not the URL is relative.
   * 
   * @param src the test url
   * @return <code>true</code> if the URL is relative, <code>false</code> if not
   */
  protected static boolean isRelativeURL(String src) {
    // A straight absolute url for the same domain, server, and protocol.
    if (src.startsWith("/")) {
      return false;
    }

    // If it can be parsed as a URL, then it's probably absolute.
    try {
      // Just check to see if it can be parsed, no need to store the result.
      new URL(src);

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

  /**
   * This maps each unique permutation to the property settings for that
   * compilation. A single compilation can have multiple property settings if
   * the compiles for those settings yielded the exact same compiled output.
   */
  private final SortedMap<PermutationId, List<Map<String, String>>> propMapsByPermutation = new TreeMap<PermutationId, List<Map<String, String>>>();

  /**
   * This method is left in place for existing subclasses of
   * SelectionScriptLinker that have not been upgraded for the sharding API.
   */
  @Override
  public ArtifactSet link(TreeLogger logger, LinkerContext context,
      ArtifactSet artifacts) throws UnableToCompleteException {
    ArtifactSet toReturn = link(logger, context, artifacts, true);
    toReturn = link(logger, context, toReturn, false);
    return toReturn;
  }

  @Override
  public ArtifactSet link(TreeLogger logger, LinkerContext context,
      ArtifactSet artifacts, boolean onePermutation)
      throws UnableToCompleteException {
    if (onePermutation) {
      ArtifactSet toReturn = new ArtifactSet(artifacts);

      /*
       * Support having multiple compilation results because this method is also
       * called from the legacy link method.
       */
      for (CompilationResult compilation : toReturn.find(CompilationResult.class)) {
        toReturn.addAll(doEmitCompilation(logger, context, compilation));
      }
      return toReturn;
    } else {
      processSelectionInformation(artifacts);

      ArtifactSet toReturn = new ArtifactSet(artifacts);
      toReturn.add(emitSelectionScript(logger, context, artifacts));
      return toReturn;
    }
  }

  protected Collection<Artifact<?>> doEmitCompilation(TreeLogger logger,
      LinkerContext context, CompilationResult result)
      throws UnableToCompleteException {
    String[] js = result.getJavaScript();
    byte[][] bytes = new byte[js.length][];
    bytes[0] = generatePrimaryFragment(logger, context, result, js);
    for (int i = 1; i < js.length; i++) {
      bytes[i] = Util.getBytes(generateDeferredFragment(logger, context, i,
          js[i]));
    }

    Collection<Artifact<?>> toReturn = new ArrayList<Artifact<?>>();
    toReturn.add(emitBytes(logger, bytes[0], result.getStrongName()
        + getCompilationExtension(logger, context)));
    for (int i = 1; i < js.length; i++) {
      toReturn.add(emitBytes(logger, bytes[i], FRAGMENT_SUBDIR + File.separator
          + result.getStrongName() + File.separator + i + FRAGMENT_EXTENSION));
    }

    toReturn.addAll(emitSelectionInformation(result.getStrongName(), result));

    return toReturn;
  }

  protected EmittedArtifact emitSelectionScript(TreeLogger logger,
      LinkerContext context, ArtifactSet artifacts)
      throws UnableToCompleteException {
    String selectionScript = generateSelectionScript(logger, context, artifacts);
    selectionScript = context.optimizeJavaScript(logger, selectionScript);

    /*
     * Last modified is important to keep hosted mode refreses from clobbering
     * web mode compiles. We set the timestamp on the hosted mode selection
     * script to the same mod time as the module (to allow updates). For web
     * mode, we just set it to now.
     */
    long lastModified;
    if (propMapsByPermutation.isEmpty()) {
      lastModified = context.getModuleLastModified();
    } else {
      lastModified = System.currentTimeMillis();
    }

    return emitString(logger, selectionScript, context.getModuleName()
        + ".nocache.js", lastModified);
  }

  protected String generateDeferredFragment(TreeLogger logger,
      LinkerContext context, int fragment, String js) {
    return js;
  }

  /**
   * Generate the primary fragment. The default implementation is based on
   * {@link #getModulePrefix(TreeLogger, LinkerContext, String, int)} and
   * {@link #getModuleSuffix(TreeLogger, LinkerContext)}.
   */
  protected byte[] generatePrimaryFragment(TreeLogger logger,
      LinkerContext context, CompilationResult result, String[] js)
      throws UnableToCompleteException {
    StringBuffer b = new StringBuffer();
    b.append(getModulePrefix(logger, context, result.getStrongName(), js.length));
    b.append(js[0]);
    b.append(getModuleSuffix(logger, context));
    return Util.getBytes(b.toString());
  }

  protected String generatePropertyProvider(SelectionProperty prop) {
    StringBuffer toReturn = new StringBuffer();

    if (prop.tryGetValue() == null && !prop.isDerived()) {
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

  /**
   * Generate a selection script. The selection information should previously
   * have been scanned using {@link #processSelectionInformation(ArtifactSet)}.
   */
  protected String generateSelectionScript(TreeLogger logger,
      LinkerContext context, ArtifactSet artifacts)
      throws UnableToCompleteException {
    StringBuffer selectionScript;
    String processMetas;
    String computeScriptBase;
    try {
      selectionScript = new StringBuffer(
          Utility.getFileFromClassPath(getSelectionScriptTemplate(logger,
              context)));
      processMetas = Utility.getFileFromClassPath(PROCESS_METAS_JS);
      computeScriptBase = Utility.getFileFromClassPath(COMPUTE_SCRIPT_BASE_JS);
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Unable to read selection script template",
          e);
      throw new UnableToCompleteException();
    }

    replaceAll(selectionScript, "__PROCESS_METAS__", processMetas);
    replaceAll(selectionScript, "__COMPUTE_SCRIPT_BASE__", computeScriptBase);
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
    startPos = selectionScript.indexOf("// __PERMUTATIONS_END__");
    if (startPos != -1) {
      StringBuffer text = new StringBuffer();
      if (propMapsByPermutation.size() == 0) {
        // Hosted mode link.
        text.append("alert(\"GWT module '" + context.getModuleName()
            + "' may need to be (re)compiled\");");
        text.append("return;");

      } else if (propMapsByPermutation.size() == 1) {
        // Just one distinct compilation; no need to evaluate properties
        text.append("strongName = '"
            + propMapsByPermutation.keySet().iterator().next() + "';");
      } else {
        Set<String> propertiesUsed = new HashSet<String>();
        for (PermutationId permutationId : propMapsByPermutation.keySet()) {
          for (Map<String, String> propertyMap : propMapsByPermutation.get(permutationId)) {
            // unflatten([v1, v2, v3], 'strongName' + ':softPermId');
            text.append("unflattenKeylistIntoAnswers([");
            boolean needsComma = false;
            for (SelectionProperty p : context.getProperties()) {
              if (p.tryGetValue() != null) {
                continue;
              } else if (p.isDerived()) {
                continue;
              }

              if (needsComma) {
                text.append(",");
              } else {
                needsComma = true;
              }
              text.append("'" + propertyMap.get(p.getName()) + "'");
              propertiesUsed.add(p.getName());
            }

            // Concatenate the soft permutation id to improve string interning
            text.append("], '").append(permutationId.getStrongName()).append(
                "' + ':").append(permutationId.getSoftPermutationId()).append(
                "');\n");
          }
        }

        // strongName = answers[compute('p1')][compute('p2')];
        text.append("strongName = answers[");
        boolean needsIndexMarkers = false;
        for (SelectionProperty p : context.getProperties()) {
          if (!propertiesUsed.contains(p.getName())) {
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
   * Generate a Snippet of JavaScript to inject an external stylesheet.
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
   * Compute the beginning of a JavaScript file that will hold the main module
   * implementation.
   */
  protected abstract String getModulePrefix(TreeLogger logger,
      LinkerContext context, String strongName)
      throws UnableToCompleteException;

  /**
   * Compute the beginning of a JavaScript file that will hold the main module
   * implementation. By default, calls
   * {@link #getModulePrefix(TreeLogger, LinkerContext, String)}.
   * 
   * @param strongName strong name of the module being emitted
   * @param numFragments the number of fragments for this module, including the
   *          main fragment (fragment 0)
   */
  protected String getModulePrefix(TreeLogger logger, LinkerContext context,
      String strongName, int numFragments) throws UnableToCompleteException {
    return getModulePrefix(logger, context, strongName);
  }

  protected abstract String getModuleSuffix(TreeLogger logger,
      LinkerContext context) throws UnableToCompleteException;

  protected abstract String getSelectionScriptTemplate(TreeLogger logger,
      LinkerContext context) throws UnableToCompleteException;

  /**
   * Find all instances of {@link SelectionInformation} and add them to the
   * internal map of selection information.
   */
  protected void processSelectionInformation(ArtifactSet artifacts) {
    for (SelectionInformation selInfo : artifacts.find(SelectionInformation.class)) {
      processSelectionInformation(selInfo);
    }
  }

  private List<Artifact<?>> emitSelectionInformation(String strongName,
      CompilationResult result) {
    List<Artifact<?>> emitted = new ArrayList<Artifact<?>>();

    for (SortedMap<SelectionProperty, String> propertyMap : result.getPropertyMap()) {
      TreeMap<String, String> propMap = new TreeMap<String, String>();
      for (Map.Entry<SelectionProperty, String> entry : propertyMap.entrySet()) {
        propMap.put(entry.getKey().getName(), entry.getValue());
      }

      // The soft properties may not be a subset of the existing set
      for (SoftPermutation soft : result.getSoftPermutations()) {
        // Make a copy we can add add more properties to
        TreeMap<String, String> softMap = new TreeMap<String, String>(propMap);
        // Make sure this SelectionInformation contains the soft properties
        for (Map.Entry<SelectionProperty, String> entry : soft.getPropertyMap().entrySet()) {
          softMap.put(entry.getKey().getName(), entry.getValue());
        }
        emitted.add(new SelectionInformation(strongName, soft.getId(), softMap));
      }
    }

    return emitted;
  }

  private Map<String, String> processSelectionInformation(
      SelectionInformation selInfo) {
    TreeMap<String, String> entries = selInfo.getPropMap();
    PermutationId permutationId = new PermutationId(selInfo.getStrongName(),
        selInfo.getSoftPermutationId());
    if (!propMapsByPermutation.containsKey(permutationId)) {
      propMapsByPermutation.put(permutationId,
          Lists.<Map<String, String>> create(entries));
    } else {
      propMapsByPermutation.put(permutationId, Lists.add(
          propMapsByPermutation.get(permutationId), entries));
    }
    return entries;
  }
}
