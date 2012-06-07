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
import com.google.gwt.dev.CompilerOptions;
import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.cfg.ConfigurationProperty;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.cfg.Property;
import com.google.gwt.dev.cfg.ResourceLoader;
import com.google.gwt.dev.cfg.ResourceLoaders;
import com.google.gwt.dev.javac.CompilationStateBuilder;
import com.google.gwt.dev.resource.impl.ResourceOracleImpl;
import com.google.gwt.dev.resource.impl.ZipFileClassPathEntry;
import com.google.gwt.dev.util.log.CompositeTreeLogger;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Recompiles a GWT module on demand.
 */
class Recompiler {
  private final AppSpace appSpace;
  private final String originalModuleName;
  private final List<File> sourcePath;
  private final TreeLogger logger;
  private int compilesDone = 0;

  // after renaming
  private AtomicReference<String> moduleName = new AtomicReference<String>(null);

  private final AtomicReference<CompileDir> lastBuild = new AtomicReference<CompileDir>();
  private final AtomicReference<ResourceLoader> resourceLoader =
      new AtomicReference<ResourceLoader>();

  Recompiler(AppSpace appSpace, String moduleName, List<File> sourcePath,
      TreeLogger logger) {
    this.appSpace = appSpace;
    this.originalModuleName = moduleName;
    this.sourcePath = sourcePath;
    this.logger = logger;
  }

  synchronized CompileDir compile(Map<String, String> bindingProperties)
      throws UnableToCompleteException {
    if (compilesDone == 0) {
      System.setProperty("java.awt.headless", "true");
      System.setProperty("gwt.speedtracerlog", appSpace.getSpeedTracerLogFile().getAbsolutePath());
      CompilationStateBuilder.init(logger, appSpace.getUnitCacheDir());
    }

    long startTime = System.currentTimeMillis();
    CompileDir compileDir = makeCompileDir(++compilesDone);
    TreeLogger compileLogger = makeCompileLogger(compileDir);

    ModuleDef module = loadModule(compileLogger, bindingProperties);
    String newModuleName = module.getName(); // includes any rename
    moduleName.set(newModuleName);


    CompilerOptions options = new CompilerOptionsImpl(compileDir, newModuleName);

    boolean success = new Compiler(options).run(compileLogger, module);
    lastBuild.set(compileDir);
    if (!success) {
      compileLogger.log(TreeLogger.Type.ERROR, "Compiler returned " + success);
      throw new UnableToCompleteException();
    }

    long elapsedTime = System.currentTimeMillis() - startTime;
    compileLogger.log(TreeLogger.Type.INFO, "Compile completed in " + elapsedTime + " ms");
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
    resources = ResourceLoaders.forPathAndFallback(sourcePath, resources);
    this.resourceLoader.set(resources);
    
    ModuleDef moduleDef =
        ModuleDefLoader.loadFromResources(logger, originalModuleName, resources, true);

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

    // Normally the GWT bootstrap script installs GWT code by calling eval() with a string of
    // JavaScript, so that it can control the scope that the code runs in. Sourcemaps don't seem
    // to be working in Chrome when we do this, so turn it off for now.
    // TODO(cromwellian) remove when Chrome is fixed.
    overrideConfig(moduleDef, "installScriptJs",
        "com/google/gwt/core/ext/linker/impl/installScriptDirect.js");
    overrideConfig(moduleDef, "installCode", "false");

    // override computeScriptBase.js to enable the "Compile" button
    overrideConfig(moduleDef, "computeScriptBaseJs",
        "com/google/gwt/dev/codeserver/computeScriptBase.js");

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
      overrideBinding(moduleDef, propName, propValue);
    }

    overrideBinding(moduleDef, "compiler.useSourceMaps", "true");
    return moduleDef;
  }

  private static void overrideBinding(ModuleDef module, String propName, String newValue) {
    Property prop = module.getProperties().find(propName);
    if (prop instanceof BindingProperty) {
      BindingProperty binding = (BindingProperty) prop;

      if (binding.isAllowedValue(newValue) || "compiler.useSourceMaps".equals(propName)) {
        binding.setAllowedValues(binding.getRootCondition(), newValue);
      }
    }
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
