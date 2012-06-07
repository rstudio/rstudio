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
import com.google.gwt.core.ext.linker.ConfigurationProperty;
import com.google.gwt.core.ext.linker.EmittedArtifact;
import com.google.gwt.core.ext.linker.EmittedArtifact.Visibility;
import com.google.gwt.core.ext.linker.LinkerOrder;
import com.google.gwt.core.ext.linker.LinkerOrder.Order;
import com.google.gwt.core.ext.linker.ScriptReference;
import com.google.gwt.core.ext.linker.Shardable;
import com.google.gwt.core.ext.linker.impl.PropertiesMappingArtifact;
import com.google.gwt.core.ext.linker.impl.PropertiesUtil;
import com.google.gwt.core.ext.linker.impl.ResourceInjectionUtil;
import com.google.gwt.core.ext.linker.impl.SelectionScriptLinker;
import com.google.gwt.dev.About;
import com.google.gwt.dev.js.JsToStringGenerationVisitor;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.TextOutput;
import com.google.gwt.thirdparty.guava.common.base.Joiner;
import com.google.gwt.thirdparty.guava.common.base.Splitter;
import com.google.gwt.util.tools.Utility;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

/**
 * This linker uses an iframe to hold the code and a script tag to download the
 * code. It can download code cross-site, because it uses a script tag to
 * download it and because it never uses XHR. The iframe, meanwhile, makes it
 * trivial to install additional code as the app runs.
 */
@LinkerOrder(Order.PRIMARY)
@Shardable
public class CrossSiteIframeLinker extends SelectionScriptLinker {
  /**
   * A configuration property that can be used to have the linker ignore the
   * script tags in gwt.xml rather than fail to compile if they are present.
   */
  private static final String FAIL_IF_SCRIPT_TAG_PROPERTY = "xsiframe.failIfScriptTag";

  @Override
  public String getDescription() {
    return "Cross-Site-Iframe";
  }

  @Override
  protected String fillSelectionScriptTemplate(StringBuffer ss, TreeLogger logger,
      LinkerContext context, ArtifactSet artifacts, CompilationResult result)
      throws UnableToCompleteException {

    if (shouldUseSelfForWindowAndDocument(context)) {
      replaceAll(ss, "__WINDOW_DEF__", "self");
      replaceAll(ss, "__DOCUMENT_DEF__", "self");
    } else {
      replaceAll(ss, "__WINDOW_DEF__", "window");
      replaceAll(ss, "__DOCUMENT_DEF__", "document");
    }

    // Must do installScript before waitForBodyLoaded and we must do
    // waitForBodyLoaded before isBodyLoaded
    includeJs(ss, logger, getJsInstallScript(context), "__INSTALL_SCRIPT__");
    includeJs(ss, logger, getJsWaitForBodyLoaded(context), "__WAIT_FOR_BODY_LOADED__");
    includeJs(ss, logger, getJsIsBodyLoaded(context), "__IS_BODY_LOADED__");

    // Must do permutations before providers
    includeJs(ss, logger, getJsPermutations(context), "__PERMUTATIONS__");
    includeJs(ss, logger, getJsProperties(context), "__PROPERTIES__");

    // Order doesn't matter for the rest
    includeJs(ss, logger, getJsProcessMetas(context), "__PROCESS_METAS__");
    includeJs(ss, logger, getJsInstallLocation(context), "__INSTALL_LOCATION__");
    includeJs(ss, logger, getJsComputeScriptBase(context), "__COMPUTE_SCRIPT_BASE__");
    includeJs(ss, logger, getJsComputeUrlForResource(context), "__COMPUTE_URL_FOR_RESOURCE__");
    includeJs(ss, logger, getJsLoadExternalStylesheets(context), "__LOAD_STYLESHEETS__");
    includeJs(ss, logger, getJsRunAsync(context), "__RUN_ASYNC__");
    includeJs(ss, logger, getJsDevModeRedirectHook(context), "__DEV_MODE_REDIRECT_HOOK__");

    // This Linker does not support <script> tags in the gwt.xml
    SortedSet<ScriptReference> scripts = artifacts.find(ScriptReference.class);
    if (!scripts.isEmpty()) {
      String list = "";
      for (ScriptReference script : scripts) {
        list += (script.getSrc() + "\n");
      }
      boolean failIfScriptTags = true;
      for (ConfigurationProperty prop : context.getConfigurationProperties()) {
        if (prop.getName().equalsIgnoreCase(FAIL_IF_SCRIPT_TAG_PROPERTY)) {
          if (prop.getValues().get(0).equalsIgnoreCase("false")) {
            failIfScriptTags = false;
          }
        }
      }
      if (failIfScriptTags) {
        // CHECKSTYLE_OFF
        String msg =
            "The " + getDescription()
                + " linker does not support <script> tags in the gwt.xml files, but the"
                + " gwt.xml file (or the gwt.xml files which it includes) contains the"
                + " following script tags: \n" + list
                + "In order for your application to run correctly, you will need to"
                + " include these tags in your host page directly. In order to avoid"
                + " this error, you will need to remove the script tags from the"
                + " gwt.xml file, or add this property to the gwt.xml file:"
                + " <set-configuration-property name='xsiframe.failIfScriptTag' value='FALSE'/>";
        // CHECKSTYLE_ON
        logger.log(TreeLogger.ERROR, msg);
        throw new UnableToCompleteException();
      } else {
        if (logger.isLoggable(TreeLogger.INFO)) {
          logger.log(TreeLogger.INFO, "Ignoring the following script tags in the gwt.xml "
              + "file\n" + list);
        }
      }
    }

    ss = ResourceInjectionUtil.injectStylesheets(ss, artifacts);
    ss = permutationsUtil.addPermutationsJs(ss, logger, context);

    if (result != null) {
      replaceAll(ss, "__KNOWN_PROPERTIES__", PropertiesUtil.addKnownPropertiesJs(logger, result));
    }
    replaceAll(ss, "__MODULE_FUNC__", context.getModuleFunctionName());
    replaceAll(ss, "__MODULE_NAME__", context.getModuleName());
    replaceAll(ss, "__HOSTED_FILENAME__", getHostedFilenameFull(context));

    if (context.isOutputCompact()) {
      replaceAll(ss, "__START_OBFUSCATED_ONLY__", "");
      replaceAll(ss, "__END_OBFUSCATED_ONLY__", "");
    } else {
      replaceAll(ss, "__START_OBFUSCATED_ONLY__", "/*");
      replaceAll(ss, "__END_OBFUSCATED_ONLY__", "*/");
    }

    String jsModuleFunctionErrorCatch = getJsModuleFunctionErrorCatch(context);
    if (jsModuleFunctionErrorCatch != null) {
      replaceAll(ss, "__BEGIN_TRY_BLOCK__", "try {");
      replaceAll(ss, "__END_TRY_BLOCK_AND_START_CATCH__", "} catch (moduleError) {");
      includeJs(ss, logger, jsModuleFunctionErrorCatch, "__MODULE_FUNC_ERROR_CATCH__");
      replaceAll(ss, "__END_CATCH_BLOCK__", "}");
    } else {
      replaceAll(ss, "__BEGIN_TRY_BLOCK__", "");
      replaceAll(ss, "__END_TRY_BLOCK_AND_START_CATCH__", "");
      replaceAll(ss, "__MODULE_FUNC_ERROR_CATCH__", "");
      replaceAll(ss, "__END_CATCH_BLOCK__", "");
    }

    return ss.toString();
  }

  protected boolean getBooleanConfigurationProperty(LinkerContext context,
      String name, boolean def) {
    String value = getStringConfigurationProperty(context, name, null);
    if (value != null) {
      if (value.equalsIgnoreCase("true")) {
        return true;
      } else if (value.equalsIgnoreCase("false")) {
        return false;
      }
    }
    return def;
  }

  @Override
  protected String getCompilationExtension(TreeLogger logger, LinkerContext context) {
    return ".cache.js";
  }

   protected String getDeferredFragmentSuffix(TreeLogger logger, LinkerContext context,
      int fragment) {
    return "\n//@ sourceURL=" + fragment + ".js\n";
  }

  @Override
  protected String getHostedFilename() {
    return "devmode.js";
  }

  protected String getHostedFilenameFull(LinkerContext context) {
    return context.getModuleName() + "." + getHostedFilename();
  }

  /**
   * Returns the name of the {@code ComputeScriptBase} script.  By default,
   * returns {@code "com/google/gwt/core/ext/linker/impl/computeScriptBase.js"}.
   *
   * @param context a LinkerContext
   */
  protected String getJsComputeScriptBase(LinkerContext context) {
    return getStringConfigurationProperty(context, "computeScriptBaseJs",
        "com/google/gwt/core/ext/linker/impl/computeScriptBase.js");
  }

  /**
   * Returns the name of the {@code UrlForResource} script.  By default,
   * returns {@code "com/google/gwt/core/ext/linker/impl/computeUrlForResource.js"}.
   *
   * @param context a LinkerContext
   */
  protected String getJsComputeUrlForResource(LinkerContext context) {
    return "com/google/gwt/core/ext/linker/impl/computeUrlForResource.js";
  }

  /**
   * Returns a code fragment to check for the new development mode.
   */
  protected String getJsDevModeRedirectHook(LinkerContext context) {
    // Enable Super Dev Mode for this app if the devModeRedirectEnabled config property is true.
    // TODO(skybrian) Change the default to enabled once we're sure it's safe.
    if (getBooleanConfigurationProperty(context, "devModeRedirectEnabled", false)) {
      return "com/google/gwt/core/linker/DevModeRedirectHook.js";
    } else {
      return "";
    }
  }

  /**
   * Returns the name of the {@code JsInstallLocation} script.  By default,
   * returns {@code "com/google/gwt/core/ext/linker/impl/installLocationIframe.js"}.
   *
   * @param context a LinkerContext
   */
  protected String getJsInstallLocation(LinkerContext context) {
    return "com/google/gwt/core/ext/linker/impl/installLocationIframe.js";
  }

  /**
   * Returns the name of the {@code JsInstallScript} script.  By default,
   * returns {@code "com/google/gwt/core/ext/linker/impl/installScriptEarlyDownload.js"}.
   *
   * <p> If you override this to return {@code installScriptDirect.js}, then you
   * should also override {@link #shouldInstallCode(LinkerContext)} to return
   * {@code false}.
   *
   * @param context a LinkerContext
   */
  protected String getJsInstallScript(LinkerContext context) {
    return getStringConfigurationProperty(context, "installScriptJs",
        "com/google/gwt/core/ext/linker/impl/installScriptEarlyDownload.js");
  }

  /**
   * Returns the name of the {@code JsIsBodyLoaded} script.  By default,
   * returns {@code "com/google/gwt/core/ext/linker/impl/isBodyLoaded.js"}.
   *
   * @param context a LinkerContext
   */
  protected String getJsIsBodyLoaded(LinkerContext context) {
    return "com/google/gwt/core/ext/linker/impl/isBodyLoaded.js";
  }

  /**
   * Returns the name of the {@code JsLoadExternalStylesheets} script.  By default,
   * returns {@code "com/google/gwt/core/ext/linker/impl/loadExternalStylesheets.js"}.
   *
   * @param context a LinkerContext
   */
  protected String getJsLoadExternalStylesheets(LinkerContext context) {
    return "com/google/gwt/core/ext/linker/impl/loadExternalStylesheets.js";
  }

  /**
   * Returns the name of the {@code JsModuleFunctionErrorCatch} script. By default returns null.
   * This script executes if there's an error loading the module function or executing it.
   * The error will be available under a local variable named "moduleError". If non-null, the
   * module function and the call to the module function will be placed in a try/catch block.
   *
   * @param context a LinkerContext
   */
  protected String getJsModuleFunctionErrorCatch(LinkerContext context) {
    return null;
  }

  /**
   * Returns the name of the {@code JsPermutations} script.  By default,
   * returns {@code "com/google/gwt/core/ext/linker/impl/permutations.js"}.
   *
   * @param context a LinkerContext
   */
  protected String getJsPermutations(LinkerContext context) {
    return "com/google/gwt/core/ext/linker/impl/permutations.js";
  }

  /**
   * Returns the name of the {@code JsProcessMetas} script.  By default,
   * returns {@code "com/google/gwt/core/ext/linker/impl/processMetas.js"}.
   *
   * @param context a LinkerContext
   */
  protected String getJsProcessMetas(LinkerContext context) {
    return "com/google/gwt/core/ext/linker/impl/processMetas.js";
  }

  /**
   * Returns the name of the {@code JsProperties} script.  By default,
   * returns {@code "com/google/gwt/core/ext/linker/impl/properties.js"}.
   *
   * @param context a LinkerContext
   */
  protected String getJsProperties(LinkerContext context) {
    return "com/google/gwt/core/ext/linker/impl/properties.js";
  }

  /**
   * Returns the name of the {@code JsRunAsync} script.  By default,
   * returns {@code "com/google/gwt/core/ext/linker/impl/runAsync.js"}.
   *
   * @param context a LinkerContext
   */
  protected String getJsRunAsync(LinkerContext context) {
    return "com/google/gwt/core/ext/linker/impl/runAsync.js";
  }

  /**
   * Returns the name of the {@code JsWaitForBodyLoaded} script.  By default,
   * returns {@code "com/google/gwt/core/ext/linker/impl/waitForBodyLoaded.js"}.
   *
   * @param context a LinkerContext
   */
  protected String getJsWaitForBodyLoaded(LinkerContext context) {
    return "com/google/gwt/core/ext/linker/impl/waitForBodyLoaded.js";
  }

  @Override
  protected String getModulePrefix(TreeLogger logger, LinkerContext context, String strongName)
    throws UnableToCompleteException {
    TextOutput out = new DefaultTextOutput(context.isOutputCompact());

    // We assume that the $wnd has been set in the same scope as this code is
    // executing in. $wnd is the main window which the GWT code is affecting. It
    // is also usually the location the bootstrap function was defined in.
    // In iframe based linkers, $wnd = window.parent;
    // Usually, in others, $wnd = window;

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

    // The functions for runAsync are set up in the bootstrap script so they
    // can be overriden in the same way as other bootstrap code is, however
    // they will be called from, and expected to run in the scope of the GWT code
    // (usually an iframe) so, here we set up those pointers.
    out.print("function __gwtStartLoadingFragment(frag) {");
    out.newlineOpt();
    String fragDir = getFragmentSubdir(logger, context) + '/';
    out.print("var fragFile = '" + fragDir + "' + $strongName + '/' + frag + '" + FRAGMENT_EXTENSION + "';");
    out.newlineOpt();
    out.print("return __gwtModuleFunction.__startLoadingFragment(fragFile);");
    out.newlineOpt();
    out.print("}");
    out.newlineOpt();
    out.print("function __gwtInstallCode(code) {return __gwtModuleFunction.__installRunAsyncCode(code);}");
    out.newlineOpt();

    // Even though we call the $sendStats function in the code written in this
    // linker, some of the compilation code still needs the $stats and
    // $sessionId
    // variables to be available.
    out.print("var $stats = $wnd.__gwtStatsEvent ? function(a) {return $wnd.__gwtStatsEvent(a);} : null;");
    out.newlineOpt();
    out.print("var $sessionId = $wnd.__gwtStatsSessionId ? $wnd.__gwtStatsSessionId : null;");
    out.newlineOpt();

    return out.toString();
  }

  @Override
  protected String getModuleSuffix(TreeLogger logger, LinkerContext context) {
    DefaultTextOutput out = new DefaultTextOutput(context.isOutputCompact());

    out.print("$sendStats('moduleStartup', 'moduleEvalEnd');");
    out.newlineOpt();
    out.print("gwtOnLoad(" + "__gwtModuleFunction.__errFn, " + "__gwtModuleFunction.__moduleName, "
        + "__gwtModuleFunction.__moduleBase, " + "__gwtModuleFunction.__softPermutationId,"
        + "__gwtModuleFunction.__computePropValue);");
    out.newlineOpt();
    out.print("$sendStats('moduleStartup', 'end');");
    out.print("\n//@ sourceURL=0.js\n");
    return out.toString();
  }

  @Override
  protected String getScriptChunkSeparator(TreeLogger logger, LinkerContext context) {
    return shouldInstallCode(context) ? "__SCRIPT_CHUNK_SEPARATOR_MARKER__" : "";
  }

  @Override
  protected String getSelectionScriptTemplate(TreeLogger logger, LinkerContext context) {
    return "com/google/gwt/core/linker/CrossSiteIframeTemplate.js";
  }

  protected String getStringConfigurationProperty(LinkerContext context,
      String name, String def) {
    for (ConfigurationProperty property : context.getConfigurationProperties()) {
      if (property.getName().equals(name) && property.getValues().size() > 0) {
        if (property.getValues().get(0) != null) {
          return property.getValues().get(0);
        }
      }
    }
    return def;
  }

  protected void includeJs(StringBuffer selectionScript, TreeLogger logger, String jsSource,
      String templateVar) throws UnableToCompleteException {
    String js;
    if (jsSource.endsWith(".js")) {
      try {
        js = Utility.getFileFromClassPath(jsSource);
      } catch (IOException e) {
        logger.log(TreeLogger.ERROR, "Unable to read file: " + jsSource, e);
        throw new UnableToCompleteException();
      }
    } else {
      js = jsSource;
    }
    replaceAll(selectionScript, templateVar, js);
  }

  @Override
  protected void maybeAddHostedModeFile(TreeLogger logger, LinkerContext context,
      ArtifactSet artifacts, CompilationResult result) throws UnableToCompleteException {
    String filename = getHostedFilename();
    if ("".equals(filename)) {
      return;
    }

    // when we're including bootstrap in the primary fragment, we should be
    // generating devmode files for each permutation. Otherwise, we generate it
    // only in the final link stage.
    boolean isSinglePermutation = (result != null);
    if (isSinglePermutation != shouldIncludeBootstrapInPrimaryFragment(context)) {
      return;
    }

    long lastModified = System.currentTimeMillis();
    StringBuffer buffer =
        readFileToStringBuffer("com/google/gwt/core/ext/linker/impl/" + filename, logger);

    String outputFilename = filename;
    if (result != null) {
      // If we're including bootstrap in the primary fragment, we generate a
      // devmode.js for each permutation, and it's name is XXX.devmode.js,
      // where XXX is the md5 hash for this permutation
      outputFilename = result.getStrongName() + "." + outputFilename;
    } else {
      // If we're not including bootstrap in the primary fragment, we generate
      // a devmode.js for this module and it's name is XXX.devmode.js, where
      // XXX is the module name.
      outputFilename = getHostedFilenameFull(context);
    }

    replaceAll(buffer, "__MODULE_NAME__", context.getModuleName());
    String script =
        generatePrimaryFragmentString(logger, context, result, buffer.toString(), 1, artifacts);

    EmittedArtifact devArtifact = emitString(logger, script, outputFilename, lastModified);
    artifacts.add(devArtifact);
  }

  // Output compilation-mappings.txt
  @Override
  protected void maybeOutputPropertyMap(TreeLogger logger, LinkerContext context,
      ArtifactSet toReturn) {
    if (permutationsUtil.getPermutationsMap() == null
        || permutationsUtil.getPermutationsMap().isEmpty()) {
      return;
    }

    PropertiesMappingArtifact mappingArtifact =
        new PropertiesMappingArtifact(CrossSiteIframeLinker.class, permutationsUtil
            .getPermutationsMap());

    toReturn.add(mappingArtifact);
    EmittedArtifact serializedMap;
    try {
      String mappings = mappingArtifact.getSerialized();
      // CHECKSTYLE_OFF
      mappings = mappings.concat("Devmode:" + getHostedFilename());
      // CHECKSTYLE_ON
      serializedMap = emitString(logger, mappings, "compilation-mappings.txt");
      // TODO(unnurg): make this Deploy
      serializedMap.setVisibility(Visibility.Public);
      toReturn.add(serializedMap);
    } catch (UnableToCompleteException e) {
      e.printStackTrace();
    }
  }

  // If you set this to return true, you should also override
  // getJsPermutations() to return permutationsNull.js and
  // getJsInstallScript() to return installScriptAlreadyIncluded.js
  protected boolean shouldIncludeBootstrapInPrimaryFragment(LinkerContext context) {
    return false;
  }

  protected boolean shouldInstallCode(LinkerContext context) {
    return getBooleanConfigurationProperty(context, "installCode", true);
  }

  /**
   * Returns whether to use "self" for $wnd and $doc references. Defaults to false.
   * Useful for worker threads.
   */
  protected boolean shouldUseSelfForWindowAndDocument(LinkerContext context) {
    return false;
  }

  @Override
  protected String wrapDeferredFragment(TreeLogger logger,
      LinkerContext context, int fragment, String js, ArtifactSet artifacts) {
    // TODO(unnurg): This assumes that the xsiframe linker is using the
    // ScriptTagLoadingStrategy (since it is also xs compatible).  However,
    // it should be completely valid to use the XhrLoadingStrategy with this
    // linker, in which case we would not want to wrap the deferred fragment
    // in this way.  Ideally, we should make a way for this code to be dependent
    // on what strategy is being used. Otherwise, we should make a property which
    // users can set to turn this wrapping off if they override the loading strategy.
    return String.format("$wnd.%s.runAsyncCallback%d(%s)\n",
        context.getModuleFunctionName(),
        fragment,
        JsToStringGenerationVisitor.javaScriptString(js));
  }

  @Override
  protected String wrapPrimaryFragment(TreeLogger logger, LinkerContext context, String script,
      ArtifactSet artifacts, CompilationResult result) throws UnableToCompleteException {

    StringBuilder out = new StringBuilder();

    if (shouldIncludeBootstrapInPrimaryFragment(context)) {
      out.append(generateSelectionScript(logger, context, artifacts, result));
      // only needed in SSSS, breaks WebWorker linker if done all the time
      out.append("if (" + context.getModuleFunctionName() + ".succeeded) {\n");
    }


    if (shouldInstallCode(context)) {
      // Rewrite the code so it can be installed with
      // __MODULE_FUNC__.onScriptDownloaded
      out.append(context.getModuleFunctionName());
      out.append(".onScriptDownloaded([");
      Iterable<String> chunks = Splitter.on(getScriptChunkSeparator(logger, context)).split(script);
      List<String> newChunks = new ArrayList<String>();
      for (String chunk : chunks) {
        newChunks.add(JsToStringGenerationVisitor.javaScriptString(chunk));
      }
      out.append(Joiner.on(", ").join(newChunks));
      out.append("]);\n");
    } else {
      out.append(script);
      out.append("\n");
    }
    if (shouldIncludeBootstrapInPrimaryFragment(context)) {
      out.append("}\n");
    }
    return out.toString();
  }
}
