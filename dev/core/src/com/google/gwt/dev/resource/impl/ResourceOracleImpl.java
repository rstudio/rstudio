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
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.resource.ResourceOracle;
import com.google.gwt.dev.util.PerfLogger;
import com.google.gwt.dev.util.msg.Message0;
import com.google.gwt.dev.util.msg.Message1String;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

/**
 * The normal implementation of {@link ResourceOracle}.
 */
public class ResourceOracleImpl implements ResourceOracle {

  private static class Messages {
    static final Message1String EXAMINING_PATH_ROOT = new Message1String(
        TreeLogger.DEBUG, "Searching for resources within $0");

    static final Message1String IGNORING_SHADOWED_RESOURCE = new Message1String(
        TreeLogger.DEBUG,
        "Resource '$0' is being shadowed by another resource higher in the classpath having the same name; this one will not be used");

    static final Message1String NEW_RESOURCE_FOUND = new Message1String(
        TreeLogger.DEBUG, "Found new resource: $0");

    static final Message0 NO_RESOURCES_CHANGED = new Message0(TreeLogger.DEBUG,
        "No resources changed");

    static final Message0 REFRESHING_RESOURCES = new Message0(TreeLogger.TRACE,
        "Refreshing resources");

    static final Message1String RESOURCE_BECAME_INVALID_BECAUSE_IT_IS_STALE = new Message1String(
        TreeLogger.SPAM,
        "Resource '$0' has been modified since it was last loaded and needs to be reloaded");

    static final Message1String RESOURCE_BECAME_INVALID_BECAUSE_IT_MOVED = new Message1String(
        TreeLogger.DEBUG,
        "Resource '$0' was found on a different classpath entry and needs to be reloaded");
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
    public boolean isStale() {
      return resource.isStale();
    }

    @Override
    public InputStream openContents() {
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
      this.resource = pathPrefix.shouldReroot() ? new RerootedResource(
          resource, pathPrefix) : resource;
      this.pathPrefix = pathPrefix;
    }

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

  private static final Map<ClassLoader, List<ClassPathEntry>> classPathCache = new HashMap<ClassLoader, List<ClassPathEntry>>();

  public static ClassPathEntry createEntryForUrl(TreeLogger logger, URL url)
      throws URISyntaxException, IOException {
    if (url.getProtocol().equals("file")) {
      File f = new File(url.toURI());
      String lowerCaseFileName = f.getName().toLowerCase(Locale.ENGLISH);
      if (f.isDirectory()) {
        return new DirectoryClassPathEntry(f);
      } else if (f.isFile() && lowerCaseFileName.endsWith(".jar")) {
        return new ZipFileClassPathEntry(new JarFile(f));
      } else if (f.isFile() && lowerCaseFileName.endsWith(".zip")) {
        return new ZipFileClassPathEntry(new ZipFile(f));
      } else {
        // It's a file ending in neither jar nor zip, speculatively try to
        // open as jar/zip anyway.
        try {
          return new ZipFileClassPathEntry(new JarFile(f));
        } catch (Exception ignored) {
        }
        try {
          return new ZipFileClassPathEntry(new ZipFile(f));
        } catch (Exception ignored) {
        }
        logger.log(TreeLogger.TRACE, "Unexpected entry in classpath; " + f
            + " is neither a directory nor an archive (.jar or .zip)");
        return null;
      }
    } else {
      logger.log(TreeLogger.WARN, "Unknown URL type for " + url, null);
      return null;
    }
  }

  private static void addAllClassPathEntries(TreeLogger logger,
      ClassLoader classLoader, List<ClassPathEntry> classPath) {
    // URL is expensive in collections, so we use URI instead
    // See: http://michaelscharf.blogspot.com/2006/11/javaneturlequals-and-hashcode-make.html
    Set<URI> seenEntries = new HashSet<URI>();
    for (; classLoader != null; classLoader = classLoader.getParent()) {
      if (classLoader instanceof URLClassLoader) {
        URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
        URL[] urls = urlClassLoader.getURLs();
        for (URL url : urls) {
          URI uri;
          try {
            uri = url.toURI();
          } catch (URISyntaxException e) {
            logger.log(TreeLogger.WARN, "Error processing classpath URL '"
                + url + "'", e);
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
            logger.log(TreeLogger.DEBUG,
                "Skipping URL due to access restrictions: " + url);
            continue;
          } catch (URISyntaxException e) {
            caught = e;
          } catch (IOException e) {
            caught = e;
          }
          logger.log(TreeLogger.WARN, "Error processing classpath URL '" + url
              + "'", caught);
        }
      }
    }
  }

  private static synchronized List<ClassPathEntry> getAllClassPathEntries(
      TreeLogger logger, ClassLoader classLoader) {
    List<ClassPathEntry> classPath = classPathCache.get(classLoader);
    if (classPath == null) {
      classPath = new ArrayList<ClassPathEntry>();
      addAllClassPathEntries(logger, classLoader, classPath);
      classPathCache.put(classLoader, classPath);
    }
    return classPath;
  }

  private final List<ClassPathEntry> classPath = new ArrayList<ClassPathEntry>();

  private Set<String> exposedPathNames = Collections.emptySet();

  private Map<String, Resource> exposedResourceMap = Collections.emptyMap();

  private Set<Resource> exposedResources = Collections.emptySet();

  private Map<String, ResourceData> internalMap = Collections.emptyMap();

  private PathPrefixSet pathPrefixSet = new PathPrefixSet();

  /**
   * Constructs a {@link ResourceOracleImpl} from a set of
   * {@link ClassPathEntry ClassPathEntries}. The passed-in list is copied, but
   * the underlying entries in the list are not. Those entries must be
   * effectively immutable except for reflecting actual changes to the
   * underlying resources.
   */
  public ResourceOracleImpl(List<ClassPathEntry> classPath) {
    this.classPath.addAll(classPath);
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
   * {@link URLClassLoader} will have their class path entries added to this
   * instances underlying class path.
   */
  public ResourceOracleImpl(TreeLogger logger, ClassLoader classLoader) {
    this(getAllClassPathEntries(logger, classLoader));
  }

  public void clear() {
    exposedPathNames = Collections.emptySet();
    exposedResourceMap = Collections.emptyMap();
    exposedResources = Collections.emptySet();
    internalMap = Collections.emptyMap();
  }

  public Set<String> getPathNames() {
    return exposedPathNames;
  }

  public PathPrefixSet getPathPrefixes() {
    return pathPrefixSet;
  }

  public Map<String, Resource> getResourceMap() {
    return exposedResourceMap;
  }

  public Set<Resource> getResources() {
    return exposedResources;
  }

  /**
   * Rescans the associated paths to recompute the available resources.
   * 
   * @param logger status and error details are written here
   */
  public void refresh(TreeLogger logger) {
    PerfLogger.start("ResourceOracleImpl.refresh");
    TreeLogger refreshBranch = Messages.REFRESHING_RESOURCES.branch(logger,
        null);

    /*
     * Allocate fresh data structures in anticipation of needing to honor the
     * "new identity for the collections if anything changes" guarantee. Use a
     * LinkedHashMap because we do not want the order to change.
     */
    Map<String, ResourceData> newInternalMap = new LinkedHashMap<String, ResourceData>();

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
    for (ClassPathEntry pathRoot : classPath) {
      TreeLogger branchForClassPathEntry = Messages.EXAMINING_PATH_ROOT.branch(
          refreshBranch, pathRoot.getLocation(), null);

      Map<AbstractResource, PathPrefix> resourceToPrefixMap = pathRoot.findApplicableResources(
          branchForClassPathEntry, pathPrefixSet);
      for (Entry<AbstractResource, PathPrefix> entry : resourceToPrefixMap.entrySet()) {
        ResourceData newCpeData = new ResourceData(entry.getKey(),
            entry.getValue());
        String resourcePath = newCpeData.resource.getPath();
        ResourceData oldCpeData = newInternalMap.get(resourcePath);
        // Old wins unless the new resource has higher priority.
        if (oldCpeData == null || oldCpeData.compareTo(newCpeData) < 0) {
          newInternalMap.put(resourcePath, newCpeData);
        } else {
          Messages.IGNORING_SHADOWED_RESOURCE.log(branchForClassPathEntry,
              resourcePath, null);
        }
      }
    }

    /*
     * Update the newInternalMap to preserve identity for any resources that
     * have not changed; also record whether or not there are ANY changes.
     * 
     * There's definitely a change if the sizes don't match; even if the sizes
     * do match, every new resource must match an old resource for there to be
     * no changes.
     */
    boolean didChange = internalMap.size() != newInternalMap.size();
    for (Map.Entry<String, ResourceData> entry : newInternalMap.entrySet()) {
      String resourcePath = entry.getKey();
      ResourceData newData = entry.getValue();
      ResourceData oldData = internalMap.get(resourcePath);
      if (shouldUseNewResource(logger, oldData, newData)) {
        didChange = true;
      } else {
        if (oldData.resource != newData.resource) {
          newInternalMap.put(resourcePath, oldData);
        }
      }
    }

    if (!didChange) {
      // Nothing to do, keep the same identities.
      PerfLogger.end();
      return;
    }

    internalMap = newInternalMap;

    Map<String, Resource> externalMap = new HashMap<String, Resource>();
    Set<Resource> externalSet = new HashSet<Resource>();
    for (Entry<String, ResourceData> entry : internalMap.entrySet()) {
      String path = entry.getKey();
      ResourceData data = entry.getValue();
      externalMap.put(path, data.resource);
      externalSet.add(data.resource);
    }

    // Update exposed collections with new (unmodifiable) data structures.
    exposedResources = Collections.unmodifiableSet(externalSet);
    exposedResourceMap = Collections.unmodifiableMap(externalMap);
    exposedPathNames = Collections.unmodifiableSet(externalMap.keySet());
    PerfLogger.end();
  }

  public void setPathPrefixes(PathPrefixSet pathPrefixSet) {
    this.pathPrefixSet = pathPrefixSet;
  }

  // @VisibleForTesting
  List<ClassPathEntry> getClassPath() {
    return classPath;
  }

  private boolean shouldUseNewResource(TreeLogger logger, ResourceData oldData,
      ResourceData newData) {
    AbstractResource newResource = newData.resource;
    String resourcePath = newResource.getPath();
    if (oldData != null) {
      // Test 1: Is the resource found in a different location than before?
      AbstractResource oldResource = oldData.resource;
      if (oldResource.getClassPathEntry() == newResource.getClassPathEntry()) {
        // Test 2: Has the resource changed since we last found it?
        if (!oldResource.isStale()) {
          // The resource has not changed.
          return false;
        } else {
          Messages.RESOURCE_BECAME_INVALID_BECAUSE_IT_IS_STALE.log(logger,
              resourcePath, null);
        }
      } else {
        Messages.RESOURCE_BECAME_INVALID_BECAUSE_IT_MOVED.log(logger,
            resourcePath, null);
      }
    } else {
      Messages.NEW_RESOURCE_FOUND.log(logger, resourcePath, null);
    }

    return true;
  }
}
