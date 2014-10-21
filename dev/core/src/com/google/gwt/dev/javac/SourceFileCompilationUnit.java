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
package com.google.gwt.dev.javac;

import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.resource.Resource;

import org.eclipse.jdt.core.compiler.CategorizedProblem;

import java.util.Collection;
import java.util.List;

/**
 * A compilation unit that was created from a source file in the ResourceLoader's classpath.
 */
class SourceFileCompilationUnit extends CompilationUnitImpl {

  private final Resource sourceFile;

  private final ContentId contentId;

  private final long lastModified;

  public SourceFileCompilationUnit(Resource sourceFile, ContentId contentId,
      List<CompiledClass> compiledClasses, List<JDeclaredType> types, Dependencies dependencies,
      Collection<? extends JsniMethod> jsniMethods, MethodArgNamesLookup methodArgs,
      CategorizedProblem[] problems, long lastModified) {
    super(compiledClasses, types, dependencies, jsniMethods, methodArgs, problems);
    this.sourceFile = sourceFile;
    // The resource can be updated out from underneath, affecting future
    // comparisons.
    this.lastModified = lastModified;
    this.contentId = contentId;
  }

  @Override
  public CachedCompilationUnit asCachedCompilationUnit() {
    return new CachedCompilationUnit(this, astToken);
  }

  @Override
  public long getLastModified() {
    return lastModified;
  }

  @Override
  public String getResourceLocation() {
    return sourceFile.getLocation();
  }

  @Override
  public String getResourcePath() {
    return sourceFile.getPathPrefix() + sourceFile.getPath();
  }

  @Override
  public String getTypeName() {
    return Shared.getTypeName(sourceFile);
  }

  @Deprecated
  @Override
  public boolean isGenerated() {
    return false;
  }

  @Deprecated
  @Override
  public boolean isSuperSource() {
    return sourceFile.wasRerooted();
  }

  @Override
  ContentId getContentId() {
    return contentId;
  }
}
