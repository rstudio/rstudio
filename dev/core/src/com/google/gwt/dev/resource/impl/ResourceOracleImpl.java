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
import com.google.gwt.dev.util.msg.Message0;
import com.google.gwt.dev.util.msg.Message1String;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
   * Used by rebasing {@link ResourceOracle ResourceOracles} to map from a full
   * classpath-based abstract path to an abstract path within a logical package.
   * 
   * @see ResourceOracleImpl#shouldRebasePaths()
   */
  private static class ResourceWrapper extends AbstractResource {
    private final String path;
    private final AbstractResource resource;

    public ResourceWrapper(String path, AbstractResource resource) {
      this.path = path;
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
    public URL getURL() {
      return resource.getURL();
    }

    @Override
    public boolean isStale() {
      return resource.isStale();
    }

    @Override
    public InputStream openContents() {
      return resource.openContents();
    }
  }

  public static ClassPathEntry createEntryForUrl(TreeLogger logger, URL url)
      throws URISyntaxException, IOException {
    if (url.getProtocol().equals("file")) {
      File f = new File(url.toURI());
      String lowerCaseFileName = f.getName().toLowerCase();
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
    for (; classLoader != null; classLoader = classLoader.getParent()) {
      if (classLoader instanceof URLClassLoader) {
        URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
        URL[] urls = urlClassLoader.getURLs();
        for (URL url : urls) {
          Throwable caught;
          try {
            ClassPathEntry entry = createEntryForUrl(logger, url);
            if (entry != null) {
              classPath.add(entry);
            }
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

  private static List<ClassPathEntry> getAllClassPathEntries(TreeLogger logger,
      ClassLoader classLoader) {
    ArrayList<ClassPathEntry> classPath = new ArrayList<ClassPathEntry>();
    addAllClassPathEntries(logger, classLoader, classPath);
    return classPath;
  }

  private final List<ClassPathEntry> classPath = new ArrayList<ClassPathEntry>();

  private Set<String> exposedPathNames = Collections.emptySet();

  private Map<String, Resource> exposedResourceMap = Collections.emptyMap();

  private Set<Resource> exposedResources = Collections.emptySet();

  private Map<String, AbstractResource> internalMap = Collections.emptyMap();

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

  public Set<String> getPathNames() {
    return exposedPathNames;
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
   * @throws UnableToCompleteException
   */
  public void refresh(TreeLogger logger) {
    TreeLogger refreshBranch = Messages.REFRESHING_RESOURCES.branch(logger,
        null);

    /*
     * Allocate fresh data structures in anticipation of needing to honor the
     * "new identity for the collections if anything changes" guarantee.
     */
    final Map<String, AbstractResource> newInternalMap = new HashMap<String, AbstractResource>();

    /*
     * Walk across path roots (i.e. classpath entries) in priority order. This
     * is a "reverse painter's algorithm", relying on being careful never to add
     * a resource that has already been added to the new map under construction
     * to create the effect that resources founder earlier on the classpath take
     * precedence.
     */
    int changeCount = 0;
    for (ClassPathEntry pathRoot : classPath) {
      TreeLogger branchForClassPathEntry = Messages.EXAMINING_PATH_ROOT.branch(
          refreshBranch, pathRoot.getLocation(), null);

      int prevChangeCount = changeCount;

      Set<AbstractResource> newResources = pathRoot.findApplicableResources(
          branchForClassPathEntry, pathPrefixSet);
      for (AbstractResource newResource : newResources) {
        String resourcePath = newResource.getPath();

        // Make sure we don't already have a resource by this name.
        if (newInternalMap.containsKey(resourcePath)) {
          Messages.IGNORING_SHADOWED_RESOURCE.log(branchForClassPathEntry,
              resourcePath, null);
          continue;
        }

        AbstractResource oldResource = internalMap.get(resourcePath);
        if (shouldUseNewResource(branchForClassPathEntry, oldResource,
            newResource)) {
          newInternalMap.put(resourcePath, newResource);
          ++changeCount;
        } else if (oldResource != null) {
          // Nothing changed, so carry the identity of the old one forward.
          newInternalMap.put(resourcePath, oldResource);
        }
      }

      if (changeCount == prevChangeCount) {
        Messages.NO_RESOURCES_CHANGED.log(branchForClassPathEntry, null);
      }
    }

    if (changeCount == 0) {
      /*
       * Nothing was added or modified, but we still have to be sure we didn't
       * lose any resources.
       */
      if (newInternalMap.size() == internalMap.size()) {
        /*
         * Exit without changing the current exposed collections to maintain the
         * identity requirements described in the spec for ResourceOracle.
         */
        return;
      }
    }

    internalMap = newInternalMap;
    Map<String, Resource> externalMap = rerootResourcePaths(newInternalMap);

    // Create a constant-time set for resources.
    Set<Resource> newResources = new HashSet<Resource>(externalMap.values());
    assert (newResources.size() == externalMap.size());

    // Update the gettable fields with the new (unmodifiable) data structures.
    exposedResources = Collections.unmodifiableSet(newResources);
    exposedResourceMap = Collections.unmodifiableMap(externalMap);
    exposedPathNames = Collections.unmodifiableSet(externalMap.keySet());
  }

  public void setPathPrefixes(PathPrefixSet pathPrefixSet) {
    this.pathPrefixSet = pathPrefixSet;
  }

  private Map<String, Resource> rerootResourcePaths(
      Map<String, AbstractResource> newInternalMap) {
    Map<String, Resource> externalMap;
    // Create an external map with rebased path names.
    externalMap = new HashMap<String, Resource>();
    for (AbstractResource resource : newInternalMap.values()) {
      String path = resource.getPath();
      if (externalMap.get(path) instanceof ResourceWrapper) {
        // A rerooted resource blocks any other resource at this path.
        continue;
      }
      int hitCount = 0;
      for (PathPrefix pathPrefix : pathPrefixSet.values()) {
        if (pathPrefix.allows(path)) {
          assert (path.startsWith(pathPrefix.getPrefix()));
          if (pathPrefix.shouldReroot()) {
            String rerootedPath = pathPrefix.getRerootedPath(path);
            // Try to reuse the same wrapper.
            Resource exposed = exposedResourceMap.get(rerootedPath);
            if (exposed instanceof ResourceWrapper) {
              ResourceWrapper exposedWrapper = (ResourceWrapper) exposed;
              if (exposedWrapper.resource == resource) {
                externalMap.put(rerootedPath, exposedWrapper);
                ++hitCount;
                break;
              }
            }
            // Just create a new wrapper.
            AbstractResource wrapper = new ResourceWrapper(rerootedPath,
                resource);
            externalMap.put(rerootedPath, wrapper);
            ++hitCount;
          } else {
            externalMap.put(path, resource);
            ++hitCount;
          }
        }
      }
      assert (hitCount > 0);
    }
    return externalMap;
  }

  private boolean shouldUseNewResource(TreeLogger logger,
      AbstractResource oldResource, AbstractResource newResource) {
    String resourcePath = newResource.getPath();
    if (oldResource != null) {
      // Test 1: Is the resource found in a different location than before?
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
