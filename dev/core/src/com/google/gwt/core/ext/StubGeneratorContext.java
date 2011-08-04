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
package com.google.gwt.core.ext;

import com.google.gwt.core.ext.linker.Artifact;
import com.google.gwt.core.ext.linker.GeneratedResource;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.resource.ResourceOracle;

import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * An abstract generator context class which by default throws
 * UnsupportedOperationException for all methods. Implementing classes can
 * selectively override individual methods. Useful for mocking and/or selective
 * reuse of generator functionality.
 */
public abstract class StubGeneratorContext implements GeneratorContext {

  @Override
  public boolean checkRebindRuleAvailable(String sourceTypeName) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void commit(TreeLogger logger, PrintWriter pw) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void commitArtifact(TreeLogger logger, Artifact<?> artifact) {
    throw new UnsupportedOperationException();
  }

  @Override
  public GeneratedResource commitResource(TreeLogger logger, OutputStream os) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CachedGeneratorResult getCachedGeneratorResult() {
    throw new UnsupportedOperationException();
  }

  @Override
  public PropertyOracle getPropertyOracle() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResourceOracle getResourcesOracle() {
    throw new UnsupportedOperationException();
  }

  @Override
  public TypeOracle getTypeOracle() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isGeneratorResultCachingEnabled() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isProdMode() {
    throw new UnsupportedOperationException();
  }

  @Override
  public PrintWriter tryCreate(TreeLogger logger, String packageName, String simpleName) {
    throw new UnsupportedOperationException();
  }

  @Override
  public OutputStream tryCreateResource(TreeLogger logger, String partialPath) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean tryReuseTypeFromCache(String typeName) {
    throw new UnsupportedOperationException();
  }
}