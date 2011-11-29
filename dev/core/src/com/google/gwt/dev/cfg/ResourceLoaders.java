/*
 * Copyright 2011 Google Inc.
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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Creates instances of {@link ResourceLoader}.
 */
public class ResourceLoaders {

  private static class ClassLoaderAdapter implements ResourceLoader {
    private final ClassLoader wrapped;

    public ClassLoaderAdapter(ClassLoader wrapped) {
      this.wrapped = wrapped;
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof ClassLoaderAdapter)) {
        return false;
      }
      ClassLoaderAdapter otherAdapter = (ClassLoaderAdapter) other;
      return wrapped.equals(otherAdapter.wrapped);
    }

    /**
     * Returns the URLs of the wrapped ClassLoader and each parent that's a URLClassLoader.
     */
    @Override
    public List<URL> getClassPath() {
      List<URL> result = new ArrayList<URL>();
      for (ClassLoader candidate = wrapped; candidate != null; candidate = candidate.getParent()) {
        if (candidate instanceof URLClassLoader) {
          URL[] urls = ((URLClassLoader) candidate).getURLs();
          result.addAll(Arrays.asList(urls));
        }
      }
      return result;
    }

    @Override
    public URL getResource(String resourceName) {
      return wrapped.getResource(resourceName);
    }

    @Override
    public int hashCode() {
      return wrapped.hashCode();
    }
  }

  /**
   * A ResourceLoader that prefixes some directories to another ResourceLoader.
   */
  private static class PrefixLoader implements ResourceLoader {
    private final List<File> path;
    private final List<URL> pathAsUrls = new ArrayList<URL>();
    private final ResourceLoader fallback;

    public PrefixLoader(List<File> path, ResourceLoader fallback) {
      assert path != null;
      this.path = path;
      this.fallback = fallback;
      for (File file : path) {
        try {
          pathAsUrls.add(file.toURI().toURL());
        } catch (MalformedURLException e) {
          throw new RuntimeException("can't create URL for file: " + file);
        }
      }
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof PrefixLoader)) {
        return false;
      }
      PrefixLoader otherLoader = (PrefixLoader) other;
      return path.equals(otherLoader.path) && fallback.equals(otherLoader.fallback);
    }

    @Override
    public List<URL> getClassPath() {
      List<URL> result = new ArrayList<URL>();
      result.addAll(pathAsUrls);
      result.addAll(fallback.getClassPath());
      return result;
    }

    @Override
    public URL getResource(String resourceName) {
      for (File prefix : path) {
        File candidate = new File(prefix, resourceName);
        if (candidate.exists()) {
          try {
            return candidate.toURI().toURL();
          } catch (MalformedURLException e) {
            return null;
          }
        }
      }
      return fallback.getResource(resourceName);
    }

    @Override
    public int hashCode() {
      return path.hashCode() ^ fallback.hashCode();
    }
  }

  /**
   * Creates a ResourceLoader that loads from the given thread's class loader.
   */
  public static ResourceLoader forClassLoader(Thread thread) {
    return wrap(thread.getContextClassLoader());
  }

  /**
   * Creates a ResourceLoader that loads from a list of directories and falls back
   * to another ResourceLoader.
   */
  public static ResourceLoader forPathAndFallback(List<File> path, ResourceLoader fallback) {
    return new PrefixLoader(path, fallback);
  }
  
  /**
   * Adapts a ClassLoader to work as a ResourceLoader.
   * (Caveat: any ClassLoader in the chain that isn't a URLClassLoader won't contribute to the
   * results of {@link  ResourceLoader#getClassPath}.)
   */
  public static ResourceLoader wrap(ClassLoader loader) {
    return new ClassLoaderAdapter(loader);
  }

  private ResourceLoaders() {
  }
}
