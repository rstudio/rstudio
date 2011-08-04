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
 * A wrapper to access a base {@link GeneratorContext} instance but with
 * generator result caching disabled.
 */
public class NonIncrementalGeneratorContext extends DelegatingGeneratorContext {

  /**
   * Get a new instance wrapped from a base {@link GeneratorContext}
   * implementation.
   */
  public static GeneratorContext newInstance(GeneratorContext baseContext) {
    return new NonIncrementalGeneratorContext(baseContext);
  }

  private NonIncrementalGeneratorContext(GeneratorContext baseContext) {
    super(baseContext);
  }

  @Override
  public CachedGeneratorResult getCachedGeneratorResult() {
    // disabled
    return null;
  }

  @Override
  public boolean isGeneratorResultCachingEnabled() {
    // disabled
    return false;
  }

  @Override
  public boolean tryReuseTypeFromCache(String typeName) {
    // disabled
    return false;
  }
}
