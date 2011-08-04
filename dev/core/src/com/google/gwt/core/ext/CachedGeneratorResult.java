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

/**
 * An interface to represent the cached results from a previous generator
 * invocation. This is made available in the {@link GeneratorContext} to
 * subsequent invocations of the same generator, when called under the same
 * conditions (e.g. for the same rebind rule and requested type name).
 * 
 * @see GeneratorContext#getCachedGeneratorResult
 */
public interface CachedGeneratorResult {
  /**
   * Retrieves cached client data by key.
   */
  Object getClientData(String key);

  /**
   * Returns the cached result rebind type name.
   */
  String getResultTypeName();

  /**
   * Returns the time this generator result was created.
   */
  long getTimeGenerated();

  /**
   * Check whether a given type is present in the cached result.
   */
  boolean isTypeCached(String typeName);
}