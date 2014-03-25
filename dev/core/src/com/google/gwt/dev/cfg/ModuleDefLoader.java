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
package com.google.gwt.dev.cfg;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.CompilerContext;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.resource.impl.UrlResource;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.dev.util.xml.ReflectiveParser;
import com.google.gwt.thirdparty.guava.common.collect.MapMaker;
import com.google.gwt.util.tools.Utility;

import java.io.File;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * The top-level API for loading module XML.
 */
public class ModuleDefLoader {
  /*
   * TODO(scottb,tobyr,zundel): synchronization????
   */

  /**
   * Filename suffix used for Precompiled GWT Module files.
   */
  public static final String COMPILATION_UNIT_ARCHIVE_SUFFIX = ".gwtar";

  /**
   * Filename suffix used for GWT Module XML files.
   */
  public static final String GWT_MODULE_XML_SUFFIX = ".gwt.xml";

  /**
   * Keep soft references to loaded modules so the VM can gc them when memory is
   * tight. The current context class loader used as a key for modules cache.
   * The module's physical name is used as a key inside the cache.
   */
  private static final Map<ClassLoader, Map<String, ModuleDef>> loadedModulesCaches =
      new MapMaker().weakKeys().makeMap();

  /**
   * A mapping from effective to physical module names.
   */
  private static final Map<String, String> moduleEffectiveNameToPhysicalName =
    new HashMap<String, String>();

  public static void clearModuleCache() {
    getModulesCache().clear();
  }

  /**
   * Creates a module in memory that is not associated with a
   * <code>.gwt.xml</code> file on disk.
   *
   * @param logger logs the process
   * @param compilerContext shared read only compiler state
   * @param moduleName the synthetic module to create
   * @param inherits a set of modules to inherit from
   * @param refresh whether to refresh the module
   * @return the loaded module
   * @throws UnableToCompleteException
   */
  public static ModuleDef createSyntheticModule(TreeLogger logger,
      CompilerContext compilerContext, String moduleName, final String[] inherits, boolean refresh)
      throws UnableToCompleteException {
    ModuleDef moduleDef = tryGetLoadedModule(moduleName, refresh);
    if (moduleDef != null) {
      return moduleDef;
    }

    ResourceLoader resources = ResourceLoaders.forClassLoader(Thread.currentThread());

    ModuleDefLoader loader = new ModuleDefLoader(compilerContext, resources) {
      @Override
      protected void load(TreeLogger logger, String nameOfModuleToLoad, ModuleDef dest)
          throws UnableToCompleteException {
        logger.log(TreeLogger.TRACE, "Loading module '" + nameOfModuleToLoad + "'");
        for (String inherit : inherits) {
          nestedLoad(logger, inherit, dest);
        }
      }
    };

    ModuleDef module = doLoadModule(loader, logger, moduleName, resources);
    /*
     * Must reset name override on synthetic modules. Otherwise they'll be
     * incorrectly affected by the last inherits tag, because they have no XML
     * which would reset the name at the end of parse.
     */
    module.setNameOverride(null);
    return module;
  }

  /**
   * Loads a new module from the class path and defers scanning associated directories for
   * resources.
   */
  public static ModuleDef loadFromClassPath(
      TreeLogger logger, CompilerContext compilerContext, String moduleName)
      throws UnableToCompleteException {
    return loadFromClassPath(logger, compilerContext, moduleName, false);
  }

  /**
   * Loads a new module from the class path and may or may not immediately scan associated
   * directories for resources.
   */
  public static ModuleDef loadFromClassPath(
      TreeLogger logger, CompilerContext compilerContext, String moduleName, boolean refresh)
      throws UnableToCompleteException {
    ResourceLoader resources = ResourceLoaders.forClassLoader(Thread.currentThread());
    return loadFromResources(logger, compilerContext, moduleName, resources, refresh);
  }

  /**
   * Loads a new module from the given ResourceLoader and may or may not immediately scan associated
   * directories for resources.
   */
  public static ModuleDef loadFromResources(TreeLogger logger, CompilerContext compilerContext,
      String moduleName, ResourceLoader resources, boolean refresh)
      throws UnableToCompleteException {

    Event moduleDefLoadFromClassPathEvent = SpeedTracerLogger.start(
        CompilerEventType.MODULE_DEF, "phase", "loadFromClassPath", "moduleName", moduleName);
    try {
      // Look up the module's physical name; if null, we are either encountering
      // the module for the first time, or else the name is already physical
      String physicalName = moduleEffectiveNameToPhysicalName.get(moduleName);
      if (physicalName != null) {
        moduleName = physicalName;
      }
      ModuleDef moduleDef = tryGetLoadedModule(moduleName, refresh);
      if (moduleDef != null) {
        return moduleDef;
      }
      ModuleDefLoader loader = new ModuleDefLoader(compilerContext, resources);
      ModuleDef module = ModuleDefLoader.doLoadModule(
          loader, logger, moduleName, resources, compilerContext.shouldCompileMonolithic());

      LibraryWriter libraryWriter = compilerContext.getLibraryWriter();
      libraryWriter.setLibraryName(module.getCanonicalName());
      libraryWriter.addDependencyLibraryNames(module.getExternalLibraryCanonicalModuleNames());

      // Records binding property defined values that were newly created in this library.
      for (BindingProperty bindingProperty : module.getProperties().getBindingProperties()) {
        libraryWriter.addNewBindingPropertyValuesByName(
            bindingProperty.getName(), bindingProperty.getTargetLibraryDefinedValues());
      }
      // Records configuration property value changes that occurred in this library.
      for (ConfigurationProperty configurationProperty :
          module.getProperties().getConfigurationProperties()) {
        libraryWriter.addNewConfigurationPropertyValuesByName(
            configurationProperty.getName(), configurationProperty.getTargetLibraryValues());
      }
      // Saves non java and gwt.xml build resources, like PNG and CSS files.
      for (Resource buildResource : module.getBuildResourceOracle().getResources()) {
        if (buildResource.getPath().endsWith(".java")
            || buildResource.getPath().endsWith(".gwt.xml")) {
          continue;
        }
        libraryWriter.addBuildResource(buildResource);
      }
      // Saves public resources.
      for (Resource publicResource : module.getPublicResourceOracle().getResources()) {
        libraryWriter.addPublicResource(publicResource);
      }

      return module;
    } finally {
      moduleDefLoadFromClassPathEvent.end();
    }
  }

  /**
   * This method loads a module while assuming it is monolithic.
   *
   * @param loader the loader to use
   * @param logger used to log the loading process
   * @param moduleName the name of the module
   * @param resources where to load source code from
   * @return the module returned -- cannot be null
   * @throws UnableToCompleteException if module loading failed
   */
  private static ModuleDef doLoadModule(ModuleDefLoader loader, TreeLogger logger,
      String moduleName, ResourceLoader resources)
      throws UnableToCompleteException {
    return doLoadModule(loader, logger, moduleName, resources, true);
  }

  /**
   * This method loads a module.
   *
   * @param loader the loader to use
   * @param logger used to log the loading process
   * @param moduleName the name of the module
   * @param resources where to load source code from
   * @param monolithic whether to encapsulate the entire module tree
   * @return the module returned -- cannot be null
   * @throws UnableToCompleteException if module loading failed
   */
  private static ModuleDef doLoadModule(ModuleDefLoader loader, TreeLogger logger,
      String moduleName, ResourceLoader resources, boolean monolithic)
      throws UnableToCompleteException {

    ModuleDef moduleDef = new ModuleDef(moduleName, resources, monolithic);
    Event moduleLoadEvent = SpeedTracerLogger.start(CompilerEventType.MODULE_DEF,
        "phase", "strategy.load()");
    loader.load(logger, moduleName, moduleDef);
    moduleLoadEvent.end();

    // Do any final setup.
    //
    Event moduleNormalizeEvent = SpeedTracerLogger.start(CompilerEventType.MODULE_DEF,
        "phase", "moduleDef.normalize()");
    moduleDef.normalize(logger);
    moduleNormalizeEvent.end();

    // Add the "physical" module name: com.google.Module
    getModulesCache().put(moduleName, moduleDef);

    // Add a mapping from the module's effective name to its physical name
    moduleEffectiveNameToPhysicalName.put(moduleDef.getName(), moduleName);
    return moduleDef;
  }

  static Map<String, ModuleDef> getModulesCache() {
    ClassLoader keyClassLoader = Thread.currentThread().getContextClassLoader();
    Map<String, ModuleDef> cache = loadedModulesCaches.get(keyClassLoader);
    if (cache == null) {
      cache = new MapMaker().softValues().makeMap();
      loadedModulesCaches.put(keyClassLoader, cache);
    }
    return cache;
  }

  private static ModuleDef tryGetLoadedModule(String moduleName, boolean refresh) {
    ModuleDef moduleDef = getModulesCache().get(moduleName);
    if (moduleDef == null || moduleDef.isGwtXmlFileStale()) {
      return null;
    } else if (refresh) {
      moduleDef.refresh();
    }
    return moduleDef;
  }

  private final CompilerContext compilerContext;

  private final ResourceLoader resourceLoader;

  private ModuleDefLoader(CompilerContext compilerContext, ResourceLoader loader) {
    this.compilerContext = compilerContext;
    this.resourceLoader = loader;
  }

  public boolean enforceStrictResources() {
    return compilerContext.getOptions().enforceStrictResources();
  }

  /**
   * Loads a module and all its included modules, recursively, into the given ModuleDef.
   * @throws UnableToCompleteException
   */
  protected void load(TreeLogger logger, String nameOfModuleToLoad, ModuleDef dest)
      throws UnableToCompleteException {
    nestedLoad(logger, nameOfModuleToLoad, dest);
  }

  /**
   * Loads a new module and its descendants into <code>moduleDef</code> as included modules.
   * (If there are any descendants, this method will be called recursively.)
   *
   * @param parentLogger Logs the process.
   * @param moduleName The module to load.
   * @param moduleDef The module to add the new module to.
   * @throws UnableToCompleteException
   */
  void nestedLoad(TreeLogger parentLogger, String moduleName, ModuleDef moduleDef)
      throws UnableToCompleteException {
    if (moduleDef.isInherited(moduleName)) {
      // No need to parse module again.
      return;
    }

    TreeLogger logger = parentLogger.branch(TreeLogger.DEBUG, "Loading inherited module '"
        + moduleName + "'", null);
    LibraryWriter libraryWriter = compilerContext.getLibraryWriter();
    LibraryGroup libraryGroup = compilerContext.getLibraryGroup();

    if (!ModuleDef.isValidModuleName(moduleName)) {
      logger.log(TreeLogger.ERROR, "Invalid module name: '" + moduleName + "'",
          null);
      throw new UnableToCompleteException();
    }
    moduleDef.addInheritedModules(moduleName);

    // Find the specified module using the classpath.
    //
    String slashedModuleName = moduleName.replace('.', '/');
    String resName = slashedModuleName + ModuleDefLoader.GWT_MODULE_XML_SUFFIX;
    URL moduleURL = resourceLoader.getResource(resName);
    if (moduleURL == null && libraryGroup.containsBuildResource(resName)) {
      moduleURL = libraryGroup.getBuildResourceByPath(resName).getURL();
    }

    long lastModified = 0;
    if (moduleURL != null) {
      String externalForm = moduleURL.toExternalForm();
      if (logger.isLoggable(TreeLogger.DEBUG)) {
        logger.log(TreeLogger.DEBUG, "Module location: " + externalForm, null);
      }
      try {
        if ((!(externalForm.startsWith("jar:file")))
            && (!(externalForm.startsWith("zip:file")))
            && (!(externalForm.startsWith("http://")))
            && (!(externalForm.startsWith("ftp://")))) {
          File gwtXmlFile = new File(moduleURL.toURI());
          lastModified = gwtXmlFile.lastModified();
          moduleDef.addGwtXmlFile(gwtXmlFile);
        }
      } catch (URISyntaxException e) {
        logger.log(TreeLogger.ERROR, "Error parsing URI", e);
        throw new UnableToCompleteException();
      }
      String compilationUnitArchiveName = slashedModuleName + ModuleDefLoader.COMPILATION_UNIT_ARCHIVE_SUFFIX;
      URL compiledModuleURL = resourceLoader.getResource(compilationUnitArchiveName);
      if (compiledModuleURL != null) {
        moduleDef.addCompilationUnitArchiveURL(compiledModuleURL);
      }
    }
    if (moduleURL == null) {
      logger.log(TreeLogger.ERROR,"Unable to find '" + resName + "' on your classpath; "
          + "could be a typo, or maybe you forgot to include a classpath entry for source?");
      throw new UnableToCompleteException();
    }

    // Extract just the directory containing the module.
    //
    String moduleDir = "";
    int i = slashedModuleName.lastIndexOf('/');
    if (i != -1) {
      moduleDir = slashedModuleName.substring(0, i) + "/";
    }

    // Parse it.
    //
    Reader r = null;
    try {
      r = Util.createReader(logger, moduleURL);
      ModuleDefSchema schema =
          new ModuleDefSchema(logger, this, moduleName, moduleURL, moduleDir, moduleDef);
      ReflectiveParser.parse(logger, schema, r);

      // If this module.gwt.xml file is one of the target modules that together make up this
      // ModuleDef.
      if (moduleDef.getTargetLibraryCanonicalModuleNames().contains(moduleName)) {
        // Then save a copy of the xml file in the created library file.
        libraryWriter.addBuildResource(new UrlResource(moduleURL, resName, lastModified));
      }
    } catch (Throwable e) {
      logger.log(TreeLogger.ERROR, "Unexpected error while processing XML", e);
      throw new UnableToCompleteException();
    } finally {
      Utility.close(r);
    }
  }
}
