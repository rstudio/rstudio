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
package com.google.gwt.dev.javac;

import com.google.gwt.dev.util.DiskCache;
import com.google.gwt.dev.util.Util;

import org.eclipse.jdt.core.compiler.CategorizedProblem;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Used by {@link MemoryUnitCacheTest} and {@link PersistentUnitCacheTest}.
 */
public class MockCompilationUnit extends CompilationUnit {
  private static final AtomicInteger nextTimestamp = new AtomicInteger(1);

  private final ContentId contentId;
  private final long lastModified;
  private final String resourceLocation;
  private final String typeName;

  public MockCompilationUnit(String typeName, String source) {
    this(typeName, source, "/mock/" + Shared.toPath(typeName));
  }

  public MockCompilationUnit(String typeName, String source, String resourceLocation) {
    this.typeName = typeName;
    this.resourceLocation = resourceLocation;
    contentId = new ContentId(typeName, source);
    lastModified = nextTimestamp.getAndIncrement();
  }

  @Override
  public CachedCompilationUnit asCachedCompilationUnit() {
    DiskCache diskCache = DiskCache.INSTANCE;
    long astToken = diskCache.writeByteArray(Util.getBytes("Dummy AST data"));
    return new CachedCompilationUnit(this, astToken);
  }

  @Override
  public Collection<CompiledClass> getCompiledClasses() {
    return null;
  }

  @Override
  public List<JsniMethod> getJsniMethods() {
    return null;
  }

  @Override
  public long getLastModified() {
    return lastModified;
  }

  @Override
  public MethodArgNamesLookup getMethodArgs() {
    return null;
  }

  @Override
  public String getResourceLocation() {
    return resourceLocation;
  }

  @Override
  public String getResourcePath() {
    return Shared.toPath(typeName);
  }

  @Override
  public String getTypeName() {
    return typeName;
  }

  @Override
  public byte[] getTypesSerialized() {
    return null;
  }

  @Override
  public boolean isError() {
    return false;
  }

  @Override
  public boolean isGenerated() {
    return false;
  }

  @Override
  public boolean isSuperSource() {
    return false;
  }

  @Override
  ContentId getContentId() {
    return contentId;
  }

  @Override
  Dependencies getDependencies() {
    return null;
  }

  @Override
  CategorizedProblem[] getProblems() {
    return new CategorizedProblem[0];
  }
}
