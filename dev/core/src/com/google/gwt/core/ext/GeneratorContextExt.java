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

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.dev.javac.rebind.CachedRebindResult;

/**
 * EXPERIMENTAL and subject to change. Do not use this in production code.
 * <p>
 * An extension to GeneratorContext which includes access to previously cached
 * rebind results.
 * <p> 
 * TODO(jbrosenberg): Merge this into {@link GeneratorContext} directly, once 
 * the api has stabilized and we can remove the "experimental" moniker.
 */
public interface GeneratorContextExt extends GeneratorContext {
  
  /**
   * Get cached result from a previous run of the current generator, if available.
   * 
   * @return A {@link com.google.gwt.dev.javac.rebind.CachedRebindResult} object,
   *         if one has been provided to the context.  Null is returned if there
   *         is no previous result, or if generator result caching is not enabled.
   */
  CachedRebindResult getCachedGeneratorResult();
  
  /**
   * Get source last modified time.
   * <p>
   * TODO(jbrosenberg): Implement in terms of a getVersion method yet to be
   * added to TypeOracle, instead of looking for age of a java source file.
   * This will soon be removed.
   */
  long getSourceLastModifiedTime(JClassType sourceType);
  
  /**
   * Check whether generator result caching is currently enabled.
   */
  boolean isGeneratorResultCachingEnabled();
  
  /**
   * Mark a type to be reused from the generator result cache.  Calling this
   * method with a successful response indicates that the calling generator will 
   * not re-generate this type.  A cached version of this type will be added
   * to the context once the calling generator returns from 
   * {@link GeneratorExt#generateIncrementally}, with a result containing 
   * {@link com.google.gwt.dev.javac.rebind.RebindStatus#USE_PARTIAL_CACHED}.
   * 
   * @param typeName the fully qualified name of a type.
   * @return true if the requested type is available from the generator result 
   *         cache, false otherwise.
   */
  boolean reuseTypeFromCacheIfAvailable(String typeName);
}
