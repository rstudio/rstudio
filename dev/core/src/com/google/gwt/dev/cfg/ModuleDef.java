/*
 * Copyright 2006 Google Inc.
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
import com.google.gwt.dev.util.Empty;
import com.google.gwt.dev.util.FileOracle;
import com.google.gwt.dev.util.FileOracleFactory;
import com.google.gwt.dev.util.Util;
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
 * Represents a module specification.
 */
public class ModuleDef {

  private static final FileFilter JAVA_ACCEPTOR = new FileFilter() {
    public boolean accept(String name) {
      return name.endsWith(".java");
    }
  };

  private static final Comparator REV_NAME_CMP = new Comparator() {
    public int compare(Object o1, Object o2) {
      Map.Entry entry1 = (Entry) o1;
      Map.Entry entry2 = (Entry) o2;
      String key1 = (String) entry1.getKey();
      String key2 = (String) entry2.getKey();
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

  private final ArrayList allCups = new ArrayList();

  private final Set alreadySeenFiles = new HashSet();

  private final CacheManager cacheManager = new CacheManager(".gwt-cache",
      new TypeOracle());

  private CompilationUnitProvider[] cups = new CompilationUnitProvider[0];

  private final List entryPointTypeNames = new ArrayList();

  private final Set gwtXmlFiles = new HashSet();

  private FileOracle lazyPublicOracle;

  private FileOracle lazySourceOracle;

  private TypeOracle lazyTypeOracle;

  private final long moduleDefCreationTime = System.currentTimeMillis();

  private final String name;

  private final Properties properties = new Properties();

  private final FileOracleFactory publicPathEntries = new FileOracleFactory();

  private final Rules rules = new Rules();

  private final Scripts scripts = new Scripts();

  private final Map servletClassNamesByPath = new HashMap();

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

  public synchronized void addPublicPackage(String publicPackage,
      String[] includeList, String[] excludeList, boolean defaultExcludes,
      boolean caseSensitive) {

    if (lazyPublicOracle != null) {
      throw new IllegalStateException("Already normalized");
    }

    /*
     * Hijack Ant's ZipScanner to handle inclusions/exclusions exactly as Ant
     * does. We're only using its pattern-matching capabilities; the code path
     * I'm using never tries to hit the filesystem in Ant 1.6.5.
     */
    final ZipScanner scanner = new ZipScanner();
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

    // index from this package down
    publicPathEntries.addRootPackage(publicPackage, new FileFilter() {
      public boolean accept(String name) {
        return scanner.match(name);
      }
    });
  }

  public synchronized void addSourcePackage(String sourcePackage) {
    if (lazySourceOracle != null) {
      throw new IllegalStateException("Already normalized");
    }

    // index from from the base package
    sourcePathEntries.addPackage(sourcePackage, JAVA_ACCEPTOR);
  }

  public synchronized void addSuperSourcePackage(String superSourcePackage) {
    if (lazySourceOracle != null) {
      throw new IllegalStateException("Already normalized");
    }

    // index from this package down
    sourcePathEntries.addRootPackage(superSourcePackage, JAVA_ACCEPTOR);
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
    Set entrySet = servletClassNamesByPath.entrySet();
    Map.Entry[] entries = (Entry[]) Util.toArray(Map.Entry.class, entrySet);
    Arrays.sort(entries, REV_NAME_CMP);
    for (int i = 0, n = entries.length; i < n; ++i) {
      String mapping = (String) entries[i].getKey();
      /*
       * Ensure that URLs that match the servlet mapping, including those that
       * have additional path_info, get routed to the correct servlet.
       * 
       * See "Inside Servlets", Second Edition, pg. 208
       */
      if (actual.equals(mapping) || actual.startsWith(mapping + "/")) {
        return (String) entries[i].getValue();
      }
    }
    return null;
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
    return (String[]) entryPointTypeNames.toArray(new String[n]);
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
    return (String[]) servletClassNamesByPath.keySet().toArray(Empty.STRINGS);
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

      // Refresh the type oracle.
      //
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
        lazyTypeOracle = builder.build(branch);
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
      if (lazyTypeOracle.findType("java.lang.Object") == null) {
        Util.logMissingTypeErrorWithHints(logger, "java.lang.Object");
        seedTypesMissing = true;
      } else {
        TreeLogger branch = logger.branch(TreeLogger.TRACE,
            "Finding entry point classes", null);
        String[] typeNames = getEntryPointTypeNames();
        for (int i = 0; i < typeNames.length; i++) {
          String typeName = typeNames[i];
          if (lazyTypeOracle.findType(typeName) == null) {
            Util.logMissingTypeErrorWithHints(branch, typeName);
            seedTypesMissing = true;
          }
        }
      }

      if (seedTypesMissing) {
        throw new UnableToCompleteException();
      }
    }

    return lazyTypeOracle;
  }

  public boolean isGwtXmlFileStale() {
    for (Iterator iter = gwtXmlFiles.iterator(); iter.hasNext();) {
      File xmlFile = (File) iter.next();
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

    cacheManager.invalidateVolatileFiles();
    lazyTypeOracle = null;
    normalize(logger);
    getTypeOracle(logger);
    Util.invokeInaccessableMethod(TypeOracle.class, "incrementReloadCount",
        new Class[] {}, lazyTypeOracle, new Object[] {});
  }

  /**
   * The final method to call when everything is setup. Before calling this
   * method, several of the getter methods may not be called. After calling this
   * method, the add methods may not be called.
   * 
   * @param logger Logs the activity.
   */
  synchronized void normalize(TreeLogger logger) {
    // Normalize property providers.
    //
    for (Iterator iter = getProperties().iterator(); iter.hasNext();) {
      Property prop = (Property) iter.next();
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
            prop.setProvider(new DefaultPropertyProvider(prop));
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
      Set files = new HashSet();
      files.addAll(Arrays.asList(allFiles));
      files.removeAll(alreadySeenFiles);
      for (Iterator iter = files.iterator(); iter.hasNext();) {
        String fileName = (String) iter.next();
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
      this.cups = (CompilationUnitProvider[]) allCups.toArray(this.cups);
    }

    // Create the public path.
    //
    branch = Messages.PUBLIC_PATH_LOCATIONS.branch(logger, null);
    lazyPublicOracle = publicPathEntries.create(branch);
  }

}
