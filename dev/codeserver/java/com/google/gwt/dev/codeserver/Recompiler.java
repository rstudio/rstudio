/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.dev.codeserver;

import com.google.gwt.core.ext.Linker;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.linker.CrossSiteIframeLinker;
import com.google.gwt.core.linker.IFrameLinker;
import com.google.gwt.dev.Compiler;
import com.google.gwt.dev.CompilerContext;
import com.google.gwt.dev.CompilerOptions;
import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.cfg.ConfigurationProperty;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.cfg.Property;
import com.google.gwt.dev.cfg.ResourceLoader;
import com.google.gwt.dev.cfg.ResourceLoaders;
import com.google.gwt.dev.javac.UnitCacheSingleton;
import com.google.gwt.dev.resource.impl.ResourceOracleImpl;
import com.google.gwt.dev.resource.impl.ZipFileClassPathEntry;
import com.google.gwt.dev.util.log.CompositeTreeLogger;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Recompiles a GWT module on demand.
 */
class Recompiler {
  private final AppSpace appSpace;
  private final String originalModuleName;
  private final TreeLogger logger;
  private String serverPrefix;
  private int compilesDone = 0;

  // after renaming
  private AtomicReference<String> moduleName = new AtomicReference<String>(null);

  private final AtomicReference<CompileDir> lastBuild = new AtomicReference<CompileDir>();
  private final AtomicReference<ResourceLoader> resourceLoader =
      new AtomicReference<ResourceLoader>();
  private final CompilerContext.Builder compilerContextBuilder = new CompilerContext.Builder();
  private CompilerContext compilerContext;
  private Options options;

  Recompiler(AppSpace appSpace, String moduleName, Options options, TreeLogger logger) {
    this.appSpace = appSpace;
    this.originalModuleName = moduleName;
    this.options = options;
    this.logger = logger;
    this.serverPrefix = options.getPreferredHost() + ":" + options.getPort();
    compilerContext = compilerContextBuilder.build();
  }

  synchronized CompileDir compile(Map<String, String> bindingProperties)
      throws UnableToCompleteException {
    if (compilesDone == 0) {
      System.setProperty("java.awt.headless", "true");
      if (System.getProperty("gwt.speedtracerlog") == null) {
        System.setProperty("gwt.speedtracerlog",
            appSpace.getSpeedTracerLogFile().getAbsolutePath());
      }
      compilerContext = compilerContextBuilder.unitCache(
          UnitCacheSingleton.get(logger, appSpace.getUnitCacheDir())).build();
    }

    long startTime = System.currentTimeMillis();
    int compileId = ++compilesDone;
    CompileDir compileDir = makeCompileDir(compileId);
    TreeLogger compileLogger = makeCompileLogger(compileDir);

    boolean listenerFailed = false;
    try {
      options.getRecompileListener().startedCompile(originalModuleName, compileId, compileDir);
    } catch (Exception e) {
      compileLogger.log(TreeLogger.Type.WARN, "listener threw exception", e);
      listenerFailed = true;
    }

    boolean success = false;
    try {
      CompilerOptions compilerOptions = new CompilerOptionsImpl(
          compileDir, options.getModuleNames(), options.getSourceLevel(),
          options.enforceStrictResources(), options.getLogLevel());
      compilerContext = compilerContextBuilder.options(compilerOptions).build();
      ModuleDef module = loadModule(compileLogger, bindingProperties);

      // Propagates module rename.
      String newModuleName = module.getName();
      moduleName.set(newModuleName);
      compilerOptions = new CompilerOptionsImpl(
          compileDir, Lists.newArrayList(newModuleName), options.getSourceLevel(),
          options.enforceStrictResources(), options.getLogLevel());
      compilerContext = compilerContextBuilder.options(compilerOptions).build();

      success = new Compiler(compilerOptions).run(compileLogger, module);
      lastBuild.set(compileDir); // makes compile log available over HTTP
    } finally {
      try {
        options.getRecompileListener().finishedCompile(originalModuleName, compileId, success);
      } catch (Exception e) {
        compileLogger.log(TreeLogger.Type.WARN, "listener threw exception", e);
        listenerFailed = true;
      }
    }

    if (!success) {
      compileLogger.log(TreeLogger.Type.ERROR, "Compiler returned " + success);
      throw new UnableToCompleteException();
    }

    long elapsedTime = System.currentTimeMillis() - startTime;
    compileLogger.log(TreeLogger.Type.INFO, "Compile completed in " + elapsedTime + " ms");

    if (options.isCompileTest() && listenerFailed) {
      throw new UnableToCompleteException();
    }

    return compileDir;
  }

  synchronized CompileDir noCompile() throws UnableToCompleteException {
    long startTime = System.currentTimeMillis();
    CompileDir compileDir = makeCompileDir(++compilesDone);
    TreeLogger compileLogger = makeCompileLogger(compileDir);

    ModuleDef module = loadModule(compileLogger, new HashMap<String, String>());
    String newModuleName = module.getName();  // includes any rename.
    moduleName.set(newModuleName);

    lastBuild.set(compileDir);

    try {
      // Prepare directory.
      File outputDir = new File(
          compileDir.getWarDir().getCanonicalPath() + "/" + getModuleName());
      if (!outputDir.exists()) {
        outputDir.mkdir();
      }

      // Creates a "module_name.nocache.js" that just forces a recompile.
      String moduleScript = PageUtil.loadResource(Recompiler.class, "nomodule.nocache.js");
      moduleScript = moduleScript.replace("__MODULE_NAME__", getModuleName());
      PageUtil.writeFile(outputDir.getCanonicalPath() + "/" + getModuleName() + ".nocache.js",
          moduleScript);

    } catch (IOException e) {
      compileLogger.log(TreeLogger.Type.ERROR, "Error creating uncompiled module.", e);
    }
    long elapsedTime = System.currentTimeMillis() - startTime;
    compileLogger.log(TreeLogger.Type.INFO, "Module setup completed in " + elapsedTime + " ms");
    return compileDir;
  }

  /**
   * Returns the log from the last compile. (It may be a failed build.)
   */
  File getLastLog() {
    return lastBuild.get().getLogFile();
  }

  String getModuleName() {
    return moduleName.get();
  }

  ResourceLoader getResourceLoader() {
    return resourceLoader.get();
  }

  private TreeLogger makeCompileLogger(CompileDir compileDir)
      throws UnableToCompleteException {
    try {
      PrintWriterTreeLogger fileLogger =
          new PrintWriterTreeLogger(compileDir.getLogFile());
      fileLogger.setMaxDetail(TreeLogger.Type.INFO);
      return new CompositeTreeLogger(logger, fileLogger);
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "unable to open log file: " + compileDir.getLogFile(), e);
      throw new UnableToCompleteException();
    }
  }

  private ModuleDef loadModule(TreeLogger logger, Map<String, String> bindingProperties)
      throws UnableToCompleteException {

    // make sure we get the latest version of any modified jar
    ZipFileClassPathEntry.clearCache();
    ResourceOracleImpl.clearCache();
    ModuleDefLoader.clearModuleCache();

    ResourceLoader resources = ResourceLoaders.forClassLoader(Thread.currentThread());
    resources = ResourceLoaders.forPathAndFallback(options.getSourcePath(), resources);
    this.resourceLoader.set(resources);

    ModuleDef moduleDef = ModuleDefLoader.loadFromResources(
        logger, compilerContext, originalModuleName, resources, true);
    compilerContext = compilerContextBuilder.module(moduleDef).build();

    // We need a cross-site linker. Automatically replace the default linker.
    if (IFrameLinker.class.isAssignableFrom(moduleDef.getActivePrimaryLinker())) {
      moduleDef.addLinker("xsiframe");
    }

    // Check that we have a compatible linker.
    Class<? extends Linker> linker = moduleDef.getActivePrimaryLinker();
    if (! CrossSiteIframeLinker.class.isAssignableFrom(linker)) {
      logger.log(TreeLogger.ERROR,
          "linkers other than CrossSiteIFrameLinker aren't supported. Found: " + linker.getName());
      throw new UnableToCompleteException();
    }

    // Print a nice error if the superdevmode hook isn't present
    if (moduleDef.getProperties().find("devModeRedirectEnabled") == null) {
      throw new RuntimeException("devModeRedirectEnabled isn't set for module: " +
          moduleDef.getName());
    }

    // Disable the redirect hook here to make sure we don't have an infinite loop.
    // (There is another check in the JavaScript, but just in case.)
    overrideConfig(moduleDef, "devModeRedirectEnabled", "false");

    // Turn off "installCode" if it's on because it makes debugging harder.
    // (If it's already off, don't change anything.)
    if (getBooleanConfig(moduleDef, "installCode", true)) {
      overrideConfig(moduleDef, "installCode", "false");
      // Make sure installScriptJs is set to the default for compiling without installCode.
      overrideConfig(moduleDef, "installScriptJs",
          "com/google/gwt/core/ext/linker/impl/installScriptDirect.js");
    }

    // override computeScriptBase.js to enable the "Compile" button
    overrideConfig(moduleDef, "computeScriptBaseJs",
        "com/google/gwt/dev/codeserver/computeScriptBase.js");
    // Fix bug with SDM and Chrome 24+ where //@ sourceURL directives cause X-SourceMap header to be ignored
    // Frustratingly, Chrome won't canonicalize a relative URL
    overrideConfig(moduleDef, "includeSourceMapUrl", "http://" + serverPrefix +
        WebServer.sourceMapLocationForModule(moduleDef.getName()));

    // If present, set some config properties back to defaults.
    // (Needed for Google's server-side linker.)
    maybeOverrideConfig(moduleDef, "includeBootstrapInPrimaryFragment", "false");
    maybeOverrideConfig(moduleDef, "permutationsJs",
        "com/google/gwt/core/ext/linker/impl/permutations.js");
    maybeOverrideConfig(moduleDef, "propertiesJs",
        "com/google/gwt/core/ext/linker/impl/properties.js");

    for (Map.Entry<String, String> entry : bindingProperties.entrySet()) {
      String propName = entry.getKey();
      String propValue = entry.getValue();
      logger.log(TreeLogger.Type.INFO, "binding: " + propName + "=" + propValue);
      maybeSetBinding(moduleDef, propName, propValue);
    }

    overrideBinding(moduleDef, "compiler.useSourceMaps", "true");
    overrideBinding(moduleDef, "superdevmode", "on");
    return moduleDef;
  }

  /**
   * Sets a binding unless it's hard-coded in the GWT application.
   */
  private static void maybeSetBinding(ModuleDef module, String propName, String newValue) {
    Property prop = module.getProperties().find(propName);
    if (prop instanceof BindingProperty) {
      BindingProperty binding = (BindingProperty) prop;

      if (binding.isAllowedValue(newValue)) {
        binding.setAllowedValues(binding.getRootCondition(), newValue);
      }
    }
  }

  /**
   * Sets a binding even if it's set to a different value in the GWT application.
   */
  private static void overrideBinding(ModuleDef module, String propName, String newValue) {
    Property prop = module.getProperties().find(propName);
    if (prop instanceof BindingProperty) {
      BindingProperty binding = (BindingProperty) prop;
      binding.setAllowedValues(binding.getRootCondition(), newValue);
    }
  }

  /**
   * Returns a boolean configuration property. If not defined, returns the default.
   */
  private static boolean getBooleanConfig(ModuleDef module, String propName,
      boolean defaultValue) {
    Property prop = module.getProperties().find(propName);
    if (prop instanceof ConfigurationProperty) {
      ConfigurationProperty config = (ConfigurationProperty) prop;
      String value = config.getValue();
      if (value != null) {
        if (value.equalsIgnoreCase("true")) {
          return true;
        } else if (value.equalsIgnoreCase("false")) {
          return false;
        }
      }
    }
    return defaultValue;
  }

  private static boolean maybeOverrideConfig(ModuleDef module, String propName, String newValue) {
    Property prop = module.getProperties().find(propName);
    if (prop instanceof ConfigurationProperty) {
      ConfigurationProperty config = (ConfigurationProperty) prop;
      config.setValue(newValue);
      return true;
    }
    return false;
  }

  private static void overrideConfig(ModuleDef module, String propName, String newValue) {
    if (!maybeOverrideConfig(module, propName, newValue)) {
      throw new RuntimeException("not found: " + propName);
    }
  }

  private CompileDir makeCompileDir(int compileId)
      throws UnableToCompleteException {
    return CompileDir.create(appSpace.getCompileDir(compileId), logger);
  }
}
