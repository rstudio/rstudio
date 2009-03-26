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
package com.google.gwt.resources.rebind.context;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.resources.ext.ResourceContext;

/**
 * Defines base methods for ResourceContext implementations.
 */
public abstract class AbstractResourceContext implements ResourceContext {
  private final TreeLogger logger;
  private final GeneratorContext context;
  private final JClassType resourceBundleType;
  private String simpleSourceName;

  protected AbstractResourceContext(TreeLogger logger,
      GeneratorContext context, JClassType resourceBundleType) {
    this.logger = logger;
    this.context = context;
    this.resourceBundleType = resourceBundleType;
  }

  public JClassType getClientBundleType() {
    return resourceBundleType;
  }

  public GeneratorContext getGeneratorContext() {
    return context;
  }

  public String getImplementationSimpleSourceName() {
    if (simpleSourceName == null) {
      throw new IllegalStateException(
          "Simple source name has not yet been set.");
    }
    return simpleSourceName;
  }

  protected TreeLogger getLogger() {
    return logger;
  }

  void setSimpleSourceName(String name) {
    simpleSourceName = name;
  }
}
