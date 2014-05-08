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
package com.google.gwt.dev.resource.impl;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.cfg.ResourceLoader;
import com.google.gwt.dev.cfg.ResourceLoaders;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.resource.ResourceOracle;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.dev.util.msg.Message0;
import com.google.gwt.dev.util.msg.Message1String;
import com.google.gwt.thirdparty.guava.common.base.Joiner;
import com.google.gwt.thirdparty.guava.common.collect.HashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.MapMaker;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.SetMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * The normal implementation of {@link ResourceOracle}.
 */
public class ResourceOracleImpl implements ResourceOracle {

  private static class Messages {
    static final Message1String EXAMINING_PATH_ROOT = new Message1String(TreeLogger.DEBUG,
        "Searching for resources within $0");
    static final Message1String IGNORING_SHADOWED_RESOURCE =
        new Message1String(
            TreeLogger.DEBUG,
            "Resource '$0' is being shadowed by another resource higher in the classpath having the same name; this one will not be used");
    static final Message0 REFRESHING_RESOURCES = new Message0(TreeLogger.TRACE,
        "Refreshing resources");
  }

  /**
   * Wrapper object around a resource to change its path when it is rerooted.
   */
  private static class RerootedResource extends AbstractResource {
    private final String path;
    private final AbstractResource resource;

    public RerootedResource(AbstractResource resource, PathPrefix pathPrefix) {
      this.path = pathPrefix.getRerootedPath(resource.getPath());
      this.resource = resource;
    }

    @Override
    public ClassPathEntry getClassPathEntry() {
      return resource.getClassPathEntry();
    }

    @Override
    public long getLastModified() {
      return resource.getLastModified();
    }

    @Override
    public String getLocation() {
      return resource.getLocation();
    }

    @Override
    public String getPath() {
      return path;
    }

    @Override
    public String getPathPrefix() {
      int fullPathLen = resource.getPath().length();
      return resource.getPath().substring(0, fullPathLen - path.length());
    }

    @Override
    public InputStream openContents() throws IOException {
      return resource.openContents();
    }

    @Override
    public boolean wasRerooted() {
      return true;
    }
  }

  private static class ResourceDescription implements Comparable<ResourceDescription> {
    public final PathPrefix pathPrefix;
    public final AbstractResource resource;

    public ResourceDescription(AbstractResource resource, PathPrefix pathPrefix) {
      this.resource =
          pathPrefix.shouldReroot() ? new RerootedResource(resource, pathPrefix) : resource;
      this.pathPrefix = pathPrefix;
    }

    public boolean isPreferredOver(ResourceDescription that) {
      return this.compareTo(that) > 0;
    }

    @Override
    public int compareTo(ResourceDescription other) {
      // Rerooted takes precedence over not rerooted.
      if (this.resource.wasRerooted() != other.resource.wasRerooted()) {
        return this.resource.wasRerooted() ? 1 : -1;
      }
      // Compare priorities of the path prefixes, high number == high priority.
      return this.pathPrefix.getPriority() - other.pathPrefix.getPriority();
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof ResourceDescription)) {
        return false;
      }
      ResourceDescription other = (ResourceDescription) o;
      return this.pathPrefix.getPriority() == other.pathPrefix.getPriority()
          && this.resource.wasRerooted() == other.resource.wasRerooted();
    }

    @Override
    public int hashCode() {
      return (pathPrefix.getPriority() << 1) + (resource.wasRerooted() ? 1 : 0);
    }

    @Override
    public String toString() {
      return "{" + resource + "," + pathPrefix + "}";
    }
  }

  private static final Map<ResourceLoader, List<ClassPathEntry>> classPathCache =
      new MapMaker().weakKeys().makeMap();

  /**
   * A mapping from resource paths to the name of the library module that
   * created the PathPrefix (usually because of a <source> entry) that made
   * the resource path live.
   * <p>
   * For example com/google/gwt/user/client/DOM.java was made live by the
   * com.google.gwt.user.User library module.
   */
  private SetMultimap<String, String> sourceModulesByTypeSourceName =
      HashMultimap.create();

  private Map<String, String> overlapWarningsByModuleSet = Maps.newHashMap();

  public static void clearCache() {
    classPathCache.clear();
  }

  public static ClassPathEntry createEntryForUrl(TreeLogger logger, URL url)
      throws URISyntaxException, IOException {
    if (url.getProtocol().equals("file")) {
      File f = new File(url.toURI());
      String lowerCaseFileName = f.getName().toLowerCase(Locale.ENGLISH);
      if (f.isDirectory()) {
        return new DirectoryClassPathEntry(f);
      } else if (f.isFile() && lowerCaseFileName.endsWith(".jar")) {
        return ZipFileClassPathEntry.get(f);
      } else if (f.isFile() && lowerCaseFileName.endsWith(".zip")) {
        return ZipFileClassPathEntry.get(f);
      } else {
        // It's a file ending in neither jar nor zip, speculatively try to
        // open as jar/zip anyway.
        try {
          return ZipFileClassPathEntry.get(f);
        } catch (Exception ignored) {
        }
        if (logger.isLoggable(TreeLogger.TRACE)) {
          logger.log(TreeLogger.TRACE, "Unexpected entry in classpath; " + f
              + " is neither a directory nor an archive (.jar or .zip)");
        }
        return null;
      }
    } else {
      logger.log(TreeLogger.WARN, "Unknown URL type for " + url, null);
      return null;
    }
  }

  /**
   * Preinitializes the classpath from the thread default {@link ClassLoader}.
   */
  public static void preload(TreeLogger logger) {
    preload(logger, Thread.currentThread().getContextClassLoader());
  }

  /**
   * Preinitializes the classpath for a given {@link ClassLoader}.
   */
  public static void preload(TreeLogger logger, ClassLoader classLoader) {
    preload(logger, ResourceLoaders.wrap(classLoader));
  }

  /**
   * Preinitializes the classpath for a given {@link ResourceLoader}.
   */
  public static void preload(TreeLogger logger, ResourceLoader resources) {
    Event resourceOracle =
        SpeedTracerLogger.start(CompilerEventType.RESOURCE_ORACLE, "phase", "preload");
    List<ClassPathEntry> entries = getAllClassPathEntries(logger, resources);
    for (ClassPathEntry entry : entries) {
      // We only handle pre-indexing jars, the file system could change.
      if (entry instanceof ZipFileClassPathEntry) {
        ZipFileClassPathEntry zpe = (ZipFileClassPathEntry) entry;
        zpe.index(logger);
      }
    }
    resourceOracle.end();
  }

  /**
   * Returns a mapping from resource paths to the set of names of library
   * modules that created PathPrefixes (usually because of a <source> entry)
   * that made the resource path live.
   * <p>
   * For example com/google/gwt/user/client/DOM.java was made live by the
   * com.google.gwt.user.User library module.
   */
  public SetMultimap<String, String> getSourceModulesByTypeSourceName() {
    return sourceModulesByTypeSourceName;
  }

  /**
   * Print overlapping include warnings that accumulated during resource
   * scanning. Prints only one entry per set of overlapping modules.
   */
  public void printOverlappingModuleIncludeWarnings(TreeLogger logger) {
    for (String overlapWarning : overlapWarningsByModuleSet.values()) {
      logger.log(TreeLogger.WARN, overlapWarning);
    }
  }

  /**
   * Scans the associated paths to recompute the available resources.
   *
   * @param logger status and error details are written here
   */
  public synchronized void scanResources(TreeLogger logger) {
    Event resourceOracle =
        SpeedTracerLogger.start(CompilerEventType.RESOURCE_ORACLE, "phase", "refresh");
    TreeLogger refreshBranch = Messages.REFRESHING_RESOURCES.branch(logger, null);

    Map<String, ResourceDescription> resourceDescriptionsByPath =
        new LinkedHashMap<String, ResourceDescription>();

    for (ClassPathEntry classPathEntry : classPathEntries) {
      TreeLogger branchForClassPathEntry =
          Messages.EXAMINING_PATH_ROOT.branch(refreshBranch, classPathEntry.getLocation(), null);

      Map<AbstractResource, ResourceResolution> prefixesByResource =
          classPathEntry.findApplicableResources(branchForClassPathEntry, pathPrefixSet);
      for (Entry<AbstractResource, ResourceResolution> entry : prefixesByResource.entrySet()) {
        AbstractResource resource = entry.getKey();
        ResourceResolution resourceResolution = entry.getValue();
        ResourceDescription resourceDescription =
            new ResourceDescription(resource, resourceResolution.getPathPrefix());
        String resourcePath = resourceDescription.resource.getPath();
        maybeRecordTypeForModule(resourceResolution, resourcePath);
        maybeRecordOverlapWarning(resourceResolution, resourcePath);

        // In case of collision.
        if (resourceDescriptionsByPath.containsKey(resourcePath)) {
          ResourceDescription oldResourceDescription = resourceDescriptionsByPath.get(resourcePath);
          if (resourceDescription.isPreferredOver(oldResourceDescription)) {
            resourceDescriptionsByPath.put(resourcePath, resourceDescription);
          } else {
            Messages.IGNORING_SHADOWED_RESOURCE.log(branchForClassPathEntry, resourcePath, null);
          }
        } else {
          resourceDescriptionsByPath.put(resourcePath, resourceDescription);
        }
      }
    }

    Map<String, Resource> resourcesByPath = new HashMap<String, Resource>();
    for (Entry<String, ResourceDescription> entry : resourceDescriptionsByPath.entrySet()) {
      resourcesByPath.put(entry.getKey(), entry.getValue().resource);
    }

    // Update exposed collections with new (unmodifiable) data structures.
    exposedResources = Collections.unmodifiableSet(Sets.newHashSet(resourcesByPath.values()));
    exposedResourceMap = Collections.unmodifiableMap(resourcesByPath);
    exposedPathNames = Collections.unmodifiableSet(resourcesByPath.keySet());

    resourceOracle.end();
  }

  private void maybeRecordTypeForModule(ResourceResolution resourceResolution,
      String resourcePath) {
    // If PathPrefix->Module associations are inaccurate because PathPrefixes have been merged.
    if (pathPrefixSet.mergePathPrefixes()) {
      // Then don't record any Type<->Module associations since they won't be accurate;
      return;
    }

    sourceModulesByTypeSourceName.putAll(asTypeSourceName(resourcePath),
        resourceResolution.getSourceModuleNames());
  }

  private String asTypeSourceName(String resourcePath) {
    return resourcePath.replace(".java", "").replace("/", ".");
  }

  private void maybeRecordOverlapWarning(ResourceResolution resourceResolution,
      String resourcePath) {
    // If PathPrefix->Module associations are inaccurate because PathPrefixes have been merged.
    if (pathPrefixSet.mergePathPrefixes()) {
      // Then don't record any overlap warnings since they won't be accurate;
      return;
    }

    if (resourceResolution.getSourceModuleNames().size() > 1) {
      if (!overlapWarningsByModuleSet.containsKey(
          resourceResolution.getSourceModuleNames().toString())) {
        overlapWarningsByModuleSet.put(
            resourceResolution.getSourceModuleNames().toString(), String.format(
                "Resource %s is included by multiple modules (%s).",
                resourcePath, Joiner.on(", ").join(
                    resourceResolution.getSourceModuleNames())));
      }
    }
  }

  private static void addAllClassPathEntries(TreeLogger logger, ResourceLoader loader,
      List<ClassPathEntry> classPath) {
    // URL is expensive in collections, so we use URI instead
    // See:
    // http://michaelscharf.blogspot.com/2006/11/javaneturlequals-and-hashcode-make.html
    Set<URI> seenEntries = new HashSet<URI>();

    for (URL url : loader.getClassPath()) {
      URI uri;
      try {
        uri = url.toURI();
      } catch (URISyntaxException e) {
        logger.log(TreeLogger.WARN, "Error processing classpath URL '" + url + "'", e);
        continue;
      }
      if (seenEntries.contains(uri)) {
        continue;
      }
      seenEntries.add(uri);
      Throwable caught;
      try {
        ClassPathEntry entry = createEntryForUrl(logger, url);
        if (entry != null) {
          classPath.add(entry);
        }
        continue;
      } catch (AccessControlException e) {
        if (logger.isLoggable(TreeLogger.DEBUG)) {
          logger.log(TreeLogger.DEBUG, "Skipping URL due to access restrictions: " + url);
        }
        continue;
      } catch (URISyntaxException e) {
        caught = e;
      } catch (IOException e) {
        caught = e;
      }
      logger.log(TreeLogger.WARN, "Error processing classpath URL '" + url + "'", caught);
    }
  }

  private static synchronized List<ClassPathEntry> getAllClassPathEntries(TreeLogger logger,
      ResourceLoader resources) {
    List<ClassPathEntry> classPath = classPathCache.get(resources);
    if (classPath == null) {
      classPath = new ArrayList<ClassPathEntry>();
      addAllClassPathEntries(logger, resources, classPath);
      classPathCache.put(resources, classPath);
    }
    return classPath;
  }

  private final List<ClassPathEntry> classPathEntries;

  private Set<String> exposedPathNames = Collections.emptySet();

  private Map<String, Resource> exposedResourceMap = Collections.emptyMap();

  private Set<Resource> exposedResources = Collections.emptySet();

  private PathPrefixSet pathPrefixSet = new PathPrefixSet();

  /**
   * Constructs a {@link ResourceOracleImpl} from a set of
   * {@link ClassPathEntry ClassPathEntries}. The list is held by reference and
   * must not be modified.
   */
  public ResourceOracleImpl(List<ClassPathEntry> classPathEntries) {
    this.classPathEntries = classPathEntries;
  }

  /**
   * Constructs a {@link ResourceOracleImpl} from the thread's default
   * {@link ClassLoader}.
   */
  public ResourceOracleImpl(TreeLogger logger) {
    this(logger, Thread.currentThread().getContextClassLoader());
  }

  /**
   * Constructs a {@link ResourceOracleImpl} from a {@link ClassLoader}. The
   * specified {@link ClassLoader} and all of its parents which are instances of
   * {@link java.net.URLClassLoader} will have their class path entries added to this
   * instances underlying class path.
   */
  public ResourceOracleImpl(TreeLogger logger, ClassLoader classLoader) {
    this(logger, ResourceLoaders.wrap(classLoader));
  }

  public ResourceOracleImpl(TreeLogger logger, ResourceLoader resources) {
    this(getAllClassPathEntries(logger, resources));
  }

  @Override
  public void clear() {
    sourceModulesByTypeSourceName.clear();
    overlapWarningsByModuleSet.clear();
    exposedPathNames = Collections.emptySet();
    exposedResourceMap = Collections.emptyMap();
    exposedResources = Collections.emptySet();
  }

  @Override
  public Set<String> getPathNames() {
    return exposedPathNames;
  }

  public PathPrefixSet getPathPrefixes() {
    return pathPrefixSet;
  }

  @Override
  public Map<String, Resource> getResourceMap() {
    return exposedResourceMap;
  }

  @Override
  public Set<Resource> getResources() {
    return exposedResources;
  }

  public void setPathPrefixes(PathPrefixSet pathPrefixSet) {
    this.pathPrefixSet = pathPrefixSet;
  }

  // @VisibleForTesting
  List<ClassPathEntry> getClassPathEntries() {
    return classPathEntries;
  }
}
