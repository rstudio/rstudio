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

import org.apache.commons.collections.map.AbstractReferenceMap;
import org.apache.commons.collections.map.ReferenceMap;

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

  private static class ResourceData implements Comparable<ResourceData> {
    public final PathPrefix pathPrefix;
    public final AbstractResource resource;

    public ResourceData(AbstractResource resource, PathPrefix pathPrefix) {
      this.resource =
          pathPrefix.shouldReroot() ? new RerootedResource(resource, pathPrefix) : resource;
      this.pathPrefix = pathPrefix;
    }

    @Override
    public int compareTo(ResourceData other) {
      // Rerooted takes precedence over not rerooted.
      if (this.resource.wasRerooted() != other.resource.wasRerooted()) {
        return this.resource.wasRerooted() ? 1 : -1;
      }
      // Compare priorities of the path prefixes, high number == high priority.
      return this.pathPrefix.getPriority() - other.pathPrefix.getPriority();
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof ResourceData)) {
        return false;
      }
      ResourceData other = (ResourceData) o;
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

  @SuppressWarnings("unchecked")
  private static final Map<ResourceLoader, List<ClassPathEntry>> classPathCache = new ReferenceMap(
      AbstractReferenceMap.WEAK, AbstractReferenceMap.HARD);
  
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
   * Rescans the associated paths to recompute the available resources.
   * 
   * TODO(conroy,scottb): This synchronization could be improved upon to allow
   * disjoint sets of oracles to be refreshed simultaneously.
   * 
   * @param logger status and error details are written here
   * @param first At least one ResourceOracleImpl must be passed to refresh
   * @param rest Callers may optionally pass several oracles
   */
  public static synchronized void refresh(TreeLogger logger, ResourceOracleImpl first,
      ResourceOracleImpl... rest) {
    int len = 1 + rest.length;
    ResourceOracleImpl[] oracles = new ResourceOracleImpl[1 + rest.length];
    oracles[0] = first;
    System.arraycopy(rest, 0, oracles, 1, rest.length);

    Event resourceOracle =
        SpeedTracerLogger.start(CompilerEventType.RESOURCE_ORACLE, "phase", "refresh");
    TreeLogger refreshBranch = Messages.REFRESHING_RESOURCES.branch(logger, null);

    /*
     * Allocate fresh data structures in anticipation of needing to honor the
     * "new identity for the collections if anything changes" guarantee. Use a
     * LinkedHashMap because we do not want the order to change.
     */
    List<Map<String, ResourceData>> resourceDataMaps = new ArrayList<Map<String, ResourceData>>();

    List<PathPrefixSet> pathPrefixSets = new ArrayList<PathPrefixSet>();
    for (ResourceOracleImpl oracle : oracles) {
      if (!oracle.classPath.equals(oracles[0].classPath)) {
        throw new IllegalArgumentException("Refreshing multiple oracles with different classpaths");
      }
      resourceDataMaps.add(new LinkedHashMap<String, ResourceData>());
      pathPrefixSets.add(oracle.pathPrefixSet);
    }

    /*
     * Walk across path roots (i.e. classpath entries) in priority order. This
     * is a "reverse painter's algorithm", relying on being careful never to add
     * a resource that has already been added to the new map under construction
     * to create the effect that resources founder earlier on the classpath take
     * precedence.
     * 
     * Exceptions: super has priority over non-super; and if there are two super
     * resources with the same path, the one with the higher-priority path
     * prefix wins.
     */
    for (ClassPathEntry pathRoot : oracles[0].classPath) {
      TreeLogger branchForClassPathEntry =
          Messages.EXAMINING_PATH_ROOT.branch(refreshBranch, pathRoot.getLocation(), null);

      List<Map<AbstractResource, PathPrefix>> resourceToPrefixMaps =
          pathRoot.findApplicableResources(branchForClassPathEntry, pathPrefixSets);
      for (int i = 0; i < len; ++i) {
        Map<String, ResourceData> resourceDataMap = resourceDataMaps.get(i);
        Map<AbstractResource, PathPrefix> resourceToPrefixMap = resourceToPrefixMaps.get(i);
        for (Entry<AbstractResource, PathPrefix> entry : resourceToPrefixMap.entrySet()) {
          ResourceData newCpeData = new ResourceData(entry.getKey(), entry.getValue());
          String resourcePath = newCpeData.resource.getPath();
          ResourceData oldCpeData = resourceDataMap.get(resourcePath);
          // Old wins unless the new resource has higher priority.
          if (oldCpeData == null || oldCpeData.compareTo(newCpeData) < 0) {
            resourceDataMap.put(resourcePath, newCpeData);
          } else {
            Messages.IGNORING_SHADOWED_RESOURCE.log(branchForClassPathEntry, resourcePath, null);
          }
        }
      }
    }

    for (int i = 0; i < len; ++i) {
      Map<String, ResourceData> resourceDataMap = resourceDataMaps.get(i);
      Map<String, Resource> externalMap = new HashMap<String, Resource>();
      Set<Resource> externalSet = new HashSet<Resource>();
      for (Entry<String, ResourceData> entry : resourceDataMap.entrySet()) {
        String path = entry.getKey();
        ResourceData data = entry.getValue();
        externalMap.put(path, data.resource);
        externalSet.add(data.resource);
      }

      // Update exposed collections with new (unmodifiable) data structures.
      oracles[i].exposedResources = Collections.unmodifiableSet(externalSet);
      oracles[i].exposedResourceMap = Collections.unmodifiableMap(externalMap);
      oracles[i].exposedPathNames = Collections.unmodifiableSet(externalMap.keySet());
    }

    resourceOracle.end();
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

  private final List<ClassPathEntry> classPath;

  private Set<String> exposedPathNames = Collections.emptySet();

  private Map<String, Resource> exposedResourceMap = Collections.emptyMap();

  private Set<Resource> exposedResources = Collections.emptySet();

  private PathPrefixSet pathPrefixSet = new PathPrefixSet();

  /**
   * Constructs a {@link ResourceOracleImpl} from a set of
   * {@link ClassPathEntry ClassPathEntries}. The list is held by reference and
   * must not be modified.
   */
  public ResourceOracleImpl(List<ClassPathEntry> classPath) {
    this.classPath = classPath;
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
  List<ClassPathEntry> getClassPath() {
    return classPath;
  }
}
