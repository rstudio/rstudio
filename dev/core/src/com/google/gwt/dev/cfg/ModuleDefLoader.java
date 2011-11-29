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
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.dev.util.xml.ReflectiveParser;
import com.google.gwt.util.tools.Utility;

import org.apache.commons.collections.map.AbstractReferenceMap;
import org.apache.commons.collections.map.ReferenceMap;

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
   * Filename suffix used for GWT Module XML files.
   */
  public static final String GWT_MODULE_XML_SUFFIX = ".gwt.xml";
  
  /**
   * Filename suffix used for Precompiled GWT Module files.
   */
  public static final String COMPILATION_UNIT_ARCHIVE_SUFFIX = ".gwtar";

  /**
   * Keep soft references to loaded modules so the VM can gc them when memory is
   * tight. The current context class loader used as a key for modules cache.
   * The module's physical name is used as a key inside the cache.
   */
  @SuppressWarnings("unchecked")
  private static final Map<ClassLoader, Map<String, ModuleDef>> loadedModulesCaches = new ReferenceMap(
      AbstractReferenceMap.WEAK, AbstractReferenceMap.HARD);

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
   * @param moduleName the synthetic module to create
   * @param inherits a set of modules to inherit from
   * @param refresh whether to refresh the module
   * @return the loaded module
   * @throws UnableToCompleteException
   */
  public static ModuleDef createSyntheticModule(TreeLogger logger,
      String moduleName, final String[] inherits, boolean refresh)
      throws UnableToCompleteException {
    ModuleDef moduleDef = tryGetLoadedModule(moduleName, refresh);
    if (moduleDef != null) {
      return moduleDef;
    }

    ResourceLoader resources = ResourceLoaders.forClassLoader(Thread.currentThread());

    ModuleDefLoader loader = new ModuleDefLoader(resources) {
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
   * Loads a new module from the class path.  Equivalent to
   * {@link #loadFromClassPath(TreeLogger, String, boolean)}.
   *
   * @param logger logs the process
   * @param moduleName the module to load
   * @return the loaded module
   * @throws UnableToCompleteException
   */
  public static ModuleDef loadFromClassPath(TreeLogger logger, String moduleName)
      throws UnableToCompleteException {
    return loadFromClassPath(logger, moduleName, false);
  }

  /**
   * Loads a new module from the class path.
   *
   * @param logger logs the process
   * @param moduleName the module to load
   * @param refresh whether to refresh the module
   * @return the loaded module
   * @throws UnableToCompleteException
   */
  public static ModuleDef loadFromClassPath(TreeLogger logger,
      String moduleName, boolean refresh) throws UnableToCompleteException {

    ResourceLoader resources = ResourceLoaders.forClassLoader(Thread.currentThread());

    return loadFromResources(logger, moduleName, resources, refresh);
  }

  /**
   * Loads a new module from the given ResourceLoader.
   * @param moduleName the module to load
   * @param resources where to look for module.xml and module.gwtar files.
   * @param refresh whether to refresh the module
   * @return the loaded module
   * @throws UnableToCompleteException
   */
  public static ModuleDef loadFromResources(TreeLogger logger, String moduleName,
      ResourceLoader resources, boolean refresh) throws UnableToCompleteException {

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
      ModuleDefLoader loader = new ModuleDefLoader(resources);
      return ModuleDefLoader.doLoadModule(loader, logger, moduleName, resources);
    } finally {
      moduleDefLoadFromClassPathEvent.end();
    }
  }

  /**
   * This method loads a module.
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

    ModuleDef moduleDef = new ModuleDef(moduleName, resources);
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

  @SuppressWarnings("unchecked")
  private static Map<String, ModuleDef> getModulesCache() {
    ClassLoader keyClassLoader = Thread.currentThread().getContextClassLoader();
    Map<String, ModuleDef> cache = loadedModulesCaches.get(keyClassLoader);
    if (cache == null) {
      cache = new ReferenceMap(AbstractReferenceMap.HARD, AbstractReferenceMap.SOFT);
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

  private final ResourceLoader resourceLoader;

  private ModuleDefLoader(ResourceLoader loader) {
    this.resourceLoader = loader;
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

    if (!ModuleDef.isValidModuleName(moduleName)) {
      logger.log(TreeLogger.ERROR, "Invalid module name: '" + moduleName + "'",
          null);
      throw new UnableToCompleteException();
    }
    moduleDef.addInteritedModule(moduleName);

    // Find the specified module using the classpath.
    //
    String slashedModuleName = moduleName.replace('.', '/');
    String resName = slashedModuleName + ModuleDefLoader.GWT_MODULE_XML_SUFFIX;
    URL moduleURL = resourceLoader.getResource(resName);

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
      ModuleDefSchema schema = new ModuleDefSchema(logger, this, moduleName,
          moduleURL, moduleDir, moduleDef);
      ReflectiveParser.parse(logger, schema, r);
    } catch (Throwable e) {
      logger.log(TreeLogger.ERROR, "Unexpected error while processing XML", e);
      throw new UnableToCompleteException();
    } finally {
      Utility.close(r);
    }
  }
}
