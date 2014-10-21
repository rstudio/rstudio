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

import com.google.gwt.dev.jjs.impl.GwtAstBuilder;
import com.google.gwt.dev.util.DiskCacheToken;
import com.google.gwt.dev.util.Util;

import org.eclipse.jdt.core.compiler.CategorizedProblem;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A convenient way to serialize a {@CompilationUnit}.
 */
public class CachedCompilationUnit extends CompilationUnit {
  private static final boolean normalizeTimestamps = Boolean.parseBoolean(
      System.getProperty("gwt.normalizeTimestamps", "false"));

  private final DiskCacheToken astToken;
  private final long astVersion;
  private transient Collection<CompiledClass> compiledClasses;
  private final ContentId contentId;
  private final Dependencies dependencies;
  private final boolean isError;
  private final boolean isGenerated;
  private final boolean isSuperSource;
  private transient List<JsniMethod> jsniMethods;
  private final long lastModified;
  private final MethodArgNamesLookup methodArgNamesLookup;
  private final CategorizedProblem[] problems;
  private final String resourceLocation;
  private final String resourcePath;
  private final String typeName;

  /**
   * Shallow copy of a CachedCompiliationUnit, replacing some parameters in the new copy.
   *
   * @param unit Unit to clone.
   * @param lastModified last modified date to replace in the clone
   * @param resourceLocation location to replace in the clone.
   */
  public CachedCompilationUnit(CachedCompilationUnit unit, long lastModified,
      String resourceLocation) {
    assert unit != null;
    this.compiledClasses = CompiledClass.copyForUnit(unit.getCompiledClasses(), this);
    this.contentId = unit.getContentId();
    this.dependencies = unit.getDependencies();
    this.resourcePath = unit.getResourcePath();
    this.resourceLocation = Util.stripJarPathPrefix(resourceLocation);
    this.jsniMethods = unit.getJsniMethods();
    this.methodArgNamesLookup = unit.getMethodArgs();
    this.typeName = unit.getTypeName();
    this.isError = unit.isError();
    this.isGenerated = unit.isGenerated();
    this.isSuperSource = unit.isSuperSource();
    this.problems = unit.problems;
    this.astToken = unit.astToken;
    this.astVersion = unit.astVersion;

    // Override these fields
    this.lastModified = normalizeTimestamps ? 0 : lastModified;
  }

  /**
   * Create a compilation unit that can be serialized from another
   * {@link CompilationUnit}.
   *
   * @param unit A unit to copy
   * @param astToken A valid {@DiskCache} token for this unit's
   *          serialized AST types.
   */
  @SuppressWarnings("deprecation")
  CachedCompilationUnit(CompilationUnit unit, long astToken) {
    assert unit != null;
    this.compiledClasses = CompiledClass.copyForUnit(unit.getCompiledClasses(), this);
    this.contentId = unit.getContentId();
    this.dependencies = unit.getDependencies();
    this.resourcePath = unit.getResourcePath();
    this.resourceLocation = Util.stripJarPathPrefix(unit.getResourceLocation());
    this.jsniMethods = unit.getJsniMethods();
    this.lastModified = normalizeTimestamps ? 0 : unit.getLastModified();
    this.methodArgNamesLookup = unit.getMethodArgs();
    this.typeName = unit.getTypeName();
    this.isError = unit.isError();
    this.isGenerated = unit.isGenerated();
    this.isSuperSource = unit.isSuperSource();
    CategorizedProblem[] problemsIn = unit.getProblems();
    if (problemsIn == null) {
      this.problems = null;
    } else {
      this.problems = new CategorizedProblem[problemsIn.length];
      for (int i = 0; i < problemsIn.length; i++) {
        this.problems[i] = new SerializableCategorizedProblem(problemsIn[i]);
      }
    }
    this.astToken = new DiskCacheToken(astToken);
    this.astVersion = GwtAstBuilder.getSerializationVersion();
  }

  @Override
  public CachedCompilationUnit asCachedCompilationUnit() {
    return this;
  }

  @Override
  public Collection<CompiledClass> getCompiledClasses() {
    return compiledClasses;
  }

  @Override
  public List<JsniMethod> getJsniMethods() {
    return jsniMethods;
  }

  @Override
  public long getLastModified() {
    return lastModified;
  }

  @Override
  public MethodArgNamesLookup getMethodArgs() {
    return methodArgNamesLookup;
  }

  @Override
  public String getResourceLocation() {
    return resourceLocation;
  }

  @Override
  public String getResourcePath() {
    return resourcePath;
  }

  @Override
  public String getTypeName() {
    return typeName;
  }

  @Override
  public byte[] getTypesSerialized() {
    return astToken.readByteArray();
  }

  @Override
  public boolean isError() {
    return isError;
  }

  @Override
  @Deprecated
  public boolean isGenerated() {
    return isGenerated;
  }

  @Override
  @Deprecated
  public boolean isSuperSource() {
    return isSuperSource;
  }

  @Override
  ContentId getContentId() {
    return contentId;
  }

  @Override
  Dependencies getDependencies() {
    return dependencies;
  }

  @Override
  CategorizedProblem[] getProblems() {
    return problems;
  }

  long getTypesSerializedVersion() {
    return astVersion;
  }

  @SuppressWarnings("unchecked")
  private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
    ois.defaultReadObject();
    this.compiledClasses = (Collection<CompiledClass>) ois.readObject();
    this.jsniMethods = (List<JsniMethod>) ois.readObject();
  }

  private <T> Collection<T> sort(Collection<T> collection, Comparator<T> comparator) {
    if (collection == null) {
      return null;
    }

    // copy because the source may be unmodifiable or singleton
    ArrayList<T> copy = new ArrayList<T>(collection);

    Collections.sort(copy, comparator);
    return copy;
  }

  private void writeObject(ObjectOutputStream oos) throws IOException {
    oos.defaultWriteObject();
    oos.writeObject(sort(this.compiledClasses, new Comparator<CompiledClass>() {
      @Override
      public int compare(CompiledClass o1, CompiledClass o2) {
        return o1.getSourceName().compareTo(o2.getSourceName());
      }
    }));
    oos.writeObject(sort(this.jsniMethods, new Comparator<JsniMethod>() {
      @Override
      public int compare(JsniMethod o1, JsniMethod o2) {
         return o1.name().compareTo(o2.name());
      }
    }));
  }
}
