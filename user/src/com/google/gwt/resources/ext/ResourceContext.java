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
package com.google.gwt.resources.ext;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;

import java.net.URL;

/**
 * Context object for ResourceGenerators. An instance of this type will be
 * provided by the resource generation framework to implementations of
 * ResourceGenerator via {@link ResourceGenerator#init}. Because this interface
 * is not intended to be implemented by end-users, the API provided by this
 * interface may be extended in the future.
 * <p>
 * Depending on the optimizations made by the implementation of {@link #deploy},
 * the resulting URL may or may not be compatible with standard
 * {@link com.google.gwt.http.client.RequestBuilder} / XMLHttpRequest security
 * semantics. If the resource is intended to be used with XHR, or if there are
 * other reasons why embedding the resource is undesirable such as known
 * incompatibilities, the <code>forceExternal</code> parameter should be set to
 * <code>true</code> when invoking {@link #deploy}.
 * </p>
 */
public interface ResourceContext {

  /**
   * Cause a specific collection of bytes to be available in the program's
   * compiled output. The return value of this method is a Java expression which
   * will evaluate to the location of the resource at runtime. The exact format
   * should not be depended upon.
   * 
   * @param suggestedFileName an unobfuscated filename to possibly use for the
   *          resource
   * @param mimeType the MIME type of the data being provided
   * @param data the bytes to add to the output
   * @param forceExternal prevents embedding of the resource, e.g. in case of
   *          known incompatibilities or for example to enforce compatibility
   *          with security restrictions if the resource is intended to be
   *          accessed via an XMLHttpRequest
   * @return a Java expression which will evaluate to the location of the
   *         provided resource at runtime
   */
  String deploy(String suggestedFileName, String mimeType, byte[] data,
      boolean forceExternal) throws UnableToCompleteException;

  /**
   * Cause a specific collection of bytes to be available in the program's
   * compiled output. The return value of this method is a Java expression which
   * will evaluate to the location of the resource at runtime. The exact format
   * should not be depended upon.
   *
   * @param resource the resource to add to the compiled output
   * @param forceExternal prevents embedding of the resource, e.g. in case of
   *          known incompatibilities or for example to enforce compatibility
   *          with security restrictions if the resource is intended to be
   *          accessed via an XMLHttpRequest
   * @return a Java expression which will evaluate to the location of the
   *         provided resource at runtime
   * @deprecated use {@link #deploy(URL, String, boolean)} instead
   */
  @Deprecated
  String deploy(URL resource, boolean forceExternal)
      throws UnableToCompleteException;

  /**
   * Cause a specific collection of bytes to be available in the program's
   * compiled output. The return value of this method is a Java expression which
   * will evaluate to the location of the resource at runtime. The exact format
   * should not be depended upon.
   * 
   * @param resource the resource to add to the compiled output
   * @param mimeType optional MIME Type to be used for an embedded resource
   * @param forceExternal prevents embedding of the resource, e.g. in case of
   *          known incompatibilities or for example to enforce compatibility
   *          with security restrictions if the resource is intended to be
   *          accessed via an XMLHttpRequest
   * @return a Java expression which will evaluate to the location of the
   *         provided resource at runtime
   */
  String deploy(URL resource, String mimeType, boolean forceExternal)
      throws UnableToCompleteException;

  /**
   * Retrieve data from the ResourceContext.
   * 
   * @param <T> the type of data to retrieve
   * @param key the key value passed to {@link #getCachedData}
   * @param clazz the type to which the cached value must be assignable
   * @return the value previously passed to {@link #putCachedData} or
   *         <code>null</code> if the data was not found
   * @throws ClassCastException if the cached data is not assignable to the
   *           specified type
   */
  <T> T getCachedData(String key, Class<T> clazz);

  /**
   * Return the interface type of the resource bundle being generated.
   */
  JClassType getClientBundleType();

  /**
   * Return the GeneratorContext in which the overall resource generation
   * framework is being run. Implementations of ResourceGenerator should prefer
   * {@link #deploy} over {@link GeneratorContext#tryCreateResource} in order to
   * take advantage of serving optimizations that can be performed by the bundle
   * architecture.
   */
  GeneratorContext getGeneratorContext();

  /**
   * Returns the simple source name of the implementation of the bundle being
   * generated. This can be used during code-generation to refer to the instance
   * of the bundle (e.g. via <code>SimpleSourceName.this</code>).
   * 
   * @throws IllegalStateException if this method is called during
   *           {@link ResourceGenerator#init} or
   *           {@link ResourceGenerator#prepare} methods.
   */
  String getImplementationSimpleSourceName() throws IllegalStateException;
  
  /**
   * Returns a {@link ClientBundleRequirements} object, which can be used to
   * track deferred-binding and configuration properties that are relevant to a
   * resource context.
   */
  ClientBundleRequirements getRequirements();
  
  /**
   * Store data in the ResourceContext. ResourceGenerators may reduce the amount
   * of recomputation performed by caching data the ResourceContext. This cache
   * will be invalidated when the compiler's TypeOracle is refreshed or
   * replaced. Each ResourceGenerator has an isolated view of the cache.
   * 
   * @param <T> the type of data being stored
   * @param key a string key to locate the data
   * @param value the value to store
   * @return <code>true</code> if the cache did not previously contain the
   *         key-value pair
   */
  <T> boolean putCachedData(String key, T value);

  /**
   * Indicates if the runtime context supports data: urls. When data URLs are
   * supported by the context, aggregation of resource data into larger payloads
   * is discouraged, as it offers reduced benefit to the application at runtime.
   */
  boolean supportsDataUrls();
}
