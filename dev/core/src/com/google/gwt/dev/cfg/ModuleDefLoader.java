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
import com.google.gwt.dev.util.xml.ReflectiveParser;
import com.google.gwt.util.tools.Utility;

import java.io.File;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * The top-level API for loading module XML.
 */
public class ModuleDefLoader {

  // Should always be true. If it is false complete type oracle analysis and
  // bytecode cache reload will occur on each reload.
  private static boolean enableCachingModules = true;
  private static final Set forceInherits = new HashSet();
  private static final Map loadedModules = new HashMap();

  /**
   * Forces all modules loaded via subsequent calls to
   * {@link #loadFromClassPath(TreeLogger, String)} to automatically inherit the
   * specified module. If this method is called multiple times, the order of
   * inclusion is unspecified.
   * 
   * @param moduleName The module all subsequently loaded modules should inherit
   *          from.
   */
  public static void forceInherit(String moduleName) {
    forceInherits.add(moduleName);
  }

  /**
   * Gets whether module caching is enabled.
   * 
   * @return <code>true</code> if module cachine is enabled, otherwise
   *         <code>false</code>.
   */
  public static boolean getEnableCachingModules() {
    return enableCachingModules;
  }

  public static ModuleDef loadFromClassPath(TreeLogger logger, String moduleName)
      throws UnableToCompleteException {
    return loadFromClassPath(logger, moduleName, true);
  }

  /**
   * Loads a new module from the class path.
   * 
   * @param logger Logs the process.
   * @param moduleName The module to load.
   * @return The loaded module.
   * @throws UnableToCompleteException
   */
  public static ModuleDef loadFromClassPath(TreeLogger logger,
      String moduleName, boolean refresh) throws UnableToCompleteException {
    ModuleDef moduleDef = (ModuleDef) loadedModules.get(moduleName);
    if (moduleDef == null || moduleDef.isGwtXmlFileStale()) {
      moduleDef = new ModuleDefLoader().load(logger, moduleName);
      if (enableCachingModules) {
        loadedModules.put(moduleName, moduleDef);
      }
    } else {
      if (refresh) {
        moduleDef.refresh(logger);
      }
    }
    return moduleDef;
  }

  /**
   * Enables or disables caching loaded modules for subsequent requests.
   * 
   * @param enableCachingModules If <code>true</code> subsequent calls to
   *          {@link #loadFromClassPath(TreeLogger, String)} will cause the
   *          resulting module to be cached. If <code>false</code> such
   *          modules will not be cached.
   */
  public static void setEnableCachingModules(boolean enableCachingModules) {
    ModuleDefLoader.enableCachingModules = enableCachingModules;
  }

  private final Set alreadyLoadedModules = new HashSet();

  private final ClassLoader classLoader;

  private ModuleDefLoader() {
    this.classLoader = Thread.currentThread().getContextClassLoader();
  }

  /**
   * Loads a new module into <code>moduleDef</code> as an included module.
   * 
   * @param logger Logs the process.
   * @param moduleName The module to load.
   * @param moduleDef The module to add the new module to.
   * @throws UnableToCompleteException
   */
  void nestedLoad(TreeLogger logger, String moduleName, ModuleDef moduleDef)
      throws UnableToCompleteException {

    if (alreadyLoadedModules.contains(moduleName)) {
      logger.log(TreeLogger.TRACE, "Module '" + moduleName
          + "' has already been loaded and will be skipped", null);
      return;
    } else {
      alreadyLoadedModules.add(moduleName);
    }

    // Find the specified module using the classpath.
    //
    String slashedModuleName = moduleName.replace('.', '/');
    String resName = slashedModuleName + ".gwt.xml";
    URL moduleURL = classLoader.getResource(resName);

    if (moduleURL != null) {
      String externalForm = moduleURL.toExternalForm();
      logger.log(TreeLogger.TRACE, "Module location: " + externalForm, null);
      try {
        if ((!(externalForm.startsWith("jar:file")))
            && (!(externalForm.startsWith("zip:file")))
            && (!(externalForm.startsWith("http://")))
            && (!(externalForm.startsWith("ftp://")))) {
          File gwtXmlFile = new File(new URI(externalForm));
          moduleDef.addGwtXmlFile(gwtXmlFile);
        }
      } catch (URISyntaxException e) {
        logger.log(TreeLogger.ERROR, "Error parsing URI", e);
        throw new UnableToCompleteException();
      }
    }
    if (moduleURL == null) {
      String msg = "Unable to find '"
          + resName
          + "' on your classpath; could be a typo, or maybe you forgot to include a classpath entry for source?";
      logger.log(TreeLogger.ERROR, msg, null);
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
      ModuleDefSchema schema = new ModuleDefSchema(logger, this, moduleURL,
          moduleDir, moduleDef);
      ReflectiveParser.parse(logger, schema, r);
    } finally {
      Utility.close(r);
    }
  }

  /**
   * 
   * This method loads a module.
   * 
   * @param logger used to log the loading process
   * @param moduleName the name of the module
   * @return the module returned -- cannot be null
   * @throws UnableToCompleteException if module loading failed.
   */
  private ModuleDef load(TreeLogger logger, String moduleName)
      throws UnableToCompleteException {
    logger = logger.branch(TreeLogger.TRACE, "Loading module '" + moduleName
        + "'", null);

    if (!ModuleDef.isValidModuleName(moduleName)) {
      logger.log(TreeLogger.ERROR, "Invalid module name: '" + moduleName + "'",
          null);
      throw new UnableToCompleteException();
    }

    ModuleDef moduleDef = new ModuleDef(moduleName);
    for (Iterator it = forceInherits.iterator(); it.hasNext();) {
      String forceInherit = (String) it.next();
      TreeLogger branch = logger.branch(TreeLogger.TRACE,
          "Loading forceably inherited module '" + forceInherit + "'", null);
      nestedLoad(branch, forceInherit, moduleDef);
    }
    nestedLoad(logger, moduleName, moduleDef);

    // Do any final setup.
    //
    moduleDef.normalize(logger);

    return moduleDef;
  }
}
