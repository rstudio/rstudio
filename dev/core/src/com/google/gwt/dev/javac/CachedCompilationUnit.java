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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A convenient way to serialize a {@CompilationUnit}.
 */
public class CachedCompilationUnit extends CompilationUnit {
  private final DiskCacheToken astToken;
  private final long astVersion;
  private final Collection<CompiledClass> compiledClasses;
  private final ContentId contentId;
  private final Dependencies dependencies;
  private final boolean isError;
  private final boolean isGenerated;
  private final boolean isSuperSource;
  private final List<JsniMethod> jsniMethods;
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
    this.jsniMethods = sortJsniMethods(unit.getJsniMethods());
    this.methodArgNamesLookup = unit.getMethodArgs();
    this.typeName = unit.getTypeName();
    this.isError = unit.isError();
    this.isGenerated = unit.isGenerated();
    this.isSuperSource = unit.isSuperSource();
    this.problems = unit.problems;
    this.astToken = unit.astToken;
    this.astVersion = unit.astVersion;

    // Override these fields
    this.lastModified = lastModified;
  }

  /**
   * Create a compilation unit that can be serialized from another
   * {@link CompilationUnit}.
   *
   * @param unit A unit to copy
   * @param sourceToken A valid {@DiskCache} token for this unit's
   *          source code.
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
    this.jsniMethods = sortJsniMethods(unit.getJsniMethods());
    this.lastModified = unit.getLastModified();
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

  private List<JsniMethod> sortJsniMethods(List<JsniMethod> jsniMethods) {
    if (jsniMethods == null) {
      return null;
    }
    
    // copy because the source may be unmodifiable or singleton
    ArrayList<JsniMethod> copy = new ArrayList<JsniMethod>(jsniMethods);

    Collections.sort(copy, new Comparator<JsniMethod>() {
      @Override
      public int compare(JsniMethod o1, JsniMethod o2) {
        return o1.name().compareTo(o2.name());
      }
    });
    return copy;
  }
}
