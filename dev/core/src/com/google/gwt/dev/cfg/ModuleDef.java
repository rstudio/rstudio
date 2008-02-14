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
import com.google.gwt.core.ext.typeinfo.CompilationUnitProvider;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.jdt.CacheManager;
import com.google.gwt.dev.jdt.TypeOracleBuilder;
import com.google.gwt.dev.jdt.URLCompilationUnitProvider;
import com.google.gwt.dev.linker.Linker;
import com.google.gwt.dev.util.Empty;
import com.google.gwt.dev.util.FileOracle;
import com.google.gwt.dev.util.FileOracleFactory;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.PerfLogger;
import com.google.gwt.dev.util.FileOracleFactory.FileFilter;

import org.apache.tools.ant.types.ZipScanner;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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

  private String[] activeLinkerNames = new String[0];

  private final ArrayList<URLCompilationUnitProvider> allCups = new ArrayList<URLCompilationUnitProvider>();

  private final Set<String> alreadySeenFiles = new HashSet<String>();

  private final CacheManager cacheManager = new CacheManager(".gwt-cache",
      new TypeOracle());

  private CompilationUnitProvider[] cups = new CompilationUnitProvider[0];

  private final List<String> entryPointTypeNames = new ArrayList<String>();

  private final Set<File> gwtXmlFiles = new HashSet<File>();

  private FileOracle lazyPublicOracle;

  private FileOracle lazySourceOracle;

  private TypeOracle lazyTypeOracle;

  private final Map<String, Linker> linkersByName = new HashMap<String, Linker>();

  private final Map<String, String> linkerTypesByName = new HashMap<String, String>();

  private final long moduleDefCreationTime = System.currentTimeMillis();

  private final String name;

  private final Properties properties = new Properties();

  private final FileOracleFactory publicPathEntries = new FileOracleFactory();

  private final Rules rules = new Rules();

  private final Scripts scripts = new Scripts();

  private final Map<String, String> servletClassNamesByPath = new HashMap<String, String>();

  private final FileOracleFactory sourcePathEntries = new FileOracleFactory();

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

  public void addLinker(String name, String className) {
    linkerTypesByName.put(name, className);
  }

  public synchronized void addPublicPackage(String publicPackage,
      String[] includeList, String[] excludeList, boolean defaultExcludes,
      boolean caseSensitive) {

    if (lazyPublicOracle != null) {
      throw new IllegalStateException("Already normalized");
    }

    final ZipScanner scanner = getScanner(includeList, excludeList,
        defaultExcludes, caseSensitive);

    // index from this package down
    publicPathEntries.addRootPackage(publicPackage, new FileFilter() {
      public boolean accept(String name) {
        return scanner.match(name);
      }
    });
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

    final FileFilter sourceFileFilter = new FileFilter() {
      public boolean accept(String name) {
        return scanner.match(name);
      }
    };

    if (isSuperSource) {
      sourcePathEntries.addRootPackage(sourcePackage, sourceFileFilter);
    } else {
      sourcePathEntries.addPackage(sourcePackage, sourceFileFilter);
    }
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

  public synchronized URL findPublicFile(String partialPath) {
    return lazyPublicOracle.find(partialPath);
  }

  public synchronized String findServletForPath(String actual) {
    // Walk in backwards sorted order to find the longest path match first.
    //
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

  public String[] getActiveLinkerNames() {
    return activeLinkerNames;
  }

  public String[] getAllPublicFiles() {
    return lazyPublicOracle.getAllFiles();
  }

  public CacheManager getCacheManager() {
    return cacheManager;
  }

  public synchronized CompilationUnitProvider[] getCompilationUnits() {
    return cups;
  }

  public synchronized String[] getEntryPointTypeNames() {
    final int n = entryPointTypeNames.size();
    return entryPointTypeNames.toArray(new String[n]);
  }

  public synchronized String getFunctionName() {
    return name.replace('.', '_');
  }

  public Linker getLinker(String name) {
    return linkersByName.get(name);
  }

  public synchronized String getName() {
    return name;
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
      lazyTypeOracle = createTypeOracle(logger);
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
    cacheManager.invalidateVolatileFiles();
    normalize(logger);
    lazyTypeOracle = createTypeOracle(logger);
    Util.invokeInaccessableMethod(TypeOracle.class, "incrementReloadCount",
        new Class[] {}, lazyTypeOracle, new Object[] {});
    PerfLogger.end();
  }

  public void setActiveLinkerNames(String... names) {
    activeLinkerNames = names;
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
  synchronized URL findSourceFile(String partialPath) {
    return lazySourceOracle.find(partialPath);
  }

  /**
   * The final method to call when everything is setup. Before calling this
   * method, several of the getter methods may not be called. After calling this
   * method, the add methods may not be called.
   * 
   * @param logger Logs the activity.
   */
  synchronized void normalize(TreeLogger logger)
      throws UnableToCompleteException {
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

    // Create the source path.
    //
    TreeLogger branch = Messages.SOURCE_PATH_LOCATIONS.branch(logger, null);
    lazySourceOracle = sourcePathEntries.create(branch);

    if (lazySourceOracle.isEmpty()) {
      branch.log(TreeLogger.WARN,
          "No source path entries; expect subsequent failures", null);
    } else {
      // Create the CUPs
      String[] allFiles = lazySourceOracle.getAllFiles();
      Set<String> files = new HashSet<String>();
      files.addAll(Arrays.asList(allFiles));
      files.removeAll(alreadySeenFiles);
      for (Iterator<String> iter = files.iterator(); iter.hasNext();) {
        String fileName = iter.next();
        int pos = fileName.lastIndexOf('/');
        String packageName;
        if (pos >= 0) {
          packageName = fileName.substring(0, pos);
          packageName = packageName.replace('/', '.');
        } else {
          packageName = "";
        }
        URL url = lazySourceOracle.find(fileName);
        allCups.add(new URLCompilationUnitProvider(url, packageName));
      }
      alreadySeenFiles.addAll(files);
      this.cups = allCups.toArray(this.cups);
    }

    // Create the public path.
    //
    branch = Messages.PUBLIC_PATH_LOCATIONS.branch(logger, null);
    lazyPublicOracle = publicPathEntries.create(branch);

    /*
     * Validate all linkers before we go off and compile something. badLinker is
     * checked at the end of the method so we can maximize the number of Linkers
     * to report errors about before exiting.
     * 
     * It is not legal to have zero linkers defined
     */
    boolean badLinker = false;
    for (Map.Entry<String, String> entry : linkerTypesByName.entrySet()) {
      try {
        Class<?> clazz = Class.forName(entry.getValue());
        Class<? extends Linker> linkerClazz = clazz.asSubclass(Linker.class);
        linkersByName.put(entry.getKey(), linkerClazz.newInstance());
      } catch (ClassCastException e) {
        logger.log(TreeLogger.ERROR, "Not actually a Linker", e);
        badLinker = true;
      } catch (ClassNotFoundException e) {
        logger.log(TreeLogger.ERROR, "Unable to find Linker", e);
        badLinker = true;
      } catch (InstantiationException e) {
        logger.log(TreeLogger.ERROR, "Unable to create Linker", e);
        badLinker = true;
      } catch (IllegalAccessException e) {
        logger.log(TreeLogger.ERROR,
            "Linker does not have a public constructor", e);
        badLinker = true;
      }
    }

    for (String linkerName : activeLinkerNames) {
      if (!linkersByName.containsKey(linkerName)) {
        logger.log(TreeLogger.ERROR, "Unknown linker name " + linkerName, null);
        badLinker = true;
      }
    }

    if (badLinker) {
      throw new UnableToCompleteException();
    }

    PerfLogger.end();
  }

  private TypeOracle createTypeOracle(TreeLogger logger)
      throws UnableToCompleteException {

    TypeOracle newTypeOracle = null;

    try {
      String msg = "Analyzing source in module '" + name + "'";
      TreeLogger branch = logger.branch(TreeLogger.TRACE, msg, null);
      long before = System.currentTimeMillis();
      TypeOracleBuilder builder = new TypeOracleBuilder(getCacheManager());
      CompilationUnitProvider[] currentCups = getCompilationUnits();
      Arrays.sort(currentCups, CompilationUnitProvider.LOCATION_COMPARATOR);

      TreeLogger subBranch = null;
      if (branch.isLoggable(TreeLogger.DEBUG)) {
        subBranch = branch.branch(TreeLogger.DEBUG,
            "Adding compilation units...", null);
      }

      for (int i = 0; i < currentCups.length; i++) {
        CompilationUnitProvider cup = currentCups[i];
        if (subBranch != null) {
          subBranch.log(TreeLogger.DEBUG, cup.getLocation(), null);
        }
        builder.addCompilationUnit(currentCups[i]);
      }

      if (lazyTypeOracle != null) {
        cacheManager.invalidateOnRefresh(lazyTypeOracle);
      }

      newTypeOracle = builder.build(branch);
      long after = System.currentTimeMillis();
      branch.log(TreeLogger.TRACE, "Finished in " + (after - before) + " ms",
          null);
    } catch (UnableToCompleteException e) {
      logger.log(TreeLogger.ERROR, "Failed to complete analysis", null);
      throw new UnableToCompleteException();
    }

    // Sanity check the seed types and don't even start it they're missing.
    //
    boolean seedTypesMissing = false;
    if (newTypeOracle.findType("java.lang.Object") == null) {
      Util.logMissingTypeErrorWithHints(logger, "java.lang.Object");
      seedTypesMissing = true;
    } else {
      TreeLogger branch = logger.branch(TreeLogger.TRACE,
          "Finding entry point classes", null);
      String[] typeNames = getEntryPointTypeNames();
      for (int i = 0; i < typeNames.length; i++) {
        String typeName = typeNames[i];
        if (newTypeOracle.findType(typeName) == null) {
          Util.logMissingTypeErrorWithHints(branch, typeName);
          seedTypesMissing = true;
        }
      }
    }

    if (seedTypesMissing) {
      throw new UnableToCompleteException();
    }

    return newTypeOracle;
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

}
