/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.i18n.rebind;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.Artifact;
import com.google.gwt.core.ext.linker.GeneratedResource;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.resource.ResourceOracle;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

/**
 * Keeps track of types/resources previously created to avoid warnings about
 * trying to generate the same file multiple times during a single generator
 * run. This is needed when one generator calls other generators multiple times
 * (such as for runtime locale support).
 */
class CachedGeneratorContext implements GeneratorContext {
  private final GeneratorContext context;
  private Set<String> generatedResources = new HashSet<String>();
  private Set<String> generatedTypes = new HashSet<String>();

  CachedGeneratorContext(GeneratorContext context) {
    this.context = context;
  }

  public void commit(TreeLogger logger, PrintWriter pw) {
    context.commit(logger, pw);
  }

  public void commitArtifact(TreeLogger logger, Artifact<?> artifact)
      throws UnableToCompleteException {
    context.commitArtifact(logger, artifact);
  }

  public GeneratedResource commitResource(TreeLogger logger, OutputStream os)
      throws UnableToCompleteException {
    return context.commitResource(logger, os);
  }

  public PropertyOracle getPropertyOracle() {
    return context.getPropertyOracle();
  }

  public ResourceOracle getResourcesOracle() {
    return context.getResourcesOracle();
  }

  public TypeOracle getTypeOracle() {
    return context.getTypeOracle();
  }

  public PrintWriter tryCreate(TreeLogger logger, String packageName,
      String simpleName) {
    String typeName = packageName + '.' + simpleName;
    if (generatedTypes.contains(typeName)) {
      return null;
    }
    generatedTypes.add(typeName);
    return context.tryCreate(logger, packageName, simpleName);
  }

  public OutputStream tryCreateResource(TreeLogger logger, String partialPath)
      throws UnableToCompleteException {
    if (generatedResources.contains(partialPath)) {
      return null;
    }
    generatedResources.add(partialPath);
    return context.tryCreateResource(logger, partialPath);
  }
}