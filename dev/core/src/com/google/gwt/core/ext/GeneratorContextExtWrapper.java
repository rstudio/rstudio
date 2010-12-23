/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.core.ext;

import com.google.gwt.core.ext.linker.Artifact;
import com.google.gwt.core.ext.linker.GeneratedResource;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.rebind.CachedRebindResult;
import com.google.gwt.dev.resource.ResourceOracle;

import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * EXPERIMENTAL and subject to change. Do not use this in production code.
 * <p> 
 * A wrapper to access a base {@link GeneratorContext} instance as a 
 * {@link GeneratorContextExt} instance.  Methods from the 
 * {@link GeneratorContext} interface are passed through to the baseContext, 
 * while methods from the {@link GeneratorContextExt} interface are given 
 * default stub implementations.
 */
public class GeneratorContextExtWrapper implements GeneratorContextExt {
 
  /**
   * Get a new instance wrapped from a base {@link GeneratorContext} 
   * implementation.
   */
  public static GeneratorContextExt newInstance(GeneratorContext baseContext) {
    return new GeneratorContextExtWrapper(baseContext);
  } 
  
  private final GeneratorContext baseContext;
  
  public GeneratorContextExtWrapper(GeneratorContext baseContext) {
    this.baseContext = baseContext;
  }

  public void commit(TreeLogger logger, PrintWriter pw) {
    baseContext.commit(logger, pw);
  }

  public void commitArtifact(TreeLogger logger, Artifact<?> artifact)
      throws UnableToCompleteException {
    baseContext.commitArtifact(logger, artifact);
  }

  public GeneratedResource commitResource(TreeLogger logger, OutputStream os)
      throws UnableToCompleteException {
    return baseContext.commitResource(logger, os);
  }

  public CachedRebindResult getCachedGeneratorResult() {
    return null;
  }

  public PropertyOracle getPropertyOracle() {
    return baseContext.getPropertyOracle();
  }

  public ResourceOracle getResourcesOracle() {
    return baseContext.getResourcesOracle();
  }

  public long getSourceLastModifiedTime(JClassType sourceType) {
    return 0L;
  }

  public TypeOracle getTypeOracle() {
    return baseContext.getTypeOracle();
  }

  public boolean isGeneratorResultCachingEnabled() {
    return false;
  }

  public boolean reuseTypeFromCacheIfAvailable(String typeName) {
    return false;
  }

  public PrintWriter tryCreate(
      TreeLogger logger, String packageName, String simpleName) {
    return baseContext.tryCreate(logger, packageName, simpleName);
  }

  public OutputStream tryCreateResource(TreeLogger logger, String partialPath)
      throws UnableToCompleteException {
    return baseContext.tryCreateResource(logger, partialPath);
  }
}
