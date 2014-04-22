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
import com.google.gwt.dev.CompilerContext;
import com.google.gwt.dev.javac.CompilationProblemReporter;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationStateBuilder;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.resource.ResourceOracle;
import com.google.gwt.dev.resource.impl.DefaultFilters;
import com.google.gwt.dev.resource.impl.PathPrefix;
import com.google.gwt.dev.resource.impl.PathPrefixSet;
import com.google.gwt.dev.resource.impl.ResourceFilter;
import com.google.gwt.dev.resource.impl.ResourceOracleImpl;
import com.google.gwt.dev.util.Empty;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.thirdparty.guava.common.base.Objects;
import com.google.gwt.thirdparty.guava.common.base.Preconditions;
import com.google.gwt.thirdparty.guava.common.base.Predicates;
import com.google.gwt.thirdparty.guava.common.collect.ArrayListMultimap;
import com.google.gwt.thirdparty.guava.common.collect.HashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableList;
import com.google.gwt.thirdparty.guava.common.collect.Iterators;
import com.google.gwt.thirdparty.guava.common.collect.ListMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;
import com.google.gwt.thirdparty.guava.common.collect.Queues;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

/**
 * Represents a module specification. In principle, this could be built without
 * XML for unit tests.
 */
public class ModuleDef {
  /**
   * Marks a module in a way that can be used to calculate the effective bounds of a library module
   * in a module tree.
   */
  public enum ModuleType {
    /**
     * A module that acts as just a bundle of source code to be included in a referencing library.
     * The contained code may form circular references with other source within the contiguous chunk
     * of modules made up of a library and it's subtree of filesets. As a result a fileset can not
     * be compiled on it's own and must be compiled as part of a library.
     */
    FILESET,
    /**
     * The default type of module. Libraries should be independently compilable and are expected to
     * explicitly declare all of their immediate dependencies and include paths and none of their
     * contained source should be part of a circular reference with any source outside the effective
     * bounds of the library.
     */
    LIBRARY
  }

  /**
   * When walking a module tree of mixed library and fileset modules, attributes are accumulated and
   * stored. But when compiling separately, some attributes should only be collected from the
   * effective target module. AttributeSource is used to classify these "in target module" and "out
   * of target module" times.
   */
  private enum AttributeSource {
    EXTERNAL_LIBRARY, TARGET_LIBRARY
  }

  /**
   * Encapsulates two String names which conceptually represent the starting and ending points of a
   * path.
   * <p>
   * For example in the following module dependency path ["com.acme.MyApp",
   * "com.acme.widgets.Widgets", "com.google.gwt.user.User"] the fromName is "com.acme.MyApp" and
   * the toName is "com.google.gwt.user.User" and together they form a PathEndNames instance.
   * <p>
   * The purpose of the class is to serve as a key by which to lookup the fileset path that connects
   * two libraries. It makes it possible to insert fileset entries into a circular module reference
   * report.
   */
  private static class LibraryDependencyEdge {

    private String fromName;
    private String toName;

    public LibraryDependencyEdge(String fromName, String toName) {
      this.fromName = fromName;
      this.toName = toName;
    }

    @Override
    public boolean equals(Object object) {
      if (object instanceof LibraryDependencyEdge) {
        LibraryDependencyEdge that = (LibraryDependencyEdge) object;
        return Objects.equal(this.fromName, that.fromName)
            && Objects.equal(this.toName, that.toName);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(fromName, toName);
    }
  }

  private static final ResourceFilter NON_JAVA_RESOURCES = new ResourceFilter() {
    @Override
    public boolean allows(String path) {
      return !path.endsWith(".java") && !path.endsWith(".class");
    }
  };

  private static final Comparator<Map.Entry<String, ?>> REV_NAME_CMP =
      new Comparator<Map.Entry<String, ?>>() {
        @Override
        public int compare(Map.Entry<String, ?> entry1, Map.Entry<String, ?> entry2) {
          String key1 = entry1.getKey();
          String key2 = entry2.getKey();
          // intentionally reversed
          return key2.compareTo(key1);
        }
      };

  public static boolean isValidModuleName(String moduleName) {
    // Check for an empty string between two periods.
    if (moduleName.contains("..")) {
      return false;
    }
    // Insure the package name components are a valid Java ident.
    String[] parts = moduleName.split("\\.");
    for (int i = 0; i < parts.length - 1; i++) {
      String part = parts[i];
      if (!Util.isValidJavaIdent(part)) {
        return false;
      }
    }
    return true;
  }

  private static LinkedList<String> createExtendedCopy(LinkedList<String> list,
      String extendingElement) {
    LinkedList<String> extendedCopy = Lists.newLinkedList(list);
    extendedCopy.add(extendingElement);
    return extendedCopy;
  }

  /**
   * All resources found on the public path, specified by <public> directives in
   * modules (or the implicit ./public directory). Marked 'lazy' because it does not
   * start searching for resources until a query is made.
   */
  protected ResourceOracleImpl lazyPublicOracle;

  private final Set<String> activeLinkers = new LinkedHashSet<String>();

  private String activePrimaryLinker;

  /**
   * A set of URLs for <module>.gwtar files found on the classpath that correspond
   * to <module>.gwt.xml files loaded as a part of this module's nested load.
   *
   * @see com.google.gwt.dev.CompileModule
   */
  private final List<URL> archiveURLs = new ArrayList<URL>();

  private boolean collapseAllProperties;

  /**
   * Used to keep track of the current category of attribute source. When the attribute source is
   * TARGET_LIBRARY all attributes are accumulated but when it is EXTERNAL_LIBRARY there are some
   * attributes that are not collected.
   */
  private Deque<AttributeSource> currentAttributeSource = Lists.newLinkedList();

  private final DefaultFilters defaultFilters;

  /**
   * The canonical module names of dependency library modules per depending library module.
   */
  private Multimap<String, String> directDependencyModuleNamesByModuleName = HashMultimap.create();

  private final List<String> entryPointTypeNames = new ArrayList<String>();

  /**
   * Names of free-standing compilable library modules that are depended upon by this module.
   */
  private final Set<String> externalLibraryCanonicalModuleNames = Sets.newLinkedHashSet();

  /**
   * Records the canonical module names for filesets.
   */
  private Set<String> filesetModuleNames = Sets.newHashSet();

  /**
   * A mapping from a pair of starting and ending module names of a path to the ordered list of
   * names of fileset modules that connect those end points. The mapping allows one to discover what
   * path of filesets are connecting a pair of libraries when those libraries do not directly depend
   * on one another. Multiple paths may exist; this map contains only one of them.
   */
  private ListMultimap<LibraryDependencyEdge, String> filesetPathPerEdge =
      ArrayListMultimap.create();

  private final Set<File> gwtXmlFiles = new HashSet<File>();

  private final Set<String> inheritedModules = new HashSet<String>();

  /**
   * Contains files other than .java and .class files (such as CSS and PNG files) from either the
   * source or resource paths.
   */
  private ResourceOracleImpl lazyResourcesOracle;

  /**
   * Contains all files from the source path, specified by <source> and <super>
   * directives in modules (or the implicit ./client directory).
   */
  private ResourceOracleImpl lazySourceOracle;

  private final Map<String, Class<? extends Linker>> linkerTypesByName =
      new LinkedHashMap<String, Class<? extends Linker>>();

  private final long moduleDefCreationTime = System.currentTimeMillis();

  /**
   * Whether the module tree is being considered to be one contiguous compilable whole as opposed to
   * a tree of independently compilable library modules.
   */
  private final boolean monolithic;

  private final String name;

  /**
   * Must use a separate field to track override, because setNameOverride() will
   * get called every time a module is inherited, but only the last one matters.
   */
  private String nameOverride;

  private final Properties properties = new Properties();

  private PathPrefixSet publicPrefixSet = new PathPrefixSet();

  private Set<PathPrefix> resourcePrefixes = Sets.newHashSet();

  private final ResourceLoader resources;

  private boolean resourcesScanned;

  private final Rules rules = new Rules();

  private final Scripts scripts = new Scripts();

  private final Map<String, String> servletClassNamesByPath = new HashMap<String, String>();

  private PathPrefixSet sourcePrefixSet = new PathPrefixSet();

  private final Styles styles = new Styles();

  /**
   * Canonical names of non-independently-compilable modules (mostly filesets) that together make up
   * the current module.
   */
  private final Set<String> targetLibraryCanonicalModuleNames = Sets.newLinkedHashSet();

  public ModuleDef(String name) {
    this(name, ResourceLoaders.forClassLoader(Thread.currentThread()));
  }

  public ModuleDef(String name, ResourceLoader resources) {
    this(name, resources, true);
  }

  /**
   * Constructs a ModuleDef.
   */
  public ModuleDef(String name, ResourceLoader resources, boolean monolithic) {
    this.name = name;
    this.resources = resources;
    this.monolithic = monolithic;
    defaultFilters = new DefaultFilters();
  }

  /**
   * Register a {@code dependencyModuleName} as a directDependency of {@code currentModuleName}
   */
  public void addDirectDependency(String currentModuleName, String dependencyModuleName) {
    directDependencyModuleNamesByModuleName.put(currentModuleName, dependencyModuleName);
  }

  public synchronized void addEntryPointTypeName(String typeName) {
    if (!attributeIsForTargetLibrary()) {
      return;
    }
    entryPointTypeNames.add(typeName);
  }

  /**
   * Registers a module as a fileset.
   */
  public void addFileset(String filesetModuleName) {
    filesetModuleNames.add(filesetModuleName);
  }

  public void addGwtXmlFile(File xmlFile) {
    gwtXmlFiles.add(xmlFile);
  }

  public void addLinker(String name) {
    Class<? extends Linker> clazz = getLinker(name);
    assert clazz != null;

    LinkerOrder order = clazz.getAnnotation(LinkerOrder.class);
    if (order.value() == Order.PRIMARY) {
      if (activePrimaryLinker != null) {
        activeLinkers.remove(activePrimaryLinker);
      }
      activePrimaryLinker = name;
    }

    activeLinkers.add(name);
  }

  public synchronized void addPublicPackage(String publicPackage, String[] includeList,
      String[] excludeList, String[] skipList, boolean defaultExcludes, boolean caseSensitive) {
    if (lazyPublicOracle != null) {
      throw new IllegalStateException("Already normalized");
    }
    if (!attributeIsForTargetLibrary()) {
      return;
    }
    publicPrefixSet.add(new PathPrefix(publicPackage, defaultFilters.customResourceFilter(
        includeList, excludeList, skipList, defaultExcludes, caseSensitive), true, excludeList));
  }

  public void addResourcePath(String resourcePath) {
    if (lazyResourcesOracle != null) {
      throw new IllegalStateException("Already normalized");
    }
    if (!attributeIsForTargetLibrary()) {
      return;
    }
    resourcePrefixes.add(new PathPrefix(resourcePath, NON_JAVA_RESOURCES));
  }

  public void addRule(Rule rule) {
    if (!attributeIsForTargetLibrary()) {
      return;
    }
    getRules().prepend(rule);
  }

  public void addSourcePackage(String sourcePackage, String[] includeList, String[] excludeList,
      String[] skipList, boolean defaultExcludes, boolean caseSensitive) {
    addSourcePackageImpl(sourcePackage, includeList, excludeList, skipList, defaultExcludes,
        caseSensitive, false);
  }

  public void addSourcePackageImpl(String sourcePackage, String[] includeList,
      String[] excludeList, String[] skipList, boolean defaultExcludes, boolean caseSensitive,
      boolean isSuperSource) {
    if (lazySourceOracle != null) {
      throw new IllegalStateException("Already normalized");
    }
    if (!attributeIsForTargetLibrary()) {
      return;
    }
    PathPrefix pathPrefix =
        new PathPrefix(sourcePackage, defaultFilters.customJavaFilter(includeList, excludeList,
            skipList, defaultExcludes, caseSensitive), isSuperSource, excludeList);
    sourcePrefixSet.add(pathPrefix);
  }

  public void addSuperSourcePackage(String superSourcePackage, String[] includeList,
      String[] excludeList, String[] skipList, boolean defaultExcludes, boolean caseSensitive) {
    addSourcePackageImpl(superSourcePackage, includeList, excludeList, skipList, defaultExcludes,
        caseSensitive, true);
  }

  /**
   * Free up memory no longer needed in later compile stages. After calling this
   * method, the ResourceOracle will be empty and unusable. Calling
   * {@link #ensureResourcesScanned()} will restore them.
   */
  public synchronized void clear() {
    if (lazySourceOracle != null) {
      lazySourceOracle.clear();
    }
    rules.dispose();
  }

  public void clearEntryPoints() {
    entryPointTypeNames.clear();
  }

  /**
   * Associate a Linker class with a symbolic name. If the name had been
   * previously assigned, this method will redefine the name. If the redefined
   * linker had been previously added to the set of active linkers, the old
   * active linker will be replaced with the new linker.
   */
  public void defineLinker(TreeLogger logger, String name, Class<? extends Linker> linker)
      throws UnableToCompleteException {
    Class<? extends Linker> old = getLinker(name);
    if (old != null) {
      // Redefining an existing name
      if (activePrimaryLinker.equals(name)) {
        // Make sure the new one is also a primary linker
        if (!linker.getAnnotation(LinkerOrder.class).value().equals(Order.PRIMARY)) {
          logger.log(TreeLogger.ERROR, "Redefining primary linker " + name
              + " with non-primary implementation " + linker.getName());
          throw new UnableToCompleteException();
        }

      } else if (activeLinkers.contains(name)) {
        // Make sure it's a not a primary linker
        if (linker.getAnnotation(LinkerOrder.class).value().equals(Order.PRIMARY)) {
          logger.log(TreeLogger.ERROR, "Redefining non-primary linker " + name
              + " with primary implementation " + linker.getName());
          throw new UnableToCompleteException();
        }
      }
    }
    linkerTypesByName.put(name, linker);
  }

  /**
   * Called as module tree parsing enters and exits individual module file of various types and so
   * provides the ModuleDef with an opportunity to keep track of what context it is currently
   * operating.<br />
   *
   * At the moment the ModuleDef uses it's monolithic property in combination with the entering
   * modules ModuleType to updates its idea of attribute source.
   */
  public void enterModule(ModuleType moduleType, String canonicalModuleName) {
    if (moduleType == ModuleType.FILESET) {
      addFileset(canonicalModuleName);
    }
    if (monolithic) {
      // When you're monolithic the module tree is all effectively one giant library.
      currentAttributeSource.push(AttributeSource.TARGET_LIBRARY);
      targetLibraryCanonicalModuleNames.add(canonicalModuleName);
      return;
    }

    // When compiling separately it's not legal to have a fileset module at the root.
    Preconditions.checkArgument(
        !(currentAttributeSource.isEmpty() && moduleType == ModuleType.FILESET));

    // The compilation target always starts at the root of the graph so if there is a library type
    // module at the root of the graph.
    if (currentAttributeSource.isEmpty() && moduleType == ModuleType.LIBRARY) {
      // Then any attributes you see are your own.
      currentAttributeSource.push(AttributeSource.TARGET_LIBRARY);
      targetLibraryCanonicalModuleNames.add(canonicalModuleName);
      return;
    }

    // If at the moment any attributes you see are your own.
    if (currentAttributeSource.peek() == AttributeSource.TARGET_LIBRARY) {
      // And you enter a fileset module
      if (moduleType == ModuleType.FILESET) {
        // Then any attributes you see are still your own.
        currentAttributeSource.push(AttributeSource.TARGET_LIBRARY);
        targetLibraryCanonicalModuleNames.add(canonicalModuleName);
      } else {
        // But if you enter a library module then any attributes you see are external.
        currentAttributeSource.push(AttributeSource.EXTERNAL_LIBRARY);
        addExternalLibraryCanonicalModuleName(canonicalModuleName);
      }
    } else if (currentAttributeSource.peek() == AttributeSource.EXTERNAL_LIBRARY) {
      // If your current attribute source is an external library then regardless of whether you
      // enter another fileset or library your attribute source is still external.
      currentAttributeSource.push(AttributeSource.EXTERNAL_LIBRARY);
    }
  }

  public void exitModule() {
    currentAttributeSource.pop();
  }

  public synchronized Resource findPublicFile(String partialPath) {
    ensureResourcesScanned();
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

  /**
   * Returns the Resource for a source file if it is found; <code>null</code>
   * otherwise.
   *
   * @param partialPath the partial path of the source file
   * @return the resource for the requested source file
   */
  public synchronized Resource findSourceFile(String partialPath) {
    ensureResourcesScanned();
    return lazySourceOracle.getResourceMap().get(partialPath);
  }

  public Set<String> getActiveLinkerNames() {
    return new LinkedHashSet<String>(activeLinkers);
  }

  public Set<Class<? extends Linker>> getActiveLinkers() {
    Set<Class<? extends Linker>> toReturn = new LinkedHashSet<Class<? extends Linker>>();
    for (String linker : activeLinkers) {
      assert linkerTypesByName.containsKey(linker) : linker;
      toReturn.add(linkerTypesByName.get(linker));
    }
    return toReturn;
  }

  public Class<? extends Linker> getActivePrimaryLinker() {
    assert linkerTypesByName.containsKey(activePrimaryLinker) : activePrimaryLinker;
    return linkerTypesByName.get(activePrimaryLinker);
  }

  /**
   * Returns URLs to fetch archives of precompiled compilation units.
   *
   * @see com.google.gwt.dev.CompileModule
   */
  public Collection<URL> getAllCompilationUnitArchiveURLs() {
    return Collections.unmodifiableCollection(archiveURLs);
  }

  /**
   * Strictly for statistics gathering. There is no guarantee that the source
   * oracle has been initialized.
   */
  public String[] getAllSourceFiles() {
    ensureResourcesScanned();
    return lazySourceOracle.getPathNames().toArray(Empty.STRINGS);
  }

  public synchronized ResourceOracle getBuildResourceOracle() {
    if (lazyResourcesOracle == null) {
      lazyResourcesOracle = new ResourceOracleImpl(TreeLogger.NULL, resources);
      PathPrefixSet pathPrefixes = lazySourceOracle.getPathPrefixes();

      // For backwards compatibility, register source resource paths as build resource paths as
      // well.
      PathPrefixSet newPathPrefixes = new PathPrefixSet();
      for (PathPrefix pathPrefix : pathPrefixes.values()) {
        newPathPrefixes.add(
            new PathPrefix(pathPrefix.getPrefix(), NON_JAVA_RESOURCES, pathPrefix.shouldReroot()));
      }

      // Register build resource paths.
      for (PathPrefix resourcePathPrefix : resourcePrefixes) {
        newPathPrefixes.add(resourcePathPrefix);
      }
      lazyResourcesOracle.setPathPrefixes(newPathPrefixes);
      lazyResourcesOracle.scanResources(TreeLogger.NULL);
    } else {
      ensureResourcesScanned();
    }
    return lazyResourcesOracle;
  }

  /**
   * Returns the physical name for the module by which it can be found in the
   * classpath.
   */
  public String getCanonicalName() {
    return name;
  }

  public CompilationState getCompilationState(TreeLogger logger, CompilerContext compilerContext)
      throws UnableToCompleteException {
    ensureResourcesScanned();
    CompilationState compilationState = CompilationStateBuilder.buildFrom(
        logger, compilerContext, compilerContext.getSourceResourceOracle().getResources());
    checkForSeedTypes(logger, compilationState);
    return compilationState;
  }

  /**
   * The module names for its direct non fileset dependents.
   */
  public Collection<String> getDirectDependencies(String libraryModuleName) {
    assert !filesetModuleNames.contains(libraryModuleName);
    return directDependencyModuleNamesByModuleName.get(libraryModuleName);
  }

  public synchronized String[] getEntryPointTypeNames() {
    final int n = entryPointTypeNames.size();
    return entryPointTypeNames.toArray(new String[n]);
  }

  /**
   * Returns the names of free-standing compilable library modules that are depended upon by this
   * module.
   */
  public Set<String> getExternalLibraryCanonicalModuleNames() {
    return externalLibraryCanonicalModuleNames;
  }

  public List<String> getFileSetPathBetween(String fromModuleName, String toModuleName) {
    return filesetPathPerEdge.get(new LibraryDependencyEdge(fromModuleName, toModuleName));
  }

  public synchronized String getFunctionName() {
    return getName().replace('.', '_');
  }

  public List<Rule> getGeneratorRules() {
    return ImmutableList.copyOf(
        Iterators.filter(rules.iterator(), Predicates.instanceOf(RuleGenerateWith.class)));
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

  public ResourceOracleImpl getPublicResourceOracle() {
    ensureResourcesScanned();
    return lazyPublicOracle;
  }

  public long getResourceLastModified() {
    long resourceLastModified = 1000;
    Set<Resource> allResources = Sets.newHashSet();
    allResources.addAll(getPublicResourceOracle().getResources());
    allResources.addAll(getSourceResourceOracle().getResources());
    allResources.addAll(getBuildResourceOracle().getResources());
    for (Resource resource : allResources) {
      resourceLastModified = Math.max(resourceLastModified, resource.getLastModified());
    }
    return resourceLastModified;
  }

  public Set<Resource> getResourcesNewerThan(long modificationTime) {
    Set<Resource> newerResources = Sets.newHashSet();

    for (Resource resource : getPublicResourceOracle().getResources()) {
      if (resource.getLastModified() > modificationTime) {
        newerResources.add(resource);
      }
    }

    for (Resource resource : getSourceResourceOracle().getResources()) {
      if (resource.getLastModified() > modificationTime) {
        newerResources.add(resource);
      }
    }

    for (Resource resource : getBuildResourceOracle().getResources()) {
      if (resource.getLastModified() > modificationTime) {
        newerResources.add(resource);
      }
    }

    return newerResources;
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

  public synchronized ResourceOracle getSourceResourceOracle() {
    ensureResourcesScanned();
    return lazySourceOracle;
  }

  /**
   * Gets a reference to the internal styles list for this module def.
   */
  public Styles getStyles() {
    return styles;
  }

  /**
   * Returns the names of non-independently-compilable modules (mostly filesets) that together make
   * up the current module.
   */
  public Set<String> getTargetLibraryCanonicalModuleNames() {
    return targetLibraryCanonicalModuleNames;
  }

  public boolean isGwtXmlFileStale() {
    return lastModified() > moduleDefCreationTime;
  }

  public boolean isInherited(String moduleName) {
    return inheritedModules.contains(moduleName);
  }

  public long lastModified() {
    long lastModified = 0;
    for (File xmlFile : gwtXmlFiles) {
      if (xmlFile.exists()) {
        lastModified = Math.max(lastModified, xmlFile.lastModified());
      }
    }
    return lastModified > 0 ? lastModified : moduleDefCreationTime;
  }

  /**
   * For convenience in unit tests, servlets can be automatically loaded and
   * mapped in the embedded web server. If a servlet is already mapped to the
   * specified path, it is replaced.
   *
   * @param path the url path at which the servlet resides
   * @param servletClassName the name of the servlet to publish
   */
  public synchronized void mapServlet(String path, String servletClassName) {
    servletClassNamesByPath.put(path, servletClassName);
  }

  public synchronized void refresh() {
    resourcesScanned = false;
  }

  /**
   * Mainly for testing and decreasing compile times.
   */
  public void setCollapseAllProperties(boolean collapse) {
    collapseAllProperties = collapse;
  }

  /**
   * Override the module's apparent name. Setting this value to
   * <code>null<code>will disable the name override.
   */
  public synchronized void setNameOverride(String nameOverride) {
    this.nameOverride = nameOverride;
  }

  void addBindingPropertyDefinedValue(BindingProperty bindingProperty, String token) {
    if (attributeIsForTargetLibrary()) {
      bindingProperty.addTargetLibraryDefinedValue(bindingProperty.getRootCondition(), token);
    } else {
      bindingProperty.addDefinedValue(bindingProperty.getRootCondition(), token);
    }
  }

  void addCompilationUnitArchiveURL(URL url) {
    archiveURLs.add(url);
  }

  void addConfigurationPropertyValue(ConfigurationProperty configurationProperty, String value) {
    if (attributeIsForTargetLibrary()) {
      configurationProperty.addTargetLibraryValue(value);
    } else {
      configurationProperty.addValue(value);
    }
  }

  void addInheritedModules(String moduleName) {
    inheritedModules.add(moduleName);
  }

  /**
   * The final method to call when everything is setup. Before calling this
   * method, several of the getter methods may not be called. After calling this
   * method, the add methods may not be called.
   *
   * @param logger Logs the activity.
   */
  synchronized void normalize(TreeLogger logger) {
    Event moduleDefNormalize =
        SpeedTracerLogger.start(CompilerEventType.MODULE_DEF, "phase", "normalize");

    // Compute compact dependency graph;
    computeLibraryDependencyGraph();

    // Normalize property providers.
    //
    for (Property current : getProperties()) {
      if (current instanceof BindingProperty) {
        BindingProperty prop = (BindingProperty) current;

        if (collapseAllProperties) {
          prop.addCollapsedValues("*");
        }

        prop.normalizeCollapsedValues();

        /*
         * Create a default property provider for any properties with more than
         * one possible value and no existing provider.
         */
        if (prop.getProvider() == null && prop.getConstrainedValue() == null) {
          String src = "{";
          src += "return __gwt_getMetaProperty(\"";
          src += prop.getName();
          src += "\"); }";
          prop.setProvider(new PropertyProvider(src));
        }
      }
    }

    // Create the public path.
    TreeLogger branch = Messages.PUBLIC_PATH_LOCATIONS.branch(logger, null);
    lazyPublicOracle = new ResourceOracleImpl(branch, resources);
    lazyPublicOracle.setPathPrefixes(publicPrefixSet);

    // Create the source path.
    branch = Messages.SOURCE_PATH_LOCATIONS.branch(logger, null);
    lazySourceOracle = new ResourceOracleImpl(branch, resources);
    lazySourceOracle.setPathPrefixes(sourcePrefixSet);

    moduleDefNormalize.end();
  }

  void setConfigurationPropertyValue(ConfigurationProperty configurationProperty,
      String value) {
    if (attributeIsForTargetLibrary()) {
      configurationProperty.setTargetLibraryValue(value);
    } else {
      configurationProperty.setValue(value);
    }
  }

  private void addExternalLibraryCanonicalModuleName(String externalLibraryCanonicalModuleName) {
    // Ignore circular dependencies on self.
    if (externalLibraryCanonicalModuleName.equals(getCanonicalName())) {
      return;
    }
    externalLibraryCanonicalModuleNames.add(externalLibraryCanonicalModuleName);
  }

  private boolean attributeIsForTargetLibrary() {
    return currentAttributeSource.isEmpty()
        || currentAttributeSource.peek() == AttributeSource.TARGET_LIBRARY;
  }

  private void checkForSeedTypes(TreeLogger logger, CompilationState compilationState)
      throws UnableToCompleteException {
    // Sanity check the seed types and don't even start it they're missing.
    boolean seedTypesMissing = false;
    TypeOracle typeOracle = compilationState.getTypeOracle();
    if (typeOracle.findType("java.lang.Object") == null) {
      CompilationProblemReporter.logMissingTypeErrorWithHints(logger, "java.lang.Object",
          compilationState);
      seedTypesMissing = true;
    } else {
      TreeLogger branch = logger.branch(TreeLogger.TRACE, "Finding entry point classes", null);
      String[] typeNames = getEntryPointTypeNames();
      for (int i = 0; i < typeNames.length; i++) {
        String typeName = typeNames[i];
        if (typeOracle.findType(typeName) == null) {
          CompilationProblemReporter.logMissingTypeErrorWithHints(branch, typeName,
              compilationState);
          seedTypesMissing = true;
        }
      }
    }

    if (seedTypesMissing) {
      throw new UnableToCompleteException();
    }
  }

  /**
   * Reduce the direct dependency graph to exclude filesets.
   */
  private void computeLibraryDependencyGraph() {
    for (String moduleName : Lists.newArrayList(directDependencyModuleNamesByModuleName.keySet())) {
      Set<String> libraryModules = Sets.newHashSet();
      Set<String> filesetsProcessed = Sets.newHashSet();

      // Direct dependents might be libraries or fileset, so add them to a queue of modules
      // to process.
      Queue<LinkedList<String>> modulePathsToProcess = Queues.newArrayDeque();
      Collection<String> directDependencyModuleNames =
          directDependencyModuleNamesByModuleName.get(moduleName);
      for (String directDependencyModuleName : directDependencyModuleNames) {
        modulePathsToProcess.add(Lists.newLinkedList(ImmutableList.of(directDependencyModuleName)));
      }
      while (!modulePathsToProcess.isEmpty()) {
        LinkedList<String> dependentModuleNamePath = modulePathsToProcess.poll();
        String dependentModuleName = dependentModuleNamePath.getLast();

        boolean isLibrary = !filesetModuleNames.contains(dependentModuleName);
        if (isLibrary) {
          // Current dependent is not a fileset so it will be in the list of dependents unless it is
          // the library itself.
          if (!moduleName.equals(dependentModuleName)) {
            libraryModules.add(dependentModuleName);

            dependentModuleNamePath.removeLast();
            filesetPathPerEdge.putAll(new LibraryDependencyEdge(moduleName, dependentModuleName),
                dependentModuleNamePath);
          }
          continue;
        }
        // Mark the module as processed to avoid an infinite loop.
        filesetsProcessed.add(dependentModuleName);

        // Get the dependencies of the dependent module under consideration and add all those
        // that have not been already processed to the queue of modules to process.
        Set<String> unProcessedModules =
            Sets.newHashSet(directDependencyModuleNamesByModuleName.get(dependentModuleName));
        unProcessedModules.removeAll(filesetsProcessed);
        for (String unProcessedModule : unProcessedModules) {
          modulePathsToProcess.add(createExtendedCopy(dependentModuleNamePath, unProcessedModule));
        }
      }
      // Rewrite the dependents with the set just computed.
      directDependencyModuleNamesByModuleName.replaceValues(moduleName, libraryModules);
    }
    // Remove all fileset entries.
    directDependencyModuleNamesByModuleName.removeAll(filesetModuleNames);
  }

  private synchronized void ensureResourcesScanned() {
    if (resourcesScanned) {
      return;
    }
    resourcesScanned = true;

    Event moduleDefEvent = SpeedTracerLogger.start(
        CompilerEventType.MODULE_DEF, "phase", "refresh", "module", getName());
    if (lazyResourcesOracle != null) {
      lazyResourcesOracle.scanResources(TreeLogger.NULL);
    }
    lazyPublicOracle.scanResources(TreeLogger.NULL);
    lazySourceOracle.scanResources(TreeLogger.NULL);
    moduleDefEvent.end();
  }
}
