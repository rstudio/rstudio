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
package com.google.gwt.dev.javac.impl;

import com.google.gwt.dev.javac.JavaSourceFile;
import com.google.gwt.dev.javac.JavaSourceOracle;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.resource.ResourceOracle;
import com.google.gwt.dev.util.Util;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Implements {@link JavaSourceOracle} on top of a {@link ResourceOracle}.
 */
public class JavaSourceOracleImpl implements JavaSourceOracle {

  private static class JavaSourceFileImpl extends JavaSourceFile {

    private final String location;
    private final String name;
    private final String packageName;
    private Resource resource;
    private final String shortName;

    public JavaSourceFileImpl(Resource resource) {
      this.resource = resource;
      location = resource.getLocation();
      String path = resource.getPath();
      assert (path.endsWith(".java"));
      path = path.substring(0, path.lastIndexOf('.'));
      name = path.replace('/', '.');
      int pos = name.lastIndexOf('.');
      if (pos < 0) {
        shortName = name;
        packageName = "";
      } else {
        shortName = name.substring(pos + 1);
        packageName = name.substring(0, pos);
      }
    }

    @Override
    public String getLocation() {
      return location;
    }

    @Override
    public String getPackageName() {
      return packageName;
    }

    @Override
    public String getShortName() {
      return shortName;
    }

    @Override
    public String getTypeName() {
      return name;
    }

    @Override
    public String readSource() {
      if (resource != null) {
        InputStream contents = resource.openContents();
        return Util.readStreamAsString(contents);
      }
      return null;
    }

    Resource getResource() {
      return resource;
    }

    void invalidate() {
      resource = null;
    }
  }

  /**
   * The last resource set returned by my oracle.
   */
  private Set<Resource> cachedResources = Collections.emptySet();

  /**
   * An unmodifiable set of exposedClassNames to return to a client.
   */
  private Set<String> exposedClassNames = Collections.emptySet();

  /**
   * An unmodifiable set of exposedSourceFiles to return to a client.
   */
  private Set<JavaSourceFile> exposedSourceFiles = Collections.emptySet();

  /**
   * An unmodifiable source map to return to a client.
   */
  private Map<String, JavaSourceFile> exposedSourceMap = Collections.emptyMap();

  /**
   * My resource oracle.
   */
  private final ResourceOracle oracle;

  /**
   * My internal set of source files.
   */
  private final Set<JavaSourceFileImpl> sourceFiles = new HashSet<JavaSourceFileImpl>();

  public JavaSourceOracleImpl(ResourceOracle oracle) {
    this.oracle = oracle;
  }

  public Set<String> getClassNames() {
    refresh();
    return exposedClassNames;
  }

  public Set<JavaSourceFile> getSourceFiles() {
    refresh();
    return exposedSourceFiles;
  }

  public Map<String, JavaSourceFile> getSourceMap() {
    refresh();
    return exposedSourceMap;
  }

  private void refresh() {
    Set<Resource> newResources = oracle.getResources();
    if (newResources == cachedResources) {
      // We're up to date.
      return;
    }

    // Divide resources into changed and unchanged.
    Set<Resource> unchanged = new HashSet<Resource>(cachedResources);
    unchanged.retainAll(newResources);

    Set<Resource> changed = new HashSet<Resource>(newResources);
    changed.removeAll(unchanged);

    // First remove any stale source files.
    for (Iterator<JavaSourceFileImpl> it = sourceFiles.iterator(); it.hasNext();) {
      JavaSourceFileImpl sourceFile = it.next();
      if (!unchanged.contains(sourceFile.getResource())) {
        sourceFile.invalidate();
        it.remove();
      }
    }

    // Then add any new source files.
    for (Resource newResource : changed) {
      sourceFiles.add(new JavaSourceFileImpl(newResource));
    }

    // Finally rebuild the unmodifiable views.
    Map<String, JavaSourceFile> sourceMap = new HashMap<String, JavaSourceFile>();
    for (JavaSourceFileImpl sourceFile : sourceFiles) {
      sourceMap.put(sourceFile.getTypeName(), sourceFile);
    }
    exposedSourceMap = Collections.unmodifiableMap(sourceMap);
    exposedClassNames = Collections.unmodifiableSet(sourceMap.keySet());
    HashSet<JavaSourceFile> sourceFilesConstantLookup = new HashSet<JavaSourceFile>(
        sourceMap.values());
    exposedSourceFiles = Collections.unmodifiableSet(sourceFilesConstantLookup);

    // Record the update.
    cachedResources = newResources;
  }
}
