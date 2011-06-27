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
import com.google.gwt.util.tools.Utility;

import org.eclipse.jdt.core.compiler.CategorizedProblem;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

/**
 * A compilation unit that was generated.
 */
class SourceFileCompilationUnit extends CompilationUnitImpl {

  /**
   * A token to retrieve this object's bytes from the disk cache. It's generally
   * much faster to read from the disk cache than to reread individual
   * resources.
   */
  private long sourceToken = -1;

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
    if (sourceToken < 0) {
      InputStream in = null;
      try {
        in = sourceFile.openContents();
        sourceToken = diskCache.transferFromStream(in);
      } catch (IOException ex) {
        throw new RuntimeException("Can't read resource:" + sourceFile.getLocation(), ex);
      } finally {
        Utility.close(in);
      }
    }
    return new CachedCompilationUnit(this, sourceToken, astToken);
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

  @Deprecated
  @Override
  public String getSource() {
    try {
      if (sourceToken < 0) {
        String sourceCode = Shared.readSource(sourceFile);
        sourceToken = diskCache.writeString(sourceCode);
        return sourceCode;
      } else {
        return diskCache.readString(sourceToken);
      }
    } catch (IOException ex) {
      throw new RuntimeException("Can't read resource:" + sourceFile, ex);
    }
  }

  public Resource getSourceFile() {
    return sourceFile;
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
