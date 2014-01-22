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

import com.google.gwt.core.ext.DelegatingGeneratorContext;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;

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
class CachedGeneratorContext extends DelegatingGeneratorContext {
  private final GeneratorContext context;
  private Set<String> generatedResources = new HashSet<String>();
  private Set<String> generatedTypes = new HashSet<String>();

  CachedGeneratorContext(GeneratorContext context) {
    super(context);
    this.context = context;
  }

  /**
   * Provide the canonical context represented by this context.
   * 
   * @return the GeneratorContext backing this implementation.
   */
  public GeneratorContext getWrappedGeneratorContext() {
    return context;
  }

  @Override
  public PrintWriter tryCreate(TreeLogger logger, String packageName, String simpleName) {
    String typeName = packageName + '.' + simpleName;
    if (generatedTypes.contains(typeName)) {
      return null;
    }
    generatedTypes.add(typeName);
    return context.tryCreate(logger, packageName, simpleName);
  }

  @Override
  public OutputStream tryCreateResource(TreeLogger logger, String partialPath)
      throws UnableToCompleteException {
    if (generatedResources.contains(partialPath)) {
      return null;
    }
    generatedResources.add(partialPath);
    return context.tryCreateResource(logger, partialPath);
  }
}