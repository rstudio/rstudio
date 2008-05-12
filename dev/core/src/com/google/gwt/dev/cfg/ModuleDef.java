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

import com.google.gwt.core.ext.Linker;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.LinkerOrder;
import com.google.gwt.core.ext.linker.LinkerOrder.Order;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.JavaSourceFile;
import com.google.gwt.dev.javac.JavaSourceOracle;
import com.google.gwt.dev.javac.impl.JavaSourceOracleImpl;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.resource.impl.PathPrefix;
import com.google.gwt.dev.resource.impl.PathPrefixSet;
import com.google.gwt.dev.resource.impl.ResourceFilter;
import com.google.gwt.dev.resource.impl.ResourceOracleImpl;
import com.google.gwt.dev.util.Empty;
import com.google.gwt.dev.util.PerfLogger;
import com.google.gwt.dev.util.Util;

import org.apache.tools.ant.types.ZipScanner;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Represents a module specification. In principle, this could be built without
 * XML for unit tests.
 */
public class ModuleDef implements PublicOracle {
  /**
   * Default to recursive inclusion of java files if no explicit include
   * directives are specified.
   */
  private static final String[] DEFAULT_SOURCE_FILE_INCLUDES_LIST = new String[] {"**/*.java"};

  private static final Comparator<Map.Entry<String, ?>> REV_NAME_CMP = new Comparator<Map.Entry<String, ?>>() {
    public int compare(Map.Entry<String, ?> entry1, Map.Entry<String, ?> entry2) {
      String key1 = entry1.getKey();
      String key2 = entry2.getKey();
      // intentionally reversed
      return key2.compareTo(key1);
    }
  };

  public static boolean isValidModuleName(String moduleName) {
    String[] parts = moduleName.split("\\.");
    for (int i = 0; i < parts.length; i++) {
      String part = parts[i];
      if (!Util.isValidJavaIdent(part)) {
        return false;
      }
    }
    return true;
  }

  private final Set<Class<? extends Linker>> activeLinkers = new LinkedHashSet<Class<? extends Linker>>();

  private Class<? extends Linker> activePrimaryLinker;

  private CompilationState compilationState;

  private final List<String> entryPointTypeNames = new ArrayList<String>();

  private final Set<File> gwtXmlFiles = new HashSet<File>();

  private JavaSourceOracle lazyJavaSourceOracle;

  private ResourceOracleImpl lazyPublicOracle;

  private ResourceOracleImpl lazySourceOracle;

  private TypeOracle lazyTypeOracle;

  private final Map<String, Class<? extends Linker>> linkerTypesByName = new LinkedHashMap<String, Class<? extends Linker>>();

  private final long moduleDefCreationTime = System.currentTimeMillis();

  private final String name;

  /**
   * Must use a separate field to track override, because setNameOverride() will
   * get called every time a module is inherited, but only the last one matters.
   */
  private String nameOverride;

  private final Properties properties = new Properties();

  private PathPrefixSet publicPrefixSet = new PathPrefixSet();

  private final Rules rules = new Rules();

  private final Scripts scripts = new Scripts();

  private final Map<String, String> servletClassNamesByPath = new HashMap<String, String>();

  private PathPrefixSet sourcePrefixSet = new PathPrefixSet();

  private final Styles styles = new Styles();

  public ModuleDef(String name) {
    this.name = name;
  }

  public synchronized void addEntryPointTypeName(String typeName) {
    entryPointTypeNames.add(typeName);
  }

  public void addGwtXmlFile(File xmlFile) {
    gwtXmlFiles.add(xmlFile);
  }

  public void addLinker(String name) {
    Class<? extends Linker> clazz = getLinker(name);
    assert clazz != null;

    LinkerOrder order = clazz.getAnnotation(LinkerOrder.class);
    if (order.value() == Order.PRIMARY) {
      activePrimaryLinker = clazz;
    } else {
      activeLinkers.add(clazz);
    }
  }

  public synchronized void addPublicPackage(String publicPackage,
      String[] includeList, String[] excludeList, boolean defaultExcludes,
      boolean caseSensitive) {

    if (lazyPublicOracle != null) {
      throw new IllegalStateException("Already normalized");
    }

    final ZipScanner scanner = getScanner(includeList, excludeList,
        defaultExcludes, caseSensitive);

    publicPrefixSet.add(new PathPrefix(publicPackage, new ResourceFilter() {
      public boolean allows(String path) {
        return scanner.match(path);
      }
    }, true));
  }

  public void addSourcePackage(String sourcePackage, String[] includeList,
      String[] excludeList, boolean defaultExcludes, boolean caseSensitive) {
    addSourcePackageImpl(sourcePackage, includeList, excludeList,
        defaultExcludes, caseSensitive, false);
  }

  public void addSourcePackageImpl(String sourcePackage, String[] includeList,
      String[] excludeList, boolean defaultExcludes, boolean caseSensitive,
      boolean isSuperSource) {
    if (lazySourceOracle != null) {
      throw new IllegalStateException("Already normalized");
    }

    if (includeList.length == 0) {
      /*
       * If no includes list was provided then, use the default.
       */
      includeList = DEFAULT_SOURCE_FILE_INCLUDES_LIST;
    }

    final ZipScanner scanner = getScanner(includeList, excludeList,
        defaultExcludes, caseSensitive);

    ResourceFilter sourceFileFilter = new ResourceFilter() {
      public boolean allows(String path) {
        return path.endsWith(".java") && scanner.match(path);
      }
    };

    PathPrefix pathPrefix = new PathPrefix(sourcePackage, sourceFileFilter,
        isSuperSource);
    sourcePrefixSet.add(pathPrefix);
  }

  public void addSuperSourcePackage(String superSourcePackage,
      String[] includeList, String[] excludeList, boolean defaultExcludes,
      boolean caseSensitive) {
    addSourcePackageImpl(superSourcePackage, includeList, excludeList,
        defaultExcludes, caseSensitive, true);
  }

  public void clearEntryPoints() {
    entryPointTypeNames.clear();
  }

  public void defineLinker(String name, Class<? extends Linker> linker) {
    linkerTypesByName.put(name, linker);
  }

  public synchronized Resource findPublicFile(String partialPath) {
    return lazyPublicOracle.getResourceMap().get(partialPath);
  }

  public synchronized String findServletForPath(String actual) {
    // Walk in backwards sorted order to find the longest path match first.
    Set<Entry<String, String>> entrySet = servletClassNamesByPath.entrySet();
    Entry<String, String>[] entries = Util.toArray(Entry.class, entrySet);
    Arrays.sort(entries, REV_NAME_CMP);
    for (int i = 0, n = entries.length; i < n; ++i) {
      String mapping = entries[i].getKey();
      /*
       * Ensure that URLs that match the servlet mapping, including those that
       * have additional path_info, get routed to the correct servlet.
       * 
       * See "Inside Servlets", Second Edition, pg. 208
       */
      if (actual.equals(mapping) || actual.startsWith(mapping + "/")) {
        return entries[i].getValue();
      }
    }
    return null;
  }

  public Set<Class<? extends Linker>> getActiveLinkers() {
    return activeLinkers;
  }

  public Class<? extends Linker> getActivePrimaryLinker() {
    return activePrimaryLinker;
  }

  public String[] getAllPublicFiles() {
    return lazyPublicOracle.getPathNames().toArray(Empty.STRINGS);
  }

  /**
   * Returns the physical name for the module by which it can be found in the
   * classpath.
   */
  public String getCanonicalName() {
    return name;
  }

  public CompilationState getCompilationState() {
    return compilationState;
  }

  public synchronized String[] getEntryPointTypeNames() {
    final int n = entryPointTypeNames.size();
    return entryPointTypeNames.toArray(new String[n]);
  }

  public synchronized String getFunctionName() {
    return getName().replace('.', '_');
  }

  public Class<? extends Linker> getLinker(String name) {
    return linkerTypesByName.get(name);
  }

  public Map<String, Class<? extends Linker>> getLinkers() {
    return linkerTypesByName;
  }

  public synchronized String getName() {
    return nameOverride != null ? nameOverride : name;
  }

  /**
   * The properties that have been defined.
   */
  public synchronized Properties getProperties() {
    return properties;
  }

  /**
   * Gets a reference to the internal rules for this module def.
   */
  public synchronized Rules getRules() {
    return rules;
  }

  /**
   * Gets a reference to the internal scripts list for this module def.
   */
  public Scripts getScripts() {
    return scripts;
  }

  public synchronized String[] getServletPaths() {
    return servletClassNamesByPath.keySet().toArray(Empty.STRINGS);
  }

  /**
   * Gets a reference to the internal styles list for this module def.
   */
  public Styles getStyles() {
    return styles;
  }

  public synchronized TypeOracle getTypeOracle(TreeLogger logger)
      throws UnableToCompleteException {
    if (lazyTypeOracle == null) {
      lazyTypeOracle = compilationState.getTypeOracle();
      updateTypeOracle(logger);
    }
    return lazyTypeOracle;
  }

  public boolean isGwtXmlFileStale() {
    for (Iterator<File> iter = gwtXmlFiles.iterator(); iter.hasNext();) {
      File xmlFile = iter.next();
      if ((!xmlFile.exists())
          || (xmlFile.lastModified() > moduleDefCreationTime)) {
        return true;
      }
    }
    return false;
  }

  /**
   * For convenience in hosted mode, servlets can be automatically loaded and
   * delegated to via {@link com.google.gwt.dev.shell.GWTShellServlet}. If a
   * servlet is already mapped to the specified path, it is replaced.
   * 
   * @param path the url path at which the servlet resides
   * @param servletClassName the name of the servlet to publish
   */
  public synchronized void mapServlet(String path, String servletClassName) {
    servletClassNamesByPath.put(path, servletClassName);
  }

  public synchronized void refresh(TreeLogger logger)
      throws UnableToCompleteException {
    PerfLogger.start("ModuleDef.refresh");

    // Refresh resource oracles.
    lazyPublicOracle.refresh(logger);
    lazySourceOracle.refresh(logger);

    // Update the compilation state to reflect the resource oracle changes.
    compilationState.refresh();

    // Refresh type oracle if needed.
    if (lazyTypeOracle != null) {
      updateTypeOracle(logger);
    }
    PerfLogger.end();
  }

  /**
   * Override the module's apparent name. Setting this value to
   * <code>null<code> will disable the name override.
   */
  public void setNameOverride(String nameOverride) {
    this.nameOverride = nameOverride;
  }

  /**
   * Returns the URL for a source file if it is found; <code>false</code>
   * otherwise.
   * 
   * NOTE: this method is for testing only.
   * 
   * @param partialPath
   * @return
   */
  synchronized JavaSourceFile findSourceFile(String partialPath) {
    return lazyJavaSourceOracle.getSourceMap().get(partialPath);
  }

  /**
   * The final method to call when everything is setup. Before calling this
   * method, several of the getter methods may not be called. After calling this
   * method, the add methods may not be called.
   * 
   * @param logger Logs the activity.
   */
  synchronized void normalize(TreeLogger logger) {
    PerfLogger.start("ModuleDef.normalize");

    // Normalize property providers.
    //
    for (Iterator<Property> iter = getProperties().iterator(); iter.hasNext();) {
      Property prop = iter.next();
      if (prop.getActiveValue() == null) {
        // If there are more than one possible values, then create a provider.
        // Otherwise, pretend the one value is an active value.
        //
        String[] knownValues = prop.getKnownValues();
        assert (knownValues.length > 0);
        if (knownValues.length > 1) {
          if (prop.getProvider() == null) {
            // Create a default provider.
            //
            prop.setProvider(new DefaultPropertyProvider(this, prop));
          }
        } else {
          prop.setActiveValue(knownValues[0]);
        }
      }
    }

    // Create the public path.
    TreeLogger branch = Messages.PUBLIC_PATH_LOCATIONS.branch(logger, null);
    // lazyPublicOracle = publicPathEntries.create(branch);
    if (lazyPublicOracle == null) {
      lazyPublicOracle = new ResourceOracleImpl(branch);
      lazyPublicOracle.setPathPrefixes(publicPrefixSet);
    }
    lazyPublicOracle.refresh(branch);

    // Create the source path.
    branch = Messages.SOURCE_PATH_LOCATIONS.branch(logger, null);
    lazySourceOracle = new ResourceOracleImpl(branch);
    lazySourceOracle.setPathPrefixes(sourcePrefixSet);
    lazySourceOracle.refresh(branch);
    if (lazySourceOracle.getResources().isEmpty()) {
      branch.log(TreeLogger.WARN,
          "No source path entries; expect subsequent failures", null);
    }
    lazyJavaSourceOracle = new JavaSourceOracleImpl(lazySourceOracle);

    // Create the compilation state.
    compilationState = new CompilationState(lazyJavaSourceOracle);

    PerfLogger.end();
  }

  private ZipScanner getScanner(String[] includeList, String[] excludeList,
      boolean defaultExcludes, boolean caseSensitive) {
    /*
     * Hijack Ant's ZipScanner to handle inclusions/exclusions exactly as Ant
     * does. We're only using its pattern-matching capabilities; the code path
     * I'm using never tries to hit the filesystem in Ant 1.6.5.
     */
    ZipScanner scanner = new ZipScanner();
    if (includeList.length > 0) {
      scanner.setIncludes(includeList);
    }
    if (excludeList.length > 0) {
      scanner.setExcludes(excludeList);
    }
    if (defaultExcludes) {
      scanner.addDefaultExcludes();
    }
    scanner.setCaseSensitive(caseSensitive);
    scanner.init();

    return scanner;
  }

  private void updateTypeOracle(TreeLogger logger)
      throws UnableToCompleteException {
    PerfLogger.start("ModuleDef.updateTypeOracle");
    TreeLogger branch = logger.branch(TreeLogger.TRACE,
        "Compiling Java source files in module '" + getName() + "'");
    compilationState.compile(branch);
    PerfLogger.end();

    TypeOracle typeOracle = compilationState.getTypeOracle();

    // Sanity check the seed types and don't even start it they're missing.
    boolean seedTypesMissing = false;
    if (typeOracle.findType("java.lang.Object") == null) {
      Util.logMissingTypeErrorWithHints(logger, "java.lang.Object");
      seedTypesMissing = true;
    } else {
      branch = logger.branch(TreeLogger.TRACE, "Finding entry point classes",
          null);
      String[] typeNames = getEntryPointTypeNames();
      for (int i = 0; i < typeNames.length; i++) {
        String typeName = typeNames[i];
        if (typeOracle.findType(typeName) == null) {
          Util.logMissingTypeErrorWithHints(branch, typeName);
          seedTypesMissing = true;
        }
      }
    }

    if (seedTypesMissing) {
      throw new UnableToCompleteException();
    }
  }

}
